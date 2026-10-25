# Bài 12 — Bắt đầu dự án thật: ngày đầu ở dự án có sẵn, dựng service mới trong 1 phút, bí mật, nâng lên .NET 10

> **Bạn sẽ làm được:** vào một dự án .NET có sẵn và chạy được nó trong buổi sáng đầu tiên; kiểm tra package lỗ hổng
> và lần ra nó từ đâu tới; giữ mật khẩu ngoài git bằng user-secrets và biến môi trường; dựng khung một service .NET 8
> mới (Api, Core, Worker, test, quản lý version tập trung) bằng **một lệnh**; biết đường nâng lên .NET 10.
>
> **Thời lượng:** đọc 30 phút · làm 45 phút
>
> **Trước đó:** [Bài 11](11-test-xunit-testcontainers.md) · **Tiếp theo:** hết bộ, quay về [mục lục](README.md)

---

## 0. Bạn đang ở tình huống nào

| Tình huống | Đọc |
|---|---|
| **A.** Vào một dự án .NET đang chạy ở công ty | §1 (checklist ngày đầu), §2 (bí mật), §8 (lỗi hay gặp) |
| **B.** Được giao dựng một service mới | §3 (script 1 phút), §4 (chuẩn hoá build), §5 (CI), §2 |
| Cả hai | §6 (.NET 10: .NET 8 hết hỗ trợ ngày 10/11/2026), §7 (lên Azure) |

---

## 1. Ngày đầu ở một dự án .NET có sẵn: checklist

Làm theo thứ tự; mỗi bước có lệnh và thứ cần nhìn. Ví dụ dùng chính OrderFlow như thể đó là dự án bạn vừa clone.

### 1.1 Đúng SDK

```bash
cat global.json            # dự án ghim SDK nào
dotnet --list-sdks         # máy bạn có SDK nào
```

`global.json` của OrderFlow ghi `"version": "8.0.100"` với `"rollForward": "latestFeature"`: chấp nhận mọi SDK
**8.0.x** từ 8.0.100 trở lên, **không** chấp nhận 10.x. Máy chỉ cài .NET 10 thì mọi lệnh `dotnet` trong thư mục đó
lỗi ngay. Lỗi thật (đã gặp khi chạy SDK 10 trong thư mục OrderFlow):

```text
  * You intended to execute a .NET SDK command:
      A compatible .NET SDK was not found.

Requested SDK version: 8.0.100
global.json file: .../OrderFlow/global.json

Install the [8.0.100] .NET SDK or update [.../OrderFlow/global.json] to match an installed SDK.
```

Sửa: cài thêm SDK 8 ([bài 01](01-cai-dat-macos.md)); **đừng** sửa `global.json` của team cho khớp máy mình. Nhiều SDK
cài song song được, mỗi thư mục tự chọn theo `global.json` của nó.

### 1.2 Restore, build, test

```bash
dotnet restore
dotnet build               # nhìn dòng "N Warning(s)": dự án lành thường là 0
dotnet test                # cần Docker nếu có Testcontainers (bài 11)
```

`restore` lỗi `NU1301` hoặc `401 Unauthorized`: dự án dùng feed NuGet riêng của công ty. Tìm file `NuGet.config` ở
gốc repo để biết feed nào, rồi hỏi team cách lấy quyền (thường là một token cá nhân). Đừng commit token vào
`NuGet.config`.

### 1.3 Hạ tầng và cấu hình chạy local

```bash
docker compose up -d       # nếu repo có docker-compose.yml (Postgres, Service Bus emulator... như bài 10)
```

Rồi tìm xem app cần cấu hình gì mà repo **không** chứa (mật khẩu, connection string thật): README của dự án, hoặc hỏi
team. Đặt chúng bằng user-secrets (§2), không sửa `appsettings.json`.

### 1.4 Đọc bản đồ dự án trong 30 phút

| Đọc | Để biết |
|---|---|
| `*.sln` và cây thư mục `src/`, `tests/` | Có những project nào, project nào tham chiếu project nào |
| `Program.cs` của từng app | **Danh sách mọi service** đăng ký vào DI, thứ tự middleware, app dùng cấu hình nào. Đọc file này trước tiên |
| `appsettings.json`, `appsettings.Development.json` | Các khoá cấu hình, mức log |
| `Properties/launchSettings.json` | Cổng, biến môi trường khi chạy từ Rider |
| `Directory.Build.props`, `Directory.Packages.props` | Quy định chung cho mọi project, version package (§4) |
| `.editorconfig` | Quy tắc code style mà Rider và `dotnet format` sẽ áp dụng |
| `Dockerfile`, `docker-compose.yml` | App chạy thật thế nào |
| `.github/workflows/*.yml` hoặc `azure-pipelines.yml` | CI kiểm tra gì trước khi merge |

Trong Rider, các phím giúp đọc code lạ nhanh ([bài 05](05-phim-tat-rider-macos.md)):

