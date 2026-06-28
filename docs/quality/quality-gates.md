# Quality Gates: Location Notifier Social Gathering App

**Version**: 1.0
**Date**: 2026-06-27
**Project**: Location Notifier — iOS app + Java Lambda backend
**Quality Model Reference**: `docs/quality/quality-model.md`
**Test Strategy Reference**: `docs/test/test-strategy.md`

---

## Overview

Quality gates are binary pass/fail checkpoints enforced at defined points in the development lifecycle. A gate failure blocks progression. There is no "mostly passing." Each gate below specifies what is checked, how it is checked, and what the failure action is.

---

## Gate 1: Commit Gate (every pull request / merge to main)

**Trigger**: Every push that targets the `main` branch (enforced via CI branch protection rule).

**Automated checks (must all pass — zero tolerance for failures)**:

### 1.1 Build Passes

- Java Lambda: `mvn clean compile` exits 0
- iOS: `xcodebuild build -scheme LocationNotifier` exits 0
- Failure action: block merge; fix compilation error

### 1.2 All Tests Pass

- Java: `mvn test` — all JUnit 5 unit tests pass
- Java: `mvn verify -Pintegration` (Firestore emulator) — all integration tests pass
- iOS: `xcodebuild test -scheme LocationNotifier -destination "platform=iOS Simulator,name=iPhone 15"` — all XCTest and XCUITest tests pass
- Failure action: block merge; fix failing tests before re-pushing

### 1.3 Code Coverage Thresholds

| Layer | Metric | Threshold | Tool |
|-------|--------|-----------|------|
| Java Lambda | Line coverage | >= 80% | JaCoCo (`jacoco:check` goal) |
| Java Lambda | Branch coverage | >= 70% | JaCoCo |
| iOS Swift | Line coverage | >= 70% | Xcode coverage report via `xcov` |

- Failure action: block merge; write missing tests
- Exclusions: auto-generated code, `**/test/**`, `**/mocks/**`, configuration classes with no logic

### 1.4 Zero Lint Errors

- Java: `mvn checkstyle:check` — Checkstyle configuration at `checkstyle.xml`; zero violations at ERROR severity
- Swift: `swiftlint lint --strict` — zero errors (warnings are allowed but must not increase week-over-week)
- Failure action: block merge; fix lint violations

### 1.5 Zero Compilation Errors and Type Errors

- Java: enforced by 1.1 (compilation fails equals build fails)
- Swift: `xcodebuild build` with `-warnConcurrency` and `-strictConcurrency complete` compiler flags; zero errors
- Failure action: block merge

### 1.6 No New Critical or High Dependency Vulnerabilities

- Java: `mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7` — fails build if any dependency has CVSS score >= 7.0 (High or Critical)
- Swift: `swift package audit` — fails if any dependency has a known Critical or High vulnerability
- Failure action: block merge; update or replace the vulnerable dependency
- Exception process: if no fix is available, document the exception in `docs/security/security-checklist.md` with a mitigation and a remediation deadline; then override the gate manually for that specific CVE only

### 1.7 OpenAPI Contract Validation

- For any change to a Lambda handler file or the OpenAPI spec: `openapi-generator validate -i docs/arch/api/openapi.yaml` — spec must be valid
- Failure action: block merge; fix the spec or the handler

### 1.8 Cyclomatic Complexity

- Java: Checkstyle `CyclomaticComplexity` rule with `max=10`; any method exceeding 10 fails the Checkstyle check (covered by 1.4)
- Swift: SwiftLint `cyclomatic_complexity` rule with `error` threshold at 10 (covered by 1.4)
- Failure action: block merge; refactor the offending method

### 1.9 No Secrets Committed

- CI runs `gitleaks detect --source . --no-git` on every push
- Failure action: block merge; remove the secret; rotate the credential immediately

---

## Gate 2: Milestone M1 Gate — Backend Feature-Complete (Target: Week 5)

**Trigger**: Engineer declares backend feature-complete; this gate is run manually and results are recorded in the project milestone log.

**All Commit Gate checks must also pass.** In addition:

### 2.1 All Use Cases UC-001 through UC-010 Have Integration Tests

- Verify that an integration test file exists for each use case
- Each integration test covers: main success scenario + at least one critical extension path
- Checklist: manually reviewed against `docs/req/use-cases/use-cases.md`
- Failure action: write missing tests; do not advance milestone

### 2.2 Lambda Coverage Elevated

| Metric | Threshold |
|--------|-----------|
| Line coverage | >= 85% |
| Branch coverage | >= 75% |

- Tool: JaCoCo report at end of `mvn verify` full run (unit + integration)
- Failure action: write missing tests; re-run gate

### 2.3 Security Tests SEC-001 through SEC-007 Passing

- Run the authentication and authorization integration tests from `docs/test/test-strategy.md`
- All seven tests must return the expected HTTP status code
- Failure action: fix the security control; re-run gate

