# Work Breakdown Structure: Location Notifier MVP

---

## 1.0 Location Notifier MVP

---

### 1.1 Project Management

#### 1.1.1 Project Planning
- 1.1.1.1 Create project charter
- 1.1.1.2 Develop WBS
- 1.1.1.3 Create project schedule with milestones
- 1.1.1.4 Create risk register

#### 1.1.2 Project Monitoring and Control
- 1.1.2.1 Weekly self-review against schedule and risk register
- 1.1.2.2 Update risk register as new risks emerge
- 1.1.2.3 Track milestone progress; adjust schedule if needed

---

### 1.2 Setup and Infrastructure

#### 1.2.1 Firebase Configuration
- 1.2.1.1 Create Firebase project (dev + prod environments)
- 1.2.1.2 Enable Firebase Authentication (Apple Sign-In, Google Sign-In)
- 1.2.1.3 Configure Firestore database and initial security rules
- 1.2.1.4 Configure Firebase Cloud Messaging (FCM) for push notifications
- 1.2.1.5 Configure APNs certificates for iOS push delivery
- 1.2.1.6 Enable Firebase Crashlytics for iOS crash reporting

#### 1.2.2 AWS Configuration
- 1.2.2.1 Create AWS account and configure IAM roles and policies
- 1.2.2.2 Create Lambda execution role with least-privilege permissions
- 1.2.2.3 Configure Lambda layers (Java runtime dependencies, shared libraries)
- 1.2.2.4 Set up environment variables and AWS Secrets Manager for API keys
- 1.2.2.5 Configure CloudWatch alarms for Lambda errors and timeout events
- 1.2.2.6 Set up EventBridge scheduled rule for weekly Lambda job trigger

#### 1.2.3 Google Places API
- 1.2.3.1 Provision Google Cloud project and enable Places API
- 1.2.3.2 Generate and secure API key (restrict to server-side usage)
- 1.2.3.3 Test API connectivity and validate quota limits

#### 1.2.4 Local Development Environment and Repository
- 1.2.4.1 Initialize monorepo structure (backend + iOS in single repository)
- 1.2.4.2 Configure .gitignore, secrets handling (no secrets in source control)
- 1.2.4.3 Set up backend project (Java, Maven or Gradle, Lambda handler scaffolding)
- 1.2.4.4 Set up iOS project (Xcode project, dependency manager - Swift Package Manager)
- 1.2.4.5 Configure CI pipeline (GitHub Actions or equivalent) for backend build and test
- 1.2.4.6 Document local development setup (README)

---

### 1.3 Backend Implementation

#### 1.3.1 Authentication and Authorization Layer
- 1.3.1.1 Implement Firebase ID token verification in Lambda (Java Firebase Admin SDK)
- 1.3.1.2 Implement authentication middleware/filter applied to all protected endpoints
- 1.3.1.3 Write unit tests for token verification (valid token, expired token, malformed token)

#### 1.3.2 User API
- 1.3.2.1 Implement GET /me - return current user profile from Firestore
- 1.3.2.2 Implement PATCH /me - update user profile fields (display name, city preference)
- 1.3.2.3 Implement auto-provisioning of user document on first authenticated request
- 1.3.2.4 Write unit tests for user read and update logic
- 1.3.2.5 Write integration tests for GET /me and PATCH /me endpoints

#### 1.3.3 Feed API
- 1.3.3.1 Implement GET /me/feed - return current weekly spot and 4-week history
- 1.3.3.2 Include RSVP counts (yes, no, interested) per spot in feed response
- 1.3.3.3 Include caller's own RSVP status per spot if they have responded
- 1.3.3.4 Write unit tests for feed assembly logic
- 1.3.3.5 Write integration tests for GET /me/feed

#### 1.3.4 RSVP API
- 1.3.4.1 Implement POST /invites/{id}/rsvp - record RSVP response (yes/no/interested)
- 1.3.4.2 Validate invite belongs to requesting user; reject if not
- 1.3.4.3 Handle idempotent RSVP updates (user changes response)
- 1.3.4.4 Write unit tests for RSVP write and validation logic
- 1.3.4.5 Write integration tests for POST /invites/{id}/rsvp

#### 1.3.5 Push Token Registration API
- 1.3.5.1 Implement POST /push-tokens - register or update FCM device token for authenticated user
- 1.3.5.2 Handle token refresh (overwrite existing token for device)
- 1.3.5.3 Write unit and integration tests for push token registration

#### 1.3.6 Weekly Job Lambda
- 1.3.6.1 Implement spot selection logic - query Google Places API for candidate venues in target city
- 1.3.6.2 Implement venue exclusion list check (skip venues in VenueExclusionList collection)
- 1.3.6.3 Implement venue rotation logic (avoid repeating recent spots)
- 1.3.6.4 Implement WeeklySpot document creation in Firestore
- 1.3.6.5 Implement Invite document creation for all eligible users in the city
- 1.3.6.6 Implement push notification dispatch via FCM for all users with registered tokens
- 1.3.6.7 Implement email notification dispatch (AWS SES or Firebase Extension)
- 1.3.6.8 Implement error handling and retry logic for notification failures
- 1.3.6.9 Write unit tests for spot selection, exclusion, and rotation logic
- 1.3.6.10 Write integration tests for full weekly job flow (with Firestore emulator)

