# Test Data Setup: WBS 1.3.6 Weekly Job Lambda

**Version**: 1.0
**Date**: 2026-07-12
**Used By**: `weekly-job-unit-tests.md`, `weekly-job-integration-tests.md`

---

## 1. Mock Google Places API Responses (WireMock)

WireMock stubs live at `backend/src/test/resources/wiremock/mappings/`.

### 1.1 Success Response — 5 Eligible Venues (generic, used per city)

**File**: `places-success-5-venues.json`

```json
{
  "status": "OK",
  "results": [
    {
      "name": "The Taproom",
      "vicinity": "100 First Ave",
      "place_id": "place_001",
      "rating": 4.8,
      "user_ratings_total": 320
    },
    {
      "name": "Harbor Lights Bar",
      "vicinity": "200 Harbor St",
      "place_id": "place_002",
      "rating": 4.6,
      "user_ratings_total": 210
    },
    {
      "name": "The Local Pub",
      "vicinity": "301 Pine St",
      "place_id": "place_003",
      "rating": 4.5,
      "user_ratings_total": 180
    },
    {
      "name": "Northside Bar",
      "vicinity": "400 Oak Ave",
      "place_id": "place_004",
      "rating": 4.2,
      "user_ratings_total": 90
    },
    {
      "name": "Corner Tap",
      "vicinity": "501 Elm Blvd",
      "place_id": "place_005",
      "rating": 4.0,
      "user_ratings_total": 55
    }
  ]
}
```

### 1.2 Success Response — Single Eligible Venue (for rotation/exclusion tests)

**File**: `places-success-1-venue.json`

```json
{
  "status": "OK",
  "results": [
    {
      "name": "The Only Bar",
      "vicinity": "1 Main St",
      "place_id": "place_only",
      "rating": 4.5,
      "user_ratings_total": 100
    }
  ]
}
```

### 1.3 Mixed Response — Below-Threshold Venues Included

**File**: `places-mixed-eligibility.json`

Used to verify that `GooglePlacesClient` filters out sub-threshold entries.

```json
{
  "status": "OK",
  "results": [
    {
      "name": "Eligible Bar",
      "vicinity": "10 Good St",
      "place_id": "eligible_001",
      "rating": 4.3,
      "user_ratings_total": 75
    },
    {
      "name": "Low Rating Bar",
      "vicinity": "20 Bad St",
      "place_id": "ineligible_rating",
      "rating": 3.8,
      "user_ratings_total": 200
    },
    {
      "name": "Low Reviews Bar",
      "vicinity": "30 Sparse St",
      "place_id": "ineligible_reviews",
      "rating": 4.7,
      "user_ratings_total": 30
    },
    {
      "name": "No Rating Bar",
      "vicinity": "40 Unknown St",
      "place_id": "no_rating"
    }
  ]
}
```

### 1.4 API Error Response — REQUEST_DENIED

**File**: `places-error-request-denied.json`

```json
{
  "status": "REQUEST_DENIED",
  "error_message": "This API project is not authorized to use this API."
}
```

### 1.5 API Error Response — ZERO_RESULTS

**File**: `places-error-zero-results.json`

```json
{
  "status": "ZERO_RESULTS",
  "results": []
}
```

### 1.6 Timeout Simulation

WireMock fixed delay mapping for timeout tests:

**File**: `places-timeout.json`

```json
{
  "request": { "method": "GET", "urlPattern": "/maps/api/place/nearbysearch/json.*timeout=true.*" },
  "response": {
    "fixedDelayMilliseconds": 31000,
    "status": 200,
    "body": "{\"status\":\"OK\",\"results\":[]}"
  }
}
```

### 1.7 Per-City Stubs for IT-06 (All 7 Cities)

One WireMock mapping per city, each returning 3 venues with unique `placeId` prefixes to prevent cross-city contamination in assertions.

| City | placeId Prefix | File |
|------|---------------|------|
| Seattle | `sea_` | `places-seattle-3-venues.json` |
| Tacoma | `tac_` | `places-tacoma-3-venues.json` |
| Bellevue | `bel_` | `places-bellevue-3-venues.json` |
| Renton | `ren_` | `places-renton-3-venues.json` |
| Kirkland | `kir_` | `places-kirkland-3-venues.json` |
| Redmond | `red_` | `places-redmond-3-venues.json` |
| Sammamish | `sam_` | `places-sammamish-3-venues.json` |

---

## 2. Firestore Emulator Seed Data

Seed documents live at `backend/src/test/resources/fixtures/firestore/`.
The test `@Before` method loads these via the Firestore Admin SDK pointed at
the emulator (`FIRESTORE_EMULATOR_HOST=localhost:8080`).

### 2.1 Users Collection

**File**: `fixtures/users.json`

