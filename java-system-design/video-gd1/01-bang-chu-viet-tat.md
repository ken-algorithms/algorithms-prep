# Bảng chữ viết tắt — giai đoạn 1 (tuần 1–4)

> Mọi chữ viết tắt xuất hiện trong phần giai đoạn 1 của lộ trình: [10](../10-implement-gd1-nen-tang.md)
> (toàn bộ), [00](../00-lo-trinh-6-thang.md), [01](../01-java-code-cham-duoi-tai-cao.md) và
> [02](../02-ve-he-thong-100k-1m-10m.md) (các mục liệt kê ở
> [kế hoạch, mục 1](00-ke-hoach-va-lich-su.md#1-phạm-vi-nguồn)).
> Dùng cho thẻ *Acronyms in this video* của từng video và cho ba video ôn Ep27–Ep29.

**Cách đọc bảng**

- **Tom đọc**: `✓` là Kokoro đọc đúng khi để nguyên; nếu không thì là chuỗi phải viết vào `say`
  (đã kiểm bằng bộ tách âm của Kokoro, xem [kế hoạch, mục 5](00-ke-hoach-va-lich-su.md#5-phát-âm--các-chỗ-phải-viết-say)).
- **Video**: video đầu tiên dùng và giải thích chữ đó. Mọi video sau dùng lại vẫn giải thích lần đầu
  trong video của nó. `Ep27`–`Ep29` nghĩa là chữ chỉ có trong tài liệu, không có video bài học nào
  cần tới, nên chỉ ôn ở video ôn tập.
- Không đưa vào bảng: tên node trong sơ đồ mermaid (`PGP`, `RC`, `AUD`…), tên lớp và hàm Java, từ khoá
  SQL (`SELECT`, `OFFSET`…) trừ khi nó là chữ viết tắt.

---

## 1. Tải, độ trễ, đơn vị, độ tin cậy — ôn ở Ep27

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| **DAU** | Daily Active Users | Số user dùng app ít nhất một lần trong ngày. Bước 1 của công thức 4 bước | ✓ | Ep02 |
| **CCU** | Concurrent Users | Số user đang dùng **cùng một lúc**; lấy theo giờ cao điểm. Phụ thuộc độ dài phiên hơn số user | ✓ | Ep00 |
| **RPS** | Requests Per Second | Số request mỗi giây vào hệ thống. Đơn vị tải chính của lộ trình | ✓ | Ep00 |
| **QPS** | Queries Per Second | Thường dùng như RPS; khi nói về database là số truy vấn mỗi giây | ✓ | Ep01 |
| **TPS** | Transactions Per Second | Số giao dịch nghiệp vụ (ví dụ chuyển tiền) mỗi giây, khác số request | ✓ | Ep02 |
| req/s, rps | requests per second | Cách viết khác của RPS trong bảng số đo | `requests per second` | Ep00 |
| **p50, p95, p99** | 50th / 95th / 99th percentile | 50%, 95%, 99% request nhanh hơn con số này. p99 là "đuôi" mà user chậm nhất thấy | ✓ (*P fifty*, *P ninety-nine*) | Ep01 |
| **RTT** | Round-Trip Time | Thời gian một gói tin đi và về giữa hai máy | ✓ | Ep01 |
| 0-RTT | zero round-trip resumption | TLS 1.3 nối lại phiên cũ, gửi dữ liệu ngay mà không cần bắt tay; có rủi ro bị phát lại (replay) | `zero R T T` | Ep07 |
| ns, µs, ms, s | nanosecond, microsecond, millisecond, second | 10⁻⁹, 10⁻⁶, 10⁻³ giây và giây | `nanoseconds`, `microseconds`, `milliseconds`, `seconds` | Ep01 |
| B, KB, MB, GB, TB | byte, kilobyte, megabyte, gigabyte, terabyte | Đơn vị dung lượng, mỗi bậc × 1.000 (ước lượng) | `bytes`, `kilobytes`… | Ep01 |
| Mbit/s | megabits per second | Đơn vị băng thông; 1 byte = 8 bit, nên 20 MB/s ≈ 160 Mbit/s | `megabits per second` | Ep03 |
| ns/op, B/op | nanoseconds per operation, bytes per operation | Hai cột kết quả của JMH: thời gian và lượng bộ nhớ cấp phát mỗi lần gọi | `nanoseconds per op`, `bytes per op` | Ep04 |
| **CPU** | Central Processing Unit | Bộ xử lý; "core" là một lõi | ✓ | Ep00 |
| **vCPU** | virtual CPU | Một luồng phần cứng trên cloud; "pod 2 vCPU" | ✓ | Ep02 |
| **RAM** | Random-Access Memory | Bộ nhớ chính; đọc ~100 ns | `ram` | Ep01 |
| **SSD** | Solid-State Drive | Ổ cứng thể rắn; đọc ngẫu nhiên ~100 µs | ✓ | Ep01 |
| **I/O** | Input/Output | Đọc ghi đĩa hoặc mạng; thứ làm thread phải chờ | `I O` | Ep13 |
| **GC** | Garbage Collection / Collector | Bộ dọn rác của JVM; mỗi lần dừng GC cộng vào p99 | ✓ | Ep01 |
| G1 | Garbage-First (collector) | GC mặc định của JDK hiện đại, chia heap thành vùng | `G one` | Ep01 |
| **OOM** | Out Of Memory (`OutOfMemoryError`) | JVM hết heap, process chết | ✓ | Ep04 |
| **SLA** | Service Level Agreement | Cam kết có hợp đồng với khách hàng, vi phạm thì bồi thường | ✓ | Ep01 |
| **SLO** | Service Level Objective | Mục tiêu nội bộ cho một chỉ số, ví dụ "99,9% request dưới 300 ms" | `S L O` (để nguyên Kokoro đọc *slow*) | Ep00 |
| **SLI** | Service Level Indicator | Chỉ số đo được để so với SLO, ví dụ tỉ lệ request thành công | ✓ | Ep00 |
| **SPOF** | Single Point Of Failure | Điểm chết đơn: một thành phần hỏng là cả hệ thống ngừng | ✓ (*spoff*) | Ep01 |
| **HA** | High Availability | Sẵn sàng cao: có bản dự phòng để một thành phần chết vẫn chạy | ✓ | Ep25 |
| **DR** | Disaster Recovery | Khôi phục sau thảm hoạ, thường là một region dự phòng | ✓ | Ep25 |
| **RPO** | Recovery Point Objective | Tối đa được mất bao nhiêu dữ liệu (tính bằng thời gian) khi có sự cố | ✓ | Ep25 |
| **RTO** | Recovery Time Objective | Tối đa bao lâu thì hệ thống phải chạy lại | ✓ | Ep25 |
| **MVP** | Minimum Viable Product | Bản sản phẩm nhỏ nhất dùng được; bậc L0 của thang | ✓ | Ep00 |
| L = λ × W | Little's Law | Số thứ đang trong hệ thống = throughput × thời gian mỗi thứ ở lại | ✓ (*L equals lambda times W*) | Ep01 |

## 2. Java, JVM, Spring, code — ôn ở Ep27

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| **API** | Application Programming Interface | Giao diện để chương trình khác gọi vào; ở đây chủ yếu là HTTP API | ✓ | Ep00 |
| **JVM** | Java Virtual Machine | Máy ảo chạy bytecode Java | ✓ | Ep04 |
| **JDK** | Java Development Kit | Bộ công cụ và runtime Java; lab dùng JDK 21 | ✓ | Ep04 |
| **JIT** | Just-In-Time compiler | Trình biên dịch lúc chạy, biến code nóng thành mã máy; lý do benchmark cần warm-up | ✓ | Ep05 |
| C1, C2 | JIT tier 1 (client), tier 2 (server) | Hai tầng JIT: C1 biên dịch nhanh, C2 tối ưu sâu | `C one`, `C two` | Ep05 |
| **JMH** | Java Microbenchmark Harness | Thư viện đo hiệu năng hàm Java đúng cách (warm-up, fork, chống JIT xoá code) | ✓ | Ep01 |
| **JFR** | Java Flight Recorder | Profiler có sẵn trong JDK, ghi sự kiện với chi phí thấp | ✓ | Ep04 |
| JEP | JDK Enhancement Proposal | Đề xuất thay đổi JDK có số hiệu; file 01 nhắc một JEP sửa lỗi pinning của virtual thread | ✓ | Ep27 |
| **JSON** | JavaScript Object Notation | Định dạng dữ liệu dạng văn bản của REST API | ✓ (*jay-son*) | Ep02 |
| **CSV** | Comma-Separated Values | File bảng, mỗi dòng các giá trị cách nhau dấu phẩy; dùng cho export | ✓ | Ep05 |
| UTF-8 | Unicode Transformation Format, 8-bit | Bảng mã chữ dùng khi ghi CSV ra byte | ✓ | Ep23 |
| **DTO** | Data Transfer Object | Object chỉ chứa dữ liệu để truyền giữa các tầng hoặc qua API | ✓ | Ep05 |
| **JPA** | Jakarta Persistence API | Chuẩn ánh xạ object ↔ bảng (object-relational mapping) của Java; Hibernate là bản cài đặt Spring dùng | `J P eigh` (chữ A đứng riêng giữa câu bị đọc thành mạo từ *a*) | Ep04 |
| **JDBC** | Java Database Connectivity | API cấp thấp để Java nói chuyện với database; `fetchSize` là của JDBC | ✓ | Ep23 |
| **MVC** | Model-View-Controller | Ở đây là Spring MVC, tầng web xử lý HTTP request | ✓ | Ep04 |
| **AOP** | Aspect-Oriented Programming | Cơ chế Spring bọc method (transaction, log) bằng proxy; làm stack sâu thêm | ✓ | Ep04 |
| SLF4J | Simple Logging Facade for Java | API log chung của Java; placeholder `{}` hoãn dựng chuỗi | ✓ | Ep04 |
| ReDoS | Regular-expression Denial of Service | Regex viết kém bị một chuỗi đầu vào làm chạy hàng giây, treo thread | `ree-doss` | Ep04 |
| O(n²) | Big-O, quadratic | Thời gian tăng theo bình phương kích thước dữ liệu (P07) | `O of n squared` | Ep04 |
| **LRU** | Least Recently Used | Chính sách xoá phần tử lâu chưa dùng nhất khi cache đầy | ✓ | Ep23 |
| **UUID** | Universally Unique Identifier | Mã 128 bit sinh ngẫu nhiên, gần như không trùng; dùng làm idempotency key | ✓ | Ep10 |
| **ID** | identifier | Mã định danh | ✓ | Ep00 |
| MD5, SHA | Message Digest 5, Secure Hash Algorithm | Hàm băm; một cách sinh mã URL ngắn (phải kiểm va chạm) | ✓ | Ep18 |
| HikariCP | Hikari Connection Pool | Connection pool mặc định của Spring Boot (CP = connection pool) | ✓ | Ep16 |
| N+1 | N+1 query problem | Một query lấy danh sách rồi N query lấy từng phần tử con (P15) | ✓ | Ep04 |
| **TTL** | Time To Live | Thời gian sống của một key trong cache hoặc một idempotency key | ✓ | Ep10 |
| MAT | (Eclipse) Memory Analyzer Tool | Công cụ mở heap dump, tìm object nào giữ nhiều bộ nhớ nhất (P18, P19) | ✓ (*M A T*) | Ep23 |
| -Xmx | maximum heap size (cờ JVM) | Giới hạn heap, ví dụ `-Xmx64m` trong demo OOM | `X M X` | Ep23 |
| **ADR** | Architecture Decision Record | Một trang ghi một quyết định kiến trúc: bối cảnh, lựa chọn, đánh đổi | ✓ | Ep11 |
| **PR** | Pull Request | Yêu cầu gộp code, nơi làm code review | ✓ | Ep05 |
| **DB** | Database | Cơ sở dữ liệu | ✓ | Ep01 |

## 3. Mạng, API, bảo mật, thông báo — ôn ở Ep28

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| **DNS** | Domain Name System | Đổi tên miền ra địa chỉ IP | ✓ | Ep07 |
| **IP** | Internet Protocol (address) | Địa chỉ của máy trên mạng; WAF thường giới hạn theo IP | ✓ | Ep08 |
| **TCP** | Transmission Control Protocol | Giao thức truyền tin cậy, có bắt tay 1 RTT | ✓ | Ep01 |
| **UDP** | User Datagram Protocol | Giao thức không bắt tay, không đảm bảo thứ tự; HTTP/3 chạy trên nó | ✓ | Ep07 |
| **TLS** | Transport Layer Security | Mã hoá kết nối; TLS 1.3 bắt tay 1 RTT | ✓ | Ep01 |
| mTLS | mutual TLS | Cả hai phía cùng trình chứng chỉ; đối tác ngân hàng hay yêu cầu | ✓ | Ep08 |
| **HTTP** | Hypertext Transfer Protocol | Giao thức của web và REST API | ✓ | Ep00 |
| **HTTPS** | HTTP Secure | HTTP chạy trên TLS | ✓ | Ep07 |
| HTTP/1.1, HTTP/2, HTTP/3 | ba phiên bản HTTP | 1.1: một request một lúc mỗi connection; 2: ghép nhiều request (multiplexing) trên TCP; 3: chạy trên QUIC | `HTTP one point one`, `HTTP two`, `HTTP three` | Ep07 |
| QUIC | (tên riêng, gốc là Quick UDP Internet Connections) | Giao thức truyền trên UDP của HTTP/3; mất gói chỉ chặn đúng stream đó | ✓ (*quick*) | Ep07 |
| **NAT** | Network Address Translation | Nhiều máy dùng chung một IP công khai; chặn theo IP dễ chặn oan | ✓ | Ep08 |
| **LB** | Load Balancer | Bộ chia tải tới nhiều instance | ✓ | Ep06 |
| L4, L7 | OSI Layer 4 (transport), Layer 7 (application) | Load balancer chỉ nhìn IP/port (L4) hay đọc được URL, header (L7). **Khác** bậc L0–L4 của Track S | `layer four`, `layer seven` | Ep06 |
| ALB, NLB | Application / Network Load Balancer | Load balancer L7 và L4 của AWS | ✓ | Ep08 |
| **WAF** | Web Application Firewall | Tường lửa tầng HTTP: chặn bot, tấn công, giới hạn theo IP | ✓ | Ep08 |
| DDoS | Distributed Denial of Service | Tấn công từ chối dịch vụ bằng rất nhiều máy | `dee-doss` | Ep08 |
| **CDN** | Content Delivery Network | Mạng máy chủ đặt gần user, giữ bản sao file tĩnh | ✓ | Ep06 |
| **URL** | Uniform Resource Locator | Địa chỉ web; đề "URL shortener" | ✓ | Ep00 |
| **REST** | Representational State Transfer | Kiểu API theo tài nguyên + HTTP method, thường trả JSON | ✓ | Ep09 |
| **RPC**, gRPC | Remote Procedure Call; gRPC Remote Procedure Calls | Gọi hàm ở máy khác; gRPC là framework RPC trên HTTP/2 + protobuf | ✓ | Ep06 |
| gRPC-Web | gRPC for browsers | Bản gRPC trình duyệt gọi được (qua proxy) | ✓ | Ep09 |
| WS | WebSocket | Kết nối hai chiều giữ lâu; app chat giữ một kết nối mỗi user | `WebSocket` | Ep03 |
| **SSE** | Server-Sent Events | Server đẩy dữ liệu một chiều xuống trình duyệt qua HTTP | ✓ | Ep09 |
| **BFF** | Backend For Frontend | Một backend riêng cho mỗi loại client, gom nhiều lời gọi thành một | ✓ | Ep07 |
| MDN | MDN Web Docs (trước là Mozilla Developer Network) | Tài liệu web; tuần 2 đọc bài *An overview of HTTP* | ✓ | Ep07 |
| **JWT** | JSON Web Token | Token có chữ ký chứa thông tin đăng nhập; app không cần lưu session | ✓ | Ep06 |
| **OIDC** | OpenID Connect | Chuẩn đăng nhập dựa trên OAuth 2.0; Keycloak hỗ trợ | `O I D C` | Ep25 |
| **OTP** | One-Time Password | Mã dùng một lần gửi qua SMS hoặc app | ✓ | Ep08 |
| **SMS** | Short Message Service | Tin nhắn điện thoại | ✓ | Ep06 |
| **FCM** | Firebase Cloud Messaging | Dịch vụ push của Google cho Android | ✓ | Ep24 |
| APNs | Apple Push Notification service | Dịch vụ push của Apple cho iOS | ✓ | Ep24 |
| **DLQ** | Dead-Letter Queue | Hàng đợi chứa message đã thử nhiều lần vẫn lỗi, để xử lý tay hoặc đối soát | ✓ | Ep24 |
| **VM** | Virtual Machine | Máy ảo; bậc L0 là một VM | ✓ | Ep06 |

## 4. Dữ liệu và cache — ôn ở Ep29

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| **SQL** | Structured Query Language | Ngôn ngữ truy vấn database quan hệ | ✓ | Ep09 |
| NoSQL | Not only SQL | Nhóm database không theo mô hình bảng quan hệ (Cassandra, DynamoDB…) | ✓ | Ep13 |
| **PK** | Primary Key | Khoá chính của bảng | ✓ | Ep18 |
| **KV** | Key-Value (store) | Kho lưu dạng khoá → giá trị | ✓ | Ep18 |
| B-tree | Balanced tree | Cấu trúc index của Postgres, MySQL: sửa tại chỗ, đọc theo khoá ổn định | ✓ | Ep13 |
| **LSM**-tree | Log-Structured Merge-tree | Ghi tuần tự vào bộ nhớ rồi xả ra file; ghi rất nhanh (Cassandra, RocksDB) | ✓ | Ep03 |
| SSTable | Sorted String Table | File bất biến đã sắp xếp mà LSM-tree xả ra đĩa | ✓ | Ep13 |
| memtable | memory table | Bảng trong RAM của LSM-tree trước khi xả ra SSTable | `mem table` | Ep13 |
| BRIN | Block Range Index | Index rất nhỏ của Postgres cho cột tăng dần theo thời gian | ✓ | Ep17 |
| **MVCC** | Multi-Version Concurrency Control | Giữ nhiều phiên bản của một dòng để đọc không chặn ghi (Postgres) | ✓ | Ep14 |
| **SSI** | Serializable Snapshot Isolation | Cách Postgres cài mức Serializable: phát hiện xung đột rồi huỷ một transaction | ✓ | Ep14 |
| tx | transaction | Viết tắt trong tài liệu tiếng Việt ("hai tx cùng đọc") | `transaction` | Ep14 |
| **LSN** | Log Sequence Number | Vị trí trong nhật ký ghi (write-ahead log) của Postgres; dùng để biết replica đã theo kịp tới đâu | ✓ | Ep19 |
| **CAP** | Consistency, Availability, Partition tolerance | Khi mạng bị chia cắt phải chọn nhất quán hoặc sẵn sàng | `cap` | Ep19 |
| **PACELC** | if Partition: Availability or Consistency; Else: Latency or Consistency | Thêm vế lúc bình thường: chọn độ trễ hay nhất quán | `pass-elk` | Ep19 |
| **CDC** | Change Data Capture | Đọc log thay đổi của database để phát sự kiện | ✓ | Ep06 |
| **CQRS** | Command Query Responsibility Segregation | Tách mô hình ghi và mô hình đọc (read model riêng) | ✓ | Ep25 |
| **DDIA** | *Designing Data-Intensive Applications* (Martin Kleppmann) | Sách đọc tuần 3–4 | `D D I eigh` (*ay* bị đọc thành *eye*) | Ep13 |
| **NTP** | Network Time Protocol | Đồng bộ đồng hồ; có thể kéo đồng hồ lùi, làm Snowflake sinh trùng | ✓ | Ep18 |
| INCR, EXPIRE, PEXPIRE | increment; expire (giây); P = milliseconds | Lệnh Redis: tăng bộ đếm; đặt hạn sống của key | `increment`, `expire`, `P expire` | Ep11 |
| HMGET, HSET | hash multi-get, hash set | Lệnh Redis đọc, ghi nhiều trường của một hash | ✓ | Ep11 |
| SET … NX PX | Not eXists; expiry in milliseconds | Đặt key chỉ khi chưa có, kèm hạn sống: cách lấy khoá phân tán | ✓ | Ep22 |

## 5. Cloud và vận hành — ôn ở Ep28

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| **AWS** | Amazon Web Services | Nhà cung cấp cloud dùng trong lộ trình | ✓ | Ep03 |
| **AZ** | Availability Zone | Một trung tâm dữ liệu độc lập trong một region; AZ-a, AZ-b là tên từng zone | ✓ (AZ-a: `AZ eigh`) | Ep01 |
| Multi-AZ | multiple Availability Zones | Chạy bản dự phòng ở AZ khác; Postgres Multi-AZ chờ thêm 1–2 ms mỗi commit | ✓ | Ep01 |
| **S3** | Simple Storage Service | Object storage của AWS: ảnh, video, file sao kê | ✓ | Ep03 |
| **SQS** | Simple Queue Service | Hàng đợi của AWS; một lựa chọn thay Kafka ở bậc L2 | ✓ | Ep25 |
| OTel | OpenTelemetry | Chuẩn mở để thu trace, metric, log | ✓ (*O tel*) | Ep25 |
| **SaaS** | Software as a Service | Phần mềm bán dạng thuê bao trên cloud; capstone B | `sass` | Ep00 |
| C4 | C4 model (Context, Containers, Components, Code) | Cách vẽ kiến trúc theo 4 mức zoom; dùng cho bản vẽ V8 | ✓ | Ep06 |
| PgBouncer | Postgres connection pooler | Đứng giữa app và Postgres để nhiều pod chia ít connection thật | ✓ | Ep16 |
| **PDF** | Portable Document Format | File sao kê | ✓ | Ep06 |

## 6. Mã của lộ trình, tên riêng, viết tắt tiếng Việt — ôn ở Ep29

| Viết tắt | Đầy đủ | Nghĩa | Tom đọc | Video |
|---|---|---|---|---|
| GĐ1 … GĐ4 | giai đoạn 1 … 4 | Phase 1 … 4 của lộ trình 24 tuần | `phase one` | Ep00 |
| Track P | Performance track | Code Java chậm dưới tải cao, file [01](../01-java-code-cham-duoi-tai-cao.md) | ✓ | Ep00 |
| Track S | Scale / drawing track | Vẽ hệ thống 100k → 10M users, file [02](../02-ve-he-thong-100k-1m-10m.md) | ✓ | Ep00 |
| P01 … P20 | anti-pattern 1 … 20 | Mã các đoạn code xấu của Track P. Giai đoạn 1 dùng P01–P10, P15, P16, P18, P19 | `P one` … | Ep00 |
| D09, D10, D15, D18, D19 | demo 9, 10, … | Demo chạy được trong `perf-lab`, số trùng với mã P | `D nine` … | Ep00 |
| V1 … V10 | drawing 1 … 10 | Mười bản vẽ của Track S. Giai đoạn 1 vẽ V1–V5 | `V one` … | Ep00 |
| L0 … L4 | scale level 0 … 4 | Năm bậc kiến trúc theo quy mô. **Khác** L4/L7 của load balancer | `L zero` … (nói rõ *level*) | Ep00 |
| Hồ sơ A, B | load profile A (ngân hàng), B (chat) | Bộ giả định mặc định của công thức 4 bước | `profile eigh`, `profile B` | Ep02 |
| Họ A, B, G | problem family A, B, G | Họ bài trong [katalon-system-design](../../katalon-prep/katalon-system-design/README.md): A đếm và tổng hợp theo thời gian, B chia tài nguyên hữu hạn cho công bằng, G đọc → tính → ghi tranh chấp | `family eigh` … | Ep03 |
| Lab 1A … 4 | lab tuần 1 phần A … | Bài thực hành có tiêu chí chấm trong file 10 | ✓ | Ep00 |
| BT 1.4 | bài tập 1.4 | Exercise 1.4 của file 10; video nói *exercise one point four* | `exercise one point four` | Ep00 |
| § | mục (section) | §1.2 là mục 1.2 | `section` | Ep00 |
| Ep00 … Ep29 | episode 0 … 29 | Mã video của bộ này | `episode zero` … | Ep00 |
| STAR | Situation, Task, Action, Result | Khung kể một câu chuyện kinh nghiệm trong phỏng vấn | ✓ | Ep00 |
| CV | Curriculum Vitae | Hồ sơ xin việc | ✓ | Ep00 |
| HQ | headquarters | Trụ sở chính | ✓ | Ep00 |
| NAB | National Australia Bank | Một trong hai hướng ứng tuyển; app ở Việt Nam gọi hệ thống ở Úc | ✓ | Ep00 |
| TP.HCM | Thành phố Hồ Chí Minh | Ho Chi Minh City | `Ho Chee Minh City` | Ep01 |
| VN | Việt Nam | Vietnam | `Vietnam` | Ep01 |
| VND, USD | Vietnamese đồng, US dollar | Mã tiền tệ (ISO 4217) | `Vietnamese dong`, `US dollars` | Ep09 (VND), Ep21 (USD) |
| Vol 1 | Volume 1 | *System Design Interview, Volume 1* của Alex Xu | `Volume one` (tác giả: `Alex Shoo`) | Ep01 |

---

## Đếm

| Nhóm | Số dòng | Video ôn |
|---|---:|---|
| 1. Tải, độ trễ, đơn vị, độ tin cậy | 31 | Ep27 |
| 2. Java, JVM, Spring, code | 31 | Ep27 |
| 3. Mạng, API, bảo mật, thông báo | 33 | Ep28 |
| 4. Dữ liệu và cache | 22 | Ep29 |
| 5. Cloud và vận hành | 10 | Ep28 |
| 6. Mã lộ trình, tên riêng, tiếng Việt | 21 | Ep29 |
| **Tổng** | **148 dòng** (một số dòng gộp nhiều chữ, ví dụ ns, µs, ms) | Ep27: 62 · Ep28: 43 · Ep29: 43 |

Thêm chữ mới vào bảng khi viết kịch bản gặp chữ chưa có; ghi vào nhật ký của
[kế hoạch](00-ke-hoach-va-lich-su.md#9-nhật-ký--lịch-sử-đã-làm).
