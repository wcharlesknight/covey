# Unit Test Plan: WBS 1.3.6 Weekly Job Lambda

**Version**: 1.0
**Date**: 2026-07-12
**Test Class Targets**:
- `com.covey.services.WeeklyJobServiceTest` (replace placeholder tests)
- `com.covey.integrations.GooglePlacesClientTest` (new)
- `com.covey.handlers.WeeklyJobHandlerTest` (replace placeholder test)
**Coverage Targets**: `WeeklyJobService` 95% lines / 90% branches; `GooglePlacesClient` 95% lines / 90% branches

---

## Prerequisites

- JUnit 4 (existing project standard)
- Mockito (already in `build.gradle`)
- A `Firestore` mock injectable via constructor or `FirestoreClient` override
- The following classes must exist before tests can compile:
  - `WeeklyJobService` constructor must accept `GooglePlacesClient` and `Firestore` (currently uses static `FirestoreClient.getFirestore()` — refactor needed for testability)
  - `NotificationService` or equivalent FCM/email methods in `WeeklyJobService`
  - `VenueExclusionListRepository` or equivalent method for exclusion list reads

---

## Section A: Spot Selection Logic

### A1 — Valid input returns highest-rated eligible venue

**Method**: `WeeklyJobService.selectVenue(List<WeeklySpot> candidates, Set<String> excludedPlaceIds)`

- [ ] Given candidates `[{rating:4.8, reviews:200, placeId:"p1"}, {rating:4.2, reviews:80, placeId:"p2"}]` and empty exclusion set, selected venue has `placeId == "p1"`
- [ ] Given candidates sorted ascending by rating, selected venue is still the highest-rated (selection is not order-dependent)
- [ ] Given two candidates with identical ratings, one is selected without throwing an exception

### A2 — Excluded venue is skipped

**Method**: `WeeklyJobService.selectVenue(List<WeeklySpot> candidates, Set<String> excludedPlaceIds)`

- [ ] Given candidates `[{placeId:"p1"}, {placeId:"p2"}]` and exclusion set `{"p1"}`, selected venue has `placeId == "p2"`
- [ ] Given candidates where all placeIds are in the exclusion set, method throws `NoEligibleVenueException` (or equivalent checked exception)
- [ ] Given a single candidate whose placeId is not excluded, that candidate is returned

### A3 — Venue with rating below minimum is filtered by GooglePlacesClient

**Method**: `GooglePlacesClient.searchVenues(String city, double lat, double lng)`

- [ ] Given API response with a place having `rating: 3.9`, that place is not included in the returned list
- [ ] Given API response with a place having `rating: 4.0` (exact minimum), that place is included
- [ ] Given API response with a place having `user_ratings_total: 49`, that place is not included
- [ ] Given API response with a place having `user_ratings_total: 50` (exact minimum), that place is included
- [ ] Given API response where a place has no `rating` field, that place is not included
- [ ] Given API response where a place has no `user_ratings_total` field, that place is not included

---

## Section B: Venue Rotation (12-Week Lookback)

### B1 — Rotation query produces correct lookback window

**Method**: `WeeklyJobService.getRecentlyUsedPlaceIds(String city, int weekCount)`

- [ ] Given 15 WeeklySpot documents for city "Seattle" in Firestore mock, query returns only the 12 most recent `placeId` values
- [ ] Given fewer than 12 documents, query returns all available `placeId` values (no exception)
- [ ] Given zero documents in the collection for the city, returns an empty set
- [ ] Query filters by city — documents from "Tacoma" are not included in "Seattle" results
- [ ] Returned set contains `placeId` strings, not document IDs

### B2 — Rotation integrates with venue selection

**Method**: `WeeklyJobService.executeForCity(String city, double lat, double lng)`

- [ ] Given the top-rated candidate is in the 12-week exclusion set and a second candidate is not, the second candidate is selected
- [ ] Given all candidates are in the rotation exclusion set, `NoEligibleVenueException` is thrown

---

## Section C: WeeklySpot Document Creation

### C1 — Valid WeeklySpot is written to Firestore

**Method**: `WeeklyJobService.saveWeeklySpot(WeeklySpot spot)`

- [ ] Given a valid `WeeklySpot`, `Firestore.collection("weeklySpots").document(spot.getId()).set(spot)` is called exactly once
- [ ] The document ID written matches `spot.getId()` (UUID set before calling save)
- [ ] `WeeklySpot` fields `city`, `name`, `address`, `placeId`, `rating`, `reviewCount`, `createdAt` are all non-null after construction

### C2 — Firestore write failure is handled

**Method**: `WeeklyJobService.saveWeeklySpot(WeeklySpot spot)`

- [ ] Given `Firestore.set()` throws `ExecutionException`, the exception propagates and the city's job fails
- [ ] Failure is logged via the context logger before re-throwing (verified via Mockito `verify`)

---

## Section D: Invite Batch Creation

### D1 — Invites created for all users in city

**Method**: `WeeklyJobService.createInvites(String city, WeeklySpot spot, List<User> users)`

