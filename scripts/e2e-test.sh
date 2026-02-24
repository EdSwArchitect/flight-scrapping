#!/bin/bash
# End-to-end test: requires docker compose services to be running
# Usage: ./scripts/e2e-test.sh

set -e
API_URL="${API_URL:-http://localhost:8080}"

echo "Testing API at $API_URL"

# Test list-flights
echo "GET /list-flights"
curl -sf "$API_URL/list-flights?page=0&size=5" | jq -e 'type == "array"' || (echo "Failed: list-flights" && exit 1)

# Test geobox
echo "POST /geobox-list-flight"
curl -sf -X POST "$API_URL/geobox-list-flight" \
  -H "Content-Type: application/json" \
  -d '{"minLat":30,"maxLat":40,"minLon":-120,"maxLon":-100}' | jq -e 'type == "array"' || (echo "Failed: geobox" && exit 1)

echo "E2E tests passed"
