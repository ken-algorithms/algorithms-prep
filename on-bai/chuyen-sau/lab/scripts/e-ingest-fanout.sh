#!/usr/bin/env bash
# Bai 09 — fan-out cua ingest: moi ingest node giu MOT block mo cho MOI partition, gui khi day 2.000 ban ghi
# HOAC moi flush ms. Doi so partition va flush, do: request/s, request/block, record Kafka/s, byte/request,
# rac cap phat moi request va so lan GC. Hai vong de thay do nhieu cua may lab (4 vCPU dung chung voi Kafka).
# Can cum: docker compose -f docker/cluster.yml up -d --wait
#   scripts/e-ingest-fanout.sh 2>&1 | tee results/e-ingest-fanout.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BS=localhost:19092,localhost:29092,localhost:39092
echo "1 ingest node, 3 luong submit, moi cau hinh do 12 giay sau 3 giay khoi dong"
echo "vong partition flush  request/s req/block record/s  B/req vao  B/req dia  rac B/req   GC"
for round in 1 2; do
  for cfg in "3 5" "12 5" "48 5" "12 20"; do
    set -- $cfg
    out=$(scripts/run.sh com.prep.cs.counting.bench.IngestBench "$BS" 3 12 "$1" "$2" 2>&1)
    rps=$(grep -oP 'TONG: \K[0-9,]+' <<<"$out")
    pre=$(grep -oP 'truoc nen \K[0-9.]+' <<<"$out")
    recs=$(grep -oP 'block: \K[0-9,]+' <<<"$out")
    rpb=$(grep -oP 'trung binh \K[0-9.]+(?= request/block)' <<<"$out")
    disk=$(grep -oP '= \K[0-9.]+(?= byte/request$)' <<<"$out")
    alloc=$(grep -oP 'cap phat: \K[0-9,]+' <<<"$out")
    gc=$(grep -oP 'GC: \K[0-9]+ lan, [0-9]+ ms' <<<"$out")
    printf '%3d %8d %4d ms %10s %9s %8s %9s %10s %10s   %s\n' "$round" "$1" "$2" "$rps" "$rpb" "$recs" "$pre" "$disk" "$alloc" "$gc"
  done
done
