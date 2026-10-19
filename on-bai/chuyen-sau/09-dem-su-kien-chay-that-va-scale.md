# Bài 09 — Đếm sự kiện: chạy thật với sự cố, đo fan-out, và kế hoạch scale 1M → 10M request/s

> **Dùng cho:** **Katalon**, phần cuối của bài fake key: *"chứng minh nó đúng"*, *"cái gì vỡ trước khi tải ×10"*,
> *"cần bao nhiêu máy"*. Bài này chạy toàn tuyến trên Kafka + Postgres thật với sự cố cố ý, đo một hiệu ứng chỉ thấy khi
> chạy thật (fan-out của block), sửa nó, rồi gom mọi số đo thành một kế hoạch cỡ hệ thống.
>
> **Thời lượng:** đọc 45 phút · lab 20 phút · drill 20 phút
>
> **Lab:** [`PipelineTest`](lab/src/test/java/com/prep/cs/counting/PipelineTest.java),
> [`scripts/e-ingest-fanout.sh`](lab/scripts/e-ingest-fanout.sh) · output thật:
> [`results/test-output.txt`](lab/results/test-output.txt), [`results/e-ingest-fanout-truoc.txt`](lab/results/e-ingest-fanout-truoc.txt)
> (trước khi sửa), [`results/e-ingest-fanout.txt`](lab/results/e-ingest-fanout.txt) (sau khi sửa)
>
> **Cần trước:** [bài 06](06-dem-su-kien-1m-10m-de-va-kien-truc.md), [07](07-dem-su-kien-code-ingest.md),
> [08](08-dem-su-kien-code-aggregator.md). **Học tiếp:** [bài 10](10-ledger-tinh-so-du-tu-code.md) (sổ cái).

```bash
cd on-bai/chuyen-sau/lab
docker compose -f docker/cluster.yml up -d --wait
mvn -q test -Dtest=PipelineTest          # ~25 giây: toàn tuyến + sự cố
scripts/e-ingest-fanout.sh               # ~4 phút: số partition, flush và cỡ block
```

---

## 0. Trọng tâm phỏng vấn

| # | Câu hỏi | Trả lời của senior |
|:-:|---|---|
| 1 | Làm sao biết hệ thống đếm đúng? | Một test toàn tuyến có **oracle**: đếm lúc sinh request, độc lập với code. Bơm sự cố (aggregator chết, ingest chết, retry, client khởi động lại) rồi so **chính xác** với Postgres, cho cả 100 khoảng ngẫu nhiên |
| 2 | Cái gì vỡ đầu tiên khi tải ×10? | Tầng HTTP (CPU, băng thông). Sau đó là **số record Kafka** nếu để mỗi ingest node rải block ra quá nhiều partition. Postgres không vỡ: nó chỉ thấy số đã gộp |
| 3 | "Fan-out" của ingest là gì? | Mỗi node giữ một block mở cho **mỗi** partition, flush mỗi 5 ms. Nhiều partition hơn thì block nhỏ hơn, nhiều batch Kafka hơn. Đo: 48 partition so với 12 thì **−25% thông lượng, ×2 rác** |
| 4 | Đo ra lỗi gì, sửa ra sao? | Bộ đệm 64 KB cấp mới cho **mỗi** block dù block chỉ có vài chục bản ghi. Dùng lại bộ đệm: rác ở 12 partition giảm từ 3,2–3,4 KB xuống **1,8 KB** mỗi request |
| 5 | 10M request/s cần gì? | ~85 node ingest 16 vCPU, LB 80 Gbit/s, 6 broker Kafka, 12–16 partition, 3 tiến trình aggregator, **một** Postgres. Tầng HTTP chiếm >90% chi phí |

---

## 1. Toàn tuyến với sự cố: `PipelineTest`

📄 [`PipelineTest.java`](lab/src/test/java/com/prep/cs/counting/PipelineTest.java). Mọi thứ đều thật: topic Kafka
mới, registry qua topic compacted, 2 ingest node, 2 aggregator trong một consumer group, Postgres.