- [ ] Given 3 users in "Seattle", exactly 3 Invite documents are written to Firestore
- [ ] Each Invite has `userId` matching the corresponding user's UID
- [ ] Each Invite has `weeklySpotId` matching `spot.getId()`
- [ ] Each Invite has `city == "Seattle"`
- [ ] Each Invite has `status == Invite.Status.INVITED`
- [ ] Each Invite has a non-null UUID as its document ID

### D2 — Zero users in city produces no Invites

**Method**: `WeeklyJobService.createInvites(String city, WeeklySpot spot, List<User> users)`

- [ ] Given empty user list, zero calls to `Firestore.collection("invites").document().set()` are made
- [ ] Method returns without throwing an exception

### D3 — Invite Firestore write failure is isolated

**Method**: `WeeklyJobService.createInvites(...)`

- [ ] Given 3 users and the second write throws `ExecutionException`, the third Invite is still attempted
- [ ] Failure for the second user is logged
- [ ] Method does not re-throw; overall city job continues

---

## Section E: FCM Push Notification Dispatch

### E1 — FCM message sent to users with registered tokens

**Method**: `NotificationService.sendPushNotifications(WeeklySpot spot, List<User> usersWithTokens)`

- [ ] Given 2 users each with one FCM token, `FirebaseMessaging.send()` is called exactly 2 times
- [ ] Message payload contains `spot.getName()` in the notification title
- [ ] Message payload contains `spot.getAddress()` in the notification body
- [ ] Message is sent to the correct token for each user

### E2 — Users without FCM tokens are skipped

- [ ] Given a user with null or empty `fcmToken`, no `FirebaseMessaging.send()` call is made for that user
- [ ] Other users in the list still receive their push

### E3 — FCM send failure is caught and logged, job continues

- [ ] Given `FirebaseMessaging.send()` throws for user 1, user 2's notification is still attempted
- [ ] Exception is logged via the context logger
- [ ] Method does not propagate the exception

### E4 — Google Places API timeout triggers retry

**Method**: `GooglePlacesClient.searchVenues(String city, double lat, double lng)`

- [ ] Given the HTTP call throws `SocketTimeoutException` on the first attempt and succeeds on the second, result is returned without exception (requires retry logic in `GooglePlacesClient`)
- [ ] After 3 consecutive timeouts, a typed exception is thrown (do not retry infinitely)

---

## Section F: Email Notification Dispatch

### F1 — Email sent for each eligible user

**Method**: `EmailService.sendWeeklyEmails(WeeklySpot spot, List<User> usersWithEmail)`

- [ ] Given 3 users with email addresses, SES (or equivalent) send method is called 3 times
- [ ] Email subject contains the spot name
- [ ] Email body contains the spot address and city

### F2 — Users without email address are skipped

- [ ] Given a user with null email, no send call is made for that user

### F3 — Email send failure does not abort the job

- [ ] Given SES throws for one user, remaining emails are still attempted
- [ ] Failure is logged; method does not re-throw

---

## Section G: Error Handling

### G1 — Google Places API non-OK status throws exception

**Method**: `GooglePlacesClient.searchVenues(...)`

- [ ] Given API response `{"status": "REQUEST_DENIED"}`, `Exception` is thrown with message containing "REQUEST_DENIED"
- [ ] Given API response `{"status": "ZERO_RESULTS"}`, `Exception` is thrown (no venues found)
- [ ] Given malformed JSON response, `JsonParseException` or equivalent propagates

### G2 — Handler returns 200 on success

**Method**: `WeeklyJobHandler.handleRequest(event, context)`

- [ ] Given `WeeklyJobService.executeWeeklyJob()` completes without exception, response `statusCode` is 200
- [ ] Response body JSON contains `"message": "Weekly job completed successfully"`

### G3 — Handler returns 500 on unhandled exception

**Method**: `WeeklyJobHandler.handleRequest(event, context)`

- [ ] Given `WeeklyJobService.executeWeeklyJob()` throws any `Exception`, response `statusCode` is 500
- [ ] Response body JSON contains the exception message in an `"error"` field
- [ ] Exception is logged via `context.getLogger().log()` before returning

### G4 — City-level failure does not abort other cities

**Method**: `WeeklyJobService.executeWeeklyJob()`

- [ ] Given "Seattle" throws `NoEligibleVenueException` and "Tacoma" succeeds, "Tacoma" job completes and WeeklySpot + Invites are written
- [ ] The Seattle failure is logged
- [ ] Method does not re-throw for the individual city; it continues to the next city

---

## Deprecated / Placeholder Tests to Replace

The following existing tests must be deleted and replaced with the tests above:

| File | Test | Reason |
|------|------|--------|
| `WeeklyJobServiceTest` | `testWeeklyJobServiceExists` | Tests `assertNotNull(weeklyJobService)` — zero behaviour coverage |
| `WeeklyJobServiceTest` | `testCityCoordinates` | `assertTrue(true)` — no assertion |
| `WeeklyJobServiceTest` | `testVenueSelectionCriteria` | Tests model construction only, not service logic |
| `WeeklyJobHandlerTest` | `testWeeklyJobHandlerExists` | `assertTrue(true)` — no assertion |
