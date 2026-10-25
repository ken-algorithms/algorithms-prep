# .NET 8 + JetBrains Rider trên MacBook: từ cài đặt tới dự án thật với PostgreSQL, Worker, Azure Service Bus và Docker

Bộ 12 bài cho developer **đã biết lập trình** (ví dụ Java) nhưng **mới** với .NET và Rider, muốn vào làm dự án càng
sớm càng tốt. Mỗi bài dạy lý thuyết ngay cạnh thực hành: giải thích một khái niệm, gõ code, chạy, nhìn output thật,
rồi cố tình làm hỏng để hiểu vì sao nó đúng. Sáu bài giữa dựng từ đầu một hệ thống nhỏ, **OrderFlow**: Web API ghi
đơn hàng vào PostgreSQL, Worker gửi sự kiện qua Azure Service Bus và xử lý nó, tất cả chạy bằng Docker, có 24 test.

> **Trước khi bắt đầu:** bộ bài viết và chạy kiểm chứng ngày 21/10/2026; .NET 8 chỉ được vá bảo mật tới **10/11/2026**. Học .NET 8 vẫn đúng vì
> rất nhiều dự án công ty đang chạy nó, và code của bộ bài chạy y nguyên trên .NET 10 (đã thử, [bài 12 §6](12-bat-dau-du-an-that.md)).
> Dự án **mới** thì nên bắt đầu thẳng bằng .NET 10.

---

## Danh sách bài

### Phần 1 · Công cụ: cài đặt, Rider, C#, debug, phím tắt

