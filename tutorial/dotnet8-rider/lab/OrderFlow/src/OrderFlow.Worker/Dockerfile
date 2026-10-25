# Build context la thu muc goc solution:
#   docker build -f src/OrderFlow.Worker/Dockerfile -t orderflow-worker .

FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY src/OrderFlow.Core/OrderFlow.Core.csproj src/OrderFlow.Core/
COPY src/OrderFlow.Worker/OrderFlow.Worker.csproj src/OrderFlow.Worker/
RUN dotnet restore src/OrderFlow.Worker/OrderFlow.Worker.csproj

COPY src/ src/
RUN dotnet publish src/OrderFlow.Worker/OrderFlow.Worker.csproj -c Release -o /app/publish --no-restore

# Worker khong phuc vu HTTP nen chi can runtime .NET thuong, nhe hon runtime ASP.NET
FROM mcr.microsoft.com/dotnet/runtime:8.0 AS final
WORKDIR /app
COPY --from=build /app/publish .
USER $APP_UID
ENTRYPOINT ["dotnet", "OrderFlow.Worker.dll"]