```java
static final int PARTITIONS = 6, CLIENTS = 200, PER_CLIENT = 500, ITEMS = 20;
...
Aggregator a1 = new Aggregator("agg-1", Infra.KAFKA, topic, group, store, 500, 15 * 60_000).start();
Aggregator a2 = new Aggregator("agg-2", Infra.KAFKA, topic, group, store, 500, 15 * 60_000).start();
IngestNode n1 = new IngestNode(Infra.KAFKA, topic, PARTITIONS, registry::current, 2_000, 5);
IngestNode n2 = new IngestNode(Infra.KAFKA, topic, PARTITIONS, registry::current, 2_000, 5);
```

**Kịch bản sự cố** chạy song song với tải, theo tiến độ thật (số request đã được xác nhận):

```java
while (acked.get() < total * 3 / 10) Thread.sleep(20);
a1.crash();                     // 30%: chet sau khi xu ly, truoc khi commit -> delta trong RAM mat het
while (acked.get() < total * 5 / 10) Thread.sleep(20);
aggs.add(newAggregator("agg-3", topic, group, store));   // 50%: thanh vien moi -> rebalance
while (acked.get() < total * 6 / 10) Thread.sleep(20);
n1.crash();                     // 60%: block trong RAM chua gui -> client het gio, retry sang n2
```

**Mỗi client** cư xử như SDK thật, và oracle chỉ đếm request **duy nhất**:

```java
for (int i = 0; i < PER_CLIENT + (clientId < 1_010 ? 1 : 0); i++) {
    long seq = i;
    if (clientId < 1_010 && i == PER_CLIENT / 2) epoch = 2;              // 10 client khoi dong lai
    if (epoch == 2) seq = i - PER_CLIENT / 2;
    long ts = BASE_MS + r.nextLong(SPREAD_MS);                            // event time trai 10 phut
    long[] exp = new long[4];
    byte[] body = gen.body(clientId, epoch, seq, ts, ITEMS, r, exp);
    send(body, nodes, r.nextInt(2));                                     // cho 202; loi thi thu node khac, CUNG body
    oracle.merge(ts / 1000, exp, ...);                                   // dem MOT lan, theo giay
    if (r.nextInt(100) < 5) send(body, nodes, r.nextInt(2));             // 5%: mat response, gui lai
}
if (clientId < 1_010) send(gen.body(clientId, 1, 3, BASE_MS, ...), nodes, 0);   // tien trinh cu "song lai"
```

`send()` thử lại **cùng body** sang node khác cho tới khi nhận 202, với timeout 2 giây. Đúng hợp đồng SDK ở bài 06 §4.1.

### 1.1 Output thật

```text
[LAB] registry tu topic compacted: 10000 key (legacy_removed: false)
[agg-1] nhan p0: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-1] nhan p1: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-1] nhan p2: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-1] nhan p3: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-1] nhan p4: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-1] nhan p5: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-2] nhan p3: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-2] nhan p4: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[agg-2] nhan p5: offset trong Postgres = 0, doc lai tu 0 de nap state khu trung
[LAB] 9.4s 30300/100000 request da xac nhan: agg-1 CHET (delta chua commit mat het)
[agg-2] nhan p0: offset trong Postgres = 812, doc lai tu 0 de nap state khu trung
[agg-2] nhan p1: offset trong Postgres = 898, doc lai tu 0 de nap state khu trung
[agg-2] nhan p2: offset trong Postgres = 913, doc lai tu 0 de nap state khu trung
[LAB] 12.2s 50544/100000: agg-3 vao group (rebalance)
[LAB] 13.5s 60163/100000: ingest node 1 CHET
[agg-3] nhan p3: offset trong Postgres = 2090, doc lai tu 0 de nap state khu trung
[agg-3] nhan p4: offset trong Postgres = 1886, doc lai tu 0 de nap state khu trung
[agg-3] nhan p5: offset trong Postgres = 2040, doc lai tu 0 de nap state khu trung
[LAB] aggregator da commit toi cuoi moi partition: {0=2119, 1=2028, 2=2087, 3=2213, 4=2027, 5=2190}
[LAB] 100010 request duy nhat trong 19.3 s (5191 request/s, 103827 item/s), retry 20752, gui trung co chu y 5039; Postgres co 614 o
[LAB] oracle  TRUE=900174 FALSE=599142 FAKE_KEY=300450 MALFORMED=200434
[LAB] Postgres TRUE=900174 FALSE=599142 FAKE_KEY=300450 MALFORMED=200434 successRate=0.6004 (doc 69 bucket)
[LAB] 100 khoang ngau nhien (do phan giai giay) deu khop oracle
```

