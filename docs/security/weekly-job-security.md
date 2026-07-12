# Security Considerations: Weekly Job Lambda (WBS 1.3.6)

**Scope**: Lambda function that runs Thursday night, queries Google Places API, writes WeeklySpot and Invite documents to Firestore, and dispatches FCM push notifications and email notifications Friday 9am.

---

## 1. Google Places API Key Handling

### Current Design (from secrets-management.md)

The Google Places API key is stored in AWS Secrets Manager under `google-places-dev` and `google-places-prod`. The Lambda retrieves it at cold start via the AWS SDK. The Lambda execution role has a least-privilege IAM policy scoped to the specific secret ARN. Rotation is scheduled every 90 days.

### Risks and Controls

**Risk**: The API key is fetched at Lambda cold start, meaning the same key is used for the entire lifetime of a warm container. If the key is leaked from memory or from a crash dump, it could be abused before rotation.

**Controls**:
- Retrieve the key once at cold start; store in a local `final` field. Do not re-fetch on every city iteration.
- Never log the key value, not even partially (no `key.substring(0, 4) + "..."` patterns in logs).
- The key must be restricted in Google Cloud Console to the `places.googleapis.com` API only, with no other APIs permitted.
- The key must have an HTTP referrer restriction set to deny — since this is a server-side call, use an IP restriction scoped to the Lambda NAT gateway outbound IP range, or alternatively use a server-key restriction that allows no HTTP referrers (server keys with no restriction are valid for Lambda-to-Google calls but must be treated as secret).
- AWS Secrets Manager should have rotation automated using a Lambda rotation function rather than manual 90-day rotation, which is easy to miss in a solo project. Even without automated rotation, set a CloudWatch Events reminder at 80 days.
- Secret access must be audited: enable AWS CloudTrail for `secretsmanager:GetSecretValue` calls on the secret ARN and alert on any calls from unexpected principals (i.e., anything that is not the WeeklyJobLambda execution role ARN).

**What to add to the implementation**:
- On `GetSecretValue` failure at cold start, fail the Lambda invocation with a clear error rather than falling through to use a null/empty key, which would produce confusing downstream errors.
- Validate the retrieved key is non-null and non-empty before any Google Places call.

---

## 2. Google Places API Rate Limit Protection

### Quota Context

Google Places Nearby Search has per-project quotas (daily request limit, and a per-second QPS cap). A bug in the job that causes it to loop or retry excessively could exhaust the daily quota, causing the job to fail for the rest of that day and potentially incurring unexpected billing charges.

### Controls

**Job-level controls**:
- The job must not run more than once per calendar day. The EventBridge cron rule enforces this at the scheduler level, but the Lambda handler itself should verify: before calling Google Places for any city, check whether a `weekly_spots/{cityId}_{weekId}` document already exists. If it does, skip that city entirely rather than making a redundant Places call. This makes the job idempotent.
- The number of active cities is bounded. Before entering the city loop, assert that the city count is within a reasonable upper bound (e.g., `<= 20`). If the count is unexpectedly large (e.g., a data entry error created hundreds of city documents), abort and alert rather than issuing hundreds of Places API calls.
- The retry logic for "no eligible venue" (expand search radius 50%, retry) must be hard-capped at one retry per city per run. Do not implement exponential backoff that could re-run the Places call multiple times within a single invocation.

**Manual trigger protection**:
- The EventBridge scheduled rule is the only authorised trigger. Direct Lambda invocations (e.g., for testing) should be guarded by an environment variable flag (`ALLOW_MANUAL_TRIGGER=true`) that is set only in the dev environment, never in prod. Any invocation in prod that does not originate from the EventBridge source should be rejected at the handler entry point.
- This guard prevents a misconfigured CI/CD pipeline or a developer running `aws lambda invoke` in prod from consuming API quota or creating duplicate WeeklySpot/Invite documents.

**Alerting**:
- If the Places API returns a quota-exceeded error (`OVER_QUERY_LIMIT` or HTTP 429), the Lambda should log a structured error and raise a CloudWatch custom metric `PlacesQuotaExceeded`. A CloudWatch alarm on this metric with threshold >= 1 should send an SNS alert so the quota situation is visible immediately.

---

## 3. Google Places Data Validation

### Risk

