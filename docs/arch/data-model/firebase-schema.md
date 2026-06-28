# Firebase / Firestore Schema Design

**App:** Location-Notifier Social Gathering App
**Backend:** Java Lambda (Firebase Admin SDK)
**Datastore:** Cloud Firestore (Native mode)
**Auth:** Firebase Auth (Apple Sign In + Google Sign In federated providers)
**Last Updated:** 2026-06-27

---

## Design Principles

1. **Document IDs are deterministic where possible** — e.g. a user can only have one invite per week, so `invites/{userId}_{weekId}` enforces idempotency at the document level and avoids duplicate writes.
2. **Denormalize for read paths** — the iOS feed reads a single user's invite + the matching weekly spot. We store `venueId`, `weekId`, and `city` directly on the invite so the feed needs minimal joins.
3. **Status counts via aggregation queries** — RSVP counts ("12 going") are computed with Firestore `count()` aggregation queries filtered by `weekId + city + status`, avoiding counter-document write contention.
4. **Subcollections vs top-level** — push tokens and email addresses are modeled as subcollections under the user because they are only ever queried in the context of that user. Invites are top-level because they are queried both per-user (feed) and per-spot (counts).
5. **All timestamps are Firestore `Timestamp`**, written server-side via `FieldValue.serverTimestamp()` from the Lambda.

---

## Collections

### `/users/{userId}`

`userId` = Firebase Auth UID.

```
/users/{userId}
  - email: string            // lowercased canonical email
  - name: string
  - selectedCity: string      // City document id, e.g. "london"
  - createdAt: timestamp
  - updatedAt: timestamp
```

Subcollections:

#### `/users/{userId}/pushTokens/{tokenId}`

`tokenId` = SHA-256 hash of the device token (deterministic, lets re-registration upsert instead of duplicate).

```
  - deviceToken: string
  - platform: string          // "ios" | "android"
  - createdAt: timestamp
  - lastUsedAt: timestamp
  - isValid: boolean          // set false when FCM/APNs reports invalid
```

#### `/users/{userId}/emailAddresses/{emailId}`

```
  - email: string
  - isVerified: boolean
```

#### `/users/{userId}/friends/{friendId}`

`friendId` = the other user's UID. Models the `UserFriend` edge as a subcollection
for fast "who are my friends" reads. Friendship is stored bidirectionally
(write both `users/A/friends/B` and `users/B/friends/A`) so each side can query
its own list without a collection-group scan.

```
  - friendId: string          // duplicated in the doc for convenience
  - createdAt: timestamp
```

---

### `/cities/{cityId}`

`cityId` = url-safe slug, e.g. `london`, `nyc`.

```
/cities/{cityId}
  - name: string              // "London"
  - isActive: boolean         // hide inactive cities from the picker
  - timezone: string          // IANA tz, e.g. "Europe/London"
```

---

### `/weekly_spots/{spotId}`

`spotId` = `{cityId}_{weekId}`, e.g. `london_2026-W26`. Deterministic id guarantees
exactly one featured spot per city per week.

```
/weekly_spots/{spotId}
  - city: string              // City document id (reference by id)
  - weekId: string            // ISO week, e.g. "2026-W26"
  - venueId: string           // Google Places ID
  - venueName: string
  - address: string
  - photoUrl: string
  - rating: number            // 0.0 - 5.0
  - reviewCount: number
  - hours: map                // { mon: {open:"17:00",close:"23:00"}, ... }
  - createdAt: timestamp
```

---

### `/invites/{inviteId}`

`inviteId` = `{userId}_{weekId}`. One invite per user per week (idempotent generation).

```
/invites/{inviteId}
  - userId: string            // User UID (reference by id)
  - weekId: string            // ISO week
  - venueId: string           // Google Places ID of the spot for that week/city
  - city: string              // City document id
  - status: string            // "INVITED" | "YES" | "NO" | "INTERESTED"
  - timestamp: timestamp       // last status change
  - createdAt: timestamp
```

