# Weekly Job Lambda — Atomicity, Concurrency & Failure Scenarios

**WBS:** 1.3.6 (Weekly Job Lambda)
**Status:** Draft for review
**Author:** Solution Architect
**Date:** 2026-07-12

Companion to `implementation-sequence.md` and `data-flow.mmd`. This document
defines the processing model, transaction boundaries, idempotency strategy, and
partial-failure behavior for the weekly job.

---

## 1. Processing Model: Per-City, Sequential

**Decision:** Process cities **one at a time (sequential), each city as its own
unit of work.** Not all-at-once, and not a single global transaction.

**Rationale:**

- The requirement **"if a city has no available spots, skip it (don't fail the
  job)"** demands that a city be an independent unit. A global transaction would
  force all-or-nothing across cities, which directly violates this.
- Failure isolation: a Google Places outage or empty result for Seattle must not
  prevent Tacoma from getting a spot.
- Firestore transactions are bounded (≤500 operations, must complete quickly). A
  single global transaction spanning all cities × all users × all notifications
  would exceed limits and increase contention/timeout risk.
- Sequential (vs parallel) keeps the initial implementation simple, keeps Places
  API usage well under quota, and makes logs deterministic. City count is small
  (2 today). If city count grows large enough to matter, cities can later be
  fanned out to parallel Lambda invocations — the per-city unit boundary already
  makes that safe.

**Unit of work = one city.** Within a city, the writes (WeeklySpot + exclusion +
Invites + queued notifications) are grouped into one atomic boundary.

---

## 2. Transaction Boundaries

### 2.1 Selection run — per-city atomic write

For each successfully-selected city, perform the following as a single atomic
operation (Firestore transaction, or a `WriteBatch` when no read-modify-write is
needed within the group):

1. Create `WeeklySpot` — id `{city}_{weekStartDate}`
2. Append selected venue to `venue_exclusion_list` — `expiresAt = weekStartDate + 12 weeks`
3. Create one `Invite` per user in the city — id `{weeklySpotId}_{userId}`
4. Create one `ScheduledNotification` per user per channel — id `{weeklySpotId}_{userId}_{channel}`, `status = PENDING`, `deliverAt = Friday 09:00 EST`

**Why these four together:** they must be mutually consistent. A `WeeklySpot`
without its exclusion entry would let the same venue be re-picked next week. Invites
without queued notifications would leave users invited but never notified. Grouping
them guarantees "spot exists ⟺ it's excluded ⟺ users are invited ⟺ users are queued."

**500-op limit handling:** if a city has more users than fit in one batch
(spot + exclusion + N invites + N tasks > 500 ops), chunk **the invite/notification
writes** into sequential ≤500-op batches. The `WeeklySpot` + exclusion write is the
first, committed batch; invite/notification chunks follow. Deterministic ids make
each chunk independently idempotent, so a mid-chunk crash is fully recoverable on
re-run (see §3). Document this as an accepted relaxation of strict per-city
atomicity for large cities.

### 2.2 Delivery run — per-task, single-delivery

Notification delivery is **not** transactional with sending (you cannot roll back
a sent push/email). Instead each `scheduled_notifications` task is processed with a
**claim → send → mark** pattern to guarantee at-most-once *effective* delivery:

1. Conditionally transition task `PENDING → SENDING` (compare-and-set on `status`).
   If the CAS fails, another worker/run already owns it — skip.
2. Send via FCM or SES.
3. On success, set `status = SENT`. On hard failure, set `status = FAILED` (DLQ).

This makes the delivery run safely re-runnable: already-`SENT`/`SENDING` tasks are
not resent.

---

## 3. Idempotency — "What if the job runs twice?"

Running either handler twice (retry, manual re-invoke, missed-then-fired cron) must
be safe. Three mechanisms combine:

1. **Deterministic document ids** (primary defense):
   - `WeeklySpot`: `{city}_{weekStartDate}`
   - `Invite`: `{weeklySpotId}_{userId}`
   - `ScheduledNotification`: `{weeklySpotId}_{userId}_{channel}`
   - Exclusion entry keyed by `{venueId}_{weekStartDate}`

   A second run writes the **same** ids. Use `set()` (overwrite) semantics so
   re-writing an identical doc is a harmless no-op rather than creating duplicates.
   > **Migration note:** current `WeeklyJobService` uses `UUID.randomUUID()` for
   > `WeeklySpot` and `Invite` ids, which is **not** idempotent. Switching to
   > deterministic ids is a required change for this WBS.

2. **Run-level guard (`weekly_job_runs`):** the selection handler writes a run doc
   keyed by `weekStartDate` with `status ∈ {RUNNING, COMPLETED, PARTIAL}`. On start,
   if a `COMPLETED` run exists for this week, short-circuit and return 200. This
   avoids redundant Places API spend on a clean re-invoke. A `PARTIAL`/`RUNNING`
   run is allowed to proceed and will only fill in the missing/failed cities
   (per-city "already exists?" check + deterministic ids).

