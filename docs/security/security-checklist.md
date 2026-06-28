# Security Requirements & Implementation Checklist

This checklist captures all security implementation requirements for the location-notifier app. Items are organized by category and prioritized by risk. Each item maps to one or more threat model findings (see `threat-model.md`).

**Status key**: [ ] Not started | [x] Complete | [~] Partial / In progress

---

## 1. Transport Security

Requirements apply to all communication between the iOS app, API Gateway, Java Lambda, and external services.

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 1.1 | All API Gateway endpoints must be HTTPS only; HTTP must be rejected at the gateway (return 301 redirect or reject entirely) | T4 | Critical |
| 1.2 | API Gateway TLS policy must enforce TLS 1.2 as minimum; TLS 1.3 preferred | T4 | Critical |
| 1.3 | iOS App Transport Security (ATS) must not have `NSAllowsArbitraryLoads` set to `true` in `Info.plist` | T4 | Critical |
| 1.4 | All Lambda-to-external-service calls (Firebase, Google Places, APNs, FCM, email provider) must use HTTPS | T4 | High |
| 1.5 | Do not log raw HTTP request bodies that may contain tokens or user data | I2, I4 | High |

**Implementation notes**:
- API Gateway enforces HTTPS by default; verify no HTTP stage is enabled.
- ATS is enabled by default on iOS; confirm no `Info.plist` exceptions are present.

---

## 2. Authentication

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 2.1 | Every API request to a protected endpoint must include a valid Firebase ID token in the `Authorization: Bearer` header | S1, S2 | Critical |
| 2.2 | The Java Lambda must validate Firebase ID tokens using the Firebase Admin SDK `verifyIdToken()` method on every request (not cached validation) | S2 | Critical |
| 2.3 | Token validation must check: signature (RS256 via Google public keys), `aud` (Firebase project ID), `iss` (`https://securetoken.google.com/{project-id}`), and `exp` (not expired) | S2 | Critical |
| 2.4 | The `uid` field must always be sourced from the verified token claims, never from client-supplied request parameters | E1 | Critical |
| 2.5 | Firebase token revocation must be used on account deletion and optionally on suspected compromise; use `verifyIdToken(token, true)` on security-sensitive endpoints | S1 | High |
| 2.6 | Requests with missing, malformed, or expired tokens must return `401 Unauthorized` with a generic error body | S2, I5 | High |
| 2.7 | Firebase Admin SDK must be initialized once at Lambda startup (cold start), not on every invocation, to avoid initialization overhead and key cache thrashing | - | Medium |

---

## 3. iOS Keychain Storage

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 3.1 | Firebase ID tokens and refresh tokens must be stored in the iOS Keychain (the Firebase iOS SDK uses Keychain by default; do not override this) | S1 | Critical |
| 3.2 | Tokens must not be stored in `UserDefaults`, `NSUserDefaults`, local files, or any unencrypted storage | S1 | Critical |
| 3.3 | On user sign-out, call `Auth.auth().signOut()` to clear all Firebase tokens from the Keychain | S1 | High |
| 3.4 | Keychain items must use `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` accessibility level (the Firebase SDK default; verify this is not changed) | S1 | High |
| 3.5 | No sensitive data (email addresses, push tokens, raw user data) may be stored in the iOS Keychain beyond what the Firebase SDK stores automatically | I2, I4 | Medium |

---

