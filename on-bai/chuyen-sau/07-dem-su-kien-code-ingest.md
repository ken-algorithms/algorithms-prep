# Bài 07 — Đếm sự kiện: code phía ingest, từ JSON 835 byte tới bản ghi 32 byte trong Kafka

> **Dùng cho:** **Katalon**, phần "code tại chỗ" của bài fake key: phân loại item, tra key hợp lệ, chống đếm trùng khi
> retry, và đẩy vào Kafka mà không làm nghẽn. Mỗi bước có code thật, test thật, và số đo CPU/byte trên **một** request.
>
> **Thời lượng:** đọc 60 phút · lab 15 phút · drill 20 phút
>
> **Code:** [`counting/`](lab/src/main/java/com/prep/cs/counting/): `Outcome`, `Decoded`, `Hash128`, `KeyRegistry`,
> `RequestDecoder`, `RequestRecord`, `IngestNode`, `RegistryTopic`, `NettyIngestServer`, `HttpIngestServer` ·
> test: `KeyRegistryTest`, `RequestDecoderTest` · đo: [`CountingBench`](lab/src/main/java/com/prep/cs/counting/bench/CountingBench.java)
> (JMH), [`e-http-bench.sh`](lab/scripts/e-http-bench.sh), [`IngestBench`](lab/src/main/java/com/prep/cs/counting/bench/IngestBench.java),
> [`e-curl-demo.sh`](lab/scripts/e-curl-demo.sh)
>
> **Cần trước:** [bài 06](06-dem-su-kien-1m-10m-de-va-kien-truc.md) (kiến trúc, luật phân loại). **Học tiếp:**
> [bài 08](08-dem-su-kien-code-aggregator.md) (aggregator).

```bash
cd on-bai/chuyen-sau/lab
docker compose -f docker/cluster.yml up -d --wait
mvn -q test -Dtest='KeyRegistryTest,RequestDecoderTest'     # không cần Kafka
scripts/e-curl-demo.sh                                       # API thật: POST /events, GET /stats
```

---

## 0. Trọng tâm phỏng vấn

| # | Câu hỏi | Trả lời của senior |
|:-:|---|---|
| 1 | Parse JSON thế nào ở 10M request/s? | **Streaming** (token một), phân loại ngay khi đọc; không POJO, không `String` cho key. JMH: **10,1 µs** và 744 B rác mỗi request, so với 27,9 µs và 8.026 B khi đọc cả cây JSON |
| 2 | Tra key hợp lệ thế nào? | Bảng băm địa chỉ mở trên **hai mảng `long`**, fingerprint 128 bit có seed bí mật. 1M key = 33,6 MB; 1M key giả → 0 nhận nhầm |
| 3 | Sao không dùng Bloom filter cho đỡ RAM? | Bloom filter có dương tính giả: key giả bị đếm là thật. Bài này cần **số chính xác**. 128 bit cho xác suất nhầm ~10⁻³⁸, đổi lại 16 B/key |
| 4 | Gửi Kafka mỗi request một message? | Không. Gom vào **block theo partition** (≤ 2.000 bản ghi 32 B hoặc mỗi 5 ms), một ack Kafka trả 202 cho cả block |
| 5 | Partition theo gì? | `hash(clientId)`: bản gốc và bản retry của cùng client luôn vào cùng partition, nên khử trùng chỉ cần state cục bộ |
| 6 | Một request tốn bao nhiêu CPU? | Đo cả tiến trình: Netty không làm gì **24 µs**; ingest đầy đủ **80 µs** ở 1.024 kết nối, **213 µs** ở 64 kết nối (batch kém hơn) |

---

## 1. Bản đồ code

```text
HTTP body (835 B, 20 item)
  └─ NettyIngestServer ── submit(body) ──> IngestNode
                                             ├─ RequestDecoder.decode()  → Decoded {clientId, epoch, seq, ts, counts[4]}
                                             │     └─ Hash128 + KeyRegistry.contains()   (registry từ RegistryTopic)
                                             ├─ RequestRecord.write()     → 32 byte vào block của partition hash(clientId)
                                             └─ flush (đầy 2.000 / mỗi 5 ms) → KafkaProducer (acks=all, lz4)
                                                     └─ ack → CompletableFuture.complete() → HTTP 202 cho cả block
```

| File | Dòng | Việc | Test / số đo |
|---|---:|---|---|
| `Outcome`, `Decoded` | 8 + 35 | 4 kết quả; một request đã phân loại, **object dùng lại** | — |
| `Hash128` | 53 | Băm chuỗi thành 128 bit, một lần duyệt, có seed | `KeyRegistryTest` |
| `KeyRegistry` | 83 | Tập key hợp lệ: địa chỉ mở, hai mảng `long` | `KeyRegistryTest` (2) |
| `RequestDecoder` | ~115 | Parser streaming, phân loại theo luật bài 06 §1.1 | `RequestDecoderTest` (4), JMH |
| `RequestRecord` | 35 | Bản ghi 32 byte | — |
| `IngestNode` | ~175 | Block theo partition, flush, producer, 202 sau ack | `PipelineTest` (bài 09), `IngestBench` |
| `RegistryTopic` | 109 | Registry phát qua topic compacted | `PipelineTest` |
| `NettyIngestServer` | 129 | HTTP event loop, CPU/request | `e-http-bench` |

---

## 2. Bước 1 — Mô hình dữ liệu: bốn kết quả, một object dùng lại

```java
/** Bon ket qua cua mot cap key-value. Thu tu ordinal() la chi so trong mang dem int[4]. */
public enum Outcome {
    TRUE, FALSE, FAKE_KEY, MALFORMED;
}
```