Status lifecycle: `INVITED` (initial) -> `YES` | `NO` | `INTERESTED` (user RSVP).

---

### `/venue_exclusions/{exclusionId}`

`exclusionId` = `{cityId}_{weekId}`. Drives the spot-selection job so a venue is not
re-featured in the same city in the recent past.

```
/venue_exclusions/{exclusionId}
  - city: string              // City document id
  - weekId: string
  - venueIds: array<string>   // Google Places IDs to skip
  - updatedAt: timestamp
```

---

## Query Support Matrix

| Use case | Query | Index |
|----------|-------|-------|
| Get current user profile | `doc("users/{uid}")` | default |
| Get current week spot for user's city | `doc("weekly_spots/{cityId}_{weekId}")` | default |
| Get 4-week spot history for a city | `weekly_spots where city == X order by weekId desc limit 5` | composite: `city ASC, weekId DESC` |
| Get a user's invite for a week | `doc("invites/{uid}_{weekId}")` | default |
| **Count YES for a spot** | `invites where city==X and weekId==Y and status=="YES" -> count()` | composite: `city ASC, weekId ASC, status ASC` |
| Count by each status for a spot | same as above, one count() per status | same composite |
| Get user's invite history (feed) | `invites where userId==uid order by weekId desc limit 5` | composite: `userId ASC, weekId DESC` |
| List active cities | `cities where isActive == true` | single-field `isActive` |
| Get user's friends | `collection("users/{uid}/friends")` | default |
| Find friends going to spot | `invites where city==X and weekId==Y and status=="YES" and userId in [friendIds]` | composite: `city, weekId, status, userId` |
| Look up valid push tokens for user | `users/{uid}/pushTokens where isValid == true` | single-field `isValid` |

---

## Recommended Composite Indexes

Define these in `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "weekly_spots",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "city", "order": "ASCENDING" },
        { "fieldPath": "weekId", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "invites",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "city", "order": "ASCENDING" },
        { "fieldPath": "weekId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "invites",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "weekId", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "invites",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "city", "order": "ASCENDING" },
        { "fieldPath": "weekId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "userId", "order": "ASCENDING" }
      ]
    }
  ]
}
```

**Notes on indexing:**

- The **status-count** queries are the hottest path (feed shows "X going / Y interested"). The `city + weekId + status` composite serves all four status counts and the friends-going query (which extends it with `userId`).
- Use Firestore **aggregation `count()`** queries rather than maintaining counter documents. This avoids write hotspots when many users RSVP to the same spot in the same hour and keeps counts strongly consistent at read time.
- Single-field indexes on `isActive` and `isValid` are auto-created by Firestore; listed here only for documentation.
- The deterministic doc IDs (`{cityId}_{weekId}`, `{userId}_{weekId}`) mean the most common reads (current spot, current invite) are direct `getDocument` lookups requiring **no index** and costing a single read.

---

## Security Rules (summary)

Client (iOS) reads/writes go through the Lambda using the Admin SDK, but direct
Firestore access from the app should be locked down:

- `users/{uid}` — read/write only when `request.auth.uid == uid`.
- `cities`, `weekly_spots` — read for any authenticated user; writes Admin-only.
- `invites/{inviteId}` — read where the invite's `userId == request.auth.uid`;
  status writes go through the Lambda (Admin) to enforce valid transitions.
- `venue_exclusions` — Admin-only (no client access).
- Subcollections under `users/{uid}` inherit the owner check.

---

## Data Volume & Lifecycle Notes

- `weekly_spots` grows at `cities x 52` per year — trivial. Keep indefinitely.
- `invites` grows at `active_users x 52` per year. The feed only needs 5 weeks, so older invites may be archived to cold storage or deleted after a retention window (e.g. 12 months).
- `venue_exclusions` can be pruned to a rolling window (e.g. last 8 weeks) since exclusion only matters for recent history.
- `pushTokens` flagged `isValid: false` should be garbage-collected periodically.