### 2.4 Weekly Lambda Job End-to-End Run (Staging)

- Manually trigger the WeeklySpotLambda in the staging AWS environment
- Verify: `weekly_spots` document written for `seattle` and `tacoma` with current `weekId`
- Verify: CloudWatch Logs show no ERROR entries
- Verify: WireMock / staging APNs endpoint received the push payload
- Failure action: fix the job; re-trigger; document in CloudWatch

### 2.5 API Response Contract Verification

- Run `newman run` against the OpenAPI-generated Postman collection, targeting the staging API Gateway endpoint
- All included examples from `openapi.yaml` must return 2xx with schema-conformant responses
- Failure action: fix the handler; re-run collection

### 2.6 Performance Baseline (Staging)

- Use `hey` or `k6` to send 50 concurrent requests to each of the three highest-traffic endpoints:
  - `GET /v1/feed/current-spot`
  - `POST /v1/invites/{id}/rsvp`
  - `POST /v1/auth/apple-signin`
- p95 latency must meet thresholds in `docs/quality/quality-model.md`
- Failure action: investigate Lambda cold start, Firestore query, or Firebase token verify latency; resolve before proceeding

---

## Gate 3: Milestone M2 Gate — iOS Feature-Complete (Target: Week 10)

**Trigger**: Engineer declares iOS feature-complete.

**All Commit Gate checks must also pass.** In addition:

### 3.1 Five Critical UI Test Flows Automated and Passing

From `docs/test/test-strategy.md` Layer 3:

1. New user onboarding (UC-001 + UC-002)
2. Weekly spot view (UC-004)
3. RSVP / checkin flow (UC-007)
4. History view (UC-005)
5. Error state: no spot yet

All five must pass on the iPhone 15 simulator and on at least one physical device.

### 3.2 iOS Coverage Elevated

| Metric | Threshold |
|--------|-----------|
| iOS line coverage | >= 75% |

- Tool: `xcov` report after full test run
- Failure action: write missing tests

### 3.3 Crashlytics Integration Verified

- Firebase Crashlytics SDK integrated and reporting to the staging Firebase project
- Trigger a deliberate test crash via `Crashlytics.crashlytics().crash()` in a debug build
- Verify the crash appears in Firebase console within 5 minutes
- Failure action: fix SDK integration

### 3.4 Accessibility Spot Check

- Run Xcode Accessibility Inspector on the Feed screen, Spot Detail screen, and RSVP flow
- Zero critical accessibility violations (missing labels, contrast failures)
- Minimum touch target size 44 x 44 pt on all interactive elements
- Failure action: fix violations; re-inspect

---

## Gate 4: Milestone M3 Gate — Integration and Security Review (Target: Week 12)

**Trigger**: All feature work is complete; pre-beta hardening.

**All prior gate checks must pass.** In addition:

### 4.1 Full Security Test Suite Passing

All SEC-001 through SEC-011 tests from `docs/test/test-strategy.md` must pass.

- Failure action: fix the security control; re-run the full suite

### 4.2 All Threat Model Critical Items Verified

From `docs/security/threat-model.md` Risk Prioritization — "Critical — Address Before Launch":

| Item | Verification |
|------|-------------|
| S2: Forged JWT | SEC-001, SEC-002 passing |
| T1: Invite ownership tampering | SEC-003 passing |
| E1 / E3: IDOR + privilege escalation | SEC-004, SEC-005 passing |
| I1: Unauthorized invite history access | SEC-005 passing |
| I6: Database breach | Firestore security rules reviewed; no public endpoint confirmed in AWS console |

- Failure action: fix the control; re-verify; document in `docs/security/security-checklist.md`

### 4.3 OWASP Mobile Top 10 Checklist

Complete the OWASP Mobile Top 10 checklist manually for the iOS app and Lambda backend. Document results in `docs/security/security-checklist.md`. Zero High or Critical findings may remain open.

### 4.4 End-to-End Flow Validated in Staging

Run the full user flow with a real iOS device against the staging environment:

1. Sign in with Apple (real Apple credentials on TestFlight build)
2. Grant location permission, city detected
3. Register push token, token written to Firestore
4. Manually trigger weekly job, spot appears in app
5. RSVP to spot, count updates in real-time
6. View 4-week history, data displayed correctly

All six steps must succeed without developer intervention.

### 4.5 Weekly Job Two-Consecutive-Week Run

The weekly Lambda job must have run successfully in the staging environment for two consecutive scheduled runs (two Thursdays) without manual intervention.

- Failure action: investigate and fix; restart the two-week clock

### 4.6 Performance Final Verification

Repeat the performance baseline from Gate 2.6 against the final staging build. All p95 thresholds from `docs/quality/quality-model.md` must be met.

---

