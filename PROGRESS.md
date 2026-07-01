# Covey Project Progress & Memory

## 📍 Quick Status

**Current WBS:** 2.1 - iOS Firebase Configuration & Local Testing  
**Status:** Debugging DOMRectReadOnly polyfill issue, fixing path resolution  
**Branch:** `feature/expo-local-startup` (active debugging session)
**Latest Session:** 2026-06-29 - Fixing Firebase SDK initialization with polyfill

---

## 🔧 Latest Debugging Session (2026-06-30) - RESOLVED ✅

### Issue: Non-std C++ Exception + DOMRectReadOnly Error
**Symptoms:**
1. RCTFatal crash: `non-std C++ exception` at native bridge initialization
2. JS Error: `ReferenceError: Property 'DOMRectReadOnly' doesn't exist`

**Root Cause:** Corrupted Metro bundler cache + misaligned Babel/Metro configuration with Expo
- babel.config.js was using generic @babel presets instead of Expo-aware `babel-preset-expo`
- metro.config.js was missing entirely
- tsconfig.json had manual config instead of extending expo/tsconfig.base
- Combined with stale cache, this caused Hermes to crash

### Solution Applied:
1. ✅ **babel.config.js** — Simplified to use `babel-preset-expo`
2. ✅ **metro.config.js** — Created with Expo's default config template
3. ✅ **tsconfig.json** — Simplified to extend `expo/tsconfig.base` with just `strict: true`
4. ✅ **Cleared all caches** — Metro, Watchman, Jest, node_modules/.cache
5. ✅ **Rebuilt from scratch** — `npx expo run:ios --clear`

### Result:
**✅ App now launches successfully on iPhone 16 Pro simulator**
- 0 errors, 1 warning (normal `-lc++` linker warning)
- No C++ crashes
- No JavaScript errors
- Firebase initializes correctly

### Files Modified:
- `ios/babel.config.js` — Clean Expo preset config
- `ios/metro.config.js` — NEW: Expo Metro default config
- `ios/tsconfig.json` — Simplified to extend Expo base
- `ios/src/services/firebase.ts` — Reverted to clean state
- `ios/index.js` — Reverted to clean state
- `ios/App.tsx` — Reverted to clean state

---

## 📚 Full Progress Documentation

**Location:** `/Users/charlieknight/.claude/projects/-Users-charlieknight/memory/covey_wbs_progress.md`

This file contains:
- ✅ Completed WBS items
- ⏳ Current work in progress
- 📋 Next steps and how to resume
- 🔗 Key files and branches
- 🏗️ Infrastructure reference

---

## 🚀 Quick Resume Guide

To pick up where we left off:

```bash
# 1. Update your repo and switch to active branch
cd /Users/charlieknight/covey
git checkout feature/firebase-config && git pull

# 2. Ensure Node 22 is active (required for dependency compatibility)
nvm use 22

# 3. Install/update iOS dependencies
cd ios && npm install

# 4. Note: Local Expo startup has known issues (see below)
# For now, work continues on separate branch: feature/expo-local-startup
```

---

## 📱 iOS Firebase Configuration (PR #20) - COMPLETED ✅

### What Was Done:
- ✅ Firebase client configuration extracted from GoogleService-Info.plist
- ✅ 8 GitHub Secrets configured for CI/CD pipeline:
  - EXPO_PUBLIC_FIREBASE_API_KEY
  - EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN
  - EXPO_PUBLIC_FIREBASE_PROJECT_ID
  - EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET
  - EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID
  - EXPO_PUBLIC_FIREBASE_APP_ID
  - EXPO_PUBLIC_GOOGLE_CLIENT_ID
  - EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID
- ✅ Local .env file created (git-ignored)
- ✅ EAS project ID linked: `36924851-085c-4e48-8e07-94098d4b6b7b`
- ✅ Firebase test utilities created
- ✅ Frontend tests passing in CI/CD
- ✅ Lambda deployment fixed to use S3 (>70MB package size issue)
- ✅ Upgraded to Node v22.23.1 for compatibility

