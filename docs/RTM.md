# Requirements Traceability Matrix (RTM) - Covey

**Traceability from requirements → design → implementation → testing**

---

## Overview

This RTM links each confirmed requirement to:
- **Design artifact** (use case, domain model, API endpoint, UI screen)
- **Implementation task** (WBS item from project plan)
- **Test case** (unit test, integration test, E2E test, or manual test)

All requirements are derived from confirmed scenarios (Maya, James, Priya) and 10 locked use cases.

---

## Requirements Dictionary

| ID | Requirement | Source | Priority | Status |
|----|-------------|--------|----------|--------|
| REQ-001 | User can sign in with Apple Sign-In | UC-001 | Critical | Confirmed |
| REQ-002 | User can sign in with Google Sign-In | UC-001 | Critical | Confirmed |
| REQ-003 | Session token stored securely in iOS Keychain | UC-001 | Critical | Confirmed |
| REQ-004 | User can select city from dropdown (Seattle, Tacoma) | UC-002 | Critical | Confirmed |
| REQ-005 | Selected city persisted on user profile | UC-002 | High | Confirmed |
| REQ-006 | User receives weekly push notification Thursday morning | UC-003 | Critical | Confirmed |
| REQ-007 | Push notification contains venue name and city | UC-003 | High | Confirmed |
| REQ-008 | Tapping push notification opens feed with current spot | UC-003 | Critical | Confirmed |
| REQ-009 | User can view this week's curated spot | UC-004 | Critical | Confirmed |
| REQ-010 | Spot shows: venue name, address, rating, hours, photo | UC-004 | Critical | Confirmed |
| REQ-011 | User can open venue in Apple Maps | UC-004 | High | Confirmed |
| REQ-012 | User can view 4 weeks of past venue history | UC-005 | Critical | Confirmed |
| REQ-013 | History shows venue name, date, RSVP counts | UC-005 | High | Confirmed |
| REQ-014 | User can see total invite count for current spot | UC-006 | High | Confirmed |
| REQ-015 | User can see RSVP counts (yes/interested/no) | UC-006 | High | Confirmed |
| REQ-016 | User can RSVP "Yes" to a spot | UC-007 | Critical | Confirmed |
| REQ-017 | User can RSVP "Interested" (Maybe) to a spot | UC-007 | Critical | Confirmed |
| REQ-018 | User can RSVP "No" to a spot | UC-007 | Critical | Confirmed |
| REQ-019 | RSVP status saved and persisted | UC-007 | Critical | Confirmed |
| REQ-020 | RSVP counts update live after user responds | UC-007 | High | Confirmed |
| REQ-021 | Backend selects one venue per city per week | UC-008 | Critical | Confirmed |
| REQ-022 | Venue selected from Google Places API results | UC-008 | Critical | Confirmed |
| REQ-023 | Excluded venues (4-week history) not re-selected | UC-008 | Critical | Confirmed |
| REQ-024 | Curation rules applied (rating ≥ 4.0, reviews ≥ 50) | UC-008 | High | Confirmed |
| REQ-025 | Invites created for all users in city | UC-008 | Critical | Confirmed |
| REQ-026 | Push notifications sent to all registered devices | UC-008 | Critical | Confirmed |
| REQ-027 | Email notifications sent to verified email addresses | UC-008 | High | Confirmed |
| REQ-028 | Weekly job retry logic on failure | UC-008 | High | Confirmed |
| REQ-029 | Backend receives location updates from iOS app | UC-009 | High | Confirmed |
| REQ-030 | User's city determined from location | UC-009 | High | Confirmed |
| REQ-031 | 4-week history fetched and returned for feed | UC-010 | Critical | Confirmed |
| REQ-032 | RSVP counts calculated and returned with history | UC-010 | High | Confirmed |

---

## Traceability Links

### Sign-In & Authentication (REQ-001 to REQ-003)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-001: Apple Sign-In | UC-001, Sign-In Screen (UI prototype) | 1.4.1.1 - 1.4.1.3 | test_AppleSignInFlow, test_TokenValidation |
| REQ-002: Google Sign-In | UC-001, Sign-In Screen (UI prototype) | 1.4.1.1 - 1.4.1.3 | test_GoogleSignInFlow, test_TokenValidation |
| REQ-003: Keychain storage | Auth Design doc, Security Checklist SR-3 | 1.4.1.5 - 1.4.1.8 | test_KeychainStorage, test_TokenExpiry |

