# Production Deployment Checklist

Before running `deploy-prod.yml` for the first time, everything below must exist.
Work through it top to bottom — AWS infra first, then GitHub, then verify.

---

## 1. AWS Infrastructure

### Lambda
- [ ] Create function `covey-weekly-spot-prod` (Java 17, same config as dev)
- [ ] Enable SnapStart on the function
- [ ] Set memory to 1536 MB, timeout to 60s

### S3
- [ ] Create bucket `covey-lambda-deployments-prod` (same region: us-west-2)
- [ ] Block all public access

### API Gateway
- [ ] Create new REST API for prod (or add a `prod` stage to the existing gateway)
- [ ] Note the **API ID** and **proxy resource ID** — needed for GitHub secrets
- [ ] Prod endpoint will be: `https://<PROD_API_ID>.execute-api.us-west-2.amazonaws.com/prod`

### Secrets Manager
- [ ] Create secret `covey/firebase-admin-prod` with the prod Firebase service account JSON
  - If using same Firebase project for now: copy the dev secret value
  - Longer term: create a separate `covey-prod` Firebase project

### IAM
- [ ] Confirm `GitHubActionsRole` has permissions to deploy to prod Lambda and S3
  - If the role is scoped to dev resources only, add prod ARNs to its policy

### EventBridge (weekly jobs)
- [ ] Deploy `infrastructure/eventbridge-weekly-job.yaml` with `Environment=prod`
  ```bash
  aws cloudformation deploy \
    --template-file infrastructure/eventbridge-weekly-job.yaml \
    --stack-name covey-eventbridge-prod \
    --parameter-overrides Environment=prod LambdaFunctionArn=arn:aws:lambda:us-west-2:ACCOUNT:function:covey-weekly-spot-prod \
    --capabilities CAPABILITY_NAMED_IAM \
    --region us-west-2
  ```

### SES
- [ ] Verify `hello@coveyspot.app` is verified in SES for us-west-2 (already done for dev)
- [ ] If SES is still in sandbox mode: request production access in the AWS console
  (sandbox = can only send to verified addresses; prod = send to anyone)

---

## 2. GitHub Secrets (Settings → Secrets → Actions)

These must be set in the repo before the workflow can run:

| Secret | Value |
|---|---|
| `AWS_ACCOUNT_ID` | Your 12-digit AWS account ID (already set for nonprod) |
| `GOOGLE_PLACES_API_KEY` | Same key used in dev (already set) |
| `EXPO_TOKEN` | EAS token (already set for nonprod) |
| `PROD_API_GATEWAY_ID` | The API Gateway ID from step 1 (e.g. `abc123xyz`) |
| `PROD_API_PROXY_RESOURCE_ID` | The proxy resource ID from step 1 |
| `COVEY_LOGO_URL` | Public URL of the Covey icon (Firebase Storage or S3) — optional |

---

## 3. GitHub Environment: `prod`

- [ ] Go to **Settings → Environments → New environment** → name it `prod`
- [ ] Add **Required reviewers** (yourself, or a second person)
- [ ] This gates both jobs — the workflow will pause and wait for approval before deploying

---

## 4. App Store Connect

- [ ] iOS 1.0 metadata filled in (name, description, keywords, screenshots)
- [ ] Privacy Policy URL live and linked in ASC
- [ ] Support URL linked in ASC
- [ ] At least 3 screenshots uploaded for iPhone 6.1" (1179×2556)
- [ ] Build has been tested on TestFlight before submitting to App Store review

---

## 5. Running the Deployment

Go to **Actions → Deploy to Production → Run workflow**:

| Input | Recommended for first run |
|---|---|
| Deploy backend | ✅ true |
| Deploy iOS | ✅ true |
| Submit to App Store review | ❌ false (build only — review manually in ASC first) |
| Confirm | Type `DEPLOY` |

The `prod` environment gate will pause and ask for approval before anything deploys.

---

## 6. Post-Deployment Verification

- [ ] Hit `https://<prod-api>/me` with a valid token — expect 200
- [ ] Send a test email via `POST /notifications/send-test` against prod URL
- [ ] Check CloudWatch dashboard: `covey-weekly-job-prod`
- [ ] Verify EventBridge rules are enabled for prod
- [ ] If iOS submitted: monitor App Store review status in ASC (typically 1–3 days)