Google Places API responses are external data. A compromised or malfunctioning Google endpoint, a Places API schema change, or a crafted response from an SSRF-like scenario could inject unexpected data into Firestore documents that are then surfaced to users via the iOS feed.

### Validation Requirements

Every field read from a Places API response must be validated before writing to Firestore:

| Field | Validation |
|-------|-----------|
| `place_id` (venueId) | Non-null, non-empty string, max 500 chars, no whitespace |
| `name` (venueName) | Non-null, non-empty string, max 200 chars, strip control characters |
| `formatted_address` | Non-null string, max 500 chars, strip control characters |
| `photos[0].photo_reference` (photoUrl) | Non-null string; must be used only to construct a Google Places Photo API URL — never treat as a user-supplied URL or redirect target |
| `rating` | Numeric, in range 0.0–5.0 |
| `user_ratings_total` (reviewCount) | Non-negative integer |
| `opening_hours` | Validated as a map with expected keys; missing hours treated as "unknown" rather than throwing |
| `geometry.location` | Lat/lng within plausible geographic bounds for the target city |

**Rejection policy**: If any of the mandatory fields (`place_id`, `name`, `formatted_address`, `rating`, `user_ratings_total`) fails validation, discard that candidate venue entirely. Do not write partial data to Firestore.

**Injection protection**: `venueName` and `address` are written to Firestore and later displayed in iOS push notification payloads and email bodies. These must be stripped of HTML tags and control characters server-side before storage. Firestore stores data as structured documents (not SQL), so SQL injection is not applicable, but stored XSS is a risk if the iOS app ever renders these fields via a web view or HTML email template.

**Photo URLs**: Never proxy or redirect to `photoUrl` from the Lambda. The iOS app should call the Google Places Photo API directly using the photo reference, not an app-controlled redirect endpoint. This eliminates any open redirect risk.

---

## 4. Firestore Operations: Service Account Permissions

### Trust Model

The Weekly Job Lambda uses the Firebase Admin SDK with a service account that has Firestore Admin-level write access. This is correct for creating WeeklySpot and Invite documents that users cannot create directly. However, the blast radius if this service account is compromised is the entire Firestore database.

### Controls

**Least privilege for the service account**:
The Firebase Admin SDK service account (`firebase-admin-prod`) has project-owner-level access by default when generated from the Firebase Console. This is overly broad. The service account should be granted only the minimum required roles:

- `roles/datastore.user` — allows read/write to Firestore documents (sufficient for the job).
- No `roles/firebase.admin` unless needed for Auth operations.
- No `roles/storage.admin` or other Firebase services not used by the job.

To restrict the existing service account in Google Cloud IAM:
1. Go to Google Cloud Console -> IAM & Admin -> IAM.
2. Locate the service account email (e.g., `firebase-adminsdk-xxxxx@covey-prod.iam.gserviceaccount.com`).
3. Remove the `Editor` role if present.
4. Grant only `Cloud Datastore User` (`roles/datastore.user`).

**Separate service accounts per Lambda function** (preferred for future work):
The WeeklyJobLambda and the user-facing API Lambda use the same service account today (implied by a single `firebase-admin-prod` secret). After MVP, create separate service accounts per Lambda function so that a compromise of the WeeklyJobLambda service account cannot also be used to call Firebase Auth APIs.

**Firestore Security Rules do not protect Admin SDK writes**:
Firestore security rules apply to client SDK calls only. The Admin SDK bypasses all rules. This is intentional — it means the Lambda can write any document. The corollary is that there is no Firestore-layer check on what the Lambda writes. All validation must happen in the Lambda code itself before writing to Firestore (see Data Validation section above). Do not rely on Firestore rules to catch bad data from the Lambda.

**What the Lambda must not do**:
- Must not delete or overwrite existing WeeklySpot documents for past weeks.
- Must not modify any user's profile, RSVP status, or email address (the job should only create new Invite documents with status `INVITED`; it must not touch existing Invite documents).
- Must not read or write to collections outside of: `cities`, `weekly_spots`, `venue_exclusions`, `invites`, `users/{uid}/pushTokens`, `users/{uid}/emailAddresses`.

---

## 5. FCM / Push Token Handling

### Risk

FCM push tokens are device identifiers. If logged or exposed in error messages, they can be used by a third party to enumerate devices or attempt to correlate users with device fingerprints.

### Controls

