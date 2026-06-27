# Covey iOS — React Native + Expo

**Cross-platform iOS app built with React Native and Expo**

---

## Overview

Covey iOS is a React Native app built with **Expo** for rapid development and deployment to both iOS and Android (future). The app allows users to:
- Sign in with Apple/Google
- Select their city (Seattle, Tacoma)
- View this week's curated venue + 4-week history
- RSVP to venues (Yes / Interested / No)
- Receive push notifications

**Tech Stack:**
- React Native 18+ (JavaScript/TypeScript)
- Expo SDK 51+
- Firebase Auth (sign-in)
- Firebase Realtime Database or REST API
- Expo Notification Center (push notifications)
- EAS Build & Submit (CI/CD)

---

## Quick Start

### Prerequisites

- Node.js 18+ ([download](https://nodejs.org/))
- npm or yarn
- Expo CLI: `npm install -g eas-cli expo-cli`
- iOS simulator (built into Xcode) or physical iPhone + Expo Go app

### Setup

```bash
cd covey/ios

# Install dependencies
npm install

# Start Expo dev server
npx expo start

# Press 'i' to open in iOS simulator
# OR scan QR code with Expo Go app on physical device
```

### Running Tests

**Unit tests (Jest)**:
```bash
npm test
npm test -- --coverage  # with coverage
npm test -- --watch     # watch mode
```

**E2E tests (Detox)**:
```bash
# Build Detox test app
npx detox build-framework-cache
npx detox build-app

# Run tests
npx detox test-runner
npx detox test-runner --cleanup  # teardown after
```

### Building for Production

**Using EAS Build** (recommended):
```bash
# Preview build (internal testing)
eas build --platform ios --profile preview

# Production build (App Store)
eas build --platform ios --profile production
```

**Manual build** (if needed):
```bash
npx expo prebuild --clean
xcodebuild -workspace ios/Covey.xcworkspace -scheme Covey -configuration Release
```

---

## Project Structure

```
ios/
├── app/
│   ├── screens/                    # Screen components
│   │   ├── AuthScreen.tsx          # Sign-in (Apple/Google)
│   │   ├── CitySelectionScreen.tsx # City picker
│   │   ├── FeedScreen.tsx          # Home feed + history
│   │   ├── RsvpScreen.tsx          # RSVP interaction (or inline in FeedScreen)
│   │   └── SettingsScreen.tsx      # User settings
│   ├── components/                 # Reusable components
│   │   ├── VenueCard.tsx           # Venue display card
│   │   ├── RsvpToggle.tsx          # RSVP yes/no/interested buttons
│   │   ├── HistoryList.tsx         # 4-week history list
│   │   ├── Button.tsx              # Styled button
│   │   ├── Card.tsx                # Styled card
│   │   └── Loader.tsx              # Loading spinner
│   ├── services/
│   │   ├── api.ts                  # HTTP client (axios or fetch wrapper)
│   │   ├── firebase.ts             # Firebase Auth & Realtime DB
│   │   ├── notifications.ts        # Expo Notifications
│   │   └── storage.ts              # AsyncStorage (session, cache)
│   ├── models/
│   │   ├── User.ts
│   │   ├── Invite.ts
│   │   ├── WeeklySpot.ts
│   │   └── types.ts                # Global TypeScript types
│   ├── context/
│   │   ├── AuthContext.tsx         # Auth state management
│   │   ├── AppContext.tsx          # Global app state
│   │   └── useAuth.ts              # Auth hook
│   ├── hooks/
│   │   ├── useFeed.ts              # Feed data fetching
│   │   ├── useRsvp.ts              # RSVP mutations
│   │   └── useNotifications.ts     # Notification handling
│   ├── styles/
│   │   ├── theme.ts                # Design tokens (colors, spacing, fonts)
│   │   ├── typography.ts           # Text styles
│   │   └── colors.ts               # Color palette
│   ├── app.json                    # Expo configuration
│   ├── App.tsx                     # Root component
│   └── index.ts                    # Entry point
├── __tests__/                      # Jest unit tests
│   ├── screens/                    # Screen component tests
│   │   ├── AuthScreen.test.tsx
│   │   ├── FeedScreen.test.tsx
│   │   └── RsvpScreen.test.tsx
│   ├── components/                 # Component tests
│   │   ├── VenueCard.test.tsx
│   │   └── RsvpToggle.test.tsx
│   ├── services/                   # Service tests (mocked)
│   │   ├── api.test.ts
│   │   └── firebase.test.ts
│   └── setup.ts                    # Jest configuration
├── e2e/                            # Detox E2E tests
│   ├── firstTest.e2e.ts            # Example test
│   ├── authFlow.e2e.ts             # Sign-in flow
│   ├── rsvpFlow.e2e.ts             # RSVP flow
│   └── config.json                 # Detox configuration
├── .detoxrc.json                   # Detox configuration
├── eas.json                        # EAS Build/Submit configuration
├── app.json                        # Expo app configuration
├── tsconfig.json                   # TypeScript configuration
├── package.json                    # Dependencies
├── package-lock.json               # Locked dependencies
└── README.md                       # This file
```

---

## Environment Variables

Create `.env.local` in the `ios/` directory (git-ignored):

```bash
# Firebase
EXPO_PUBLIC_FIREBASE_PROJECT_ID=covey-dev
EXPO_PUBLIC_FIREBASE_API_KEY=AIzaSy...

# API
EXPO_PUBLIC_API_BASE_URL=https://dev-api.covey.local

# Logging
EXPO_PUBLIC_LOG_LEVEL=debug

# Analytics
EXPO_PUBLIC_ANALYTICS_ENABLED=true
```

**Important**: Expo environment variables must be prefixed with `EXPO_PUBLIC_` to be available at runtime.

---

## Key Dependencies

| Package | Purpose | Docs |
|---------|---------|------|
| `expo` | Framework for React Native | [expo.dev](https://expo.dev) |
| `expo-firebase-core` | Firebase integration | [docs](https://docs.expo.dev/build/setup/) |
| `expo-notifications` | Push notifications | [docs](https://docs.expo.dev/push-notifications/) |
| `@react-navigation/native` | Navigation (Tab + Stack) | [docs](https://reactnavigation.org/) |
| `axios` | HTTP client | [docs](https://axios-http.com/) |
| `react-native-gesture-handler` | Touch handling | [docs](https://swmansion.com/react-native-gesture-handler/) |
| `zustand` or `redux` | State management | Choose based on complexity |
| `jest` | Unit testing | [docs](https://jestjs.io/) |
| `detox` | E2E testing | [docs](https://wix.github.io/Detox/) |

---

## Development Workflow

### Local Development

1. Start Expo dev server: `npx expo start`
2. Open in simulator: press `i`
3. Make code changes → HMR reloads automatically
4. Use React DevTools: press `j` in terminal

### Testing Before Commit

```bash
# Lint
npx eslint app/

# Type check
npx tsc --noEmit

# Unit tests
npm test

# E2E tests (local)
npx detox build-app && npx detox test-runner

# Build check
npx expo prebuild --clean
```

### Debugging

**In Expo Go**:
- Shake device (iOS) or press `cmd+d` in simulator
- Open debugger: "Open Debugger"
- Use React DevTools browser extension

**In VS Code**:
- Install "React Native Tools" extension
- Set breakpoints, use Watch, Debug Console

### Hot Reload vs Full Reload

- **Hot Reload** (HMR): Preserves state, reloads JS only (default)
- **Full Reload**: Restarts app, clears state (press `r` in Expo CLI)
- **Clear Cache**: `npx expo start --clear`

---

## Testing

### Unit Tests (Jest)

**Example test**:
```typescript
// __tests__/components/RsvpToggle.test.tsx
import { render, fireEvent } from '@testing-library/react-native'
import RsvpToggle from '../../app/components/RsvpToggle'

describe('RsvpToggle', () => {
  it('toggles RSVP state on tap', () => {
    const mockOnRsvp = jest.fn()
    const { getByTestId } = render(
      <RsvpToggle status="INVITED" onRsvp={mockOnRsvp} />
    )

    fireEvent.press(getByTestId('rsvp-yes-button'))
    expect(mockOnRsvp).toHaveBeenCalledWith('YES')
  })
})
```

**Run tests**:
```bash
npm test                              # Run once
npm test -- --watch                   # Watch mode
npm test -- --coverage                # With coverage report
npm test -- --testPathPattern=RsvpToggle  # Specific test
```

### E2E Tests (Detox)

**Example test**:
```typescript
// e2e/rsvpFlow.e2e.ts
describe('RSVP Flow', () => {
  beforeAll(async () => {
    await device.launchApp()
  })

  beforeEach(async () => {
    await device.reloadReactNative()
  })

  it('should RSVP to current spot', async () => {
    // Sign in
    await element(by.id('apple-signin-button')).tap()
    await waitFor(element(by.id('feed-screen')))
      .toBeVisible()
      .withTimeout(5000)

    // Tap Yes RSVP button
    await element(by.id('rsvp-yes-button')).tap()

    // Verify count updated
    await expect(element(by.text('1 confirmed'))).toBeVisible()
  })
})
```

**Run E2E tests**:
```bash
npx detox build-app
npx detox test-runner
```

---

## Building & Deployment

### EAS Build

**Preview build** (for testing):
```bash
eas build --platform ios --profile preview
# Download and run with: eas build:list, then open link
```

**Production build** (for App Store):
```bash
eas build --platform ios --profile production
# Automatically submits to TestFlight
```

### EAS Submit

**Submit to App Store Connect**:
```bash
eas submit --platform ios --latest
```

**Configuration** (eas.json):
```json
{
  "build": {
    "preview": {
      "ios": {
        "simulator": false,
        "scheme": "Covey",
        "buildConfiguration": "Debug"
      }
    },
    "production": {
      "ios": {
        "simulator": false,
        "scheme": "Covey",
        "buildConfiguration": "Release"
      }
    }
  },
  "submit": {
    "production": {
      "ios": {
        "appleId": "$EXPO_APPLE_ID",
        "ascAppId": "$EXPO_ASC_APP_ID",
        "appleTeamId": "$EXPO_APPLE_TEAM_ID"
      }
    }
  }
}
```

---

## Troubleshooting

### App won't start

```bash
# Clear cache and node_modules
rm -rf node_modules .expo-shared
npm install

# Rebuild
npx expo prebuild --clean
npx expo start --clear
```

### Tests failing locally but passing in CI

- Ensure Node version matches CI (check `.nvmrc`)
- Clear `node_modules` and reinstall
- Check for missing environment variables (`.env.local`)

### Push notifications not working

- Check APNs certificate is valid in Apple Developer
- Verify `EXPO_TOKEN` environment variable is set (EAS)
- Test with Expo's notification tool: `npx expo send:ios`

---

## References

- [Expo Documentation](https://docs.expo.dev/)
- [React Native Docs](https://reactnative.dev/)
- [Detox E2E Testing](https://wix.github.io/Detox/)
- [EAS Build Documentation](https://docs.expo.dev/build/introduction/)
- [Firebase React Native Guide](https://firebase.google.com/docs/database/usage/quickstart)
