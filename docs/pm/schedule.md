# Project Schedule: Location Notifier MVP

**Start Date**: 2026-06-27 (Week 1)
**Target Launch Date**: 2026-10-03 (end of Week 14)
**Basis**: Solo engineer at approximately 15-20 hours per week

---

## Milestone Summary

| Milestone | Target Date | Week | Key Deliverables |
|---|---|---|---|
| M0: Kickoff Complete | 2026-07-04 | End of Week 1 | Charter, WBS, Schedule, Risk Register; dev environment ready; Firebase + AWS configured |
| M1: Backend Feature-Complete | 2026-08-01 | End of Week 5 | All 5 API endpoints live; weekly job tested end-to-end; backend tests passing |
| M2: iOS Feature-Complete | 2026-09-05 | End of Week 10 | All screens built; push notifications working; iOS tests passing; wired to backend |
| M3: Integration + Security Review | 2026-09-19 | End of Week 12 | Full E2E flows verified; security review complete; performance validated |
| M4: Beta (TestFlight) | 2026-09-26 | End of Week 13 | Beta deployed; critical beta feedback resolved |
| M5: App Store Launch | 2026-10-03 | End of Week 14 | App Store submission approved; public launch |

---

## Phase 1: Setup and Infrastructure (Weeks 1-2)

**Target**: 2026-06-27 to 2026-07-11
**Estimated effort**: 28 hours

| Task | WBS | Duration | Start | End |
|---|---|---|---|---|
| Project planning (charter, WBS, schedule, risk register) | 1.1 | 3 days | 2026-06-27 | 2026-06-29 |
| Firebase project setup (Auth, Firestore, FCM, Crashlytics) | 1.2.1 | 3 days | 2026-06-29 | 2026-07-01 |
| AWS setup (Lambda, IAM, EventBridge, CloudWatch, Secrets Manager) | 1.2.2 | 3 days | 2026-07-01 | 2026-07-03 |
| Google Places API provisioning and connectivity test | 1.2.3 | 1 day | 2026-07-03 | 2026-07-04 |
| Monorepo structure, CI pipeline, local dev environment | 1.2.4 | 3 days | 2026-07-07 | 2026-07-09 |
| Validation and documentation of environment setup | 1.2.4.6 | 1 day | 2026-07-09 | 2026-07-11 |

**Exit criteria**: All three environments (Firebase dev, AWS dev, local) functional. A "hello world" Lambda function deploys and invokes successfully. Firestore emulator runs locally.

---

## Phase 2: Backend Implementation (Weeks 3-5)

**Target**: 2026-07-12 to 2026-08-01
**Estimated effort**: 78 hours

| Task | WBS | Duration | Start | End | Dependencies |
|---|---|---|---|---|---|
| Auth layer (Firebase token verification middleware) | 1.3.1 | 2 days | 2026-07-12 | 2026-07-13 | Phase 1 complete |
| User API (GET /me, PATCH /me) | 1.3.2 | 2 days | 2026-07-14 | 2026-07-15 | Auth layer |
| Feed API (GET /me/feed) | 1.3.3 | 3 days | 2026-07-16 | 2026-07-18 | User API |
| RSVP API (POST /invites/{id}/rsvp) | 1.3.4 | 2 days | 2026-07-19 | 2026-07-20 | Feed API |
| Push Token API (POST /push-tokens) | 1.3.5 | 1 day | 2026-07-21 | 2026-07-21 | Auth layer |
| Weekly Job Lambda - spot selection and Firestore writes | 1.3.6.1-1.3.6.5 | 4 days | 2026-07-22 | 2026-07-25 | Feed API |
| Weekly Job Lambda - push + email notification dispatch | 1.3.6.6-1.3.6.8 | 3 days | 2026-07-26 | 2026-07-28 | Push Token API, spot selection |
| Backend testing (unit + integration) | 1.3.7 | 2 days | 2026-07-29 | 2026-07-30 | All backend features |
| Backend deployment to AWS + validation | 1.3.8 | 2 days | 2026-07-31 | 2026-08-01 | Backend testing |

**Exit criteria (M1)**: All API endpoints return correct responses against live Firebase dev project. Weekly job runs manually and produces WeeklySpot, Invites, push notifications, and emails. 80%+ unit test coverage. API Gateway (or Function URLs) endpoint URLs available for iOS team.

**Note on parallelization**: iOS development (Phase 3) can begin in parallel from Week 4 (2026-07-22) once the Auth layer and User API are stable. iOS sign-in and profile screens do not depend on the feed or weekly job.

---

## Phase 3: iOS Implementation (Weeks 4-10)

**Target**: 2026-07-22 to 2026-09-05
**Estimated effort**: 74 hours

Phase 3 starts in Week 4, overlapping with the latter half of Phase 2. The first two tasks (sign-in, city selection) can begin as soon as the auth layer and User API are deployed. Feed, RSVP, and push notification work begins once those backend endpoints are stable (Week 6+).

