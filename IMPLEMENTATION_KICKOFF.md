# Covey Implementation Kickoff

**Date**: 2026-06-27
**Status**: ✅ Ready for Development

---

## Project Summary

**Covey** is a social gathering app that notifies users of weekly curated local venues and lets them RSVP.

- **Platform**: iOS (React Native + Expo)
- **Backend**: Java Lambda (AWS)
- **Database**: Firebase (Firestore)
- **MVP Timeline**: 14 weeks (2026-06-27 to 2026-10-03)
- **Team**: Solo engineer
- **Scope**: Seattle + Tacoma, weekly venue notifications, RSVP (yes/interested/no), 4-week history

---

## Implementation Order (WBS Priority)

### Phase 1: Infrastructure Setup (Week 1)
**Duration**: 1 week | **Effort**: 20 hours

1. **Initialize monorepo**
   ```bash
   cd /Users/charlieknight/covey
   git init
   git remote add origin https://github.com/williamchknight/covey.git
   ```

2. **Set up GitHub**
   - Create repository
   - Configure branch protection (main branch)
   - Add GitHub Secrets (AWS account IDs, iOS certs, Expo token, Slack webhook)

3. **Set up backends**
   - Create AWS dev/prod environments (Lambda, IAM roles, Secrets Manager)
   - Create Firebase dev/prod projects
   - Generate Google Places API keys
   - Configure APNs certificates

4. **Test CI/CD pipeline**
   - Push a dummy commit to test workflows
   - Verify test.yml runs successfully
   - Verify deploy-nonprod would trigger (but block actual deploy)

**Exit Criteria**: Environments configured, CI/CD tested, ready for feature work

---

### Phase 2: Backend Implementation (Weeks 2-5)
**Duration**: 4 weeks | **Effort**: 78 hours

**WBS Items** (in order):
1. **1.3.1** — Auth layer (Firebase token validation middleware)
2. **1.3.2** — User API (GET /me, PATCH /me)
3. **1.3.3** — Feed API (GET /me/feed with 4-week history)
4. **1.3.4** — RSVP API (POST /invites/{id}/rsvp)
5. **1.3.5** — Push Token API (POST /push-tokens)
6. **1.3.6** — Weekly Job Lambda (spot selection, invite creation, push/email dispatch) — **10 sub-tasks, highest complexity**
7. **1.3.7** — Backend testing (unit + integration tests, 80%+ coverage)
8. **1.3.8** — Backend deployment to AWS dev

**Milestones**:
- Week 3: Auth + User API complete
- Week 4: Feed + RSVP APIs complete
- Week 5: Weekly Job complete + all tests passing

**Exit Criteria**: 
- All 5 API endpoints working
- Weekly job tested end-to-end (manual trigger successful)
- Backend unit + integration tests ≥80% coverage
- Deployed to AWS dev environment

---

### Phase 3: iOS Implementation (Weeks 4-10, parallel with backend)
**Duration**: 7 weeks | **Effort**: 74 hours

**Start Week 4** (after auth layer is stable). Screens can be built in parallel with backend.

**WBS Items** (in order):
1. **1.4.1** — Sign-in screens (Apple Sign In + Google Sign In, Keychain storage)
2. **1.4.2** — City selection screen
3. **1.4.3** — Home feed screen (current spot + 4-week history)
4. **1.4.4** — RSVP UI (yes/interested/no toggle)
5. **1.4.5** — Push notification handling (FCM token registration, deep linking)
6. **1.4.6** — iOS testing (unit + UI tests, 70%+ coverage)
7. **1.4.7** — App Store submission prep (screenshots, metadata, privacy manifest)

**Milestones**:
- Week 6: Sign-in + City selection complete
- Week 8: Feed + RSVP screens complete
- Week 9: Push notifications working
- Week 10: All iOS tests passing, App Store prep done

**Exit Criteria**:
- All screens built and wired to backend
- Push notifications working end-to-end
- iOS unit + UI tests ≥70% coverage
- Build uploadable to TestFlight

---