| Phím | Dùng khi |
|---|---|
| **⇧⇧** | Không biết gì: gõ tên bất kỳ (class, file, setting) |
| **⌘O** / **⇧⌘O** | Mở class / mở file theo tên |
| **⌥F7** | Ai gọi method này |
| **⌥⌘B** | Từ interface nhảy tới class cài đặt (DI đăng ký interface, đây là cách tìm code thật) |
| **⌘F12** | Danh sách member của file đang mở |
| **⌃H** | Cây kế thừa |
| **⇧⌘F** | Tìm chuỗi trong cả solution, ví dụ một khoá cấu hình `"ConnectionStrings:"` |

### 1.5 Sức khoẻ package: lỗ hổng và bản cũ

```bash
dotnet list package --vulnerable --include-transitive
```

Đây là chuyện có thật của chính OrderFlow. Lần đầu chạy lệnh trên (khi project test còn dùng `xunit 2.5.3` mà
`dotnet new xunit` tạo ra):

```text
The given project `OrderFlow.Api` has no vulnerable packages given the current sources.
Project `OrderFlow.UnitTests` has the following vulnerable packages
   [net8.0]:
   Transitive Package                    Resolved   Severity   Advisory URL
   > System.Net.Http                     4.3.0      High       https://github.com/advisories/GHSA-7jgj-8wvc-jh57
   > System.Text.RegularExpressions      4.3.0      High       https://github.com/advisories/GHSA-cmhx-cq75-c4mj
```

Không project nào khai báo `System.Net.Http` cả: đó là gói **bắc cầu**. Hỏi "ai kéo nó vào" bằng `dotnet nuget why`
(có từ SDK 8.0.400):

```bash
dotnet nuget why tests/OrderFlow.UnitTests System.Net.Http
```

```text
Project 'OrderFlow.UnitTests' has the following dependency graph(s) for 'System.Net.Http':

  [net8.0]
   │
   └─ xunit (v2.5.3)
      ├─ xunit.assert (v2.5.3)
      │  └─ NETStandard.Library (v1.6.1)
      │     └─ System.Net.Http (v4.3.0)
      └─ xunit.core (v2.5.3)
         ├─ xunit.extensibility.core (v2.5.3)
         │  └─ NETStandard.Library (v1.6.1)
         │     └─ System.Net.Http (v4.3.0)
```

Gốc là `xunit 2.5.3`. Nâng gốc thay vì vá ngọn: `dotnet add tests/OrderFlow.UnitTests package xunit --version 2.9.3`
(và project IntegrationTests). Chạy lại: cả 5 project `has no vulnerable packages`, 24 test vẫn xanh. Bộ bài đã đưa
bước này vào [bài 11 §2](11-test-xunit-testcontainers.md); bằng chứng trước/sau:
[`results/package-audit.txt`](lab/OrderFlow/results/package-audit.txt).

Xem package nào có bản mới, và **cẩn thận** với cột `Latest`:

```bash
dotnet list src/OrderFlow.Core package --outdated
```

```text
   Top-level Package                                           Requested   Resolved   Latest
   > EFCore.NamingConventions                                  8.0.3       8.0.3      10.0.1
   > Microsoft.Extensions.Options.ConfigurationExtensions      8.0.0       8.0.0      10.0.13
   > Microsoft.Extensions.Options.DataAnnotations              8.0.0       8.0.0      10.0.13
   > Npgsql.EntityFrameworkCore.PostgreSQL                     8.0.11      8.0.11     10.0.3
```

Mọi bản `Latest` ở đây là 10.x, **không** chạy trên `net8.0` ([bài 02 §5.3](02-rider-du-an-dau-tien.md)). Lệnh hữu ích
cho dự án .NET 8 là chỉ hỏi bản vá cùng dòng:

```bash
dotnet list src/OrderFlow.Core package --outdated --highest-patch
```

```text
The given project `OrderFlow.Core` has no updates given the current sources.
```

### 1.6 Những câu nên hỏi team trong ngày đầu

- Nhánh chính tên gì, quy trình tạo PR và review ra sao, CI chạy những gì?
- Secret cho môi trường dev lấy ở đâu (Key Vault dev, 1Password, file mẫu)?
- Có những môi trường nào (dev, staging, prod), deploy bằng cách nào, ai được deploy?
- Log và metric của app xem ở đâu khi có sự cố?
- Dự án có kế hoạch lên .NET 10 chưa (§6)?

---

## 2. Bí mật: mật khẩu không bao giờ nằm trong git

### 2.1 Cấu hình đến từ đâu

[Bài 06 §8](06-web-api-asp-net-core.md) đã có thứ tự: nguồn sau **đè** nguồn trước.

```text
appsettings.json  <  appsettings.{Environment}.json  <  user secrets (CHỈ khi Development)  <  biến môi trường  <  tham số dòng lệnh
```

`appsettings.json` của OrderFlow có mật khẩu `orderflow`: chấp nhận được vì đó là container dev trên máy bạn. Dự án
thật: file commit chỉ chứa giá trị **không bí mật**, bí mật đến từ user-secrets (máy dev) hoặc biến môi trường / Key
Vault (server).

### 2.2 Thực hành `dotnet user-secrets` (chạy thật trên bản sao OrderFlow)

Để thấy rõ nguồn nào thắng, bản sao này cố tình để mật khẩu **sai** trong `appsettings.json`
(`Password=WRONG-in-appsettings`), mật khẩu đúng chỉ nằm trong user-secrets.