#### 1.3.7 Backend Testing and Quality
- 1.3.7.1 Achieve minimum 80% unit test coverage across Lambda handlers and service classes
- 1.3.7.2 Run integration tests against Firebase Emulator Suite (Auth, Firestore)
- 1.3.7.3 Manual end-to-end API test using real Firebase dev project and Postman/curl
- 1.3.7.4 Review Firestore security rules - ensure users can only read their own data

#### 1.3.8 Backend Deployment
- 1.3.8.1 Build deployment package (JAR or ZIP) and deploy Lambda functions to AWS
- 1.3.8.2 Configure API Gateway (or Lambda Function URLs) for HTTP endpoint exposure
- 1.3.8.3 Validate all endpoints respond correctly in deployed environment
- 1.3.8.4 Configure EventBridge rule and verify weekly job triggers on schedule
- 1.3.8.5 Confirm CloudWatch alarms are active and alerting correctly

---

### 1.4 iOS Implementation

#### 1.4.1 Sign-In Screens
- 1.4.1.1 Build sign-in screen with Apple Sign-In button and Google Sign-In button
- 1.4.1.2 Implement Apple Sign-In flow (ASAuthorizationController, credential handling)
- 1.4.1.3 Implement Google Sign-In flow (Google Sign-In SDK for iOS)
- 1.4.1.4 Exchange identity provider credential for Firebase ID token
- 1.4.1.5 Store Firebase ID token securely in iOS Keychain
- 1.4.1.6 Implement session restoration on app launch (auto sign-in if valid token in Keychain)
- 1.4.1.7 Implement sign-out flow (clear Keychain, revoke Firebase session)
- 1.4.1.8 Write unit tests for auth state management and Keychain operations

#### 1.4.2 City Selection
- 1.4.2.1 Build city selection screen or component (dropdown or segmented control)
- 1.4.2.2 Persist selected city to user profile via PATCH /me
- 1.4.2.3 Surface city preference in user settings / profile screen
- 1.4.2.4 Write unit tests for city selection logic and persistence

#### 1.4.3 Home Feed Screen
- 1.4.3.1 Build home feed screen layout (current spot card + history list)
- 1.4.3.2 Integrate GET /me/feed API call with loading and error states
- 1.4.3.3 Display weekly spot: venue name, address, description, Google Maps link or in-app map preview
- 1.4.3.4 Display RSVP counts (yes, no, interested) for current spot
- 1.4.3.5 Display 4-week history cards with spot summary and RSVP counts
- 1.4.3.6 Implement pull-to-refresh
- 1.4.3.7 Handle empty state (no spot yet this week)
- 1.4.3.8 Write unit tests for feed view model / data parsing

#### 1.4.4 RSVP UI
- 1.4.4.1 Build RSVP control (yes / no / interested buttons or segmented picker)
- 1.4.4.2 Wire RSVP actions to POST /invites/{id}/rsvp API call
- 1.4.4.3 Show optimistic UI update immediately on tap; revert on API error
- 1.4.4.4 Disable RSVP control for historical spots (past weeks are read-only)
- 1.4.4.5 Write unit tests for RSVP state management and API interaction

#### 1.4.5 Push Notification Handling
- 1.4.5.1 Request push notification permission from user on first launch (post sign-in)
- 1.4.5.2 Register device with FCM and call POST /push-tokens with received token
- 1.4.5.3 Handle APNs token refresh (re-register on token change)
- 1.4.5.4 Implement notification tap handler: deep link to home feed for the current weekly spot
- 1.4.5.5 Handle foreground notification display (show banner or in-app alert)
- 1.4.5.6 Write unit tests for notification registration and deep link routing

#### 1.4.6 iOS Testing
- 1.4.6.1 Write unit tests for all view models and service classes (target: 75%+ coverage)
- 1.4.6.2 Write UI tests for critical flows: sign-in, view feed, RSVP
- 1.4.6.3 Test on multiple iPhone simulator sizes (iPhone SE, iPhone 15, iPhone 15 Pro Max)
- 1.4.6.4 Test on physical device for push notifications and Keychain behavior

