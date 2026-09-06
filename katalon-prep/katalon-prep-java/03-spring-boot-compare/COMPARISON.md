# Quarkus vs Spring Boot — bảng số của CHÍNH BẠN

> Chưa chạy. Chạy `./bench.sh` rồi điền vào. **Mang bảng này vào phỏng vấn.**
>
> Ứng viên nói *"Quarkus nhanh hơn"* thì nhiều. Ứng viên có **số đo của chính mình** thì rất ít.

Điều kiện đo: MacBook (JDK 21.0.12 Temurin), Quarkus 3.28.5 vs Spring Boot 3.3.13, cùng API,
cùng PostgreSQL 16, client cùng máy, warm-up 5s, đo 30s.

| | Quarkus JVM | Quarkus native | Spring Boot | Spring Boot + virtual threads |
|---|---|---|---|---|
| Startup time | | | | |
| RSS lúc idle | | | | |
| p50 @200 conc | | | | |
| p95 @200 conc | | | | |
| p99 @200 conc | | | | |
| Throughput (req/s) | | | | |
| Kích thước artifact | | | | |

## Cách lấy từng số

```bash
# 1. Startup + RSS + latency (tự động cả 2 framework)
CONC=200 SECS=30 ./bench.sh

# 2. Quarkus native image (cần GraalVM hoặc container-build)
cd ../02-quarkus-service
./mvnw package -Dnative -Dquarkus.native.container-build=true   # cần Docker
# Không có Docker: cài GraalVM qua sdkman rồi
#   sdk install java 21.0.2-graalce && ./mvnw package -Dnative

# 3. Spring Boot bật/tắt virtual threads
# sửa spring.threads.virtual.enabled trong application.properties rồi chạy lại
```

## Kết luận cần rút ra (điền sau khi có số)

1. **Startup**: chênh bao nhiêu lần? Ảnh hưởng gì tới scale-to-zero và rolling deploy?
2. **RSS**: chênh bao nhiêu MB? Với 50 service trên K8s thì tiết kiệm bao nhiêu node?
3. **p95/p99 dưới tải**: có khác biệt đáng kể không, hay thực ra bottleneck nằm ở **Postgres**?
   → Nếu bottleneck là DB thì việc chọn framework **không quan trọng bằng** việc tuning query.
   Nói ra được điều này là tín hiệu Lead: biết chỗ nào **không** đáng tối ưu.

4. **Virtual threads** giúp bao nhiêu với endpoint blocking? Có chạm trần **connection pool** không?
   → Nhắc lại quy tắc #3 ở [module 04](../04-concurrency/): virtual thread bỏ giới hạn về *thread*,
   không bỏ giới hạn về *connection pool*.
