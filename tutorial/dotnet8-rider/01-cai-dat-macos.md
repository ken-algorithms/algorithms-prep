# Bài 01 — Cài đặt trên macOS: .NET 8 SDK, JetBrains Rider, Docker Desktop, công cụ dòng lệnh

> **Bạn sẽ làm được:** máy Mac có đủ .NET 8 SDK, Rider, Docker Desktop và `dotnet-ef`; kiểm tra từng thứ bằng
> **một lệnh** và biết khi lệnh đó sai thì sửa ở đâu.
>
> **Thời lượng:** đọc 20 phút · làm 40 phút (phần lớn là chờ tải)
>
> **Tiếp theo:** [Bài 02 — Rider: solution, project và chương trình đầu tiên](02-rider-du-an-dau-tien.md)

---

## 0. Bức tranh: cài những gì, để làm gì

Nếu bạn đến từ Java, mỗi món ở đây đều có một "người anh em" quen thuộc:

| Cài | Để làm gì | Tương đương bên Java |
|---|---|---|
| **.NET 8 SDK** | Biên dịch, chạy, test, đóng gói code C# | JDK |
| .NET Runtime (nằm sẵn trong SDK) | Chạy app đã biên dịch | JRE |
| `dotnet` CLI (nằm sẵn trong SDK) | Tạo project, thêm thư viện, build, run, test | `mvn` / `gradle` |
| **NuGet** (nuget.org) | Kho thư viện | Maven Central |
| **JetBrains Rider** | IDE | IntelliJ IDEA (cùng hãng, cùng nền tảng) |
| **Docker Desktop** | Chạy PostgreSQL, Service Bus emulator, đóng gói app | như nhau |
| `dotnet-ef` | Tạo và chạy migration database | Flyway / Liquibase CLI |