#### 1.4.7 App Store Submission Preparation
- 1.4.7.1 Configure App Store Connect: app name, bundle ID, app description, keywords
- 1.4.7.2 Generate distribution certificate and provisioning profile
- 1.4.7.3 Prepare app screenshots (required sizes: 6.7", 6.5", 5.5" displays)
- 1.4.7.4 Write App Store description and privacy policy URL
- 1.4.7.5 Configure app privacy manifest (required by Apple for third-party SDKs)
- 1.4.7.6 Archive and upload build to App Store Connect via Xcode
- 1.4.7.7 Submit for TestFlight review (internal testers do not require review; external does)

---

### 1.5 Integration and Testing

#### 1.5.1 End-to-End User Flow Testing
- 1.5.1.1 Test: New user sign-in via Apple Sign-In, profile created, city selected
- 1.5.1.2 Test: New user sign-in via Google Sign-In, profile created, city selected
- 1.5.1.3 Test: User views home feed with current weekly spot
- 1.5.1.4 Test: User RSVPs yes, verifies count updates on refresh
- 1.5.1.5 Test: User changes RSVP from yes to interested, verifies counts update
- 1.5.1.6 Test: User views 4-week history with past spots and RSVP counts
- 1.5.1.7 Test: User receives push notification, taps it, lands on feed with current spot

#### 1.5.2 Weekly Job End-to-End Testing
- 1.5.2.1 Manually trigger weekly Lambda job in dev environment
- 1.5.2.2 Verify WeeklySpot document created in Firestore with correct venue data
- 1.5.2.3 Verify Invite documents created for all users in the targeted city
- 1.5.2.4 Verify push notifications delivered to all registered devices (check FCM delivery receipts)
- 1.5.2.5 Verify email notifications sent (check SES/email send log)
- 1.5.2.6 Verify exclusion list prevents excluded venues from being selected
- 1.5.2.7 Simulate two consecutive weeks; verify same venue is not repeated

#### 1.5.3 Security Review
- 1.5.3.1 Verify Firestore security rules reject unauthenticated reads and writes
- 1.5.3.2 Verify API endpoints reject requests with invalid or expired Firebase tokens
- 1.5.3.3 Verify user A cannot read or modify user B's invite or RSVP
- 1.5.3.4 Verify Google Places API key is not embedded in the iOS binary
- 1.5.3.5 Review iOS app for any sensitive data logged to console in production builds

#### 1.5.4 Performance and Reliability
- 1.5.4.1 Measure weekly job execution time with 100 simulated users; verify completes within Lambda timeout
- 1.5.4.2 Measure GET /me/feed response time under normal load (target: under 500ms p95)
- 1.5.4.3 Test app cold launch time on minimum supported iOS version

---

### 1.6 Deployment and Launch

#### 1.6.1 Beta Testing (TestFlight)
- 1.6.1.1 Distribute build to internal beta group via TestFlight
- 1.6.1.2 Collect beta feedback for 1 week minimum
- 1.6.1.3 Triage beta feedback; fix critical bugs; defer non-critical to post-launch backlog
- 1.6.1.4 Confirm weekly job runs automatically during beta period

#### 1.6.2 App Store Submission
- 1.6.2.1 Final pre-submission checklist review (screenshots, description, privacy policy, app manifest)
- 1.6.2.2 Submit app for App Store review
- 1.6.2.3 Monitor review status; respond to any reviewer questions or rejection notes
- 1.6.2.4 Coordinate release timing once approved

#### 1.6.3 Launch
- 1.6.3.1 Release app on App Store (manual or automatic release)
- 1.6.3.2 Announce launch to initial user base (personal network, social channels)
- 1.6.3.3 Monitor Crashlytics for crash rate in first 48 hours post-launch
- 1.6.3.4 Monitor CloudWatch for Lambda errors in first weekly job run post-launch
- 1.6.3.5 Confirm push and email notification delivery for first live weekly spot

---

### 1.7 Project Closure

- 1.7.1 Document lessons learned from MVP build
- 1.7.2 Archive and tag v1.0 release in source control
- 1.7.3 Document post-MVP backlog (features deferred from scope)
- 1.7.4 Document runbook: how to manually trigger weekly job, manage exclusion list, add a new city

---

## Effort Estimates

| WBS Item | Estimated Hours | Primary Resource |
|---|---|---|
| 1.1 Project Management | 8 | PM/Engineer |
| 1.2 Setup and Infrastructure | 20 | Engineer |
| 1.3.1 Auth Layer | 8 | Engineer |
| 1.3.2 User API | 8 | Engineer |
| 1.3.3 Feed API | 10 | Engineer |
| 1.3.4 RSVP API | 8 | Engineer |
| 1.3.5 Push Token API | 4 | Engineer |
| 1.3.6 Weekly Job Lambda | 24 | Engineer |
| 1.3.7-8 Backend Testing + Deploy | 16 | Engineer |
| 1.4.1 Sign-In Screens | 16 | Engineer |
| 1.4.2 City Selection | 4 | Engineer |
| 1.4.3 Home Feed Screen | 16 | Engineer |
| 1.4.4 RSVP UI | 8 | Engineer |
| 1.4.5 Push Notification Handling | 10 | Engineer |
| 1.4.6 iOS Testing | 12 | Engineer |
| 1.4.7 App Store Preparation | 8 | Engineer |
| 1.5 Integration and Testing | 20 | Engineer |
| 1.6 Deployment and Launch | 12 | Engineer |
| 1.7 Project Closure | 4 | Engineer |
| **TOTAL** | **220 hours** | |

> Note: 220 hours at ~15-20 hours/week (part-time solo) equals approximately 11-15 weeks, consistent with the 11-14 week schedule estimate.
