# Test Strategy: Location Notifier Social Gathering App

**Version**: 1.0
**Date**: 2026-06-27
**Project**: Location Notifier — iOS app + Java Lambda backend
**Quality Model Reference**: `docs/quality/quality-model.md`
**Use Cases Reference**: `docs/req/use-cases/use-cases.md`

---

## Testing Philosophy

Testing for this project serves three concrete goals:

1. **Confidence to ship**: every confirmed use case (UC-001 through UC-010) has automated coverage so a regression is caught before it reaches beta users.
2. **Safety for a solo engineer**: with a single developer, there is no human safety net. Automated tests substitute for a QA team.
3. **Cost control**: tests run in CI before any deployment so broken builds never consume AWS Lambda invocations or Firestore reads.

---

## Test Scope

### In Scope

- Java Lambda handler logic (all API endpoints in `docs/arch/api/openapi.yaml`)
- Weekly scheduled job (UC-008): spot selection, exclusion logic, push dispatch, email dispatch
- Firestore read/write logic (mocked at unit level, real emulator at integration level)
- Firebase token validation middleware
- iOS ViewModels and service layer
- iOS UI flows for all confirmed use cases
- Security controls: ownership checks (T1, E1, E3 from threat model), rate limiting (D1, SR-6)

### Out of Scope

- Apple Sign In / Google Sign In OAuth flows (tested by Firebase Auth; not owned by this project)
- APNs delivery confirmation (Apple infrastructure; verified via delivery receipts, not test assertions)
- Google Places API responses (mocked in all tests; Google's reliability is not the project's responsibility)
- Firebase Auth backend (tested by Google; not owned by this project)
- Android (out of project scope)

---

## Test Layers

### Layer 1: Unit Tests

**Purpose**: Verify individual methods and classes in isolation.

**Coverage target**: Lambda >= 80% lines, >= 70% branches per file. iOS >= 70% lines.

**Tooling**:
- Java Lambda: JUnit 5 + Mockito (mock Firebase Admin SDK, Google Places client, APNs client)
- Swift/iOS: XCTest (mock URLSession, mock Firebase SDK via protocol abstraction)

**What to unit test**:

| Component | What to test |
|-----------|-------------|
| `WeeklySpotLambda` | City iteration, exclusion list application, fallback to least-recently-used venue, admin override path |
| `AuthHandler` | Token validation passes, token validation fails (401 returned), new user creation path, returning user path |
| `InviteHandler` | RSVP state transitions (INVITED to YES/NO/INTERESTED), duplicate RSVP rejection, ownership check (wrong userId returns 403) |
| `LocationHandler` | City derivation from coordinates, city-change detection, invalid session token returns 401 |
| `FeedHandler` | Current spot returned for user's city, no-spot-yet empty state, 4-week history ordering |
| `PushTokenService` | Token registration, stale token removal on APNs invalid response |
| `iOS FeedViewModel` | Loading state transitions, error state on network failure, cached data shown when offline |
| `iOS AuthViewModel` | Sign-in success stores session token in Keychain, sign-in failure surfaces error message |
| `iOS CheckinViewModel` | Distance validation (within 500m success, > 500m error), duplicate checkin error handling |

**Do not unit test**:
- Firestore query construction (covered at integration level)
- iOS SwiftUI view hierarchy (covered at UI test level)

---

### Layer 2: Integration Tests

**Purpose**: Verify that Lambda handlers interact correctly with Firestore (using the Firebase Local Emulator Suite) and that the OpenAPI contract is honored.

**Coverage target**: All ten use cases have at least one integration test covering the main success scenario and the most critical extension path.

**Tooling**:
- Java Lambda: JUnit 5 + Firebase Local Emulator Suite (`firebase emulators:start`)
- Contract testing: `openapi-validator` against `docs/arch/api/openapi.yaml` — all responses validated for schema conformance
- Google Places API: WireMock stub server for deterministic venue data

**What to integration test** (one test file per use case):

