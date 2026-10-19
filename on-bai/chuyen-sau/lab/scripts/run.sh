#!/usr/bin/env bash
# Chay mot class co main() trong lab: scripts/run.sh com.prep.cs.kafka.ConnectCheck localhost:19092
# Lan dau: compile + ghi classpath vao target/cp.txt. Sau do chi compile lai khi code doi.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f target/cp.txt || pom.xml -nt target/cp.txt ]]; then
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
fi
mvn -q compile
exec java -cp "target/classes:$(cat target/cp.txt)" "$@"