| # | Bài | Lý thuyết | Thực hành |
|---|---|---|---|
| 01 | [Cài đặt trên macOS: .NET 8 SDK, Rider, Docker Desktop](01-cai-dat-macos.md) | SDK và runtime, LTS và ngày hết hạn, chip Apple (arm64) hay Intel | Cài SDK bằng bộ cài `.pkg` (hoặc `dotnet-install.sh`), Rider qua Toolbox, Docker Desktop, `dotnet-ef`; checklist 7 lệnh |
| 02 | [Rider: solution, project, chương trình đầu tiên](02-rider-du-an-dau-tien.md) | Solution, project, `.csproj`, NuGet, so với Maven | Tạo solution trong Rider và bằng CLI, thêm thư viện, tham chiếu project, build Release, `publish` |
| 03 | [C# và .NET 8 cho người đến từ Java](03-csharp-cho-dev-java.md) | 10 nhóm cú pháp: `record`, nullable, LINQ, `async`/`await`, pattern matching, primary constructor | `playground/CSharpBasics`: mỗi nhóm một lệnh `dotnet run`, output thật đi kèm |
| 04 | [Debug trong Rider](04-debug-trong-rider.md) | Breakpoint thường, có điều kiện, exception breakpoint, evaluate, attach | `playground/DebugDrill`: tìm 5 lỗi cài sẵn bằng debugger |
| 05 | [Phím tắt Rider trên MacBook](05-phim-tat-rider-macos.md) | Keymap macOS, phím bị macOS chiếm và cách gỡ, gõ tiếng Việt không bị kẹt phím | 15 phím sống còn, bảng đầy đủ theo nhóm, bài luyện 10 thao tác trên OrderFlow |

### Phần 2 · Dựng OrderFlow từ đầu: API, PostgreSQL, Worker, Service Bus, Docker, test

| # | Bài | Lý thuyết | Thực hành |
|---|---|---|---|
| 06 | [ASP.NET Core Web API từ đầu](06-web-api-asp-net-core.md) | DI và vòng đời service, cấu hình, middleware, controller, validation, ProblemDetails | API đơn hàng chạy được, gọi bằng Swagger và file `.http` của Rider |
| 07 | [PostgreSQL với EF Core 8](07-postgresql-ef-core.md) | Entity, `DbContext`, migration, LINQ dịch ra SQL, transaction, UTC | Postgres trong Docker, migration đầu tiên, đơn và outbox ghi trong một transaction |
| 08 | [Background Worker và outbox](08-background-worker.md) | `BackgroundService`, `PeriodicTimer`, scope DI, tắt máy êm, `FOR UPDATE SKIP LOCKED` | Worker gửi outbox theo lô, hai worker không gửi trùng, thí nghiệm exception làm sập host |
| 09 | [Azure Service Bus](09-azure-service-bus.md) | Queue, topic, peek-lock, retry, dead-letter, duplicate detection, idempotency | Emulator chạy bằng Docker, 7 thí nghiệm SDK, consumer idempotent, đóng băng và mất kết nối broker |
| 10 | [Docker hoá cả hệ thống](10-docker-hoa.md) | Multi-stage build, image .NET 8 (cổng 8080, user không phải root), compose có profile | Hai Dockerfile, compose chạy migrate rồi api và worker, smoke test, Rider + Docker |
| 11 | [Test: xUnit, Testcontainers, Rider](11-test-xunit-testcontainers.md) | xUnit cho người quen JUnit, fixture, chạy song song, vì sao test trên Postgres thật, coverage | 24 test, `WebApplicationFactory`, test race tất định bằng interceptor, chạy và debug test trong Rider |

### Phần 3 · Vào dự án thật

| # | Bài | Lý thuyết | Thực hành |
|---|---|---|---|
| 12 | [Bắt đầu dự án thật](12-bat-dau-du-an-that.md) | Checklist ngày đầu, bí mật và nguồn cấu hình, `Directory.Build.props`, quản lý version tập trung, .NET 10 | Kiểm package lỗ hổng, user-secrets, script dựng service mới trong 1 phút, nâng OrderFlow lên .NET 10 |

---

## Học theo thứ tự nào

**Đầy đủ (khoảng 17 giờ, gồm cả làm):** 01 → 12 theo thứ tự. Mỗi bài dùng kết quả của bài trước.

**Nhanh nhất để vào dự án (1–2 ngày):** 01 → 02 → [05 §2](05-phim-tat-rider-macos.md) (15 phím sống còn) → 06 → 07 →
08 → 10 → [12 §1](12-bat-dau-du-an-that.md) (checklist ngày đầu). Gặp cú pháp lạ thì mở 03; app chạy sai thì mở 04;
dự án dùng Service Bus thì học 09; viết code thật đầu tiên thì học 11 trước.

**Được giao dựng service mới ngay:** 01 → 02 → [12 §3](12-bat-dau-du-an-that.md): một lệnh sinh Api + Core + Worker +
test, build 0 warning; sau đó học 06–11 để đắp từng phần lên khung đó.

---

## Lab

Code hoàn chỉnh và mọi output thật nằm ở [`lab/`](lab/README.md):

| | |
|---|---|
| [`lab/OrderFlow/`](lab/README.md) | Solution cuối cùng của bài 06–12: 5 project, `docker-compose.yml`, 24 test, `results/` chứa output thật |
| `lab/playground/` | `CSharpBasics` (bài 03), `DebugDrill` (bài 04), `ServiceBusDemo` (bài 09) |
| `lab/steps/` | File bản tạm của bài 06–09 và `verify-steps.sh`: dựng lại OrderFlow từ thư mục trống đúng theo lệnh trong bài, so với lab |
| `lab/bootstrap/` | `new-dotnet8-service.sh` của bài 12 |

Tự gõ theo bài trong một thư mục riêng; dùng `lab/` để so khi bị kẹt.

---

## Quy ước trong bài

- 📄 **`lab/...`** ngay trên một khối code: khối đó là **nguyên văn** file trong lab, chép vào là chạy. Một test của
  repo kiểm tra điều này mỗi lần chạy.
- ✂️ **`lab/...`** (trích): một **đoạn** của file đó.
- Khối `text` ngay sau một lệnh là **output thật** khi chạy lệnh đó, chỉ rút gọn đường dẫn hoặc bỏ bớt dòng lặp.
- Phím tắt ghi theo **keymap macOS** của Rider: ⌘ Command, ⌥ Option, ⌃ Control, ⇧ Shift.
- Cuối mỗi bài có **Lỗi hay gặp**, **Tự kiểm tra** và **Ranh giới trung thực**: cái gì đã chạy thật, cái gì chỉ viết
  theo tài liệu.

## Đọc trên web

Tab **Tutorial** của bản một trang (`web/index.html`, dựng bằng `web/build.py`): bấm một bài để mở trong popup, mỗi
khối code có nút **Copy**, tick **Đã học** để theo dõi tiến độ (lưu trong trình duyệt).

## Ranh giới trung thực của cả bộ

- **Đã chạy thật** trên Linux x64 (Ubuntu 24.04, .NET SDK 8.0.426, Docker): mọi lệnh `dotnet`, mọi code, 24 test,
  Docker compose, Service Bus emulator, nâng lên .NET 10, script dựng service; `verify-steps.sh` dựng lại lab từ thư
  mục trống và ra đúng từng file.
- **Chưa chạy trên macOS.** Bước cài đặt (bài 01), giao diện Rider (menu, hộp thoại, cửa sổ) và Docker Desktop viết
  theo tài liệu chính thức và bản Rider gần đây; tên menu có thể lệch chút giữa các phiên bản. Phím tắt kiểm theo file
  keymap chính thức của IntelliJ Platform (bài 05).
- **Chưa chạy trên Azure thật:** Managed Identity, Key Vault, Service Bus ngoài emulator.
