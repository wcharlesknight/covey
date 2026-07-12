# Integration Test Plan: WBS 1.3.6 Weekly Job Lambda

**Version**: 1.0
**Date**: 2026-07-12
**Test Class**: `com.covey.integration.WeeklyJobIntegrationTest` (new, replaces
the placeholder scenario tests in `WeeklyJobScenariosTest` for service-layer coverage)
**Infrastructure**: Firebase Local Emulator Suite (Firestore only) + WireMock for Google Places API
**Run Command**: `firebase emulators:exec --only firestore "./gradlew integrationTest"`

---

## Emulator Setup

Before each test class, the Firestore emulator is started and pre-seeded using
the fixture set defined in `weekly-job-test-data.md`. After each test method,
all emulator documents are deleted to guarantee test isolation. The WireMock
server is started on port 8089 and all tests configure `GooglePlacesClient` to
use `http://localhost:8089` as the base URL.

---

## IT-01: Full Thursday Trigger — WeeklySpots and Invites Created

**Scenario**: Happy path. The job runs against all 7 cities. Each city has eligible venues and users.

**Pre-conditions**:
- WireMock returns 5 eligible venues for each of the 7 cities (see `mock-places-7cities-success.json`)
- Firestore emulator contains 3 users per city (21 users total), each with `city` field set
- `VenueExclusionList` collection is empty for all cities

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] Firestore `weeklySpots` collection contains exactly 7 documents after the run
- [ ] Each WeeklySpot document has a non-null, unique `id` (UUID)
- [ ] Each WeeklySpot has `city` matching one of the 7 target cities
- [ ] Each WeeklySpot has `name`, `address`, `placeId`, `rating >= 4.0`, `reviewCount >= 50`, `createdAt > 0`
- [ ] Firestore `invites` collection contains exactly 21 documents (3 per city)
- [ ] Each Invite document has `status == "INVITED"`
- [ ] Each Invite document has `weeklySpotId` matching the WeeklySpot for that city
- [ ] Each Invite document has `userId` matching an existing user in that city

---

## IT-02: Partial Failure — One City Fails, Others Complete

**Scenario**: WireMock returns a `REQUEST_DENIED` error for Seattle only. The remaining 6 cities succeed.

**Pre-conditions**:
- WireMock: Seattle returns `{"status": "REQUEST_DENIED"}`; all other cities return 5 eligible venues
- Firestore emulator: 3 users per city

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] Firestore `weeklySpots` collection contains exactly 6 documents (no Seattle spot)
- [ ] Firestore `invites` collection contains exactly 18 documents (no Seattle invites)
- [ ] No exception is propagated from `executeWeeklyJob()` — partial failure is handled gracefully
- [ ] Seattle failure is recorded in the failure log or error count returned by the method (verify logging via test appender or returned result object)

---

## IT-03: Idempotency — Run Twice, No Duplicate Invites

**Scenario**: EventBridge fires twice in close succession. The job must not create duplicate Invites or WeeklySpots for the same week.

