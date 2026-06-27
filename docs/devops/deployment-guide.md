# Covey Deployment Guide

**Environments, workflows, and deployment procedures for Covey backend and iOS**

---

## Environments

### Development (Local)

- **Firebase**: Local Firebase Emulator Suite (Auth, Firestore)
- **AWS**: Local Lambda runtime (SAM CLI or serverless-offline)
- **Database**: Firebase Emulator Suite
- **Purpose**: Developer machines, feature development

### Nonprod (Development)

- **Firebase**: `covey-dev` project
- **AWS**: `dev-api.covey.local` (dev Lambda endpoints)
- **AWS Account**: `123456789012` (dev account)
- **AWS Region**: `us-west-2` (Seattle region)
- **Database**: Firestore (dev collection)
- **Deployment**: Automatic on merge to `main`
- **Retention**: 7 days (old data auto-purged)
- **Monitoring**: CloudWatch, Firebase Console

### Prod (Production)

- **Firebase**: `covey-prod` project
- **AWS**: `api.covey.io` (prod Lambda endpoints)
- **AWS Account**: `987654321098` (prod account)
- **AWS Region**: `us-west-2` (Seattle region)
- **Database**: Firestore (prod collection)
- **Deployment**: Manual on tag or release
- **Retention**: 90 days minimum
- **Monitoring**: CloudWatch, Firebase Console, Sentry

---

## Deployment Pipeline

```
Feature Branch
    ↓
Create Pull Request
    ↓
GitHub Actions: Test (backend unit, iOS unit, SAST, dependencies)
    ↓
Code Review + Approval
    ↓
Merge to Main
    ↓
GitHub Actions: Deploy Nonprod
  - Build backend Lambda
  - Deploy to AWS dev
  - Run smoke tests
  - Upload iOS to TestFlight
  - Notify Slack
    ↓
Manual Testing in Nonprod (QA, dogfooding)
    ↓
Create Release Tag (v1.0.0)
    ↓
GitHub Actions: Deploy Prod
  - Build production Lambda
  - Deploy to AWS prod
  - Release iOS to App Store
  - Run prod smoke tests
  - Notify on-call team
    ↓
Production Live
```

---

## Deployment Procedures

### Nonprod Deployment (Automatic on `main`)

**Trigger**: Merge to `main` branch

**Workflow**: `.github/workflows/deploy-nonprod.yml`

**What happens**:
1. Backend code built and tested
2. Lambda function `covey-weekly-spot-dev` updated in dev AWS account
3. Smoke tests run against dev environment
4. iOS app built and uploaded to TestFlight (developer group)
5. Slack notification sent to `#deployments` channel

**Duration**: ~15 minutes

**Manual rollback**:
```bash
# Rollback Lambda to previous version
aws lambda update-function-code \
  --function-name covey-weekly-spot-dev \
  --s3-bucket covey-dev-deployments \
  --s3-key covey-lambda-<previous-version>.zip \
  --region us-west-2
```

### Prod Deployment (Manual on Release)

**Trigger**: Create a git tag or release (e.g., `git tag v1.0.0`)

**Workflow**: `.github/workflows/deploy-prod.yml` (to be created)

**Pre-deployment checklist**:
- [ ] All tests pass in nonprod
- [ ] Security review complete
- [ ] Release notes written
- [ ] On-call team notified
- [ ] Database migrations tested (if applicable)
- [ ] Feature flags configured (if applicable)

**Steps**:
1. Create release tag: `git tag -a v1.0.0 -m "Release v1.0.0: weekly RSVP feature"`
2. Push tag: `git push origin v1.0.0`
3. GitHub Actions triggers prod deployment workflow
4. Backend deployed to prod AWS account
5. iOS submitted to App Store (or auto-released to internal testing first)
6. Smoke tests run against prod
7. Slack notification to on-call team

**Duration**: ~20 minutes (excluding App Store review time)

**Manual rollback (if needed)**:
```bash
# Rollback to previous version tag
git tag -a v1.0.0-rollback -m "Rollback from v1.0.0"
git push origin v1.0.0-rollback
# GitHub Actions will deploy this tag to prod
```

