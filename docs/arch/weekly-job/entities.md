# Weekly Job Lambda — Entity Reference

**WBS Item:** 1.3.6 Weekly Job Lambda
**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12

---

## Entities Used by Weekly Job

### City

**Role:** Defines active cities to process. Each city gets one WeeklySpot per week.

**Key fields:**
- `id` — city slug (e.g. `seattle`)
- `isActive` — filter: only `true` cities processed
- `name` — display name
- `timezone` — IANA tz (e.g. `America/Los_Angeles`)
- `lat`, `lng` — city centre coordinates for Google Places ✅ **NEW** (required before Phase 2.1)

**Firestore path:** `/cities/{cityId}`

---

### VenueExclusion

**Role:** Tracks venues used in last 12 weeks to prevent repeats.

**Query pattern:** Read last 12 weeks for a city, union all `venueIds`

**Firestore path:** `/venueExclusions/{cityId}_{weekId}`

---

### WeeklySpot

**Role:** The weekly venue selected for a city. iOS feed reads this.

**Firestore path:** `/weeklySpots/{cityId}_{weekId}` (deterministic ID, `create` semantics)

**Idempotency:** First-run-wins (skip if exists)

---

### User

**Role:** Read to find all users in each city.

**Query:** `/users where selectedCity == {cityId}`

---

### Invite

**Role:** Links user to WeeklySpot. Tracks RSVP status.

**Firestore path:** `/invites/{userId}_{weekId}` (deterministic ID, `create` semantics)

**Initial status:** Always `INVITED` when created by weekly job

---

### PushToken

**Role:** Device tokens for FCM notifications.

**Query:** `/users/{userId}/pushTokens where isValid == true`

**Updated on:** Token rejection (set `isValid = false`), successful send (set `lastUsedAt`)

---

### EmailAddress

**Role:** Email addresses for digest notifications.

**Query:** `/users/{userId}/emailAddresses where isVerified == true`

---

## Data Access Pattern Summary

| Step | Operation | Path | Filter | Notes |
|------|-----------|------|--------|-------|
| 1 | Read | `/cities` | `isActive == true` | Single-field index (auto) |
| 2A | Read | `/venueExclusions` | `city ==`, order by `weekId DESC` | **NEW** composite index needed |
| 2F | Read | `/users` | `selectedCity ==` | Single-field index (auto) |
| 2G | Write (batch) | `/invites/{userId}_{weekId}` | — | `create` semantics (first-run-wins) |
| 2H | Read | `/users/{userId}/pushTokens` | `isValid == true` | Single-field (auto) |
| 2H | Read | `/users/{userId}/emailAddresses` | `isVerified == true` | Single-field (auto) |

**New Firestore index required:**
```json
{
  "collectionGroup": "venueExclusions",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "city", "order": "ASCENDING" },
    { "fieldPath": "weekId", "order": "DESCENDING" }
  ]
}
```

Add to `firestore.indexes.json` before deploying.

---

**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12
