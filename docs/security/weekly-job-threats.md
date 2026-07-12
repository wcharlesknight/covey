# Threat Model: Weekly Job Lambda (WBS 1.3.6)

**Scope**: The automated Lambda that selects a weekly venue via Google Places, writes WeeklySpot and Invite documents to Firestore, and dispatches FCM push notifications and email notifications.

This document extends the main threat model (`docs/security/threat-model.md`) with threats specific to the scheduled job component and its external dependencies.

---

## System Boundary

```
EventBridge (Scheduler)
    |
    v
WeeklyJobLambda (AWS, private network)
    |-- reads/writes --> Firestore (via Firebase Admin SDK)
    |-- calls --> Google Places API (external, HTTPS)
    |-- calls --> APNs / FCM (external, HTTPS)
    |-- calls --> Email Service / AWS SES (external, HTTPS)
```

Trust boundaries crossed:
- EventBridge -> Lambda: internal AWS (trusted, but invocation source should be verified)
- Lambda -> Firestore: internal to GCP via Firebase Admin SDK (trusted, but writes are unrestricted)
- Lambda -> Google Places: external HTTPS (untrusted response data)
- Lambda -> APNs/FCM: external HTTPS (push delivery confirmation only; not a data source)
- Lambda -> Email Service: external HTTPS (delivery confirmation only)

---

## Threat 1: Data Leakage via Notification Payloads

**Category**: Information Disclosure (STRIDE: I)

**Description**: Push notification payloads and email notification bodies are delivered to user devices and inboxes. If these payloads are constructed from Firestore data that includes PII (email addresses, Firebase UIDs, invite document IDs, raw Firestore paths), that data is exposed to the device and to any notification log or analytics system (e.g., Apple Push Notification reporting, FCM Analytics).

**Attack Vectors**:
1. A developer misconfigures the push payload builder to include the user's email address from the Invite document (which carries `userId`) in the notification body.
2. The Firestore document ID (`invites/{userId}_{weekId}`) is included verbatim in a deep link in the notification, exposing the user's Firebase UID.
3. The FCM/APNs push payload is logged in CloudWatch for debugging purposes, revealing device tokens.

**Likelihood**: Medium (common mistake in notification system implementation)

**Impact**: Medium — push tokens are device identifiers; UIDs combined with invite IDs allow social graph inference; email addresses in payloads violate data minimisation.

**Mitigations**:
- Push notification payload must contain only: venue name, city display name, week identifier (`2026-W28`), and a deep link that uses `weekId` only (not the Firestore invite document ID).
- Email notification body must contain: venue name, address, hours, and an RSVP deep link using `weekId`. It must not contain the Firebase UID or the raw Firestore path.
- The Lambda logger must be configured with a field filter that strips any key named `deviceToken`, `token`, `email`, or `emailAddress` from structured log output.
- Code review checklist item: verify payload builder unit test asserts no PII fields in the output object.

**Residual Risk**: Low — if payload builders are tested against a field whitelist.

---

## Threat 2: API Quota Exhaustion via Repeated Job Invocation

**Category**: Denial of Service (STRIDE: D)

**Description**: An attacker or operational error causes the WeeklyJobLambda to execute multiple times in quick succession, exhausting the Google Places API daily quota. This results in the job failing for legitimate Thursday runs, meaning no WeeklySpot is created and no notifications are sent.

**Attack Vectors**:
1. An attacker with write access to the AWS account (e.g., through a compromised CI/CD pipeline or stolen AWS credentials) directly invokes the Lambda via the AWS CLI: `aws lambda invoke --function-name WeeklyJobLambda`.
2. An EventBridge rule misconfiguration (e.g., cron syntax error) triggers the job every minute instead of once per week.
3. A deployment pipeline re-invokes the Lambda as part of a smoke test using the production function ARN instead of a dev/staging function.
4. A Lambda DLQ (Dead Letter Queue) is misconfigured and re-drives failed invocations repeatedly.

**Likelihood**: Medium (Lambda direct invocation is a common testing shortcut; EventBridge cron misconfiguration has happened in practice)

**Impact**: High — quota exhaustion prevents all users in all cities from receiving their weekly notification. The Places API free tier allows 5,000 Nearby Search requests/day. With 10 cities and 1 retry each, the job uses ~20 requests. Repeated invocations can hit the daily cap within minutes.

**Mitigations**:
- **Idempotency guard**: At the start of each city's processing, check whether `weekly_spots/{cityId}_{weekId}` already exists. If it does, skip the Places API call and Invite creation entirely for that city. This makes re-running the job on the same week safe and quota-neutral.
- **Invocation source check**: At the Lambda handler entry point, inspect the invocation event source. EventBridge events include `"source": "aws.events"` and a `"detail-type": "Scheduled Event"` field. If either is absent, the invocation is manual or from an unexpected source. In production (`ENVIRONMENT=prod`), reject non-EventBridge invocations with an early return and a CloudWatch log entry.
- **Lambda concurrency cap**: Set reserved concurrency for the WeeklyJobLambda to 1. This prevents concurrent executions from multiplying Places API calls if two invocations overlap.
- **CloudWatch alarm**: Create a `WeeklyJobInvocations` metric filter and alarm if invocation count exceeds 1 within any 1-hour window. Alert via SNS.
- **IAM policy restriction**: The Lambda execution role should not include `lambda:InvokeFunction` permission on its own ARN (prevent self-invocation). The CI/CD role should invoke only dev/staging function ARNs, never the prod function.