## 4. API Authorization (Resource-Level)

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 4.1 | All read operations on invites, RSVPs, and user profiles must verify that the resource's `userId` matches the authenticated `uid` | E1, I1 | Critical |
| 4.2 | All write operations (create, update, delete) on invites, RSVPs, and user profiles must verify resource ownership before executing | T1, E1 | Critical |
| 4.3 | Database queries must include a `WHERE userId = :uid` clause; do not fetch all records and filter in application memory | I1 | Critical |
| 4.4 | All resource IDs (invite IDs, RSVP IDs, profile IDs) must be UUID v4 (non-sequential, cryptographically random) to prevent IDOR enumeration | E3 | High |
| 4.5 | Requests accessing resources belonging to another user must return `403 Forbidden`, not `404 Not Found`, to avoid information leakage about resource existence - OR - consistently return `404` to avoid confirming resource existence to unauthorized users (choose one approach and apply consistently) | I1, E1 | High |
| 4.6 | The 4-week history filter must be applied server-side; any client-supplied date range parameters must be clamped to a maximum of 4 weeks from the current date | E4 | Medium |
| 4.7 | Social signal aggregates (invite counts, RSVP counts) must only be returned in the context of the authenticated user's own events; no cross-user aggregate queries in MVP | I3 | Medium |
| 4.8 | Admin operations (user deletion, data export, metrics) must not be exposed via the public API Gateway; use direct Lambda invocation with IAM authentication | E2 | High |

---

## 5. Input Validation & Sanitization

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 5.1 | All user-supplied text fields (display names, invite titles, descriptions) must be validated for maximum length server-side before persistence | T2 | High |
| 5.2 | All user-supplied text must be treated as untrusted; use parameterized queries or a typed ORM for all database interactions; do not concatenate user input into SQL strings | T2 | Critical |
| 5.3 | City selection must be validated against the server-side list of allowed cities; do not accept arbitrary strings for location fields | T2 | High |
| 5.4 | Email addresses supplied in any request body must be validated against RFC 5322 format before use | T2 | Medium |
| 5.5 | Content-Type headers must be validated on all POST/PUT endpoints; reject requests with unexpected content types | T2 | Medium |
| 5.6 | Request body size limits must be enforced at API Gateway (e.g., maximum 10 KB for invite payloads) | D3 | Medium |

---

## 6. Rate Limiting

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 6.1 | Invite creation must be rate-limited to a maximum of 20 invites per user per hour; enforce in Lambda middleware | D1 | High |
| 6.2 | Transactional email sends must be limited to a maximum of 10 emails per user per day; enforce in Lambda before calling the email service | D2 | High |
| 6.3 | API Gateway throttling must be configured: steady-state rate and burst limits appropriate for the expected user base (start conservatively, e.g., 500 req/s steady, 1000 req/s burst per stage) | D3 | High |
| 6.4 | AWS WAF must be attached to API Gateway with IP-based rate limiting rules to block volumetric attacks | D3 | Medium |
| 6.5 | Push notification sends must be de-duplicated: do not send more than one notification per event per user per 5-minute window | D2 | Medium |

---

## 7. Push Token Security

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 7.1 | Push token registration must require a valid Firebase ID token | S4 | High |
| 7.2 | Push tokens must be stored in the database bound to the authenticated `uid`; a user cannot register a push token under another user's account | S4 | High |
| 7.3 | Push tokens must never be returned in any API response body | I4 | High |
| 7.4 | Push tokens must be excluded from Lambda log output and error messages | I4 | High |
| 7.5 | On APNs/FCM delivery failure indicating an invalid or unregistered token, the Lambda handler must delete the token from the database | S4, D2 | Medium |
| 7.6 | Push tokens must be deleted from the database when a user signs out (via `DELETE /users/me/push-token`) or deletes their account | S4 | High |

---

## 8. Audit Logging

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 8.1 | Audit log entries must be written to an append-only log destination (e.g., CloudWatch Logs with a retention policy and no delete permissions for the Lambda execution role) | R1, R2 | High |
| 8.2 | The following events must be audit-logged: user account creation, invite creation, invite deletion, RSVP status change, push token registration, push token deletion, account deletion | R1, R2 | High |
| 8.3 | Each audit log entry must include: event type, authenticated `uid`, resource ID, timestamp (UTC), and source IP address | R1, R2 | High |
| 8.4 | Audit log entries must not include raw token values, email addresses in plain text, or push tokens | I2, I4 | High |
| 8.5 | Application error logs must not include stack traces, database connection strings, or internal Lambda environment variables in any output visible outside the AWS account | I5 | Medium |

---

