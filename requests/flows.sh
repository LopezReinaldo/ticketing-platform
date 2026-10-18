#!/usr/bin/env bash
# Main flows exercised with curl. Requires: curl, jq. Usage: ./requests/flows.sh [BASE_URL]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

echo "## 1. Create event"
EVENT=$(curl -s -X POST "$BASE_URL/api/v1/events" \
  -H "Content-Type: application/json" \
  -d '{"name":"Rock Fest","date":"2027-05-20T20:00:00Z","venue":"Movistar Arena","totalCapacity":100}')
echo "$EVENT" | jq .
EVENT_ID=$(echo "$EVENT" | jq -r .id)

echo "## 2. List events / get event"
curl -s "$BASE_URL/api/v1/events" | jq .
curl -s "$BASE_URL/api/v1/events/$EVENT_ID" | jq .

echo "## 3. Availability"
curl -s "$BASE_URL/api/v1/events/$EVENT_ID/availability" | jq .

echo "## 4. Place order (202 Accepted, processed asynchronously)"
IDEMPOTENCY_KEY=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)
ORDER=$(curl -s -X POST "$BASE_URL/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"eventId\":\"$EVENT_ID\",\"customerId\":\"customer-1\",\"quantity\":2}")
echo "$ORDER" | jq .
ORDER_ID=$(echo "$ORDER" | jq -r .id)

echo "## 4b. Retry with the same Idempotency-Key returns the same order"
curl -s -X POST "$BASE_URL/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"eventId\":\"$EVENT_ID\",\"customerId\":\"customer-1\",\"quantity\":2}" | jq .

echo "## 5. Poll order status until RESERVED"
for _ in $(seq 1 10); do
  STATUS=$(curl -s "$BASE_URL/api/v1/orders/$ORDER_ID" | jq -r .status)
  echo "status=$STATUS"
  [ "$STATUS" != "PENDING" ] && break
  sleep 1
done

echo "## 6. Availability after reservation"
curl -s "$BASE_URL/api/v1/events/$EVENT_ID/availability" | jq .

echo "## 7. Confirm order"
curl -s -X POST "$BASE_URL/api/v1/orders/$ORDER_ID/confirm" | jq .

echo "## 8. Final availability"
curl -s "$BASE_URL/api/v1/events/$EVENT_ID/availability" | jq .
