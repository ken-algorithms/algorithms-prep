#!/usr/bin/env bash
# Bai 07 — chi phi tang HTTP tren moi request: JDK HttpServer va Netty, che do noop (chi HTTP) va ingest
# (HTTP + giai ma + phan loai + Kafka acks=all). CPU do tren TIEN TRINH SERVER qua /metrics.
# Can cum: docker compose -f docker/cluster.yml up -d --wait
#   scripts/e-http-bench.sh 2>&1 | tee results/e-http-bench.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BS=localhost:19092,localhost:29092,localhost:39092
mvn -q compile 2>&1 | grep -v JAVA_TOOL_OPTIONS
[[ -f target/cp.txt ]] || mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"
bench() {  # $1 nhan, $2 cong, $3 so ket noi, $4.. lenh server
  local label=$1 port=$2 conns=$3; shift 3
  java -Dsun.net.httpserver.nodelay=true -cp "$CP" "$@" >/dev/null 2>&1 &
  local pid=$!
  for _ in $(seq 1 60); do curl -s "localhost:$port/metrics" >/dev/null 2>&1 && break; sleep 1; done
  java -cp "$CP" com.prep.cs.counting.bench.RawHttpLoad localhost "$port" 64 10 >/dev/null 2>&1   # khoi dong JIT
  printf '%-40s ' "$label"
  java -cp "$CP" com.prep.cs.counting.bench.RawHttpLoad localhost "$port" "$conns" 15 2>&1 | grep -v JAVA_TOOL_OPTIONS
  kill "$pid"; wait "$pid" 2>/dev/null
}
bench "JDK HttpServer, noop"            8081 64   com.prep.cs.counting.HttpIngestServer 8081 noop
bench "Netty 2 event loop, noop"        8082 64   com.prep.cs.counting.NettyIngestServer 8082 noop "$BS" 2
bench "Netty, ingest, 64 ket noi"       8082 64   com.prep.cs.counting.NettyIngestServer 8082 ingest "$BS" 2
bench "Netty, ingest, 1024 ket noi"     8082 1024 com.prep.cs.counting.NettyIngestServer 8082 ingest "$BS" 2
true