```bash
dotnet user-secrets init --project src/OrderFlow.Api
```

```text
Set UserSecretsId to 'dd6f8380-c870-4a37-b231-10bb5c4592c2' for MSBuild project '.../src/OrderFlow.Api/OrderFlow.Api.csproj'.
```

Lệnh này chỉ thêm một dòng `<UserSecretsId>...</UserSecretsId>` vào `.csproj` (commit được, nó không phải bí mật).

```bash
dotnet user-secrets set "ConnectionStrings:Postgres" \
  "Host=localhost;Port=5432;Database=orderflow;Username=orderflow;Password=orderflow" --project src/OrderFlow.Api
dotnet user-secrets list --project src/OrderFlow.Api
```

```text
Successfully saved ConnectionStrings:Postgres = Host=localhost;Port=5432;Database=orderflow;Username=orderflow;Password=orderflow to the secret store.
ConnectionStrings:Postgres = Host=localhost;Port=5432;Database=orderflow;Username=orderflow;Password=orderflow
```

Giá trị nằm ở `~/.microsoft/usersecrets/<UserSecretsId>/secrets.json`, **ngoài** thư mục repo, nên không thể lỡ tay
commit. Đây là file JSON thường, **không** mã hoá: nó giữ bí mật khỏi git, không giữ khỏi người dùng máy bạn.

Chạy API ở hai môi trường, cùng một code, cùng một máy:

| Chạy | `/health` |
|---|---|
| `ASPNETCORE_ENVIRONMENT=Development` | `Healthy` (HTTP 200): đọc user-secrets, mật khẩu đúng |
| `ASPNETCORE_ENVIRONMENT=Production` | `Unhealthy` (HTTP 503): **không** đọc user-secrets, dùng mật khẩu sai trong `appsettings.json` |
| `Production` + biến môi trường `ConnectionStrings__Postgres=...` | `Healthy` (HTTP 200): cách server thật nhận bí mật |

Ở Production, `POST /api/orders` trả `500`, log ghi đúng nguyên nhân:

```text
Npgsql.PostgresException (0x80004005): 28P01: password authentication failed for user "orderflow"
```

Trong Rider: chuột phải project → **Tools → .NET User Secrets** mở thẳng `secrets.json` (tên mục có thể khác chút
theo bản Rider). Xoá hết: `dotnet user-secrets clear --project src/OrderFlow.Api`.

### 2.3 Trên server: biến môi trường, Key Vault, Managed Identity

- Nền tảng chạy app (Kubernetes Secret, Azure App Service / Container Apps settings, biến của pipeline) đặt **biến môi
  trường**, ví dụ `ConnectionStrings__Postgres` (hai dấu `_`, [bài 10](10-docker-hoa.md)). App không cần sửa gì.
- **Azure Key Vault** làm nguồn cấu hình (package `Azure.Extensions.AspNetCore.Configuration.Secrets` và
  `Azure.Identity`):

  ```csharp
  builder.Configuration.AddAzureKeyVault(new Uri("https://orderflow-prod-kv.vault.azure.net/"), new DefaultAzureCredential());
  ```

  Secret tên `ConnectionStrings--Postgres` trong Key Vault thành khoá `ConnectionStrings:Postgres` (Key Vault không cho
  dấu `:` trong tên nên dùng `--`).
- **Managed Identity**: app được cấp một danh tính của Azure, `DefaultAzureCredential` tự lấy token; không còn mật khẩu
  nào để lộ. Service Bus làm được như [bài 09 §9](09-azure-service-bus.md).

Phần §2.3 viết theo tài liệu Azure, **chưa chạy** trên Azure thật.

### 2.4 Lỡ commit mật khẩu thì làm gì

**Đổi mật khẩu đó ngay** (rotate), coi như nó đã lộ. Xoá khỏi lịch sử git là việc làm thêm, không thay được bước
đổi mật khẩu: bản clone, bản fork, log CI có thể đã giữ nó.

---

## 3. Dựng service mới trong 1 phút: `new-dotnet8-service.sh`

Script dưới đây gói mọi lệnh `dotnet new` / `dotnet add` của bài 02, 06, 08, 11 cùng các file chuẩn hoá ở §4 thành
một lệnh. Nó dừng ngay nếu bất kỳ bước nào lỗi, và chỉ báo xong khi build 0 warning, test xanh, không package lỗ hổng,
`dotnet format` không còn gì phải sửa.

📄 **`lab/bootstrap/new-dotnet8-service.sh`**