```java
public final class Decoded {
    public long clientId;
    public int epoch;        // moi lan tien trinh client khoi dong lai: epoch moi, seq dem lai tu 0
    public long seq;         // so thu tu request cua client trong epoch; retry gui lai DUNG seq cu
    public long tsMillis;    // event time do client gan
    public int items;
    public final int[] counts = new int[4];   // chi so = Outcome.ordinal()

    void reset() { clientId = -1; epoch = -1; seq = -1; tsMillis = -1; items = 0; Arrays.fill(counts, 0); }
}
```

Ba quyết định nhỏ, mỗi cái đáng một câu khi phỏng vấn:

- **`int[4]` theo `ordinal()`**, không phải `Map<Outcome, Integer>`. Map thì mỗi lần `merge` có boxing và một node.
  Mảng 4 phần tử nằm trọn trong một cache line.
- **Một `Decoded` cho mỗi luồng** (`ThreadLocal` trong `IngestNode`), `reset()` trước mỗi request. Ở 10M request/s,
  mỗi object 48 byte cấp phát thêm là 480 MB rác mỗi giây.
- **`(clientId, epoch, seq)` là danh tính của request.** Không có id ngẫu nhiên cho mỗi request: retry gửi lại đúng
  ba số này, và khử trùng ở bài 08 chỉ cần 64 bit cho mỗi client.

---

## 3. Bước 2 — `Hash128`: băm 128 bit trong một lần duyệt, có seed

📄 [`Hash128.java`](lab/src/main/java/com/prep/cs/counting/Hash128.java)

```java
public void hash(char[] c, int off, int len, long[] out) {
    long h1 = seed1 ^ (len * C1), h2 = seed2 ^ (len * C2);
    int i = off, end = off + len;
    for (; i + 4 <= end; i += 4) {                       // 4 char = 64 bit moi vong
        long w = c[i] | (long) c[i + 1] << 16 | (long) c[i + 2] << 32 | (long) c[i + 3] << 48;
        h1 = Long.rotateLeft(h1 ^ (w * C1), 31) * C2 + 0x52dce729;
        h2 = Long.rotateLeft(h2 ^ (w * C2), 33) * C1 + 0x38495ab5;
    }
    long tail = 0;
    for (int s = 0; i < end; i++, s += 16) tail |= (long) c[i] << s;
    h1 = Long.rotateLeft(h1 ^ (tail * C1), 31) * C2;
    h2 = Long.rotateLeft(h2 ^ (tail * C2), 33) * C1;
    out[0] = fmix64(h1 ^ h2 * 31);                       // lane 1: chon o trong bang
    out[1] = fmix64(h2 + h1);                            // lane 2: xac nhan
}
```

- **Băm trên `char[]`, không trên `String`.** Parser Jackson đưa text dưới dạng `char[]` nằm sẵn trong buffer của nó
  (`getTextCharacters()`). Băm thẳng trên đó thì **không tạo `String`** cho 20 key của mỗi request.
- **Kết quả ghi vào `long[] out` truyền vào**, không trả về mảng mới: đường nóng gọi hàm này ~20 lần mỗi request.
- **Hai lane 64 bit:** lane 1 chọn ô trong bảng, lane 2 xác nhận. Hai key khác nhau trùng cả 128 bit có xác suất cỡ
  n / 2¹²⁸, coi như không xảy ra. Vì vậy bảng **không cần lưu chuỗi key**, chỉ 16 byte/key.
- **Seed bí mật** (`seed1`, `seed2`): không có seed, kẻ tấn công tự tính được hàng nghìn key giả rơi vào **cùng một ô**
  (HashDoS), làm chuỗi dò dài ra và mỗi lần tra tốn hàng µs. Test `differentSeedsGiveDifferentLayouts` kiểm điều này.
  Đây không phải hàm băm mật mã. Cần chống tấn công thật thì dùng SipHash, đắt hơn vài lần.
- `fmix64` là bước trộn cuối của MurmurHash3: mọi bit vào ảnh hưởng mọi bit ra.

---

## 4. Bước 3 — `KeyRegistry`: tập key hợp lệ trên hai mảng `long`

📄 [`KeyRegistry.java`](lab/src/main/java/com/prep/cs/counting/KeyRegistry.java)

```java
public static KeyRegistry of(Collection<String> keys, Hash128 hash, long version) {
    int capacity = Integer.highestOneBit(Math.max(16, (int) (keys.size() / 0.6))) << 1;  // tai <= 0.6, luy thua 2
    KeyRegistry r = new KeyRegistry(hash, capacity, version);
    long[] h = new long[2];
    for (String k : keys) {
        hash.hash(k, h);
        r.insert(h[0], h[1]);
    }
    return r;
}

/** Duong nong: (h1, h2) la Hash128 cua key, nguoi goi bam san bang mang dung lai. */
public boolean contains(long h1, long h2) {
    if (h1 == 0) h1 = 1;                                   // 0 danh dau o trong
    for (int slot = (int) h1 & mask; ; slot = (slot + 1) & mask) {
        long f = fp1[slot];
        if (f == 0) return false;                          // gap o trong: chac chan khong co
        if (f == h1 && fp2[slot] == h2) return true;
    }
}
```

Vì sao cấu trúc này, không phải `HashSet<String>`:

| | `HashSet<String>` (1M key) | `KeyRegistry` (1M key) |
|---|---|---|
| Bộ nhớ | ~100 MB+: mỗi key một `String` (header + `byte[]`) + một node `HashMap` | **33,6 MB**: 2²¹ ô × 16 B, không object nào |
| Tra một key | Băm `String` (đã cache), dò bảng, `equals()` so từng ký tự | So hai `long`, thường trúng ngay ô đầu |
| Rác mỗi lần tra | Phải có `String` key, tức tạo từ parser | 0 |
| JMH, một lần tra | 56,8 ns | **35,2 ns** (key đã băm sẵn) |
| Cập nhật | Có khoá hoặc `ConcurrentHashMap` | **Bất biến**: dựng bảng mới rồi đổi tham chiếu (copy-on-write, §9) |