## 9. Secrets Management

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 9.1 | Database credentials, Firebase service account keys, Google Places API keys, and email service credentials must be stored in AWS Secrets Manager or AWS Parameter Store (SecureString); never hardcoded in Lambda source code or environment variables in plaintext | I6 | Critical |
| 9.2 | Firebase service account JSON must not be committed to source control; use `.gitignore` and secret scanning in CI/CD to enforce this | I6 | Critical |
| 9.3 | Lambda execution IAM role must follow least privilege: only the specific Secrets Manager ARNs, DynamoDB/RDS resources, and CloudWatch log groups it needs | E2 | High |
| 9.4 | Secret scanning (e.g., using `git-secrets`, `trufflehog`, or a GitHub Advanced Security secret scanning policy) must run on every pull request to detect accidentally committed credentials | I6 | High |
| 9.5 | API keys and database credentials must have a defined rotation schedule: high-risk secrets annually minimum, or immediately on suspected exposure | I6 | Medium |

---

## 10. Error Handling

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 10.1 | All API error responses must use a generic, consistent format: `{"error": "message"}` without internal details, stack traces, or system paths | I5 | High |
| 10.2 | Lambda handlers must catch all unhandled exceptions and return a `500 Internal Server Error` with a generic body; full exception detail must be written to CloudWatch Logs only | I5 | High |
| 10.3 | Error messages must not indicate whether a user ID, invite ID, or email address exists when returning `404` or `403` responses (to prevent user enumeration) | I1 | Medium |

---

## 11. GDPR & Privacy Considerations

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 11.1 | Users must be able to delete their account and all associated data (profile, invites, RSVPs, push tokens, audit log references) via a self-service API endpoint | I1, I2 | High |
| 11.2 | On account deletion: revoke Firebase refresh tokens (Firebase Admin SDK), delete all database records for the user, delete all push tokens, and confirm to the user via email | I2, I4 | High |
| 11.3 | Data retention must be defined and enforced: invite and RSVP records older than the 4-week display window may be purged after a defined retention period (e.g., 90 days) to minimize data held | I1 | Medium |
| 11.4 | A privacy policy accessible from within the app must describe: what data is collected (email, city, invite/RSVP data, push token), how it is used, and how users can request deletion | - | High |
| 11.5 | Email addresses must only be used for the purposes disclosed in the privacy policy (transactional notifications); do not use them for marketing without explicit opt-in | I2 | High |
| 11.6 | If the app is made available to users in the EU or California, a legal review of GDPR and CCPA obligations must be completed before launch | - | High |

---

## 12. Dependency & Supply Chain Security

| # | Requirement | Threat Ref | Priority |
|---|-------------|------------|----------|
| 12.1 | Run `gradle dependencyCheckAnalyze` (OWASP Dependency Check) or equivalent in the CI/CD pipeline on every build to detect known vulnerabilities in Java dependencies | - | High |
| 12.2 | Run `pod audit` or `swift package audit` for iOS CocoaPods / Swift Package Manager dependencies in CI/CD | - | Medium |
| 12.3 | Critical and high severity dependency vulnerabilities must block the build / deployment pipeline | - | High |
| 12.4 | Pin dependency versions in `build.gradle` and `Package.swift` / `Podfile.lock`; avoid wildcard version ranges | - | Medium |

---

## Pre-Launch Security Gate

The following items are non-negotiable before the app goes to production or public TestFlight:

- [ ] 2.1, 2.2, 2.3 - Firebase token validation on all endpoints
- [ ] 4.1, 4.2, 4.3 - Resource ownership checks on all reads and writes
- [ ] 4.4 - UUID resource IDs
- [ ] 1.1 - HTTPS only on API Gateway
- [ ] 3.1, 3.2 - iOS Keychain storage enforced
- [ ] 9.1, 9.2 - No secrets in source code or environment variables in plain text
- [ ] 5.2 - Parameterized queries / ORM in use
- [ ] 7.3, 7.4 - Push tokens excluded from API responses and logs
- [ ] 11.1, 11.2 - Account deletion flow implemented end-to-end
- [ ] 11.4 - Privacy policy accessible in-app
- [ ] 12.1, 12.3 - Dependency vulnerability scan passing in CI/CD
