# Covey — Social Gathering App

**Monorepo for iOS app + Java Lambda backend**

---

## Repository Structure

```
covey/
├── backend/                    # Java Lambda backend
│   ├── src/
│   │   ├── main/java/         # Lambda handlers, services
│   │   └── test/java/         # Unit & integration tests
│   ├── build.gradle           # Gradle build config
│   ├── pom.xml               # Maven config (if using Maven)
│   └── README.md             # Backend-specific docs
│
├── ios/                        # React Native app (JavaScript/TypeScript)
│   ├── app/                   # React Native Expo app
│   │   ├── screens/           # Screen components (Sign-in, Feed, RSVP, etc.)
│   │   ├── components/        # Reusable components (buttons, cards, RSVP toggle)
│   │   ├── services/          # API client, Firebase service
│   │   ├── models/            # TypeScript domain models
│   │   ├── context/           # React Context (auth state, app state)
│   │   ├── hooks/             # Custom React hooks
│   │   ├── styles/            # Design tokens, theme
│   │   └── app.json           # Expo configuration
│   ├── __tests__/             # Jest unit tests
│   ├── e2e/                   # Detox E2E tests
│   ├── package.json           # Dependencies
│   ├── tsconfig.json          # TypeScript config
│   ├── eas.json               # Expo Application Services config
│   └── README.md             # iOS/React Native documentation
│
├── infrastructure/             # IaC & deployment config
│   ├── terraform/             # Terraform (if using)
│   ├── cloudformation/        # CloudFormation templates
│   └── README.md
│
├── .github/
│   └── workflows/
│       ├── test.yml          # PR: run tests
│       ├── deploy-nonprod.yml # main: deploy to nonprod
│       └── deploy-prod.yml    # tag: deploy to prod
│
├── docs/
│   ├── sdlc.state.json       # Planning state
│   ├── devops/
│   │   ├── monorepo-guide.md
│   │   ├── environments.md
│   │   ├── deployment-guide.md
│   │   └── secrets-management.md
│   ├── architecture/
│   ├── security/
│   ├── api/
│   └── ...
│
├── .gitignore
├── README.md                  # This file
└── DEVELOPMENT.md            # Developer setup guide
```

---

## Quick Start

### Prerequisites

- **Backend**: Java 11+, Maven or Gradle, AWS CLI
- **iOS**: Xcode 14+, CocoaPods or SPM, iOS 14+
- **DevOps**: Docker (optional), GitHub Actions enabled

### Local Development Setup

See [DEVELOPMENT.md](./DEVELOPMENT.md) for detailed setup instructions.

#### Backend

```bash
cd backend
gradle build
gradle test
```

#### iOS (React Native)

```bash
cd ios
npm install
npx expo start  # Start Expo dev server
# Press 'i' to open in iOS simulator, or scan QR code with Expo Go app
```

**Run tests**:
```bash
npm test                    # Jest unit tests
npx detox test-runner       # E2E tests
```

---

## GitHub Deployment Pipeline

### Branches & Environments

| Branch | Environment | Trigger | Action |
|--------|-------------|---------|--------|
| `feature/*`, `bugfix/*` | None | Pull Request opened | Run tests (linting, unit, integration) |
| `main` | Nonprod | Merge to main | Deploy to nonprod (Firebase dev, Lambda dev) |
| `release/*` or tag `v*` | Prod | Tag or release created | Deploy to prod (Firebase prod, Lambda prod) |

### CI/CD Workflows

**On Pull Request:**
- Backend: linting, unit tests, integration tests, OWASP dependency check
- iOS: SwiftLint, unit tests, UI tests, code coverage
- Approval required from code owner before merge

**On Merge to Main (Nonprod Deployment):**
- Build and push backend Docker image to ECR (if using)
- Deploy Lambda functions to AWS dev environment
- Deploy iOS to TestFlight (beta)
- Run smoke tests against nonprod environment

**On Tag / Release (Prod Deployment):**
- Build production-ready backend artifacts
- Deploy Lambda to AWS prod
- Release iOS to App Store (manual or automatic)
- Run production smoke tests

See [.github/workflows/](./github/workflows/) for detailed workflow configs.

---

## Deployment Checklist

Before deploying to production, ensure:
- [ ] All PR checks passing (tests, linting, coverage gates)
- [ ] Security review complete (threat model, SAST, dependency audit)
- [ ] Code review approved by at least one owner
- [ ] Release notes updated
- [ ] Database migrations (if any) tested on nonprod
- [ ] Feature flags or kill switches in place (if needed)
- [ ] On-call team notified of pending deployment

---

## Secrets Management

**Never commit secrets** (API keys, certificates, credentials) to the repository.

Secrets are stored in:
- **GitHub**: GitHub Secrets (used in CI/CD workflows)
- **AWS**: AWS Secrets Manager (used by Lambda at runtime)
- **Local**: `.env.local` file (in `.gitignore`, only for local development)

See [docs/devops/secrets-management.md](./docs/devops/secrets-management.md) for setup instructions.

---

## Environment Variables

### Nonprod

```
FIREBASE_PROJECT_ID=covey-dev
AWS_LAMBDA_ENV=dev
GOOGLE_PLACES_API_KEY_SECRET_ARN=arn:aws:secretsmanager:...
LOG_LEVEL=debug
```

### Prod

```
FIREBASE_PROJECT_ID=covey-prod
AWS_LAMBDA_ENV=prod
GOOGLE_PLACES_API_KEY_SECRET_ARN=arn:aws:secretsmanager:...
LOG_LEVEL=info
```

See [docs/devops/environments.md](./docs/devops/environments.md) for full list.

---

## Monitoring & Alerts

- **Backend**: CloudWatch Logs, CloudWatch Alarms on Lambda errors
- **iOS**: Firebase Crashlytics, Firebase Analytics
- **Database**: Firestore metrics in Firebase Console

See [docs/devops/deployment-guide.md](./docs/devops/deployment-guide.md) for monitoring setup.

---

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make changes and commit: `git commit -m "describe your change"`
3. Push and open a PR: `git push origin feature/your-feature`
4. All checks must pass before merge
5. Once merged to `main`, nonprod deployment begins automatically

See [DEVELOPMENT.md](./DEVELOPMENT.md) for code style and testing guidelines.

---

## Support

For questions or issues:
- **Backend**: See [backend/README.md](./backend/README.md)
- **iOS**: See [ios/README.md](./ios/README.md)
- **DevOps**: See [docs/devops/](./docs/devops/)
- **Architecture**: See [docs/arch/](./docs/arch/)
