# Weekly Job Lambda — Integration Points & Implementation Sequence

**WBS:** 1.3.6 (Weekly Job Lambda)
**Status:** Draft for review
**Author:** Solution Architect
**Date:** 2026-07-12

This document covers the external/internal integration points and the recommended
build order (with dependency rationale). Atomicity and failure semantics are
covered in the companion doc `concurrency-atomicity.md`.

---

## 1. Integration Points

The weekly job is split into two invocations that share Firestore state:

- **Selection run** — Thursday night. Selects venues, creates `WeeklySpot` +
  `Invite` docs, and enqueues notification tasks.
- **Delivery run** — Friday 9am EST. Drains the notification queue and sends
  FCM + email.

This split decouples the expensive/failure-prone selection work from the
time-sensitive delivery, so a delivery hiccup never blocks selection and vice
versa.

| # | Integration | Direction | Operations | Rate Limits / Quotas | Latency Expectation | Failure Mode & Handling |
|---|-------------|-----------|------------|----------------------|---------------------|-------------------------|
| 1 | **Google Places API — Nearby Search** | Outbound (Selection) | 1 `nearbysearch` call per city (optionally +1 `Place Details` per finalist to confirm hours). Fields: `name`, `vicinity`, `place_id`, `rating`, `user_ratings_total`, `business_status`. | Default project quota; billed per request. `OVER_QUERY_LIMIT` / `RESOURCE_EXHAUSTED` on burst. With ~2–20 cities this is well within quota. | 200–800 ms P50, up to ~2 s P95 per call. | Retry with exponential backoff (max 3) on 5xx / `OVER_QUERY_LIMIT` / timeout. On `ZERO_RESULTS` → treat as empty city (skip, not error). On hard failure after retries → record `cityError`, continue to next city. |
| 2 | **Firestore — read `cities`** | Inbound (Selection) | 1 collection read to resolve target cities + coordinates. (Currently hardcoded in `WeeklyJobService.CITY_COORDS`; should migrate to the `cities` collection.) | Firestore read quotas (very high). | <100 ms. | Retry on transient `UNAVAILABLE`. Fail the run early if cities cannot be loaded (nothing to do). |
| 3 | **Firestore — read `venue_exclusion_list`** | Inbound (Selection) | Per city: query exclusions where `city ==` and `expiresAt > now` (12-week window). Used to filter candidates. | Firestore read quotas. | <150 ms per city. | Retry on transient error. If unreadable → fail the city (cannot guarantee rotation), record `cityError`. |
| 4 | **Firestore — write `weekly_spots`** | Outbound (Selection) | 1 write per successful city. Deterministic doc id `{city}_{weekStartDate}`. | Firestore write quotas; 500 writes/sec/collection soft guidance. Volume is trivial. | <200 ms. | Part of the per-city transaction (see atomicity doc). Deterministic id makes re-runs idempotent. |
| 5 | **Firestore — write `venue_exclusion_list`** | Outbound (Selection) | 1 write per successful city — append selected venue with `expiresAt = weekStartDate + 12 weeks`. | Firestore write quotas. | <200 ms. | Same transaction as the `WeeklySpot` write, so the spot and its exclusion are always consistent. |
| 6 | **Firestore — read `users`** | Inbound (Selection) | Per city: query `users where city ==`. Provides invite recipients. | Firestore read quotas. Large cities may paginate. | <300 ms per city (may grow with user base — paginate at scale). | Retry on transient error. If the user query fails mid-city, the whole city transaction rolls back and is retried. |
| 7 | **Firestore — write `invites`** | Outbound (Selection) | 1 write per user per city. Deterministic doc id `{weeklySpotId}_{userId}`. | Firestore write quotas; batch/transaction limited to 500 ops. Chunk large cities into ≤500-op batches. | <200 ms per batch. | Written in the per-city transaction/batch. Deterministic id ⇒ safe to re-run. |
| 8 | **Firestore — write `scheduled_notifications`** | Outbound (Selection) | 1 (or 2 for FCM+Email) task doc per user, `deliverAt = Friday 09:00 EST`, `status = PENDING`. This is the queue between selection and delivery. | Firestore write quotas; ≤500 per batch. | <200 ms per batch. | Written in the same per-city transaction as invites. Deterministic id `{weeklySpotId}_{userId}_{channel}` prevents duplicate tasks on re-run. |
| 9 | **Firestore — read `scheduled_notifications`** | Inbound (Delivery) | Friday run queries `deliverAt <= now AND status == PENDING`, batched. | Firestore read quotas. | <300 ms per page. | Retry on transient error; delivery run is safely re-runnable (drains remaining PENDING). |
| 10 | **Firestore — read `pushTokens`** | Inbound (Delivery) | Per user: active tokens via existing `PushTokenService.getActiveTokensByUser`. | Firestore read quotas. | <150 ms per user (cache per run). | Retry on transient error; skip user's FCM channel if tokens unreadable (email still attempted). |
| 11 | **Firebase Cloud Messaging (FCM)** | Outbound (Delivery) | `FirebaseMessaging.sendEachForMulticast` (≤500 tokens/call). One multicast per user (usually 1–3 tokens). | FCM has generous throughput; per-message send. Batch to ≤500 tokens. | 100–500 ms per multicast. | Per-token result inspected. `UNREGISTERED` / `INVALID_ARGUMENT` → mark token inactive via `PushTokenService.markTokenInactive`. Transient → retry (max 3). Task marked `SENT` only after success. |
| 12 | **AWS SES — SendEmail / SendTemplatedEmail** | Outbound (Delivery) | 1 templated email per user. | SES sending rate (msgs/sec) + daily quota; sandbox requires verified recipients until production access granted. | 100–400 ms per send. | Throttling (`Throttling`) / 5xx → exponential backoff retry (max 3). Hard failure → move task to DLQ (`status = FAILED`) for manual/replay handling. Bounce/complaint handling via SES notifications (out of scope for this WBS). |
| 13 | **EventBridge — cron triggers** | Inbound (both runs) | Two rules: selection (Thu ~21:00 EST) and delivery (Fri 09:00 EST). Note DST: schedule in a fixed offset and confirm EST vs EDT, or use a timezone-aware schedule. | N/A | N/A | If a scheduled invoke is missed, idempotent doc ids + `weekly_job_runs` guard make a manual re-invoke safe. |
| 14 | **CloudWatch — metrics/alarms/logs** | Outbound (both runs) | Emit structured logs + custom metrics (cities processed, skipped, errored, notifications sent/failed). Alarm on partial failure and delivery DLQ depth. | N/A | N/A | Observability only; never blocks job. |

