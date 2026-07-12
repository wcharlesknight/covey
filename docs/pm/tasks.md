# Active Task Tracker

## Current Priority Tasks

---

### Task: Debug Smoke Test Failures and Add CI Artifact Uploads (WBS 1.3.9)

**Task ID:** 1.3.9-debug-smoke-tests-001  
**Priority:** 🔴 HIGH  
**Status:** 📋 BACKLOG (Ready to move to `in_progress`)  
**Assigned To:** Charlie Knight  
**Created:** 2026-07-12  
**Target Completion:** 2026-07-14  

---

## Summary

After PR #27 (smoke test HTTP request fixes), 5 of 7 smoke tests are still failing in CI/CD pipeline. Tests are failing due to HTTP status code mismatches, but detailed error information is being discarded in ephemeral CI runner directories, preventing root cause analysis.

**Blocking Issue:** Test reports generated in `/home/runner/work/covey/covey/backend/build/reports/` are not uploaded as GitHub Actions artifacts, making debugging impossible.

---

## Failing Tests (5 of 7)

All tests are in: `backend/src/smokeTest/java/com/covey/smoke/LambdaDeploymentSmokeTest.java`

| Test Name | Line | Expected | Issue | Status |
|-----------|------|----------|-------|--------|
| `testLambdaIsRespondingToAuthRequest` | 24-42 | HTTP 200 | Auth request returns non-200 | 🔴 FAILING |
| `testUserEndpointRouting` | 62-76 | HTTP 200 | GET /me returns non-200 | 🔴 FAILING |
| `testRsvpEndpointRouting` | 78-93 | HTTP 200 | POST /invites/{id}/rsvp returns non-200 | 🔴 FAILING |
| `testPushTokenEndpointRouting` | 95-110 | HTTP 201 | POST /push-tokens returns non-201 | 🔴 FAILING |
| `testWeeklyJobReturns200` | 112-122 | HTTP 200 | POST /weekly-job returns non-200 | 🔴 FAILING |

**Tests Passing:** 5/10
- `testLambdaJavaCodeIsExecuting` ✅ (line 44-59)
- `testWeeklyJobResponseBodyContainsCompletedMessage` ✅ (line 124-147)
- `testWeeklyJobResponseIsValidJson` ✅ (line 149-169)
- `testInvalidPathReturns404` ✅ (line 171-181)

---

## Root Issues to Investigate

1. **HTTP status code mismatches** - Assertions expecting specific codes (200, 201) but receiving different codes
2. **Possible API response format issues** - Response body/structure may not match test expectations
3. **Auth token/header handling** - Test payload headers may be malformed or missing
4. **Lambda endpoint responses** - Actual API responses not matching expected codes
5. **PR #27 incompleteness** - HTTP request fixes in PR #27 may have been partial or introduced new issues

---

## Action Items

### Phase 1: Improve CI Observability (Enable Debugging)

- [ ] **1.1** Add `actions/upload-artifact` step to `.github/workflows/deploy-nonprod.yml`
  - **Location:** Add after the "Run smoke tests" step (after line 116)
  - **Artifact name:** `smoke-test-reports`
  - **Paths to capture:**
    - `backend/build/reports/tests/smokeTest/` (HTML test report)
    - `backend/build/test-results/smokeTest/` (XML test results)
  - **Configuration:** Always upload, even if tests fail
  - **YAML snippet to add:**
    ```yaml
    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: smoke-test-reports
        path: |
          backend/build/reports/tests/smokeTest/
          backend/build/test-results/smokeTest/
        retention-days: 30
    ```

- [ ] **1.2** Re-run CI pipeline after artifact upload step is added
  - Trigger workflow manually or commit a dummy change
  - Verify test reports appear in GitHub Actions artifacts tab

- [ ] **1.3** Download test report HTML and analyze detailed failure messages
  - Extract assertion error details
  - Identify actual HTTP status codes being returned
  - Note any request/response differences

### Phase 2: Root Cause Analysis (Debug Tests)

