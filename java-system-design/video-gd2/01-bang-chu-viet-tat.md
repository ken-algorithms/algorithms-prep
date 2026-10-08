# Bảng chữ viết tắt — giai đoạn 2 (tuần 5–10): chữ mới

> Chỉ gồm chữ viết tắt **chưa có** trong [bảng của giai đoạn 1](../video-gd1/01-bang-chu-viet-tat.md)
> mà phần giai đoạn 2 của lộ trình dùng tới: [20](../20-implement-gd2-du-lieu-phan-tan.md) (toàn bộ),
> phần giai đoạn 2 và capstone của [00](../00-lo-trinh-6-thang.md), P20 của
> [01](../01-java-code-cham-duoi-tai-cao.md), §3.3 và §5 của [02](../02-ve-he-thong-100k-1m-10m.md).
> Video giai đoạn 2 được kiểm với **cả hai** bảng (`check --glossary … --glossary …`); thẻ *Acronyms in
> this video* của từng video vẫn liệt kê mọi chữ video đó dùng, kể cả chữ cũ.

**Cách đọc bảng**

- **Emma đọc**: `✓` là Kokoro đọc đúng khi để nguyên; nếu không thì là cách `tools/lesson_video/speak.py`
  đổi chữ trước khi đọc, hoặc cách viết trong `say` (kiểm bằng bộ tách âm của Kokoro ngày 08/10/2026, xem
  [kế hoạch, mục 5](00-ke-hoach-va-lich-su.md#5-phát-âm)).
- **Video**: video đầu tiên dùng và giải thích chữ đó.
- Không đưa vào bảng: tên node trong sơ đồ mermaid (`ORC`, `LED`, `GW`…), trạng thái viết hoa trong code
  (`PENDING`, `COMPLETED`…), từ khoá SQL và lệnh Redis (`VACUUM`, `MOVED`).

---

## 1. Replication, partitioning, đồng thuận

| Viết tắt | Đầy đủ | Nghĩa | Emma đọc | Video |
|---|---|---|---|---|
| **LWW** | Last-write-wins | Giải xung đột bằng cách giữ bản ghi có timestamp lớn nhất; lệnh ghi kia **mất im lặng** | ✓ | Ep01 |
| **CRDT** | Conflict-free replicated data type | Kiểu dữ liệu tự gộp được khi hai replica cùng sửa (bộ đếm, tập hợp…), không cần chọn bên thắng | ✓ | Ep01 |
| KRaft | Kafka Raft | Chế độ Kafka tự bầu controller bằng Raft; từ Kafka 4.0 là chế độ duy nhất, không còn ZooKeeper | ✓ (*K raft*) | Ep00 |
| etcd | (tên riêng: "/etc" + "d" của distributed) | Kho khoá–giá trị dùng Raft, nơi Kubernetes lưu trạng thái | `et-see-dee` | Ep19 |

## 2. Transaction phân tán, thanh toán

| Viết tắt | Đầy đủ | Nghĩa | Emma đọc | Video |
|---|---|---|---|---|
| **2PC** | Two-phase commit | Commit hai pha: coordinator hỏi mọi bên "prepare", rồi mới "commit"; giữ khoá qua mạng suốt hai pha | ✓ (*two P C*) | Ep00 |
| **WAL** | Write-ahead log | Nhật ký ghi trước của Postgres; CDC (Debezium) đọc WAL để phát sự kiện | ✓ | Ep08 |
| **PSP** | Payment service provider | Cổng thanh toán (trừ thẻ thay merchant), ví dụ Stripe, Adyen | ✓ | Ep08 |
| **PCI DSS** | Payment Card Industry Data Security Standard | Chuẩn bảo mật dữ liệu thẻ; lưu số thẻ là phải gánh toàn bộ phạm vi của chuẩn | ✓ | Ep11 |
| **eKYC** | Electronic know your customer | Định danh khách hàng điện tử (ảnh giấy tờ, khuôn mặt) khi mở tài khoản | `e K Y C` | Ep10 |
| EM | Engineering manager | Quản lý kỹ thuật; ở NAB là người phỏng vấn vòng system design (saga, event-driven) | ✓ | Ep07 |

## 3. Kafka, stream processing

| Viết tắt | Đầy đủ | Nghĩa | Emma đọc | Video |
|---|---|---|---|---|
| **ISR** | In-sync replicas | Các replica đang theo kịp leader của một partition; `acks=all` nghĩa là "mọi replica trong ISR" | `I S R` | Ep12 |
| **DLT** | Dead-letter topic | Topic chứa message xử lý lỗi sau vài lần retry, kèm header lỗi (Spring Kafka: `<topic>.DLT`) | ✓ | Ep13 |
| **KIP** | Kafka Improvement Proposal | Đề xuất thay đổi Kafka, đánh số; KIP-848 là giao thức consumer group mới | ✓ | Ep13 |
| **OLAP** | Online analytical processing | Hệ phân tích dữ liệu lớn theo cột (ClickHouse, Druid) | ✓ (*oh-lap*) | Ep17 |
