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