```bash
#!/usr/bin/env bash
# Dung khung mot service .NET 8 moi trong khoang 1 phut: Api (controllers) + Core + Worker + UnitTests + IntegrationTests,
# global.json, .gitignore, .gitattributes, .editorconfig, Directory.Build.props (warning = loi), Central Package
# Management (moi version package nam o MOT file Directory.Packages.props). Cuoi script: build 0 warning, test xanh,
# khong co package lo hong, dotnet format khong con gi phai sua.
#
#   ./new-dotnet8-service.sh Acme.Billing            # tao ./Acme.Billing
#   ./new-dotnet8-service.sh Acme.Billing ~/code     # tao ~/code/Acme.Billing
#
# Can: .NET SDK 8 (dotnet --list-sdks co dong 8.0.x) va mang toi nuget.org. Chay duoc tren macOS (bash 3.2, sed/find
# cua BSD) va Linux.
set -euo pipefail

NAME=${1:?usage: new-dotnet8-service.sh <Name> [parent-dir]}
case "$NAME" in *[!A-Za-z0-9.]*|.*|*.) echo "Name must look like Acme.Billing" >&2; exit 1 ;; esac
DIR="${2:-.}/$NAME"
if [ -e "$DIR" ]; then echo "$DIR already exists" >&2; exit 1; fi
mkdir -p "$DIR"
cd "$DIR"

step() { printf '\n== %s\n' "$*"; }
quiet() { "$@" > .bootstrap.log 2>&1 || { cat .bootstrap.log; exit 1; }; }

step "SDK, solution, file chung"
quiet dotnet new globaljson --sdk-version 8.0.100 --roll-forward latestFeature # khoa SDK 8.0.x cho ca team
dotnet --version
quiet dotnet new sln -n "$NAME"
quiet dotnet new gitignore
quiet dotnet new editorconfig
# mau editorconfig cua Microsoft dat end_of_line = crlf (kieu Windows); team dung Mac/Linux thi doi sang lf
sed -i.bak -e 's/^end_of_line = crlf/end_of_line = lf/' -e 's/^insert_final_newline = false/insert_final_newline = true/' .editorconfig
rm .editorconfig.bak
printf '# moi file text vao git voi xuong dong LF, du ai sua tren Windows hay Mac\n* text=auto eol=lf\n' > .gitattributes

step "5 project"
quiet dotnet new webapi --use-controllers -n "$NAME.Api" -o "src/$NAME.Api"
quiet dotnet new classlib -n "$NAME.Core" -o "src/$NAME.Core"
quiet dotnet new worker -n "$NAME.Worker" -o "src/$NAME.Worker"
quiet dotnet new xunit -n "$NAME.UnitTests" -o "tests/$NAME.UnitTests"
quiet dotnet new xunit -n "$NAME.IntegrationTests" -o "tests/$NAME.IntegrationTests"
rm "src/$NAME.Core/Class1.cs" "tests/$NAME.UnitTests/UnitTest1.cs" "tests/$NAME.IntegrationTests/UnitTest1.cs"
quiet dotnet sln add src/*/*.csproj tests/*/*.csproj

step "Tham chieu giua cac project, package"
quiet dotnet add "src/$NAME.Api" reference "src/$NAME.Core"
quiet dotnet add "src/$NAME.Worker" reference "src/$NAME.Core"
quiet dotnet add "tests/$NAME.UnitTests" reference "src/$NAME.Core"
quiet dotnet add "tests/$NAME.IntegrationTests" reference "src/$NAME.Api"
quiet dotnet add "tests/$NAME.IntegrationTests" package Microsoft.AspNetCore.Mvc.Testing --version 8.0.32 # 8.0.x: ban 10.x khong chay tren net8.0
for t in UnitTests IntegrationTests; do # template ghim xunit 2.5.3, keo theo System.Net.Http 4.3.0 co lo hong (High)
  quiet dotnet add "tests/$NAME.$t" package xunit --version 2.9.3
done

step "Central Package Management: chuyen Version tu .csproj sang Directory.Packages.props"
{
  echo '<Project>'
  echo '  <PropertyGroup>'
  echo '    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>'
  echo '  </PropertyGroup>'
  echo '  <ItemGroup>'
  sed -n 's/.*<PackageReference Include="\([^"]*\)" Version="\([^"]*\)".*/    <PackageVersion Include="\1" Version="\2" \/>/p' src/*/*.csproj tests/*/*.csproj | sort -u
  echo '  </ItemGroup>'
  echo '</Project>'
} > Directory.Packages.props
for f in src/*/*.csproj tests/*/*.csproj; do
  sed -i.bak 's/\(<PackageReference Include="[^"]*"\) Version="[^"]*"/\1/' "$f" && rm "$f.bak" # sed -i.bak: chay ca BSD lan GNU
done
cat Directory.Packages.props

step "Directory.Build.props: ap cho moi project trong thu muc"
cat > Directory.Build.props <<'EOF'
<Project>
  <PropertyGroup>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
  </PropertyGroup>
</Project>
EOF

step "Mot integration test that cho API mau"
echo 'public partial class Program { } // de project test dung duoc WebApplicationFactory<Program>' >> "src/$NAME.Api/Program.cs"
cat > "tests/$NAME.IntegrationTests/ApiSmokeTests.cs" <<EOF
using System.Net;

using Microsoft.AspNetCore.Mvc.Testing;

namespace $NAME.IntegrationTests;

public class ApiSmokeTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public ApiSmokeTests(WebApplicationFactory<Program> factory) => _client = factory.CreateClient();

    [Fact]
    public async Task Weatherforecast_returns_200()
    {
        var response = await _client.GetAsync("/WeatherForecast");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}
EOF

step "Doi xuong dong CRLF cua file template sang LF"
find . \( -name bin -o -name obj \) -prune -o -type f \( -name '*.cs' -o -name '*.csproj' -o -name '*.sln' \
  -o -name '*.json' -o -name '*.http' -o -name '.gitignore' -o -name '.editorconfig' \) -print |
  while IFS= read -r f; do tr -d '\r' < "$f" > "$f.lf" && mv "$f.lf" "$f"; done # tr: giong nhau tren macOS va Linux

step "Build, test, lo hong, dinh dang"
quiet dotnet build
grep -E "Warning\(s\)|Error\(s\)" .bootstrap.log
quiet dotnet test --no-build
grep -E "Passed!|Failed!" .bootstrap.log
quiet dotnet list package --vulnerable --include-transitive
grep -E "vulnerable" .bootstrap.log
quiet dotnet format --verify-no-changes # code dung .editorconfig; neu khong, script dung o day va in loi
echo "dotnet format: khong co gi phai sua"
rm .bootstrap.log

step "Xong: $(pwd)"
echo "Mo bang Rider: open -a Rider \"$NAME.sln\"   |   chay API: dotnet run --project src/$NAME.Api"
```

