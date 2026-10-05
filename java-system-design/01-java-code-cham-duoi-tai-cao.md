# Track P — Code Java chậm dưới tải cao: kiến trúc tốt cũng không cứu được

> **Track mới của lộ trình v2.** 20 đoạn code Java phổ biến làm hệ thống chậm hoặc sập khi tải lên,
> dù LB, autoscale, cache, replica đều đúng. Mỗi đoạn có: code xấu, code sửa, **số đo thật**, cách
> quy ra tài nguyên ở mức QPS thật, và metric để bắt nó trên production.
>
> Cha: [00 — Lộ trình 6 tháng](00-lo-trinh-6-thang.md) · Track song song: [02 — Vẽ hệ thống 100k → 10M](02-ve-he-thong-100k-1m-10m.md)
> · Code chạy được: [perf-lab/](perf-lab/) · Số đo thô: [results/](perf-lab/results/)

---

## 0. Đọc file này trong 60 giây

Đo trên máy lab (Linux 4 vCPU, **JDK 21.0.11**), mỗi demo chạy 2 lần, chênh nhau dưới 5% trừ chỗ có ghi:

| Demo | Bản xấu | Bản sửa | Vì sao thêm pod **không** cứu được |
|---|---|---|---|
| **P09** pool 10 connection, gọi HTTP 50 ms **bên trong** `@Transactional` | **163 req/s**, p99 **1,23 s** | **930 req/s**, p99 **0,26 s** | Trần nằm ở pool DB, không ở CPU |
| **P10** đối tác chậm 2 s, gọi **không timeout** | `/balance` (không liên quan) p99 **3,1 s** | p99 **5 ms** | Thread kẹt chờ, CPU vẫn rảnh, autoscale theo CPU không kích hoạt |
| **P12** 1.000 virtual thread, `synchronized` quanh I/O 20 ms | **5,0 s** | **23 ms** | Carrier thread bị ghim; chỉ có 4 cái |
| **P13** quá tải 1,5×, hàng đợi **vô hạn** | p99 **2,05 s**, chỉ 576/1.200 request kịp trong 1 s | p99 **0,22 s**, 833 kịp, 367 bị từ chối **ngay** | Hàng đợi dài ra thì mọi request cùng chậm |
| **P15** trang 100 đơn hàng, **N+1** | **101 query**, 116 ms | **2 query**, 9 ms | Thêm pod thì DB nhận thêm query |
| **P18** cache **không giới hạn**, heap 128 MB | **OOM** sau ~114k request | 1 triệu request, heap ổn định | Mỗi pod tự leak riêng |
| **P11** bộ đếm `synchronized`, 4 thread | **8,9** ops/µs (tệ hơn 1 thread: 24,7) | **218** ops/µs | Thêm core làm tệ hơn |

**Ba bài học, theo thứ tự quan trọng:**

1. **Thứ làm sập hệ thống là code *giữ* tài nguyên khan hiếm** (connection, thread, carrier, hàng
   đợi), không phải code *tốn* CPU. Little's Law: `số đang bận = throughput × thời gian giữ`. Giữ lâu
   gấp 6 thì trần throughput giảm 6 lần, và thêm pod không đổi được trần đó (nhóm 2).
2. **Lãng phí CPU mỗi lần gọi thường chỉ vài µs.** Đừng tối ưu mù: ở 10k RPS, `new ObjectMapper()`
   mỗi request chỉ tốn thêm 0,14 core. Nó thành vấn đề thật khi nằm trong **vòng lặp theo kích thước
   dữ liệu**: nối chuỗi 10.000 dòng mất **0,64 s và 2,2 GB rác** cho một request (nhóm 1).
3. **Round trip là thuế cố định.** Mỗi query tốn ~1 ms dù chỉ lấy 1 dòng. N+1 biến 200 req/s thành
   **20.200 query/s** vào DB (nhóm 3).

---

## 1. Ba cơ chế biến code xấu thành sự cố hệ thống

| Cơ chế | Công thức | Tài nguyên bị đánh | Thêm pod có cứu không? | Nhóm |
|---|---|---|---|:---:|
| **Nhân với QPS** | `CPU/request × RPS = số core`; `byte rác/request × RPS = MB/s cho GC` | CPU, GC | Có, nhưng tốn tiền tuyến tính | 1 |
| **Little's Law** | `L = λ × W`: số slot đang bận = throughput × thời gian giữ slot | Connection pool, thread pool, carrier thread, lock, hàng đợi | **Không**, nếu tài nguyên là chung (DB pool tổng, lock, đối tác) | 2 |
| **Round trip** | `latency = số_lần_đi_mạng × RTT + xử lý`; `tải DB = query/request × RPS` | Latency nối tiếp, CPU của DB | **Không**, thêm pod thì thêm query vào cùng một DB | 3 |
| **Heap sống** | Heap sống to → GC thế hệ già chạy dài và dày → p99 xấu → OOM | Bộ nhớ, thời gian dừng GC | Không, mỗi pod tự hỏng riêng | 4 |

**Áp Little's Law vào P09 để thấy công thức hoạt động:** pool 10 connection, mỗi request giữ
connection 60 ms (5 + 50 + 5) → trần = `10 ÷ 0,060 = 166 req/s`. Đo được **162,7 req/s**. Sửa để chỉ
giữ 10 ms → trần = `10 ÷ 0,010 = 1.000 req/s`. Đo được **928–935 req/s**. Công thức đoán trước kết
quả với sai số dưới 7%, và đó chính là thứ bạn nói trên bảng trắng.

---

## 2. Nhóm 1 — Lãng phí CPU và rác mỗi request (P01–P07)

Đo bằng JMH (`-prof gc`), 3 vòng warm-up + 5 vòng đo, 1 fork. `B/op` là byte cấp phát mỗi lần gọi.