### Files Created/Modified:
- `ios/.env` — Firebase credentials (git-ignored)
- `ios/src/utils/firebaseTestUtils.ts` — Test utilities
- `ios/FIREBASE_SETUP.md` — Setup documentation
- `ios/GITHUB_SECRETS_SETUP.md` — Secret configuration guide
- `ios/app.json` — Updated with EAS project ID, removed invalid fields
- `ios/package.json` — Updated to Expo SDK 51 expected versions
- `babel.config.js` — Added private methods plugin, Flow preset
- `jest.config.js` — Babel transformation configuration
- Various dependency updates for Node 22 compatibility

---

## 🐛 Known Issues - Local Expo Startup (SEPARATE TASK)

### Issue: NSPOSIXErrorDomain code=60 (CoreSimulator Timeout)
- **Symptom:** Expo CLI hangs when trying to open exp:// URLs on iOS simulator
- **Root Cause:** CoreSimulator's `launchd_sim` daemon times out after 30 seconds
- **Affects:** QR code connections, auto-open, deep linking to dev server
- **Status:** **RESEARCHED** - Ready for separate branch: `feature/expo-local-startup`

### Issue: React Native C++ Exception (RCTFatal)
- **Symptom:** `non-std C++ exception` crash when app tries to initialize bridge
- **Root Causes Identified:**
  1. Architecture mismatch on M-series Macs (arm64 not excluded for simulator)
  2. Dependency version incompatibilities
  3. Stale CocoaPods/native build artifacts
  4. Node version constraints (v20.12.1 too old)
- **Status:** **PARTIALLY RESOLVED**
  - ✅ Upgraded to Node 22.23.1
  - ✅ Updated dependencies to Expo SDK 51 expected versions
  - ✅ Ran prebuild to generate native iOS project with proper CocoaPods
  - ⏳ Still need to test app launch after recent fixes

### Workarounds Applied:
- ✅ Cleared CoreSimulator caches: `rm -rf ~/Library/Developer/CoreSimulator/Caches/*`
- ✅ Erased and reset simulator: `xcrun simctl erase all`
- ✅ Installed CocoaPods via Gem (rbenv conflict workaround)
- ✅ Created placeholder PNG assets (required for prebuild)
- ✅ Fixed Babel configuration (added private methods plugin)
- ✅ Fixed Xcode schema validation (removed invalid `supportsTabletMode`)

### Recommended Next Steps for Local Testing:
1. **Create `feature/expo-local-startup` branch** to isolate testing from Firebase work
2. Run `npx expo run:ios` (managed workflow - bypasses Expo Go installation issues)
3. Manually open Expo dev server on exp://127.0.0.1:8081 (avoids QR code timeout)
4. Test app renders SignIn screen without C++ crash
5. Test Firebase auth flow end-to-end