- **Dò tuyến tính (linear probing)** trên mảng liền nhau: ô kế tiếp nằm cùng cache line. Hệ số tải ≤ 0,6 giữ chuỗi dò
  ngắn. Bảng ở lab có tải 0,48.
- **Bất biến sau khi dựng**, nên đọc từ hàng chục luồng event loop không cần khoá, không cần `volatile` từng ô.
- **Cỡ:** 1M key là 2²¹ ô × 16 B = 33,6 MB; 10M key là 2²⁴ ô = 268 MB. Mỗi ingest node giữ một bản.

Output thật của [`KeyRegistryTest`](lab/src/test/java/com/prep/cs/counting/KeyRegistryTest.java):

```text
[LAB] registry 1.000.000 key: 2097152 o (tai 0.48), 33.6 MB cho hai mang fp; 1.000.000 key gia -> 0 bi nhan nham
```

---

## 5. Bước 4 — `RequestDecoder`: parser streaming, phân loại ngay khi đọc

📄 [`RequestDecoder.java`](lab/src/main/java/com/prep/cs/counting/RequestDecoder.java)

Vòng ngoài đọc các trường của request. Trường lạ thì **bỏ cả cây con** (`skipChildren`), nên SDK thêm trường mới
không làm server vỡ:

```java
try (JsonParser p = factory.createParser(body, off, len)) {
    if (p.nextToken() != JsonToken.START_OBJECT) throw new BadRequest("body phai la object");
    boolean sawItems = false;
    while (p.nextToken() == JsonToken.FIELD_NAME) {
        String field = p.currentName();            // ten truong duoc Jackson intern: khong cap phat
        JsonToken t = p.nextToken();
        switch (field) {
            case "clientId" -> out.clientId = longValue(p, t, field);
            case "epoch" -> out.epoch = (int) longValue(p, t, field);
            case "seq" -> out.seq = longValue(p, t, field);
            case "ts" -> out.tsMillis = longValue(p, t, field);
            case "items" -> {
                if (t != JsonToken.START_ARRAY) throw new BadRequest("items phai la mang");
                readItems(p, registry, out);
                sawItems = true;
            }
            default -> p.skipChildren();           // truong la: bo qua ca cay con
        }
    }
    if (out.clientId < 0 || out.epoch < 0 || out.seq < 0 || out.tsMillis < 0 || !sawItems) {
        throw new BadRequest("thieu clientId/epoch/seq/ts/items");
    }
    if (out.items == 0) throw new BadRequest("items rong");
} catch (IOException e) {
    // khong tra nguyen thong diep cua Jackson: dai, nhieu dong (vo JSON cua response), lo chi tiet noi bo
    long col = e instanceof JsonProcessingException j && j.getLocation() != null ? j.getLocation().getColumnNr() : -1;
    throw new BadRequest("JSON hong o cot " + col);
}
```

Vòng trong phân loại từng item. **Key trước, value sau**: key giả thì value là gì cũng là FAKE_KEY. Thứ tự trường
trong object không quan trọng (`{"value":true,"key":"..."}` vẫn đúng):

```java
while ((item = p.nextToken()) != JsonToken.END_ARRAY) {
    if (item == null) throw new BadRequest("JSON bi cat giua chung");
    if (++out.items > MAX_ITEMS) throw new BadRequest("qua " + MAX_ITEMS + " item");
    if (item != JsonToken.START_OBJECT) {     // phan tu khong phai object: van dem, la MALFORMED
        p.skipChildren();
        out.counts[Outcome.MALFORMED.ordinal()]++;
        continue;
    }
    int key = 0;                 // 0 thieu, 1 hop le, 2 gia, 3 khong phai chuoi
    JsonToken value = null;
    while (p.nextToken() == JsonToken.FIELD_NAME) {
        String f = p.currentName();
        JsonToken t = p.nextToken();
        if (f.equals("key")) {
            if (t == JsonToken.VALUE_STRING) {
                // char[] nam san trong buffer cua parser: bam thang, khong tao String
                registry.hash().hash(p.getTextCharacters(), p.getTextOffset(), p.getTextLength(), h);
                key = registry.contains(h[0], h[1]) ? 1 : 2;
            } else {
                key = 3;
                p.skipChildren();
            }
        } else if (f.equals("value")) {
            value = t;
            p.skipChildren();
        } else {
            p.skipChildren();
        }
    }
    Outcome o;
    if (key == 0 || key == 3) o = Outcome.MALFORMED;
    else if (key == 2) o = Outcome.FAKE_KEY;
    else if (value == JsonToken.VALUE_TRUE) o = Outcome.TRUE;
    else if (value == JsonToken.VALUE_FALSE) o = Outcome.FALSE;
    else o = Outcome.MALFORMED;
    out.counts[o.ordinal()]++;
}
```

Bốn chi tiết làm nó rẻ và an toàn:

1. **`BadRequest` không chụp stack trace** (`super(msg, null, false, false)`). Request hỏng là chuyện thường. Chụp
   stack trace đắt cỡ µs, và [Track P04](../../java-system-design/01-java-code-cham-duoi-tai-cao.md) đo exception cho
   luồng nghiệp vụ chậm hơn ~100 lần so với kiểm tra thường.
2. **`MAX_ITEMS = 1.000`**: không giới hạn thì một request 50 MB với một triệu item giữ một luồng hàng giây. Giới hạn
   này cũng là lý do mỗi con đếm vừa `short` trong bản ghi 32 byte (§7).
