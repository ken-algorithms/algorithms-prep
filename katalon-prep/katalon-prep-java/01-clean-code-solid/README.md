# Module 01 — Clean code / SOLID / Design patterns (flagship)

```bash
mvn -pl 01-clean-code-solid test    # 27 test pass
```

👉 **Hướng dẫn đầy đủ: [REVIEW.md](REVIEW.md)** — 14 vấn đề kèm rule Sonar thật, bảng đối chiếu
before/after, map SOLID vào code cụ thể, và phát hiện "hai bug che lấp nhau".

- [`src/main/java/com/prep/cleancode/before/`](src/main/java/com/prep/cleancode/before/) — code RCI
  thật của bạn (đã ẩn danh), **cố ý viết xấu, đừng sửa**

- [`src/main/java/com/prep/cleancode/after/`](src/main/java/com/prep/cleancode/after/) — refactor đúng
- [`src/test/.../before/BeforeFailureModesTest.java`](src/test/java/com/prep/cleancode/before/BeforeFailureModesTest.java)
  — 7 test chứng minh từng failure mode

- [`src/test/.../after/HealthAggregatorTest.java`](src/test/java/com/prep/cleancode/after/HealthAggregatorTest.java)
  — 20 test, **không Spring, không mock framework, không network**

⚠️ SonarLint sáng đèn đỏ ở `before/` là **đúng mục đích**. Đèn phải sạch ở `after/`.