| # | Anti-pattern | Xấu | Sửa | Chênh | Rác xấu → sửa |
|---|---|---:|---:|---:|---|
| P01 | `new ObjectMapper()` mỗi lần serialize | 14,6 µs | 0,48 µs | **30×** | 19,6 KB → 688 B |
| P01 | `DateTimeFormatter.ofPattern(..)` mỗi lần | 474 ns | 175 ns | 2,7× | 872 B → 256 B |
| P02 | `s.matches(regex)` | 429 ns | 116 ns | 3,7× | 1.472 B → 208 B |
| P02 | `s.split("\\s*,\\s*")` | 648 ns | 418 ns | 1,5× | 1.152 B → 544 B |
| P03 | `out += row` trong vòng lặp, **1.000 dòng** | 5,58 ms | 0,033 ms | **171×** | 21,9 MB → 92 KB |
| P03 | như trên, **10.000 dòng** | **639 ms** | 0,35 ms | **1.841×** | **2,23 GB** → 928 KB |
| P04 | exception cho input sai (50% sai), stack nông | 942 ns | 8,8 ns | 107× | 440 B → 0 |
| P04 | như trên, **stack sâu 150 frame** (giống Spring) | 4.995 ns | 556 ns¹ | 9× | 2.176 B → 16 B |
| P05 | `Map<Integer,Integer>.merge` làm histogram, 10.000 phần tử | 213 µs | 6,5 µs | 33× | 188 KB → 4 KB |
| P05 | `Long total += x`, 10.000 phần tử | 50 µs | 3,1 µs | 16× | **240 KB** → 0 |
| P06 | `LOG.fine("…" + describe(p))` khi FINE đang **tắt** | 1.023 ns | ~1 ns | ~1.000× | 1.584 B → 0 |
| P07 | khử trùng bằng `list.contains`, 1.000 phần tử | 879 µs | 29 µs | 30× | — |
| P07 | như trên, **10.000 phần tử** | **117 ms** | 0,46 ms | **255×** | — |

¹ 556 ns là chi phí của chính 150 lời gọi đệ quy dùng để dựng stack, có ở cả hai bản. Phần chênh do
exception: ~933 ns ở stack nông, **~4.440 ns** ở stack 150 frame. Exception đắt theo **độ sâu stack**,
mà stack Spring MVC + filter + AOP + JPA thường sâu 100–200 frame.

**Đọc bảng này thế nào cho đúng (đây là chỗ senior khác mid-level):**

