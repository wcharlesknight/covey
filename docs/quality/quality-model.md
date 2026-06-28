# Quality Model: Location Notifier Social Gathering App

**Version**: 1.0
**Date**: 2026-06-27
**Project**: Location Notifier — iOS app + Java Lambda backend
**Based on**: ISO/IEC 25010 Software Product Quality Model

---

## Quality Attributes

### 1. Functional Suitability

**Definition**: Degree to which the product provides functions that meet stated needs, as expressed in the confirmed use cases (UC-001 through UC-010).

**Sub-characteristics**:
- **Functional completeness**: All ten confirmed use cases are implemented and testable before TestFlight beta
- **Functional correctness**: Weekly spot selection, RSVP state transitions, and social signals produce correct results
- **Functional appropriateness**: The app reduces social coordination friction; features do not exceed MVP scope

**Measurement**:
- Use case coverage: 100% of UC-001 through UC-010 have at least one automated test asserting their main success scenario
- Defect density: fewer than 2 defects per 100 lines of Lambda handler code at each milestone
- User acceptance test pass rate: 100% of beta testers complete the core flow (sign-in → receive invite → RSVP) without abandonment due to a product defect

---

### 2. Performance Efficiency

**Definition**: Performance relative to resources used, under expected MVP load (100 active users, Seattle and Tacoma).

**Sub-characteristics**:
- **Time behavior**: API responses are fast enough that the iOS app does not show loading spinners for more than 2 seconds on a typical mobile connection
- **Resource utilization**: Lambda cold-start time does not cause visible latency on first open; Firestore read costs stay within the free Spark plan at MVP scale
- **Capacity**: Architecture handles at least 500 concurrent users without architectural changes (sufficient for MVP and near-term growth)

**Measurement**:

| Metric | Target | How Measured |
|--------|--------|-------------|
| GET /v1/feed/current-spot p95 latency | < 800 ms | Lambda CloudWatch duration metric |
| POST /v1/invites/{id}/rsvp p95 latency | < 600 ms | Lambda CloudWatch duration metric |
| POST /v1/auth/apple-signin p95 latency | < 1500 ms | Lambda CloudWatch duration metric (includes Firebase token verify round-trip) |
| Weekly Lambda job total duration | < 120 s | CloudWatch Logs timing |
| iOS app time-to-interactive (cold launch) | < 3 s on iPhone 12+ | Instruments / Xcode profiler |
| Firestore document reads per weekly job per city | < 500 reads | Firebase console usage |
| Lambda memory consumption | < 512 MB per invocation | CloudWatch MemoryUsed metric |

---

### 3. Compatibility

**Definition**: Degree to which the product exchanges information correctly with iOS, Firebase, Apple APNs, Google Places, and the AWS ecosystem.

**Measurement**:
- iOS version support: iOS 16.0 and above (covers 95%+ of active iPhone users as of 2026)
- iPhone model support: iPhone 12 and above (A14 chip minimum)
- API contract: OpenAPI 3.1 spec at `docs/arch/api/openapi.yaml` is the source of truth; all Lambda handler responses must validate against it
- Firebase SDK version: pinned major version; minor/patch updates applied within one sprint of release
- APNs compatibility: all push payloads conform to current APNs HTTP/2 API payload format

---

### 4. Usability

**Definition**: Degree to which the iOS app can be used by the target personas (Maya, James, Priya) to accomplish weekly spot discovery and RSVP without friction.

**Measurement**:

| Metric | Target |
|--------|--------|
| Task completion rate: sign-in to RSVP (new user) | > 90% (beta tester observation) |
| Time-on-task: receive notification → tap → RSVP | < 60 seconds |
| Crash-free session rate (Firebase Crashlytics) | >= 98% |
| Beta tester satisfaction score (1-5 scale) | >= 4.0 |
| Location permission grant rate on first prompt | >= 70% |

Accessibility target: WCAG 2.1 AA for all text elements, contrast ratios, and interactive targets (minimum 44 x 44 pt touch targets as per Apple HIG).

---

### 5. Reliability

**Definition**: Degree to which the system performs its specified functions under normal and failure conditions, with particular attention to the weekly Lambda job (UC-008) which is the core product behaviour.

**Sub-characteristics**:
- **Availability**: API Gateway endpoint is available to handle requests whenever iOS users open the app
- **Fault tolerance**: Weekly Lambda job continues processing remaining cities when one city fails; APNs invalid token errors do not halt the job
- **Recoverability**: If the weekly job fails mid-run, a manual re-trigger produces correct results without duplicate spots or duplicate notifications

**Measurement**:

| Metric | Target |
|--------|--------|
| Weekly Lambda job success rate | >= 99% of scheduled runs complete without manual intervention |
| API Gateway uptime | >= 99.5% (AWS SLA for API Gateway + Lambda) |
| Mean Time To Recovery (MTTR) on Lambda failure | < 30 minutes (CloudWatch alarm → developer alert → fix) |
| Push delivery success rate (valid tokens) | >= 97% |
| Duplicate spot prevention | 0 instances of the same venue selected in the same city within a 4-week rolling window |

---

### 6. Security

**Definition**: Degree to which the product protects user data (emails, push tokens, RSVP history, social signals) and prevents unauthorized access, as required by the threat model at `docs/security/threat-model.md`.

**Measurement**:

| Security Requirement | Target | Verification |
|---------------------|--------|-------------|
| SR-1: Firebase token validation on every endpoint | 100% of handler tests assert 401 on missing/invalid token | Integration tests |
| SR-2: Ownership checks on every resource | 0 endpoints that return or modify another user's invite or profile | Code review + penetration test |
| SR-3: Non-sequential UUIDs for all resource IDs | 100% of Firestore document IDs use deterministic or UUID v4 format | Static analysis of Firestore write paths |
| SR-4: Email addresses never returned cross-user | 0 API responses include another user's email address | Contract tests against OpenAPI spec |
| SR-5: HTTPS/TLS 1.2+ enforced | API Gateway configured TLS policy verified | AWS console check at each deployment |
| SR-6: Rate limiting on invite creation | <= 20 invites per user per hour enforced server-side | Load test + unit test |
| OWASP Mobile Top 10 | Zero High or Critical findings | Pre-launch review |
| Dependency vulnerabilities | Zero Critical, zero High in production dependencies | `mvn dependency-check` + `swift package audit` in CI |

---

### 7. Maintainability

**Definition**: Degree to which the Lambda backend (Java) and iOS app (Swift) can be modified efficiently as requirements evolve, especially given a solo-engineer team.

**Sub-characteristics**:
- **Modularity**: Lambda handlers are individually testable; iOS screens have no business logic in view code
- **Analysability**: A new engineer (or returning engineer after a break) can understand any module within one working session
- **Modifiability**: Adding a new city requires only a Firestore document write and no code change
- **Testability**: Every public method in the Lambda has a corresponding unit test; every Lambda handler has an integration test with a mock Firebase

**Measurement**:

| Metric | Target |
|--------|--------|
| Cyclomatic complexity per method | <= 10 (McCabe) |
| Nesting depth per method | <= 3 levels |
| Lines per method | <= 50 |
| Lines per file | <= 300 |
| Method parameters | <= 4 |
| Code duplication (jscpd / Simian) | < 5% across the codebase |
| Public API documentation | 100% of Lambda handler public methods have Javadoc; 100% of public Swift types have doc comments |
| TODO comments without a linked issue | 0 at merge time |

---

### 8. Portability

**Definition**: Degree to which the system can be deployed to different environments (dev, staging, production) reliably.

**Measurement**:
- Environment parity: dev, staging, and production all use the same Lambda JAR built by CI; differences are limited to environment variables
- Deployment repeatability: deploying from scratch using documented steps takes < 2 hours
- Firebase project isolation: separate Firebase projects for dev, staging, and production with no shared data

---

## Quality Gates

### Code Commit Gate (every pull request / merge to main)

All of the following must pass before code merges:

- [ ] All unit tests pass (Java: Maven Surefire; Swift: XCTest)
- [ ] Lambda line coverage >= 80%, branch coverage >= 70%
- [ ] iOS unit test line coverage >= 70%
- [ ] Zero lint errors (Java: Checkstyle; Swift: SwiftLint)
- [ ] Zero type errors / compilation errors
- [ ] No new Critical or High dependency vulnerabilities (`mvn dependency-check`)
- [ ] Code review approved by at least one reviewer (self-review acceptable for solo sprints, with a 24-hour cool-off period)
- [ ] All acceptance criteria in the linked use case are checked as verified

### Milestone Gate (end of M1 Backend, M2 iOS)

- [ ] All confirmed use cases (UC-001 through UC-010) have passing integration tests for their main success scenario and at least two extension paths
- [ ] Lambda line coverage >= 85%
- [ ] Zero open security findings rated High or Critical
- [ ] Weekly Lambda job runs end-to-end in a staging environment with synthetic users
- [ ] Performance targets met for all API endpoints (p95 < thresholds above)
- [ ] API responses validated against OpenAPI spec for all endpoints

### Release Gate (M3 Integration Review → M4 TestFlight → M5 App Store)

- [ ] All unit, integration, and UI tests pass in CI
- [ ] Lambda line coverage >= 85%, iOS coverage >= 75%
- [ ] Zero Critical and zero High security vulnerabilities
- [ ] Weekly Lambda job has run successfully for at least two consecutive weeks in staging
- [ ] Crash-free session rate >= 98% over at least 100 TestFlight sessions
- [ ] All STRIDE threats rated Critical or High have documented mitigations verified
- [ ] App Store submission checklist complete (App Privacy disclosure, content rating, screenshots)
- [ ] Rollback procedure documented and tested

---

## Quality Metric Thresholds Summary

| Metric | Commit Gate | Milestone Gate | Release Gate |
|--------|------------|---------------|-------------|
| Lambda line coverage | >= 80% | >= 85% | >= 85% |
| Lambda branch coverage | >= 70% | >= 75% | >= 75% |
| iOS unit test coverage | >= 70% | >= 75% | >= 75% |
| Cyclomatic complexity | <= 10 | <= 10 | <= 10 |
| Nesting depth | <= 3 | <= 3 | <= 3 |
| Critical dependency vulns | 0 | 0 | 0 |
| High dependency vulns | 0 | 0 | 0 |
| Lint errors | 0 | 0 | 0 |
| Open Critical/High security findings | Block merge | 0 | 0 |
| Weekly job success rate (staging) | N/A | >= 99% | >= 99% |
| API p95 latency | N/A | Met | Met |
