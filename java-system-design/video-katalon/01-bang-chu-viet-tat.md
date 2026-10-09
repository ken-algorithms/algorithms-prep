# Bảng chữ viết tắt — video chen ngang Katalon

> Chỉ ghi chữ **mới** của bộ [video chen ngang](00-ke-hoach-va-lich-su.md). Mọi chữ khác đã có trong
> [bảng giai đoạn 1](../video-gd1/01-bang-chu-viet-tat.md) và [bảng giai đoạn 2](../video-gd2/01-bang-chu-viet-tat.md);
> lệnh `check` của bộ này kiểm với cả ba bảng.

**Cách đọc bảng**

- **Đọc**: `✓` là Kokoro đọc đúng khi để nguyên (đã kiểm bằng bộ tách âm của Kokoro); nếu không thì là chuỗi
  phải viết vào `say`.
- **Video**: video đầu tiên dùng và giải thích chữ đó. Video sau dùng lại vẫn giải thích lần đầu trong
  thẻ *Acronyms in this video* của nó.

| Viết tắt | Đầy đủ | Nghĩa | Đọc | Video |
|---|---|---|---|---|
| **PII** | Personally Identifiable Information | Dữ liệu định danh cá nhân (email, tên, số điện thoại). Bẫy của họ C: dữ liệu này rời client trước khi được redact | ✓ | K00 |
| **SDK** | Software Development Kit | Thư viện phía client gửi sự kiện: gom lô, sinh `batch_id`, retry có backoff + jitter, buffer có giới hạn | ✓ | K03 |
| **JMX** | Java Management Extensions | Chuẩn giám sát metric thời gian thực của JVM và Kafka (vd consumer lag, thread, memory) | ✓ | K13 |