**Residual Risk**: Low — idempotency guard eliminates quota waste even if invoked multiple times.

---

## Threat 3: Service Account Privilege Escalation

**Category**: Elevation of Privilege (STRIDE: E)

**Description**: The Firebase Admin SDK service account used by the WeeklyJobLambda has unrestricted write access to Firestore (and potentially to other Firebase services if the service account was generated with default project-owner permissions). A compromise of this service account — through the AWS Secrets Manager secret, a Lambda memory dump, or an SSRF vulnerability — would allow an attacker to read or write any Firestore document in the production database.

**Attack Vectors**:
1. The `firebase-admin-prod` secret in AWS Secrets Manager is accessed by a principal outside the Lambda execution role (e.g., a developer, a misconfigured CI/CD pipeline).
2. The Lambda encounters a server-side request forgery (SSRF) vulnerability in the venue data processing path, allowing an attacker to exfiltrate the service account credentials from the Lambda environment.
3. A Lambda code vulnerability (e.g., an unintended deserialization or log injection) leaks the service account JSON into CloudWatch logs.
4. The service account private key is committed to source control by a developer testing locally.

**Likelihood**: Low — SSRF in a Lambda that only calls known Google endpoints is low probability; source control leakage is mitigated by existing pre-commit hooks and GitHub secret scanning.

**Impact**: Critical — full read/write access to all Firestore collections, including user profiles, email addresses, push tokens, and all invite/RSVP data.

**Mitigations**:
- **Least privilege on the service account role**: Reduce the Firebase Admin SDK service account from project-owner to `roles/datastore.user` only (see weekly-job-security.md section 4). This caps the blast radius: an attacker with the service account can read/write Firestore documents but cannot modify Firebase Auth users, access Firebase Storage, or call Firebase Admin APIs.
- **Restrict Secrets Manager access**: The Lambda IAM execution role should be the only principal with `secretsmanager:GetSecretValue` permission on the `firebase-admin-prod` secret ARN. Verify this via IAM policy simulator. Enable CloudTrail logging for the secret and alert on unexpected callers.
- **Never log the service account JSON**: The Lambda initialisation code that calls `FirebaseApp.initializeApp()` must not log the credentials object, its JSON string representation, or any derived key material.
- **SSRF prevention**: The Lambda should only make outbound HTTPS calls to a known allowlist of hostnames: `places.googleapis.com`, Firebase services (`firestore.googleapis.com`, `firebase.googleapis.com`), APNs (`api.push.apple.com`), and the configured email service endpoint. Implement an outbound domain allowlist via Lambda VPC configuration and a restrictive security group egress rule, rather than relying on application-level controls.
- **Separate service accounts** (post-MVP): Create a dedicated service account for the WeeklyJobLambda with only Firestore write access, separate from the service account used by the user-facing API Lambda. This limits cross-function blast radius.

**Residual Risk**: Medium until service account role is scoped down. Low after role reduction.

---

## Threat 4: Google Places API Compromise or Malicious Response

**Category**: Tampering + Information Disclosure (STRIDE: T, I)

