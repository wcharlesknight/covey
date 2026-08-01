# Covey Project Progress

## 📍 Quick Status

**Current Branch:** `feature/places-api-new` (PR #56 open, pending manual validation)  
**Last Updated:** 2026-07-19  
**Next Action:** Validate Places API (New) works by triggering weekly job manually, then merge PR #56

### ⚠️ How to Resume Next Session
1. Trigger weekly Lambda job, check CloudWatch logs for venues (not REQUEST_DENIED)
2. If working: merge PR #56 (`feature/places-api-new`)
3. Then: WBS 1.4.5 push notifications (FCM registration, APNs, deep link)

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
| SDK 54 upgrade | RN 0.81.5, React 19, Expo 54, Swift AppDelegate | #49 |
| Weekly job env fix | `GOOGLE_PLACES_API_KEY` added to CI/CD Lambda config | #54 |
| Error logging | Places API error detail (status + error_message) | #55 |

---

## 🔄 In Progress

### Places API (New) Migration — PR #56 open
**Branch:** `feature/places-api-new`  
**Status:** Awaiting manual validation  
**What changed:** `GooglePlacesClient.java` migrated from legacy `nearbysearch` GET to `places:searchNearby` POST with `X-Goog-Api-Key`/`X-Goog-FieldMask` headers. Response field names updated: `displayName.text`, `formattedAddress`, `id`, `userRatingCount`.  
**Why needed:** Legacy Places API never enabled on GCP project — only Places API (New) is active.

**To validate:**
```bash
# Invoke weekly job manually
aws lambda invoke \
  --function-name covey-weekly-spot-dev \
  --payload '{"triggerType":"WEEKLY_SELECTION"}' \
  --region us-west-2 /tmp/out.json && cat /tmp/out.json

# Follow logs
aws logs tail /aws/lambda/covey-weekly-spot-dev --follow --region us-west-2
```
Look for: venue names logged for Seattle, Tacoma, Bainbridge Island (not REQUEST_DENIED).

---

## 🎯 Next WBS Items

### WBS 1.4.5 — Push Notifications
- FCM token registration on app launch
- APNs entitlements in app.json
- Deep link from notification tap to HomeScreen
- Lambda `NotificationDeliveryHandler` already wired

### After push notifications: WBS 1.5 (testing/QA pass)

---

## 🏗️ Infrastructure Reference

| Component | Value |
|-----------|-------|
| Lambda | `covey-weekly-spot-dev` (java17, 512MB) |
| API Gateway | `https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev` |
| Firebase Project | `covey-76e19` |
| Firestore Rules | `/firestore.rules` (row-level, create/update split) |
| CI/CD | `.github/workflows/deploy-nonprod.yml` |

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