Chạy (trên Mac: Terminal hoặc Terminal của Rider):

```bash
chmod +x lab/bootstrap/new-dotnet8-service.sh
./lab/bootstrap/new-dotnet8-service.sh Acme.Billing ~/code
```

Kết quả thật trên Linux x64, 64 giây ([`bootstrap-output.txt`](lab/bootstrap/bootstrap-output.txt), rút gọn):

```text
== SDK, solution, file chung
8.0.426

== Central Package Management: chuyen Version tu .csproj sang Directory.Packages.props
<Project>
  <PropertyGroup>
    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
  </PropertyGroup>
  <ItemGroup>
    <PackageVersion Include="Microsoft.AspNetCore.Mvc.Testing" Version="8.0.32" />
    <PackageVersion Include="Microsoft.Extensions.Hosting" Version="8.0.1" />
    <PackageVersion Include="Microsoft.NET.Test.Sdk" Version="17.8.0" />
    <PackageVersion Include="Swashbuckle.AspNetCore" Version="6.6.2" />
    <PackageVersion Include="coverlet.collector" Version="6.0.0" />
    <PackageVersion Include="xunit" Version="2.9.3" />
    <PackageVersion Include="xunit.runner.visualstudio" Version="2.5.3" />
  </ItemGroup>
</Project>

== Build, test, lo hong, dinh dang
    0 Warning(s)
    0 Error(s)
Passed!  - Failed:     0, Passed:     1, Skipped:     0, Total:     1, Duration: < 1 ms - Acme.Billing.IntegrationTests.dll (net8.0)
The given project `Acme.Billing.Api` has no vulnerable packages given the current sources.
...
dotnet format: khong co gi phai sua
```

Thứ script tạo ra (25 file, `git add -A` không dính `bin/` hay `obj/`):

```text
Acme.Billing/
├── .editorconfig  .gitattributes  .gitignore  global.json
├── Directory.Build.props  Directory.Packages.props  Acme.Billing.sln
├── src/
│   ├── Acme.Billing.Api/        ← webapi --use-controllers: WeatherForecastController, Swagger, file .http
│   ├── Acme.Billing.Core/       ← classlib rỗng: domain, data, interface đặt ở đây
│   └── Acme.Billing.Worker/     ← worker: Worker.cs ghi log mỗi giây
└── tests/
    ├── Acme.Billing.UnitTests/         ← rỗng, tham chiếu Core
    └── Acme.Billing.IntegrationTests/  ← ApiSmokeTests: WebApplicationFactory gọi /WeatherForecast
```

Đã thử chạy luôn: API trả `200` ở `/WeatherForecast` và `/swagger/index.html` sau khoảng 5 giây; Worker in
`Worker running at: ...` mỗi giây.

Ba điều script sửa so với "cứ `dotnet new` là xong", cả ba đều phát hiện khi viết và chạy nó:

| Vấn đề thật | Script làm gì |
|---|---|
| `dotnet new xunit` ghim `xunit 2.5.3`, kéo theo hai gói lỗ hổng mức High (§1.5) | Nâng lên 2.9.3 |
| `dotnet new editorconfig` đặt `end_of_line = crlf`: chạy `dotnet format` trên Mac báo lỗi ở **mọi** file, đòi đổi sang CRLF | Đổi thành `lf` và `insert_final_newline = true` |
| File do template `webapi`, `worker`, `sln`, `gitignore`... sinh ra dùng xuống dòng CRLF | Đổi một lần sang LF; `.gitattributes` giữ LF từ đó về sau |

`dotnet test` trên project vừa tạo in thêm `No test is available in .../Acme.Billing.UnitTests.dll`: đúng, project
unit test chưa có test nào; viết test đầu tiên là hết.

**Bước tiếp theo** sau khi chạy script:

1. `cd ~/code/Acme.Billing && git init && git add -A && git commit -m "Skeleton from new-dotnet8-service.sh"`.
2. Mở trong Rider: `open -a Rider Acme.Billing.sln`.
3. Cần PostgreSQL: chép `docker-compose.yml` phần `postgres` của OrderFlow, thêm package theo [bài 07](07-postgresql-ef-core.md)
   (`dotnet add package ... --version ...` vẫn chạy đúng với quản lý version tập trung, xem §4.2).
