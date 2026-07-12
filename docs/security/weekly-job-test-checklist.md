# Security Test Checklist: Weekly Job Lambda (WBS 1.3.6)

**Scope**: Security-focused verification tasks for the WeeklyJobLambda before integration testing (WBS 1.5.2) and launch.

Reference documents:
- Threat model: `docs/security/weekly-job-threats.md`
- Security considerations: `docs/security/weekly-job-security.md`
- Auth design: `docs/security/auth-design.md`

---

## Section A: Google Places API Response Validation

- [ ] **A1** — Unit test: verify that a Places API response with a venue `name` containing `<script>alert(1)</script>` is stripped to plain text before being written to Firestore.
- [ ] **A2** — Unit test: verify that a venue `name` longer than 200 characters is truncated or rejected before write.
- [ ] **A3** — Unit test: verify that a `place_id` containing whitespace, slashes, or special characters causes the candidate venue to be discarded.
- [ ] **A4** — Unit test: verify that a `rating` of `6.0` (out of bounds) causes the candidate to be rejected; a `rating` of `-1` likewise.
- [ ] **A5** — Unit test: verify that a `user_ratings_total` (reviewCount) of `null` or negative causes the candidate to be rejected.
- [ ] **A6** — Unit test: verify that a `geometry.location` with latitude/longitude coordinates outside the bounding box of the target city (e.g., lat 0.0, lng 0.0 for a London search) causes the candidate to be discarded.
- [ ] **A7** — Unit test: verify that the `photo_reference` token from the Places response is stored as-is (opaque string) and that no full photo URL is stored on the WeeklySpot Firestore document.
- [ ] **A8** — Unit test: verify that a completely empty Places API response (no candidates) is handled without throwing an exception and produces the correct "no eligible venue" log entry.
- [ ] **A9** — Unit test: verify that a Places API response containing only candidates that are all in the venue exclusion list produces the "all filtered" log entry and does not write a WeeklySpot document.

---

## Section B: Service Account and Firestore Permissions

- [ ] **B1** — Manual verification: in Google Cloud Console (IAM), confirm the Firebase Admin SDK service account for the prod environment has only `roles/datastore.user` and no `roles/editor`, `roles/owner`, or `roles/firebase.admin`.
- [ ] **B2** — Manual verification: confirm that only the Lambda execution role ARN has `secretsmanager:GetSecretValue` permission on the `firebase-admin-prod` secret ARN in AWS IAM.
- [ ] **B3** — Manual verification: confirm that CloudTrail logging is enabled for the `firebase-admin-prod` secret in AWS Secrets Manager and that an alarm exists for access from any principal other than the Lambda execution role.
- [ ] **B4** — Code review: confirm that the Lambda initialisation code that calls `FirebaseApp.initializeApp()` does not log the credentials string, the service account JSON, or any derived key.
- [ ] **B5** — Code review: confirm that the Lambda does not write to any Firestore collection outside of: `cities` (read-only), `weekly_spots`, `venue_exclusions`, `invites`, `users/{uid}/pushTokens` (update `isValid` only), `users/{uid}/emailAddresses` (read-only).
- [ ] **B6** — Integration test: confirm that an attempt to write to `users/{uid}/email` (the root user document email field) from the WeeklyJobLambda code path is either absent or would fail with an access-denied error (validate via Firestore emulator with restricted rules applied).

---

## Section C: FCM Token Handling and Notification Payload

- [ ] **C1** — Code review: confirm that no Lambda log statement (SLF4J, CloudWatch structured log) outputs a raw FCM device token string.
- [ ] **C2** — Unit test: run the push notification batch builder with a real-looking device token in the input and assert it does not appear in any log output captured during the test (use a log capture appender to assert on log content).
- [ ] **C3** — Unit test: verify that the APNs push notification payload object (the `Notification` or `ApsAlert` builder output) does not contain: user email, Firebase UID, Firestore invite document ID, or any field derived from a `userId`.
- [ ] **C4** — Unit test: verify that the push payload deep link uses only `weekId` (e.g., `covey://feed?weekId=2026-W28`) and not a full Firestore document path or a `userId`.
- [ ] **C5** — Unit test: verify that when APNs returns a `BadDeviceToken` error for a specific token, the Lambda calls the Firestore update for `isValid = false` on the correct document path and does not retry the push for that token within the same job run.
- [ ] **C6** — Unit test: verify that the Lambda logs only the count of tokens processed (`token_count`) and the count of stale tokens found (`stale_token_count`), not any token values, in the per-city completion log entry.

---

## Section D: Rate Limiting and Idempotency

- [ ] **D1** — Integration test: trigger the WeeklyJobLambda twice in rapid succession for the same `weekId` using the Firestore emulator. Confirm that: (a) the second run does not call Google Places for any city where a `weekly_spots/{cityId}_{weekId}` document already exists; (b) no duplicate Invite documents are created (document count for `invites` is the same after the second run as after the first).
- [ ] **D2** — Integration test: simulate a user who has already RSVPed (`status = "YES"`) before the job re-runs. Confirm the Invite document's `status` field remains `"YES"` after the second run and is not reset to `"INVITED"`.
- [ ] **D3** — Unit test: verify that the Lambda handler rejects a non-EventBridge invocation in the prod environment. Pass a mock event with no `source` or `detail-type` field and assert the handler returns an error response without calling Google Places.
- [ ] **D4** — Infrastructure check: confirm the WeeklyJobLambda has reserved concurrency set to 1 in the Lambda console (prevents parallel executions multiplying Places API calls).
- [ ] **D5** — Infrastructure check: confirm a CloudWatch alarm exists on the Lambda's `Invocations` metric with a threshold of `> 1` per hour, routed to an SNS alert.
- [ ] **D6** — Unit test: assert that if the active city count returned from Firestore exceeds 20, the Lambda logs a warning and aborts rather than proceeding to call Google Places for all cities.

