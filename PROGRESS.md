# Covey Project Progress

## 📍 Quick Status

**Current Branch:** `feature/testflight-submit-fix` (PR #64 open)
**Last Updated:** 2026-08-15
**Next Action:** Debug loading screen hang in TestFlight (works fine in simulator, hangs on real device via TestFlight)

### ⚠️ How to Resume Next Session
1. Merge PR #64 (`feature/testflight-submit-fix`) if not already merged
2. Sync to main: `git checkout main && git pull origin main`
3. Create new branch: `feature/testflight-loading-fix`
4. Investigate loading screen hang — `SplashScreen.preventAutoHideAsync()` / `SplashScreen.hideAsync()` likely the culprit in production builds; also check `initializeFirebase()` and `initializeAuth()` timing

---

## ✅ Completed WBS Items (all PRs merged to main)

| WBS | Description | PR |
|-----|-------------|-----|
| 1.2 | Infrastructure: Lambda, API Gateway, Firebase, Secrets Manager | #1–18 |
| 1.3 | Backend: all handlers, auth, weekly job, smoke tests | #19–28 |
| 1.4.1.1–4 | iOS auth screens: sign-in, sign-up, Apple, Google | #45–48 |
| 1.4.1.5 | Firebase auth persistence via AsyncStorage | #50 |
| 1.4.1.6 | Session restoration on app launch | #50 |
| 1.4.1.7 | Sign-out flow cleanup | #52 |
| 1.4.2 | City selection screen, profile city row, home city pill | #48, #51 |
| 1.4.3.3–7 | Feed: maps link, RSVP counts, history cards, pull-to-refresh, empty state | #51 |
| 1.4.4.3–4 | Optimistic RSVP updates + disable past-week buttons | #53 |
| 1.4.5.1–5 | Push notifications: permission, Expo token registration, APNs refresh, tap handler, foreground display | #60 |
| 1.4.3.1–2 | SpotDetailScreen: venue card, RSVP, deep link from notification tap + email | #60 |
| 1.3.6a.1–3,.5 | Email deliverability: coveyspot.app domain, SPF/DKIM/DMARC, sender update, SES production request | #61 |
| 1.4.7.6 | EAS CI/CD pipeline: build + submit to TestFlight on every merge to main | #62, #63, #64 |
| 1.6.1.1 | First build successfully submitted to and available in TestFlight | #64 |
| 1.8.3.1 | CI Node bumped from 18 → 22 | #63 |
| SDK 54 upgrade | RN 0.81.5, React 19, Expo 54, Swift AppDelegate | #49 |
| Weekly job env fix | `GOOGLE_PLACES_API_KEY` added to CI/CD Lambda config | #54 |
| Error logging | Places API error detail (status + error_message) | #55 |

---

## 🔄 In Progress

### TestFlight loading screen hang — PR #64 open, not yet debugged
**Branch:** `feature/testflight-submit-fix` (contains icon fix + encryption compliance)
**Status:** Build is live in TestFlight but app hangs on loading screen on real device
**Suspected cause:** `SplashScreen.preventAutoHideAsync()` / `SplashScreen.hideAsync()` behaving differently in production builds vs simulator. May also be `initializeFirebase()` or `initializeAuth()` timing on a cold start.

**What to investigate:**
- `App.tsx` bootstrap: `initializeFirebase()` → `initializeAuth()` → `SplashScreen.hideAsync()` — does any step throw silently in production?
- The `isInitializing` state: if it never flips to `false`, loading screen stays forever
- Add try/catch logging around bootstrap steps and watch Expo logs

---

## 🎯 Next WBS Items

### Fix TestFlight loading screen hang
- Debug `App.tsx` bootstrap sequence in production build
- Likely need to add timeout or error fallback to `SplashScreen.hideAsync()`

### WBS 1.6.1.2 — Collect beta feedback (1 week minimum)
- Add yourself and any testers to TestFlight internal group

### WBS 1.3.6a.4 — BIMI email icon (inbox brand logo)
- Requires DMARC + verified logo — pending

### WBS 1.3.6a.6 — Mail Tester validation
- Test deliverability score at mail-tester.com

---

## 🏗️ Infrastructure Reference

| Component | Value |
|-----------|-------|
| Lambda | `covey-weekly-spot-dev` (java17, 512MB) |
| API Gateway | `https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev` |
| Firebase Project | `covey-76e19` |
| Firestore Rules | `/firestore.rules` (row-level, create/update split) |
| CI/CD | `.github/workflows/deploy-nonprod.yml` |
| EAS Project | `36924851-085c-4e48-8e07-94098d4b6b7b` |
| App Store Connect App ID | `6801878266` |
| Bundle ID | `com.covery.app` |
| SES Sender | `hello@coveyspot.app` |

### CI/CD — Every merge to main triggers:
1. **Backend** → Gradle build → S3 → Lambda update → smoke tests
2. **iOS** → EAS cloud build → `eas submit` → App Store Connect TestFlight

### Supported Cities (iOS + backend)
- Seattle (47.6062, -122.3321)
- Tacoma (47.2529, -122.4443)
- Bainbridge Island (47.6262, -122.5209)

---

## 🔑 Key Technical Decisions

| Decision | Detail |
|----------|--------|
| Firebase imports | `@firebase/app`, `@firebase/auth`, `@firebase/firestore` direct (not `firebase`) |
| Metro config | `unstable_enablePackageExports = false` (fixes Firebase split-module bug) |
| Auth persistence | `getReactNativePersistence(AsyncStorage)` + module augmentation `.d.ts` |
| Auth state | `onAuthStateChanged` is sole setter — eliminates CityPicker flash race |
| User provisioning | Lambda `GET /me` creates Firestore doc via Admin SDK (client never writes) |
| Lambda deploys | CI/CD only — never `aws lambda update-function-code` directly |
| Push notifications | Expo push tokens → Expo push API (not FCM directly) |
| Weekly spot doc IDs | `city + "_" + weekId` format (e.g. `Tacoma_2026-W31`) |
| App icon | Must be RGB, no alpha channel (Apple ITMS-90717 requirement) |
| Export compliance | `ITSAppUsesNonExemptEncryption = false` in Info.plist (standard HTTPS only) |
| EAS credentials | Distribution cert + provisioning profile stored in Expo credential storage; ASC API key (F9H97W4QT7) also stored via `eas credentials` |