```json
[
  { "uid": "user_sea_1", "email": "alice@test.com", "city": "Seattle", "fcmToken": "fcm_token_sea_1", "displayName": "Alice" },
  { "uid": "user_sea_2", "email": "bob@test.com",   "city": "Seattle", "fcmToken": "fcm_token_sea_2", "displayName": "Bob" },
  { "uid": "user_sea_3", "email": "carol@test.com", "city": "Seattle", "fcmToken": null,               "displayName": "Carol" },
  { "uid": "user_tac_1", "email": "dave@test.com",  "city": "Tacoma",  "fcmToken": "fcm_token_tac_1", "displayName": "Dave" },
  { "uid": "user_tac_2", "email": "eve@test.com",   "city": "Tacoma",  "fcmToken": "fcm_token_tac_2", "displayName": "Eve" },
  { "uid": "user_tac_3", "email": "frank@test.com", "city": "Tacoma",  "fcmToken": "fcm_token_tac_3", "displayName": "Frank" },
  { "uid": "user_bel_1", "email": "grace@test.com", "city": "Bellevue","fcmToken": "fcm_token_bel_1", "displayName": "Grace" },
  { "uid": "user_ren_1", "email": "henry@test.com", "city": "Renton",  "fcmToken": "fcm_token_ren_1", "displayName": "Henry" },
  { "uid": "user_kir_1", "email": "iris@test.com",  "city": "Kirkland","fcmToken": "fcm_token_kir_1", "displayName": "Iris" },
  { "uid": "user_red_1", "email": "jack@test.com",  "city": "Redmond", "fcmToken": "fcm_token_red_1", "displayName": "Jack" },
  { "uid": "user_sam_1", "email": "kate@test.com",  "city": "Sammamish","fcmToken": "fcm_token_sam_1","displayName": "Kate" },
  { "uid": "user_no_city", "email": "anon@test.com", "fcmToken": null,  "displayName": "NoCityUser" }
]
```

Notes:
- `user_sea_3` has no FCM token — used in E2 (users without token are skipped)
- `user_no_city` has no `city` field — used in IT-08

### 2.2 VenueExclusionList Collection

**File**: `fixtures/venue-exclusion-list.json`

```json
[
  { "id": "excl_sea_1", "city": "Seattle",  "placeId": "place_001", "weekId": "2026-W27" },
  { "id": "excl_sea_2", "city": "Seattle",  "placeId": "place_003", "weekId": "2026-W26" },
  { "id": "excl_tac_1", "city": "Tacoma",   "placeId": "tac_001",   "weekId": "2026-W27" }
]
```

- `place_001` (highest-rated Seattle venue) is excluded — used in IT-04
- `tac_001` is excluded; used in conjunction with IT-07 variant

### 2.3 WeeklySpots Collection — 12-Week History (for rotation tests)

**File**: `fixtures/weekly-spots-history.json`

Pre-populate 12 Seattle WeeklySpot documents, each referencing `placeId: "place_only"`,
timestamped weekly going back 12 weeks from the test run date (2026-07-12, ISO week W28):

| weekId | placeId | city |
|--------|---------|------|
| 2026-W27 | place_only | Seattle |
| 2026-W26 | place_only | Seattle |
| 2026-W25 | place_only | Seattle |
| 2026-W24 | place_only | Seattle |
| 2026-W23 | place_only | Seattle |
| 2026-W22 | place_only | Seattle |
| 2026-W21 | place_only | Seattle |
| 2026-W20 | place_only | Seattle |
| 2026-W19 | place_only | Seattle |
| 2026-W18 | place_only | Seattle |
| 2026-W17 | place_only | Seattle |
| 2026-W16 | place_only | Seattle |

Used in IT-05 to verify the 12-week rotation window blocks this venue.

### 2.4 Empty State (Baseline)

For tests that need a clean slate, the test `@Before` deletes all documents
from `weeklySpots`, `invites`, and `VenueExclusionList` before seeding.
The helper method is `EmulatorUtils.clearCollections(firestore, List.of("weeklySpots", "invites", "VenueExclusionList"))`.

---

## 3. Test Cities

All integration tests must cover all 7 cities. The full set is:

| City | Coordinates (lat, lng) | WireMock Stub Key |
|------|------------------------|-------------------|
| Seattle | 47.6062, -122.3321 | `seattle` |
| Tacoma | 47.2529, -122.4443 | `tacoma` |
| Bellevue | 47.6101, -122.2015 | `bellevue` |
| Renton | 47.4829, -122.2171 | `renton` |
| Kirkland | 47.6815, -122.2087 | `kirkland` |
| Redmond | 47.6740, -122.1215 | `redmond` |
| Sammamish | 47.6163, -122.0356 | `sammamish` |

The current `WeeklyJobService.CITY_COORDS` only contains Seattle and Tacoma.
The implementation must be extended to include all 7 cities before integration
tests IT-06 can pass.

---

## 4. FCM Stub (WireMock)

**File**: `places-fcm-success.json`

WireMock stub for `POST https://fcm.googleapis.com/v1/projects/covey-dev/messages:send`:

```json
{
  "request": {
    "method": "POST",
    "urlPattern": ".*/messages:send"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "body": "{\"name\":\"projects/covey-dev/messages/stub-message-id\"}"
  }
}
```

---

## 5. Unit Test Mock Setup (Java Snippet Reference)

```java
// Standard mock setup for WeeklyJobService unit tests
GooglePlacesClient mockPlaces = mock(GooglePlacesClient.class);
Firestore mockFirestore = mock(Firestore.class);
CollectionReference mockCollection = mock(CollectionReference.class);
DocumentReference mockDoc = mock(DocumentReference.class);
ApiFuture<WriteResult> mockFuture = mock(ApiFuture.class);

when(mockFirestore.collection("weeklySpots")).thenReturn(mockCollection);
when(mockFirestore.collection("invites")).thenReturn(mockCollection);
when(mockCollection.document(anyString())).thenReturn(mockDoc);
when(mockDoc.set(any())).thenReturn(mockFuture);
when(mockFuture.get()).thenReturn(mock(WriteResult.class));

// Standard venue list for selection tests
List<WeeklySpot> twoVenues = List.of(
    new WeeklySpot("Seattle", "Best Bar", "1 Main St", "p1", 4.8, 300, System.currentTimeMillis()),
    new WeeklySpot("Seattle", "Second Bar", "2 Oak Ave", "p2", 4.5, 150, System.currentTimeMillis())
);
when(mockPlaces.searchVenues(eq("Seattle"), anyDouble(), anyDouble())).thenReturn(twoVenues);
```