**Never log push tokens**:
- The array of device tokens fetched from Firestore must never appear in CloudWatch logs, not as individual tokens, not as a count-with-sample, not in exception messages.
- Use a structured log format that includes only non-sensitive metadata: `{ "event": "push_dispatch_start", "city": "london", "token_count": 42 }`. Log the count, not any token values.
- Exception handlers that catch APNs/FCM delivery errors must log the error code and the document path (e.g., `users/{uid}/pushTokens/{tokenId}`) but never the raw token string.

**Token lifecycle during the job**:
- Stale token handling: When APNs returns `BadDeviceToken` or `Unregistered`, the Lambda marks `isValid = false` on the push token document. This is already in the sequence diagram. Ensure the Firestore write uses a targeted field update (`update({ "isValid": false })`) rather than overwriting the whole document.
- The Lambda must not attempt to re-use a token that has been marked invalid within the same job run. Process the invalid response and move on.

**Notification payload content**:
- Push notification payloads dispatched via APNs must contain only: venue name, city, a deep link URI. They must not include: user email, Firebase UID, invite document ID (use the deep link path instead of the raw Firestore ID), or any other PII.
- The `weekId` (e.g., `2026-W28`) is safe to include in a deep link.

---

## 6. Email Notification Handling

### Risk

The job reads `emailAddresses` subcollections for each user in the target city and sends spot notifications. This involves processing PII (email addresses) in a scheduled, automated context.

### Controls

**Email address handling in logs**:
- Email addresses must be redacted from all log output. Use a helper that masks the local part: `w***@example.com`. Assign to a local variable named to make the masking intent clear (e.g., `redactedEmail`).
- Never log the raw email array fetched from Firestore.

**Verified emails only**:
- Only send to `emailAddresses` where `isVerified == true`. This is already in the sequence diagram. The Firestore query must include this filter; do not fetch all addresses and filter in memory.

**Email send rate**:
- The auth-design.md document specifies a maximum of 10 emails per user per day. For the weekly job, each user receives at most one email per run (the weekly spot notification). The job runs once per week. This is within the rate limit, but the limit should still be checked programmatically before sending to guard against edge cases (e.g., a manual re-trigger).

**Unsubscribe handling**:
- There is no explicit unsubscribe mechanism in the current schema. Before launch, add an `isUnsubscribed: boolean` field to `emailAddresses` and filter it out of the job's email list. This is a GDPR requirement (right to opt out of marketing/notification emails) and an essential eight email hygiene control.

---

## 7. Error Handling and Retry Safety

### Risk

A Lambda that throws an uncaught exception on retry could create duplicate Firestore documents or send duplicate push notifications/emails.

### Controls

**Idempotency by design**:
- `weekly_spots/{cityId}_{weekId}` uses a deterministic document ID. A second write will overwrite the first rather than create a duplicate. This is safe as long as the venue selection logic is deterministic (same input -> same output). Ensure the spot selection picks the highest-ranked venue consistently; do not use random tiebreaking.
- `invites/{userId}_{weekId}` uses a deterministic document ID. Re-running the job for the same week will overwrite existing Invite documents. This is acceptable only if the status is preserved: use `set(..., SetOptions.mergeFields("createdAt", "venueId", "city", "weekId"))` so that a user's existing RSVP status (`YES`, `NO`, `INTERESTED`) is not overwritten to `INVITED` on a retry.
- Before writing any Invite, check if the document already exists (`getDocument()` before `setDocument()`). If it exists and status is not `INVITED`, skip the write. This avoids clobbering a user's RSVP.

**Push notification deduplication**:
- There is no native push deduplication in APNs for topic-based pushes. In a retry scenario, users could receive two push notifications for the same weekly spot. Mitigate by: (a) checking the `weekly_spots/{cityId}_{weekId}` document for a `pushSentAt` timestamp field before dispatching; if present, skip push dispatch; (b) after a successful push batch, write `pushSentAt: serverTimestamp()` to the WeeklySpot document.

**Lambda timeout**:
- The Lambda must complete within its configured timeout. With many active cities and large user bases, the per-city inner loop (write Invite, send push, send email per user) could be slow. Use Firestore batched writes for Invite creation (up to 500 operations per batch). Use FCM batch notification API (send up to 500 tokens per call) rather than individual APNs calls per token.
- Set a per-city timeout budget and skip to the next city rather than letting one slow city cause a Lambda timeout that aborts all remaining cities.
