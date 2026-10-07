# dist-lab — lời giải tham khảo giai đoạn 2, đã chạy

> **Tự làm lab trước, mở thư mục này sau.** Đây là bản tham khảo cho các lab ở
> [20 — Implement giai đoạn 2](../20-implement-gd2-du-lieu-phan-tan.md). Bản bạn tự viết bằng Spring
> Boot sẽ dài hơn; ở đây chỉ giữ phần lõi để chứng minh thiết kế đúng.

| Class | Lab | Cần gì để chạy |
|---|---|---|
| [`ConsistentHash`](src/main/java/com/prep/dist/ConsistentHash.java) | Tuần 5–6: vòng băm + virtual node, đo số key phải chuyển | Không cần gì |
| [`Saga`](src/main/java/com/prep/dist/Saga.java) | Tuần 7: orchestration, bù theo thứ tự ngược | Không cần gì |
| [`OutboxLab`](src/main/java/com/prep/dist/OutboxLab.java) | Tuần 8–9: outbox cùng transaction với sổ cái, relay `SKIP LOCKED`, consumer idempotent | **Postgres 16 thật** (embedded, tự tải trong Maven) + **Kafka thật** ở `localhost:9092` |
| [`Fencing`](src/main/java/com/prep/dist/Fencing.java) | Tuần 10: khoá phân tán + GC pause, có và không có fencing token | Không cần gì |

```bash
mvn -q package
java -cp target/dist-lab.jar com.prep.dist.ConsistentHash
java -cp target/dist-lab.jar com.prep.dist.Saga
java -cp target/dist-lab.jar com.prep.dist.Fencing
java -cp target/dist-lab.jar com.prep.dist.OutboxLab            # cần Kafka, xem bên dưới
```

## Chạy Kafka không cần Docker

Kafka 4.x chỉ còn chế độ KRaft (không ZooKeeper), chạy thẳng từ bản tải về:

```bash
curl -O https://downloads.apache.org/kafka/4.1.2/kafka_2.13-4.1.2.tgz     # kiểm tra file .sha512 đi kèm
tar xzf kafka_2.13-4.1.2.tgz && cd kafka_2.13-4.1.2
bin/kafka-storage.sh format --standalone -t "$(bin/kafka-storage.sh random-uuid)" -c config/server.properties
bin/kafka-server-start.sh config/server.properties                         # cổng 9092
```

Có Docker thì `docker run -p 9092:9092 apache/kafka:4.1.2` thay cho cả khối trên (lệnh Docker chưa chạy thử ở đây vì sandbox không có Docker daemon).

**Postgres không chạy dưới user `root`.** Trên Mac không gặp. Trong container Linux chạy bằng root
thì chạy JVM bằng một user thường (`runuser -u <user> -- java …`).

## Kết quả đã chạy (07/10/2026)

Output đầy đủ: [results/2026-10-07.txt](results/2026-10-07.txt). Tóm tắt:

| Lab | Kết quả |
|---|---|
| ConsistentHash, 1 triệu key, 4 → 5 node | `hash mod N`: **80%** key phải chuyển. Vòng băm 100 vnode: **18,9%** (lý tưởng 20%), tải lệch max/min 1,11. 1 vnode: tải lệch **6,77 lần** |
| Saga | Bước 3 lỗi → bù bước 2 rồi bước 1; ví về lại 1.000, merchant 0 |
| OutboxLab, 1.000 lệnh chuyển, relay A crash sau khi gửi lô 3 | Topic có **1.100** message (100 trùng); 2 relay chạy song song gửi đúng 800 phần còn lại, không trùng thêm; consumer áp dụng **1.000**, bỏ qua **100**; tổng số dư không đổi, tổng sổ cái = 0. Hai lần chạy giống hệt |
| Fencing | Không token: client bị GC pause ghi đè giá trị mới bằng giá trị cũ. Có token: lệnh ghi cũ bị từ chối |
