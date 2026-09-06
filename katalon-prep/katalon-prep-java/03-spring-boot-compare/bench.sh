#!/usr/bin/env bash
# So sanh Quarkus vs Spring Boot tren CUNG mot may, CUNG mot API, CUNG mot Postgres.
# KHONG can hey/k6/wrk — load generator viet bang Java + virtual threads.
#
#   ./bench.sh
#
# Yeu cau: mot Postgres dang chay o localhost:5432 (db=testops, user/pass=postgres).
# Neu khong co, khoi dong tam bang embedded-postgres:
#   mvn -q -pl 05-postgres-depth test -Dtest=Lab01IndexesTest    # xem port trong log
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
J21="$HOME/.sdkman/candidates/java/21.0.12-tem/bin/java"
CONC="${CONC:-200}"
SECS="${SECS:-30}"

measure() {  # $1=ten  $2=lenh chay  $3=port
  echo "=== $1 ==="
  $2 > "/tmp/bench-$1.log" 2>&1 &
  local pid=$!
  # Doi tin hieu san sang + do thoi gian khoi dong tu log cua framework
  for _ in $(seq 1 60); do
    if curl -s -o /dev/null "http://localhost:$3/api/runs" -H 'X-Tenant-Id: bench' 2>/dev/null; then break; fi
    sleep 1
  done
  grep -oE '(started in|Started .* in) [0-9.]+ ?s' "/tmp/bench-$1.log" | head -1 || true
  ps -o rss= -p "$pid" | awk '{printf "RSS: %.0f MB\n", $1/1024}'
  $J21 -cp "$ROOT/03-spring-boot-compare/target/classes" \
       com.prep.spring.bench.LoadGenerator "http://localhost:$3" "$CONC" "$SECS"
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  echo
}

cd "$ROOT"
mvn -q -pl 02-quarkus-service,03-spring-boot-compare -DskipTests package

measure quarkus "$J21 -jar $ROOT/02-quarkus-service/target/quarkus-app/quarkus-run.jar" 8080
measure spring  "$J21 -jar $ROOT/03-spring-boot-compare/target/03-spring-boot-compare-1.0.0.jar" 8080
