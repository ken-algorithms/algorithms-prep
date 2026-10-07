# Video giai đoạn 1 — Nền tảng (tuần 1–4): kế hoạch và nhật ký

> Bộ video tiếng Anh cho [giai đoạn 1](../10-implement-gd1-nen-tang.md) của
> [lộ trình 6 tháng](../00-lo-trinh-6-thang.md). Giọng đọc là **Kokoro, giọng nam Tom (`am_michael`)**,
> dựng theo cách đã dùng ở repo
> [superken-ielts/ielts-target-5-5](https://github.com/superken-ielts/ielts-target-5-5) (công cụ
> `agents/lesson_video`).
>
> File này vừa là kế hoạch, vừa là chỗ theo dõi việc đã làm. Mỗi đợt làm xong phải cập nhật
> [bảng trạng thái (mục 8)](#8-bảng-trạng-thái) và [nhật ký (mục 9)](#9-nhật-ký--lịch-sử-đã-làm).
> Chữ viết tắt: [01 — Bảng chữ viết tắt](01-bang-chu-viet-tat.md).

---

## 0. Trả lời ngắn: nên làm bao nhiêu video

**Đề xuất 30 video, tổng khoảng 3 giờ 55 phút:**

| Nhóm | Số video | Mã | Dài dự kiến |
|---|---:|---|---:|
| Định hướng | 1 | Ep00 | 7 phút |
| Tuần 1 — ước lượng, chi phí một request | 6 | Ep01–Ep06 | 50 phút |
| Tuần 2 — networking, API, rate limiter | 6 | Ep07–Ep12 | 46 phút |
| Tuần 3 — database, URL shortener, ID | 6 | Ep13–Ep18 | 48 phút |
| Tuần 4 — cache, nhất quán, notification, mốc tuần 4 | 8 | Ep19–Ep26 | 64 phút |
| Ôn chữ viết tắt | 3 | Ep27–Ep29 | 21 phút |
| **Tổng** | **30** | | **≈ 236 phút** |

Vì sao là con số này:

1. **Nguồn dài.** File 10 có 1.211 dòng; cộng phần giai đoạn 1 của file 00, 01, 02 là khoảng
   **22.000 chữ**. Mỗi tuần có 3–5 ghi chú khái niệm, 1–3 lab, một phần Track P, một bản vẽ Track S,
   5–8 bài tập có đáp án và một bài nói 2 phút.
2. **Mỗi video dài 6–9 phút.** Repo IELTS đã dùng độ dài này cho 36 video: đủ cho một ý lớn, xem một
   lần không mỏi, dựng mất 10–20 phút nên sửa lại rẻ. Ở tốc độ đọc 0,9, 9 phút là khoảng 1.100 từ
   tiếng Anh.
3. **Mỗi video là một ý lớn, kèm bài tập của chính ý đó.** Khi làm bài tập 3.3 mà quên công thức thì
   mở đúng Ep16, không phải tua một video dài 40 phút. Bài nói 2 phút gắn vào cuối video cùng chủ
   đề, không tách thành video riêng.
4. **Tuần 4 có 8 video** vì ngoài nội dung của tuần còn có thang L0–L4 với câu hỏi "tải tăng 10 lần"
   (Ep25), và mốc tuần 4 có mock interview chấm theo rubric (Ep26).
5. **Ba video ôn chữ viết tắt (Ep27–Ep29) là phần thêm.** Mỗi video bài học đã tự giải thích mọi chữ
   viết tắt nó dùng (quy tắc ở mục 2). Ba video này để ôn lại toàn bộ 148 dòng trong
   [bảng chữ viết tắt](01-bang-chu-viet-tat.md). Nếu muốn gọn thì bỏ ba video này, còn **27 video**,
   vẫn cover đủ nội dung.

---

## 1. Phạm vi nguồn

| File | Phần dùng cho video | Ghi chú |
|---|---|---|
| [10 — Implement giai đoạn 1](../10-implement-gd1-nen-tang.md) | **Toàn bộ** | Nguồn chính: khái niệm, lab, bài tập, đáp án, mốc tuần 4 |
| [00 — Lộ trình 6 tháng](../00-lo-trinh-6-thang.md) | Mục tiêu, tự đánh giá, tổng quan, lịch tuần mẫu, giai đoạn 1, Track P, Track S, khung trả lời phỏng vấn, mốc tuần 4 | Giai đoạn 2–4, capstone, công ty mục tiêu: chỉ nhắc một câu trong Ep00 |
| [01 — Track P](../01-java-code-cham-duoi-tai-cao.md) | §0–2 (P01–P07), P08, P09, P10, P15, P16, P18, P19, §6–9 | P11–P14, P17, P20 thuộc tuần 8–9 và 12, để cho bộ video giai đoạn sau |
| [02 — Track S](../02-ve-he-thong-100k-1m-10m.md) | §0–6 | §7–8 (tài liệu, ranh giới) chỉ nhắc |
| [ops-lab](../ops-lab/README.md) | Bảng kết quả P09 trên stack thật | Chỉ dùng trong Ep16 làm bằng chứng cho Little's Law |

Số liệu trong video **chỉ lấy từ các file trên**. Số đo thật nói rõ là "measured on the lab
machine"; số điển hình (bảng latency, ba con số nhẩm) nói rõ là "typical".

---

## 2. Quy ước chung cho mọi video

| Hạng mục | Quy ước |
|---|---|
| Giọng | Kokoro-82M bản nén q8, giọng **Tom `am_michael`** (nam, Mỹ), **một người đọc** cho cả bộ |
| Tốc độ | **0,9**, giống repo IELTS |
| Ngôn ngữ | Lời đọc và phụ đề **tiếng Anh**. Tiếng Việt chỉ xuất hiện trên thẻ chữ viết tắt (một dòng nghĩa ngắn) |
| Độ dài | 6–9 phút. Kịch bản quá 1.200 từ thì tách cảnh hoặc chuyển bớt sang video khác |
| Khung hình | 1280×720, slide tĩnh có hiện dần (cách của công cụ IELTS), H.264 + AAC, có chương (chapter) |
| Tên file | `lessons/epNN-<slug>.yaml` và `.mp4` cùng tên, ví dụ `ep01-estimation-toolkit.mp4` |

**Khung một video** (thứ tự cố định để người xem quen):

1. **Title** (10 giây): mã, tên, tuần, file nguồn.
2. **Acronyms in this video**: mọi chữ viết tắt **lần đầu xuất hiện trong video này**. Mỗi dòng có
   dạng tắt, dạng đầy đủ tiếng Anh và nghĩa ngắn tiếng Việt. Tom đọc dạng đầy đủ.
3. **Nội dung**: 3–5 chương. Bảng trong tài liệu thì hiện từng dòng; phép tính thì hiện từng bước;
   code thì tô dòng đang nói.
4. **Bài tập**: hiện đề, thẻ "Pause and try" đếm ngược 5 giây (người xem tự bấm dừng), rồi mới tới
   đáp án. Đáp án là **một** lời giải hợp lý, giống ghi chú trong file 10.
5. **Interview line**: 1–3 câu tiếng Anh mẫu để nói trong phỏng vấn. Ở 4 video có bài nói 2 phút
   (Ep03, Ep11, Ep14, Ep21) thì đây là bài nói mẫu đầy đủ.
6. **Recap** và, khi cần, **"measured vs typical"**: phần nào đã chạy thật, phần nào là suy luận
   (lấy từ mục *Ranh giới trung thực* của file nguồn).

**Quy tắc chữ viết tắt** (đáp ứng yêu cầu "mô tả hết các chữ viết tắt"):

- Lần đầu trong **mỗi** video: nói dạng đầy đủ trước rồi mới dùng dạng tắt, ví dụ *"concurrent users,
  CCU for short"*. Không giả định người xem đã xem video trước.
- Mọi chữ viết tắt hiện trên slide đều phải có trong thẻ ở bước 2 **và** trong
  [bảng chữ viết tắt](01-bang-chu-viet-tat.md). Lúc dựng sẽ kiểm bằng script: quét chữ hoa trong
  YAML, so với bảng (mục 6, bước 4).
- Hai nghĩa của chữ **L**: `L4/L7` là **tầng mạng** (Tom đọc *"layer four, layer seven"*), còn
  `L0–L4` là **bậc quy mô** của Track S (đọc *"level L zero to L four"*). Video nào dùng cả hai thì
  nói rõ ngay lần đầu.

---

## 3. Danh sách 30 video

Cột **Nguồn**: `10 §1.1` là mục 1.1 của file 10; `BT 1.4` là bài tập 1.4; `01 P09` là P09 trong file
01; `02 §3.2` là mục 3.2 của file 02.

### Định hướng

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep00** | Phase 1 orientation: the roadmap, two tracks, and how to use these videos | Ba kết quả sau 24 tuần; 4 giai đoạn; lịch 5 buổi một tuần; Track P và Track S là gì và vì sao thêm; bảng giai đoạn 1; thư mục `my-work/` và sổ lỗi; cách xem video (dừng ở "Pause and try"); các mã P, D, V, L, Ep | 00 mục tiêu, tự đánh giá, tổng quan, lịch tuần mẫu, giai đoạn 1, Track P, Track S; 10 §0; README | 7 |

### Tuần 1 — Khung tư duy, ước lượng, chi phí một request

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep01** | The estimation toolkit: conversions, nines, latency, and Little's Law | Đầu ra và tài liệu đọc của tuần 1; 1 ngày ≈ 10^5 s, 1 triệu request/ngày ≈ 12 RPS, đỉnh × 2–3; storage và băng thông; 99,9% và 99,99%; bảng latency theo bậc độ lớn; L = λ × W và ba cách dùng | 10 tuần 1 (đầu ra, đọc), §1.1–1.3 | 8 |
| **Ep02** | From "N users" to CCU and RPS: the four-step formula | 4 bước users → DAU → CCU → RPS; vì sao bước 2 viết như vậy; giả định mặc định hồ sơ A và B; bảng 100k/1M/10M; chat có CCU gấp 12 lần; đi ngược từ CCU; ba con số nhẩm (pod, Postgres, Redis) | 02 §0–1.4; BT 1.4, 1.8 | 8 |
| **Ep03** | Estimation workout: a bank, a test platform, and a chat app | Format bài ước lượng (giả định → 4 bước → "con số nào đổi thiết kế"); ba bài giải từng bước; **bài nói 2 phút**: ước lượng app ngân hàng 5 triệu user | 10 Lab 1A; BT 1.1–1.3; bài nói tuần 1 | 9 |
| **Ep04** | Track P, group 1: per-request waste, P01 to P07 | Ba cơ chế biến code xấu thành sự cố hệ thống; P01–P07: code xấu, code sửa, ns/op, B/op; đọc bảng cho đúng: µs chỉ đáng khi nhân theo dữ liệu (P03, P05, P07); "không tối ưu mù" | 01 §0–2 | 8 |
| **Ep05** | Measure before you fix: JMH, multiplying by QPS, and a review drill | Chạy lab 1B; vì sao benchmark phải viết như vậy (constant folding, dead code, warm-up, fork); nhân với QPS ra core và MB/s rác; "dưới 1%" nghĩa là gì; code xấu nhân với số máy; review `StatementService` tìm 8 lỗi và thứ tự sửa | 10 Lab 1B; BT 1.5–1.7; 01 §6, §8; 02 §6 | 9 |
| **Ep06** | Track S: drawing V1 (one machine) and V2 (100k users) | Thang 5 bậc tóm tắt (luật: chỉ lên bậc khi có thứ đang vỡ); L0 và danh sách SPOF; L1 cho 100k users, mỗi hộp xoá một SPOF; lộ trình 10 bản vẽ; quy ước vẽ; tự chấm 6 câu | 10 Track S tuần 1; 02 §2, §3.1–3.2, §4–4.2 | 8 |

### Tuần 2 — Networking, API, rate limiter

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep07** | The life of an HTTPS request | Đầu ra và tài liệu đọc của tuần 2; DNS → TCP → TLS 1.3 → HTTP: connection mới ~3 RTT, tái dùng 1 RTT; HTTP/2 multiplexing và head-of-line blocking; HTTP/3 trên QUIC; bài TP.HCM ↔ Sydney (1.440 ms so với 720 ms) | 10 tuần 2 (đầu ra, đọc), §2.1; BT 2.1 | 6 |
| **Ep08** | The edge: L4 vs L7, WAF, API gateway, and where rate limits live | Bảng L4/L7; bốn ca chọn L4/L7 (WebSocket, định tuyến path, mTLS, gRPC); **V3**: CDN → WAF/LB → gateway → app, ba chỗ đặt rate limiter, mỗi chỗ chặn được gì | 10 §2.2, Track S tuần 2 (V3); BT 2.2 | 8 |
| **Ep09** | Choosing an API style and writing a transfer API | REST, gRPC, WebSocket, SSE: dùng khi nào; đặc tả `POST /v1/transfers` và mã lỗi; phân trang bằng cursor thay OFFSET; tiền là chuỗi hoặc số nguyên, không dùng `double` | 10 §2.3; BT 2.3 | 8 |
| **Ep10** | Idempotency keys: four rules and three hard cases | Bốn quy tắc; hai request cùng key ở hai pod; pod chết sau khi ghi sổ; bẫy TTL (key 24 giờ, retry 48 giờ) | 10 §2.4; BT 2.4 | 7 |
| **Ep11** | Lab 2: a distributed token bucket on Redis and Lua | Yêu cầu 100/phút burst 20; đọc script Lua từng khối (nạp lười, `TIME` của Redis, `PEXPIRE`, `retry_ms`); filter và timeout 50 ms; ADR fail-open hay fail-closed; 6 test; bug biên cửa sổ của fixed window; **bài nói 2 phút**: token bucket hay fixed window. Nói rõ: script mới chạy trên `fakeredis`, chưa chạy trên Redis thật | 10 Lab 2; bài nói tuần 2; ranh giới trung thực | 9 |
| **Ep12** | Track P: P08 and P10, client reuse, timeouts, and timeout budgets | P08 tạo HTTP client mỗi request; P10 không timeout làm `/balance` (không liên quan) p99 3,1 s, sửa còn 5 ms; ba việc Track P tuần 2; đặt timeout từ trong ra ngoài | 01 P08, P10; 10 Track P tuần 2; BT 2.5 | 8 |

### Tuần 3 — Database sâu, URL shortener, ID generator

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep13** | Storage engines and indexes: B-tree, LSM-tree, and reading EXPLAIN | Đầu ra và tài liệu đọc của tuần 3; B-tree hay LSM-tree; thứ tự cột trong index ghép; covering index; partial index; đọc `EXPLAIN (ANALYZE, BUFFERS)`; chọn index cho bảng 500 triệu dòng | 10 tuần 3 (đầu ra, đọc), §3.1–3.2; BT 3.1 | 8 |
| **Ep14** | Isolation levels: lost update, write skew, and three fixes | Bảng dị thường × isolation level; `@Version` không bắt được write skew; update có điều kiện, optimistic, pessimistic: chọn thế nào; hai ca hạn mức; **bài nói 2 phút**: optimistic hay pessimistic | 10 §3.3–3.4; BT 3.2; bài nói tuần 3 | 9 |
| **Ep15** | Lab 3A: reproduce a lost update, then fix it three ways | Ép hai transaction xen kẽ bằng `CyclicBarrier`; test đỏ (rút 200 từ 150); ba cách sửa, mỗi cách một test; bảng đo dưới tranh chấp. Nói rõ: kết quả "kỳ vọng" chưa đo trong workspace | 10 Lab 3A; ranh giới trung thực | 7 |
| **Ep16** | Track P: P09, HTTP inside a transaction, and connection-pool math | P09 với pool 10: ~163 so với ~930 req/s; Little's Law ra trần; trên stack thật (ops-lab): ở 150 rps bản xấu trông như bản sửa, ở 250 rps dừng ở 183 req/s, p99 11,1 s; virtual threads làm bản xấu tệ hơn; trần của pool, 6 pod, `max_connections`, cần 6.000 RPS thì làm gì | 01 P09; ops-lab kết quả; BT 3.3 | 8 |
| **Ep17** | Track P: N+1 and findAll, caught by a query-count test | P15: 101 query 116 ms so với 2 query 9 ms; P16: `findAll()` lọc trong Java, OFFSET sâu; lab 3C: test đếm query đỏ trước, sửa bằng `@EntityGraph` hoặc `IN`; tắt open-in-view | 01 P15, P16; 10 Lab 3C | 7 |
| **Ep18** | V4: a URL shortener and its ID generator | Ước lượng (40 ghi/s, 4.000 đọc/s, 3 TB); base62 7 ký tự; ba cách sinh mã (cấp dải số, Snowflake, hash); 302 hay 301; thống kê click bất đồng bộ; Snowflake 41/10/12 bit, đồng hồ lùi, node id trên Kubernetes | 10 Track S tuần 3 (V4), Lab 3B | 9 |

### Tuần 4 — Caching, nhất quán, notification, mốc tuần 4

| Mã | Tên (tiếng Anh) | Nội dung chính | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep19** | Replicas, read-your-writes, CAP and PACELC | Đầu ra và tài liệu đọc của tuần 4; replication lag; ba cách cho read-your-writes; CAP và PACELC trong 30 giây; mạng giữa hai AZ đứt 30 giây: ghi chọn C, màn hình lịch sử chọn A | 10 tuần 4 (đầu ra, đọc), §3.5, §4.4; BT 3.4, 4.3 | 8 |
| **Ep20** | Four cache strategies, and Caffeine or Redis | Cache-aside, read-through, write-through, write-behind; xoá chứ không cập nhật cache khi ghi; Caffeine hay Redis; chọn cách cache cho 5 loại dữ liệu (số dư dùng để quyết định thì **không** cache) | 10 §4.1, §4.3; BT 4.1 | 7 |
| **Ep21** | Cache stampede, hot keys, and penetration | Ba bệnh và cách chữa; single-flight trong JVM và giữa các pod; TTL có jitter; hit ratio tối thiểu 85%; Redis restart thì sao; **bài nói 2 phút**: chống cache stampede | 10 §4.2; BT 4.2; bài nói tuần 4 | 8 |
| **Ep22** | Lab 4: a two-tier cache that survives a stampede | 7 bước, mỗi bước assert trên **số đếm** `dbLoads`; `SET NX PX`; xoá cache sau commit; thử phá: xoá trước commit thì giá trị cũ nằm lại tới hết TTL | 10 Lab 4 | 8 |
| **Ep23** | Track P: P18 unbounded caches and P19 loading everything | P18: OOM sau ~114k request với heap 128 MB, bản LRU chạy 1 triệu request; P19: đỉnh heap ~400 MB so với 69 MB; stream cần transaction + `fetchSize`; ba việc Track P tuần 4 | 01 P18, P19; 10 Track P tuần 4 | 7 |
| **Ep24** | V5: a notification system for a bank | Ước lượng (25/s giao dịch, 1.700 push/s chiến dịch); hàng đợi theo kênh và độ ưu tiên; dedup theo `(eventId, channel)`; retry + DLQ; circuit breaker và nhà cung cấp SMS dự phòng; cái vỡ trước khi × 10 | 10 Track S tuần 4 (V5) | 8 |
| **Ep25** | The scale ladder L0 to L4, and "what if load grows ten times?" | Đi lại thang L0 → L4 với số của hồ sơ A; L2 (1M) và L3 (10M) vẽ gì và vì sao; L4 chỉ cần nói được; khung 5 câu trả lời "× 10"; hoàn thiện V2 có số; tự kiểm tiêu chí 2 của mốc | 02 §2, §3.3–3.5, §5 | 9 |
| **Ep26** | Week-4 milestone: the interview framework, a mock, and the rubric | Khung 6 bước 45–60 phút; mock Rate limiter rút gọn theo từng bước có bấm giờ; rubric 5 tiêu chí × 0–2 điểm; 8 tiêu chí qua giai đoạn; trễ quá 2 tuần thì cắt gì | 00 khung trả lời phỏng vấn, mốc kiểm tra; 10 mốc tuần 4 | 9 |

### Ôn chữ viết tắt

| Mã | Tên (tiếng Anh) | Nội dung | Nguồn | Phút |
|---|---|---|---|---:|
| **Ep27** | Acronyms review 1: load, latency, reliability, Java and the JVM | Nhóm 1 và 2 của bảng chữ viết tắt (62 dòng) | [01 — Bảng chữ viết tắt](01-bang-chu-viet-tat.md) | 7 |
| **Ep28** | Acronyms review 2: network, API, security, messaging, cloud | Nhóm 3 và 5 (43 dòng) | như trên | 7 |
| **Ep29** | Acronyms review 3: data, caching, and roadmap codes | Nhóm 4 và 6 (43 dòng) | như trên | 7 |

---

## 4. Bảng phủ nội dung

Mỗi mục của nguồn phải có ít nhất một video. Khi dựng xong một video, đánh dấu ở
[bảng trạng thái](#8-bảng-trạng-thái); bảng này chỉ đổi khi danh sách video đổi.

### 4.1 File 10 — Implement giai đoạn 1

| Mục | Video |
|---|---|
| [§0 Cách dùng file này](../10-implement-gd1-nen-tang.md#0-cách-dùng-file-này) (nhịp tuần, quy tắc làm bài, `my-work/`, sổ lỗi, stack) | Ep00 |
| [Tuần 1](../10-implement-gd1-nen-tang.md#tuần-1--khung-tư-duy-ước-lượng-và-chi-phí-của-một-request): đầu ra, đọc | Ep01 |
| §1.1 Quy đổi · §1.2 Bảng latency · §1.3 Little's Law | Ep01 |
| Lab 1A Sổ ước lượng | Ep03 |
| Lab 1B JMH | Ep05 |
| Track S tuần 1 (V1, V2) | Ep06 |
| BT 1.1, 1.2, 1.3 | Ep03 |
| BT 1.4, 1.8 | Ep02 |
| BT 1.5, 1.6, 1.7 | Ep05 |
| Bài nói tuần 1 | Ep03 |
| [Tuần 2](../10-implement-gd1-nen-tang.md#tuần-2--networking-api-và-rate-limiter): đầu ra, đọc | Ep07 |
| §2.1 Đường đi request HTTPS | Ep07 |
| §2.2 L4 hay L7 | Ep08 |
| §2.3 Chọn kiểu API | Ep09 |
| §2.4 Idempotency key | Ep10 |
| Lab 2 Rate limiter | Ep11 |
| Track P tuần 2 | Ep12 |
| Track S tuần 2 (V3) | Ep08 |
| BT 2.1 · 2.2 · 2.3 · 2.4 · 2.5 | Ep07 · Ep08 · Ep09 · Ep10 · Ep12 |
| Bài nói tuần 2 | Ep11 |
| [Tuần 3](../10-implement-gd1-nen-tang.md#tuần-3--database-sâu-url-shortener-id-generator): đầu ra, đọc | Ep13 |
| §3.1 B-tree hay LSM · §3.2 Index | Ep13 |
| §3.3 Isolation · §3.4 Ba cách chống lost update | Ep14 |
| §3.5 Replica và read-your-writes | Ep19 (cùng chủ đề nhất quán của tuần 4) |
| Lab 3A · Lab 3B · Lab 3C | Ep15 · Ep18 · Ep17 |
| Track S tuần 3 (V4) | Ep18 |
| BT 3.1 · 3.2 · 3.3 · 3.4 | Ep13 · Ep14 · Ep16 · Ep19 |
| Bài nói tuần 3 | Ep14 |
| [Tuần 4](../10-implement-gd1-nen-tang.md#tuần-4--caching-nhất-quán-notification-system): đầu ra, đọc | Ep19 |
| §4.1 Bốn chiến lược · §4.3 Caffeine hay Redis | Ep20 |
| §4.2 Ba bệnh của cache | Ep21 |
| §4.4 CAP, PACELC | Ep19 |
| Lab 4 | Ep22 |
| Track P tuần 4 | Ep23 |
| Track S tuần 4 (V5) | Ep24 |
| BT 4.1 · 4.2 · 4.3 | Ep20 · Ep21 · Ep19 |
| Bài nói tuần 4 | Ep21 |
| [Mốc tuần 4](../10-implement-gd1-nen-tang.md#mốc-tuần-4--tiêu-chí-qua-giai-đoạn) (8 tiêu chí, rubric) | Ep26 |
| [Ranh giới trung thực](../10-implement-gd1-nen-tang.md#ranh-giới-trung-thực) | Nói trong Ep01 (số latency là điển hình), Ep03 (giả định là đoán), Ep11 (Lua chạy trên `fakeredis`), Ep15 (lab 3A chưa đo), Ep18 (Snowflake đã chạy) |

### 4.2 File 01, 02, 00

| Mục | Video |
|---|---|
| [01 §0–1](../01-java-code-cham-duoi-tai-cao.md#1-ba-cơ-chế-biến-code-xấu-thành-sự-cố-hệ-thống) Ba cơ chế | Ep04 |
| [01 §2](../01-java-code-cham-duoi-tai-cao.md#2-nhóm-1--lãng-phí-cpu-và-rác-mỗi-request-p01p07) P01–P07 | Ep04 (số đo), Ep05 (nhân với QPS) |
| 01 P08, P10 | Ep12 |
| [01 P09](../01-java-code-cham-duoi-tai-cao.md#p09--gọi-http-ra-ngoài-bên-trong-transactional-) | Ep16 (nhắc trước ở Ep01 khi nói Little's Law) |
| 01 P15, P16 | Ep17 |
| [01 P18](../01-java-code-cham-duoi-tai-cao.md#p18--cache-không-giới-hạn-), P19 | Ep23 |
| 01 §6 Checklist review · §8 Chạy lab | Ep05 |
| 01 §7 Metric trên production | Mỗi video Track P có một slide "how to catch it in production" (Ep04, Ep12, Ep16, Ep17, Ep23) |
| 01 §9 Kể trong phỏng vấn | Phần "Interview line" của các video Track P |
| [02 §0–1.4](../02-ve-he-thong-100k-1m-10m.md#1-từ-n-users-ra-ccu-và-rps--công-thức-4-bước) Công thức 4 bước | Ep02 |
| [02 §2](../02-ve-he-thong-100k-1m-10m.md#2-thang-5-bậc--tóm-tắt-trên-một-trang) Thang 5 bậc | Ep06 (tóm tắt), Ep25 (đầy đủ) |
| 02 §3.1–3.2 L0, L1 | Ep06 |
| 02 §3.3–3.5 L2, L3, L4 | Ep25 |
| 02 §4–4.2 Lộ trình vẽ, quy ước, tự chấm | Ep06 |
| [02 §5](../02-ve-he-thong-100k-1m-10m.md#5-câu-hỏi-tải-tăng-10-lần-thì-sao--cách-trả-lời) "Tải tăng 10 lần" | Ep25 |
| 02 §6 Code xấu nhân với số máy | Ep05 |
| [00](../00-lo-trinh-6-thang.md) mục tiêu, tự đánh giá, tổng quan, lịch tuần, giai đoạn 1, Track P, Track S | Ep00 |
| [00 Khung trả lời phỏng vấn](../00-lo-trinh-6-thang.md#khung-trả-lời-phỏng-vấn-4560-phút), mốc kiểm tra tuần 4 | Ep26 |

---

## 5. Phát âm — các chỗ phải viết `say`

Đã kiểm ngày 07/10/2026 bằng bộ tách âm của chính Kokoro
(`kokoro_onnx.tokenizer.Tokenizer().phonemize(text, "en-us")`, dựa trên espeak-ng) với **khoảng 400
từ và cụm** lấy từ nguồn (nhiều lượt). Bộ tách âm này chính là bước biến chữ thành âm trước khi Kokoro đọc, nên
kết quả khớp với cái Tom sẽ đọc. Lần kiểm này **chưa sinh tiếng**: nghe lại vẫn là bước cuối.

### 5.1 Đọc đúng sẵn, không cần `say`

API, CPU, vCPU, SSD, GC, JVM, JDK, JIT, JMH, JFR, JSON (*jay-son*), CSV, DTO, JPA, JDBC, UUID, SQL,
NoSQL, MVCC, LSM, SSTable, BRIN, LSN, SSI, RPS, QPS, TPS, CCU, DAU, RTT, TTL, SPOF (*spoff*), SLA,
SLO, DNS, TCP, UDP, TLS, mTLS, HTTP, HTTPS, QUIC (*quick*), REST, gRPC, SSE, URL, BFF, JWT, OTP, SMS,
FCM, APNs, CDC, CQRS, DLQ, ADR, AZ, Multi-AZ, AWS, S3, OTel (*O tel*), C4, STAR,
p50, p99, `≈` (*approximately*), `×` (*times*), `≥`, `L = λ × W` (*L equals lambda times W*),
HikariCP, PgBouncer, Redis, Kafka, PostgreSQL, Postgres, Lua, Caffeine, Bucket4j, Lettuce,
Testcontainers, WireMock, Micrometer, Cassandra, ScyllaDB, RocksDB, InnoDB, MySQL, Kubernetes,
Nginx (*engine x*), Envoy, Keycloak, Snowflake, base62, N+1, Kleppmann, Sydney, Singapore, Discord,
Stripe, tên lớp Java viết kiểu camelCase (`ObjectMapper`, `StringBuilder`, `RestTemplate`…), annotation
(`@Transactional` đọc *at transactional*).

### 5.2 Phải viết `say`

| Trên màn hình | `say` | Kokoro đọc nếu để nguyên |
|---|---|---|
| OIDC | `O I D C` | *oydk* |
| DDIA | `dee dee eye ay` | *dee-dai-a*; viết `D, D, I, A` thì chữ A cuối thành mạo từ |
| CAP (theorem) | `cap` | đánh vần C-A-P, chữ A thành mạo từ |
| PACELC | `pass-elk` | *pay-selk* |
| ReDoS · DDoS | `ree-doss` · `dee-doss` | *ree-doo-ess* |
| SaaS | `sass` | *saa-ess* |
| RAM | `ram` | đánh vần R-A-M (nghe được, nhưng không ai nói vậy) |
| idempotency, idempotent, Idempotency-Key | `idem-potency`, `idem-potent`, `idem-potency key` | *aid-m-potency*, sai trọng âm |
| regex | `reg-ex` | *rə-jex* |
| memtable | `mem table` | *mem-tə-bəl* |
| Alex Xu | `Alex Shoo` | *Alex Zoo* |
| TP.HCM, Ho Chi Minh City | `Ho Chee Minh City` | *Ho Kai Minh* |
| chữ A đứng riêng (hồ sơ A, AZ-a, Lab 3A vẫn đúng) | `profile eigh`, `AZ eigh` | đọc thành mạo từ /ɐ/; `A Z eigh` cũng sai vì chữ A đầu thành mạo từ, phải viết liền `AZ` |
| dấu `/`: I/O, HTTP/2, HTTP/3, CCU/RPS, WebSocket/SSE | `I O`, `HTTP two`, `HTTP three`, `CCU and RPS`, `WebSocket or SSE` | đọc to chữ *slash* |
| HTTP/1.1, TLS 1.3 | `HTTP one point one`, `TLS one point three` | ngắt câu ở dấu chấm |
| dải có gạch: P01–P07, V1–V5, L0–L4, 1–4 | `P one to P seven`, `V one to V five`… | **bỏ mất dấu gạch**: *P zero one P zero seven* |
| mã có số 0: P01, D09 | `P one`, `D nine` | *P zero one* (đúng nhưng nặng tai) |
| L4/L7 nghĩa tầng mạng | `layer four`, `layer seven` | *L four*: nhầm với bậc L4 của Track S |
| 1M, 10M, 100k, 1.5M | `one million`, `ten million`, `a hundred thousand`, `one point five million` | *one M*, *one hundred K*, *one. five M* |
| số thập phân: 99.9%, 0.64 s, 2.4×, 3.3 | `ninety-nine point nine percent`, `zero point six four seconds`, `two point four times` | ngắt câu ở dấu chấm |
| số kiểu Việt: 86.400, 1.000, 0,64 | viết số kiểu Anh trong `say`: `eighty-six thousand four hundred` | *one. zero zero zero* |
| 10^5, 62^7 | `ten to the fifth`, `sixty-two to the seventh` | *ten five* |
| ms, µs, ns | `milliseconds`, `microseconds`, `nanoseconds` | *M S*, *micro S* |
| KB, MB, GB, TB, Mbit/s | `kilobytes`… `megabits per second` | đánh vần |
| ns/op, B/op, req/s | `nanoseconds per op`, `bytes per op`, `requests per second` | đọc *slash* |
| O(n²) | `O of n squared` | *O n two* |
| `~` | `about` | *tilde* |
| `→` | `to` hoặc `then` | *right arrow* |
| INCR, PEXPIRE | `increment`, `P expire` | *inker*, *pexpire* |
| X-Api-Key | `X API key` | dính liền |

Câu mẫu: `say: "The pool holds ten connections, each held for about fifty-three milliseconds, so the
ceiling is about one hundred eighty-eight requests per second."` trong khi phụ đề vẫn là
*"10 connections ÷ ~53 ms ≈ 188 req/s"*.

Lệnh kiểm một kịch bản trước khi dựng (mục 6, bước 4) in ra phiên âm của mọi câu `say` và mọi chữ
viết hoa còn lại, để soát bằng mắt những chỗ chưa có trong bảng trên.

---

## 6. Công cụ và quy trình

### 6.1 Công cụ

Mang `agents/lesson_video` từ repo IELTS (bản ở commit `930feee`) sang repo này tại
`tools/lesson_video/`, giữ nguyên lõi (TTS, timeline, ghép video, bộ nhớ tiếng từng câu) và đổi:

| Thay đổi | Lý do |
|---|---|
| Một người đọc: `speakers: {tom: …}` với `voice: {kokoro: am_michael}` | Yêu cầu giọng Tom; không có vai cô giáo Emma |
| Bỏ phụ thuộc `book.json`; mỗi kịch bản có `covers:` là danh sách anchor của file nguồn | Ở đây không có sách in; anchor là thứ để đối chiếu với bảng phủ ở mục 4 |
| Lệnh `coverage` đọc heading của file 10, 00, 01, 02 và in mục nào đã có video | Thay cho lệnh `coverage` theo unit sách của IELTS |
| Cảnh mới: `acronyms` (thẻ chữ viết tắt), `code` (Java/Lua/SQL, tô dòng), `table` (bảng hiện từng dòng), `calc` (phép tính hiện từng bước), `diagram` (ảnh PNG dựng từ mermaid bằng Chromium có sẵn), `checklist` | Nội dung system design cần code, bảng, phép tính, sơ đồ; các cảnh IELTS chủ yếu cho từ vựng và bài đọc |
| Giữ: `title`, `bullets`, `compare`, `qa`, `mcq`, `errors`, `order`, `timing`, `practice`, `pairs` | Dùng lại cho bài tập, so sánh, lỗi thường gặp, mock bấm giờ |
| Script kiểm chữ viết tắt: mọi chữ hoa trong kịch bản phải có trong `01-bang-chu-viet-tat.md` và trong thẻ `acronyms` của video | Thực thi quy tắc ở mục 2 |

Model: bản q8 `model_quantized.onnx` (~92 MB) và giọng `am_michael.bin`, để ở
`~/.cache/lesson_video/kokoro/`, **không commit**. Môi trường cloud chặn Hugging Face, nên lấy theo
cách repo IELTS đã làm: model từ gói npm `kokoro-q8-shards` (ghép 6 mảnh, kiểm sha256 theo ghi chú ở
`docs/agent-hoc-tap/05-lich-su-thay-doi.md` của repo IELTS), giọng từ gói npm `kokoro-js`.

Kịch bản dự kiến (định dạng IELTS, thêm `covers`, `acronyms`, `table`):

```yaml
id: ep01-estimation-toolkit
title: "Ep01 · The estimation toolkit"
subtitle: "Conversions, nines, latency, and Little's Law"
covers:
  - 10-implement-gd1-nen-tang.md#11-quy-đổi-phải-nhẩm-được
  - 10-implement-gd1-nen-tang.md#13-littles-law--công-thức-dùng-nhiều-nhất-cả-lộ-trình
speakers:
  tom: {name: Tom, role: narrator, color: "#1F5FAD", voice: {kokoro: am_michael, flite: rms}}
scenes:
  - kind: acronyms
    chapter: "Acronyms in this video"
    items:
      - {abbr: RPS, full: "requests per second", vi: "số request mỗi giây"}
      - {abbr: RTT, full: "round-trip time", vi: "thời gian gói tin đi và về"}
    lines:
      - tom: "RPS means requests per second. RTT is the round-trip time of one network message."
  - kind: table
    chapter: "Conversions"
    rows:
      - ["1 day", "86,400 s ≈ 10^5 s"]
      - ["1 million requests/day", "≈ 12 RPS on average"]
    lines:
      - tom: "One day is 86,400 seconds, roughly 10^5."
        say: "One day is eighty-six thousand four hundred seconds, roughly ten to the fifth."
        reveal: 1
```

### 6.2 Quy trình một video

1. **Đọc nguồn** theo cột *Nguồn* ở mục 3; ghi danh sách chương và số liệu sẽ dùng.
2. **Viết kịch bản** YAML tiếng Anh theo khung ở mục 2; viết `say` theo mục 5.
3. `python -m lesson_video check <yaml>`: kiểm cú pháp, anchor trong `covers` có thật.
4. **Kiểm chữ viết tắt và phát âm**: script quét chữ hoa (mục 6.1) và in phiên âm của mọi câu `say`.
5. `build --engine silent --work <thư mục>`: dựng nhanh không tiếng để soát slide, chữ tràn.
6. `build --engine kokoro`: dựng thật, **từng video một** (Kokoro dùng hết CPU).
7. **Xem lại ở tốc độ 1×**, ghi lỗi; sửa, dựng lại (nhờ bộ nhớ tiếng, chỉ đọc lại câu đã đổi).
8. **Cập nhật** bảng trạng thái và nhật ký trong file này, rồi commit kịch bản + video.

Kích thước: video slide của IELTS khoảng 0,8 MB/phút, nên 236 phút ≈ **190 MB**. Video commit thẳng
vào git như `leetcode-38-bai-video/` đang làm (93 MB). Workflow Pages cần thêm một dòng copy
`java-system-design/video-gd1/lessons/*.mp4` vào `_deploy/media/`.

---

## 7. Các đợt làm

| Đợt | Phạm vi | Đầu ra | Điều kiện xong |
|---|---|---|---|
| **0** | Kế hoạch | File này, [bảng chữ viết tắt](01-bang-chu-viet-tat.md), kiểm phát âm | **Xong 07/10/2026** |
| 1 | Công cụ + 2 video mẫu | `tools/lesson_video/` (mục 6.1), Ep00, Ep01 | Người dùng duyệt giọng, tốc độ, bố cục slide trước khi làm hàng loạt |
| 2 | Tuần 1 | Ep02–Ep06 | 5 video, mỗi video qua bước 3–7 |
| 3 | Tuần 2 | Ep07–Ep12 | 6 video |
| 4 | Tuần 3 | Ep13–Ep18 | 6 video |
| 5 | Tuần 4 | Ep19–Ep26 | 8 video |
| 6 | Ôn tập + gắn vào web | Ep27–Ep29; trang video trong app algorithms-prep; workflow Pages copy mp4; lệnh `coverage` cho thấy mọi mục ở mục 4 đã có video | Cả 30 video chạy được trên GitHub Pages |

Mỗi đợt dựng 10–20 phút/video, tức 1–2 giờ cho một tuần học. Commit sau mỗi video để cây git luôn
sạch nếu phiên làm việc bị ngắt.

---

## 8. Bảng trạng thái

Trạng thái: **—** chưa làm · **Kịch bản** đã viết YAML · **Dựng thử** đã dựng `silent` · **Xong**
đã dựng Kokoro và xem lại.

| Mã | Tuần | Trạng thái | Dài thật | MB | Chương | Đợt | Ghi chú |
|---|:---:|:---:|---:|---:|---:|:---:|---|
| Ep00 | — | — | | | | | |
| Ep01 | 1 | — | | | | | |
| Ep02 | 1 | — | | | | | |
| Ep03 | 1 | — | | | | | |
| Ep04 | 1 | — | | | | | |
| Ep05 | 1 | — | | | | | |
| Ep06 | 1 | — | | | | | |
| Ep07 | 2 | — | | | | | |
| Ep08 | 2 | — | | | | | |
| Ep09 | 2 | — | | | | | |
| Ep10 | 2 | — | | | | | |
| Ep11 | 2 | — | | | | | |
| Ep12 | 2 | — | | | | | |
| Ep13 | 3 | — | | | | | |
| Ep14 | 3 | — | | | | | |
| Ep15 | 3 | — | | | | | |
| Ep16 | 3 | — | | | | | |
| Ep17 | 3 | — | | | | | |
| Ep18 | 3 | — | | | | | |
| Ep19 | 4 | — | | | | | |
| Ep20 | 4 | — | | | | | |
| Ep21 | 4 | — | | | | | |
| Ep22 | 4 | — | | | | | |
| Ep23 | 4 | — | | | | | |
| Ep24 | 4 | — | | | | | |
| Ep25 | 4 | — | | | | | |
| Ep26 | 4 | — | | | | | |
| Ep27 | — | — | | | | | |
| Ep28 | — | — | | | | | |
| Ep29 | — | — | | | | | |

**Tổng đã xong: 0/30 video, 0 phút.**

---

## 9. Nhật ký / lịch sử đã làm

Mỗi đợt thêm một mục ở **cuối** danh sách: ngày, làm gì, số liệu, lỗi gặp và cách xử lý, commit.

### Đợt 0 — 07/10/2026: lập kế hoạch

- **Xem lại công cụ IELTS** (`superken-ielts/ielts-target-5-5`, commit `930feee`): kịch bản YAML →
  slide Pillow 1280×720 → Kokoro qua `kokoro-onnx` → ffmpeg H.264/AAC có chương → `lessons.json`. Repo
  đó đã làm 36 video (Unit 1–3), mỗi video khoảng 4–10 phút, Emma `af_heart` + Tom `am_michael`, tốc độ 0,9.
  Bài học mang sang: dựng từng video một; `build --engine silent` để soát nhanh; bộ nhớ tiếng từng câu
  làm sửa lại rẻ; chữ "A" đứng riêng phải viết `eigh`; model lấy từ npm vì Hugging Face bị chặn.
- **Xem lại pipeline video có sẵn trong repo này** (`leetcode-38-bai-video/`: manim + edge-tts giọng
  tiếng Việt). Không dùng cho bộ này: edge-tts cần mạng tới dịch vụ của Microsoft và không phải
  Kokoro; manim đẹp nhưng mỗi cảnh phải lập trình, quá chậm cho 30 video nhiều bảng.
- **Lấy phạm vi nguồn** (mục 1): khoảng 22.000 chữ. Quét chữ viết tắt bằng regex trên phần nguồn đó,
  bỏ tên node mermaid (`PGP`, `RC`, `AUD`…) và tên lớp Java, còn **148 dòng** (một số dòng gộp nhiều chữ) →
  [bảng chữ viết tắt](01-bang-chu-viet-tat.md).
- **Kiểm phát âm** bằng bộ tách âm của Kokoro cho khoảng 400 từ và cụm (nhiều lượt). Tìm ra 30 nhóm
  phải viết `say` (mục 5.2). Phát hiện đáng nhớ: dấu gạch dải số bị bỏ (*P01–P07* thành *P zero one
  P zero seven*); số thập phân và số kiểu Việt bị ngắt câu ở dấu chấm; `~` đọc thành *tilde*; `A Z
  eigh` vẫn sai, phải viết liền `AZ eigh`.
- **Chốt 30 video** (mục 0, 3) và bảng phủ nội dung (mục 4): mọi mục của file 10 và phần giai đoạn 1
  của file 00, 01, 02 đều có video.
- Chưa làm: chưa mang công cụ sang, chưa tải model, chưa có video nào.
- Commit trên nhánh `claude/wizardly-pascal-9yhum5`: hai file của thư mục này, link ở README của
  `java-system-design/` và trang chủ bản nhiều trang; dựng lại `web/index.html`.

---

## 10. Quyết định đã chọn mặc định (đổi được trước đợt 1)

| # | Câu hỏi | Mặc định | Phương án khác |
|---|---|---|---|
| 1 | Phụ đề | Chỉ tiếng Anh; tiếng Việt chỉ trên thẻ chữ viết tắt | Thêm dòng `vi` dưới mỗi câu như repo IELTS (dễ hiểu hơn, nhưng làm chậm việc luyện nghe tiếng Anh) |
| 2 | Số giọng | Chỉ Tom | Thêm Emma `af_heart` làm người phỏng vấn trong Ep26 và các bài nói mẫu |
| 3 | Tốc độ | 0,9 | 1,0 cho gần tốc độ phỏng vấn thật |
| 4 | Lưu video | Commit mp4 vào git (~190 MB) | Git LFS, hoặc chỉ commit kịch bản và dựng video trong workflow |

---

## 11. Ranh giới trung thực

| Nội dung | Trạng thái |
|---|---|
| Số video, thời lượng | **Ước tính** từ số từ của nguồn và độ dài video IELTS; thời lượng thật ghi vào bảng trạng thái sau khi dựng |
| Phát âm ở mục 5 | **Đã kiểm** bằng bộ tách âm của Kokoro (cùng bước chuyển chữ → âm mà Kokoro dùng); **chưa nghe** tiếng thật, vì chưa tải model trong repo này |
| Công cụ ở mục 6.1 | **Kế hoạch**: các cảnh `acronyms`, `code`, `table`, `calc`, `diagram`, `checklist` chưa viết |
| Kích thước 190 MB | Suy từ tỉ lệ ~0,8 MB/phút của video IELTS (một video 6:52 nặng 5,47 MB) |