---

## Environment Configuration

### Nonprod Environment Variables

**Backend (Lambda)**:
```
FIREBASE_PROJECT_ID=covey-dev
FIREBASE_DB_URL=https://covey-dev.firebaseio.com
GOOGLE_PLACES_API_KEY_SECRET_ARN=arn:aws:secretsmanager:us-west-2:123456789012:secret:google-places-dev
AWS_LOG_LEVEL=DEBUG
WEEKLY_SPOT_SCHEDULE=cron(0 8 ? * THU *)  # Thursday 8 AM UTC
```

**iOS**:
```
FIREBASE_PROJECT_ID=covey-dev
API_BASE_URL=https://dev-api.covey.local
LOG_LEVEL=debug
ANALYTICS_ENABLED=true
```

### Prod Environment Variables

**Backend (Lambda)**:
```
FIREBASE_PROJECT_ID=covey-prod
FIREBASE_DB_URL=https://covey-prod.firebaseio.com
GOOGLE_PLACES_API_KEY_SECRET_ARN=arn:aws:secretsmanager:us-west-2:987654321098:secret:google-places-prod
AWS_LOG_LEVEL=INFO
WEEKLY_SPOT_SCHEDULE=cron(0 8 ? * THU *)  # Thursday 8 AM UTC
SENTRY_DSN=https://key@sentry.io/project
```

**iOS**:
```
FIREBASE_PROJECT_ID=covey-prod
API_BASE_URL=https://api.covey.io
LOG_LEVEL=info
ANALYTICS_ENABLED=true
CRASH_REPORTING_ENABLED=true
```

---

## Monitoring & Alerts

### Backend Monitoring (Lambda + CloudWatch)

- **Lambda Metrics**:
  - Invocation count
  - Error count (threshold: > 5 errors/hour → alert)
  - Duration (p99, target: < 10 seconds)
  - Throttles (threshold: > 0 → alert immediately)

- **CloudWatch Logs**:
  - Log group: `/aws/lambda/covey-weekly-spot-dev` (nonprod) and `-prod` (prod)
  - Error pattern matching: alert on `ERROR`, `FATAL`
  - Log retention: 7 days (nonprod), 30 days (prod)

- **CloudWatch Alarms**:
  - Lambda error rate > 5% → Slack alert to `#alerts`
  - Lambda timeout > 0 → Slack alert to `#alerts`
  - Lambda throttle > 0 → Page on-call engineer

### iOS Monitoring (Firebase Crashlytics + Analytics)

- **Crashlytics**:
  - Crash-free session rate (target: ≥ 98%)
  - Top crashes tracked daily
  - Alert if crash-free rate drops below 95%

- **Analytics**:
  - Daily active users
  - RSVP action funnels
  - Notification delivery rate

- **Alerts**:
  - Crash-free session rate < 95% → Slack to `#alerts`
  - 404 errors on API endpoints → Slack to `#devops`

---

## Secrets Management

### GitHub Secrets (Used in CI/CD)

Secrets are stored in GitHub and injected at CI/CD runtime.

**Backend secrets**:
- `AWS_ACCOUNT_ID`: 12-digit AWS account ID for dev environment
- `AWS_ACCOUNT_ID_PROD`: 12-digit AWS account ID for prod environment

**iOS secrets**:
- `IOS_SIGNING_CERT_BASE64`: Base64-encoded p12 signing certificate
- `IOS_SIGNING_CERT_PASSWORD`: Password for signing certificate
- `IOS_PROVISIONING_PROFILE_BASE64`: Base64-encoded provisioning profile
- `APPSTORE_ISSUER_ID`: App Store Connect issuer ID
- `APPSTORE_API_KEY_ID`: App Store Connect API key ID
- `APPSTORE_API_PRIVATE_KEY`: App Store Connect API private key (base64-encoded)

**Notifications**:
- `SLACK_WEBHOOK_URL`: Slack webhook URL for deployment notifications

