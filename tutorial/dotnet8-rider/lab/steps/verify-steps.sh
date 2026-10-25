#!/usr/bin/env bash
# Dung lai OrderFlow tu mot thu muc TRONG, dung thu tu va dung lenh cua bai 06 -> 11, kiem tra o cuoi moi bai,
# roi so sanh ket qua voi lab/OrderFlow (phai giong het, tru ten file migration va GUID ngau nhien).
#
#   ./verify-steps.sh                 # chay het, khoang 8 phut; can .NET 8 SDK, dotnet-ef 8.0.32, Docker dang chay
#   ./verify-steps.sh 06 07           # chi chay vai bai, dung lai WORK cua lan truoc:  WORK=/tmp/x ./verify-steps.sh 08
#
# Truoc khi chay: tat app/container dang dung cong 5080, 5432, 5672, 5300, 8080 (vd. lab: docker compose --profile app down).
# PREBUILD_CMD + COMPOSE_BUILD=--no-build: chi dung khi docker build phai di qua proxy rieng (moi truong kiem chung cua bai).
set -euo pipefail

STEPS=$(cd "$(dirname "$0")" && pwd)
FINAL=$(cd "$STEPS/../OrderFlow" && pwd)
WORK=${WORK:-$(mktemp -d -t orderflow-steps-XXXXXX)}
APP=$WORK/OrderFlow
COMPOSE_BUILD=${COMPOSE_BUILD:---build}
export COMPOSE_PROJECT_NAME=orderflow-steps # khong dung chung container/volume voi lab
PIDS=()

log() { printf '\n== %s\n' "$*"; }
ok() { printf '  ok: %s\n' "$*"; }
fail() { printf '  FAIL: %s\n' "$*"; exit 1; }
check() { local what=$1; shift; if "$@"; then ok "$what"; else fail "$what"; fi; } # check <mo ta> <lenh kiem tra...>
final() { mkdir -p "$APP/$(dirname "$1")"; cp "$FINAL/$1" "$APP/$1"; } # file ban cuoi, lay tu lab/OrderFlow
step() { mkdir -p "$APP/$(dirname "$2")"; cp "$STEPS/$1/$2" "$APP/$2"; } # file ban tam cua bai $1, lay tu steps/$1
quiet() { "$@" > "$WORK/last-command.log" 2>&1 || { cat "$WORK/last-command.log"; fail "$*"; }; }
json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }
psql_q() { docker compose exec -T postgres psql -U orderflow -d orderflow -tAc "$1"; }
ef() { quiet dotnet ef "$@" --project src/OrderFlow.Core --startup-project src/OrderFlow.Api; }

wait_url() { # wait_url <url> <giay>
  local t=0
  until curl -fs -o /dev/null "$1"; do
    sleep 1; t=$((t + 1))
    if [ "$t" -ge "$2" ]; then fail "$1 not ready after $2 s"; fi
  done
}
start() { # start <ten-log> <lenh...>: chay nen, ghi log vao $WORK
  local name=$1; shift
  "$@" > "$WORK/$name.log" 2>&1 &
  PIDS+=($!)
}
kill_tree() { local c; for c in $(pgrep -P "$1" 2>/dev/null); do kill_tree "$c"; done; kill "$1" 2>/dev/null || true; }
stop_all() { local p; for p in "${PIDS[@]:-}"; do [ -n "$p" ] && kill_tree "$p"; done; PIDS=(); sleep 2; }
trap stop_all EXIT

post_order() { # post_order <email> <sku> <so luong> <don gia> -> in "<http code> <id>"
  curl -s -o "$WORK/resp.json" -w '%{http_code}' -X POST http://localhost:5080/api/orders -H 'Content-Type: application/json' \
    -d "{\"customerEmail\":\"$1\",\"lines\":[{\"sku\":\"$2\",\"quantity\":$3,\"unitPrice\":$4}]}"
  printf ' %s\n' "$(json "['id']" < "$WORK/resp.json" 2>/dev/null || echo -)"
}
status_of() { curl -fs "http://localhost:5080/api/orders/$1" | json "['status']"; }
wait_status() { # wait_status <id> <trang thai> <giay>
  local t=0
  until [ "$(status_of "$1")" = "$2" ]; do
    sleep 1; t=$((t + 1))
    if [ "$t" -ge "$3" ]; then fail "order $1 is $(status_of "$1"), expected $2"; fi
  done
}