### Phase 4: Integration & Testing (Weeks 11-12)
**Duration**: 2 weeks | **Effort**: 20 hours

1. **End-to-end user flows** (7 scenarios from RTM)
   - Maya: Push notification → View spot → RSVP yes
   - James: Select city → View history → Explore venue details
   - Priya: Sign-in → City selection → View current spot → RSVP interested
   - Plus 4 more comprehensive flows

2. **Weekly job E2E** (7 scenarios)
   - Spot selection, invite creation, push delivery, email delivery
   - Error handling, retry logic, exclusion list
   - Multi-week rotation (simulate 4 consecutive weeks)

3. **Security review**
   - Firestore rules access control
   - API auth validation
   - Binary secret scanning

4. **Performance testing**
   - Weekly job: < 60 seconds for 100 users
   - GET /me/feed: p95 < 500ms
   - Cold launch: < 3 seconds

**Exit Criteria**:
- All E2E flows passing
- Security review complete, 0 high-severity issues
- Performance within targets

---

### Phase 5: Beta & Launch (Weeks 13-14)
**Duration**: 2 weeks | **Effort**: 12 hours

1. **TestFlight beta** (Week 13)
   - Deploy to internal testers
   - Collect feedback
   - Fix critical issues found

2. **App Store submission** (Week 13-14)
   - Final checklist review
   - Submit for review
   - Respond to reviewer feedback (typically 24-72 hours)

3. **Launch** (Week 14)
   - Release on App Store
   - Announce to beta testers + personal network
   - Monitor Crashlytics (target: 98% crash-free rate)
   - Monitor weekly Lambda job (target: 99% success rate)

**Exit Criteria**:
- App approved and live on App Store
- First weekly job runs successfully in production
- Crash-free session rate ≥98%

---

## Key Implementation Details

### Authentication (UC-001)
- Use Firebase Auth for Apple/Google sign-in
- Store ID token in iOS Keychain (via Expo)
- Validate token on every API request (Java Firebase Admin SDK)
- Short TTL (1 hour), rely on refresh token for persistence

### Weekly Spot Selection (UC-008) — **MOST COMPLEX**
- Query Google Places API by city center
- Filter by rating (≥4.0) + reviews (≥50) + weekday hours
- Exclude venues in 4-week history (VenueExclusionList)
- If no eligible venues, expand search radius 50% and retry
- If still none, use least-recently-selected venue from history
- Create Invite records for all users in city
- Send push notifications to all registered devices
- Send email to verified email addresses
- Retry failed notifications (3 attempts)
- Log execution (CloudWatch)
- **Test with 15 scenarios** before prod deployment

### RSVP Flow (UC-007)
- User taps yes/no/interested on home feed
- POST /invites/{id}/rsvp with status
- Backend validates: user owns invite, invite is for current week
- Update Firestore Invite.status
- Fetch live attendee count (checkins in last 2 hours)
- iOS shows updated count immediately (optimistic update)

### Push Notifications
- Register device token: POST /push-tokens
- Store token in Firestore, bound to user ID
- Weekly job sends via FCM (Android path for future)
- Deep link: notification tap → home feed with current spot scrolled into view
- On APNs failure: mark token as invalid, clean up on next launch

### Testing Requirements
- **Backend**: 80%+ unit coverage (Jacoco), integration tests with Firebase Emulator
- **iOS**: 70%+ unit coverage (Jest), E2E tests with Detox
- **Weekly job**: 15-scenario test matrix covering all branches
- **Security**: Firestore rules test, API auth validation test, no high-severity findings

---

## Development Environment

### Required Setup

**Backend**:
```bash
cd backend
./gradlew build                    # Build
./gradlew test                     # Unit tests
./gradlew integrationTest          # Integration tests (Firebase Emulator)
./gradlew checkstyleMain           # Linting
./gradlew dependencyCheckAnalyze   # Dependency audit
```

**iOS**:
```bash
cd ios
npm install
npx expo start                     # Dev server
npm test                           # Jest unit tests
npx detox build-app && npx detox test-runner  # E2E tests
```