- **P01, P02, P04, P06 tốn vài µs mỗi lần.** Ở 10.000 RPS, P01 tốn thêm `14,1 µs × 10.000 = 0,14
  core`, so với ~38 pod × 2 vCPU = 75 vCPU mà cả cụm cần ở mức tải đó (cách tính ở
  [02 §6](02-ve-he-thong-100k-1m-10m.md#6-code-xấu-nhân-với-số-máy--nối-sang-track-p)) là **dưới 0,2%**. Rác đáng kể hơn một chút: P01 thêm `18,9 KB × 10.000 = 190 MB/s`; P06 với 20 dòng
  debug mỗi request thêm `1,6 KB × 20 × 10.000 = 317 MB/s`; cộng lại **~500 MB/s cho cả cụm**, khoảng
  13 MB/s mỗi pod. Tần suất GC trẻ tỉ lệ với tốc độ cấp phát, nên đo `jvm.gc.memory.allocated ÷ RPS`
  trước và sau khi sửa để biết nó có đáng hay không. Kết luận thật: **sửa khi tiện, không đáng một
  sprint**.
- **P03, P05, P07 tăng theo kích thước dữ liệu.** Đây là chỗ µs thành trăm ms. Một request export
  10.000 dòng bằng `+=` chiếm một core **0,64 giây** và cấp phát **2,2 GB**. Mười người bấm Export
  cùng lúc là 22 GB rác trong vài giây.
- **P05 `Long total`: JIT đã không cứu.** Hay nghe "escape analysis sẽ gỡ boxing". Lần đo này nó
  không gỡ: 24 byte cho mỗi phần tử, đúng bằng một object `Long`.
- **Không tối ưu mù.** Sửa nhóm 1 khi profiler (async-profiler, JFR) chỉ ra nó nằm trong top, hoặc
  khi code review thấy nó nằm trong vòng lặp theo dữ liệu. Sửa P01–P07 rải rác mà bỏ qua một P09 là
  đổi thứ tự ưu tiên ngược.

### P01 — Tạo lại object đắt mỗi request

```java
// XẤU — mỗi lần gọi dựng lại cache serializer, introspect lại record/annotation
public String toJson(Transfer t) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(t);
}

// SỬA — một instance cho cả JVM; trong Spring: inject bean ObjectMapper Boot đã cấu hình sẵn
private final ObjectMapper mapper;              // constructor injection
public String toJson(Transfer t) throws JsonProcessingException {
    return mapper.writeValueAsString(t);
}
```

Cùng họ: `DateTimeFormatter.ofPattern`, `Pattern.compile`, `JAXBContext.newInstance`, `new
RestTemplate()` / `HttpClient.newHttpClient()` (xem P08), `MessageDigest.getInstance`, `Cipher`.
**Thread-safe thì dùng chung; không thread-safe thì dùng bản thread-safe thay thế** (`SimpleDateFormat`
→ `DateTimeFormatter`), không phải tạo mới mỗi lần.

### P02 — Biên dịch regex trên đường nóng

```java
// XẤU — String.matches/replaceAll/split(regex nhiều ký tự) gọi Pattern.compile MỖI LẦN
boolean ok = account.matches("^[0-9]{3}-[0-9]{6,10}$");

// SỬA
private static final Pattern ACCOUNT = Pattern.compile("^[0-9]{3}-[0-9]{6,10}$");
boolean ok = ACCOUNT.matcher(account).matches();
```

Bonus khi review: regex có **backtracking thảm hoạ** (`(a+)+$`) với input do người dùng gửi là lỗ
hổng ReDoS, một request làm treo một thread hàng giây. Precompile không cứu được lỗi này; phải sửa
regex hoặc giới hạn độ dài input.

### P03 — Nối chuỗi trong vòng lặp

```java
// XẤU — mỗi vòng tạo String mới và copy lại toàn bộ phần trước: O(n²) byte copy
String out = "";
for (String r : rows) out += r + "\n";

// SỬA
StringBuilder sb = new StringBuilder(rows.size() * 48);
for (String r : rows) sb.append(r).append('\n');
```

Java 9+ biên dịch `a + b` bằng `invokedynamic` nên **một** biểu thức nối chuỗi đã nhanh. Vòng lặp
`+=` thì không: compiler không gộp được các vòng với nhau. Với export lớn, tốt hơn nữa là **không dựng
chuỗi** mà ghi thẳng ra response stream (P19).

### P04 — Exception làm luồng điều khiển

```java
// XẤU — input sai là chuyện thường (30% mã giảm giá gõ sai), nhưng đi đường exception
try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }

// SỬA — kiểm tra trước; exception chỉ còn cho trường hợp thật sự hiếm (tràn số)
if (!looksLikeInt(s)) return def;
```

Chi phí chính là `fillInStackTrace()` đi qua toàn bộ stack. Nếu buộc phải dùng exception cho luồng
nghiệp vụ (ví dụ `InsufficientFundsException` ném qua nhiều tầng), cân nhắc constructor
`super(msg, null, false, false)` để tắt stack trace, **và** đặt nó ở chỗ không phải đường nóng.

### P05 — Autoboxing trên đường nóng

```java
// XẤU — mỗi merge tạo Integer mới (ngoài cache -128..127) + một Node trong HashMap
Map<Integer, Integer> h = new HashMap<>();
for (int v : codes) h.merge(v, 1, Integer::sum);

// XẤU, kín hơn — trông vô hại, tạo một Long mỗi vòng
Long total = 0L;
for (long a : amounts) total += a;

// SỬA — khoá nhỏ và liên tục → mảng primitive; tích luỹ → kiểu primitive
int[] h = new int[1000];
for (int v : codes) h[v]++;
long total = 0L;
```

### P06 — Dựng chuỗi log khi level đang tắt

```java
// XẤU — production chạy INFO; chuỗi vẫn được dựng xong rồi vứt đi
log.debug("processing " + describe(payment));

// VẪN XẤU với SLF4J — placeholder hoãn toString(), nhưng describe() vẫn chạy
// vì tham số được tính TRƯỚC khi vào hàm debug
log.debug("processing {}", describe(payment));

// SỬA — guard, hoặc Supplier (SLF4J 2: fluent API)
if (log.isDebugEnabled()) log.debug("processing {}", describe(payment));
log.atDebug().addArgument(() -> describe(payment)).log("processing {}");
```

Placeholder `{}` với tham số là **biến có sẵn** (`log.debug("id={}", id)`) thì đã đủ tốt, không cần guard.

### P07 — O(n²) ẩn

```java
// XẤU — 10.000 phần tử = 50 triệu phép so sánh trong MỘT request
for (String id : ids) if (!out.contains(id)) out.add(id);

// SỬA — giữ thứ tự xuất hiện đầu tiên, O(n)
List<String> out = new ArrayList<>(new LinkedHashSet<>(ids));
```

Cùng họ: `list.remove(0)` trên `ArrayList` (dùng `ArrayDeque`), `stream().filter()` lồng trong
`forEach` để join hai danh sách (dựng `Map` trước), `indexOf` trong vòng lặp. Khi review, hỏi một câu:
*"n tối đa của danh sách này là bao nhiêu?"*. Nếu không ai trả lời được thì n có thể là 1 triệu.

---

## 3. Nhóm 2 — Giữ tài nguyên khan hiếm quá lâu (Little's Law)

Nhóm nguy hiểm nhất. Code không tốn CPU, máy trông rảnh, autoscale theo CPU không kích hoạt, nhưng
throughput bị chặn trần và latency nổ tung.

### P08 — Tạo HTTP client mới mỗi request *(không có demo, giải thích bên dưới)*

```java
// XẤU — mỗi lần gọi một client mới: không tái dùng connection
RestTemplate rt = new RestTemplate();                  // hoặc HttpClient.newHttpClient()
rt.getForObject(fraudUrl, Verdict.class);

// SỬA — một bean dùng chung, có connection pool và TIMEOUT (xem P10)
@Bean RestClient fraudClient(RestClient.Builder b) {
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(300)).build());
    factory.setReadTimeout(Duration.ofMillis(800));
    return b.baseUrl(fraudUrl).requestFactory(factory).build();
}
```

Mất keep-alive nghĩa là mỗi request trả lại **bắt tay TCP + TLS** (1–3 round trip, vài chục ms nếu
khác region), và để lại socket ở trạng thái `TIME_WAIT` cho tới khi **cạn ephemeral port**.
`HttpClient` của JDK còn tự tạo selector thread riêng cho mỗi instance. Không demo vì đo trên
`localhost` không có độ trễ mạng thật, con số sẽ đánh giá thấp vấn đề.

### P09 — Gọi HTTP ra ngoài bên trong `@Transactional` ⭐

```java
// XẤU — connection DB (và row lock) bị giữ suốt thời gian chờ mạng
@Transactional
public TransferResult transfer(TransferCommand cmd) {
    Account from = accountRepo.findByIdForUpdate(cmd.fromId());   // lấy connection + khoá dòng
    FraudVerdict v = fraudClient.check(cmd);                        // HTTP 50–500 ms, vẫn giữ cả hai
    ledger.post(from, cmd, v);
    return ...;
}

// SỬA — gọi ngoài trước (có timeout), transaction chỉ bao phần DB
public TransferResult transfer(TransferCommand cmd) {
    FraudVerdict v = fraudClient.check(cmd);                        // không giữ connection nào
    return tx.execute(s -> {                                        // TransactionTemplate: ngắn
        Account from = accountRepo.findByIdForUpdate(cmd.fromId());
        return ledger.post(from, cmd, v);
    });
}
```

**Số đo** (pool 10, 200 client đồng thời, DB 5 ms × 2, gọi ngoài 50 ms, 1.000 request):

```text
remote call INSIDE tx    163 req/s   p50 1.222 ms  p99 1.231 ms   chờ connection p99 1.170 ms
remote call OUTSIDE tx   930 req/s   p50   212 ms  p99   258 ms   chờ connection p99   101 ms
```

Bản xấu: 95% thời gian của request là **xếp hàng chờ connection**. Thêm pod thì tổng số connection
tăng, nhưng DB có giới hạn `max_connections` và connection sống thì tốn RAM của DB; bạn sẽ chạm trần
DB trước khi chạm trần app.

**Hai biến thể hay gặp hơn bản gốc:**

- **`spring.jpa.open-in-view=true` là mặc định của Spring Boot** (Boot in cảnh báo lúc khởi động).
  EntityManager sống suốt request web; khi đã lấy connection, connection có thể bị giữ tới cuối
  request, kể cả lúc controller đang gọi HTTP hay serialize JSON. Hiệu ứng giống P09 mà không ai viết
  `@Transactional` sai. Tắt bằng `spring.jpa.open-in-view=false` và nạp đủ dữ liệu trong service.
- **Row lock bị giữ cùng connection.** `FOR UPDATE` + gọi ngoài 500 ms nghĩa là mọi giao dịch khác
  chạm cùng tài khoản (tài khoản công ty nhận lương, ví merchant) đứng chờ 500 ms. Đây là nút thắt
  theo **dữ liệu nóng**, thêm pod không giúp gì.

**Sửa đúng cho luồng tiền** không chỉ là tách transaction: nếu bước gọi ngoài thành công mà
transaction sau lỗi, cần **idempotency key** để retry an toàn và **outbox** để không mất sự kiện. Đó là
lý do P09 được đọc lại ở tuần 7 (Saga).

### P10 — Gọi downstream không timeout ⭐

```java
// XẤU — RestTemplate/HttpURLConnection mặc định: read timeout = vô hạn
restTemplate.postForObject(partnerUrl, req, Resp.class);

// SỬA — timeout theo ngân sách + fallback; cộng thêm circuit breaker (Resilience4j) ở tuần 12
try {
    return partner.post(req);                      // read timeout 200 ms (cấu hình ở client)
} catch (ResourceAccessException timeout) {
    outbox.enqueueRetry(req);                      // trả 202, đối soát sau
    return Resp.pending(req.id());
}
```

**Số đo** (Tomcat giả lập 20 worker, 200 req/s trong 3 s, 10% là `/transfer` gọi đối tác đang suy
giảm trả lời sau 2 s, 90% là `/balance` chỉ đọc DB 5 ms):

```text
no timeout                /balance  p50 1.051 ms  p99 3.147 ms   369/540 request quá 1 s
timeout 200ms + fallback  /balance  p50     5 ms  p99     5 ms     0/540 request quá 1 s
```

`/balance` không hề gọi đối tác, nhưng chết theo. Toán: 20 req/s × 2 s = cần 40 thread chỉ để
**chờ**, trong khi chỉ có 20. Có timeout 200 ms: 20 × 0,2 = 4 thread. Đây là cách một đối tác chậm kéo
sập cả app ngân hàng, và lý do **mọi** lời gọi mạng phải có timeout, kể cả gọi Redis và DB.

Hai tầng phòng thủ tiếp theo (tuần 12): **bulkhead** (đối tác dùng pool thread hoặc semaphore riêng,
không ăn vào pool chung) và **circuit breaker** (đối tác đã chết thì đừng gọi nữa).

### P11 — Một khoá toàn cục trên đường nóng

```java
// XẤU — mọi request của mọi thread qua MỘT cửa
public synchronized void inc(String route) { counts.merge(route, 1L, Long::sum); }

// SỬA — ConcurrentHashMap khoá theo bin; LongAdder chia bộ đếm theo thread khi tranh chấp
private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
public void inc(String route) { counts.computeIfAbsent(route, k -> new LongAdder()).increment(); }
```

**Số đo** (JMH throughput, 16 route):

| | 1 thread | 4 thread | Thêm thread thì |
|---|---:|---:|---|
| `synchronized` + HashMap | 24,7 ops/µs | **8,9 ops/µs** | **giảm 64%** |
| CHM + LongAdder | 65,6 ops/µs | **218 ops/µs** | tăng 3,3× |

Thêm core làm bản `synchronized` **chậm đi**: cache line của khoá nhảy qua lại giữa các core. Chỗ hay
gặp: bộ đếm metric tự viết, rate limiter tự viết, cache tự viết bằng `synchronized` method, singleton
khởi tạo lười có `synchronized` ở getter.

### P12 — Virtual thread bị ghim (pinning) — JDK 21–23 ⭐

```java
// XẤU trên JDK 21 — I/O bên trong synchronized ghim virtual thread vào carrier
synchronized (lock) {
    jdbcTemplate.update(...);        // hoặc HTTP, hoặc Thread.sleep
}

// SỬA — ReentrantLock: virtual thread unmount bình thường khi chờ I/O
lock.lock();
try { jdbcTemplate.update(...); } finally { lock.unlock(); }
```

**Số đo** (1.000 virtual thread, mỗi task một khoá **riêng**, nên không có tranh chấp khoá; khác biệt
hoàn toàn do pinning):

```text
JDK 21.0.11, cores=4
synchronized around IO    5.044 ms   (= 1.000 × 20 ms ÷ 4 carrier, đúng như lý thuyết)
ReentrantLock around IO      23 ms
```

**Phải nói đúng phiên bản khi phỏng vấn:** từ **JDK 24** ([JEP 491](https://openjdk.org/jeps/491))
`synchronized` không còn ghim; chạy demo này trên JDK 25 sẽ thấy hai bản ngang nhau. Nhưng nhiều hệ
thống production đang ở Java 21 LTS, và thứ ghim thường **không nằm trong code của bạn** mà trong thư
viện: JDBC driver cũ, connection pool cũ, SDK HTTP cũ. Phát hiện: `-Djdk.tracePinnedThreads=full`
(JDK 21–23) hoặc JFR event `jdk.VirtualThreadPinned`. (Ghi chú: module
[katalon-prep-java/04-concurrency](../katalon-prep/katalon-prep-java/04-concurrency/) cố ý không viết
test pinning vì máy chạy JDK 25; test ở đây dùng `@EnabledForJreRange(max = JAVA_23)` nên tự tắt
trên JDK mới thay vì nói dối.)

### P13 — Hàng đợi không giới hạn khi quá tải

```java
// XẤU — newFixedThreadPool dùng LinkedBlockingQueue VÔ HẠN
ExecutorService pool = Executors.newFixedThreadPool(4);

// SỬA — hàng đợi có giới hạn + từ chối sớm (HTTP 503/429 + Retry-After)
ExecutorService pool = new ThreadPoolExecutor(4, 4, 0, MILLISECONDS,
        new ArrayBlockingQueue<>(40), new ThreadPoolExecutor.AbortPolicy());
```

**Số đo** (năng lực 4 worker × 20 ms = 200 req/s, tải đến 300 req/s trong 4 s, client timeout 1 s):

```text
unbounded queue         nhận 1.200, từ chối   0   p50 1.040 ms  p99 2.047 ms  hàng đợi max 408  kịp 1 s: 576
bounded 40 + reject     nhận   833, từ chối 367   p50   219 ms  p99   223 ms  hàng đợi max  40  kịp 1 s: 833
```

Bản vô hạn **phục vụ ít người hơn** (576 so với 833): nó dành CPU xử lý những request mà client đã bỏ
đi từ lâu. Đây là định nghĩa của *goodput* sụp đổ. Cùng họ: `@Async` của Spring với
`ThreadPoolTaskExecutor` mặc định (`queueCapacity = Integer.MAX_VALUE`), consumer đọc từ broker vào
một `BlockingQueue` không giới hạn, `CompletableFuture.supplyAsync` dồn việc vô tội vạ.

Đọc thêm: Amazon Builders' Library, bài *Using load shedding to avoid overload*.

### P14 — `parallelStream()` với I/O *(không có demo)*

```java
// XẤU — chạy trên ForkJoinPool.commonPool(): (số core - 1) thread DÙNG CHUNG cho cả JVM
ids.parallelStream().map(id -> pricingClient.get(id)).toList();

// SỬA — executor riêng có giới hạn, hoặc virtual thread + Semaphore giới hạn số lời gọi đồng thời
try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
    var sem = new Semaphore(16);
    var futures = ids.stream().map(id -> ex.submit(() -> {
        sem.acquire();
        try { return pricingClient.get(id); } finally { sem.release(); }
    })).toList();
    ...
}
```

Common pool cũng là nơi `CompletableFuture.supplyAsync(..)` không truyền executor chạy. Một endpoint
dùng `parallelStream` để gọi HTTP là đủ chặn mọi `parallelStream` và `supplyAsync` khác trong JVM. Trên
máy 2 vCPU, common pool chỉ có **1** thread.

---

## 4. Nhóm 3 — Round trip thừa (P15–P17)

### P15 — N+1 query ⭐

```java
// XẤU — LAZY: 1 query lấy order, +1 query cho MỖI order khi chạm getItems()
List<Order> orders = orderRepo.findTop100ByCustomerIdOrderByCreatedAtDesc(customerId);
orders.forEach(o -> view.add(o.getId(), o.getItems().size()));

// SỬA 1 — @EntityGraph hoặc JOIN FETCH
@EntityGraph(attributePaths = "items")
List<Order> findTop100ByCustomerIdOrderByCreatedAtDesc(long customerId);

// SỬA 2 — 2 query, gom nhóm trong Java (an toàn với phân trang, không nhân dòng)
List<Item> items = itemRepo.findByOrderIdIn(orderIds);
```

**Số đo** (RTT mô phỏng 1 ms mỗi query, mức bình thường trong cùng AZ):

```text
N+1      queries=101  time=116 ms
batched  queries=2    time=9 ms      kết quả giống hệt
ở 200 req/s: N+1 = 20.200 query/s vào DB, batched = 400 query/s
```

Số query là **đếm chính xác** (có test). Thời gian là mô phỏng RTT. Lưu ý khi chọn sửa 1: `JOIN FETCH`
một collection kèm phân trang làm Hibernate phân trang **trong bộ nhớ** (cảnh báo `HHH90003004`), tức
là kéo cả bảng về rồi cắt. Có phân trang thì dùng sửa 2 hoặc `@BatchSize`.

Phát hiện sớm: bật `spring.jpa.properties.hibernate.generate_statistics=true` trong test và assert số
query, hoặc dùng thư viện đếm query trong test tích hợp. N+1 là lỗi **test bắt được**, đừng để
production bắt.

### P16 — `findAll()` rồi lọc trong Java; phân trang OFFSET sâu *(không có demo riêng)*

```java
// XẤU — kéo cả bảng qua mạng, giữ trong heap, rồi mới lọc
repo.findAll().stream().filter(t -> t.getAccountId() == id).toList();

// XẤU — OFFSET 200000 nghĩa là DB đọc rồi BỎ 200.000 dòng, mỗi trang lại kèm COUNT(*)
repo.findByAccountId(id, PageRequest.of(10_000, 20));

// SỬA — lọc trong SQL có index; keyset pagination: "đưa tôi 20 dòng sau id cuối cùng đã thấy"
@Query("select t from Txn t where t.accountId = :acc and t.id < :afterId order by t.id desc")
List<Txn> nextPage(long acc, long afterId, Limit limit);
```

Số đo thật về index, partial index, BRIN trên Postgres 16 đã có ở
[katalon-prep-java/05-postgres-depth](../katalon-prep/katalon-prep-java/05-postgres-depth/) (index nhanh
**100×**). Đây là lý do phân trang bằng cursor nằm trong tuần 2 của lộ trình.

### P17 — Gọi remote trong vòng lặp *(cùng cơ chế với P15)*

`for (id : ids) client.getPrice(id)` = N × RTT **nối tiếp**. 50 sản phẩm × 30 ms = 1,5 s. Sửa theo thứ
tự ưu tiên: (1) API batch `getPrices(ids)`; (2) cache nếu dữ liệu ít đổi; (3) gọi song song **có giới
hạn** như P14. Gọi song song không giới hạn là biến bài toán latency của mình thành sự cố tải của
service bên kia.

---

## 5. Nhóm 4 — Bộ nhớ và GC (P18–P20)

### P18 — Cache không giới hạn ⭐

```java
// XẤU — khoá chứa thành phần cardinality cao → mỗi request một khoá mới → map lớn mãi
private static final Map<String, Quote> CACHE = new HashMap<>();   // còn không thread-safe

// SỬA — Caffeine: giới hạn kích thước + hết hạn + số liệu hit ratio
private final Cache<String, Quote> cache = Caffeine.newBuilder()
        .maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(5)).recordStats().build();
```

**Số đo** (JVM con, `-Xmx128m`, mỗi entry ~1 KB, khoá gồm `userId + phút hiện tại`):

```text
UNBOUNDED   50.000 request: 50.000 entry, heap  60 MB
           100.000 request: 100.000 entry, heap 120 MB
           OutOfMemoryError sau 114.392 request      (lần 2: 114.349)
LRU(10k)    1.000.000 request phục vụ xong, luôn 10.000 entry, heap dao động 31-100 MB
```

Triệu chứng trên production luôn giống nhau: heap tăng răng cưa nhưng **đáy răng cưa cao dần** sau
mỗi lần GC, GC chạy dày hơn, p99 xấu dần theo ngày, OOM vào giờ cao điểm, restart "chữa" được vài
ngày. Cùng họ: `ThreadLocal` không `remove()` trong thread pool, listener đăng ký mà không huỷ,
`static List` gom log để "debug tạm", `String.intern()` trên dữ liệu người dùng.

### P19 — Nạp hết dữ liệu vào bộ nhớ rồi mới xử lý

```java
// XẤU — 1 triệu dòng sống CÙNG LÚC trong heap, rồi thêm một chuỗi CSV 48 MB, rồi thêm byte[]
List<Txn> all = repo.findAll();
String csv = all.stream().map(this::toCsv).collect(joining());
return ResponseEntity.ok(csv.getBytes(UTF_8));

// SỬA — stream từng dòng ra response; JDBC fetchSize để driver không nạp hết ResultSet
@Transactional(readOnly = true)
public void export(OutputStream out) {
    try (Stream<Txn> s = repo.streamAllBy(); var w = new OutputStreamWriter(out, UTF_8)) {
        s.forEach(t -> write(w, toCsv(t)));
    }
}
```

**Số đo** (JVM con `-Xmx512m`, 1 triệu giao dịch → CSV 48,7 MB):

```text
materialize all     peak heap 392-414 MB   thời gian 1.280-2.367 ms
stream row by row   peak heap        69 MB   thời gian   195-258 ms
```

Tổng byte cấp phát hai bản gần như nhau. Khác biệt là **lượng sống cùng lúc**: 400 MB sống thì chui
lên old gen và GC phải dọn nặng; năm người bấm Export cùng lúc là 2 GB. Bản stream giữ vài KB sống
tại mỗi thời điểm (69 MB đo được phần lớn là rác thế hệ trẻ chưa kịp dọn), bất kể bảng có 1 triệu
hay 1 tỷ dòng. Thời gian bản xấu dao động mạnh giữa hai lần chạy chính vì phụ thuộc GC.

Với Postgres: stream chỉ thật sự stream khi chạy **trong transaction** và có `fetchSize` (ví dụ
`@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "500"))`); thiếu một trong hai thì driver
vẫn nạp cả `ResultSet` vào bộ nhớ.

### P20 — Kafka consumer xử lý đồng bộ từng message *(không có demo; lab ở tuần 8–9)*

```java
// XẤU — mỗi message gọi HTTP 200 ms; poll 500 record = 100 s mới poll lại
@KafkaListener(topics = "transfer.completed")
void on(TransferCompleted e) { notificationClient.send(e); }
```

Khi đối tác chậm lên 1 s: 500 record × 1 s = 500 s, vượt `max.poll.interval.ms` (mặc định 300 s) →
broker coi consumer đã chết → **rebalance** → partition chuyển sang consumer khác → nó xử lý lại từ
offset chưa commit → cũng chậm → lại rebalance. Đó là *rebalance storm*, và nó gửi trùng thông báo.

Sửa: giảm `max.poll.records`; gọi theo lô (batch listener + API batch); xử lý song song có giới hạn
nhưng **giữ thứ tự theo key**; tạm `pause()` partition khi downstream chậm; consumer **idempotent**
vì trùng là chắc chắn xảy ra.

---

## 6. Checklist review code trước khi lên tải

Dùng khi review PR, và ở tuần 15–16 cho toàn bộ capstone. Mỗi dòng là một câu hỏi có/không.

| # | Câu hỏi | Bắt được |
|:---:|---|---|
| 1 | Trong `@Transactional` có gọi HTTP, gửi Kafka đồng bộ, đọc file, `sleep` không? `open-in-view` đã tắt chưa? | P09 |
| 2 | **Mọi** lời gọi mạng (HTTP, DB, Redis, Kafka producer) có connect timeout **và** read timeout không? Tổng timeout có nhỏ hơn timeout của người gọi mình không? | P10 |
| 3 | HTTP client, `ObjectMapper`, `Pattern`, formatter có được tạo một lần và dùng chung không? | P01, P02, P08 |
| 4 | Có executor, `@Async`, hàng đợi nào **không giới hạn** không? Khi đầy thì làm gì? | P13 |
| 5 | Có `parallelStream` hoặc `supplyAsync` không truyền executor mà bên trong làm I/O không? | P14 |
| 6 | Có `synchronized` nào nằm trên đường mọi request đi qua không? Có `synchronized` nào bao quanh I/O khi chạy virtual thread trên JDK 21–23 không? | P11, P12 |
| 7 | Vòng lặp nào có gọi repository, client, hoặc truy cập quan hệ LAZY không? Test có đếm số query không? | P15, P17 |
| 8 | Có `findAll()`, `PageRequest` với trang sâu, hoặc lọc trong Java thứ đáng lẽ lọc trong SQL không? | P16 |
| 9 | Có `Map`/`List` static hoặc sống lâu mà không có giới hạn kích thước không? `ThreadLocal` có `remove()` không? | P18 |
| 10 | Có chỗ nào nạp toàn bộ một tập dữ liệu mà kích thước tăng theo thời gian (export, báo cáo, import) không? | P19 |
| 11 | Vòng lặp theo dữ liệu (n không giới hạn) có `contains`/`indexOf`/`remove(0)`/nối chuỗi không? | P03, P07 |
| 12 | Log `debug`/`trace` có gọi hàm tốn kém trong tham số không? | P06 |
| 13 | Consumer Kafka có gọi đồng bộ ra ngoài cho từng message không? Có idempotent không? | P20 |

---

## 7. Bắt chúng trên production — metric nào, công cụ nào

| Anti-pattern | Metric (Micrometer/Actuator) báo hiệu | Công cụ xác nhận |
|---|---|---|
| P09, P15, P16 | `hikaricp.connections.pending` > 0 kéo dài; `hikaricp.connections.usage` (thời gian giữ) cao; `hikaricp.connections.acquire` p99 cao | Trace (OpenTelemetry) thấy span HTTP nằm **bên trong** span transaction; `pg_stat_statements` thấy một query gọi với tần suất bất thường |
| P10 | `http.client.requests` p99 cao ở một đối tác; `tomcat.threads.busy` = max trong khi CPU thấp | Thread dump (`jcmd <pid> Thread.print`): hàng loạt thread cùng đứng ở `SocketInputStream.read` |
| P11, P12 | Throughput không tăng khi tăng tải, CPU không lên | JFR: `jdk.JavaMonitorEnter` (tranh khoá), `jdk.VirtualThreadPinned`; async-profiler chế độ `lock` |
| P13 | `executor.queued` tăng dần; `executor.active` = max | Biểu đồ queue size theo thời gian, latency tăng tuyến tính |
| P01–P07 | CPU cao bất thường so với RPS; `jvm.gc.memory.allocated` (allocation rate) cao | async-profiler chế độ `cpu` và `alloc`, flame graph |
| P18, P19 | `jvm.memory.used` (old gen) đáy răng cưa tăng dần; `jvm.gc.pause` dài và dày | Heap dump (`jcmd <pid> GC.heap_dump`) + Eclipse MAT, tìm *dominator* |
| P20 | `kafka.consumer.fetch.manager.records.lag.max` tăng; số lần rebalance | Log `Member ... has left the group` lặp lại |

**Quy tắc đo đúng** (sẽ tập ở tuần 12): đo **p99**, không đo trung bình; có warm-up; đo
**allocation** không chỉ thời gian; với load test, coi chừng *coordinated omission* (load generator
chờ request chậm xong mới gửi tiếp, nên che mất đuôi dài). Bài nói *How NOT to Measure Latency* của
Gil Tene giải thích kỹ hiện tượng này.

---

## 8. Chạy lab

```bash
cd java-system-design/perf-lab
mvn -q test                                    # 33 test: bản sửa cho CÙNG kết quả + các hiệu ứng đếm được
mvn -q package -DskipTests                     # tạo target/benchmarks.jar (uber-jar)

java -cp target/benchmarks.jar com.prep.perf.demo.RunAll          # nhóm 2-4, ~40 giây
java -jar target/benchmarks.jar -prof gc                           # nhóm 1, ~6 phút
java -jar target/benchmarks.jar ContentionBench -t 1               # P11, so 1 thread với 4 thread
java -jar target/benchmarks.jar ContentionBench -t 4
java -jar target/benchmarks.jar 'p03|p07' -p n=10000               # chỉ chạy một nhóm
```

| Thư mục | Nội dung |
|---|---|
| [`perf-lab/src/main/java/com/prep/perf/code/`](perf-lab/src/main/java/com/prep/perf/code/) | Cặp `bad/good` của nhóm 1 và P11, dùng chung cho benchmark và test |
| [`perf-lab/src/main/java/com/prep/perf/bench/`](perf-lab/src/main/java/com/prep/perf/bench/) | JMH: `PerRequestBench`, `BatchBench`, `ContentionBench` |
| [`perf-lab/src/main/java/com/prep/perf/demo/`](perf-lab/src/main/java/com/prep/perf/demo/) | Demo hệ thống: `D09` … `D19`, chạy tất cả bằng `RunAll` |
| [`perf-lab/src/test/java/com/prep/perf/`](perf-lab/src/test/java/com/prep/perf/) | `SameResultTest` (bản sửa đúng như bản xấu), `CountedEffectsTest` (số query, số entry, số request bị từ chối, pinning trên JDK ≤ 23) |
| [`perf-lab/results/`](perf-lab/results/) | Output thô của lần chạy ngày 04/10/2026 |

**Chạy trên máy bạn (JDK 25):** mọi thứ chạy được, trừ P12 sẽ cho hai bản ngang nhau (đúng như JEP
491). Muốn thấy pinning thì chạy riêng D12 bằng JDK 21: `sdk use java 21.0.x-tem` rồi
`java -cp target/benchmarks.jar com.prep.perf.demo.D12Pinning`. Số tuyệt đối sẽ khác máy lab; **tỉ
lệ** và **hình dạng** (bản xấu tăng theo n², pool bị chặn trần) mới là thứ phải giống.

---

## 9. Kể trong phỏng vấn

**Câu hỏi hay gặp: *"Hệ thống của bạn đã có autoscale, sao vẫn chậm khi tải tăng?"***

> *"Because autoscaling only multiplies what one instance can do. If a request holds a database
> connection while it waits on a 50 ms partner call, the connection pool caps the whole service at
> pool size divided by hold time, about 160 requests per second for a pool of 10, no matter how many
> pods I add. I measured exactly that in a lab: 163 requests per second with the call inside the
> transaction, 930 after moving it out. So before scaling out, I check what each request holds and
> for how long."*

**Khung STAR cho câu chuyện cải thiện hiệu năng** (lấy từ việc thật ở công ty theo tuần 12; dưới
đây là khung, số liệu phải là của bạn):

```text
S  API chuyển tiền p99 1,2 s lúc 9h sáng ngày lương; CPU pod chỉ 30%.
T  Tìm nguyên nhân trong 1 sprint, không thêm hạ tầng.
A  Dashboard Hikari: pending > 0 suốt giờ cao điểm. Trace: span gọi fraud-check nằm trong span
   transaction. Tách thành tx ngắn + gọi ngoài có timeout 300 ms + idempotency key.
   Thêm test đếm query và test đo thời gian giữ connection.
R  p99 1,2 s → 250 ms ở cùng tải; số pod giảm từ 8 xuống 5. Viết ADR, thêm dòng vào checklist review.
```

**Ba câu bẫy và câu trả lời ngắn:**

| Câu hỏi | Trả lời |
|---|---|
| *"Virtual thread giải quyết hết chuyện thread pool rồi đúng không?"* | Bỏ giới hạn về **thread**, không bỏ giới hạn về **connection pool**, **đối tác**, hay **khoá**. 10.000 virtual thread đập vào pool 20 connection là 9.980 cái xếp hàng. Và trên JDK 21–23, `synchronized` quanh I/O còn ghim carrier (P12) |
| *"Tăng pool size lên 200 cho nhanh?"* | Thường **chậm hơn**: DB chỉ chạy song song thật sự cỡ vài lần số core; thêm connection là thêm tranh chấp và RAM ở DB. Xem bài *About Pool Sizing* của HikariCP. Sửa thời gian giữ, không sửa kích thước pool |
| *"Retry khi timeout là đủ an toàn?"* | Chỉ khi thao tác **idempotent**, có **backoff + jitter**, và có **ngân sách retry**. Không thì retry nhân tải đúng lúc đối tác đang yếu nhất (retry storm) |

---

## 10. Ranh giới trung thực

| Nội dung | Trạng thái |
|---|---|
| Số JMH nhóm 1, P11 | **Đã đo** trên container Linux 4 vCPU, JDK 21.0.11, JMH 1.37, 1 fork. Output thô: [results/jmh-2026-10-04.txt](perf-lab/results/jmh-2026-10-04.txt). P01 JSON lần đầu có sai số lớn hơn trung bình nên đã chạy lại với 10 vòng đo, bảng dùng số lần chạy lại |
| Số demo P09, P10, P12, P13, P15, P18, P19 | **Đã đo, chạy 2 lần**, output ở [results/demos-2026-10-04.txt](perf-lab/results/demos-2026-10-04.txt). Chênh giữa hai lần dưới 5%, trừ thời gian của P19 bản xấu (1,3–2,4 s, phụ thuộc GC) |
| Bản chất của các demo nhóm 2–3 | **Mô phỏng**: connection pool là `Semaphore`, I/O là `Thread.sleep`, DB là `FakeDb` đếm round trip. Thứ được đo là **hệ quả hàng đợi** (Little's Law), vốn không phụ thuộc vào việc I/O là thật hay giả. Chưa chạy với HikariCP + Postgres + HTTP thật |
| P08, P14, P16, P17, P20 | **Chưa có demo**, chỉ giải thích. P16 có số thật ở module 05-postgres-depth; P20 sẽ làm ở lab Kafka tuần 8–9 |
| Hành vi `open-in-view` giữ connection tới cuối request | Mô tả theo hành vi đã biết của Spring + Hibernate, **chưa kiểm chứng** trong workspace này. Kiểm chứng trên project thật bằng `hikaricp.connections.usage` khi bật và tắt |
| Con số rác "~500 MB/s" và "+0,14 core" | Phép nhân từ số đo JMH; số "20 dòng debug mỗi request" là **giả định** |
| P12 | Chỉ tái hiện được trên JDK 21–23. Trên JDK 24+ hai bản ngang nhau, và test tương ứng tự tắt |