## Gate 5: Beta / TestFlight Gate (Target: Week 13)

**Trigger**: App is submitted to TestFlight and has collected at least 100 sessions from beta users.

### 5.1 Zero Open Critical or High Defects

- Review all feedback collected from TestFlight
- No defect at Critical or High severity may be open
- Failure action: issue a new TestFlight build within 48 hours; do not submit to App Store until passing

### 5.2 Crash-Free Session Rate

- Firebase Crashlytics: crash-free session rate >= 98% over the most recent 100 sessions
- Failure action: investigate crashes; fix; release updated build; restart measurement

### 5.3 Core Flow Completion Rate

- Analytics (Firebase Analytics or manual logging): at least 80% of beta users who opened the app completed the sign-in → spot view → RSVP flow
- Failure action: investigate drop-off point; fix UX or performance issue

### 5.4 Weekly Job Reliability

- At least one weekly job run has occurred during the beta period without manual intervention
- CloudWatch Logs confirm success for all active cities
- Failure action: fix and verify before proceeding to App Store submission

---

## Gate 6: App Store Submission Gate (Target: Week 14)

**Trigger**: Final submission to App Store Connect.

**All prior gate checks must pass.** In addition:

### 6.1 App Store Submission Checklist

- [ ] App Privacy disclosure completed in App Store Connect (location, email, push token data usage declared)
- [ ] Content rating questionnaire completed
- [ ] App screenshots and preview provided for all required device sizes
- [ ] App description, keywords, and category filled
- [ ] Support URL and privacy policy URL provided
- [ ] Build is signed with distribution certificate and provisioning profile (not development)
- [ ] NSLocationWhenInUseUsageDescription and NSLocationAlwaysAndWhenInUseUsageDescription strings present and accurate
- [ ] App does not use any private or restricted APIs (confirmed via App Store Connect pre-submission scan)

### 6.2 Rollback Plan Documented

- `docs/pm/` contains a rollback procedure: how to disable the Lambda via API Gateway stage variable, how to notify TestFlight users, how to revert to the previous Lambda JAR
- Rollback procedure tested once in staging
- Failure action: document and test before submitting

### 6.3 Zero New Findings Since Gate 5

- Re-run `mvn dependency-check` and `swift package audit`
- Zero new Critical or High findings
- Failure action: address finding before submission

---

## Gate Failure Escalation

Since this is a solo-engineer project, escalation follows a straightforward decision tree:

| Condition | Action |
|-----------|--------|
| Commit Gate failure | Fix immediately; do not merge until passing |
| Milestone Gate failure (non-security) | Log the gap in the project risk register; fix within 2 business days; re-run gate before advancing to next milestone |
| Milestone Gate failure (security, Critical or High) | Stop all feature work; fix the security issue; re-run the full security test suite; advance only after passing |
| Beta Gate failure (Critical defect or crash rate) | Issue a new TestFlight build within 48 hours; do not submit to App Store until passing |

---

## CI Configuration Reference

The following CI pipeline steps implement the Commit Gate (Gate 1) automatically:

```yaml
# .github/workflows/ci.yml (or equivalent)

name: CI

on:
  push:
    branches: [main, "feature/**", "fix/**"]
  pull_request:
    branches: [main]

jobs:
  lambda-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build and unit test
        run: mvn clean verify
      - name: Integration tests (Firestore emulator)
        run: |
          npm install -g firebase-tools
          firebase emulators:exec --only firestore "mvn verify -Pintegration"
      - name: OWASP dependency check
        run: mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7
      - name: Check coverage thresholds
        run: mvn jacoco:check
      - name: Secret scan
        uses: gitleaks/gitleaks-action@v2

  ios-quality:
    runs-on: macos-14
    steps:
      - uses: actions/checkout@v4
      - name: SwiftLint
        run: swiftlint lint --strict
      - name: Build and test
        run: |
          xcodebuild test \
            -scheme LocationNotifier \
            -destination "platform=iOS Simulator,name=iPhone 15" \
            -enableCodeCoverage YES
      - name: Coverage report
        run: xcov --scheme LocationNotifier --minimum_coverage_percentage 70

  contract-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate OpenAPI spec
        run: |
          npm install -g @apidevtools/swagger-cli
          swagger-cli validate docs/arch/api/openapi.yaml
```

---

## Quality Gate Status Tracking

Update this table after each gate run:

| Gate | Last Run | Status | Notes |
|------|----------|--------|-------|
| Gate 1: Commit Gate | Automated per commit | — | CI enforced |
| Gate 2: M1 Backend | — | Pending | Target: Week 5 |
| Gate 3: M2 iOS | — | Pending | Target: Week 10 |
| Gate 4: M3 Integration | — | Pending | Target: Week 12 |
| Gate 5: Beta | — | Pending | Target: Week 13 |
| Gate 6: App Store | — | Pending | Target: Week 14 |