### Resources & Research:
- [React Native Issue #30924](https://github.com/expo/expo/issues/30924) - RCTFatal with RN 0.74.5
- [React Native Issue #37060](https://github.com/expo/expo/issues/37060) - Non-std C++ exception
- [EAS CLI Issue #2443](https://github.com/expo/eas-cli/issues/2443) - RCTFatal in SDK 51
- [Xcode 16+ Compiler Incompatibility](https://github.com/facebook/react-native/discussions) - Strict compiler with older RN
- [Core issue: M-series Macs need arm64 excluded for iOS Simulator](https://developer.apple.com/forums/thread/735232)

---

## 🔑 Current Context (Session: 2026-06-28)

**What's Working:**
- ✅ Firebase client SDK configured and linked to GCP project
- ✅ GitHub Secrets set up for CI/CD
- ✅ Frontend tests passing in GitHub Actions
- ✅ Backend Lambda deployed (with S3 upload fix for large packages)
- ✅ Native iOS project generated via prebuild (ios/ios/ directory)
- ✅ Dependencies updated for Node 22 + Expo SDK 51
- ✅ CocoaPods installed (rbenv issue resolved via global ruby 3.3.9)
- ✅ **npx expo run:ios completed successfully (exit code 0)** - Build compiled all React Native dependencies without errors

**What Needs Fixing:**
- ⏳ **Verify app is running on simulator screen** (build completed but needs visual/runtime verification)
- ⏳ Test SignIn screen renders without C++ crash
- ⏳ Simulator connectivity via QR code times out (NSPOSIXErrorDomain code=60)
- 📌 Test end-to-end auth flow with Firebase
- 📌 Verify app can connect to Lambda backend API

---

## 📖 Documentation & Memory

**Progress Memory** (persistent across sessions):
- **Path:** `/Users/charlieknight/.claude/projects/-Users-charlieknight/memory/covey_wbs_progress.md`
- **Auto-loaded:** Claude Code reads this at session start
- **Contains:** Full WBS status, branches, files, infrastructure details

**Complete Documentation** (all in covey repo - `/covey/docs/`):

**Project Management:**
- `docs/pm/wbs.md` - Work Breakdown Structure
- `docs/pm/charter.md` - Project Charter
- `docs/pm/schedule.md` - Timeline and Milestones
- `docs/pm/risk-register.csv` - Risk Register

**Architecture & Design:**
- `docs/arch/` - System architecture, API specs, data models, ADRs
- `docs/arch/api/openapi.yaml` - API specification
- `docs/arch/data-model/` - ERD and Firebase schema

**Requirements:**
- `docs/req/` - Use cases, scenarios, activity diagrams

**Security:**
- `docs/security/` - Threat model, auth design, security checklist

**Testing & Quality:**
- `docs/test/` - Test strategy
- `docs/quality/` - Quality model and gates

**UX & Design:**
- `docs/ux/` - Personas, journeys, prototypes, design tokens

**DevOps & Business Analysis:**
- `docs/devops/` - Deployment and infrastructure docs
- `docs/ba/` - Business analysis docs

**SDLC State:**
- `docs/sdlc.state.json` - Planning state tracking

All 39 docs files now in covey project for easy access!

---

## 📝 Latest Session Summary (2026-06-29)

### 🎉 BREAKTHROUGH: iOS App Running Without Crashes!

**Current Status:** ✅ App successfully running on iPhone 16 Pro simulator (PID: 92982)

1. ✅ **iOS app now running on simulator (Process ID: 75818)**
   - `npx expo run:ios` build completed with exit code 0
   - All React Native & CocoaPods dependencies compiled without errors
   - App installed and launched on iPhone 16 Pro simulator

2. ✅ **Fixed DOMRectReadOnly polyfill with correct Babel handling**
   - **Initial Problem:** Babel's import hoisting was placing ES6 imports above the polyfill
   - **Root Cause:** Firebase SDK was initializing before DOMRectReadOnly was defined
   - **Error Encountered:** `ReferenceError: Property 'DOMRectReadOnly' doesn't exist`
   - **Solution:**
     - Created separate `ios/dom-polyfill.js` file
     - Use `require()` in entry point (CommonJS not subject to Babel hoisting)
     - Ensures polyfill executes FIRST, then all other modules initialize
   - **Result:** ✅ JavaScript initialization now succeeds

3. ✅ **Resolved compiler warnings**
   - Removed: `ld: ignoring duplicate libraries: '-lc++'`
   - Now building with 0 errors, 0 warnings

### Status:
- **Build:** ✅ Successful (0 errors, 0 warnings)
- **JavaScript:** ✅ Firebase SDK initializes without errors
- **App Launch:** ✅ Running on simulator (PID 75818)
- **Polyfill:** ✅ Properly preventing Babel hoisting

### Testing Next:
- Verify app UI renders (SignIn screen should be visible)
- Test Firebase auth flow
- Test API connectivity to Lambda backend
- Verify navigation works

### Key Files Modified:
- `ios/dom-polyfill.js` — NEW: Separate polyfill file (CommonJS)
- `ios/index.js` — UPDATED: Use require() for polyfill, then import modules

### Notes for Next Session:
- Branch: `feature/expo-local-startup` (active)
- **CRITICAL:** Do NOT move polyfill back to index.js (Babel will hoist it)
- App is currently running - can now test end-to-end workflows
- Next priority: Verify SignIn screen renders, test Firebase auth

---

## 💡 Remember for Next Session

Before diving in, read the memory file to understand:
1. What's been completed
2. What PR #16 is about (classpath fix)
3. How to test the current Lambda state
4. What specifically needs to be fixed

**The memory is your guide - reference it first!**
