# ops-lab — lời giải tham khảo giai đoạn 3, đã chạy

> **Tự làm lab tuần 12 trước, mở thư mục này sau.** Đây là bản tham khảo cho
> [30 — Implement giai đoạn 3](../30-implement-gd3-microservices-cloud.md): quy trình *load test → tìm
> điểm nghẽn → sửa → đo lại* trên một stack **thật**, và demo retry storm.

| Thành phần | Là gì |
|---|---|
| [`OpsLabApplication`](src/main/java/com/prep/ops/OpsLabApplication.java) | Khởi động PostgreSQL 16 thật (embedded, không Docker), một "đối tác" HTTP giả trả lời sau 50 ms ở cổng 18081, rồi Spring Boot 3.3 ở cổng 8080 với HikariCP pool 10 |
| [`TransferService`](src/main/java/com/prep/ops/TransferService.java) | Cùng một nghiệp vụ chuyển tiền viết hai cách: `/transfers/bad` gọi đối tác **bên trong** `@Transactional` (P09), `/transfers/good` gọi trước rồi mới mở transaction ngắn |
| [`TransferSimulation`](src/test/java/com/prep/ops/TransferSimulation.java) | Gatling 3.11 (Java DSL), **mô hình mở** `constantUsersPerSec`: request đến theo tốc độ cố định, không chờ request trước, nên không dính *coordinated omission* |
| [`RetryStorm`](src/main/java/com/prep/ops/RetryStorm.java) | Resilience4j: 4 chính sách gọi một downstream đang sự cố, đo khuếch đại tải và tỉ lệ thành công |
| [`terraform/`](terraform/) | Khung AWS cho tuần 15–16: VPC 2 AZ, ALB, ECS Fargate có tự rollback, RDS PostgreSQL 16 mã hoá + mật khẩu trong Secrets Manager, **budget alert**. Đã `terraform validate` + `fmt -check` (Terraform 1.16.5, provider AWS 6.67.0), **chưa apply** |

```bash
mvn -q package -DskipTests
java -jar target/ops-lab.jar                                     # terminal 1: app + Postgres + đối tác giả
mvn -q test-compile gatling:test -Dpath=/transfers/bad -Drps=250 -Dseconds=30    # terminal 2
curl -s localhost:8080/actuator/metrics/hikaricp.connections.pending             # terminal 3, trong lúc chạy
VT=true java -jar target/ops-lab.jar                             # cùng app, bật virtual threads
java -jar target/ops-lab.jar retry-storm                         # không cần Postgres
```

Postgres không chạy dưới user `root`: trong container Linux, chạy `java -jar` bằng một user thường.

## Kết quả (07/10/2026, output đầy đủ: [results/2026-10-07.txt](results/2026-10-07.txt))

App, Gatling và Postgres chạy **cùng một máy 4 vCPU**, nên số tuyệt đối không dùng được cho production;
hình dạng và tỉ lệ mới là thứ đáng tin.

**P09 trên stack thật.** Trần lý thuyết của `/transfers/bad`: 10 connection ÷ ~53 ms giữ ≈ **188 req/s**.

| Lượt | Tải | p50 | p99 | Lỗi | Thông lượng thực | Hikari chờ (max) |
|---|---:|---:|---:|---:|---:|---:|
| good, platform threads | 250 rps | 54 ms | 103 ms | 0 | 242 req/s | 0 |
| **bad**, platform threads | **150 rps** | 54 ms | **105 ms** | 0 | 145 req/s | 0 (active 9/10) |
| **bad**, platform threads | **250 rps** | **5,6 s** | **11,1 s** | 0 | **183 req/s** | **189** |
| good, platform threads | 800 rps | 53 ms | 511 ms | 0 | 774 req/s | 0 |
| **bad, virtual threads** | 250 rps | **22 s** | **30,1 s** | **18,6–19,9%** | — | **4.260–4.483** |
| good, virtual threads | 250 rps | 53 ms | 107 ms | 0 | 242 req/s | 0 |

Ba điều đọc ra:

1. **Ở 150 rps bản xấu trông y hệt bản sửa.** Load test ở tải thấp không bắt được P09. Phải đẩy
   **vượt** trần mới thấy: thông lượng dừng ở 183 req/s (lý thuyết 188), p99 nhảy từ 0,1 s lên 11 s.
2. **Little's Law đúng tới từng connection:** 150 req/s × 0,057 s = 8,6, đo được active tối đa **9/10**.
3. **Virtual threads làm bản xấu tệ hơn.** Tomcat không còn giới hạn 200 thread, nên mọi request
   đều vào và xếp hàng ở Hikari (hơn 4.000 request chờ). Request chờ quá `connectionTimeout` 30 s thì lỗi 500.
   Trần của pool vẫn y nguyên.

**Retry storm** (downstream 2.000 req/s, sự cố 2 giây, client 1.000 req/s trong 8 giây, 2 lần chạy gần như trùng nhau):

| Chính sách | Lần gọi vào downstream trong 2 s sự cố | Thành công toàn bộ | Thành công 500 ms đầu sau hồi phục | p99 |
|---|---:|---:|---:|---:|
| Không retry | 2.000 | 74,7% | 95,2–95,4% | 100 ms |
| Retry 3 lần ngay lập tức | **7.934** (× 4) | 74,6% | 96,8–97,0% | 103 ms |
| Retry 3 lần, backoff mũ + jitter | 6.829–6.842 | **83,7–83,8%** | 100% | **945 ms** |
| Backoff + jitter + circuit breaker | **75** | 62,7–62,8% | **22,8–23,0%** | 122–125 ms |

Retry ngay lập tức nhân tải lên gấp 4 lần mà không cứu thêm request nào. Backoff + jitter cứu được
những request đến gần cuối sự cố (retry rơi vào lúc downstream đã sống lại), đổi lại p99 gần 1 giây.
Circuit breaker cắt 96% tải vào downstream đang chết, nhưng vẫn mở thêm một lúc sau khi downstream đã
hồi phục: **breaker bảo vệ downstream, không bảo vệ client.** Mô hình này có một giới hạn: downstream
quá tải thì **từ chối ngay**, không chậm đi, nên nó không tái hiện vòng xoáy "chậm → timeout → retry →
chậm hơn" của sự cố thật.

## Terraform (tuần 15–16)

```bash
cd terraform
terraform init
terraform plan -var app_image=<ecr-image> -var budget_email=<email>
```

Khung có chủ đích **rẻ cho lab**: 1 NAT gateway, RDS không Multi-AZ, HTTP thay vì HTTPS, xoá được
không cần snapshot. Mỗi chỗ đánh đổi đó có một dòng chú thích "Production: …" ngay trong file. Đã kiểm
tra cú pháp và schema bằng `terraform validate`; chưa chạy `plan`/`apply` vì không có tài khoản AWS ở
đây, nên giá tiền và hành vi thật trên AWS chưa được kiểm chứng.