---

## Section E: Secrets and Configuration

- [ ] **E1** — Code review: search the Lambda codebase (and git history) for any hardcoded string matching `AIza` (Google API key prefix) or `firebase` adjacent to a private key or JSON blob.
- [ ] **E2** — Static analysis: run `git secrets --scan` (or equivalent TruffleHog scan) against the Lambda source tree and confirm zero findings.
- [ ] **E3** — Code review: confirm the Google Places API key is retrieved only from AWS Secrets Manager; confirm no fallback path that reads from an environment variable named `GOOGLE_PLACES_API_KEY` or similar in the production Lambda configuration.
- [ ] **E4** — Code review: confirm that if `SecretsManagerClient.getSecretValue()` throws an exception at cold start, the Lambda logs the error and terminates the invocation without proceeding to the city processing loop.
- [ ] **E5** — Code review: confirm the retrieved API key is stored in a `final` local field, not written to a log, not included in any exception message, and not serialised into any Firestore document.
- [ ] **E6** — Infrastructure check: confirm the Lambda environment variables in the AWS console contain no secrets (i.e., `GOOGLE_PLACES_API_KEY` is not set as a Lambda environment variable; only `SECRETS_MANAGER_SECRET_NAME` or equivalent non-sensitive pointers are present).

---

## Section F: Audit Logging

- [ ] **F1** — Code review: confirm the Lambda writes a structured job completion log entry to a Firestore document (or CloudWatch) that includes: `timestamp`, `weekId`, `citiesProcessed`, `citiesSkipped` (already had a spot), `citiesFailed` (Places API error), `totalPushSent`, `totalEmailSent`, `totalStaleTokensRemoved`. Confirm this log does not include any email address or device token.
- [ ] **F2** — Code review: confirm an admin alert email is sent when the Places API returns a quota error for any city (as shown in the activity diagram). Confirm this alert goes to an operations inbox, not to a user-facing address.
- [ ] **F3** — Integration test: trigger the job in the dev environment and verify a job completion log document appears in Firestore (or in CloudWatch structured logs) within 60 seconds of invocation.
- [ ] **F4** — Manual verification: confirm that CloudWatch log retention for the WeeklyJobLambda log group is set to a defined period (e.g., 90 days) and not indefinite, to limit the window of exposure for any sensitive data that may have inadvertently entered the logs.

---

## Section G: Email Notifications

- [ ] **G1** — Unit test: verify that users with `isVerified = false` on their email address documents are excluded from the email send list. Build a test with a mix of verified and unverified addresses and assert only verified addresses appear in the output of the email list builder.
- [ ] **G2** — Unit test: verify that email addresses are never logged. Use a log capture appender in the unit test and assert no string matching a valid email pattern (`.*@.*\..*`) appears in log output during the email dispatch phase.
- [ ] **G3** — Unit test: verify that the email body HTML template auto-encodes the `venueName` and `address` fields. Pass a venue name of `<b>Cocktail Bar</b>` and assert the rendered email body contains `&lt;b&gt;Cocktail Bar&lt;/b&gt;`, not the raw HTML tags.
- [ ] **G4** — Code review: confirm that the email send loop checks a user-level opt-out flag (e.g., `optOutNotifications: true`) before sending. If this flag is not yet implemented, this is a blocking item for GDPR compliance before launch.
- [ ] **G5** — Integration test: trigger the job in the dev environment and verify that the sent email (captured via a dev mailbox or SES sandbox log) does not contain: Firebase UID, Firestore document paths, or the recipient's email address in any non-address field (i.e., the email address appears only in the `To:` header, not in the body).

---

## Section H: GDPR Compliance

- [ ] **H1** — Schema review: confirm that the Invite document schema does not include the user's email address or device token (only `userId`, `weekId`, `venueId`, `city`, `status`, `createdAt`, `timestamp`).
- [ ] **H2** — Code review: confirm the job's user fetch step retrieves only the fields needed for processing: `selectedCity` (to filter by city), `pushTokens` subcollection (for push dispatch), `emailAddresses` subcollection (for email dispatch). It must not fetch or process `email` (root user document field) or `name`.
- [ ] **H3** — Integration test: delete a test user account mid-job (after the user list is fetched but before Invite documents are written). Confirm the Lambda handles a missing user document gracefully (skips the user with a log entry, does not throw an exception that aborts the city's processing).
- [ ] **H4** — Manual verification: confirm the app's onboarding flow and privacy policy describe: (a) that the user's selected city is used to send weekly venue notifications; (b) that push notifications and email notifications can be opted out of in settings. This is a prerequisite before launch.

---

## Sign-Off Criteria

All items in sections A, B, C, D, E must be completed before WBS 1.3.6 is marked done.

Sections F, G, H must be completed before WBS 1.5.2 (Weekly Job End-to-End Testing) and before submission to TestFlight (WBS 1.6.1).

| Section | Owner | Status |
|---------|-------|--------|
| A: Places API validation | Backend engineer | Not started |
| B: Service account permissions | Backend engineer / DevOps | Not started |
| C: FCM token handling | Backend engineer | Not started |
| D: Rate limiting and idempotency | Backend engineer | Not started |
| E: Secrets and config | Backend engineer / DevOps | Not started |
| F: Audit logging | Backend engineer | Not started |
| G: Email notifications | Backend engineer | Not started |
| H: GDPR compliance | Backend engineer / Product | Not started |