| Use Case | Integration Test Focus |
|----------|----------------------|
| UC-001 | Apple token exchange results in user created in Firestore emulator; returning user results in existing profile returned |
| UC-002 | Location POST results in city derived and written to Firestore; city-change updates homeCity |
| UC-003 | Weekly job completion results in push notification payload delivered to mock APNs stub; invalid token removed from Firestore |
| UC-004 | GET /feed/current-spot returns correct weekly spot for city; no-spot empty state returns 200 with null data |
| UC-005 | GET /feed/history returns 4 records max ordered descending by weekId; fewer than 4 available returns all present |
| UC-006 | Social signal counts computed correctly from Firestore count() aggregation; friend enrichment returns correct subset |
| UC-007 | Checkin within 500m results in record written; checkin > 500m returns 422 with TOO_FAR error code; duplicate returns 409 with ALREADY_CHECKED_IN |
| UC-008 | Full weekly job: WireMock returns candidate venues, exclusion applied, spot written, mock APNs receives payload; city with all venues excluded triggers fallback |
| UC-009 | Location update with valid token results in Firestore updated; invalid token returns 401; reverse-geocode failure results in raw coordinates stored |
| UC-010 | History query returns enriched records; empty city returns empty array 200; friend enrichment gracefully absent when friend graph unavailable |

**Contract tests**: After each handler integration test, the response JSON is validated against the OpenAPI schema for that endpoint. A schema mismatch fails the test.

---

### Layer 3: iOS UI Tests (XCUITest)

**Purpose**: Verify critical user flows end-to-end on a device simulator, stubbing the network at the URLSession layer.

**Coverage target**: The five highest-value user journeys must each have at least one passing UI test at milestone M2.

**Tooling**: XCUITest with a URLSession stub layer (inject mock responses from JSON fixture files).

**Critical flows to automate**:

1. **New user onboarding (UC-001 + UC-002)**: Launch, sign-in screen displayed, tap "Continue with Apple" (mocked), location permission prompt displayed, permission granted, main feed screen displayed
2. **Weekly spot view (UC-004)**: Authenticated launch, spot detail screen shows venue name and address and "Open in Maps" button, pull-to-refresh loads fresh data
3. **RSVP flow (UC-007 RSVP path)**: Spot detail screen, tap "I'm Here", location within range (mocked), checkin confirmation shown, social signal count incremented in UI
4. **History view (UC-005)**: Scroll below spot detail, four history cards rendered, tap first history card, historical detail screen shown
5. **Error state: no spot yet**: Stub returns no current spot, "Check back Thursday" message displayed, no crash

---

### Layer 4: Weekly Job End-to-End Test (Staging)

**Purpose**: Verify that the full weekly Lambda job pipeline runs successfully against the staging Firebase project and staging APNs credentials.

**Frequency**: Before each milestone gate and before any production deployment of the Lambda.

**How to run**: Trigger the Lambda manually via AWS CLI with the test event, targeting the staging environment. Verify:
- A new `weekly_spots` document is written with the correct `weekId`
- At least one push notification is sent (verified by a test device registered in staging)
- No errors in CloudWatch Logs
- Exclusion list updated in Firestore

This is not automated in CI because it requires real Firebase and AWS credentials. It is documented as a manual milestone checklist step.

---

## Security Test Plan

Tests derived directly from the threat model (`docs/security/threat-model.md`).

### Authentication and Authorization Tests (Critical — must pass at every milestone)

| Test ID | Threat | Test Description | Expected Result |
|---------|--------|-----------------|----------------|
| SEC-001 | S2 | Submit a self-signed JWT (not from Firebase) to any authenticated endpoint | 401 Unauthorized |
| SEC-002 | S2 | Submit a Firebase JWT with a modified `uid` claim (but valid signature on original) | 401 Unauthorized |
| SEC-003 | T1 / E1 | Authenticated as user A, call `PATCH /v1/invites/{inviteId}` where the invite belongs to user B | 403 Forbidden |
| SEC-004 | E3 | Authenticated as user A, call `GET /v1/invites/{inviteId}` with an invite belonging to user B (valid UUID format) | 403 Forbidden |
| SEC-005 | I1 | Call `GET /v1/users/{otherUserId}` where otherUserId is not the authenticated user | 403 Forbidden |
| SEC-006 | S4 | Register a push token under user A's session, then attempt to register the same device token under user B's session | Token bound to user A; user B's registration does not overwrite |
| SEC-007 | E2 | Attempt to call any internal admin Lambda invocation via API Gateway (no path should expose admin ops) | 404 Not Found |

### Input Validation Tests (High)