| Task | WBS | Duration | Start | End | Dependency |
|---|---|---|---|---|---|
| Sign-in screens (Apple + Google, Keychain) | 1.4.1 | 4 days | 2026-07-22 | 2026-07-25 | Auth layer live |
| City selection screen + PATCH /me integration | 1.4.2 | 1 day | 2026-07-26 | 2026-07-26 | User API live |
| Home feed screen (layout, GET /me/feed integration) | 1.4.3 | 4 days | 2026-08-04 | 2026-08-07 | Feed API live (Week 6) |
| RSVP UI (yes/no/interested, POST /invites/{id}/rsvp) | 1.4.4 | 2 days | 2026-08-08 | 2026-08-09 | RSVP API live |
| Push notification handling (FCM token, deep link) | 1.4.5 | 3 days | 2026-08-10 | 2026-08-12 | Push Token API live, APNs configured |
| iOS unit + UI testing | 1.4.6 | 3 days | 2026-08-25 | 2026-08-27 | All screens complete |
| App Store submission preparation (screenshots, metadata, manifest) | 1.4.7 | 2 days | 2026-09-01 | 2026-09-02 | iOS testing complete |
| TestFlight build upload | 1.4.7.6 | 1 day | 2026-09-03 | 2026-09-03 | App Store prep |
| Buffer / polish / bug fixes | - | 3 days | 2026-09-03 | 2026-09-05 | All above |

**Exit criteria (M2)**: Complete iOS app runs on a physical device. All screens functional. Push notifications received and deep linking works. Unit and UI tests passing. Build uploaded to TestFlight.

---

## Phase 4: Integration and Testing (Weeks 11-12)

**Target**: 2026-09-06 to 2026-09-19
**Estimated effort**: 20 hours

| Task | WBS | Duration | Start | End |
|---|---|---|---|---|
| End-to-end user flow testing (all 7 E2E scenarios) | 1.5.1 | 3 days | 2026-09-07 | 2026-09-09 |
| Weekly job end-to-end testing (7 scenarios) | 1.5.2 | 2 days | 2026-09-10 | 2026-09-11 |
| Security review (Firestore rules, API auth, binary scan) | 1.5.3 | 2 days | 2026-09-12 | 2026-09-13 |
| Performance testing (Lambda execution time, API response time) | 1.5.4 | 1 day | 2026-09-14 | 2026-09-14 |
| Bug fixes from integration and security findings | - | 3 days | 2026-09-15 | 2026-09-17 |
| Final regression pass | - | 1 day | 2026-09-18 | 2026-09-18 |

**Exit criteria (M3)**: All E2E scenarios pass. No open security findings of medium or higher severity. Lambda weekly job completes in under 60 seconds for 100 users. GET /me/feed p95 under 500ms.

---

## Phase 5: Beta and Launch (Weeks 13-14)

**Target**: 2026-09-20 to 2026-10-03
**Estimated effort**: 12 hours (plus async response time for App Store review)

| Task | WBS | Duration | Start | End |
|---|---|---|---|---|
| Distribute to external beta testers via TestFlight | 1.6.1.1 | 1 day | 2026-09-20 | 2026-09-20 |
| Beta feedback period | 1.6.1.2 | 5 days | 2026-09-21 | 2026-09-25 |
| Triage and fix critical beta issues | 1.6.1.3 | 2 days | 2026-09-24 | 2026-09-25 |
| Final pre-submission checklist | 1.6.2.1 | 1 day | 2026-09-26 | 2026-09-26 |
| App Store submission | 1.6.2.2 | 1 day | 2026-09-27 | 2026-09-27 |
| App Store review (Apple SLA: typically 1-3 days, allow up to 7) | 1.6.2.3 | 1-7 days | 2026-09-28 | 2026-10-04 |
| Launch and post-launch monitoring | 1.6.3 | Ongoing | 2026-10-03 | - |

**Exit criteria (M5)**: App approved and live on App Store. First weekly job runs successfully in production. Crash-free session rate 98%+ in first 48 hours.

---

## Critical Path

The longest sequence of dependent tasks determines the minimum project duration:

1. Environment setup (Firebase + AWS) - 2 weeks
2. Backend auth layer + User API - 4 days
3. Backend Feed API + RSVP API + Weekly Job - 2 weeks
4. Backend deployment and validation - 2 days
5. iOS feed screen + RSVP UI + push notifications - 2 weeks
6. iOS testing + App Store preparation - 1 week
7. Integration and E2E testing - 2 weeks
8. Beta + App Store review + launch - 2 weeks

**Total critical path: approximately 12-14 weeks**

The weekly job Lambda (1.3.6) is the single highest-complexity backend task and is on the critical path for E2E testing. Any delays here push out the integration phase and TestFlight distribution.

App Store review is an external dependency with variable duration (typically 24-72 hours but can take up to 7 days if questions are raised). One week of buffer is built into Phase 5 to absorb this uncertainty.

---

## Schedule Risk and Buffer

| Risk | Buffer Built In |
|---|---|
| App Store review delay (up to 7 days) | Phase 5 runs 2 weeks; review can take the full week |
| Weekly job complexity overrun | 3-day polish buffer at end of Phase 3 |
| Integration testing finding critical bugs | 3 days of bug fix time built into Phase 4 |
| Beta revealing critical issues | 2 days of critical fix time in Phase 5 |

---

## Dependency Map

```
Phase 1 (Setup)
    └── Phase 2 (Backend) - starts Week 3
            ├── Auth + User API (Weeks 3-4)
            │       └── Phase 3 iOS Sign-In (starts Week 4, parallel)
            └── Feed + RSVP + Weekly Job (Weeks 4-5)
                    └── Phase 3 iOS Feed + RSVP + Push (starts Week 6)
                            └── Phase 4 Integration Testing (Weeks 11-12)
                                    └── Phase 5 Beta + Launch (Weeks 13-14)
```
