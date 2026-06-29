# Covey Project Progress & Memory

## 📍 Quick Status

**Current WBS:** 2.1 - iOS Firebase Configuration & Local Testing  
**Status:** Firebase configured, GitHub Secrets added, iOS prebuild completed, `expo run:ios` build successful (exit 0)  
**Branch:** `feature/expo-local-startup` (debugging local startup, native build artifacts generated)

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

## 📝 Latest Session Summary (2026-06-28)

### Accomplished:
1. ✅ **Successfully built iOS app using `npx expo run:ios`**
   - Xcode compiled all React Native dependencies (React-Core, Yoga, Hermes, etc.)
   - Build signed with Apple Development team certificate (7RR58R22AF)
   - CocoaPods installed 73 dependencies successfully
   - Exit code 0 indicates successful build completion
   - Build artifacts in `ios/build/` directory

2. ✅ **Resolved CocoaPods PATH shadowing issue**
   - Set rbenv global ruby version to 3.3.9 before pod install
   - Prevented PATH conflicts with Homebrew cocoapods installation

3. ✅ **Generated native iOS project structure**
   - `ios/ios/` directory created with Xcode project files
   - Proper CocoaPods integration configured
   - All native dependencies compiled and linked

### Status:
- **Build:** ✅ Successful (npx expo run:ios completed)
- **Compilation:** ✅ All dependencies compiled without reported errors
- **Next Action:** Verify app is running/visible on iPhone 16 Pro simulator
- **Test Plan:** Check SignIn screen renders, no C++ exceptions, basic UI interaction

### Notes for Next Session:
- Branch: `feature/expo-local-startup` is active and up to date with origin
- Untracked build artifacts: `.claude/`, `ios/.expo/`, `ios/assets/`, `ios/ios/` (prebuild output)
- If app is running: test Firebase auth flow, API connectivity, navigation
- If app crashed: check Xcode console logs for C++ exception details and build/launch errors
- QR code issue (NSPOSIXErrorDomain code=60) is a separate CoreSimulator timeout problem

---

## 💡 Remember for Next Session

Before diving in, read the memory file to understand:
1. What's been completed
2. What PR #16 is about (classpath fix)
3. How to test the current Lambda state
4. What specifically needs to be fixed

**The memory is your guide - reference it first!**