- [ ] **2.1** Review each failing test's logic vs actual API response
  - `testLambdaIsRespondingToAuthRequest` — Check auth token format in request header
  - `testUserEndpointRouting` — Verify `/me` endpoint auth and response structure
  - `testRsvpEndpointRouting` — Verify `/invites/{id}/rsvp` endpoint routing
  - `testPushTokenEndpointRouting` — Verify `/push-tokens` endpoint and expected 201 response
  - `testWeeklyJobEndpointRouting` — Verify `/weekly-job` endpoint routing

- [ ] **2.2** Compare PR #27 changes against current test failures
  - Review commit: HTTP request fixes
  - Identify if fixes were incomplete or if new issues introduced
  - Check if test payloads need updates to match new API behavior

- [ ] **2.3** Run local smoke tests to confirm CI behavior matches local
  ```bash
  cd backend
  ./gradlew smokeTest -i
  ```

### Phase 3: Fix Root Cause

- [ ] **3.1** Apply fixes identified in Phase 2
  - Update test assertions if API behavior is correct
  - Fix API responses if test expectations are correct
  - Fix test payloads (headers, body, auth tokens) if malformed

- [ ] **3.2** Verify all 5 failing tests now pass locally
  ```bash
  ./gradlew smokeTest
  ```

- [ ] **3.3** Commit fixes to feature branch
  - Branch: Create new feature branch from main for fixes
  - Commit message: Reference WBS 1.3.9, link to this task

### Phase 4: Validation in CI/CD

- [ ] **4.1** Push feature branch and create PR
  - Trigger GitHub Actions workflow
  - Monitor smoke test execution in Actions tab

- [ ] **4.2** Verify all 7 smoke tests pass in CI/CD
  - No test failures
  - Artifact reports available for review

- [ ] **4.3** Merge PR to main

---

## Related Files

| File | Purpose | Status |
|------|---------|--------|
| `backend/src/smokeTest/java/com/covey/smoke/LambdaDeploymentSmokeTest.java` | Smoke tests | 🔴 5/7 failing |
| `.github/workflows/deploy-nonprod.yml` | CI/CD pipeline | ⚠️ Missing artifact upload |
| `backend/build/reports/tests/smokeTest/index.html` | Test report (ephemeral) | ⚠️ Not captured |
| `docs/pm/wbs.md` (1.3.9) | WBS item | 📋 Backlog |
| `docs/pm/completion-tracker.md` (1.3.9) | Progress tracking | 🔄 In progress |

---

## Context & Dependencies

**WBS Item:** 1.3.9 - Smoke Test Local Test Runner  
**Original Scope:** Create local test runner script + fix failing assertions  
**Current Blocker:** Can't debug failures without test report artifacts

**Previous Work (PR #27):**
- Made HTTP request fixes to smoke test infrastructure
- 5 tests still failing after fixes
- Need deeper investigation

**Dependencies:**
- AWS Lambda deployed (backend/build/reports requires Lambda to be running)
- CI/CD pipeline must complete smoke test step

---

## Success Criteria

✅ All 7 smoke tests pass in CI/CD  
✅ Test report artifacts uploaded and accessible  
✅ No manual Lambda debugging needed for future test failures  
✅ Local `./gradlew smokeTest` passes  
✅ WBS 1.3.9 marked complete in completion-tracker.md  

---

## Notes & Observations

- Tests are well-structured; likely issue is minor (header/payload/response mismatch)
- Artifact upload step is simple addition to `.deploy-nonprod.yml` (2-3 lines)
- Once artifacts enabled, debugging becomes straightforward
- This is a prerequisite for WBS 1.3.6 (Weekly Job Lambda) — can't proceed until tests pass

---

## How to Resume

1. Read this task file to understand current status
2. Check GitHub Actions for existing test report artifacts
3. If artifacts exist, download and analyze; otherwise add artifact upload step
4. Follow Action Items in phase order
5. Update status column as items complete
6. Mark task complete when all success criteria met

---

## Session Log

| Date | Session | Status | Notes |
|------|---------|--------|-------|
| 2026-07-12 | Task Creation | 📋 Created | Task defined, ready for implementation |