4. Cần worker xử lý việc định kỳ: theo [bài 08](08-background-worker.md); cần Service Bus: [bài 09](09-azure-service-bus.md).
5. Viết test theo [bài 11](11-test-xunit-testcontainers.md).

---

## 4. Chuẩn hoá build cho cả solution

### 4.1 `Directory.Build.props`

MSBuild tự đọc file tên `Directory.Build.props` ở thư mục của project **và mọi thư mục cha**, áp nội dung vào mọi
project bên dưới. Đặt ở gốc repo là một chỗ cho quy định chung. Script đặt `TreatWarningsAsErrors`: warning thành
lỗi, không ai để warning tích tụ được. Thử thêm vào Core một class quên khởi tạo property:

```csharp
namespace Acme.Billing.Core;

public class Invoice
{
    public string Number { get; set; }
}
```

```text
Invoice.cs(5,19): error CS8618: Non-nullable property 'Number' must contain a non-null value when exiting constructor. Consider adding the 'required' modifier or declaring the property as nullable.
    0 Warning(s)
    1 Error(s)
```

Không có `TreatWarningsAsErrors` thì đây chỉ là một warning trôi qua. Sửa: `public required string Number { get; set; }`
hoặc `string?` tuỳ nghĩa nghiệp vụ ([bài 03](03-csharp-cho-dev-java.md)).

Dự án cũ có hàng trăm warning thì đừng bật một lần cho cả solution; bật cho project mới, hoặc dọn dần từng project.

### 4.2 Central Package Management (CPM)

Solution nhiều project mà mỗi `.csproj` tự ghi version thì sớm muộn hai project dùng hai version khác nhau của cùng
một package. CPM gom version về **một** file:

- `Directory.Packages.props` ở gốc: `<ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>` và mỗi
  package một dòng `<PackageVersion Include="..." Version="..." />`.
- `.csproj` chỉ ghi `<PackageReference Include="..." />`, **không** có `Version`.

`dotnet add package` tự hiểu CPM (đã thử trên project script tạo):

```bash
dotnet add src/Acme.Billing.Core package Npgsql.EntityFrameworkCore.PostgreSQL --version 8.0.11
```

Kết quả: `Acme.Billing.Core.csproj` có `<PackageReference Include="Npgsql.EntityFrameworkCore.PostgreSQL" />`, còn
`Directory.Packages.props` có `<PackageVersion Include="Npgsql.EntityFrameworkCore.PostgreSQL" Version="8.0.11" />`.
Lỡ ghi `Version` vào `.csproj` khi đã bật CPM thì build báo:

```text
error NU1008: Projects that use central package version management should not define the version on the PackageReference items but on the PackageVersion items: Npgsql.EntityFrameworkCore.PostgreSQL.
```

Nâng một package cho cả solution: sửa đúng một dòng trong `Directory.Packages.props`. OrderFlow không dùng CPM để
từng bài thêm package bằng lệnh quen thuộc nhất; service mới thì nên dùng ngay từ đầu.

### 4.3 `.editorconfig` và `dotnet format`

Rider đọc `.editorconfig` để format code (**⌥⌘L**) và gợi ý style; `dotnet format` áp đúng các quy tắc đó từ dòng
lệnh, `dotnet format --verify-no-changes` thì chỉ kiểm và trả mã lỗi nếu có chỗ sai (dùng trong CI). Hai quy tắc của
mẫu Microsoft đáng biết: `end_of_line` (đã nói ở §3) và `dotnet_separate_import_directive_groups = true`, tức phải có
một dòng trống giữa nhóm `using System...` và nhóm `using` khác. Chính script đã vấp quy tắc thứ hai ở file test nó
sinh ra, `dotnet format --verify-no-changes` bắt được:

```text
ApiSmokeTests.cs(2,1): error WHITESPACE: Fix whitespace formatting. Insert '\n'.
```

---

## 5. CI tối thiểu (GitHub Actions)

Đặt ở `.github/workflows/ci.yml` của **repo service** (không phải repo bộ bài này):

```yaml
name: ci
on:
  pull_request:
  push:
    branches: [main]

jobs:
  build-test:
    runs-on: ubuntu-latest           # có sẵn Docker: Testcontainers chạy được
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-dotnet@v4
        with:
          global-json-file: global.json   # cài đúng SDK mà global.json ghim
      - run: dotnet restore
      - run: dotnet build --no-restore
      - run: dotnet test --no-build
      - run: dotnet format --verify-no-changes --no-restore
      - run: dotnet list package --vulnerable --include-transitive
```

Lệnh cuối chỉ **in** danh sách, không làm job đỏ khi có lỗ hổng; muốn chặn thì thêm bước kiểm tra kết quả, hoặc dựa
vào cảnh báo `NU1902`–`NU1904` lúc restore (NuGet Audit). File này **chưa chạy** trên GitHub Actions trong bộ bài; các
lệnh bên trong thì đều đã chạy ở §3 và bài 11.

---

## 6. Nâng lên .NET 10 (đã chạy thật trên bản sao OrderFlow)

