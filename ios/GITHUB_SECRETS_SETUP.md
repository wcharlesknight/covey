# GitHub Secrets Setup for Firebase CI/CD

To enable the iOS app tests and deployments in GitHub Actions to access Firebase, add these secrets to your repository.

## How to Add GitHub Secrets

1. Go to your GitHub repository: https://github.com/wcharlesknight/covey
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret below

## Required Secrets

Add these 8 secrets:

| Secret Name | Value |
|---|---|
| `EXPO_PUBLIC_FIREBASE_API_KEY` | `AIzaSyAt_V-kktvAIvPIP8W8ijutySAK86z-Qqs` |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | `covey-76e19.firebaseapp.com` |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | `covey-76e19` |
| `EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET` | `covey-76e19.firebasestorage.app` |
| `EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | `303020987694` |
| `EXPO_PUBLIC_FIREBASE_APP_ID` | `1:303020987694:ios:c42f2b7b2d078d181c56bf` |
| `EXPO_PUBLIC_GOOGLE_CLIENT_ID` | `303020987694-macposb4igsvikt3tkef8q1168nvpu5d.apps.googleusercontent.com` |
| `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID` | `303020987694-macposb4igsvikt3tkef8q1168nvpu5d.apps.googleusercontent.com` |

## Using Secrets in GitHub Actions

Secrets are automatically injected into workflow environment:

```yaml
- name: Run iOS tests
  run: cd ios && npm test
  env:
    EXPO_PUBLIC_FIREBASE_API_KEY: ${{ secrets.EXPO_PUBLIC_FIREBASE_API_KEY }}
    EXPO_PUBLIC_FIREBASE_PROJECT_ID: ${{ secrets.EXPO_PUBLIC_FIREBASE_PROJECT_ID }}
    # ... etc
```

## Local Development

Create `.env` file in `ios/` directory with the same values (never commit this file).

## ⚠️ EAS Builds Read Their Own Environment Store — NOT These Secrets

The GitHub secrets above are used by GitHub Actions (backend deploy, iOS test job). **They are NOT what EAS bakes into the app binary.** EAS Build injects `EXPO_PUBLIC_*` values from the **EAS-hosted environment** (`production` for the `testflight`/`production` profiles), which is a separate store.

When rotating any `EXPO_PUBLIC_*` value (e.g. Firebase App ID or Google OAuth client ID), update **all three** places or builds will ship stale values:

1. `ios/.env` — local dev
2. GitHub secrets — CI test/deploy jobs
3. **EAS environment** — what actually ships in the build:
   ```bash
   cd ios
   eas env:list --environment production
   eas env:update production --variable-name EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID --value "<new>" --non-interactive
   ```

Symptom of a missed EAS update: the app behaves as if using an old credential even though `.env` and GitHub secrets look correct (e.g. Google Sign-In `redirect_uri_mismatch` after changing the OAuth client).

## Security Notes

- All `EXPO_PUBLIC_*` variables are embedded in the app binary and are not secrets
- The `.env` file is git-ignored and should never be committed
- Rotate credentials annually or when team access changes
- See `docs/devops/secrets-management.md` for full security guidelines