### City Selection & Management (REQ-004 to REQ-005)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-004: City dropdown | UC-002, City Selection Screen (UI) | 1.4.2.1 - 1.4.2.2 | test_CitySelection, test_AvailableCities |
| REQ-005: Persist city | User API (PATCH /me), Firebase schema | 1.3.2.2, 1.4.2.2 | test_CityPersistence |

### Notifications (REQ-006 to REQ-008)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-006: Weekly push Thursday | UC-003, Weekly Job (Lambda), CloudWatch schedule | 1.3.6.6, 1.6.2.4 | test_WeeklyJobSchedule, test_PushDispatch |
| REQ-007: Push payload content | OpenAPI (push schema), weekly job sequence | 1.3.6.6 | test_PushPayloadStructure |
| REQ-008: Deep link to feed | Notification Tap Flow (UI), iOS push handling | 1.4.5.4 | test_DeepLinkHandling |

### Feed & Spot Display (REQ-009 to REQ-011)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-009: View current spot | UC-004, Home Feed Screen (UI), Feed API (GET /me/feed) | 1.3.3, 1.4.3 | test_FeedLoad, test_CurrentSpotDisplay |
| REQ-010: Spot details | OpenAPI /me/feed response schema, UI design | 1.3.3, 1.4.3 | test_SpotDetailsComplete |
| REQ-011: Open in Maps | Home Feed Screen (UI), iOS URL scheme | 1.4.3.5 | test_MapsDeepLink |

### History Display (REQ-012 to REQ-013)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-012: 4-week history | UC-005, Home Feed Screen (history section), UC-010 (backend) | 1.3.3, 1.4.3 | test_HistoryFetch, test_HistoryDisplay |
| REQ-013: History details | OpenAPI history schema, UI design | 1.3.3, 1.4.3 | test_HistoryItemContent |

### Social Signals (REQ-014 to REQ-015)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-014: Invite count | UC-006, Home Feed (counts display), UC-010 | 1.3.3, 1.4.3 | test_InviteCountDisplay |
| REQ-015: RSVP counts | UC-006, Home Feed (yes/maybe/no counts), UC-010 | 1.3.3, 1.4.3 | test_RsvpCountBreakdown |

### RSVP Functionality (REQ-016 to REQ-020)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-016: RSVP Yes | UC-007, RSVP Toggle (UI), POST /invites/{id}/rsvp | 1.3.4, 1.4.4 | test_RsvpYes |
| REQ-017: RSVP Interested | UC-007, RSVP Toggle (UI), POST /invites/{id}/rsvp | 1.3.4, 1.4.4 | test_RsvpInterested |
| REQ-018: RSVP No | UC-007, RSVP Toggle (UI), POST /invites/{id}/rsvp | 1.3.4, 1.4.4 | test_RsvpNo |
| REQ-019: RSVP persistence | Firestore schema (Invite.status), API endpoint | 1.3.4.2 | test_RsvpPersistence |
| REQ-020: Count updates | Home Feed Screen (reactive), real-time Firebase | 1.4.4.3 | test_CountsUpdateOnRsvp |

### Weekly Job (REQ-021 to REQ-028)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-021: One venue per city/week | UC-008, Weekly Job sequence, domain model (WeeklySpot) | 1.3.6.1 - 1.3.6.5 | test_WeeklySpotUniqueness |
| REQ-022: Google Places API | UC-008, API integration, Places API client | 1.3.6.1 | test_GooglePlacesQuery |
| REQ-023: Exclusion list | UC-008, VenueExclusionList entity, query logic | 1.3.6.2 | test_ExclusionListFiltering |
| REQ-024: Curation rules | UC-008, WeeklyJobHandler.java | 1.3.6.3 | test_CurationRules |
| REQ-025: Invite creation | UC-008, Invite entity, Firebase write | 1.3.6.4 | test_InviteGeneration |
| REQ-026: Push dispatch | UC-008, FCM integration, PushTokenService | 1.3.6.6 | test_PushDelivery |
| REQ-027: Email dispatch | UC-008, email service integration | 1.3.6.7 | test_EmailDelivery |
| REQ-028: Retry logic | UC-008, error handling, Lambda retry policy | 1.3.6.8 | test_RetryOnFailure |