3. **Không trả nguyên thông điệp lỗi của Jackson.** Bản đầu trả `e.getMessage()` và curl demo lộ ra lỗi: thông điệp dài
   nhiều dòng, làm JSON của response 400 không hợp lệ, lại lộ chi tiết nội bộ. Giờ chỉ còn `"JSON hong o cot 59"`.
4. **Không kiểm `ts` so với đồng hồ server.** Đó là quyết định của tầng trên (bài 09 §6). Decoder chỉ kiểm hình dạng.

Output thật của [`RequestDecoderTest`](lab/src/test/java/com/prep/cs/counting/RequestDecoderTest.java). 11 item
cố ý bẫy (value `"yes"`, `null`, `1`, thiếu key, key là số, phần tử không phải object, value đứng trước key), rồi 20.000
request ngẫu nhiên so với một **oracle** tính độc lập lúc sinh dữ liệu:

```text
[LAB] client=42 epoch=7 seq=17 ts=1792390123456 TRUE=2 FALSE=1 FAKE_KEY=2 MALFORMED=6
[LAB] 20.000 request x 20 item: oracle [179803, 120158, 59892, 40147], decoder [179803, 120158, 59892, 40147]
```

---

## 6. Bước 5 — Đo bằng JMH: mỗi lựa chọn đáng bao nhiêu core

📄 [`CountingBench.java`](lab/src/main/java/com/prep/cs/counting/bench/CountingBench.java),
output [`results/e-countingbench.txt`](lab/results/e-countingbench.txt). Body thật ~1 KB, 20 item, registry 1M key;
đo lúc đã tắt cụm Kafka cho máy rảnh.

| Benchmark | Thời gian / lần | Rác / lần | Ở 10M request/s |
|---|---:|---:|---:|
| `decodeStreaming` (bài này) | **10,1 µs** ± 2,4 | **744 B** | 101 core · 7,4 GB/s rác |
| `decodeTreeAndHashSet` (`readTree` + `HashSet<String>`) | 27,9 µs ± 15,3 | 8.026 B | 279 core · 80 GB/s rác |
| `lookupRegistryPreHashed` | 35,2 ns ± 16,5 | 0 | ×20 item: 7 core |
| `lookupHashSetString` | 56,8 ns ± 9,4 | 0 | ×20 item: 11 core |

- **Streaming nhanh gấp ~2,8 lần và ít rác hơn ~11 lần.** Ở 10M request/s, đó là 178 core và ~73 GB/s rác mỗi giây,
  tức khác biệt giữa "chạy được" và "GC chạy liên tục".
- **744 B rác còn lại** chủ yếu là object parser của Jackson tạo cho mỗi request. Muốn gần 0 thì phải tự viết tokenizer
  trên `byte[]`. Đáng làm ở 10M, chưa đáng ở 1M.
- **Sai số lớn (±)** vì máy lab 4 vCPU dùng chung. Đọc tỷ lệ giữa các dòng, đừng đọc số tuyệt đối.

---

## 7. Bước 6 — `RequestRecord`: 32 byte cho một request

📄 [`RequestRecord.java`](lab/src/main/java/com/prep/cs/counting/RequestRecord.java)

```java
/**
 *   clientId 8 | epoch 4 | seq 8 | ts giay 4 | TRUE 2 | FALSE 2 | FAKE_KEY 2 | MALFORMED 2
 * Mot block (mot record Kafka) = so luong (4 byte) + n ban ghi.
 */
public static void write(ByteBuffer b, Decoded d) {
    b.putLong(d.clientId).putInt(d.epoch).putLong(d.seq).putInt((int) (d.tsMillis / 1000));
    for (int c : d.counts) b.putShort((short) c);   // <= 1000 item moi request: vua short
}
```

| Trường | Byte | Vì sao giữ |
|---|---:|---|
| `clientId`, `epoch`, `seq` | 20 | Danh tính của request: aggregator khử trùng bằng chúng |
| `ts` (giây) | 4 | Bucket nhỏ nhất là giây; bỏ phần ms. `int` không dấu đủ tới năm 2106 |
| 4 con đếm `short` | 8 | ≤ 1.000 item/request nên vừa 16 bit |
| **Bỏ đi** | — | Tên key, value, JSON. Phần còn lại không cần cho câu hỏi của đề |

Từ 835 byte JSON còn 32 byte (**26 lần**), và sau lz4 còn ~14–16 byte/request trên đĩa (đo ở §12). Nếu sau này cần
đếm **theo từng key**, đây là chỗ phải đổi: thêm `(keyId, outcome)` vào bản ghi, và kích thước tăng theo số item.

---

## 8. Bước 7 — `IngestNode`: block theo partition, 202 sau khi Kafka xác nhận

📄 [`IngestNode.java`](lab/src/main/java/com/prep/cs/counting/IngestNode.java)

### 8.1 Producer: bền trước, nhanh sau

```java
p.put(ProducerConfig.ACKS_CONFIG, "all");                     // 202 = da nam tren >= min.insync.replicas broker
p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);        // retry cua producer khong sinh block trung (bai 02)
p.put(ProducerConfig.LINGER_MS_CONFIG, 5);
p.put(ProducerConfig.BATCH_SIZE_CONFIG, 256 * 1024);
p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");         // 32 B/request -> ~14-16 B tren dia
p.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 4 * 1024 * 1024);
```

### 8.2 Partition theo client

```java
public static int partitionOf(long clientId, int partitions) {
    return (int) ((Hash128.fmix64(clientId) >>> 1) % partitions);
}
```

