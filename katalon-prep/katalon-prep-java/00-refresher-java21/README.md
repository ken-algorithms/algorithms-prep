# Module 00 — Ôn lại Java 21 (dạng test tự kiểm tra)

```bash
mvn -pl 00-refresher-java21 test    # 15 test pass
```

**Cách dùng:** mở [`ResultReporterTest.java`](src/test/java/com/prep/refresher/ResultReporterTest.java),
đọc `@DisplayName`, **tự đoán kết quả trước khi xem assert**. Chỗ nào đoán sai chính là lỗ hổng
thật sau 1 năm không viết Java — ghi lại.

**Viết tay, tắt Copilot.** Đây là module để lấy lại tốc độ gõ Java cho vòng live coding.

## Nội dung

| Chủ đề | File | Bẫy được dạy |
|---|---|---|
| **Sealed interface + record** | `TestResult.java` | Vì sao sealed thay vì enum + field nullable; exhaustiveness → thêm loại mới thì **compile error** thay vì chạy sai |
| **Pattern matching for switch** (Java 21) | `ResultReporter.describe` | Record deconstruction, **guarded pattern** (`when`), thứ tự case (nhánh có `when` phải đứng trước, không thì "dominated by preceding case label") |
| **Record invariant** | `Failed`, `Flaky` | Compact constructor để validate; **record KHÔNG tự động immutable ở nội dung collection** → phải `List.copyOf` |
| **`Collectors.toMap`** | `indexByNameLastWins` | Không có merge function → **ném** `IllegalStateException` khi trùng key. Bug tiềm ẩn dạng này có trong code RCI của bạn |
| **`Set.of` / `Map.of`** | test `immutableFactories...` | **Không dedupe — chúng NÉM** khi trùng. Khác `List.of` |
| **`groupingBy`** | `groupByOutcome` | Không tạo key rỗng → `.get("FAILED").size()` là NPE. Phải `getOrDefault` |
| **`orElse` vs `orElseGet`** | `slowest` | `orElse(expensive())` luôn eval. Và `Duration.ZERO` là **field** nên không viết được `Duration::ZERO` |
| **Chia cho 0** | `flakinessRate` | Trả `0.0` chứ không `NaN` — `NaN` làm vỡ mọi dashboard phía sau |
| **Text block** | `summary` | `.formatted()`, strip indent tự động |

## Checklist

- [ ] Giải thích được exhaustiveness của sealed interface và vì sao nó tốt hơn `default:`
- [ ] Viết được `switch` pattern matching có `when` mà không cần tra cứu
- [ ] Nói được 3 điểm khác nhau giữa `List.of`, `Set.of`, `new HashSet<>()`
- [ ] Nhớ luôn truyền merge function cho `Collectors.toMap`