3. **Per-city existence check:** before querying Places for a city, check whether a
   `WeeklySpot` already exists for `{city}_{weekStartDate}`. If so, skip selection
   for that city (saves an API call). This makes re-runs cheap, not just correct.

4. **Delivery single-delivery:** the CAS `PENDING → SENDING` transition (§2.2)
   prevents re-sending an already-processed notification if the delivery handler
   runs twice.

**Net effect:** the pair of handlers is idempotent per `weekStartDate`. Re-running
never produces duplicate spots, invites, notifications, or exclusion entries, and
never double-notifies a user.

---

## 4. Partial Failure Handling

The job is designed to **degrade, not fail.** Failure is scoped as tightly as
possible.

| Scenario | Scope | Behavior |
|----------|-------|----------|
| City has **no eligible venue** (empty results / all excluded) | City | Skip city, add to `skippedCities`, log reason. Job continues. **Not** an error. |
| Google Places **transient error** for a city (after retries exhausted) | City | Record in `cityErrors`, skip city, continue. Run ends `PARTIAL`. |
| Firestore **write contention/timeout** in a city's transaction | City | Retry the city transaction (exp backoff, max 3). If still failing, record `cityError`, continue. Deterministic ids ⇒ safe to retry. |
| **`cities` collection unreadable** at start | Run | Fail the run early (nothing to select). Return 500; safe to re-invoke. |
| One user's push token **invalid** during delivery | User/token | Mark token inactive, continue. Email channel still attempted independently. |
| **SES throttling / 5xx** for a task | Task | Retry (exp backoff, max 3). On exhaustion → `status = FAILED` (DLQ) for replay. Other tasks unaffected. |
| **FCM transient error** for a task | Task | Retry (max 3). On exhaustion → `FAILED`. Independent of other tasks and of the email channel. |
| Selection Lambda **crashes mid-run** | Run | On re-invoke, `weekly_job_runs` is `RUNNING`/absent → resume. Deterministic ids + per-city existence checks skip completed cities and finish the rest. |
| Delivery Lambda **crashes mid-drain** | Run | On re-invoke, remaining `PENDING`/`SENDING` tasks are re-processed. CAS prevents double-send of any already-`SENT` task. |

### Run status semantics

- **COMPLETED** — every target city produced a spot (or was legitimately skipped
  for having no venue). No errors.
- **PARTIAL** — at least one city hit a hard error (`cityErrors` non-empty).
  Emits a CloudWatch metric + alarm. The handler still returns **200** so the
  scheduler/monitor does not treat the whole job as down; partial success is
  surfaced via metrics, not via a hard job failure.
- **FAILED** — only for run-scoped preconditions (e.g., cannot load cities,
  cannot initialize Firebase). Returns 500.

**Key principle:** per-requirement, a single city failing (including the empty-city
case) must never fail the whole job. Only run-scoped, pre-loop failures are hard
failures.

---

## 5. Retry Strategy Summary

| Layer | Retry policy | On exhaustion |
|-------|--------------|---------------|
| Google Places call | Exp backoff, max 3, jitter; retry on 5xx / `OVER_QUERY_LIMIT` / timeout | Record `cityError`, skip city |
| Per-city Firestore transaction | Exp backoff, max 3; retry on `ABORTED`/`UNAVAILABLE` (contention) | Record `cityError`, skip city |
| FCM send (per task) | Exp backoff, max 3; retry on transient/UNAVAILABLE | Task → `FAILED` (DLQ) |
| SES send (per task) | Exp backoff, max 3; retry on `Throttling`/5xx | Task → `FAILED` (DLQ) |
| Whole-Lambda (invocation) | EventBridge/Lambda async retry or manual re-invoke | Idempotency guarantees safe re-run |

Retries are **bounded** (no infinite loops) and **scoped to the smallest unit** so a
retry storm in one city/task cannot stall the rest of the job.

---

## 6. Open Questions for Review

1. **Collection naming:** align on `weekly_spots`/`venue_exclusion_list` (requirements)
   vs current `weeklySpots`/`pushTokens` (code). Needs a decision + migration note.
2. **Timezone/DST:** confirm whether "9am EST" means fixed UTC-5 year-round or
   local Eastern (EST/EDT). Affects EventBridge schedule expressions.
3. **Cities source:** migrate hardcoded `CITY_COORDS` to the `cities` collection so
   cities can be added without a redeploy.
4. **Exclusion granularity:** exclude by `venueId` (Google `place_id`) — confirm
   that is the stable rotation key across the 12-week window.
5. **Parallelism:** stay sequential for now; revisit per-city parallel fan-out only
   if city count grows enough to matter for wall-clock time.
