#!/bin/bash
# Diagnostic script to test Lambda endpoints and see actual responses

LAMBDA_ENDPOINT="${1:-https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev}"
TOKEN="${2:-test-token}"

echo "🧪 Testing Lambda Endpoints"
echo "==========================="
echo "Endpoint: $LAMBDA_ENDPOINT"
echo ""

# Test 1: /auth endpoint (no auth required)
echo "1️⃣  Testing POST /auth (no auth required)"
curl -s -X POST "$LAMBDA_ENDPOINT/auth" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -w "\nHTTP Status: %{http_code}\n" | head -20
echo ""

# Test 2: /me endpoint (requires auth)
echo "2️⃣  Testing GET /me (requires auth)"
curl -s -X GET "$LAMBDA_ENDPOINT/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" | head -20
echo ""

# Test 3: /push-tokens endpoint (requires auth)
echo "3️⃣  Testing POST /push-tokens (requires auth)"
curl -s -X POST "$LAMBDA_ENDPOINT/push-tokens" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"token": "test-token"}' \
  -w "\nHTTP Status: %{http_code}\n" | head -20
echo ""

# Test 4: /invites/{id}/rsvp endpoint
echo "4️⃣  Testing POST /invites/test-id/rsvp (requires auth)"
curl -s -X POST "$LAMBDA_ENDPOINT/invites/test-id/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "yes"}' \
  -w "\nHTTP Status: %{http_code}\n" | head -20
echo ""

# Test 5: /weekly-job endpoint
echo "5️⃣  Testing POST /weekly-job (no auth required)"
curl -s -X POST "$LAMBDA_ENDPOINT/weekly-job" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -w "\nHTTP Status: %{http_code}\n" | head -20
echo ""

echo "✅ Diagnostic complete"
