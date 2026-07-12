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
  echo "To generate a test token locally, you need Firebase service account credentials:"
  echo ""
  echo "Option 1: Use FirebaseTestTokenGenerator (recommended)"
  echo "  1. Ensure you have FIREBASE_CREDENTIALS_SECRET or firebase-service-account.json"
  echo "  2. Run:"
  echo "     cd backend"
  echo "     gradle compileSmokeTestJava -x test"
  echo "     CLASSPATH=\$(gradle -q printClasspath)"
  echo "     TOKEN=\$(java -cp \"\$CLASSPATH:build/classes/java/smokeTest:build/classes/java/main\" \\"
  echo "       com.covey.smoke.FirebaseTestTokenGenerator test-user-local)"
  echo "     export FIREBASE_TEST_TOKEN=\$TOKEN"
  echo ""
  echo "Option 2: Use a real Firebase ID token"
  echo "  1. Sign in to the app: ios/\$ npx expo start"
  echo "  2. Get your Firebase ID token from authStore console logs"
  echo "  3. export FIREBASE_TEST_TOKEN=<your-actual-token>"
  echo ""
  echo "Option 3: Set AWS credentials for Secrets Manager"
  echo "  export AWS_REGION=us-west-2"
  echo "  export AWS_PROFILE=<your-aws-profile>"
  echo "  export FIREBASE_CREDENTIALS_SECRET=covey/firebase-admin-dev"
  echo "  Then run this script again"
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
