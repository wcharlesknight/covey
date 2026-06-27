# Covey Backend — Java + AWS Lambda

Java-based backend for the Covey social gathering app, deployed as AWS Lambda functions.

---

## Project Structure

```
backend/
├── src/
│   ├── main/java/com/covey/
│   │   ├── handlers/         # Lambda handler entry points
│   │   ├── middleware/       # Cross-cutting concerns (auth, logging, etc.)
│   │   ├── services/         # Business logic
│   │   ├── models/           # Domain models
│   │   └── config/           # Configuration (Firebase, AWS, etc.)
│   └── test/java/com/covey/  # Unit + integration tests
├── build.gradle              # Gradle build configuration
├── checkstyle.xml            # Code style rules
└── gradle/                   # Gradle wrapper

```

---

## WBS 1.3.1: Auth Layer (Current)

**What's implemented:**

- ✅ `AuthMiddleware` — Validates Firebase ID tokens
- ✅ `FirebaseConfig` — Initializes Firebase Admin SDK
- ✅ `AuthHandler` — Lambda handler for auth requests
- ✅ Tests for all components

**How it works:**

1. iOS app signs in with Firebase Auth → gets ID token
2. App makes API request: `Authorization: Bearer <id-token>`
3. `AuthMiddleware.validateToken()` calls Firebase to verify token
4. If valid → extract user UID, pass downstream
5. If invalid → reject request (401 Unauthorized)

---

## Local Development

### Prerequisites

- Java 17+
- Gradle 9.6+
- Firebase service account JSON (for local testing)

### Build

```bash
cd backend

# Compile
gradle build

# Compile without tests
gradle build -x test

# Run only tests
gradle test

# Run with linting
gradle checkstyleMain
```

### Firebase Setup (Local)

To test auth locally, you need the Firebase service account JSON:

1. Go to Firebase Console → Project Settings → Service Accounts
2. Generate new private key → download JSON
3. Save as `backend/firebase-service-account.json` (git-ignored)

**Alternatively**, for integration tests without local Firebase:
```bash
gradle integrationTest
```

### Running Tests

**Unit tests** (fast, no Firebase needed):
```bash
gradle test
```

**Integration tests** (requires Firebase service account):
```bash
gradle integrationTest
```

**With coverage report:**
```bash
gradle test jacocoTestReport
# Report at: build/reports/jacoco/test/html/index.html
```

---

## Auth Layer Testing

The auth layer is tested in two ways:

### Unit Tests (`AuthMiddlewareTest`)
- Mock Firebase Admin SDK
- Test valid tokens → UID extracted
- Test invalid tokens → rejected
- Test missing/malformed tokens → rejected

### Integration Tests (`AuthHandlerTest`)
- Real Firebase integration (if credentials provided)
- Test end-to-end auth flow
- Test error handling

**Run all tests:**
```bash
gradle test
```

---

## Deployment

### To AWS Lambda (Nonprod)

Automatic on merge to main via GitHub Actions:
```bash
git checkout -b feature/auth-improvements
# Make changes
git commit -m "feat(backend): improve auth error handling"
git push origin feature/auth-improvements
# Create PR
# Merge to main
# GitHub Actions auto-deploys to Lambda dev
```

### Manual Deploy (Local)

```bash
# Build JAR
gradle build

# Deploy to Lambda dev
aws lambda update-function-code \
  --function-name covey-weekly-spot-dev \
  --zip-file fileb://build/distributions/covey-lambda.zip \
  --region us-west-2

# Test
aws lambda invoke \
  --function-name covey-weekly-spot-dev \
  --payload '{"authorizationToken":"Bearer ..."}' \
  /tmp/response.json
```

---

## Code Style

Checkstyle configuration: `backend/checkstyle.xml`

**Check style:**
```bash
gradle checkstyleMain
```

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Firebase Admin SDK | 9.2.0 | Auth, Firestore |
| AWS Lambda Core | 1.2.3 | Lambda runtime |
| AWS Lambda Events | 3.11.4 | Event types |
| Gson | 2.10.1 | JSON serialization |
| JUnit | 4.13.2 | Unit testing |
| Mockito | 5.2.0 | Mocking |

---

## Next Steps

After auth is stable:
- WBS 1.3.2: User API (GET /me, PATCH /me)
- WBS 1.3.3: Feed API (GET /me/feed)
- WBS 1.3.4: RSVP API (POST /invites/{id}/rsvp)

See `/Users/charlieknight/covey/IMPLEMENTATION_KICKOFF.md` for full schedule.
