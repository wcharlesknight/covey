# Manual Notification Testing Guide

This guide explains how to manually test push notifications (FCM) and email notifications (SES) using the `/notifications/send-test` endpoint.

## Overview

The manual trigger endpoint allows you to send notifications on-demand without waiting for the scheduled Friday 9am delivery run. This is useful for:
- Local development and debugging
- QA testing before production
- Verifying email/push templates
- Testing token deactivation and error handling

## Prerequisites

1. **Firebase project access** - You need a valid Firebase token for authentication
2. **API Gateway endpoint** - Access to the Lambda API endpoint
3. **curl, Postman, or similar HTTP client**

## Getting a Firebase Test Token

### Option 1: Using the Smoke Test Token Generator (Recommended for Local Dev)

```bash
cd backend

# Compile the test utilities
gradle compileSmokeTestJava -x test

# Generate classpath
CLASSPATH=$(gradle -q printClasspath)

# Generate a test token
TOKEN=$(java -cp "$CLASSPATH:build/classes/java/smokeTest:build/classes/java/main" \
  com.covey.smoke.FirebaseTestTokenGenerator test-user)

echo "Your token: $TOKEN"
```

### Option 2: Using Firebase CLI (Production)

```bash
firebase login
firebase auth:export tokens.json --project covey-dev

# Extract token from the output
```

### Option 3: Copy from Browser DevTools

1. Log in to your app in the browser
2. Open DevTools → Application → Local Storage
3. Look for Firebase Auth token in `firebase:authUser:*`

## Sending Test Notifications

### Endpoint Details

- **URL:** `POST /notifications/send-test`
- **Base:** `https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev`
- **Auth:** Bearer token (Firebase ID token)
- **Content-Type:** `application/json`

### Request Body Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Target user ID (Firebase UID) |
| `weekId` | string | Yes | ISO week ID (format: `YYYY-Www`, e.g., `2026-W30`) |
| `city` | string | Yes | City name (e.g., `Seattle`, `Tacoma`) |
| `channel` | string | Yes | Notification channel: `FCM` or `EMAIL` |

### Example 1: Send Push Notification (FCM)

```bash
TOKEN="your-firebase-token-here"

curl -X POST https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev/notifications/send-test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "weekId": "2026-W30",
    "city": "Seattle",
    "channel": "FCM"
  }'
```

**Success Response (200):**
```json
{
  "message": "Notification sent successfully",
  "data": {
    "userId": "user123",
    "weekId": "2026-W30",
    "city": "Seattle",
    "channel": "FCM"
  }
}
```

### Example 2: Send Email Notification (SES)

```bash
TOKEN="your-firebase-token-here"

curl -X POST https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev/notifications/send-test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "weekId": "2026-W30",
    "city": "Seattle",
    "channel": "EMAIL"
  }'
```

### Example 3: Multiple Cities/Users (Shell Script)

```bash
#!/bin/bash

TOKEN="your-firebase-token-here"
API="https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev"

# Test both cities, both channels
USERS=("user1" "user2")
CITIES=("Seattle" "Tacoma")
CHANNELS=("FCM" "EMAIL")
WEEK="2026-W30"

for user in "${USERS[@]}"; do
  for city in "${CITIES[@]}"; do
    for channel in "${CHANNELS[@]}"; do
      echo "Sending $channel to $user in $city..."
      
      curl -X POST "$API/notifications/send-test" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
          \"userId\": \"$user\",
          \"weekId\": \"$WEEK\",
          \"city\": \"$city\",
          \"channel\": \"$channel\"
        }" \
        -w "\nStatus: %{http_code}\n"
      
      sleep 1  # Rate limit between requests
    done
  done
done
```

## Using Postman

### 1. Import Collection

Create a new Postman collection with these requests:

**Request 1: Get Firebase Token**
- Method: `GET`
- URL: (Copy your token from browser DevTools)
- Store as environment variable: `firebase_token`

**Request 2: Send FCM Notification**
- Method: `POST`
- URL: `{{api_base}}/notifications/send-test`
- Headers:
  ```
  Authorization: Bearer {{firebase_token}}
  Content-Type: application/json
  ```
- Body (raw JSON):
  ```json
  {
    "userId": "{{user_id}}",
    "weekId": "{{week_id}}",
    "city": "{{city}}",
    "channel": "FCM"
  }
  ```

**Request 3: Send Email Notification**
- Same as above, but change `"channel"` to `"EMAIL"`

### 2. Set Environment Variables

In Postman, create an environment with:
```json
{
  "api_base": "https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev",
  "firebase_token": "your-token-here",
  "user_id": "test-user",
  "week_id": "2026-W30",
  "city": "Seattle"
}
```