**Pre-conditions**:
- WireMock: 5 eligible venues per city
- Firestore emulator: 3 users per city, empty `weeklySpots` and `invites` collections
- Week identifier (`weekId`) is derived from the current ISO week number

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()` — first run
2. Call `WeeklyJobService.executeWeeklyJob()` — second run (same process, same week)

**Assertions**:
- [ ] After second run, `weeklySpots` still contains exactly 7 documents (not 14)
- [ ] After second run, `invites` still contains exactly 21 documents (not 42)
- [ ] Second run either detects existing documents for the current week and skips creation, or uses Firestore `merge` / conditional write semantics to avoid duplicates

**Note**: This test drives the implementation decision for idempotency. The
implementation must check for an existing `WeeklySpot` with the current
`weekId` before writing. If one exists, skip that city.

---

## IT-04: Exclusion List — Venues from Last 12 Weeks Are Skipped

**Scenario**: The top-rated venue for Seattle is on the exclusion list. The second-best venue is selected.

**Pre-conditions**:
- WireMock: Seattle returns 3 venues: `{placeId:"p1", rating:4.8}`, `{placeId:"p2", rating:4.5}`, `{placeId:"p3", rating:4.1}`
- Firestore emulator: `VenueExclusionList` collection contains `{city:"Seattle", placeId:"p1"}` and `{city:"Seattle", placeId:"p3"}`
- Firestore emulator: 2 users with `city == "Seattle"`

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] The created WeeklySpot for Seattle has `placeId == "p2"`
- [ ] `placeId "p1"` is not used
- [ ] `placeId "p3"` is not used
- [ ] 2 Invite documents are created referencing the "p2" WeeklySpot

---

## IT-05: Rotation Lookback — 12-Week Window Applied

**Scenario**: The last 12 weekly spots for Seattle all used the only eligible venue returned by Places API. The job should fail gracefully for Seattle with no spot written.

**Pre-conditions**:
- WireMock: Seattle returns 1 venue with `placeId: "p1"`
- Firestore emulator: 12 `weeklySpots` documents for Seattle with `placeId == "p1"`, timestamped across the last 12 weeks
- Firestore emulator: 2 users with `city == "Seattle"`

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] No new WeeklySpot is created for Seattle
- [ ] No Invites are created for Seattle users
- [ ] Job does not throw at the top level — city failure is contained
- [ ] Failure is logged

---

## IT-06: All 7 Cities — Correct City Scoping

**Scenario**: Verify each of the 7 target cities is processed and produces correctly scoped documents.

**Target cities**: Seattle, Tacoma, Bellevue, Renton, Kirkland, Redmond, Sammamish

**Pre-conditions**:
- WireMock: Each city returns 3 distinct venues (21 WireMock stubs with different `placeId` values)
- Firestore emulator: 1 user per city (city field set to the exact city string)

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] Exactly 7 WeeklySpot documents exist in `weeklySpots`
- [ ] Each of the 7 city names appears exactly once as the `city` field across WeeklySpot documents
- [ ] Exactly 7 Invite documents exist in `invites` (1 per user per city)
- [ ] Each Invite references the WeeklySpot for the correct city (cross-city contamination check)
- [ ] User in "Bellevue" has an Invite referencing the Bellevue WeeklySpot, not the Seattle one

---

## IT-07: No Eligible Venues After Exclusion — City Skipped

**Scenario**: All venues returned by Google Places for a city are in the exclusion list.

**Pre-conditions**:
- WireMock: Tacoma returns 2 venues (`placeId:"t1"`, `placeId:"t2"`)
- Firestore emulator: `VenueExclusionList` contains both `t1` and `t2` for Tacoma
- All other cities return valid venues

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] No WeeklySpot is created for Tacoma
- [ ] No Invites are created for Tacoma users
- [ ] WeeklySpots and Invites for all other cities are created normally
- [ ] The absence of Tacoma spot is logged as a warning

---

## IT-08: Users Without City Field Do Not Receive Invites

**Scenario**: Some users have no `city` field in Firestore (edge case from migration or partial signup).

**Pre-conditions**:
- WireMock: Seattle returns 3 eligible venues
- Firestore emulator: 3 users with `city == "Seattle"`, 2 users with no `city` field

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] Exactly 3 Invite documents exist for Seattle (not 5)
- [ ] Users without a `city` field have no corresponding Invite

---

## IT-09: Firestore Schema Validation Post-Run

**Scenario**: After a successful job run, all written documents conform to the expected schema.

**Pre-conditions**: Same as IT-01 happy path.

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`
2. Read back all `weeklySpots` and `invites` documents from the emulator

**WeeklySpot schema assertions** (per document):
- [ ] `id`: non-null String (UUID format)
- [ ] `city`: non-null String, one of the 7 valid cities
- [ ] `name`: non-null, non-empty String
- [ ] `address`: non-null, non-empty String
- [ ] `placeId`: non-null, non-empty String
- [ ] `rating`: double between 4.0 and 5.0
- [ ] `reviewCount`: int >= 50
- [ ] `createdAt`: long > 0, within 5 seconds of test execution time
- [ ] `weekId`: non-null String (ISO week format, e.g., "2026-W28")

**Invite schema assertions** (per document):
- [ ] `id`: non-null String (UUID format)
- [ ] `userId`: non-null String matching an existing user UID
- [ ] `weeklySpotId`: non-null String matching an existing WeeklySpot `id`
- [ ] `city`: non-null String
- [ ] `status`: exactly `"INVITED"`
- [ ] `createdAt`: long > 0
- [ ] `updatedAt`: long equal to `createdAt` on initial creation

---

## IT-10: FCM Dispatch Verified via WireMock

**Scenario**: Verify FCM messages are dispatched to users with registered tokens.

**Pre-conditions**:
- WireMock stub on `https://fcm.googleapis.com/v1/projects/.../messages:send` (or equivalent)
- Firestore emulator: 2 Seattle users, each with `fcmToken` field set
- WireMock: Seattle returns 3 venues

**Steps**:
1. Call `WeeklyJobService.executeWeeklyJob()`

**Assertions**:
- [ ] WireMock received exactly 2 POST requests to the FCM endpoint
- [ ] Each request body contains the correct FCM token for the target user
- [ ] Notification title in request body contains the selected spot name