See [secrets-management.md](./secrets-management.md) for how to set these up.

### AWS Secrets Manager (Used by Lambda at Runtime)

- `google-places-dev`: Google Places API key (dev)
- `google-places-prod`: Google Places API key (prod)
- `firebase-admin-dev`: Firebase Admin SDK service account JSON (dev)
- `firebase-admin-prod`: Firebase Admin SDK service account JSON (prod)

Lambda retrieves these at runtime using IAM role permissions.

---

## Rollback Procedures

### Nonprod Rollback

**If deployment fails**:
1. GitHub Actions workflow fails → Slack alert sent
2. Fix the issue in the code
3. Merge the fix to `main`
4. Nonprod redeployed automatically

**If deployed code has a bug**:
1. Revert the commit: `git revert <commit-hash>`
2. Push to `main`
3. Nonprod redeployed with reverted code

### Prod Rollback

**If prod deployment fails**:
1. Do NOT attempt manual fixes
2. Revert the release tag: `git push origin :refs/tags/v1.0.0`
3. Create a rollback tag pointing to the previous version: `git tag -a v0.9.9-rollback -m "Rollback from v1.0.0"`
4. Push rollback tag
5. GitHub Actions redeployes prod to the previous version

**If prod code has a critical bug**:
1. Create a hotfix branch: `git checkout -b hotfix/critical-bug`
2. Fix the issue
3. Test in nonprod
4. Merge to `main`
5. Create a new release tag: `git tag -a v1.0.1 -m "Hotfix: critical bug"`
6. Push tag to trigger prod deployment

---

## Weekly Job Deployment

The weekly spot selection Lambda runs on a schedule (Thursday 8 AM UTC).

### Testing Weekly Job Before Prod

**In nonprod**:
1. Trigger manually: `aws lambda invoke --function-name covey-weekly-spot-dev /tmp/output.json`
2. Check CloudWatch logs for errors
3. Verify Firestore has new WeeklySpot documents
4. Verify invites created for all users in the city
5. Verify push notifications sent successfully

**Before first prod deployment**:
1. Run manual trigger 2–3 times in nonprod (simulate multiple weeks)
2. Verify exclusion list prevents duplicates
3. Verify edge cases (zero users, all venues excluded, API failure)

### Monitoring Weekly Job in Prod

- Lambda logs in `/aws/lambda/covey-weekly-spot-prod`
- CloudWatch metrics: invocation count, error rate, duration
- Alarms: error count > 1 → page on-call engineer immediately
- Manual verification: spot selected, invites created, emails sent (check logs)

---

## Disaster Recovery

### Data Loss Scenario

**Firebase Firestore backup**:
- Automated daily backups enabled in Firebase Console
- Retention: 7 days
- To restore: contact Google Cloud support or restore from backup

**AWS Secrets backup**:
- Secrets stored in AWS Secrets Manager (encrypted, backed up by AWS)
- No manual backup needed

### Complete Service Outage

**If AWS Lambda region unavailable**:
1. Failover to secondary region (if configured)
2. Or: manually trigger the weekly Lambda from a dev machine
3. Or: post updates directly to Firestore in prod

**If Firebase project unavailable**:
1. Contact Google Cloud support for emergency restore
2. Or: use Firebase admin SDK to restore from backups

---

## On-Call Runbook

See [runbook.md](./runbook.md) for on-call troubleshooting and escalation procedures.

---

## FAQ

**Q: How long does a nonprod deployment take?**
A: ~15 minutes from merge to fully deployed and tested.

**Q: Can I deploy to prod during business hours?**
A: Yes, but coordinate with the team. Notify `#deployments` Slack channel.

**Q: What if a test fails in CI?**
A: Fix the test, push a new commit, the workflow re-runs automatically.

**Q: Can I skip tests and deploy anyway?**
A: No. GitHub branch protection rules enforce test passage before merge. (Can only be overridden by admins in emergencies.)

**Q: How do I verify a deployment succeeded?**
A: Check the workflow run in GitHub Actions, check Slack notification, check CloudWatch logs.