### Location & City Detection (REQ-029 to REQ-030)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-029: Location updates | UC-009, iOS location service, POST /locations endpoint | 1.4.5.2, 1.3.5 | test_LocationUpload |
| REQ-030: City determination | UC-009, reverse geocode, Firebase schema | 1.3.6.1 | test_CityDetection |

### History Retrieval (REQ-031 to REQ-032)

| Requirement | Design Artifact | Implementation | Test |
|-------------|-----------------|-----------------|------|
| REQ-031: 4-week fetch | UC-010, Firebase query (weeklySpots collection, order by weekId desc, limit 4) | 1.3.7.4, 1.5.1 | test_HistoryQuery |
| REQ-032: Count calculation | UC-010, Firestore aggregation queries (count by status) | 1.3.10 | test_CountAggregation |

---

## Design Artifact Cross-Reference

### Use Cases

| UC | Title | Requirements Satisfied |
|----|-------|------------------------|
| UC-001 | Register / Sign In | REQ-001, REQ-002, REQ-003 |
| UC-002 | Grant Location Permission | REQ-004, REQ-005 |
| UC-003 | Receive Weekly Push Notification | REQ-006, REQ-007, REQ-008 |
| UC-004 | View This Week's Spot Details | REQ-009, REQ-010, REQ-011 |
| UC-005 | View 4-Week History | REQ-012, REQ-013 |
| UC-006 | See Social Signals | REQ-014, REQ-015 |
| UC-007 | Checkin / RSVP | REQ-016, REQ-017, REQ-018, REQ-019, REQ-020 |
| UC-008 | Weekly Scheduled Job | REQ-021 through REQ-028 |
| UC-009 | Receive Location Updates | REQ-029, REQ-030 |
| UC-010 | Fetch User's 4-Week History | REQ-031, REQ-032 |

### API Endpoints

| Endpoint | UC | Requirements |
|----------|----|-|
| POST /auth/apple-signin | UC-001 | REQ-001, REQ-003 |
| POST /auth/google-signin | UC-001 | REQ-002, REQ-003 |
| GET /me | UC-002 | REQ-004, REQ-005 |
| PATCH /me | UC-002 | REQ-005 |
| GET /me/feed | UC-004, UC-005, UC-006, UC-010 | REQ-009 through REQ-015, REQ-031, REQ-032 |
| POST /invites/{id}/rsvp | UC-007 | REQ-016 through REQ-020 |
| POST /push-tokens | UC-003 | REQ-006 through REQ-008 |

### UI Screens

| Screen | UC | Requirements |
|--------|----|-|
| Sign-In Screen | UC-001 | REQ-001, REQ-002, REQ-003 |
| City Selection | UC-002 | REQ-004, REQ-005 |
| Home Feed | UC-004, UC-005, UC-006 | REQ-009 through REQ-015 |
| RSVP Toggle | UC-007 | REQ-016 through REQ-020 |
| Empty State | UC-004 | Graceful UX when no spot available |
| Notification Tap | UC-003 | REQ-008 |

---

## Test Coverage Map

### Unit Tests (Backend)

| Component | Test Suite | Requirements | Success Criteria |
|-----------|-----------|--------------|-----------------|
| AuthService | test_AuthService.java | REQ-001, REQ-002, REQ-003 | ≥ 90% coverage |
| UserService | test_UserService.java | REQ-004, REQ-005 | ≥ 90% coverage |
| InviteService | test_InviteService.java | REQ-016 through REQ-020 | ≥ 90% coverage |
| WeeklyJobHandler | test_WeeklyJobHandler.java | REQ-021 through REQ-028 | ≥ 90% coverage, 15 scenarios |
| FeedService | test_FeedService.java | REQ-009 through REQ-015, REQ-031, REQ-032 | ≥ 90% coverage |

### Integration Tests (Backend)