Đọc từng dòng:

1. **Registry** dựng từ topic compacted: 10.000 key; `legacy_removed` đã bị tombstone gỡ (bài 07 §9).
2. **Lúc đầu agg-1 nhận cả 6 partition**, rồi agg-2 vào group và lấy p3–p5. Offset trong Postgres còn là 0 vì chưa ai
   commit gì.
3. **agg-1 chết ở 30%** (9,4 s), sau khi đã xử lý nhưng trước khi commit: delta trong RAM của nó mất. agg-2 nhận p0–p2
   với offset **812 / 898 / 913**, đúng chỗ agg-1 đã commit tới lần cuối. Nó đọc lại từ 0 (dữ liệu chưa tới 15 phút
   tuổi) **chỉ để nạp state khử trùng**, rồi đếm từ 812, 898, 913. Phần agg-1 đã xử lý mà chưa commit được đếm lại
   đúng một lần.
4. **agg-3 vào ở 50%:** rebalance. agg-3 lấy p3–p5 tại offset 2090 / 1886 / 2040: agg-2 đã commit phần dở trong
   `onPartitionsRevoked` trước khi trả.
5. **Ingest node 1 chết ở 60%:** block đang mở trong RAM của nó mất. Không client nào nhận 202 cho các request đó; chúng
   hết giờ sau 2 giây và gửi lại sang node 2. Phần lớn trong 20.752 lần `retry` là đây: mỗi lần client chọn node 1 sau
   khi nó chết là một lần thử hỏng.
6. **`gui trung co chu y 5039`:** 5% request gửi hai lần. Cả 5.039 bản đều tới Kafka và đều bị aggregator đánh
   `DUPLICATE`.
7. **Oracle = Postgres, cả 4 con số**, và **100 khoảng ngẫu nhiên** độ phân giải giây đều khớp. Bảng chỉ có 614 ô cho
   2 triệu item: dữ liệu 10 phút trải trên ~600 ô giây, cộng ô phút, giờ, ngày.

### 1.2 Vì sao từng sự cố không làm lệch số

| Sự cố trong test | Dòng code chịu trách nhiệm | Bài |
|---|---|---|
| Aggregator chết trước commit | Offset và delta cùng transaction: offset trong Postgres chưa tiến → đọc lại | 08 §7 |
| Rebalance (agg-3 vào) | `onPartitionsRevoked` → `commitOne`; chủ mới `assign()` đọc offset từ Postgres | 08 §8 |
| Chủ mới gặp bản retry của request đã đếm | Replay 15 phút nạp `ClientWindow` → `DUPLICATE` | 08 §3, §8 |
| Ingest chết, block mất | 202 chỉ sau ack: client không nhận 202 → retry cùng `(clientId, epoch, seq)` | 07 §8 |
| Retry cố ý 5% | `ClientWindow`: bit đã bật → `DUPLICATE` | 08 §3 |
| Client khởi động lại (epoch 2) | Epoch lớn hơn → cửa sổ mới | 08 §3 |
| Tiến trình cũ (epoch 1) gửi tiếp | Epoch nhỏ hơn → `STALE` | 08 §3 |

> **Test này từng không đủ.** Nó không có kịch bản "aggregator xác sống commit **trước** chủ mới". Bài 08 §7.1 thêm
> riêng test `ownerResyncsWhenZombieCommittedFirst` cho trường hợp đó, sau khi đọc lại code `Aggregator` và thấy chủ
> thật sẽ buông partition. Một test toàn tuyến có sự cố là cần, nhưng chưa đủ: sự cố hiếm phải có test riêng, được dựng
> chính xác thứ tự xấu nhất.

