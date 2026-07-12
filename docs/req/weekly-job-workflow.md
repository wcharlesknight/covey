# Weekly Job Lambda — Step-by-Step Workflow

**WBS Item:** 1.3.6 Weekly Job Lambda
**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12
**Related UC:** UC-008 (Run Weekly Scheduled Job)

---

## Open Questions — Resolved Before Implementation

✅ **Timing:** Confirmed Thursday night job → Friday 9am notifications (UTC-4:30)
✅ **Exclusion window:** Confirmed 12 weeks (not 4 weeks)
✅ **Coordinates:** Will store lat/lng in Firestore City collection
✅ **Email provider:** Using Resend (not AWS SES)
✅ **Re-run behavior:** First-run-wins (skip if WeeklySpot exists)

---

## Trigger

**Mechanism:** AWS EventBridge Scheduler rule targeting the WeeklyJobLambda function.

**Confirmed schedule (per business requirements):**
- Job runs: Thursday night
- Notifications dispatched: Friday 9am (UTC-4:30 compromise for EST/EDT)

**EventBridge cron expression:**
```
cron(30 13 ? * 6 *)
```
(UTC 13:30 ≈ EST 8:30am to EDT 9:30am)

---

## Step-by-Step Workflow

### Step 0 — Pre-flight

Before processing any city:

- Read Lambda environment variables: Resend API key, Firestore project ID
- Verify Firebase Admin SDK is initialised
- Log job start: timestamp, weekId (ISO week of the upcoming Friday)

**weekId convention:** Use the ISO week string of the Friday — e.g. if the Thursday run is 2026-07-16, the weekId is `2026-W30`. Must be consistent across WeeklySpot, Invite, and VenueExclusion documents.

---

### Step 1 — Load Active Cities

**Read:** Firestore `/cities` collection, filter `isActive == true`.

**Output:** Ordered list of City documents with:
- `id` — city slug (e.g. `seattle`)
- `name` — display name
- `timezone` — IANA tz string
- `lat`, `lng` — city centre coordinates for Google Places

---

### Step 2 — For Each Active City (loop)

Cities are processed sequentially. Failures in one city must not stop processing of remaining cities.

#### Step 2A — Build the Exclusion List

**Read:** Last 12 weeks of VenueExclusion documents for this city.

Query: `venueExclusions where city == {cityId} order by weekId DESC limit 12`

**Output:** Set of excluded Google Places IDs

---

#### Step 2B — Query Google Places API

**Call:** Google Places Nearby Search API.

**Parameters:**
- `location` — city centre coordinates from City entity
- `radius` — default 2km
- `type` — `bar|cafe|restaurant`
- `rankby` — `prominence`
- `key` — Google Places API key from Secrets Manager

**Output:** Up to 20 candidate venues with `place_id`, `name`, `rating`, `user_ratings_total`, etc.

**Error handling:** Log error and skip this city (don't fail entire job).

---

#### Step 2C — Filter and Rank Candidates

Apply filters (in order):
1. Exclusion: `place_id` NOT in `excludedVenueIds`
2. Rating: `rating >= 4.0`
3. Review count: `user_ratings_total >= 50`
4. Open weekdays: Venue open at least 3 weekdays

Rank remaining candidates by `rating DESC`, then `user_ratings_total DESC`.

**If no candidates pass:**
- Expand search radius to 3km and retry once
- If still no candidates, skip city (log warning)

---

#### Step 2D — Select and Fetch Details

Take top-ranked candidate and fetch full details (address, opening hours, photos).

---

#### Step 2E — Write the WeeklySpot Document

**Write:** Firestore `/weeklySpots/{cityId}_{weekId}` (deterministic ID, `create` semantics).

Fields:
```
city, weekId, venueId, venueName, address, photoUrl, rating, reviewCount, hours, createdAt
```

Also write VenueExclusion: `/venueExclusions/{cityId}_{weekId}`

---

#### Step 2F — Load Eligible Users for This City

**Query:** `users where selectedCity == {cityId}`

---

#### Step 2G — Create Invite Documents (batch)

**Write:** `/invites/{userId}_{weekId}` for each user (deterministic ID, `create` semantics).

Fields: `userId, weekId, venueId, city, status: INVITED, createdAt`

Batch writes in groups of ≤500.

---

#### Step 2H — Queue Notifications for Friday 9am

**FCM push notifications:**
- Read valid push tokens for each user: `/users/{userId}/pushTokens where isValid == true`
- Send FCM notification with venue name and deep link
- On token rejection: set `isValid = false`

**Email notifications (via Resend):**
- Read verified emails: `/users/{userId}/emailAddresses where isVerified == true`
- Send templated email digest with venue details and RSVP link
- Log email send count

---

#### Step 2I — Log City Result

Write job log document recording venue, invite count, push count, email count, any errors.

---

### Step 3 — Job Completion

Log overall summary: total cities processed, skipped, errors, totals for invites/pushes/emails.

Return Lambda exit code 0 (per-city failures are logged, not fatal).

---

## Error Hierarchy

| Scenario | Scope | Action |
|----------|-------|--------|
| Cities query fails | Entire job | Abort, log, non-zero exit |
| Places API error | One city | Log, skip city, continue |
| WeeklySpot write fails | One city | Log, skip notifications, continue |
| Invite batch write fails | Subset of users | Log affected users, continue |
| FCM push fails | One device | Mark invalid, continue |
| Email send fails | One user | Log, continue |

---

## Idempotency Summary

- **WeeklySpot:** Deterministic ID + `create` semantics = first-run-wins
- **Invite:** Deterministic ID + `create` semantics = no RSVP overwrite
- **VenueExclusion:** Deterministic ID = consistent re-runs
- **Push:** Fresh token read each run; stale tokens cleaned up
- **Email:** Consider storing `notificationSentAt` to skip already-sent emails on re-run

---

**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12
