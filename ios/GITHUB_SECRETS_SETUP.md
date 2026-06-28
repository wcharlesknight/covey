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
| `EXPO_PUBLIC_FIREBASE_APP_ID` | `1:303020987694:ios:6f027ecd0c2ac0a31c56bf` |
| `EXPO_PUBLIC_GOOGLE_CLIENT_ID` | `303020987694-na9d43c35c3dca316e3b9so1na8er982.apps.googleusercontent.com` |
| `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID` | `303020987694-na9d43c35c3dca316e3b9so1na8er982.apps.googleusercontent.com` |

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

## Security Notes

- All `EXPO_PUBLIC_*` variables are embedded in the app binary and are not secrets
- The `.env` file is git-ignored and should never be committed
- Rotate credentials annually or when team access changes
- See `docs/devops/secrets-management.md` for full security guidelines
