# Covey Monorepo Architecture

**Single Git repository containing iOS app + Java Lambda backend with automated CI/CD pipelines.**

---

## Repository Structure

```
covey/                         # Monorepo root
├── backend/                   # Java Lambda backend
│   ├── src/
│   │   ├── main/java/
│   │   │   ├── com/covey/handlers/          # Lambda handlers
│   │   │   ├── com/covey/services/          # Business logic
│   │   │   ├── com/covey/models/            # Domain models
│   │   │   └── com/covey/config/            # Configuration
│   │   └── test/java/                       # Unit & integration tests
│   ├── build.gradle                         # Gradle build config
│   ├── settings.gradle                      # Gradle settings
│   ├── Dockerfile                           # Docker image (optional)
│   ├── README.md                            # Backend documentation
│   └── .gitignore
│
├── ios/                       # iOS app (Swift)
│   ├── Covey/                 # Xcode project
│   │   ├── Scenes/
│   │   │   ├── Auth/          # Sign-in screens
│   │   │   ├── Feed/          # Home feed + history
│   │   │   ├── RSVP/          # RSVP UI
│   │   │   └── Settings/      # User settings
│   │   ├── Models/            # Domain models (User, Invite, etc.)
│   │   ├── Services/
│   │   │   ├── APIClient.swift         # Covey API client
│   │   │   ├── FirebaseService.swift   # Firebase Auth/DB
│   │   │   └── NotificationService.swift
│   │   ├── Resources/
│   │   │   ├── Assets.xcassets/        # Images, colors
│   │   │   ├── Localizable.strings     # Strings
│   │   │   └── Covey.entitlements      # Capabilities
│   │   ├── AppDelegate.swift
│   │   ├── SceneDelegate.swift
│   │   └── Info.plist
│   ├── CoveyTests/            # Unit tests
│   ├── CoveyUITests/          # UI/integration tests
│   ├── Podfile                # CocoaPods deps (if using)
│   ├── Covey.xcodeproj/       # Xcode project
│   ├── Covey.xcworkspace/     # Xcode workspace
│   ├── README.md              # iOS documentation
│   └── .gitignore
│
├── infrastructure/            # IaC & deployment configs
│   ├── terraform/             # Terraform (if using IaC)
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── environments/
│   │       ├── dev.tfvars
│   │       └── prod.tfvars
│   ├── cloudformation/        # CloudFormation templates (if using)
│   ├── lambda-layers/         # Shared Lambda dependencies
│   └── README.md
│
├── .github/
│   ├── workflows/
│   │   ├── test.yml           # PR: test on every commit
│   │   ├── deploy-nonprod.yml # main: deploy to dev
│   │   └── deploy-prod.yml    # tag: deploy to prod
│   ├── CODEOWNERS             # Code review assignments
│   └── pull_request_template.md
│
├── docs/
│   ├── sdlc.state.json        # Planning state
│   ├── DEVELOPMENT.md         # Local setup guide
│   ├── devops/
│   │   ├── monorepo-guide.md  # This file
│   │   ├── deployment-guide.md
│   │   ├── environments.md
│   │   ├── secrets-management.md
│   │   └── runbook.md         # On-call troubleshooting
│   ├── architecture/          # System design docs
│   ├── security/              # Threat model, auth design
│   ├── quality/               # Quality model, test strategy
│   ├── api/                   # API spec (OpenAPI)
│   ├── pm/                    # Project management artifacts
│   └── ux/                    # UI design & prototypes
│
├── .gitignore                 # Global git ignore
├── .commitlintrc.json         # Commit message linting
├── README.md                  # Monorepo overview
└── CONTRIBUTING.md            # Contribution guidelines
```

---

## Development Workflow

### 1. Clone and Setup

```bash
# Clone the monorepo
git clone https://github.com/williamchknight/covey.git
cd covey

# Install backend dependencies
cd backend
./gradlew build

# Install iOS dependencies
cd ../ios
pod install

# Return to root
cd ..
```

### 2. Create a Feature Branch

```bash
# Create feature branch (branching strategy: feature/*)
git checkout -b feature/weekly-spot-rsvp

# Make changes to backend and/or iOS
# - Edit backend: backend/src/main/java/...
# - Edit iOS: ios/Covey/...
```

### 3. Test Locally

**Backend**:
```bash
cd backend
./gradlew test                      # Unit tests
./gradlew integrationTest           # Integration tests
./gradlew checkstyleMain            # Linting
```

**iOS**:
```bash
cd ios
# In Xcode: ⌘ + U to run tests
# Or from CLI:
xcodebuild test \
  -workspace Covey.xcworkspace \
  -scheme Covey \
  -configuration Debug
```

### 4. Commit and Push