| Flow | Test Suite | Requirements | Environment |
|------|-----------|--------------|-------------|
| Sign-in → Profile | test_AuthFlow.java | REQ-001, REQ-002, REQ-003, REQ-004 | Firebase Emulator |
| Weekly Job E2E | test_WeeklyJobFlow.java | REQ-021 through REQ-028 | Firebase Emulator, mocked Google Places |
| Feed Load | test_FeedFlow.java | REQ-009 through REQ-015 | Firebase Emulator |
| RSVP Update | test_RsvpFlow.java | REQ-016 through REQ-020 | Firebase Emulator |

### iOS Unit Tests

| Component | Test Suite | Requirements | Success Criteria |
|-----------|-----------|--------------|-----------------|
| AuthViewModel | AuthViewModelTests.swift | REQ-001, REQ-002, REQ-003 | ≥ 80% coverage |
| FeedViewModel | FeedViewModelTests.swift | REQ-009 through REQ-015 | ≥ 80% coverage |
| RsvpViewModel | RsvpViewModelTests.swift | REQ-016 through REQ-020 | ≥ 80% coverage |
| APIClient | APIClientTests.swift | All API calls | ≥ 80% coverage |

### iOS UI Tests

| Flow | Test Case | Requirements |
|------|-----------|--------------|
| Sign-in → Feed | test_SignInAndViewFeed | REQ-001/002, REQ-009, REQ-010 |
| View History | test_ViewHistoryScroll | REQ-012, REQ-013 |
| RSVP Flow | test_RsvpAndCountUpdate | REQ-016/017/018, REQ-020 |
| Push Notification | test_PushNotificationTap | REQ-006/007/008 |

### E2E / Manual Tests

| Scenario | Requirements | Tester | Schedule |
|----------|--------------|--------|----------|
| Maya (Commuter) | REQ-006, REQ-009, REQ-014/015, REQ-016 | QA | Week 12 |
| James (Traveler) | REQ-004, REQ-009/010, REQ-012 | QA | Week 12 |
| Priya (Newcomer) | REQ-001, REQ-004, REQ-006, REQ-016 | QA | Week 12 |
| Weekly Job (4 weeks) | REQ-021 through REQ-028 | Engineer | Weeks 8-11 |

---

## Sign-Off

### Planning Artifacts Confirmed

- [x] Scenarios & personas (3 personas, 5 scenarios)
- [x] Use cases (10 use cases, 3 activity diagrams)
- [x] Domain model (9 entities, 3 sequence diagrams)
- [x] Data model (ERD, Firebase schema)
- [x] API specification (8 endpoints, OpenAPI 3.1)
- [x] UI prototypes (5 screens, design tokens)
- [x] Security (threat model 18 STRIDE findings, auth design)
- [x] Quality (quality model, test strategy, quality gates)
- [x] Project management (charter, WBS 220 hours, schedule 14 weeks, risks)
- [x] DevOps (monorepo structure, CI/CD workflows, secrets management)

### RTM Complete

- [x] All 32 requirements traced to design, implementation, and tests
- [x] 100% traceability (no orphaned requirements)
- [x] All 10 use cases mapped
- [x] All 8 API endpoints mapped
- [x] All 5 UI screens mapped
- [x] Test coverage defined for all requirements

### Ready for Implementation

- [x] Scope locked (no new requirements without change control)
- [x] MVP features confirmed (scope-in document)
- [x] Timeline accepted (14 weeks, 220 hours)
- [x] Risks acknowledged (18 risks, mitigations documented)
- [x] Team & roles assigned (solo engineer)
- [x] Environments configured (dev, nonprod, prod)
- [x] CI/CD pipelines defined (test on PR, deploy on main/tag)

---

## Implementation Handoff

**Next Steps**:
1. Initialize git repository: `git init` in `/Users/charlieknight/covey/`
2. Create GitHub repository and push
3. Configure GitHub branch protection and secrets
4. Set up AWS and Firebase dev/prod environments
5. **Begin Stage 1 (Backend)**: Implement auth layer (WBS 1.3.1), target Week 3
6. **Begin Stage 2 (iOS, parallel)**: Implement sign-in screens (WBS 1.4.1), target Week 4

**Sign-Off Authority**: William Knight (Product Owner + Engineer)

**Date**: 2026-06-27

**Status**: ✅ READY FOR IMPLEMENTATION
