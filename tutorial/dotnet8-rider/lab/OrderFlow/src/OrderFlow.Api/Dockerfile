# Build context la thu muc goc solution (vi Api tham chieu Core):
#   docker build -f src/OrderFlow.Api/Dockerfile -t orderflow-api .

# ---- stage 1: build bang SDK (~1 GB, chi dung luc build) ----
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# copy csproj truoc va restore rieng: sua code khong lam mat cache cua buoc restore (cham nhat)
COPY src/OrderFlow.Core/OrderFlow.Core.csproj src/OrderFlow.Core/
COPY src/OrderFlow.Api/OrderFlow.Api.csproj src/OrderFlow.Api/
RUN dotnet restore src/OrderFlow.Api/OrderFlow.Api.csproj

COPY src/ src/
RUN dotnet publish src/OrderFlow.Api/OrderFlow.Api.csproj -c Release -o /app/publish --no-restore

# ---- stage 2: chay bang runtime ASP.NET (~220 MB), khong co SDK, khong co source ----
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS final
WORKDIR /app
COPY --from=build /app/publish .

# image .NET 8 co san user "app" (APP_UID=1654): khong chay bang root
USER $APP_UID
# .NET 8 mac dinh nghe cong 8080 (ASPNETCORE_HTTP_PORTS=8080), khong phai 80 nhu .NET 7 tro ve truoc
EXPOSE 8080
ENTRYPOINT ["dotnet", "OrderFlow.Api.dll"]