**Description**: If the Google Places API is compromised (supply chain attack), or if an attacker can influence the Places API responses seen by the Lambda (e.g., via DNS cache poisoning of the Lambda's resolver, or a BGP hijack — both highly unlikely), they could inject malicious data into the Firestore WeeklySpot document and ultimately into push notification payloads and email bodies sent to all users.

**Blast Radius**: Every user in every active city who receives a notification for that week's WeeklySpot is exposed to the malicious venue data.

**Attack Vectors**:
1. Google Places API returns an XSS payload in the `name` or `formatted_address` field (e.g., `<script>...</script>` in venue name). The Lambda writes this verbatim to Firestore. The iOS app renders it in a web view or the email client renders it in an HTML email.
2. The `photoUrl` / photo reference is replaced with an attacker-controlled URL. The iOS app fetches the photo from the attacker's server, leaking the user's IP address and device metadata.
3. The `place_id` (Google Places ID) is set to an attacker-controlled value that, when combined with a Google Maps deep link, redirects users to a malicious URL.
4. The `geometry.location` is set to coordinates far outside the expected city bounds, causing location-based features to malfunction.

**Likelihood**: Very Low for a true Google Places API compromise (significant nation-state or insider threat capability required). Medium for input not being validated (developer assumption that external API data is safe).

**Impact**: Medium — if HTML/script injection reaches the email template or a web view, it could enable XSS. Photo URL substitution could leak user IPs. Google Maps deep link manipulation could redirect users to phishing pages.

**Mitigations**:
- **Input validation on every Places field** (detailed in weekly-job-security.md section 3): Strip HTML/control characters from all text fields before writing to Firestore.
- **Photo URL handling**: Do not store the full photo URL from Places. Store only the `photo_reference` (a Google-issued opaque token). The iOS app constructs the full Google Places Photo API URL client-side. This means the Lambda never fetches or proxies the photo, eliminating the attacker-controlled URL risk.
- **Google Maps deep link allowlist**: Deep links to Google Maps should be constructed server-side from the validated `place_id`, not from any URL field returned by Places. Format: `https://www.google.com/maps/place/?q=place_id:{validated_place_id}`. Validate `place_id` against the pattern `^[A-Za-z0-9_-]+$` before constructing the URL.
- **Email template HTML encoding**: All Firestore-sourced fields rendered in HTML email templates (venue name, address) must be HTML-encoded. If using an email templating library, use its built-in auto-escaping; do not inject raw strings into HTML templates via string concatenation.
- **TLS certificate verification**: The Firebase Admin SDK and the Places API HTTP client must not disable TLS certificate verification. Ensure no `setSSLSocketFactory(TrustAllCerts)` or equivalent patterns exist in the codebase.

**Residual Risk**: Low — with field validation and HTML encoding in place, a Places API response compromise has limited downstream effect.

---

## Threat 5: User Location Inference from City-Level Notification Targeting

**Category**: Information Disclosure (STRIDE: I)

**Description**: The weekly job sends push notifications and emails to all users in a given city. An attacker who can observe notification delivery (e.g., an attacker who also uses the app and can see that a notification was sent at the same time to their device) cannot directly learn other users' identities. However, the FCM/APNs system logs on Apple/Google servers associate the notification with specific device tokens, which in turn map to specific user accounts. This is a third-party data exposure risk, not a first-party risk.

**Additional risk**: If the Lambda logs "sent push to N users in London", and N is a small number (e.g., 3 in a new city), a motivated attacker with CloudWatch access could infer that only 3 users have selected London, potentially identifying individuals.

**Likelihood**: Low for targeted attack; Medium for inadvertent log exposure.

**Impact**: Low — city-level user counts are not highly sensitive, but they can contribute to social graph inference.

**Mitigations**:
- CloudWatch logs must be restricted to the Lambda execution role and project administrators. No read access should be granted to general developers unless needed for debugging.
- The completion log written to Firestore (`venue_exclusions` or a job log collection) should include push count and email count as aggregate numbers only, not any breakdown of which users received which notification.
- For cities with fewer than 10 users, consider whether to suppress the per-city user count from logs entirely and log only a boolean `"push_sent": true`.

**Residual Risk**: Low.

---

## GDPR and Privacy Considerations

### Data Minimisation

- The weekly job processes user email addresses (from `emailAddresses` subcollections) and FCM device tokens (from `pushTokens` subcollections). Both are PII or device identifiers.
- The job must not copy email addresses or device tokens to any persistent log, job completion document, or intermediate storage. Process in memory only.
- The `invites/{userId}_{weekId}` document must not include the user's email address or device token. It must include only: `userId`, `weekId`, `venueId`, `city`, `status`, `createdAt`.

### Legal Basis for Processing

- Email notifications: The user's selection of a city and registration of an email address constitutes opt-in to receive weekly venue notifications. This should be made explicit in the onboarding flow and documented in the privacy policy.
- Push notifications: iOS requires explicit permission grant from the user before push tokens are registered. The existing push token registration flow (WBS 1.4.5.1) must request permission before registering the token.

### Right to Erasure (GDPR Article 17)

- On account deletion, all of the user's push tokens, email addresses, and invite documents must be deleted before the user document is removed. The weekly job must not fail if it encounters a user whose account has been deleted between the time it fetched the user list and the time it writes the Invite document (handle a missing user document gracefully with a skip, not an error).

### Unsubscribe / Opt-Out

- Add `optOutNotifications: boolean` to the user document or a dedicated preference document. The weekly job must check this flag before sending push or email to any user. This supports GDPR right to object to processing (Article 21) and is a prerequisite for App Store compliance on marketing-adjacent notifications.

---

## Risk Summary

| Threat | Likelihood | Impact | Priority |
|--------|-----------|--------|----------|
| T1: PII leakage in notification payloads | Medium | Medium | High — address in 1.3.6 implementation |
| T2: API quota exhaustion via repeated invocation | Medium | High | High — implement idempotency guard and invocation source check in 1.3.6 |
| T3: Service account privilege escalation | Low | Critical | High — scope down service account role before prod deploy |
| T4: Google Places API compromise / malicious response | Very Low | Medium | Medium — implement field validation in 1.3.6 |
| T5: User location inference from notification targeting | Low | Low | Low — restrict CloudWatch access, suppress small-city counts |