stage06() {
  log "Bai 06: Web API, luu tam trong bo nho"
  mkdir -p "$APP" && cd "$APP"
  quiet dotnet new globaljson --sdk-version 8.0.100 --roll-forward latestFeature
  quiet dotnet new sln -n OrderFlow
  quiet dotnet new classlib -n OrderFlow.Core -o src/OrderFlow.Core
  quiet dotnet new webapi --use-controllers -n OrderFlow.Api -o src/OrderFlow.Api
  quiet dotnet sln add src/OrderFlow.Core src/OrderFlow.Api
  quiet dotnet add src/OrderFlow.Api reference src/OrderFlow.Core
  rm src/OrderFlow.Core/Class1.cs src/OrderFlow.Api/WeatherForecast.cs src/OrderFlow.Api/Controllers/WeatherForecastController.cs
  final src/OrderFlow.Core/Domain/Order.cs
  final src/OrderFlow.Api/Contracts/OrderContracts.cs
  final src/OrderFlow.Api/Controllers/OrdersController.cs
  final src/OrderFlow.Api/Infrastructure/DomainExceptionHandler.cs
  final src/OrderFlow.Api/Properties/launchSettings.json
  final src/OrderFlow.Api/OrderFlow.Api.http
  step 06 src/OrderFlow.Api/Program.cs
  step 06 src/OrderFlow.Api/Services/InMemoryOrderStore.cs
  step 06 src/OrderFlow.Api/Services/OrderService.cs
  quiet dotnet build -warnaserror; ok "build: 0 warning"

  start api06 dotnet run --project src/OrderFlow.Api --launch-profile http
  wait_url http://localhost:5080/health 90
  read -r code id <<< "$(post_order an@example.com book-001 2 150000)"
  check "POST -> 201 ($code), id $id" test "$code" = 201
  check "GET -> Pending" test "$(status_of "$id")" = Pending
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:5080/api/orders -H 'Content-Type: application/json' -d '{"customerEmail":"x","lines":[]}')
  check "invalid body -> 400 ($code)" test "$code" = 400
  curl -s -o "$WORK/dup.json" -X POST http://localhost:5080/api/orders -H 'Content-Type: application/json' \
    -d '{"customerEmail":"a@b.com","lines":[{"sku":"a-1","quantity":1,"unitPrice":10},{"sku":"A-1","quantity":1,"unitPrice":10}]}'
  check "duplicate sku -> problem from DomainExceptionHandler" grep -q 'appears more than once' "$WORK/dup.json"
  check "timing middleware logged the request" grep -q 'POST /api/orders -> 201 in' "$WORK/api06.log"
  stop_all
}

stage07() {
  log "Bai 07: PostgreSQL + EF Core 8"
  cd "$APP"
  quiet dotnet add src/OrderFlow.Core package Npgsql.EntityFrameworkCore.PostgreSQL --version 8.0.11
  quiet dotnet add src/OrderFlow.Core package EFCore.NamingConventions --version 8.0.3
  quiet dotnet add src/OrderFlow.Api package Microsoft.EntityFrameworkCore.Design --version 8.0.32
  quiet dotnet add src/OrderFlow.Api package Microsoft.Extensions.Diagnostics.HealthChecks.EntityFrameworkCore --version 8.0.32
  final src/OrderFlow.Core/Messaging/Messages.cs
  final src/OrderFlow.Core/Data/OutboxMessage.cs
  final src/OrderFlow.Core/Data/OrderDbContext.cs
  final src/OrderFlow.Core/DataServiceCollectionExtensions.cs
  rm src/OrderFlow.Api/Services/InMemoryOrderStore.cs
  final src/OrderFlow.Api/Services/OrderService.cs
  final src/OrderFlow.Api/Program.cs
  final src/OrderFlow.Api/appsettings.json
  final src/OrderFlow.Api/appsettings.Development.json
  step 07 docker-compose.yml
  quiet docker compose up -d --wait postgres; ok "postgres healthy"
  ef migrations add InitialCreate --output-dir Data/Migrations; ok "migration InitialCreate created"
  ef database update; ok "database updated: $(psql_q "select string_agg(tablename, ',' order by tablename) from pg_tables where schemaname='public'")"
  quiet dotnet build -warnaserror; ok "build: 0 warning"

  start api07 dotnet run --project src/OrderFlow.Api --launch-profile http
  wait_url http://localhost:5080/health 90
  read -r code id <<< "$(post_order an@example.com book-001 2 150000)"
  check "POST -> 201 ($code)" test "$code" = 201
  check "order + outbox row in one transaction" test "$(psql_q "select count(*) from outbox_messages where payload->>'orderId' = '$id'")" = 1
  stop_all
  start api07b dotnet run --project src/OrderFlow.Api --launch-profile http
  wait_url http://localhost:5080/health 90
  check "order survives an API restart (stored in PostgreSQL)" test "$(status_of "$id")" = Pending
  stop_all
}