**Firebase Emulator** (for local integration testing):
```bash
firebase emulators:start           # Starts Auth + Firestore locally
# Firestore: localhost:8080
# Auth: localhost:9099
```

**Google Places API**:
- Get API key from Google Cloud Console
- Set via environment variable or AWS Secrets Manager
- Test with: curl "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=47.6,-122.3&type=bar&key=AIza..."

---

## Critical Paths & Risks

### Critical Path (determines minimum timeline)
1. Environment setup (1 week)
2. Backend auth + User API (1.5 weeks)
3. iOS sign-in (1 week, parallel)
4. Backend Feed + RSVP + Weekly Job (2 weeks)
5. iOS feed + RSVP (2 weeks, parallel)
6. Integration testing (1 week)
7. Beta + App Store review (2 weeks)

**Total**: ~12-14 weeks

### Highest-Risk Items
1. **Weekly Job Lambda** (UC-008) — Most complex, highest test burden
   - Mitigate: Start early (Week 3), 15-scenario test matrix, manual testing before prod
2. **App Store Review** — External dependency, unpredictable timeline
   - Mitigate: Build buffer week in Phase 5, test submission process in nonprod first
3. **Google Places API quota** — External API limit
   - Mitigate: Monitor usage, request quota increase before prod, implement caching/fallback
4. **Firebase scaling** — Heavy Firestore read/write during weekly job
   - Mitigate: Test with 100 simulated users locally, monitor metrics during nonprod

---

## Deployment Strategy

### Nonprod (Automatic on merge to main)
- Backend: Deploy to Lambda dev
- iOS: Upload to TestFlight (developer group)
- Smoke tests run automatically
- Slack notification sent

### Prod (Manual on tag)
- Create release tag: `git tag -a v1.0.0 -m "Release v1.0.0"`
- Push tag: `git push origin v1.0.0`
- GitHub Actions deploys to prod automatically
- Manual approval for App Store release

### Rollback
- **Nonprod**: Revert commit, push to main, redeploy
- **Prod**: Create rollback tag pointing to previous version, push

---

## Monitoring & On-Call

### Backend Monitoring
- CloudWatch Logs: `/aws/lambda/covey-weekly-spot-dev` and `-prod`
- CloudWatch Alarms: Lambda errors, timeouts, throttles
- Alert channels: Slack `#alerts`, on-call engineer page if critical

### iOS Monitoring
- Firebase Crashlytics: Crash-free rate ≥98%
- Firebase Analytics: Daily active users, RSVP funnel
- Alert threshold: Crash-free < 95%

### Weekly Job Verification (manual)
- Each week, check CloudWatch logs for successful completion
- Verify Firestore has new WeeklySpot
- Verify Invites created for all users
- Verify push/email sent successfully
- Document in Slack `#deployments`

---

## File Checklist Before First Commit

**Backend**:
- [ ] `backend/build.gradle` — Gradle config with all dependencies
- [ ] `backend/src/main/java/com/covey/handlers/` — Lambda handler stubs
- [ ] `backend/src/test/java/` — Test directory setup
- [ ] `.gitignore` — Excludes build/, target/, .gradle/, secrets

**iOS**:
- [ ] `ios/package.json` — All dependencies listed
- [ ] `ios/app.json` — Expo config with bundleId, notification plugins
- [ ] `ios/app/App.tsx` — Root component stub
- [ ] `ios/__tests__/` — Test directory setup
- [ ] `ios/e2e/` — Detox config + example test
- [ ] `.env.local.example` — Template for local env vars (no secrets)

**GitHub**:
- [ ] `.github/workflows/test.yml` — Test pipeline
- [ ] `.github/workflows/deploy-nonprod.yml` — Nonprod deployment
- [ ] `.github/workflows/deploy-prod.yml` — Prod deployment (reference only)
- [ ] `.gitignore` — Excludes .env.local, node_modules, build outputs, secrets