| Test ID | Threat | Test Description | Expected Result |
|---------|--------|-----------------|----------------|
| SEC-008 | T2 | Submit a venue name containing `<script>alert(1)</script>` via any writable field | 400 with VALIDATION_ERROR; value not stored |
| SEC-009 | T2 | Submit a display name of 10,000 characters | 400 with VALIDATION_ERROR |
| SEC-010 | T2 | Submit a location update with latitude 999.0 (out of valid range) | 400 with VALIDATION_ERROR |

### Rate Limiting Tests (High)

| Test ID | Threat | Test Description | Expected Result |
|---------|--------|-----------------|----------------|
| SEC-011 | D1 / SR-6 | Send 21 RSVP requests from the same user within 60 seconds | 21st request returns 429 Too Many Requests |

---

## Test Data Strategy

**Golden fixture set**: A set of Firestore documents used in all integration tests. Stored at `test/fixtures/firestore/`. Loaded into the emulator before each integration test suite. Includes:
- 2 cities: `seattle`, `tacoma`
- 4 weekly spots per city (covers the 4-week history window)
- 5 users: 3 with valid sessions, 1 with expired token, 1 with no push token
- 8 invites across the fixture users and spots
- Exclusion lists for both cities covering the last 4 weeks

**WireMock stubs**: Stored at `test/wiremock/mappings/`. One stub per Google Places API endpoint response variant (20 candidates, 0 candidates, API error).

**iOS JSON fixtures**: Stored at `LocationNotifierTests/Fixtures/`. One JSON file per API endpoint response variant (success, empty, network error, 403, 401).

---

## Defect Classification

| Severity | Definition | Response |
|----------|-----------|---------|
| Critical | Data loss, authentication bypass, app crash on main flow, weekly job not running | Block release; fix in current session |
| High | Feature unavailable, incorrect RSVP count, push not delivered, security finding High or above | Block milestone gate; fix in current sprint |
| Medium | UI glitch, minor data inconsistency, slow response on secondary screen | Fix before release gate |
| Low | Cosmetic issue, minor copy error | Backlog; fix when convenient |

---

## CI Integration

### CI Pipeline (GitHub Actions or equivalent)

Every push to any branch runs:

1. `mvn verify` — Java unit tests + Checkstyle + dependency-check
2. `firebase emulators:exec "mvn verify -Pintegration"` — Integration tests against Firestore emulator
3. `xcodebuild test -scheme LocationNotifier -destination "platform=iOS Simulator,name=iPhone 15"` — iOS unit tests + UI tests

Every push to `main` also runs:

4. Contract validation: `openapi-generator validate -i docs/arch/api/openapi.yaml`
5. Coverage threshold check: fail if Lambda < 80% lines or iOS < 70% lines

### Coverage Reports

- Java: JaCoCo HTML report published to CI artifact store after each run
- Swift: `xcov` generates HTML coverage report after each run
- Both are checked against thresholds in CI; below-threshold result fails the build

---

## Test Ownership

| Test Layer | Owner | Run Frequency |
|------------|-------|--------------|
| Lambda unit tests | Backend engineer | Every commit |
| Lambda integration tests | Backend engineer | Every commit |
| iOS unit tests | iOS engineer | Every commit |
| iOS UI tests | iOS engineer | Every commit to main |
| Weekly job E2E | Backend engineer | Before each milestone gate |
| Security tests (SEC-001 to SEC-011) | Backend engineer | Before M3 Integration Review and before production deployment |

---

## Testing Timeline by Milestone

| Milestone | Testing Requirement |
|-----------|-------------------|
| M1: Backend Feature-Complete (Week 5) | All Lambda unit and integration tests written and passing; coverage >= 80% lines; SEC-001 through SEC-007 passing |
| M2: iOS Feature-Complete (Week 10) | All iOS unit and UI tests written and passing; five critical UI flows automated; iOS coverage >= 70% |
| M3: Integration Review (Week 12) | Full security test suite (SEC-001 through SEC-011) passing; weekly job E2E run successful in staging; all contract tests passing |
| M4: Beta / TestFlight (Week 13) | Zero open Critical or High defects; crash-free rate >= 98% over 100+ TestFlight sessions |
| M5: App Store Launch (Week 14) | All gates above confirmed; rollback tested; no regressions since M4 |