```bash
# Commit with descriptive message
git add backend/ ios/
git commit -m "feat: add RSVP toggle UI and API integration"

# Push to remote
git push origin feature/weekly-spot-rsvp
```

### 5. Open a Pull Request

```bash
# GitHub CLI (recommended)
gh pr create --title "Add RSVP toggle UI" \
  --body "This PR adds the RSVP yes/no/interested toggle to the home feed screen and integrates it with the backend API."

# Or: use GitHub web UI
```

### 6. GitHub Actions Runs Automatically

**On PR**:
- Backend: linting, unit tests, integration tests, dependency audit
- iOS: linting, unit tests, UI tests
- All must pass before merge

**On Merge to Main**:
- Backend deployed to Lambda dev
- iOS uploaded to TestFlight
- Slack notification sent

### 7. Merge and Deploy

Once approved and tests pass:
```bash
# Merge via GitHub web UI (required: all checks passing, 1 approval)
# Nonprod deployment starts automatically

# Watch deployment in GitHub Actions tab
```

---

## CI/CD Pipeline Details

### Workflow Triggers

| Branch | Event | Workflow | Action |
|--------|-------|----------|--------|
| `feature/*`, `bugfix/*` | Pull Request opened/updated | `test.yml` | Test backend & iOS |
| `main` | Push (merge from PR) | `deploy-nonprod.yml` | Deploy to dev |
| `v*` tag | Push tag | `deploy-prod.yml` | Deploy to prod |

### Test Workflow (`test.yml`)

**Runs on**: Every PR and commit to feature branches

**Steps**:
1. Checkout code
2. Set up Java 11
3. Run backend tests:
   - Checkstyle linting
   - JUnit unit tests
   - Integration tests (Firebase Emulator)
   - OWASP dependency check
4. Set up Xcode (latest)
5. Run iOS tests:
   - SwiftLint linting
   - XCTest unit tests
   - XCUITest UI tests
6. Upload coverage to Codecov
7. Commit lint check (conventional commits)
8. Secret scanning (TruffleHog)

**Must Pass Before Merge**: Yes (GitHub branch protection)

**Duration**: ~15-20 minutes

### Nonprod Deployment Workflow (`deploy-nonprod.yml`)

**Runs on**: Merge to `main`

**Steps**:
1. Build backend JAR
2. Deploy to Lambda `covey-weekly-spot-dev`
3. Run smoke tests against dev API
4. Build iOS and upload to TestFlight (developer group)
5. Notify Slack `#deployments`

**Duration**: ~15 minutes

**Rollback**: Revert commit, push to `main`, redeploy

### Prod Deployment Workflow (`deploy-prod.yml`)

**Runs on**: Push of tag `v*`

**Pre-requisites**:
- [ ] Code merged to `main` and tested in nonprod
- [ ] Release notes written
- [ ] On-call team notified

**Steps**:
1. Build production backend JAR
2. Deploy to Lambda `covey-weekly-spot-prod`
3. Run smoke tests against prod API
4. Submit iOS to App Store (or upload to TestFlight for staging)
5. Notify on-call team via Slack

**Duration**: ~20 minutes (+ App Store review time)

**Rollback**: Create rollback tag, push, redeploy

---

## Code Organization Standards

### Backend

**Package structure**:
```
com.covey
├── handlers/               # Lambda handlers (entry points)
│   ├── AuthHandler.java
│   ├── FeedHandler.java
│   ├── RsvpHandler.java
│   └── WeeklyJobHandler.java
├── services/               # Business logic (no AWS/Framework coupling)
│   ├── UserService.java
│   ├── InviteService.java
│   └── NotificationService.java
├── models/                 # Domain models (POJOs)
│   ├── User.java
│   ├── Invite.java
│   └── WeeklySpot.java
├── config/                 # Configuration & DI
│   ├── FirebaseConfig.java
│   ├── AWSConfig.java
│   └── AppConfig.java
├── exceptions/             # Custom exceptions
│   ├── UnauthorizedException.java
│   └── InvalidInputException.java
└── util/                   # Utilities
    ├── SecretsUtil.java
    └── LoggingUtil.java
```

**File naming**:
- Classes: `PascalCase` (e.g., `UserService.java`)
- Methods: `camelCase` (e.g., `getUser()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRIES`)

### iOS

**File organization**:
```
Covey/
├── Scenes/                 # SwiftUI views (by feature)
│   ├── Auth/
│   │   ├── SignInView.swift
│   │   └── SignInViewModel.swift
│   ├── Feed/
│   │   ├── FeedView.swift
│   │   └── FeedViewModel.swift
│   └── RSVP/
│       ├── RsvpControl.swift
│       └── RsvpViewModel.swift
├── Models/                 # Domain models
│   ├── User.swift
│   ├── Invite.swift
│   └── Codable extensions
├── Services/               # API & Firebase clients
│   ├── APIClient.swift
│   ├── FirebaseService.swift
│   └── NotificationManager.swift
├── Resources/              # Assets, strings, config
│   ├── Assets.xcassets/
│   └── Localizable.strings
├── App/                    # App entry point
│   ├── AppDelegate.swift
│   └── SceneDelegate.swift
└── Utilities/              # Helper functions
    ├── Extensions.swift
    └── Logger.swift
```

