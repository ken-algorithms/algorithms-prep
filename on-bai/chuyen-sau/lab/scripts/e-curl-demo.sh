#!/usr/bin/env bash
# Bai 07 — goi API that bang curl: POST /events (202 / 400), gui lai CUNG body (khong bi dem hai lan),
# client khoi dong lai (epoch moi), roi GET /stats doc so tu Postgres.
# Can cum: docker compose -f docker/cluster.yml up -d --wait
#   scripts/e-curl-demo.sh 2>&1 | tee results/e-curl-demo.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BS=localhost:19092,localhost:29092,localhost:39092
mvn -q compile 2>&1 | grep -v JAVA_TOOL_OPTIONS
[[ -f target/cp.txt ]] || mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
LOG=$(mktemp)
java -cp "target/classes:$(cat target/cp.txt)" com.prep.cs.counting.HttpIngestServer 8080 ingest "$BS" >"$LOG" 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null; wait $PID 2>/dev/null; rm -f "$LOG"' EXIT
for _ in $(seq 1 60); do curl -s localhost:8080/metrics >/dev/null 2>&1 && break; sleep 1; done
grep -E 'nghe cong' "$LOG"

CLIENT=$(date +%s)            # moi lan chay mot client moi, de chay lai khong dung so cu
NOW=$(date +%s%3N)            # event time do client gan (ms)
ITEMS='[{"key":"feature_1","value":true},{"key":"feature_2","value":false},{"key":"hack_me","value":true},{"key":"feature_3","value":"yes"}]'
body() { printf '{"clientId":%s,"epoch":%s,"seq":%s,"ts":%s,"items":%s}' "$CLIENT" "$1" "$2" "$NOW" "$ITEMS"; }
post() {  # $1 nhan, $2 body
  printf '%-44s ' "$1"
  curl -s -o /tmp/resp.$$ -w '%{http_code}  %{time_total}s' -H 'Content-Type: application/json' --data "$2" localhost:8080/events
  printf '  %s\n' "$(cat /tmp/resp.$$)"; rm -f /tmp/resp.$$
}

echo "--- moi request: 4 item = 1 TRUE, 1 FALSE, 1 FAKE_KEY (hack_me), 1 MALFORMED (value \"yes\")"
echo "body: $(body 1 0)"
post "1. epoch 1, seq 0"                       "$(body 1 0)"
post "2. GUI LAI dung body do (mat response)"   "$(body 1 0)"
post "3. epoch 1, seq 1"                       "$(body 1 1)"
post "4. client khoi dong lai: epoch 2, seq 0"  "$(body 2 0)"
post "5. tien trinh cu (epoch 1) van gui seq 2"   "$(body 1 2)"
post "6. JSON hong"                             '{"clientId":1,"epoch":1,"seq":0,"ts":5,"items":[{"key":"a"'
post "7. thieu clientId"                        '{"epoch":1,"seq":0,"ts":5,"items":[{"key":"feature_1","value":true}]}'

FROM=$(date -u -d "@$(( NOW / 1000 - 5 ))" +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u -d "@$(( NOW / 1000 + 5 ))" +%Y-%m-%dT%H:%M:%SZ)
echo "--- 7 lan POST: 5 lan 202, 2 lan 400. Request duy nhat: (1,0) (1,1) (2,0) = 3 -> ky vong moi loai = 3"
for i in $(seq 1 30); do
  S=$(curl -s "localhost:8080/stats?from=$FROM&to=$TO")
  grep -q '"trueCount":3' <<<"$S" && break
  sleep 1
done
echo "GET /stats?from=$FROM&to=$TO"
echo "$S"
grep -E '^\[http-agg' "$LOG" | head -3
true
