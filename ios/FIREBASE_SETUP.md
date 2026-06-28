# Firebase Configuration Setup

## Getting Firebase Credentials

### 1. Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (covey-dev)
3. Click "Project Settings" (gear icon)
4. Under "Your apps", find your iOS app registration
5. Copy the following credentials:
   - `EXPO_PUBLIC_FIREBASE_API_KEY`
   - `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN`
   - `EXPO_PUBLIC_FIREBASE_PROJECT_ID`
   - `EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET`
   - `EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID`
   - `EXPO_PUBLIC_FIREBASE_APP_ID`

### 2. Google OAuth Credentials
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Navigate to "APIs & Services" > "Credentials"
4. Find your OAuth 2.0 Client IDs
5. Copy:
   - `EXPO_PUBLIC_GOOGLE_CLIENT_ID` (Web client ID)
   - `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID` (iOS client ID)

### 3. Create .env File
Copy `.env.example` to `.env` and fill in the credentials:

```bash
cp ios/.env.example ios/.env
```

Then edit `ios/.env` with your actual credentials.

## Using Test Tokens

For testing without real Firebase users, use `firebaseTestUtils.ts`:

```typescript
import { generateMockUser, createTestFirebaseApp } from './src/utils/firebaseTestUtils';

const mockUser = generateMockUser();
const testFirebaseApp = createTestFirebaseApp(firebaseConfig);
```

## Environment Variables

All Firebase config must be prefixed with `EXPO_PUBLIC_` to be accessible in the Expo app:

- `EXPO_PUBLIC_FIREBASE_API_KEY`
- `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN`
- `EXPO_PUBLIC_FIREBASE_PROJECT_ID`
- `EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET`
- `EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID`
- `EXPO_PUBLIC_FIREBASE_APP_ID`
- `EXPO_PUBLIC_GOOGLE_CLIENT_ID`
- `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID`
- `EXPO_PUBLIC_API_BASE_URL`