- `fmix64` trộn trước: `clientId` thường là số tăng dần, chia lấy dư thẳng thì dễ lệch.
- `>>> 1` cho ra số không âm (`%` của số âm trong Java là âm).
- Ingest **tự chọn partition** (`new ProducerRecord<>(topic, part, null, value)`), không để partitioner của Kafka
  chọn, vì một record là cả block nhiều client và không có key chung.

### 8.3 Block theo partition, bộ đệm dùng lại

```java
private static final class Buf {
    final ByteBuffer bb;                                   // 4 + 2.000 x 32 B = 64 KB, cap phat MOT lan
    List<CompletableFuture<Void>> waiters = new ArrayList<>();
    int n;

    /** Goi khi dang giu khoa cua Buf. */
    byte[] cut() {
        bb.putInt(0, n);                                   // so ban ghi o dau block
        byte[] value = Arrays.copyOf(bb.array(), bb.position());  // mang vua kich thuoc cho Kafka
        bb.position(4);                                    // dung lai bo dem
        n = 0;
        return value;
    }
}

public CompletableFuture<Void> submit(byte[] body) {
    if (crashed) throw new IllegalStateException("connection refused"); // node chet: client biet ngay
    Decoded d = decoded.get();
    decoder.get().decode(body, 0, body.length, registry.get(), d);       // BadRequest -> HTTP 400 ngay
    CompletableFuture<Void> done = new CompletableFuture<>();
    int part = partitionOf(d.clientId, partitions);
    Buf buf = bufs[part];
    byte[] full = null;
    List<CompletableFuture<Void>> fullWaiters = null;
    synchronized (buf) {                                   // khoa THEO PARTITION: 12 khoa, khong mot khoa chung
        RequestRecord.write(buf.bb, d);
        buf.waiters.add(done);
        if (++buf.n == blockRecords) {                     // day block: cat ra trong khoa, gui ngoai khoa
            full = buf.cut();
            fullWaiters = buf.waiters;
            buf.waiters = new ArrayList<>();
        }
    }
    accepted.incrementAndGet();
    if (full != null) send(part, full, fullWaiters);
    return done;
}
```

Một luồng riêng cắt mọi block đang mở mỗi `flushEveryMs` (5 ms), để request không phải chờ tới khi đủ 2.000:

```java
private void flushAll() {
    for (int part = 0; part < partitions; part++) {
        Buf buf = bufs[part];
        byte[] value;
        List<CompletableFuture<Void>> waiters;
        synchronized (buf) {
            if (buf.n == 0) continue;
            value = buf.cut();
            waiters = buf.waiters;
            buf.waiters = new ArrayList<>();
        }
        send(part, value, waiters);
    }
}
```