stage08() {
  log "Bai 08: Worker Service + outbox (publisher tam chi ghi log)"
  cd "$APP"
  quiet dotnet new worker -n OrderFlow.Worker -o src/OrderFlow.Worker
  quiet dotnet sln add src/OrderFlow.Worker
  quiet dotnet add src/OrderFlow.Worker reference src/OrderFlow.Core
  quiet dotnet add src/OrderFlow.Core package Microsoft.Extensions.Options.ConfigurationExtensions --version 8.0.0
  quiet dotnet add src/OrderFlow.Core package Microsoft.Extensions.Options.DataAnnotations --version 8.0.0
  rm src/OrderFlow.Worker/Worker.cs
  final src/OrderFlow.Core/Messaging/IMessagePublisher.cs
  final src/OrderFlow.Core/Outbox/OutboxProcessor.cs
  final src/OrderFlow.Worker/OutboxDispatcherWorker.cs
  step 08 src/OrderFlow.Worker/Program.cs
  step 08 src/OrderFlow.Worker/LoggingMessagePublisher.cs
  step 08 src/OrderFlow.Worker/appsettings.json
  quiet dotnet build -warnaserror; ok "build: 0 warning"

  start api08 dotnet run --project src/OrderFlow.Api --launch-profile http
  start worker08 dotnet run --project src/OrderFlow.Worker
  wait_url http://localhost:5080/health 90
  read -r code id <<< "$(post_order binh@example.com pen-002 5 12000)"
  local t=0
  until [ "$(psql_q "select count(*) from outbox_messages where processed_at_utc is null")" = 0 ]; do
    sleep 1; t=$((t + 1))
    if [ "$t" -ge 60 ]; then fail "outbox not drained"; fi
  done
  ok "outbox drained by the Worker"
  check "LoggingMessagePublisher logged the message" grep -q "PUBLISH order-created" "$WORK/worker08.log"
  stop_all
}

stage09() {
  log "Bai 09: Azure Service Bus (emulator) + consumer idempotent"
  cd "$APP"
  quiet dotnet add src/OrderFlow.Core package Azure.Messaging.ServiceBus --version 7.21.0
  final src/OrderFlow.Core/Messaging/ServiceBusMessaging.cs
  final src/OrderFlow.Core/Messaging/OrderCreatedHandler.cs
  final src/OrderFlow.Core/MessagingServiceCollectionExtensions.cs
  rm src/OrderFlow.Worker/LoggingMessagePublisher.cs
  final src/OrderFlow.Worker/OrderCreatedConsumerWorker.cs
  final src/OrderFlow.Worker/Program.cs
  final src/OrderFlow.Worker/appsettings.json
  final servicebus/config.json
  step 09 docker-compose.yml
  quiet docker compose up -d
  wait_url http://localhost:5300/health 240; ok "Service Bus emulator healthy"
  quiet dotnet build -warnaserror; ok "build: 0 warning"

  start api09 dotnet run --project src/OrderFlow.Api --launch-profile http
  start worker09 dotnet run --project src/OrderFlow.Worker
  wait_url http://localhost:5080/health 90
  read -r _ small <<< "$(post_order an@example.com book-001 2 150000)"
  read -r _ big <<< "$(post_order big@example.com laptop-pro 2 30000000)"
  wait_status "$small" Confirmed 60; ok "small order Confirmed through Service Bus"
  wait_status "$big" Rejected 60; ok "big order Rejected"
  stop_all
}

stage10() {
  log "Bai 10: Docker hoa ca he thong"
  cd "$APP"
  final src/OrderFlow.Api/Dockerfile
  final src/OrderFlow.Worker/Dockerfile
  final .dockerignore
  final docker-compose.yml
  final scripts/smoke-test.sh
  if [ -n "${PREBUILD_CMD:-}" ]; then "$PREBUILD_CMD" "$APP"; fi
  quiet docker compose --profile app up -d $COMPOSE_BUILD
  ./scripts/smoke-test.sh > "$WORK/smoke10.log" 2>&1 || { cat "$WORK/smoke10.log"; fail "smoke test"; }
  ok "smoke test: $(tail -1 "$WORK/smoke10.log")"
  quiet docker compose --profile app down -v
}