.NET 8 hết hỗ trợ ngày **10/11/2026** ([bài 01](01-cai-dat-macos.md)): sau ngày đó không còn bản vá bảo mật. .NET 10 là
LTS tới 11/2028. Kết quả nâng cả solution OrderFlow (bản cuối, 24 test) bằng SDK 10.0.402
([`results/net10-upgrade.txt`](lab/OrderFlow/results/net10-upgrade.txt)):

| Đổi | Chi tiết |
|---|---|
| 5 file `.csproj` | `<TargetFramework>net8.0</TargetFramework>` → `net10.0` |
| `global.json` | `"version": "8.0.100"` → `"10.0.100"` (giữ `rollForward: latestFeature`) |
| 8 package Microsoft/Npgsql | `Npgsql.EntityFrameworkCore.PostgreSQL` 8.0.11 → 10.0.3, `EFCore.NamingConventions` 8.0.3 → 10.0.1, `Microsoft.EntityFrameworkCore.Design`, `...HealthChecks.EntityFrameworkCore`, `Microsoft.AspNetCore.Mvc.Testing`, `Microsoft.Extensions.Hosting`, `...Options.ConfigurationExtensions`, `...Options.DataAnnotations` → 10.0.13 |
| **Không** đổi | `Azure.Messaging.ServiceBus` 7.21.0, `Swashbuckle.AspNetCore` 6.6.2, `Testcontainers.PostgreSql` 4.16.0, `xunit` 2.9.3, `Microsoft.NET.Test.Sdk` 17.8.0, `Microsoft.Extensions.TimeProvider.Testing` 8.10.0 |
| Code C# | **Không sửa dòng nào** |

| Kiểm tra | Kết quả |
|---|---|
| `dotnet build` | Build succeeded, 0 warning |
| `dotnet test` | 10/10 unit, 14/14 integration (`net10.0`); migration tạo bằng EF Core 8 vẫn áp dụng được |
| Chạy API | Sẵn sàng sau khoảng 6 giây; `/swagger/v1/swagger.json` trả OpenAPI 3.0.1; `POST /api/orders` → `201` |
| Docker image `sdk:10.0` / `aspnet:10.0` / `runtime:10.0` và compose | **Chưa thử** |

Làm trên Mac, trong Rider: **⇧⌘R** (Replace in Files), tìm `<TargetFramework>net8.0</TargetFramework>`, thay bằng
`<TargetFramework>net10.0</TargetFramework>`, giới hạn file `*.csproj`; sửa `global.json`; rồi nâng từng package:

```bash
dotnet add src/OrderFlow.Core package Npgsql.EntityFrameworkCore.PostgreSQL --version 10.0.3
dotnet add src/OrderFlow.Core package EFCore.NamingConventions --version 10.0.1
# ... các package Microsoft.* 8.0.x khác lên 10.0.13, xem bảng trên
dotnet build && dotnet test
```

Hai Dockerfile (bài 10) đổi `sdk:8.0` → `sdk:10.0`, `aspnet:8.0` → `aspnet:10.0`, `runtime:8.0` → `runtime:10.0`; phần
này chưa chạy thử nên hãy chạy lại smoke test của bài 10 sau khi đổi.

**Dự án mới thì bắt đầu thẳng bằng .NET 10.** Template của SDK 10 khác SDK 8 ở vài chỗ (đã chạy `dotnet new` bằng SDK
10.0.402):

| Template | SDK 8 | SDK 10 |
|---|---|---|
| `webapi --use-controllers` | `Swashbuckle.AspNetCore` 6.6.2, có Swagger UI | `Microsoft.AspNetCore.OpenApi` 10.0.13, `AddOpenApi()` + `MapOpenApi()`, **không** có Swagger UI |
| `xunit` | `xunit` 2.5.3, runner 2.5.3, Test SDK 17.8.0 | `xunit` 2.9.3, runner 3.1.4, Test SDK 17.14.1 |

Muốn dùng script ở §3 cho .NET 10: đổi `--sdk-version 8.0.100` thành `10.0.100`, đổi `8.0.32` của
`Microsoft.AspNetCore.Mvc.Testing` thành bản 10.0.x, bỏ vòng nâng `xunit`, và sửa test mẫu vì không còn Swagger. Bản đó
chưa được viết và chạy trong bộ bài.

---

## 7. Lên Azure: những gì đổi khi chạy thật

| Ở máy dev (bộ bài) | Trên Azure | Code đổi? |
|---|---|---|
| PostgreSQL trong Docker | Azure Database for PostgreSQL (Flexible Server) | Không, chỉ connection string |
| Service Bus emulator | Azure Service Bus namespace (Standard trở lên nếu cần topic) | Không, chỉ connection string hoặc Managed Identity |
| `appsettings.json`, user-secrets | App settings của nền tảng, Key Vault, Managed Identity (§2.3) | Thêm vài dòng cấu hình nếu dùng Key Vault |
| `docker compose` | Azure Container Apps, AKS, hoặc App Service | Không: cùng image của bài 10 |
| Log ra console | Application Insights / OpenTelemetry | Thêm package và vài dòng ở `Program.cs` |
| `migrate` chạy trong compose | Một bước riêng trong pipeline deploy, trước khi app mới lên | Không: vẫn `dotnet OrderFlow.Api.dll --migrate` |