Kết thúc bài, 7 lệnh trong [§7](#7-checklist-cuối-bài-7-lệnh-7-kết-quả) phải ra đúng kết quả. Đó là điều kiện để làm bài 02.

---

## 1. Lý thuyết: ".NET Core 8" thật ra tên là gì, và vì sao phải chọn đúng bản

### 1.1 Ba cái tên dễ nhầm

| Tên | Là gì | Còn dùng không |
|---|---|---|
| **.NET Framework** 4.x | Bản gốc từ 2002, **chỉ chạy trên Windows** | Chỉ còn trong dự án cũ |
| **.NET Core** 1.0 → 3.1 | Viết lại từ đầu, chạy được Windows/Linux/macOS | Đã hết hỗ trợ |
| **.NET 5, 6, 7, 8, 9, 10** | Tiếp nối .NET Core, bỏ chữ "Core" khỏi tên | **Đây là thứ bạn học** |

"**.NET Core 8**" là cách gọi quen miệng; tên chính thức là **.NET 8**. Riêng ASP.NET Core và EF Core vẫn giữ chữ
"Core" trong tên (ASP.NET Core 8, EF Core 8), nên nghe lẫn lộn là bình thường.

### 1.2 SDK, runtime và hai con số phiên bản

- **Runtime** = thứ cần để *chạy* app (CLR: máy ảo + JIT + thư viện chuẩn). Có ba loại: `Microsoft.NETCore.App`
  (app console/worker), `Microsoft.AspNetCore.App` (web), `Microsoft.WindowsDesktop.App` (chỉ Windows).
- **SDK** = runtime + trình biên dịch + `dotnet` CLI + template. Máy dev luôn cài **SDK**.
- SDK và runtime đánh số **khác nhau**. Ví dụ thật trên máy kiểm chứng của bài này: **SDK 8.0.426** đi kèm
  **runtime 8.0.32**. Chữ số hàng trăm của SDK (`4` trong `426`) là *feature band*; hai số cuối là bản vá.

### 1.3 LTS và ngày hết hỗ trợ: điều bạn phải biết trước khi bắt đầu

| Bản | Loại | Phát hành | Hết hỗ trợ |
|---|---|---|---|
| .NET 8 | **LTS** (3 năm) | 11/2023 | **10/11/2026** |
| .NET 9 | STS | 11/2024 | 10/11/2026 |
| .NET 10 | **LTS** | 11/2025 | 11/2028 |

> **Ranh giới trung thực, nói ngay từ đầu:** bài viết ngày 21/10/2026, khi .NET 8 chỉ còn khoảng **3 tuần** được vá bảo mật (tới 10/11/2026).
> Học .NET 8 vẫn hoàn toàn đúng: dự án công ty bạn sắp vào rất có thể đang chạy .NET 8, và code ở bộ bài này
> chạy y nguyên trên .NET 10. Ở [bài 12 §6](12-bat-dau-du-an-that.md) có kết quả **chạy thật** việc nâng cả
> solution OrderFlow lên .NET 10: đổi 5 dòng `net8.0`, nâng 8 package, 24/24 test pass. Dự án **mới** thì nên
> bắt đầu thẳng bằng .NET 10.

### 1.4 Chip Apple (M1–M4) hay Intel: chọn đúng bản cài

.NET, Rider và Docker đều có bản riêng cho từng loại chip. Cài nhầm bản Intel (x64) trên máy chip Apple vẫn chạy
được qua Rosetta, nhưng chậm hơn và gây lỗi khó hiểu (Rider tìm thấy hai SDK, debug không dừng ở breakpoint…).

---

## 2. Bước 0 — Kiểm tra máy (2 phút)

Mở **Terminal** (⌘Space → gõ `Terminal` → ⏎) và chạy:

```bash
uname -m          # arm64 = chip Apple (M1/M2/M3/M4), x86_64 = Intel
sw_vers           # phiên bản macOS
df -h ~           # dung lượng trống
echo $SHELL       # /bin/zsh là mặc định từ macOS Catalina
```

| Kết quả `uname -m` | Từ giờ chọn bản cài |
|---|---|
| `arm64` | **Arm64** / "Apple Silicon" / "Mac with Apple chip" |
| `x86_64` | **x64** / "Intel" |

**Dung lượng:** nên còn trống ít nhất 15 GB: Rider ~3 GB, SDK ~1 GB, image Docker của bộ bài ~3,5 GB
(riêng `azure-sql-edge` mà Service Bus emulator cần đã 2,3 GB, [số đo](lab/OrderFlow/results/image-sizes.txt)).

---

## 3. Bước 1 — Cài .NET 8 SDK

### 3.1 Cách A (khuyên dùng): bộ cài `.pkg` chính thức

1. Mở trình duyệt, vào `https://dotnet.microsoft.com/download/dotnet/8.0`.
2. Ở bảng **SDK 8.0.4xx** (dòng mới nhất), cột **macOS**, bấm **Arm64** (chip Apple) hoặc **x64** (Intel).
3. Mở file `.pkg` vừa tải → **Continue** → **Install** → nhập mật khẩu máy.
4. **Đóng hẳn Terminal và mở lại** (bộ cài thêm đường dẫn vào `/etc/paths.d/`, Terminal cũ chưa thấy).

Bộ cài đặt SDK vào `/usr/local/share/dotnet`. Kiểm tra:

```bash
dotnet --version
dotnet --list-sdks
dotnet --list-runtimes
```

Output thật (máy kiểm chứng; trên Mac đường dẫn là `/usr/local/share/dotnet/...`, số bản vá có thể mới hơn):

```text
8.0.426
8.0.426 [/root/.dotnet/sdk]
Microsoft.AspNetCore.App 8.0.32 [/root/.dotnet/shared/Microsoft.AspNetCore.App]
Microsoft.NETCore.App 8.0.32 [/root/.dotnet/shared/Microsoft.NETCore.App]
```

Thấy **cả hai** runtime `Microsoft.NETCore.App` và `Microsoft.AspNetCore.App` bản 8.0.x là đủ cho cả bộ bài.

Xem chi tiết hơn bằng `dotnet --info`. Dòng cần nhìn trên Mac chip Apple:

```text
Runtime Environment:
 OS Name:     Mac OS X
 OS Platform: Darwin
 RID:         osx-arm64          ← phải là osx-arm64 trên chip Apple; osx-x64 nghĩa là bạn cài nhầm bản Intel
 Base Path:   /usr/local/share/dotnet/sdk/8.0.4xx/
```

### 3.2 Cách B: script `dotnet-install.sh` (nhiều phiên bản song song, máy CI)

```bash
curl -sSL https://dot.net/v1/dotnet-install.sh -o dotnet-install.sh
chmod +x dotnet-install.sh
./dotnet-install.sh --channel 8.0            # cài vào ~/.dotnet, không cần sudo

# thêm vào ~/.zshrc để Terminal nào cũng thấy
echo 'export DOTNET_ROOT="$HOME/.dotnet"' >> ~/.zshrc
echo 'export PATH="$PATH:$HOME/.dotnet:$HOME/.dotnet/tools"' >> ~/.zshrc
source ~/.zshrc
dotnet --list-sdks
```

Cách B không đụng tới `/usr/local`, xoá đi chỉ cần `rm -rf ~/.dotnet`. Nhược điểm: Rider không tự tìm thấy, phải chỉ
đường dẫn ở bước [4.3](#43-chỉ-cho-rider-thấy-net-sdk). Homebrew cũng có gói .NET, nhưng tên gói và phiên bản đổi
theo thời gian; muốn chắc chắn đúng 8.0 thì dùng A hoặc B.

### 3.3 Lý thuyết: có nhiều SDK thì `dotnet` dùng cái nào? (`global.json`)

Mặc định lệnh `dotnet` chọn **SDK mới nhất** đã cài. Máy bạn sau này có cả SDK 8 và SDK 10 là chuyện thường. Để một
repo luôn build bằng .NET 8 SDK, đặt file `global.json` ở thư mục gốc repo:

```bash
dotnet new globaljson --sdk-version 8.0.100 --roll-forward latestFeature
```

📄 **`lab/OrderFlow/global.json`** (file thật của lab, sinh bằng đúng lệnh trên)

```json
{
  "sdk": {
    "rollForward": "latestFeature",
    "version": "8.0.100"
  }
}
```

Nghĩa là: "cần SDK 8.0, từ 8.0.100 trở lên, lấy bản mới nhất có trên máy". Máy chỉ có SDK 10 thì báo lỗi rõ ràng
thay vì âm thầm build bằng bản khác. Output thật khi chạy `dotnet --version` trong thư mục lab trên máy chỉ có SDK 10.0.402:

```text
  * You intended to execute a .NET SDK command:
      A compatible .NET SDK was not found.

Requested SDK version: 8.0.100
global.json file: /home/user/algorithms-prep/tutorial/dotnet8-rider/lab/OrderFlow/global.json
```

So với Java: giống `.sdkmanrc` hoặc toolchain của Maven/Gradle.

### 3.4 Tuỳ chọn: tắt telemetry, tin chứng chỉ HTTPS dev

```bash
echo 'export DOTNET_CLI_TELEMETRY_OPTOUT=1' >> ~/.zshrc   # không gửi số liệu dùng CLI về Microsoft
dotnet dev-certs https --trust                            # chỉ cần nếu chạy app bằng https://localhost
```

Bộ bài dùng `http://localhost` nên lệnh thứ hai không bắt buộc. Khi dự án công ty dùng launch profile `https`,
chạy lệnh này một lần (macOS hỏi mật khẩu để thêm chứng chỉ vào Keychain).

---

## 4. Bước 2 — Cài JetBrains Rider

### 4.1 Cài bằng JetBrains Toolbox (khuyên dùng)

1. Tải **JetBrains Toolbox App** tại `https://www.jetbrains.com/toolbox-app/`, kéo vào **Applications**, mở lên.
2. Trong danh sách, cạnh **Rider** bấm **Install**. Toolbox tự cập nhật Rider, giữ được nhiều phiên bản và quay lại
   bản cũ khi bản mới lỗi.
3. Bấm **Rider** trong Toolbox để mở.

Không muốn dùng Toolbox thì tải file `.dmg` tại `https://www.jetbrains.com/rider/download/` (chọn đúng
**Apple Silicon** hoặc **Intel**) và kéo vào Applications.

**Giấy phép:** từ 10/2024 JetBrains cho dùng Rider **miễn phí cho mục đích phi thương mại** (tự học, dự án cá nhân).
Dùng cho **công việc có trả lương** thì cần giấy phép thương mại: hỏi công ty (thường có license chung) hoặc dùng bản
dùng thử. Điều khoản có thể đổi; đọc trang giá của JetBrains trước khi dùng cho việc công ty.

### 4.2 Lần mở đầu tiên: chọn giao diện và **keymap**

Rider hỏi vài thiết lập ban đầu. Quan trọng nhất là **keymap** (bộ phím tắt). Nếu bỏ qua, vào lại ở màn hình chào
**Customize → Keymap**, hoặc khi đã mở project: **Rider → Settings… (⌘,) → Keymap**.

| Bạn từng dùng | Chọn keymap | Ghi chú |
|---|---|---|
| IntelliJ IDEA / Android Studio | **IntelliJ (macOS)** | Phím tắt giống hệt IDEA, chuyển sang không phải học lại |
| Visual Studio / chưa dùng IDE nào | Visual Studio (macOS) | Gần với đồng nghiệp dùng Visual Studio trên Windows |

**Bộ bài này dùng keymap IntelliJ (macOS)** cho mọi phím tắt (bảng đầy đủ ở [bài 05](05-phim-tat-rider-macos.md)).
Trên Mac luôn chọn keymap có hậu tố **(macOS)**: keymap không có hậu tố dùng Ctrl như Windows.

### 4.3 Chỉ cho Rider thấy .NET SDK

Cài bằng `.pkg` thì Rider tự tìm thấy. Kiểm tra (hoặc sửa nếu cài bằng cách B):

1. **Rider → Settings… (⌘,)** → **Build, Execution, Deployment → Toolset and Build**.
2. Ô **.NET CLI executable path**: `/usr/local/share/dotnet/dotnet` (cách A) hoặc `/Users/<tên-bạn>/.dotnet/dotnet` (cách B).
3. Ô MSBuild nên hiện bản đi kèm SDK 8 (17.x).

### 4.4 Plugin nên cài (Settings → Plugins → Marketplace)

| Plugin | Vì sao |
|---|---|
| **Key Promoter X** | Mỗi lần bạn bấm chuột vào một lệnh có phím tắt, nó hiện phím tắt đó. Cách học phím tắt nhanh nhất |
| Rainbow Brackets (tuỳ thích) | Tô màu cặp ngoặc |

Không cần cài thêm cho Docker, database, HTTP client, git: Rider có sẵn.

### 4.5 Mở Rider từ Terminal

```bash
open -a Rider OrderFlow.sln        # mở solution bằng Rider
open -a Rider .                    # mở thư mục hiện tại
```

---

## 5. Bước 3 — Cài Docker Desktop

### 5.1 Cài và kiểm tra

1. Tải tại `https://www.docker.com/products/docker-desktop/`: **Mac with Apple chip** hoặc **Mac with Intel chip**.
2. Kéo **Docker.app** vào Applications, mở lên, đồng ý điều khoản. Đợi biểu tượng cá voi trên thanh menu đứng yên.
3. **Settings → Resources**: Memory **ít nhất 4 GB** (bộ bài đo thật dùng ~825 MiB, nhưng build image cần thêm).

```bash
docker version            # phải thấy CẢ "Client" lẫn "Server"; thiếu Server = Docker Desktop chưa chạy
docker compose version    # Docker Compose version v2.x trở lên
docker run --rm hello-world
```

**Giấy phép:** Docker Desktop miễn phí cho cá nhân, học tập và công ty nhỏ (dưới 250 nhân viên **và** dưới 10 triệu
USD doanh thu năm). Công ty lớn hơn cần gói trả phí; hỏi IT trước khi cài máy công ty.

### 5.2 Lý thuyết: image arm64 và Rosetta

Image Docker cũng có bản theo kiến trúc CPU. Máy chip Apple chạy image `arm64` trực tiếp; image chỉ có `amd64` thì
phải giả lập. Mọi image của bộ bài **đã kiểm có bản arm64** (`postgres:16-alpine`, `dotnet/sdk:8.0`,
`dotnet/aspnet:8.0`, `dotnet/runtime:8.0`, `azure-messaging/servicebus-emulator`, `azure-sql-edge`), nên không cần
gì thêm. Khi gặp image chỉ có amd64 (ví dụ `mssql/server:2022-latest`), bật
**Settings → General → Use Rosetta for x86_64/amd64 emulation on Apple Silicon**.

Kiểm tra một image có những kiến trúc nào:

```bash
docker manifest inspect postgres:16-alpine | grep architecture | sort -u
```

---

## 6. Bước 4 — Công cụ dòng lệnh

### 6.1 `dotnet-ef`: tạo và chạy migration (dùng từ bài 07)

```bash
dotnet tool install --global dotnet-ef --version 8.0.32
dotnet ef --version
```

```text
Entity Framework Core .NET Command-line Tools
8.0.32
```

- Ghi rõ `--version 8.0.32` để công cụ khớp đúng EF Core 8.0.32 mà bộ bài dùng. Không ghi phiên bản thì bạn nhận bản
  mới nhất (lúc kiểm chứng là 10.0.13; bản này vẫn chạy được với project EF Core 8, nhưng khớp phiên bản thì không
  phải đoán khi gặp lỗi).
- Báo `dotnet-ef does not exist` hoặc `command not found`: thư mục công cụ chưa nằm trong PATH:

```bash
echo 'export PATH="$PATH:$HOME/.dotnet/tools"' >> ~/.zshrc && source ~/.zshrc
```

- Cập nhật sau này: `dotnet tool update --global dotnet-ef --version 8.0.x`. Gỡ: `dotnet tool uninstall --global dotnet-ef`.

### 6.2 git

```bash
git --version     # chưa có thì macOS tự đề nghị cài "Command Line Developer Tools"; hoặc: xcode-select --install
```

### 6.3 Không bắt buộc: `psql`

Rider có sẵn công cụ database (bài 07), nên không cần cài client PostgreSQL. Bộ bài dùng `docker compose exec postgres psql`
khi cần dòng lệnh, chạy `psql` **bên trong** container nên máy bạn không cần cài gì.

### 6.4 Một cái bẫy riêng của người Việt: bộ gõ tiếng Việt khi viết code

Bộ gõ Telex/VNI có thể tự đổi chữ khi bạn gõ code: `dd` thành `đ`, `aw` thành `ă`, `ow` thành `ơ`. Lỗi biên dịch
kiểu `CS0103: The name 'đ' does not exist` thường là do vậy.

- Chuyển sang bàn phím **ABC** khi code (phím 🌐/fn hoặc biểu tượng trên thanh menu).
- Bộ gõ như EVKey/OpenKey có chế độ **nhớ trạng thái theo từng ứng dụng**: đặt Rider và Terminal luôn ở tiếng Anh.
- Phím tắt mặc định của macOS để đổi bộ gõ là **⌃Space**, **trùng** phím gợi ý code của Rider. Cách gỡ ở [bài 05 §1](05-phim-tat-rider-macos.md).

---

## 7. Checklist cuối bài: 7 lệnh, 7 kết quả

```bash
uname -m                         # arm64 (chip Apple) hoặc x86_64 (Intel)
dotnet --version                 # 8.0.4xx (hoặc 8.0.1xx–3xx)
dotnet --list-runtimes           # có Microsoft.AspNetCore.App 8.0.x và Microsoft.NETCore.App 8.0.x
dotnet --info | grep RID         # osx-arm64 trên chip Apple
dotnet ef --version              # 8.0.32
docker compose version           # v2.x trở lên
docker run --rm hello-world      # "Hello from Docker!"
```

Và trong Rider: màn hình chào hiện được, **Settings → Keymap** đang là **IntelliJ (macOS)** (hoặc keymap bạn chọn),
**Settings → Build, Execution, Deployment → Toolset and Build** thấy .NET CLI 8.0.

---

## 8. Lỗi hay gặp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `zsh: command not found: dotnet` ngay sau khi cài | Terminal cũ chưa đọc `/etc/paths.d/dotnet` | Đóng hẳn Terminal (⌘Q) rồi mở lại; vẫn lỗi thì `ls /usr/local/share/dotnet/dotnet` |
| `RID: osx-x64` trên máy chip Apple | Cài nhầm bộ cài Intel | Gỡ, cài lại bản **Arm64** |
| `A compatible .NET SDK was not found` khi build repo | `global.json` đòi SDK chưa có trên máy | `dotnet --list-sdks`, cài thêm đúng SDK mà `global.json` ghi; repo của team thì đừng sửa `global.json` cho khớp máy mình ([bài 12 §1.1](12-bat-dau-du-an-that.md)) |
| `Cannot connect to the Docker daemon at unix:///var/run/docker.sock` | Docker Desktop chưa chạy | Mở Docker.app, đợi cá voi đứng yên |
| `dotnet ef: command not found` | `~/.dotnet/tools` chưa vào PATH | Lệnh ở [§6.1](#61-dotnet-ef-tạo-và-chạy-migration-dùng-từ-bài-07) |
| Rider báo không tìm thấy SDK | Cài bằng cách B hoặc sau khi Rider đã mở | [§4.3](#43-chỉ-cho-rider-thấy-net-sdk), rồi **File → Invalidate Caches… → Invalidate and Restart** |
| Code tự biến `dd` thành `đ` | Bộ gõ tiếng Việt đang bật | [§6.4](#64-một-cái-bẫy-riêng-của-người-việt-bộ-gõ-tiếng-việt-khi-viết-code) |

---

## 9. Tự kiểm tra

- [ ] Giải thích được SDK khác runtime thế nào, và vì sao máy dev cần SDK còn server chỉ cần runtime (§1.2).
- [ ] Biết .NET 8 hết hỗ trợ ngày nào, LTS khác STS ra sao (§1.3).
- [ ] Biết máy mình là chip Apple hay Intel, và `dotnet --info` ra đúng RID `osx-arm64` hoặc `osx-x64` (§1.4).
- [ ] Giải thích `global.json` chọn SDK thế nào khi máy có nhiều SDK (§3.3).
- [ ] Chạy đủ 7 lệnh ở §7 và ra đúng 7 kết quả.
- [ ] Rider đang dùng keymap **IntelliJ (macOS)** và thấy .NET CLI 8.0.

---

## 10. Ranh giới trung thực

- **Đã chạy thật:** mọi lệnh `dotnet` (SDK 8.0.426), `dotnet new globaljson`, cài `dotnet-ef` 8.0.32 và 10.0.13,
  `docker manifest inspect` cho từng image (đều có `arm64`). Môi trường kiểm chứng là **Linux x64**, nên output trong
  bài có đường dẫn `/root/.dotnet`; trên Mac là `/usr/local/share/dotnet`.
- **Chưa chạy được ở đây:** bộ cài `.pkg`, Toolbox, giao diện Rider và Docker Desktop (không có macOS/màn hình). Tên
  menu ghi theo Rider và Docker Desktop bản gần đây; bản của bạn lệch chữ thì tìm bằng ô tìm kiếm trong Settings
  (⌘, rồi gõ "keymap", "toolset", "rosetta").
- Ngày hết hỗ trợ .NET và điều khoản giấy phép Rider/Docker là thông tin tại thời điểm viết (10/2026).