**Secrets** (GitHub + AWS, NOT in repo):
- [ ] GitHub Secrets: AWS_ACCOUNT_ID, IOS_SIGNING_CERT_BASE64, etc.
- [ ] AWS Secrets Manager: google-places-dev, firebase-admin-dev
- [ ] Expo token (for EAS): EXPO_TOKEN

---

## Success Criteria for Each Phase

### Phase 1 (Infrastructure)
- ✅ GitHub repo created, branch protection enabled
- ✅ CI/CD pipelines run on dummy commit (no deploy)
- ✅ AWS dev/prod environments configured
- ✅ Firebase dev/prod projects created
- ✅ Google Places API key generated
- ✅ APNs certificates configured

### Phase 2 (Backend)
- ✅ All 5 API endpoints deployed to dev Lambda
- ✅ Weekly job runs manually, produces correct output
- ✅ Backend unit tests ≥80% coverage
- ✅ Integration tests passing with Firebase Emulator
- ✅ Nonprod deployment working (merge to main → auto deploy)

### Phase 3 (iOS)
- ✅ All 5 screens built and functional
- ✅ Push notifications received and deep-linked
- ✅ iOS unit tests ≥70% coverage
- ✅ E2E tests passing with Detox
- ✅ Build artifacts uploadable to TestFlight

### Phase 4 (Integration)
- ✅ All 7 E2E user flows passing
- ✅ Weekly job runs 4 weeks successfully (E2E)
- ✅ Security review complete, 0 high-severity findings
- ✅ Performance within targets (job < 60s, feed API < 500ms p95)

### Phase 5 (Launch)
- ✅ TestFlight beta distributed, feedback collected
- ✅ App Store submission approved (typically 24-72 hours)
- ✅ App live on App Store
- ✅ First weekly job runs in production successfully
- ✅ Crash-free rate ≥98%, weekly job success rate ≥99%

---

## Getting Started Now

1. **Confirm infrastructure** is in place (you'll set this up in Week 1):
   ```bash
   # GitHub
   cd /Users/charlieknight/covey
   git remote -v  # Verify origin
   
   # AWS
   aws sts get-caller-identity  # Verify credentials
   aws lambda list-functions --region us-west-2  # Verify Lambda access
   
   # Firebase
   firebase projects:list  # Verify Firebase projects
   
   # Google Places
   curl "https://maps.googleapis.com/maps/api/place/nearbysearch/json?..." # Test API key
   ```

2. **Initialize dependencies**:
   ```bash
   # Backend
   cd backend && ./gradlew build
   
   # iOS
   cd ../ios && npm install && npx expo start
   ```

3. **Start with WBS 1.3.1** (Auth layer):
   - Implement Firebase token validation middleware
   - Write unit tests (invalid token, expired token, valid token)
   - Deploy to dev Lambda
   - Verify endpoint responds

---

## Documentation References

- **Planning artifacts**: `/Users/charlieknight/docs/` (RTM, threat model, quality model, etc.)
- **Architecture**: `/Users/charlieknight/docs/arch/` (domain model, ERD, API spec)
- **DevOps**: `/Users/charlieknight/docs/devops/` (monorepo guide, deployment guide, secrets management)
- **Backend README**: `/Users/charlieknight/covey/backend/README.md`
- **iOS README**: `/Users/charlieknight/covey/ios/README.md`
- **Main README**: `/Users/charlieknight/covey/README.md`

---

## Contact & Support

- **Questions about architecture?** See `/Users/charlieknight/docs/arch/`
- **Stuck on deployment?** See `/Users/charlieknight/docs/devops/deployment-guide.md`
- **Need help with a specific API?** See `backend/README.md` for testing instructions
- **iOS setup issues?** See `ios/README.md` for troubleshooting

---

**Status**: ✅ READY FOR IMPLEMENTATION

**Next Step**: Start WBS 1.2 (Infrastructure Setup) and WBS 1.3.1 (Auth Layer)

**Target Completion**: 2026-10-03 (14 weeks from start)

---

*Planning completed by: Claude Code*
*Date: 2026-06-27*
*Project: Covey - Social Gathering App*
