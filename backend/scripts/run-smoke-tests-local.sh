#!/bin/bash
# Local Smoke Test Runner
# Runs smoke tests against a dev Lambda endpoint without requiring CI/CD merge

set -e

echo "🔍 Covey Backend - Local Smoke Test Runner"
echo "=========================================="
echo ""

# Check if we're in the backend directory
if [ ! -f "build.gradle" ]; then
  echo "❌ Error: Must be run from backend directory (where build.gradle exists)"
  exit 1
fi

# Configuration
LAMBDA_ENDPOINT="${LAMBDA_ENDPOINT:-https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev}"
FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID:-covey-dev}"

echo "📋 Configuration:"
echo "  Lambda Endpoint: $LAMBDA_ENDPOINT"
echo "  Firebase Project: $FIREBASE_PROJECT_ID"
echo ""

# Check for FIREBASE_TEST_TOKEN
if [ -z "$FIREBASE_TEST_TOKEN" ]; then
  echo "⚠️  FIREBASE_TEST_TOKEN not set in environment"
  echo ""
  echo "To obtain a test token, run:"
  echo "  1. Log in to your Firebase project:"
  echo "     firebase login"
  echo ""
  echo "  2. Get a test token (valid for 1 hour):"
  echo "     firebase --project=$FIREBASE_PROJECT_ID auth:export ./test-tokens.json --format json"
  echo ""
  echo "  3. Extract and export the token:"
  echo "     export FIREBASE_TEST_TOKEN=<token-from-json>"
  echo ""
  echo "Alternatively, for development:"
  echo "  export FIREBASE_TEST_TOKEN='test-token-for-dev'"
  echo ""
  echo "Note: If using a test token, you need a valid Firebase ID token from an actual user."
  echo "Ask another team member for a valid token, or create a test account and sign in to get one."
  echo ""
  exit 1
fi

echo "✅ Firebase test token found"
echo ""

# Build if needed
echo "🏗️  Building backend..."
./gradlew build -x test --quiet || {
  echo "❌ Build failed"
  exit 1
}

echo "✅ Build successful"
echo ""

# Run smoke tests
echo "🧪 Running smoke tests..."
echo ""

./gradlew smokeTest \
  -Denv=dev \
  -DLAMBDA_ENDPOINT="$LAMBDA_ENDPOINT" \
  -DFIREBASE_TEST_TOKEN="$FIREBASE_TEST_TOKEN" \
  --stacktrace

if [ $? -eq 0 ]; then
  echo ""
  echo "✅ All smoke tests passed!"
  exit 0
else
  echo ""
  echo "❌ Some smoke tests failed"
  echo ""
  echo "Troubleshooting:"
  echo "  1. Verify Lambda is deployed: curl $LAMBDA_ENDPOINT -X POST -H 'Content-Type: application/json' -d '{\"path\": \"/me\", \"httpMethod\": \"GET\"}'"
  echo "  2. Verify token is valid and not expired"
  echo "  3. Check Lambda logs in CloudWatch"
  echo "  4. Review test output above for specific failures"
  exit 1
fi
