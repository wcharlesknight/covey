# Test Strategy: WBS 1.3.6 Weekly Job Lambda

**Version**: 1.0
**Date**: 2026-07-12
**Feature**: Weekly Job Lambda — spot selection, rotation, WeeklySpot + Invite creation, notifications
**Parent Strategy**: `docs/test/test-strategy.md`
**WBS Reference**: 1.3.6.1 through 1.3.6.10

---

## Scope

This strategy covers the weekly Lambda job exclusively. It does not re-cover
endpoints already tested in other handler test suites.

### In Scope

- `WeeklyJobService.executeWeeklyJob()` and `executeForCity()` — core orchestration
- `GooglePlacesClient.searchVenues()` — API call and response parsing
- Venue exclusion list check (VenueExclusionList Firestore collection)
- Venue rotation logic (12-week lookback window)
- `WeeklySpot` document creation in Firestore
- `Invite` batch document creation per city user
- FCM push notification dispatch (1.3.6.6)
- Email notification dispatch (1.3.6.7)
- Error handling and retry logic for notification failures (1.3.6.8)
- `WeeklyJobHandler.handleRequest()` — Lambda entrypoint
- Smoke test coverage for the `/weekly-job` endpoint

### Out of Scope

- Firebase Auth token validation (covered by `AuthMiddlewareTest`)
- Google Places API reliability (mocked at all test layers)
- APNs / FCM delivery confirmation downstream (infrastructure boundary)
- AWS SES delivery confirmation (infrastructure boundary)
- EventBridge scheduling (infrastructure concern; not testable in unit/integration layer)

---

## Risk Assessment

Highest-risk components drive test prioritisation:

| Component | Risk | Rationale |
|-----------|------|-----------|
| Venue exclusion + rotation logic | High | Silent bug produces duplicate venues; no user-visible error |
| Invite batch creation | High | Missing Invites means users never see the weekly spot |
| Google Places API integration | High | Network dependency; must degrade gracefully |
| FCM dispatch | Medium | Failed push is tolerable but must be logged; retry matters |
| Firestore write (WeeklySpot) | Medium | Single write; straightforward but critical path |
| Email dispatch | Low | Nice-to-have channel; failure must not abort the job |
| Idempotency (duplicate run) | High | EventBridge may trigger twice; duplicates corrupt data |

---

## Test Layers

### Layer 1 — Unit Tests (JUnit 4 + Mockito)

**Purpose**: Verify business logic in isolation. Every branch in
`WeeklyJobService` and `GooglePlacesClient` must be reachable by a unit test
without touching real Firebase or Google APIs.

**Tooling**: JUnit 4 (existing project standard), Mockito for
`GooglePlacesClient`, `Firestore`, `FirebaseMessaging`, and SES client.

**Coverage target**:
- `WeeklyJobService`: 95% line coverage, 90% branch coverage
- `GooglePlacesClient`: 95% line coverage, 90% branch coverage
- `WeeklyJobHandler`: 85% line coverage (handler wiring is thin)
- Error handling paths: 85% branch coverage

**What unit tests verify**:
- Correct venue is selected from a candidate list
- Venue with placeId in exclusion list is skipped
- Venues used in the last 12 weeks are excluded via rotation lookback
- Empty candidate list after exclusion triggers correct exception/fallback
- `WeeklySpot` is constructed with all required fields
- `Invite` is constructed with correct userId, spotId, city, and INVITED status
- Google Places API non-OK status throws a typed exception
- Google Places API timeout triggers retry logic
- Firestore write failure is caught, logged, and does not silently succeed
- FCM send failure is caught, logged, and does not abort remaining users
- Email send failure is caught, logged, and does not abort the job
- Handler returns 200 with "Weekly job completed" on success
- Handler returns 500 with error detail on unhandled exception

**What unit tests do not verify**:
- Firestore query construction (emulator layer)
- Cross-city isolation (integration layer)
- Idempotency under concurrent execution (integration layer)

---

### Layer 2 — Integration Tests (JUnit 4 + Firebase Emulator + WireMock)

**Purpose**: Verify end-to-end data flow through the real Firestore emulator.
No mocks for Firestore; WireMock stubs Google Places API.

**Tooling**:
- Firebase Local Emulator Suite (`firebase emulators:start --only firestore`)
- WireMock for `maps.googleapis.com` responses
- JUnit 4 `@Before`/`@After` for emulator state reset between tests

**Coverage target**: All scenarios in `weekly-job-integration-tests.md` pass.

**What integration tests verify**:
- Full Thursday trigger simulation: WeeklySpots created, Invites created
- Partial failure: one city throws on Places API; other city completes
- Idempotency: running the job twice does not create duplicate Invites
- Exclusion list: venues present in VenueExclusionList are skipped
- 12-week rotation: Firestore query excludes placeIds used in last 12 weekly spots
- All 7 cities produce at least one WeeklySpot and correct Invite count
- Users with no registered city do not receive Invites
- Firestore documents match the expected schema after job completion

**What integration tests do not verify**:
- Real FCM delivery (stubbed at HTTP level)
- Real SES delivery (stubbed at HTTP level)

---

### Layer 3 — Smoke Tests (LambdaDeploymentSmokeTest.java)

**Purpose**: Verify the deployed Lambda responds correctly to a weekly job
trigger. Runs post-deploy against the real staging endpoint.

**New assertions added for 1.3.6**:
- `POST /weekly-job` returns HTTP 200
- Response body contains `"Weekly job completed"`
- CloudWatch log stream shows "Weekly job completed" within 30 seconds of invocation

**Frequency**: Triggered automatically by the GitHub Actions deploy workflow
after each merge to `main` that touches backend code.

---

## Testing Gaps in Current Codebase

The existing `WeeklyJobServiceTest` has three tests that pass trivially
(`assertTrue(true)`, no assertions on behaviour). These are placeholders and
must be replaced by the tests defined in `weekly-job-unit-tests.md`.

The existing `WeeklyJobHandlerTest` contains only `assertTrue(true)` and
provides zero coverage. It must be replaced with the handler tests in
`weekly-job-unit-tests.md`.

The existing `WeeklyJobScenariosTest` tests model construction only — it
exercises `WeeklySpot` and `Invite` constructors but not the service layer.
These tests are valid but incomplete; the integration tests in
`weekly-job-integration-tests.md` must be added alongside them.

---

## Gap Items Not Yet in Codebase

The following are required for 1.3.6 but do not yet have any source or test
implementation:

| Gap | Where Needed |
|-----|-------------|
| VenueExclusionList Firestore collection read | `WeeklyJobService` |
| 12-week rotation lookback query | `WeeklyJobService` |
| FCM dispatch logic | New `NotificationService` or inline in `WeeklyJobService` |
| Email dispatch logic | New `EmailService` or inline |
| Retry logic for external calls | `GooglePlacesClient` or `WeeklyJobService` |
| All 7 cities (current code has only Seattle + Tacoma) | `WeeklyJobService.CITY_COORDS` |

Tests in `weekly-job-unit-tests.md` and `weekly-job-integration-tests.md` are
written to drive implementation of these gaps via TDD.

---

## CI Integration

Unit tests run on every push via `./gradlew test`.

Integration tests run on every push via:
```
firebase emulators:exec --only firestore "./gradlew integrationTest"
```

Smoke tests run post-deploy only, triggered by the existing
`.github/workflows/deploy-nonprod.yml` workflow.

Coverage is reported by JaCoCo. The build fails if `WeeklyJobService` drops
below 95% line coverage or 90% branch coverage.