### Recommendation on Email transport

Use **AWS SES** rather than a Firebase email extension: the backend already runs
in AWS (Lambda + IAM), so SES avoids adding a Firestore-triggered extension and
keeps retry/DLQ logic in the same Java delivery handler alongside FCM. Record an
ADR (`docs/arch/adr/`) for this choice. Request SES production access early —
sandbox mode only sends to verified addresses and will block real delivery.

---

## 2. Implementation Build Order

Build bottom-up along the dependency chain: each component is a hard prerequisite
for the next, so this order maximizes the amount of the pipeline that is testable
at every step.

1. **Data model + collection contracts (foundation).**
   Finalize `WeeklySpot`, `Invite`, and new models: `ScheduledNotification`,
   `VenueExclusion`, `WeeklyJobRun`. Lock the deterministic doc-id conventions and
   reconcile the collection-name mismatch (code uses `weeklySpots`/`pushTokens`;
   requirements say `weekly_spots`/`venue_exclusion_list`). **Why first:** every
   other component reads/writes these shapes; changing them later is expensive and
   ripples through all queries and tests.

2. **Venue selection pipeline (Places → exclusion filter → validate → pick).**
   Extend `GooglePlacesClient` (retry/backoff, business_status) and the selection
   half of `WeeklyJobService`: candidate build, `venue_exclusion_list` filtering,
   validation, ranking. **Why second:** venue selection is the blocking upstream
   step — no `WeeklySpot` means no invites and no notifications. This is also the
   riskiest external integration, so proving it early de-risks the rest. Must
   implement the "empty city ⇒ skip, don't fail" rule here.

3. **Per-city atomic write (WeeklySpot + exclusion + Invites + queued tasks).**
   Wrap the writes in a per-city Firestore transaction/batch with deterministic
   ids (see `concurrency-atomicity.md`). **Why third:** depends on selection output
   (2) and the models (1). Establishing the transaction boundary here is what makes
   partial failure and re-runs safe before any delivery code exists.

4. **Run orchestration + idempotency guard (`weekly_job_runs`).**
   Add the run-level record, per-city error/skip accumulation, and "already
   completed" short-circuit in `WeeklyJobHandler`/`WeeklyJobService`. **Why fourth:**
   it coordinates the per-city units from (3); it needs those units to exist before
   it can orchestrate and record their outcomes.

5. **Notification delivery handler (queue drain → FCM + SES).**
   New `NotificationDispatchHandler` + `NotificationService`: read
   `scheduled_notifications`, send FCM (reusing `PushTokenService`), send SES email,
   mark `SENT`/`FAILED`, deactivate dead tokens, DLQ on hard failure. **Why fifth:**
   it consumes the `scheduled_notifications` queue produced by (3). Building it after
   selection means there is real queued data to test against, and a delivery bug
   cannot corrupt selection.

6. **Scheduling, observability, and alerting (EventBridge + CloudWatch).**
   Wire the two cron rules, structured logging, custom metrics, and partial-failure/
   DLQ alarms. **Why last:** scheduling is only meaningful once both selection and
   delivery run correctly on demand; observability instruments finished behavior.
   Until this step, both handlers are exercised via manual/test invokes.

**Dependency summary:** `1 → 2 → 3 → 4` forms the selection path; `3 → 5` forms
the delivery path (they meet at the `scheduled_notifications` queue); `6` wraps
both once they work manually.
