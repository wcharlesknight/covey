# Production Readiness List

Ordered by what blocks what. Do these in sequence — App Store won't let you submit
without a live privacy policy URL; AWS infra must exist before the pipeline can run.

---

## Phase 1 — Merge & Ship Open Work

- [ ] Merge PR #81 — prod deployment pipeline
- [ ] Merge PR #80 — city pills redesign + spot type badges (currently open on feature branch — re-open PR or rebase onto main)

---

## Phase 2 — App Store Prerequisites (Apple requires all of these)

- [ ] **Privacy Policy** — write one and host it at a live URL (e.g. `coveyspot.app/privacy`)
  - Must cover: data collected, Firebase Auth, push notifications, email
  - Link it in App Store Connect under App Information
- [ ] **Support URL** — a live page or `mailto:hello@coveyspot.app` link
- [ ] **Screenshots** — minimum: 3 screens at 1179×2556 (iPhone 6.1")
  - Suggested screens: home feed with spot, city picker, notification pill
  - Take on iPhone 16 simulator (not 16e) — captures at correct resolution
  - Resize any existing ones with: `sips --resampleHeightWidth 2556 1179 <file>`
- [ ] **App Store metadata** — paste the copy from the previous session into ASC:
  - Name: Covey
  - Subtitle: Your weekly spot, picked for you
  - Description, keywords, promotional text — already drafted, ready to paste
- [ ] **Age rating** — complete the questionnaire in ASC (App Information → Age Rating)
- [ ] **App Review account** — add a test account in the App Review section so Apple can log in

---

## Phase 3 — AWS Production Infrastructure

- [ ] Create Lambda function `covey-weekly-spot-prod` (Java 17, 1536 MB, SnapStart on)
- [ ] Create S3 bucket `covey-lambda-deployments-prod` (us-west-2, block public access)
- [ ] Create prod API Gateway (REST API) — note the API ID and proxy resource ID
- [ ] Add `covey/firebase-admin-prod` secret to AWS Secrets Manager
- [ ] Confirm `GitHubActionsRole` IAM policy covers prod Lambda + S3 ARNs
- [ ] Deploy EventBridge prod rules:
  ```bash
  aws cloudformation deploy \
    --template-file infrastructure/eventbridge-weekly-job.yaml \
    --stack-name covey-eventbridge-prod \
    --parameter-overrides Environment=prod \
      LambdaFunctionArn=arn:aws:lambda:us-west-2:ACCOUNT:function:covey-weekly-spot-prod \
    --capabilities CAPABILITY_NAMED_IAM \
    --region us-west-2
  ```
- [ ] **SES out of sandbox** — request production access in AWS console
  (sandbox = can only email verified addresses; this blocks real users getting emails)

---

## Phase 4 — GitHub Setup

- [ ] Add GitHub secret `PROD_API_GATEWAY_ID`
- [ ] Add GitHub secret `PROD_API_PROXY_RESOURCE_ID`
- [ ] Create GitHub environment `prod` with required reviewer approval gate
  (Settings → Environments → New environment → add yourself as reviewer)

---

## Phase 5 — Firebase

- [ ] Enable Firebase Crashlytics on the iOS app (WBS 1.2.1.6)
- [ ] Decide: share `covey-76e19` for prod or create `covey-prod` project
  - Sharing is fine for v1.0 — create prod project before scaling
- [ ] Upload `ios/assets/icon.png` to Firebase Storage → set `COVEY_LOGO_URL` env var on Lambda
  (makes the weekly email show the Covey icon instead of just text)

---

## Phase 6 — TestFlight Validation

- [ ] Merge all open PRs to main → CI auto-deploys to TestFlight
- [ ] Install from TestFlight on a real device (not simulator)
- [ ] Verify end-to-end:
  - [ ] Sign in works (Apple / Google)
  - [ ] Home feed loads for your city
  - [ ] Browse pill opens city picker with "Browse a city" copy
  - [ ] Notification pill opens city picker with "Your notification city" copy + saves correctly
  - [ ] RSVP submits and counts update
  - [ ] Push notification arrives on Friday (or trigger manually via `/notifications/send-test`)
  - [ ] Email arrives with Covey sender name (not "hello")
- [ ] Fix any bugs found → merge fix → new TestFlight build deploys automatically

---

## Phase 7 — Submit to App Store

- [ ] Run **Deploy to Production** workflow (Actions tab in GitHub):
  - Deploy backend: ✅
  - Deploy iOS: ✅
  - Submit to App Store review: ❌ (build only first time)
  - Confirm: type `DEPLOY`
- [ ] In App Store Connect, review the uploaded build
- [ ] Click **Add for Review** → **Submit to App Review**
- [ ] Monitor review status (typically 1–3 business days)

---

## Nice-to-Have Before Launch (not blocking)

- [ ] Restrict Google Places API key to server-side use only (WBS 1.2.3.2)
- [ ] Privacy policy page that's actually designed (vs. a bare text page)
- [ ] `coveyspot.app` domain pointing to something real
- [ ] Set up CloudWatch alarm notifications (email/SNS when Lambda errors spike)
- [ ] Fix Postman environment so `{{api_base}}` and `{{firebase_token}}` resolve

---

## Biggest Blockers Right Now

1. **Privacy Policy URL** — Apple will reject without it
2. **Screenshots** — can't submit without them
3. **SES out of sandbox** — real users can't get emails until this is done
4. **Prod Lambda doesn't exist yet** — pipeline will fail until AWS infra is created
