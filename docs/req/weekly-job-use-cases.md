# Weekly Job Lambda — Use Cases

**WBS Item:** 1.3.6 Weekly Job Lambda
**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12

---

## UC-WJ-01: Happy Path — Spot Found, Invites Created, Notifications Sent

**Goal:** For each active city, select a venue, create WeeklySpot and Invites, queue notifications.

**Main Success Scenario:**

1. Lambda starts; logs job start with weekId
2. Read all active cities from Firestore
3. For each city:
   - Read last 12 weeks of VenueExclusions
   - Query Google Places Nearby Search
   - Filter candidates (rating ≥4.0, reviews ≥50, open weekdays, not excluded)
   - Select top-ranked candidate
   - Write WeeklySpot to Firestore
   - Write VenueExclusion record
   - Query all users in city
   - Write Invite documents (batch ≤500)
   - Send FCM push notifications to valid tokens
   - Send email digests via Resend
   - Write job log
4. Lambda exits with status 0

**Acceptance Criteria:**
- [ ] WeeklySpot document written with all fields
- [ ] VenueExclusion document written
- [ ] One Invite per user per city
- [ ] FCM notifications sent to all valid tokens
- [ ] Email digests sent via Resend
- [ ] Job log documents created
- [ ] Lambda exits 0

---

## UC-WJ-02: Venue Exhaustion — All Candidates Used, City Skipped

**Goal:** Skip city gracefully if no eligible venues remain after 12-week window.

**Main Scenario:**

1. Initial 2km search finds candidates, but all are in 12-week exclusion set or fail rating filters
2. Expand search radius to 3km and retry
3. Still no eligible candidates
4. Log warning: `"No eligible venue found for {cityId} in week {weekId}"`
5. Do NOT write WeeklySpot, Invites, or send notifications
6. Write job log with `venueSelected: null`
7. Continue to next city without error

**Acceptance Criteria:**
- [ ] No WeeklySpot written for skipped city
- [ ] No Invites created
- [ ] No notifications sent
- [ ] Job log records skip reason
- [ ] Lambda continues processing other cities
- [ ] Lambda exits 0

---

## UC-WJ-03: Google Places API Error — Log and Continue

**Goal:** Handle API errors without failing entire job.

**Main Scenario:**

1. Places API call returns error (quota exceeded, timeout, 500, etc.)
2. Lambda catches exception
3. Log error at ERROR level with city, weekId, error details
4. Emit CloudWatch metric `PlacesAPIErrors`
5. Skip this city (no WeeklySpot, Invites, notifications)
6. Write job log with error details
7. Continue to next city

**Acceptance Criteria:**
- [ ] Lambda does not throw non-zero exit
- [ ] Error logged at ERROR level
- [ ] CloudWatch metric emitted
- [ ] No WeeklySpot/Invites written
- [ ] Job log records error
- [ ] Remaining cities processed
- [ ] Lambda exits 0

---

## UC-WJ-04: Firestore Write Failure — Log and Continue

**Goal:** Handle Firestore write failures without failing entire job.

**Main Scenario (WeeklySpot write fails):**

1. WeeklySpot write to Firestore fails (UNAVAILABLE, DEADLINE_EXCEEDED, etc.)
2. Log error at ERROR level
3. Do NOT create Invites for this city
4. Do NOT send notifications
5. Write job log recording intended venue and failure reason
6. Continue to next city

**Extension (Invite batch write fails):**

1. WeeklySpot written successfully
2. One or more Invite write batches fail
3. Log error with affected user ID range
4. Continue with remaining batches
5. Send notifications only for successfully created Invites
6. Record partial failure in job log

**Acceptance Criteria:**
- [ ] Lambda does not throw non-zero exit
- [ ] Firestore errors logged at ERROR level
- [ ] Partial invites still get notifications
- [ ] Remaining cities continue processing
- [ ] Lambda exits 0

---

**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12
