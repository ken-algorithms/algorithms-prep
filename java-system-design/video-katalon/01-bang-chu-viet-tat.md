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
| **UTC** | Coordinated Universal Time | Giờ chuẩn quốc tế. Mọi bucket và partition cắt theo UTC (`date_bin` với gốc `+00`), giờ Việt Nam là UTC+7 | ✓ | K05 |
| **SDK** | Software Development Kit | Thư viện phía client gửi sự kiện: sinh `batch_id`, gom lô, retry có backoff + jitter, buffer có giới hạn | ✓ | K11 |