stage11() {
  log "Bai 11: xUnit + Testcontainers"
  cd "$APP"
  quiet dotnet new xunit -n OrderFlow.UnitTests -o tests/OrderFlow.UnitTests
  quiet dotnet new xunit -n OrderFlow.IntegrationTests -o tests/OrderFlow.IntegrationTests
  quiet dotnet sln add tests/OrderFlow.UnitTests tests/OrderFlow.IntegrationTests
  quiet dotnet add tests/OrderFlow.UnitTests reference src/OrderFlow.Core
  quiet dotnet add tests/OrderFlow.IntegrationTests reference src/OrderFlow.Api src/OrderFlow.Core
  quiet dotnet add tests/OrderFlow.IntegrationTests package Microsoft.AspNetCore.Mvc.Testing --version 8.0.32
  quiet dotnet add tests/OrderFlow.IntegrationTests package Testcontainers.PostgreSql --version 4.16.0
  quiet dotnet add tests/OrderFlow.IntegrationTests package Microsoft.Extensions.TimeProvider.Testing --version 8.10.0
  quiet dotnet add tests/OrderFlow.UnitTests package xunit --version 2.9.3        # template ghim 2.5.3: keo goi co lo hong
  quiet dotnet add tests/OrderFlow.IntegrationTests package xunit --version 2.9.3
  rm tests/OrderFlow.UnitTests/UnitTest1.cs tests/OrderFlow.IntegrationTests/UnitTest1.cs
  final tests/OrderFlow.UnitTests/OrderTests.cs
  final tests/OrderFlow.UnitTests/OutboxMessageTests.cs
  final tests/OrderFlow.IntegrationTests/PostgresFixture.cs
  final tests/OrderFlow.IntegrationTests/OrdersApiTests.cs
  final tests/OrderFlow.IntegrationTests/OutboxProcessorTests.cs
  final tests/OrderFlow.IntegrationTests/OrderCreatedHandlerTests.cs
  quiet dotnet test
  grep -E 'Passed!|Failed!' "$WORK/last-command.log" | sed 's/^/  /'
  check "10 unit + 14 integration tests passed" test "$(grep -cE 'Passed!.*Failed: +0' "$WORK/last-command.log")" = 2
  quiet dotnet list package --vulnerable --include-transitive
  check "no vulnerable packages, direct or transitive" test "$(grep -c 'has no vulnerable packages' "$WORK/last-command.log")" = 5
}

compare() {
  log "So sanh voi lab/OrderFlow"
  local norm=$WORK/normalized
  rm -rf "$norm" && mkdir -p "$norm/a" "$norm/b"
  # bo phan sinh ngau nhien: GUID trong .sln, UserSecretsId, ten file migration co timestamp
  (cd "$FINAL" && find . -type f -not -path '*/bin/*' -not -path '*/obj/*' -not -path './results/*' -not -name '*.sln') | sort > "$norm/files-a"
  (cd "$APP" && find . -type f -not -path '*/bin/*' -not -path '*/obj/*' -not -name '*.sln') | sort > "$norm/files-b"
  sed -E 's#/[0-9]{14}_#/TIMESTAMP_#' "$norm/files-a" > "$norm/names-a"
  sed -E 's#/[0-9]{14}_#/TIMESTAMP_#' "$norm/files-b" > "$norm/names-b"
  diff "$norm/names-a" "$norm/names-b" || fail "file lists differ"
  ok "same $(wc -l < "$norm/names-a") files"
  local f
  while read -r f; do
    for side in a b; do
      local root=$FINAL; [ "$side" = b ] && root=$APP
      local src; src=$(cd "$root" && ls ${f/TIMESTAMP_/[0-9]*_} 2>/dev/null | head -1)
      sed -E -e 's/\r$//' -e '1s/^\xEF\xBB\xBF//' -e 's/[0-9]{14}_InitialCreate/TIMESTAMP_InitialCreate/g' \
        -e 's#<UserSecretsId>[^<]*</UserSecretsId>#<UserSecretsId/>#' "$root/$src" > "$norm/$side.cur"
    done
    cmp -s "$norm/a.cur" "$norm/b.cur" || { diff "$norm/a.cur" "$norm/b.cur" | head -20; fail "differs: $f"; }
  done < "$norm/names-a"
  ok "every file identical (after removing random GUIDs and migration timestamps)"
  diff <(cd "$FINAL" && dotnet sln list | tail -n +3 | sort) <(cd "$APP" && dotnet sln list | tail -n +3 | sort) || fail "solution projects differ"
  ok "solution lists the same 5 projects"
}

echo "WORK=$WORK"
stages=("$@")
[ ${#stages[@]} -eq 0 ] && stages=(06 07 08 09 10 11 compare)
for s in "${stages[@]}"; do
  case $s in
    compare) compare ;;
    *) "stage$s" ;;
  esac
done
[ -d "$APP" ] && (cd "$APP" && docker compose --profile app down -v > /dev/null 2>&1 || true)
log "Done"