Bảng này viết theo tài liệu Azure, **chưa chạy** trên Azure thật.

---

## 8. Học tiếp gì sau bộ bài

| Chủ đề | Vì sao cần sớm |
|---|---|
| Log có cấu trúc, OpenTelemetry (trace, metric) | Sự cố đầu tiên trên production sẽ cần nó |
| Xác thực, phân quyền (JWT Bearer, `[Authorize]`) | Hầu như API nào cũng phải có |
| Resilience cho HttpClient (`Microsoft.Extensions.Http.Resilience`: retry, timeout, circuit breaker) | Gọi service khác thì sẽ có lúc nó chậm hoặc chết |
| Hiệu năng EF Core (`AsNoTracking`, projection, N+1, index) | Tiếp nối [bài 07 §8](07-postgresql-ef-core.md) |
| Health check sống/sẵn sàng (liveness/readiness) cho Kubernetes | Tiếp nối `/health` của bài 06 |
| Giới hạn tốc độ (`AddRateLimiter`), cache đầu ra | Bảo vệ API khi tải tăng |

Tài liệu chính thức: learn.microsoft.com, mục ASP.NET Core và EF Core; chọn phiên bản ".NET 8" (hoặc 10) ở góc trên
trang để không đọc nhầm API của bản khác.

---

## 9. Lỗi hay gặp trong những ngày đầu

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `A compatible .NET SDK was not found. Requested SDK version: 8.0.100` | Máy thiếu SDK mà `global.json` ghim | Cài SDK 8 (bài 01); không sửa `global.json` của team |
| `NU1301: Unable to load the service index for source ...` hoặc `401` khi restore | Feed NuGet riêng, chưa có quyền; hoặc mạng/proxy | Xem `NuGet.config`; xin token; cấu hình proxy |
| `NU1202: Package ... 10.0.x is not compatible with net8.0` | Cài bản mới nhất (10.x) vào project .NET 8 | Ghi rõ `--version 8.0.x` (bài 02) |
| `NU1008: Projects that use central package version management should not define the version on the PackageReference items` | Ghi `Version` trong `.csproj` khi đã bật CPM | Chuyển version sang `Directory.Packages.props` |
| `error CS8618: Non-nullable property ... must contain a non-null value` sau khi bật `TreatWarningsAsErrors` | Warning nullable nay là lỗi | `required`, gán giá trị, hoặc `?` |
| `28P01: password authentication failed for user ...` | Chạy ở môi trường không đọc user-secrets (Production), hoặc secret đặt sai project | Kiểm `ASPNETCORE_ENVIRONMENT`; `dotnet user-secrets list --project ...` |
| `/health` trả `Unhealthy` mà log không ghi lý do | Health check của EF Core không in exception | Gọi một endpoint có dùng DB để thấy lỗi thật trong log, hoặc kiểm connection string |
| `dotnet format --verify-no-changes` báo `ENDOFLINE` ở mọi file trên Mac | `.editorconfig` mẫu đặt `end_of_line = crlf` | Đổi thành `lf` (§3) |
| `Address already in use` khi chạy API | Một bản API khác (hoặc container) đang giữ cổng | `lsof -i :5080` để tìm tiến trình, dừng nó |
| Trình duyệt chặn `https://localhost` | Chứng chỉ dev chưa được tin | `dotnet dev-certs https --trust` (bài 01) |

---

## 10. Tự kiểm tra

- [ ] Trên một repo lạ: biết SDK nào cần, restore/build/test được, biết đọc file nào trước.
- [ ] Chạy `dotnet list package --vulnerable --include-transitive`, và lần ra gốc của một gói bắc cầu bằng `dotnet nuget why`.
- [ ] Giải thích vì sao `--outdated` trên dự án .NET 8 dễ dẫn tới lỗi, và dùng `--highest-patch` thay thế.
- [ ] Đặt connection string bằng user-secrets; giải thích vì sao Production không đọc nó.
- [ ] Chạy `new-dotnet8-service.sh` tạo một service mới, mở trong Rider, chạy API và test.
- [ ] Giải thích `Directory.Build.props`, CPM, `.editorconfig` mỗi cái giải quyết vấn đề gì.
- [ ] Liệt kê những gì phải đổi để lên .NET 10.

## 11. Ranh giới trung thực

- **Đã chạy thật** (Linux x64, SDK 8.0.426): mọi lệnh và output ở §1 trên OrderFlow; thí nghiệm user-secrets ở §2.2
  (trên bản sao, đã xoá secret sau đó); script §3 nhiều lần tới khi sạch, kể cả `git add` và chạy API/Worker sinh ra;
  CS8618, CPM với `dotnet add package`, NU1008, `dotnet format` ở §4; nâng .NET 10 và hai template SDK 10 ở §6.
- **Chưa chạy:** script §3 trên macOS thật (đã viết cho bash 3.2 và `sed`/`find` của BSD nhưng chỉ chạy trên Linux);
  workflow GitHub Actions §5; Docker image .NET 10; mọi thứ về Azure (§2.3, §7); menu User Secrets của Rider.
- Thời gian (64 giây cho script) đo trên máy có sẵn cache NuGet; lần đầu trên máy bạn lâu hơn.