> **Bản đầu sai ở đây, và chỉ đo mới thấy.** Bản đầu **cấp phát bộ đệm 64 KB mới cho mỗi block**. Ở tải thật, block
> được cắt bởi timer khi mới có vài chục bản ghi (~1–2 KB), nên mỗi giây có hàng nghìn bộ đệm 64 KB gần như trống thành
> rác. Thí nghiệm fan-out ở [bài 09 §4](09-dem-su-kien-chay-that-va-scale.md#4-fan-out-số-partition-và-flush-quyết-định-cỡ-block)
> đo ra rác mỗi request tăng theo số partition; sửa thành bộ đệm dùng lại thì rác giảm và thông lượng tăng.

### 8.4 Một ack Kafka trả lời cả block

```java
private void send(int part, byte[] value, List<CompletableFuture<Void>> waiters) {
    if (crashed) return;                        // chet: block trong RAM mat, client se het gio va retry
    blocksSent.incrementAndGet();
    bytesSent.addAndGet(value.length);
    producer.send(new ProducerRecord<>(topic, part, null, value), (md, e) -> {
        for (CompletableFuture<Void> f : waiters) {
            if (e == null) f.complete(null);
            else f.completeExceptionally(e);    // HTTP 503: client retry cung (clientId, epoch, seq)
        }
    });
}
```

Những điểm interviewer hay đào:

- **Ingest chết thì sao?** Block đang mở nằm trong RAM và mất. Nhưng chưa request nào trong đó nhận 202, nên client
  hết giờ và retry. Không có "mất im lặng". `crash()` giả lập đúng chuyện này cho test ở bài 09.
- **Thứ tự hai block của cùng partition có thể đảo.** Luồng flush cắt block A và nhả khoá; luồng khác cắt block B rồi
  gửi B trước A. Không sao: với một client, đó chỉ là "request tới trễ", và cửa sổ 64 bit ở bài 08 nhận request tới trễ
  mà chưa thấy.
- **`producer.send()` có thể chặn** khi `buffer.memory` đầy (bài 04 §1). Block đầy được gửi ngay trên luồng gọi
  `submit`, tức luồng event loop của Netty. Production cần giới hạn số request đang chờ ack của mỗi node và trả **503
  sớm** khi vượt (`IngestBench` dùng `Semaphore` 20.000 cho mỗi luồng), thay vì để event loop đứng.

---

## 9. Bước 8 — `RegistryTopic`: phát registry qua topic compacted

📄 [`RegistryTopic.java`](lab/src/main/java/com/prep/cs/counting/RegistryTopic.java)

Mỗi ingest node phải có **cùng** tập key hợp lệ, cập nhật khi có key mới hay key bị gỡ. Dùng lại đúng compaction của
bài 02 bước 12: key Kafka = tên key, value `"1"` = hợp lệ, value `null` (tombstone) = gỡ.

```java
public KeyRegistry loadAndFollow() {
    consumer = new KafkaConsumer<>(p);                  // khong group: moi node doc TOAN BO topic
    TopicPartition tp = new TopicPartition(topic, 0);
    consumer.assign(List.of(tp));
    consumer.seekToBeginning(List.of(tp));
    long end = consumer.endOffsets(List.of(tp)).get(tp);
    while (consumer.position(tp) < end) apply(consumer.poll(Duration.ofMillis(200)));   // doc toi "bay gio"
    current.set(KeyRegistry.of(keys, hash, end));
    running = true;
    follower = new Thread(() -> {
        while (running) {
            var batch = consumer.poll(Duration.ofMillis(200));
            if (!batch.isEmpty()) {
                apply(batch);
                current.set(KeyRegistry.of(keys, hash, consumer.position(tp))); // copy-on-write, doi tham chieu
            }
        }
        consumer.close();
    }, "registry-follower");
    follower.start();
    return current.get();
}

private void apply(Iterable<ConsumerRecord<String, String>> batch) {
    for (ConsumerRecord<String, String> r : batch) {
        if (r.value() == null) keys.remove(r.key());       // tombstone: go key
        else keys.add(r.key());
    }
}
```

- **Không dùng consumer group** (`assign` thay cho `subscribe`): mỗi node phải đọc **cả** topic, không chia nhau.
- **Đọc tới end offset lúc khởi động rồi mới nhận traffic.** Nhận traffic khi registry còn thiếu là đếm key thật thành
  FAKE_KEY. Health check của node nên chỉ báo "sẵn sàng" sau bước này.
- **Copy-on-write:** đường nóng đọc `registry.get()` (một `AtomicReference`), không khoá. Đổi registry chỉ là một phép
  gán tham chiếu.
- **Giá phải nói ra:** mỗi lô cập nhật dựng lại **cả** bảng. Với 10M key (268 MB), mỗi lần dựng là vài giây CPU và
  268 MB rác. Production nên gộp cập nhật (dựng tối đa mỗi N giây), hoặc giữ bảng gốc lớn cộng một tập nhỏ các thay đổi
  gần đây.
- **Các node có thể lệch nhau vài trăm ms** khi một key vừa được thêm. Trong khoảng đó, cùng một key có thể là TRUE ở
  node này và FAKE_KEY ở node kia. Chấp nhận được cho dashboard. Nếu không chấp nhận được, ghi `version` của registry
  vào bản ghi và chỉ dùng key sau khi mọi node đã báo đạt version đó.

Output thật trong `PipelineTest`: topic có 10.001 key, rồi một tombstone gỡ `legacy_removed`:

```text
[LAB] registry tu topic compacted: 10000 key (legacy_removed: false)
```

---

## 10. Bước 9 — Tầng HTTP: Netty, và giá CPU thật của một request

📄 [`NettyIngestServer.java`](lab/src/main/java/com/prep/cs/counting/NettyIngestServer.java)

```java
new ServerBootstrap().group(boss, workers).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 4_096)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new HttpServerCodec(), new HttpObjectAggregator(64 * 1024),   // body > 64 KB: 413
                        new SimpleChannelInboundHandler<FullHttpRequest>() {
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
                                boolean keepAlive = HttpUtil.isKeepAlive(req);
                                byte[] body = ByteBufUtil.getBytes(req.content());   // ~835 byte: chep re
                                try {
                                    ingest.submit(body).whenComplete((v, e) -> respond(ctx,
                                            e == null ? HttpResponseStatus.ACCEPTED : HttpResponseStatus.SERVICE_UNAVAILABLE,
                                            "", keepAlive));
                                } catch (RequestDecoder.BadRequest e) {
                                    respond(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\":\"" + e.getMessage() + "\"}", keepAlive);
                                }
                            }
                        });
            }
        }).bind(port).sync();
```

Giải mã và phân loại chạy **ngay trên event loop** (vài µs). Không luồng nào ngồi chờ Kafka: response được ghi từ
callback của producer, và `ctx.writeAndFlush` tự chuyển việc ghi về đúng event loop của kết nối.

**Đo CPU trên request** ([`e-http-bench.sh`](lab/scripts/e-http-bench.sh)): server tự báo CPU của **cả tiến trình**
(`getProcessCpuTime`, kernel lấy từ `/proc/self/stat`), chia cho số request. Tải sinh bằng
[`RawHttpLoad`](lab/src/main/java/com/prep/cs/counting/bench/RawHttpLoad.java): socket thô, keep-alive, header + body
trong **một** lần `write` (tránh bẫy Nagle + delayed ACK 40 ms).

```text
JDK HttpServer, noop             64 ket noi, 15.0 s: 317,845 x 202, 0 loi -> 21,172 request/s; CPU server 57.7 us/request
Netty 2 event loop, noop         64 ket noi, 15.0 s: 484,313 x 202, 0 loi -> 32,261 request/s; CPU server 24.1 us/request (trong do kernel 12.6 us)
Netty, ingest, 64 ket noi        64 ket noi, 15.0 s:  58,752 x 202, 0 loi ->  3,910 request/s; CPU server 213.3 us/request (trong do kernel 34.2 us)
Netty, ingest, 1024 ket noi    1024 ket noi, 15.1 s: 215,742 x 202, 0 loi -> 14,329 request/s; CPU server 80.4 us/request (trong do kernel 22.4 us)
```

Đọc như một senior:

1. **Chỉ riêng HTTP đã 24 µs với Netty, một nửa nằm trong kernel** (syscall, TCP). JDK `HttpServer` với virtual thread
   tốn 58 µs. Ở 10M request/s, khác biệt đó là 340 core.
2. **Ingest đầy đủ: 80 µs ở 1.024 kết nối.** Khoảng 24 µs HTTP, ~10 µs giải mã (JMH), phần còn lại là producer Kafka,
   `CompletableFuture`, việc chuyển response từ luồng callback về event loop, và GC. Đây là con số dùng để tính cỡ ở bài 06.
3. **Cùng code, 64 kết nối thì 213 µs/request.** 64 kết nối, mỗi request chờ ~16 ms cho flush + `acks=all`, nên theo
   Little's Law chỉ có 64 / 0,016 ≈ 3.900 request/s. Block mỗi 5 ms chỉ còn vài request, và chi phí cố định của mỗi
   block và mỗi batch Kafka chia cho ít request hơn. **Hệ thống này rẻ hơn khi tải cao.**
4. **Hệ quả cho production:** vì mỗi request chờ ~15 ms, ở 10M request/s toàn cụm có ~150.000 request đang chờ (Little's
   Law), tức ~1.800 mỗi node trên 85 node. Phải là event loop (hoặc virtual thread), không thể là một luồng nền tảng cho
   mỗi kết nối.

> **Số đo phụ thuộc trạng thái cụm.** Cùng code và cùng máy, lúc cụm Kafka còn 765 partition rác từ các lần đo trước,
> ingest chỉ đạt 6.364 request/s ở 140 µs/request. Dọn còn 136 partition thì lên 14.329 request/s ở 80 µs/request.
> Partition không miễn phí, kể cả khi không ai ghi vào (bài 05 Q2).

---

## 11. Bước 10 — Chạy thử bằng curl

📄 [`scripts/e-curl-demo.sh`](lab/scripts/e-curl-demo.sh) chạy [`HttpIngestServer`](lab/src/main/java/com/prep/cs/counting/HttpIngestServer.java)
(ingest + aggregator + `/stats` trong một tiến trình), rồi gửi 7 request bằng curl:

```bash
curl -s -H 'Content-Type: application/json' localhost:8080/events --data \
 '{"clientId":1792404783,"epoch":1,"seq":0,"ts":1792404783046,"items":[{"key":"feature_1","value":true},
   {"key":"feature_2","value":false},{"key":"hack_me","value":true},{"key":"feature_3","value":"yes"}]}'
curl -s 'localhost:8080/stats?from=2026-10-19T10:12:58Z&to=2026-10-19T10:13:08Z'
```

```text
--- moi request: 4 item = 1 TRUE, 1 FALSE, 1 FAKE_KEY (hack_me), 1 MALFORMED (value "yes")
1. epoch 1, seq 0                            202  0.365289s
2. GUI LAI dung body do (mat response)       202  0.039426s
3. epoch 1, seq 1                            202  0.035499s
4. client khoi dong lai: epoch 2, seq 0      202  0.022323s
5. tien trinh cu (epoch 1) van gui seq 2     202  0.027336s
6. JSON hong                                 400  0.005834s  {"error":"JSON hong o cot 59"}
7. thieu clientId                            400  0.002756s  {"error":"thieu clientId/epoch/seq/ts/items"}
--- 7 lan POST: 5 lan 202, 2 lan 400. Request duy nhat: (1,0) (1,1) (2,0) = 3 -> ky vong moi loai = 3
GET /stats?from=2026-10-19T10:12:58Z&to=2026-10-19T10:13:08Z
{"trueCount":3,"falseCount":3,"fakeKey":3,"malformed":3,"successRate":0.5000,"bucketsRead":1}
```

- **Request 2** (gửi lại đúng body) nhận 202 nhưng **không được đếm**. Ingest không khử trùng, nó chỉ chuyển tiếp;
  aggregator thấy `(clientId, 1, 0)` lần thứ hai thì bỏ.
- **Request 5** đến từ tiến trình cũ (epoch 1) sau khi epoch 2 đã xuất hiện: `STALE`, bỏ. Thiết kế cố ý chọn như vậy:
  hai tiến trình cùng `clientId` là cấu hình sai, và bỏ là an toàn hơn đếm.
- **Request 1 mất 365 ms** (khởi tạo kết nối và metadata của producer). Các request sau 22–39 ms: đó là giá của "202
  sau khi Kafka xác nhận" trên máy lab đang tải.
- **`bucketsRead: 1`:** 10 giây nhưng dữ liệu chỉ nằm trong một ô giây. Bài 08 giải thích phân rã khoảng.

---

## 12. Bước 11 — Một ingest node chịu được bao nhiêu

📄 [`IngestBench.java`](lab/src/main/java/com/prep/cs/counting/bench/IngestBench.java): không qua HTTP, T luồng gọi
`submit()` liên tục (tối đa 20.000 request chờ ack mỗi luồng), đo số request **được Kafka xác nhận** mỗi giây:

```text
topic ingest-bench-84992, 12 partition, flush 5 ms; body trung binh 835 byte, 20 item; 1 luong submit
TONG: 77,273 request/s (1,545,463 item/s), loi 0; truoc nen 32.1 byte/request vao Kafka (body JSON 835 byte) -> 26 lan nho hon
block: 2,197 record Kafka/s, trung binh 34.8 request/block (toi da 2.000)
cap phat: 2,522 byte/request (ca tien trinh); GC: 10 lan, 113 ms trong 10 s
Tren dia (lz4, mot ban sao): 15,028,707 byte cho 889,534 request = 16.9 byte/request
topic ingest-bench-6654, 12 partition, flush 5 ms; body trung binh 835 byte, 20 item; 3 luong submit
  giay  1:   61,657 request duoc xac nhan
  giay  2:   62,660 request duoc xac nhan
  giay  3:   86,141 request duoc xac nhan
  giay  4:   97,975 request duoc xac nhan
  giay  5:   84,678 request duoc xac nhan
  giay  6:   78,668 request duoc xac nhan
  giay  7:  132,036 request duoc xac nhan
  giay  8:  130,100 request duoc xac nhan
  giay  9:  134,137 request duoc xac nhan
  giay 10:  140,520 request duoc xac nhan
TONG: 100,506 request/s (2,010,112 item/s), loi 0; truoc nen 32.1 byte/request vao Kafka (body JSON 835 byte) -> 26 lan nho hon
block: 1,928 record Kafka/s, trung binh 52.1 request/block (toi da 2.000)
cap phat: 2,126 byte/request (ca tien trinh); GC: 7 lan, 77 ms trong 10 s
Tren dia (lz4, mot ban sao): 20,097,308 byte cho 1,281,932 request = 15.7 byte/request
```

- **Bỏ tầng HTTP, một node 4 vCPU (dùng chung với cả cụm Kafka) xác nhận 100.506 request/s**, tức
  2,0 triệu item/s. Với HTTP thì 14.329 request/s (§10): **tầng HTTP mới là phần đắt**, đúng như bảng ở bài 06.
- **32,1 byte/request trước nén:** 32 byte bản ghi cộng 4 byte đếm của block chia cho ~35–52 request mỗi block.
  Block nhỏ vì timer 5 ms tới trước khi đủ 2.000 bản ghi; bài 09 §4 đo xem chuyện này đắt tới đâu.
- **Trên đĩa sau lz4: 15,7–16,9 byte/request**, tức 835 byte JSON nhỏ đi ~50 lần trước khi chạm Kafka.

---

## 13. Drill (20 phút): viết tay đường nóng

Không IDE, viết lại:

1. `KeyRegistry.contains(h1, h2)` và `insert`: địa chỉ mở, dò tuyến tính, 0 là ô trống.
2. Vòng phân loại một item của `RequestDecoder` (key trước, value sau), cho đúng 6 trường hợp ở bài 06 §1.1.
3. `IngestNode.submit()` + `flushAll()`: khoá theo partition, cắt block trong khoá, gửi ngoài khoá, 202 khi ack.

Tiêu chí chấm:

- [ ] Không tạo `String` cho key; không `Map<Outcome,Integer>`; không exception có stack trace cho request hỏng.
- [ ] Gặp ô trống thì trả `false` ngay; nói được vì sao hệ số tải ≤ 0,6.
- [ ] `producer.send` nằm **ngoài** `synchronized`; giải thích vì sao.
- [ ] Nói được 3 thứ ingest **không** làm: không khử trùng, không chờ registry mới, không trả 202 trước ack.

---

## 14. Follow-up hay gặp

<details>
<summary><b>Q1. Có cần ingest khử trùng luôn không, cho đỡ việc aggregator?</b></summary>

Không làm được cho đúng. Bản retry thường tới **một node khác** (node cũ chết hoặc load balancer chia vòng tròn). Muốn
khử trùng ở ingest thì mọi node phải chia sẻ state, tức một kho dùng chung với 10M lần tra mỗi giây. Đẩy về aggregator,
nơi partition theo `clientId` đã gom bản gốc và bản retry về **cùng một luồng**, thì state là cục bộ và rẻ.
</details>

<details>
<summary><b>Q2. Registry 10M key, 85 node: mỗi node 268 MB có phí không?</b></summary>

85 × 268 MB ≈ 23 GB RAM cho cả đội ingest, nhỏ so với 85 node × 32 GB. Đổi lại mỗi lần tra là 35 ns trong RAM, không
mạng. Một dịch vụ registry dùng chung sẽ tốn 200M lần gọi mạng mỗi giây. Nếu RAM thật sự chật, có thể dùng fingerprint
64 bit (8 B/key, xác suất nhầm ~n/2⁶⁴ ≈ 5·10⁻¹³ cho mỗi lần tra), hoặc chia registry theo tenant.
</details>

<details>
<summary><b>Q3. Sao không gửi mỗi request một record Kafka, để producer tự batch?</b></summary>

Producer batch được byte, nhưng mỗi record vẫn có vỏ (~10 B varint + header), một lần chạy partitioner, một callback.
10M callback mỗi giây là 10M lần hoàn thành `Future` trên luồng sender. Block 2.000 bản ghi biến 2.000 callback thành
một. Kafka thấy vài nghìn record lớn thay vì 10 triệu record nhỏ.
</details>

<details>
<summary><b>Q4. Body lớn bất thường hay client gửi dồn dập: chặn ở đâu?</b></summary>

Từng lớp, lớp nào cũng có giới hạn: load balancer (rate limit theo client, [gd1 bài 03](../gd1-nen-tang/03-rate-limiter.md));
Netty `HttpObjectAggregator(64 KB)` trả 413; decoder `MAX_ITEMS = 1.000`; giới hạn số request chờ ack của mỗi node, vượt
thì 503 kèm `Retry-After`. Client retry phải có backoff + jitter, nếu không 503 sinh ra bão retry.
</details>

---

## 15. Checklist

- [ ] Viết được `Hash128` + `KeyRegistry` và giải thích seed, 128 bit, tải 0,6, copy-on-write.
- [ ] Viết được vòng phân loại item; đọc được output 11 item bẫy.
- [ ] Đọc bảng JMH: streaming ×2,8 nhanh hơn, ×11 ít rác hơn; mỗi µs ở 10M = 10 core.
- [ ] Giải thích bản ghi 32 byte: giữ gì, bỏ gì, vì sao `short`.
- [ ] Giải thích `IngestNode`: partition theo client, khoá theo partition, cắt trong khoá gửi ngoài khoá, 202 sau ack.
- [ ] Đọc bảng HTTP: 24 µs HTTP, 80 µs ingest; vì sao 64 kết nối đắt hơn 1.024 kết nối.
- [ ] Chạy được `e-curl-demo.sh` và giải thích từng dòng output.
