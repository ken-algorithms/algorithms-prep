# katalon-prep-common — tài liệu chung cho cả hai ngôn ngữ

**Folder này chỉ chứa tài liệu, không chứa code.** Đó là có chủ ý: design pattern viết bằng Java
và bằng Python nhìn *khác nhau* (`interface` vs `Protocol`, Decorator class vs `@decorator`), nên
nhét code Java vào một folder tên "common" là sai sự thật.

Ranh giới:

```text
common/                 khái niệm + so sánh hai ngôn ngữ + quy trình + câu trả lời phỏng vấn
katalon-prep-java/      code chạy được, 191 test  (Maven, JDK 21)
katalon-prep-python/    code chạy được, 122 test  (uv, Python 3.12+)
```

## Bốn tài liệu

| File | Nội dung | Đọc khi nào |
|---|---|---|
| **[04-ai-agent-system-design.md](04-ai-agent-system-design.md)** ⭐ | 7 cách agent chết · **prompt injection từ DOM** · test hệ không tất định · RAG hybrid · chi phí · grounding | **Đọc trước.** TrueTest là sản phẩm lõi của Katalon |
| [01-clean-code-solid.md](01-clean-code-solid.md) | SOLID hai ngôn ngữ · 4 ranh giới thiết kế · 14 vấn đề + số hiệu rule · **7 lần tôi đoán sai** | Trước khi mở `before/` ở cả hai bên |
| [02-design-patterns.md](02-design-patterns.md) | **Bảng dịch Java ↔ Python** · 6 pattern on-domain · case study Strategy sai→đúng | Khi luyện pattern |
| [03-system-design.md](03-system-design.md) | Quy trình 45 phút · ước lượng · primitive · testability | Trước vòng system design |

Ba bài design đầy đủ (TrueTest, Distributed Execution, Real-time Analytics) nằm ở
[tài liệu chiến lược §5](../katalon-senior-lead-phong-van.md#5-ba-bài-system-design-on-domain) —
không nhân bản ở đây.

## Ba thứ xuyên suốt cả hai ngôn ngữ

Nhận ra ba mẫu này lặp lại ở nhiều chỗ khác nhau là thứ đáng nói trong phỏng vấn — nó cho thấy
bạn nghĩ ở tầng nguyên tắc, không phải tầng thư viện.

**1. Ngưỡng tin cậy ba mức, không phải hai.**
`auto` / `review` / `reject`. Dưới ngưỡng thì **đề xuất**, không tự áp dụng. Xuất hiện ở:
self-healing locator (Java, ngưỡng 70) và agent sinh test case (Python, ngưỡng 80). Lý do giống
hệt nhau: một lần tự động sửa sai vào test suite của khách là mất niềm tin vĩnh viễn.

**2. Tiêm đồng hồ và bộ sinh ngẫu nhiên.**
Không primitive nào gọi `now()` hay `random()` trực tiếp. Đó là lý do duy nhất khiến retry,
timeout, circuit breaker, rate limit test tất định được — và là câu trả lời cho *"làm sao bạn
test được những thứ đó?"*

**3. Điều kiện CẦN phải nhân, điều kiện TỐT mới được cộng.**
Bài học từ `grounding_score`: thang điểm cộng dồn cho một test case **bịa hoàn toàn** 50/100.
Trộn hai loại điều kiện vào một phép cộng là cách dễ nhất để tạo ra con điểm trông hợp lý mà
hoàn toàn vô dụng.

## Thứ tự học

```text
04 (AI agent) ─┬─→ katalon-prep-python/src/prep/agent/     ⭐ ưu tiên cao nhất
               │
01 (SOLID) ────┼─→ katalon-prep-java/01-clean-code-solid/
02 (patterns) ─┘   katalon-prep-python/src/prep/clean_code/
               │
03 (sysdesign)─┴─→ katalon-prep-java/08-system-design/
                   katalon-prep-python/src/prep/distributed/
```

Đọc tài liệu **rồi** chạy test, không làm ngược lại. Tài liệu cho biết *vì sao*; test chứng minh
*rằng nó đúng*. Chạy test mà không hiểu vì sao thì chỉ là xem đèn xanh.