## Error Responses

### 400 - Bad Request

**Missing required fields:**
```json
{
  "error": "Missing required fields: userId, weekId, city, channel"
}
```

**Invalid channel:**
```json
{
  "error": "Invalid channel: SLACK. Use FCM or EMAIL"
}
```

### 401 - Unauthorized

```json
{
  "error": "Unauthorized"
}
```

**Fix:** Ensure Bearer token is valid and not expired.

### 500 - Server Error

```json
{
  "error": "Error: {detailed error message}"
}
```

**Common causes:**
- User doesn't exist in Firestore
- Weekly spot not found for city/week
- No active FCM tokens for user
- SES rate limit or configuration issue

## Debugging Tips

### Check Logs

View Lambda execution logs in CloudWatch:
```bash
aws logs tail /aws/lambda/covey-weekly-spot-dev --follow --region us-west-2
```

### Verify User/Spot Data

```bash
# Check if user exists
firebase firestore:get users/user123 --project covey-dev

# Check if weekly spot exists
firebase firestore:get weeklySpots/Seattle_2026-W30 --project covey-dev

# Check if user has active tokens
firebase firestore:get users/user123/pushTokens --project covey-dev
```

### Test with Invalid Data

Try sending with non-existent user/week to test error handling:

```bash
TOKEN="your-token"

# Non-existent user
curl -X POST https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev/notifications/send-test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "nonexistent-user-xyz",
    "weekId": "2026-W30",
    "city": "Seattle",
    "channel": "FCM"
  }'
```

## Testing Workflow

### 1. Local Development

```bash
# Start your iOS app or have a test user logged in
npx expo run:ios

# Get Firebase token from app or use token generator
TOKEN=$(java -cp ... com.covey.smoke.FirebaseTestTokenGenerator test-user)

# Send test notification
curl -X POST http://localhost:3000/notifications/send-test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "weekId": "2026-W30",
    "city": "Seattle",
    "channel": "FCM"
  }'

# Check console/logs for delivery confirmation
```

### 2. QA Testing

1. Create test users with different device types (iOS, Android)
2. Register their push tokens via `/push-tokens` endpoint
3. Send test notifications to each:
   ```bash
   curl ... -d '{"userId": "ios-test", "weekId": "...", "city": "...", "channel": "FCM"}'
   curl ... -d '{"userId": "android-test", "weekId": "...", "city": "...", "channel": "FCM"}'
   ```
4. Verify notifications appear on devices
5. Test email by sending to verified SES recipients:
   ```bash
   curl ... -d '{"userId": "qa-user", "weekId": "...", "city": "...", "channel": "EMAIL"}'
   ```

### 3. Pre-Production Validation

Before Friday 9am delivery run:
1. Send test notifications to all target users
2. Verify FCM reaches all devices with active tokens
3. Verify emails land in inbox (not spam)
4. Check that invalid tokens are marked inactive
5. Monitor CloudWatch logs for any errors

## Observability

### CloudWatch Logs

Search for notification sends:
```bash
aws logs filter-log-events \
  --log-group-name /aws/lambda/covey-weekly-spot-dev \
  --filter-pattern "Sending test notification" \
  --region us-west-2
```

### Metrics

Custom metrics emitted:
- `NotificationsSent` (count)
- `NotificationsFailed` (count)
- `FcmLatency` (ms)
- `SesLatency` (ms)

View in CloudWatch:
```bash
aws cloudwatch get-metric-statistics \
  --namespace Covey \
  --metric-name NotificationsSent \
  --start-time 2026-07-11T00:00:00Z \
  --end-time 2026-07-12T00:00:00Z \
  --period 3600 \
  --statistics Sum
```

## FAQ

**Q: Can I send notifications without authentication?**  
A: No. All requests require a valid Firebase Bearer token for security.

**Q: Does this affect the scheduled Friday delivery?**  
A: No. Manual sends are independent and don't interfere with EventBridge scheduling.

**Q: What happens if the user doesn't have active FCM tokens?**  
A: The request succeeds but no device receives the push. Check Firebase console for registered tokens.

**Q: Can I test email to non-verified SES recipients?**  
A: Only in sandbox mode. Production SES requires verified recipient addresses. Contact DevOps to verify test emails.

**Q: How do I monitor if emails actually arrived?**  
A: Check SES delivery dashboard in AWS console or set up SNS notifications for SES bounces/complaints.

## Related Documentation

- [Notification Architecture](../arch/weekly-job/implementation-sequence.md)
- [Firebase Setup Guide](../../ios/FIREBASE_SETUP.md)
- [AWS SES Configuration](../devops/aws-ses-setup.md)
- [FCM Setup for iOS](../devops/fcm-ios-setup.md)
