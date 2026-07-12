# ADR-0007: Weekly Job Lambda — Design Decisions

**Status:** Accepted  
**Date:** 2026-07-12  
**Context:** WBS 1.3.6 (Weekly Job Lambda) planning phase  
**Decision Maker:** User (Charlie Knight)  
**Participants:** Solution Architect, Security Engineer, Quality Engineer, Domain Analyst  

---

## Summary

Five design decisions were made during the WBS 1.3.6 planning phase to resolve conflicts between existing documentation and confirmed requirements. These decisions establish the foundation for Phase 1 implementation.

---

## Decision 1: Collection Naming Convention

**Question:** Should Firestore collection names follow code style (camelCase) or database style (snake_case)?

**Options Considered:**
- Option A: Code style (`weeklySpots`, `pushTokens`, `venueExclusions`)
- Option B: Database style (`weekly_spots`, `push_tokens`, `venue_exclusions`)

**Decision:** **Code style** (camelCase)

**Rationale:**
- Existing Firestore collections in the project use camelCase (`users`, `invites`, `pushTokens`)
- Consistency with existing codebase reduces cognitive load for developers
- Java and TypeScript code is naturally camelCase; snake_case is non-idiomatic
- Firebase documentation examples use camelCase for JavaScript/Java SDK usage

**Implications:**
- All references to `venue_exclusion_list` in requirements are translated to `venueExclusions`
- All Firestore queries use camelCase collection names
- All test fixtures and mocks use camelCase

---

## Decision 2: EventBridge Schedule for "Friday 9am EST"

**Question:** How to handle Daylight Saving Time (EST vs. EDT) for the Friday 9am delivery notification?

**Options Considered:**
- Option A: UTC-4:30 compromise (always approximate, works year-round)
- Option B: UTC-5 (EST only, off by 1 hour in EDT summer months)
- Option C: UTC-4 (EDT only, off by 1 hour in EST winter months)
- Option D: Two EventBridge rules (manual seasonal swap)

**Decision:** **UTC-4:30 compromise**

**Rationale:**
- Simplest operational overhead (no manual rule switching required)
- Acceptable for MVP: users receive notifications within ±30 minutes of 9am
- Avoids the brittleness of manual seasonal switches
- Can be revisited post-MVP if precise timezone handling becomes a requirement
- CloudWatch alarms can alert if users report consistently late/early delivery

**Implications:**
- EventBridge cron expression: `cron(30 13 ? * 6 *)`  (UTC 13:30 = EST 8:30am to EDT 9:30am)
- Documentation notes this is approximate; future versions can support per-timezone delivery via Step Functions
- No code changes needed if users report ±30min variance is acceptable

---

## Decision 3: Email Provider

**Question:** How to send email notifications without a 24-48 hour AWS SES production approval wait?

**Options Considered:**
- Option A: AWS SES (requires production access request, 24-48 hour wait)
- Option B: Resend (third-party provider, no approval wait, generous free tier)
- Option C: Firebase Extensions (managed email, higher cost)

**Decision:** **Resend**

**Rationale:**
- Resend supports transactional email out of the box with no approval process
- Generous free tier supports MVP volume (thousands of emails/month)
- Better developer experience (simple REST API vs. AWS SDK complexity)
- Can be swapped for SES post-MVP if cost/compliance requires it
- Reduces time-to-MVP email functionality by 24-48 hours

**Implications:**
- Add Resend API key to AWS Secrets Manager under `resend-api-key-dev`
- Update `EmailService` (new component) to use Resend REST API instead of SES
- Update security threat model to reference Resend instead of SES
- Update CLAUDE.md to document the Resend integration

---

## Decision 4: City Centre Coordinates Storage

**Question:** Where to store latitude/longitude for Google Places Nearby Search (required for each city)?

**Options Considered:**
- Option A: Firestore City collection (normalized, one query reads both city name and coordinates)
- Option B: Lambda environment variables (hard-coded, smallest runtime payload)
- Option C: AWS Secrets Manager (encrypted, rotatable, adds per-run fetch latency)

**Decision:** **Firestore City collection**

**Rationale:**
- Single read operation fetches city name, timezone, and coordinates together
- Normalized schema allows admin to update coordinates without redeploying Lambda
- Consistent with "configuration in Firestore, secrets in Secrets Manager" philosophy
- Enables future features (city radius tuning, filtering by geographic region)
- No additional latency: city query happens once per job run regardless

**Implications:**
- Add `lat` and `lng` fields to City schema in `firebase-schema.md`
- Migrate existing cities in Firestore to include coordinates (one-time)
- Schema migration must complete before Phase 2.1 (venue selection implementation)
- Update ERD and entity reference docs

---

## Decision 5: WeeklySpot Re-run Idempotency

**Question:** If the job runs twice in the same week (e.g., EventBridge retry), should the second run overwrite or skip?

**Options Considered:**
- Option A: Last-run-wins (second run overwrites the WeeklySpot; simple but loses RSVPs)
- Option B: First-run-wins (second run skipped if spot exists; protects RSVPs)

**Decision:** **First-run-wins (skip if exists)**

**Rationale:**
- Protects existing user RSVPs if they've already replied to the invitation
- A user who replied "YES" on Thursday should not see their RSVP clobbered to "INVITED" on a retry Friday
- Firestore `create` semantics naturally implement this pattern
- Aligns with event sourcing principles: first write is the canonical source

**Implementation Details:**
- Use Firestore `create` operation (fail if document already exists) instead of `set` (overwrite)
- Catch `ALREADY_EXISTS` error and treat as idempotent success (log and continue)
- For Invite documents, use similar `create` semantics to prevent overwriting user RSVPs

**Implications:**
- WeeklySpot and Invite writes must use Firestore `create` instead of `set`
- Job logs must record when a city is skipped due to already-existing spot
- Test suite must verify that re-running the job does not produce duplicate documents or lose RSVPs

---

## Related Decisions

These decisions interact with earlier ADRs:

- **ADR-0001** (Firebase) — Collection structure, Firestore schema
- **ADR-0002** (Secrets Management) — Where API keys and provider credentials are stored
- **ADR-0003** (Error Handling) — Per-city error propagation and retry strategy

---

## Future Considerations

1. **Precise timezone delivery:** Post-MVP, implement per-city scheduled delivery using Step Functions or Lambda scheduled rules, so "Friday 9am" is exact in each city's local timezone.

2. **Email provider migration:** If Resend free tier is exhausted, migrate to AWS SES (production access will be pre-approved by then).

3. **Coordinate management:** If many cities are added, consider a CSV import flow for coordinates instead of manual Firestore edits.

4. **UTC-4:30 reconsideration:** If users report significant ±30min variance, revisit the timezone strategy.

---

**Document History:**
- 2026-07-12: Accepted (planning phase)
