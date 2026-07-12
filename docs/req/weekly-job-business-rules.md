# Weekly Job Lambda — Business Rules

**WBS Item:** 1.3.6 Weekly Job Lambda
**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12

---

## Timing Rules

- [ ] **BR-T01:** Job triggered by EventBridge cron rule once per week on Thursday night
- [ ] **BR-T02:** Notifications delivered Friday 9am (UTC-4:30 compromise for EST/EDT)
- [ ] **BR-T03:** `weekId` is ISO week string of Friday (e.g. `2026-W30`), consistent across WeeklySpot, Invite, VenueExclusion

---

## City Selection Rules

- [ ] **BR-C01:** Each active city gets exactly one WeeklySpot per week (if venues available)
- [ ] **BR-C02:** Inactive cities (`isActive == false`) are excluded from processing
- [ ] **BR-C03:** City with no eligible venues is skipped without error
- [ ] **BR-C04:** Cities processed independently; one city failure doesn't stop others

---

## Venue Selection Rules

- [ ] **BR-V01:** Venues used in last 12 weeks are excluded (rotation)
- [ ] **BR-V02:** Venue rating must be ≥4.0
- [ ] **BR-V03:** Venue must have ≥50 user reviews
- [ ] **BR-V04:** Venue must be open on ≥3 weekdays
- [ ] **BR-V05:** If no eligible venue in 2km radius, expand to 3km and retry once
- [ ] **BR-V06:** Missing required fields (place_id, name, address, coords, rating, hours) = ineligible
- [ ] **BR-V07:** Selected venue is highest-rated eligible candidate

---

## Invite Rules

- [ ] **BR-I01:** Every user in active city receives one Invite
- [ ] **BR-I02:** Initial Invite status is `INVITED`
- [ ] **BR-I03:** Deterministic ID `{userId}_{weekId}` + `create` semantics = first-run-wins
- [ ] **BR-I04:** Invite batch write failure affects only that batch
- [ ] **BR-I05:** No Invites created if WeeklySpot write failed

---

## Notification Rules

- [ ] **BR-N01:** Push sent only to tokens where `isValid == true`
- [ ] **BR-N02:** Invalid tokens marked `isValid = false`, not retried
- [ ] **BR-N03:** Email sent only to addresses where `isVerified == true`
- [ ] **BR-N04:** One user's notification failure doesn't block others
- [ ] **BR-N05:** No notifications sent if no WeeklySpot created
- [ ] **BR-N06:** Notifications idempotent within same week (no duplicates on re-run)

---

## Exclusion List Rules

- [ ] **BR-E01:** After WeeklySpot write, selected venue added to VenueExclusion
- [ ] **BR-E02:** Deterministic ID `{cityId}_{weekId}` (re-run overwrites, idempotent)
- [ ] **BR-E03:** Exclusion check reads last 12 weeks only

---

## Job Integrity Rules

- [ ] **BR-J01:** Per-city errors never cause non-zero Lambda exit
- [ ] **BR-J02:** Job log written for every city processed (success or failure)
- [ ] **BR-J03:** CloudWatch metrics emitted: CitiesProcessed, CitiesSkipped, PlacesAPIErrors, InvitesCreated, PushTokensContacted, EmailsSent
- [ ] **BR-J04:** Lambda completes within 15-minute timeout (MVP scale: <10 cities)

---

## Data Consistency Rules

- [ ] **BR-D01:** `weekId` identical on WeeklySpot, all Invites, and VenueExclusion
- [ ] **BR-D02:** `venueId` identical on WeeklySpot and all Invites
- [ ] **BR-D03:** `city` field identical across all three collections and matches City.id

---

**Status:** Planning
**Author:** Domain Analyst
**Date:** 2026-07-12
