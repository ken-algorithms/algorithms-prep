#!/usr/bin/env bash
# Kiem tra ca he thong sau "docker compose --profile app up -d --build":
# tao 2 don qua API, doi Worker xu ly qua Service Bus, in trang thai cuoi. Can curl va python3 (macOS co san).
set -euo pipefail

API="${API:-http://localhost:8080}"
SB_HEALTH="${SB_HEALTH:-http://localhost:5300/health}"

wait_for() { # wait_for <ten> <url> <so giay toi da>
  local name=$1 url=$2 max=$3 start=$SECONDS
  until curl -fs -o /dev/null "$url"; do
    if (( SECONDS - start > max )); then echo "  $name: not ready after ${max}s ($url)"; exit 1; fi
    sleep 2
  done
  echo "  $name ready after $(( SECONDS - start ))s"
}

field() { python3 -c "import json,sys; print(json.load(sys.stdin)[sys.argv[1]])" "$1"; }

echo "1) waiting for the services"
wait_for "API" "$API/health" 120
wait_for "Service Bus emulator" "$SB_HEALTH" 180

echo "2) creating two orders"
small=$(curl -fs -X POST "$API/api/orders" -H 'Content-Type: application/json' \
  -d '{"customerEmail":"an.nguyen@example.com","lines":[{"sku":"BOOK-001","quantity":2,"unitPrice":150000}]}' | field id)
big=$(curl -fs -X POST "$API/api/orders" -H 'Content-Type: application/json' \
  -d '{"customerEmail":"big.buyer@example.com","lines":[{"sku":"LAPTOP-PRO","quantity":2,"unitPrice":30000000}]}' | field id)
echo "  small order $small (300,000)"
echo "  big order   $big (60,000,000, over the 50,000,000 limit)"

echo "3) waiting for the Worker (outbox -> Service Bus -> handler)"
start=$SECONDS
for id in "$small" "$big"; do
  until [ "$(curl -fs "$API/api/orders/$id" | field status)" != "Pending" ]; do
    if (( SECONDS - start > 90 )); then echo "  still Pending after 90s: docker compose logs worker"; exit 1; fi
    sleep 1
  done
done
echo "  processed after $(( SECONDS - start ))s"

echo "4) final state"
for id in "$small" "$big"; do
  curl -fs "$API/api/orders/$id" | python3 -c "
import json, sys
o = json.load(sys.stdin)
print(f\"  {o['id']}  {o['status']:<9}  total={o['total']:>14,.0f}  reason={o['rejectReason']}\")"
done
echo "OK"