---

## 2. Thông lượng toàn tuyến trên máy lab

Dòng `100010 request duy nhat trong ... s` ở trên đo cả tuyến trong lúc có sự cố, trên **một máy 4 vCPU** chạy cùng
lúc 6 JVM Kafka, Postgres, 2 ingest node, 3 aggregator và 200 client. Mỗi client gửi tuần tự và chờ 202 (~15 ms) trước
khi gửi request kế tiếp, nên thông lượng ở đây bị chặn bởi **Little's Law** (200 request đang bay ÷ ~15–40 ms), không
bởi code. Năng lực thật của từng tầng đo riêng:

| Tầng | Số đo | Nguồn |
|---|---|---|
| HTTP + ingest, một tiến trình Netty | 14.329 request/s, 80 µs CPU/request | [bài 07 §10](07-dem-su-kien-code-ingest.md#10-bước-9--tầng-http-netty-và-giá-cpu-thật-của-một-request) |
| Ingest không HTTP, một node | ~77–100k request/s (1–3 luồng) | [bài 07 §12](07-dem-su-kien-code-ingest.md#12-bước-11--một-ingest-node-chịu-được-bao-nhiêu) |
| Aggregator, một luồng | ~5,1–5,3M request/s | [bài 08 §11](08-dem-su-kien-code-aggregator.md#11-bước-10--đo-một-luồng-aggregator-làm-được-bao-nhiêu) |
| Postgres | ~24 transaction/s ở mọi mức tải | bài 06 §2 |

---

## 3. Đo lại `IngestBench` thì thấy block nhỏ hơn thiết kế

Thiết kế ở bài 06 nói "block ≤ 2.000 bản ghi". Số đo ở bài 07 §12 lại cho **35–52 request mỗi block**. Lý do: block
được cắt khi đầy **hoặc** mỗi 5 ms, và ở ~100k request/s chia cho 12 partition, mỗi partition chỉ nhận ~40 request
trong 5 ms. Timer luôn tới trước giới hạn 2.000.

Như vậy cỡ block không do hằng số 2.000 quyết định, mà do:

```text
request mỗi block ≈ (request/s của MỘT node) × flushMs / 1000 / số partition
record Kafka/s     ≈ số node × số partition × 1000 / flushMs        (khi block chưa đầy)
```

Chỉ đọc công thức thì chưa biết điều đó đắt tới đâu. Phải đo.

---

## 4. Fan-out: số partition và flush quyết định cỡ block

📄 [`scripts/e-ingest-fanout.sh`](lab/scripts/e-ingest-fanout.sh) chạy `IngestBench` (1 node, 3 luồng, 12 giây) với
4 cấu hình, mỗi cấu hình hai vòng. Ngoài thông lượng, bench in **rác cấp phát mỗi request** của cả tiến trình
(`ThreadMXBean.getTotalThreadAllocatedBytes`) và số lần GC trong lúc đo.

### 4.1 Trước khi sửa

```text
vong partition flush  request/s req/block record/s  B/req vao  B/req dia  rac B/req   GC
  1        3    5 ms    115,249     209.6      549      32.0       14.6      1,536   7 lan, 56 ms
  1       12    5 ms    108,188      52.0    2,071      32.1       14.7      3,197   13 lan, 190 ms
  1       48    5 ms     67,595      20.1    3,402      32.0       17.1      7,849   9 lan, 158 ms
  1       12   20 ms    128,728     220.1      584      32.1       14.2      1,779   8 lan, 97 ms
  2        3    5 ms    104,484     189.9      551      32.0       14.8      1,588   7 lan, 121 ms
  2       12    5 ms     93,673      48.5    1,933      32.1       15.4      3,446   15 lan, 194 ms
  2       48    5 ms     80,254      28.8    2,784      32.1       16.2      5,960   12 lan, 226 ms
  2       12   20 ms    136,738     234.6      577      32.0       13.4      1,695   9 lan, 92 ms
```

Ba điều bất thường:

1. **Rác mỗi request tăng theo số partition**: 1,5 KB → 3,2–3,4 KB → 6–7,8 KB. Code đường nóng không đổi theo số
   partition, vậy rác phải đến từ thứ gì đó tỷ lệ với **số block**.
2. **48 partition, flush 5 ms lẽ ra là 48 × 200 = 9.600 block/giây**, nhưng chỉ đo được 2.784–3.402. Luồng flush (một
   luồng) không theo kịp nhịp 5 ms.
3. **Flush 20 ms cho thông lượng cao nhất** dù làm request chờ lâu hơn.

Tìm nguồn rác: bản đầu của `IngestNode` sau mỗi lần cắt block làm `buf.bb = newBlock()`, tức cấp phát **64 KB** (4 +
2.000 × 32 byte) mới, trong khi block vừa cắt chỉ dùng ~1,6 KB. Ở 2.071 block/giây, riêng bộ đệm mới đã là ~135 MB/s
rác, chia cho 108k request/s ≈ **1,25 KB mỗi request**, và con số này tăng đúng theo số block. Việc cấp phát và xoá 0
64 KB đó còn nằm **trong `synchronized`**, nên luồng flush giữ khoá lâu hơn và chậm hơn.

### 4.2 Sửa: dùng lại bộ đệm, chỉ chép phần đã dùng

```java
/** Goi khi dang giu khoa cua Buf. */
byte[] cut() {
    bb.putInt(0, n);
    byte[] value = Arrays.copyOf(bb.array(), bb.position());   // Kafka can mang rieng: chep phan da dung
    bb.position(4);                                            // bo dem 64 KB dung lai mai mai
    n = 0;
    return value;
}
```

### 4.3 Sau khi sửa

```text
vong partition flush  request/s req/block record/s  B/req vao  B/req dia  rac B/req   GC
  1        3    5 ms    106,141     210.6      504      32.0       15.0      1,228   5 lan, 64 ms
  1       12    5 ms    106,453      56.8    1,880      32.1       14.3      1,834   9 lan, 125 ms
  1       48    5 ms     76,157      25.1    3,109      32.2       16.9      4,155   7 lan, 82 ms
  1       12   20 ms    145,771     262.4      557      32.0       13.7      1,385   8 lan, 171 ms
  2        3    5 ms    140,606     249.8      556      32.0       14.3      1,179   6 lan, 58 ms
  2       12    5 ms    109,865      55.6    1,994      31.8       13.4      1,832   8 lan, 164 ms
  2       48    5 ms     88,103      29.8    2,946      32.2       15.0      3,697   7 lan, 138 ms
  2       12   20 ms    158,136     271.0      578      32.0       13.2      1,346   6 lan, 158 ms
```

| Cấu hình | Rác/request trước → sau | Thông lượng trước → sau (hai vòng) |
|---|---|---|
| 3 partition, 5 ms | 1,5–1,6 KB → **1,2 KB** | 104–115k → 106–141k |
| 12 partition, 5 ms | 3,2–3,4 KB → **1,8 KB** | 94–108k → 106–110k |
| 48 partition, 5 ms | 6,0–7,8 KB → **3,7–4,2 KB** | 68–80k → 76–88k |
| 12 partition, 20 ms | 1,7–1,8 KB → **1,3–1,4 KB** | 129–137k → 146–158k |

Ở 12 partition, rác giảm 1,4–1,6 KB mỗi request, đúng cỡ đã tính cho bộ đệm 64 KB.

**Phần còn lại vẫn tăng theo partition, và nằm trong Kafka.** Mỗi block thường mở một **batch** producer riêng (một
block cho mỗi partition mỗi 5 ms, `linger.ms` cũng 5). Với lz4, mỗi batch tạo một `Lz4BlockOutputStream`, và
constructor của nó cấp phát hai mảng `byte[]` cỡ block lz4 64 KB. Kiểm bằng `javap -c` trên `kafka-clients-4.1.2.jar`:
hai lệnh `newarray byte` gán cho `buffer` và `compressedBuffer`. Nhiều partition hơn thì nhiều batch hơn, nên nhiều bộ
đệm nén hơn. Phần này không sửa được trong code của mình, chỉ tránh được bằng **ít batch to** thay vì nhiều batch nhỏ.

### 4.4 Kết luận dùng được khi phỏng vấn

- **Cỡ block, không phải số partition, quyết định chi phí của ingest.** 48 partition so với 12: −25% thông lượng, ×2
  rác, cùng code.
- **Byte trên đĩa gần như không đổi** (13–17 B/request ở mọi cấu hình): producer vẫn nén cả batch. Cái đắt là **CPU và
  GC cho mỗi batch**, không phải byte.
- **Flush dài hơn thì rẻ hơn nhưng chậm hơn.** 20 ms cho +35–45% thông lượng so với 5 ms; đổi lại 202 trễ thêm tới
  15 ms.
- **Ba cách giảm fan-out ở 10M request/s**, theo thứ tự nên làm:
  1. **Ít partition:** 12–16 là đủ (phía đọc ~5M/luồng). Đừng chọn 100 partition "cho chắc".
  2. **Flush thích nghi:** cắt khi block đạt N KB **hoặc** chờ đủ T ms, với T lớn hơn khi tải cao (block tự đầy nhanh)
     và nhỏ khi tải thấp (giữ độ trễ). Có SLA độ trễ thì đặt trần cho T.
  3. **Định tuyến theo `clientId` ở load balancer** (consistent hash trên header `X-Client-Id`): mỗi node chỉ nhận
     client của một vài partition, nên block của các partition đó đầy nhanh hơn nhiều lần. Đổi lại, một client nóng
     làm nóng một node, và thêm hay bớt node làm dịch chuyển client.

---

## 5. Kế hoạch cỡ hệ thống, gom từ số đo

Giả định: 20 item/request, body ~835 B, đỉnh = 1,5 × trung bình, CPU mục tiêu 60%, node 16 vCPU. Số đo lấy từ máy lab
4 vCPU dùng chung, nên đây là **cận trên**: máy thật riêng cho từng tầng sẽ tốt hơn.

| Tầng | Công thức | 1M request/s | 10M request/s |
|---|---|---|---|
| Load balancer | request/s × ~1 KB | 8 Gbit/s | **80 Gbit/s**: nhiều LB, DNS chia |
| Ingest (Netty + giải mã + Kafka) | request/s × 80 µs ÷ 60% ÷ 16 | ~9 node | **~85 node** |
| Request đang chờ ack (Little) | request/s × ~15 ms | ~15.000 | ~150.000 (~1.800/node) |
| Registry mỗi node | 16 B × key ÷ tải 0,5 | 34 MB (1M key) | 268 MB (10M key) |
| Kafka vào (lz4) | request/s × ~16 B | ~16 MB/s | ~160 MB/s |
| Kafka ghi đĩa / giữ 24 giờ | × RF 3 | 48 MB/s / 4,1 TB | 480 MB/s / 41 TB |
| Broker | dung lượng + 3 AZ | 3 | 6 |
| Partition `count-req` | đọc dư, fan-out ít | 12 | 12–16 |
| Record Kafka/s (flush 5 ms) | node × partition × 200 | ~21.600 | ~200.000–270.000 |
| Aggregator | request/s ÷ 5M/luồng, dư ×6 để đọc bù | 2 tiến trình | 3 tiến trình × 4 luồng |
| State khử trùng | ~60 B × client | 6 MB / 100k client | 60 MB / 1M client |
| Postgres | partition × 2 commit/s | ~24 tx/s, 1 primary + 1 replica | như trái |

Nhìn bảng để trả lời "cái gì vỡ trước khi tải ×10":

1. **Tầng HTTP, ngay lập tức:** ×10 node và ×10 băng thông. Mọi µs ở đây nhân với 10M. Đây là nơi đáng tối ưu nhất:
   tự viết tokenizer trên `byte[]`, gộp response theo event loop, HTTP/2 hay gRPC để bớt chi phí mỗi request, và
   **thuyết phục SDK gộp nhiều kết quả vào một request**.
2. **Số record Kafka**, nếu để partition tăng theo số node. Giữ partition ít, flush thích nghi (§4).
3. **Không vỡ:** aggregator (dư ×6), Postgres (~24 tx/s), state khử trùng (MB).

---

## 6. Những thứ phải thêm trước khi lên production

| Việc | Vì sao | Cách làm |
|---|---|---|
| **Kẹp event time** | Đồng hồ client sai làm số rơi vào bucket năm 1970 hay năm 2099 | Ở ingest: `ts` ngoài `[now − 7 ngày, now + 5 phút]` thì không đếm theo `ts` mà vào một con đếm riêng (`ts_out_of_range`) và metric; hoặc trả 400 nếu muốn SDK sửa |
| **Backpressure** | Kafka chậm thì request chờ ack dồn lại tới hết RAM | Giới hạn số request chờ ack mỗi node; vượt thì **503 + `Retry-After`**; SDK retry có backoff + jitter |
| **Hợp đồng SDK** | Khử trùng đúng chỉ khi SDK đúng | ≤ 64 request đang bay mỗi `clientId`; retry đúng body; bỏ cuộc trước 15 phút; epoch mới mỗi lần khởi động |
| **Registry có version** | Hai node lệch registry vài trăm ms | Ghi `version` vào bản ghi nếu cần nhất quán chặt; gộp cập nhật để không dựng lại bảng 268 MB liên tục |
| **Giữ raw để đếm lại** | Đổi luật phân loại hay sửa bug thì phải tính lại quá khứ | Kafka chỉ giữ 24 giờ. Đẩy topic `count-req` ra object storage (Kafka Connect S3 sink); muốn đổi luật thì phải giữ cả **body gốc**, vì bản ghi 32 B đã mất tên key |
| **Metric và cảnh báo** | Lệch số không tự kêu | `tooOld` ≠ 0, `stale` tăng, `duplicates` tăng vọt, lag tăng đều, `fencedCommits` > 0, độ trễ commit > 1 s (bài 08 §12) |
| **Nhiều region** | Một region là một bán kính sự cố | Mỗi region một bộ ingest + Kafka + aggregator + Postgres; số toàn cầu = **cộng** các region (số đếm cộng được); client gắn với region nhà |

---

## 7. Kịch bản trình bày "1M rồi 10M" trong 6 phút

> 1. **Số:** *"1M request/s, 20 item: 20M item/s, ~8 Gbit/s vào. Tôi đo trên lab: ingest Netty tốn ~80 µs CPU mỗi
>    request, nên 1M cần ~80 core, 10M cần ~800 core. Kafka chỉ thấy 16–160 MB/s vì tôi phân loại và gộp ở ingest. Postgres
>    chỉ thấy ~24 transaction/giây ở mọi mức tải."*
> 2. **Đường ghi:** *"Ingest giải mã streaming, tra key bằng bảng băm 128 bit trong RAM, ghi bản ghi 32 byte vào block
>    theo partition `hash(clientId)`, gửi Kafka `acks=all`, trả 202 sau ack."*
> 3. **Đúng một lần:** *"Aggregator khử trùng bằng cửa sổ 64 bit mỗi client, cộng delta theo giây, commit delta cùng
>    offset trong một transaction có điều kiện. Tôi có test toàn tuyến giết aggregator, giết ingest, retry 5%, client
>    khởi động lại; số cuối khớp oracle chính xác."*
> 4. **Đọc:** *"Phân rã khoảng thành tối đa 282 + số ngày bucket. 24 giờ qua là 142 dòng."*
> 5. **Lên 10M:** *"Ingest ×10, LB ~80 Gbit/s, Kafka 6 broker theo dung lượng và AZ. Tôi **không** tăng partition theo
>    tải: đo cho thấy 48 partition chậm hơn 12 khoảng 25% vì block nhỏ đi. Aggregator vẫn dư 6 lần. Postgres không đổi."*
> 6. **Giới hạn và vận hành:** *"SDK ≤ 64 request đang bay, retry bỏ cuộc trước 15 phút; event time phải kẹp; metric
>    `tooOld` phải bằng 0. Muốn đếm lại quá khứ thì phải giữ body gốc ngoài Kafka."*

---

## 8. Drill (20 phút)

1. Chạy `PipelineTest`, rồi giải thích **từng dòng** output như §1.1 mà không nhìn bài.
2. Một node nhận 150k request/s, 16 partition, flush 5 ms: mỗi block bao nhiêu request, bao nhiêu record Kafka/s? 85
   node thì toàn cụm bao nhiêu record/s? (150.000 × 0,005 ÷ 16 ≈ 47 request; 16 × 200 = 3.200 record/s; ×85 = 272.000.)
3. Viết lại bảng §5 cho 3M request/s, 50 item/request, body 2 KB. Cái gì đổi bậc?

---

## 9. Follow-up hay gặp

<details>
<summary><b>Q1. Postgres sập 30 phút thì sao?</b></summary>

Ingest không bị ảnh hưởng: nó chỉ cần Kafka, nên client vẫn nhận 202. Aggregator không commit được, lag tăng, số trên
dashboard đứng yên ở mốc cuối. Kafka giữ 24 giờ nên không mất gì. Postgres về thì aggregator commit tiếp từ offset cũ
và đọc bù với tốc độ ~5 lần tốc độ ghi (§5), đuổi kịp 30 phút trong ~6 phút. Cần cảnh báo "lag > 1 phút" để người
trực biết số đang trễ.
</details>

<details>
<summary><b>Q2. Muốn đếm theo từng key (top key giả nhiều nhất) thì thay đổi gì?</b></summary>

Bản ghi phải mang `(keyId, outcome)` cho từng item thay vì 4 con số, nên byte vào Kafka ×20. Bảng đếm thêm chiều key,
và cardinality nổ: 1M key × 86.400 giây là quá nhiều ô. Hướng đúng: đếm chính xác 4 con số tổng như bài này, còn top-K
key thì dùng sketch (Count-Min + heap) theo phút, hoặc đẩy bản ghi chi tiết sang ClickHouse
([Katalon 04 §6.7](../../katalon-prep/katalon-system-design/04-event-counting-10k.md#67-cardinality--cái-bẫy-chính-của-bài-này)).
</details>

<details>
<summary><b>Q3. Sao không lấy mẫu (sampling) cho rẻ?</b></summary>

Đề hỏi số chính xác, và FAKE_KEY thường là dấu hiệu lỗi cấu hình hay tấn công: lấy mẫu làm mất đúng những sự kiện hiếm
cần thấy. Ở đây chi phí chính là tầng HTTP, và lấy mẫu ở server không làm giảm nó: request đã tới thì đã tốn CPU. Muốn
rẻ thật thì gộp ở client.
</details>

<details>
<summary><b>Q4. Test toàn tuyến chạy chậm và dễ chập chờn: sao vẫn đáng?</b></summary>

Vì nó là bằng chứng duy nhất cho câu "đúng khi có sự cố". Để nó ổn định: đặt sự cố theo **tiến độ** (30% request đã
xác nhận) chứ không theo thời gian; oracle tính độc lập lúc sinh dữ liệu; chờ "commit tới cuối mọi partition" thay vì
ngủ cố định. Còn trường hợp hiếm như xác sống commit trước thì viết test riêng, dựng đúng thứ tự (bài 08 §7.1).
</details>

---

## 10. Checklist

- [ ] Giải thích được từng dòng output của `PipelineTest` và cơ chế làm mỗi sự cố vẫn đúng.
- [ ] Nói được công thức cỡ block và record Kafka/s; vì sao timer tới trước giới hạn 2.000.
- [ ] Kể lại thí nghiệm fan-out: triệu chứng, nguồn rác, cách sửa, số trước/sau, phần còn lại trong Kafka (lz4 mỗi batch).
- [ ] Đọc bảng §5 và nói "cái gì vỡ trước khi ×10".
- [ ] Liệt kê 7 việc phải thêm trước production.
- [ ] Trình bày kịch bản §7 trong 6 phút.