**Naming conventions**:
- Files: `PascalCase` (e.g., `FeedView.swift`)
- Types: `PascalCase` (e.g., `class FeedViewModel`)
- Variables: `camelCase` (e.g., `var isLoading`)
- Constants: `camelCase` (e.g., `let maxRetries`)

---

## Branching Strategy

### Branch Types

| Type | Naming | Purpose | Base | Target |
|------|--------|---------|------|--------|
| Feature | `feature/description` | New features | `main` | `main` |
| Bugfix | `bugfix/description` | Bug fixes | `main` | `main` |
| Hotfix | `hotfix/description` | Critical prod fixes | `main` | `main` (then tag for prod) |
| Release | `release/v*` | Release prep | `main` | tag for prod |

### Example Workflow

```
main (stable, always deployable)
  ↑
  └─ feature/weekly-spot-rsvp (in progress)
       ↓
       PR opened → tests pass → code review → merge
       ↓
       GitHub Actions deploys to nonprod
       ↓
       Tested in nonprod
       ↓
       Create tag v1.0.0
       ↓
       GitHub Actions deploys to prod
```

---

## Dependency Management

### Backend

**Gradle** (preferred):
- Dependencies in `build.gradle`
- Update versions: `./gradlew dependencyUpdates`
- Audit for vulnerabilities: `./gradlew dependencyCheckAnalyze`

**Maven** (alternative):
- Dependencies in `pom.xml`
- Update versions: `mvn versions:display-dependency-updates`
- Audit: `mvn org.owasp:dependency-check-maven:check`

### iOS

**CocoaPods** (if using):
- Dependencies in `Podfile`
- Update: `pod update`
- Audit: `pod repo update`

**Swift Package Manager** (preferred):
- Dependencies in `Package.swift`
- Xcode auto-manages versions
- Audit: built-in vulnerability checks in Xcode 14+

---

## Standards & Guidelines

### Commit Message Format

Follow Conventional Commits:

```
<type>(<scope>): <description>

<body (optional)>

<footer (optional)>
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`

**Examples**:
```
feat(rsvp): add yes/no/interested toggle UI

fix(feed): correct date calculation for 4-week history

docs: update deployment guide with new endpoints
```

### Code Style

**Backend**:
- Java: Google Java Style (enforced by Checkstyle)
- Line length: 100 characters
- Indentation: 4 spaces

**iOS**:
- Swift: Ray Wenderlich style (enforced by SwiftLint)
- Indentation: 4 spaces
- Use modern Swift concurrency (async/await)

### Test Coverage Targets

- Backend: ≥ 80% (checked at PR)
- iOS: ≥ 70% (checked at PR)

---

## Common Tasks

### Run All Tests Locally

```bash
cd backend && ./gradlew test && cd ..
cd ios && xcodebuild test && cd ..
```

### Deploy Nonprod Manually

```bash
# Merge to main (tests auto-run, deployment auto-starts)
git push origin feature/xyz
# Create PR, wait for tests, approve, merge
```

### Deploy Prod Manually

```bash
# Create a release tag
git tag -a v1.0.0 -m "Release v1.0.0: weekly RSVP"
git push origin v1.0.0
# GitHub Actions deploys to prod automatically
```

### Rollback Nonprod

```bash
# Revert the commit
git revert <commit-hash>
git push origin main
# Nonprod redeployed automatically
```

### Rollback Prod

```bash
# Create a rollback tag
git tag -a v0.9.9-rollback -m "Rollback from v1.0.0"
git push origin v0.9.9-rollback
# GitHub Actions redeployment to prod
```

---

## Troubleshooting

**Tests failing in CI but passing locally**?
- Run on latest main: `git pull origin main`
- Clean build: `./gradlew clean build`
- Check environment: `.env.local` vs CI secrets

**Deployment stuck**?
- Check GitHub Actions logs: Go to **Actions** tab
- Check Lambda CloudWatch logs: `aws logs tail /aws/lambda/covey-weekly-spot-dev`
- Manual rollback if critical

**Secret not found at runtime**?
- Verify secret in AWS Secrets Manager: `aws secretsmanager get-secret-value --secret-id google-places-dev`
- Verify Lambda IAM role has permission: check role's inline policy
- Verify secret name matches code

---

See [deployment-guide.md](./deployment-guide.md) for detailed deployment procedures.
