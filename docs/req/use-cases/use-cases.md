# Location Notifier — Use Case Descriptions

**Status**: Draft
**Author**: Domain Analyst
**Date**: 2026-06-27
**Source of truth**: Confirmed scenarios — Maya (weekly notification), James (city arrival), Priya (sign-up)

---

## UC-001: Register / Sign In (Apple or Google)

**Actor(s):** User (primary), Firebase Auth (secondary)

**Goal:** A new or returning user authenticates with the app in under 90 seconds so they can receive weekly venue notifications and track social activity.

**Preconditions:**
- User has an iOS device with the app installed
- User has an Apple ID or Google account

**Postconditions:**
- User has an authenticated session stored on the device
- User profile record exists in Firebase (created on first sign-in)
- User is eligible to receive the next weekly push notification

**Main Success Scenario:**
1. User opens the app for the first time (or after session expiry)
2. App displays sign-in screen with "Continue with Apple" and "Continue with Google" buttons
3. User taps "Continue with Apple"
4. iOS presents the Apple Sign In sheet; user authenticates with Face ID / Touch ID
5. App receives an Apple ID credential token
6. App sends the token to the Backend Service for verification
7. Backend verifies the token with Apple's auth endpoint and creates or retrieves the user profile in Firebase
8. Backend returns a session token to the app
9. App stores the session token securely in iOS Keychain
10. App navigates the user to the main screen (this week's spot or onboarding)

**Extensions:**
- 4a. User cancels Apple Sign In sheet: App returns to sign-in screen with no error; user can retry
- 7a. Token verification fails (network error): Backend returns 401; app displays "Sign in failed — please try again" and allows retry
- 7b. New user: Backend creates a new profile record in Firebase before returning the session token; flow continues at step 8
- 9a. Keychain write fails: App falls back to in-memory session for current session only; user will need to sign in again on next launch

**Related:** UC-002 (follows immediately after first sign-in), UC-003 (user must be registered to receive push)

---

## UC-002: Grant Location Permission

**Actor(s):** User (primary), iOS Location Services (secondary)

**Goal:** The user grants the app permission to read their device location so the backend can detect their city and show the correct weekly venue.

**Preconditions:**
- User has completed UC-001 (authenticated session exists)
- App has not previously been granted location permission on this device

**Postconditions:**
- App holds "When In Use" or "Always" location permission
- App begins sending periodic location updates to the Backend Service (see UC-009)
- User's city is known to the backend for venue selection

**Main Success Scenario:**
1. On first sign-in (or if permission was previously denied and revoked), app displays an in-app explanation screen: "We use your location to find the best local spot each week. We never share your exact position."
2. User taps "Allow Location Access"
3. iOS presents the system location permission alert
4. User selects "Allow While Using App"
5. App begins reading device location via CoreLocation
6. App sends the first location update to the Backend Service (UC-009)
7. App confirms to the user that city detection is active

**Extensions:**
- 4a. User selects "Don't Allow": App displays a polite message explaining that location is needed for city-specific venues; provides a button to open iOS Settings; user can grant permission later
- 4b. User selects "Allow Once": App reads location for current session only; on next launch, app prompts again at step 1
- 5a. Location unavailable (airplane mode, GPS off): App retries silently; no error shown to user; last known city is used by backend if available

**Related:** UC-001 (must precede), UC-009 (location updates flow)

---

## UC-003: Receive Weekly Push Notification

**Actor(s):** User (primary), Backend Service / APNs (secondary)

**Goal:** Every Thursday the user receives a push notification on their iPhone announcing that week's curated local spot, so they can plan a visit with friends.

**Preconditions:**
- User has completed UC-001 (authenticated) and UC-002 (location granted)
- User has granted iOS push notification permission
- Backend has completed UC-008 (weekly spot selected and written to Firebase)
- User's device token is registered with APNs

**Postconditions:**
- Push notification appears on the user's lock screen / notification centre
- Tapping the notification opens the app and navigates to the spot detail view (UC-004)
- Notification delivery is logged in Firebase for audit

**Main Success Scenario:**
1. Thursday scheduled Lambda (UC-008) completes successfully and writes the weekly spot to Firebase
2. Lambda sends a push notification via APNs to all eligible users in the detected city
3. Notification payload contains: venue name, city, one-line tagline, and a deep link to UC-004
4. iOS displays the notification as a banner: "This week's Seattle spot: Espresso Vivace — Thursday is here"
5. User receives the notification and taps it
6. App opens and displays the spot detail screen (UC-004)

**Extensions:**
- 2a. APNs returns an invalid device token: Backend removes the stale token from Firebase; no notification sent to that device; user will re-register on next app open
- 2b. Lambda fails before sending push: Retry mechanism re-runs the push step up to 3 times; if all fail, fallback email is sent (part of UC-008)
- 5a. User does not tap notification: App state is unchanged; user can still open the app manually and see the spot (UC-004)

**Related:** UC-008 (produces the notification), UC-004 (destination after tap)

---

## UC-004: View This Week's Spot Details

**Actor(s):** User (primary), Firebase DB (secondary)

**Goal:** The user sees the full details of this week's curated venue — name, address, description, hours, and a link to maps — so they can decide whether and when to visit.

**Preconditions:**
- User is authenticated (UC-001)
- Backend has completed UC-008 for the current week (spot exists in Firebase for the user's city)

**Postconditions:**
- User has seen all relevant details for the week's spot
- App has recorded that the user viewed the spot (for social signal tracking)

**Main Success Scenario:**
1. User opens the app (via notification tap or direct launch)
2. App reads the user's city from last known location
3. App calls Backend Service to fetch the current week's spot for that city
4. Backend reads the current-week record from Firebase and returns: venue name, address, Google Places photo, category, hours, short description, and "X friends have visited"
5. App displays the spot detail screen with all fields populated
6. User can tap "Open in Maps" to launch Apple Maps at the venue address
7. User can tap "I'm Here" to check in (UC-007)
8. User can scroll down to see the 4-week history (UC-005)

**Extensions:**
- 3a. No spot exists yet for this week: App displays "Check back Thursday for this week's spot" with the last known spot shown as context
- 3b. Network request fails: App shows cached last-fetched spot with a "Last updated [date]" label; user can pull to refresh
- 3c. User's city is unknown (location permission not granted): App displays city-selector prompt; user types or selects a city manually

**Related:** UC-003 (entry point via notification), UC-005 (history below spot), UC-006 (social signals on this screen), UC-007 (checkin button)

---

## UC-005: View 4-Week History

**Actor(s):** User (primary), Backend Service / Firebase DB (secondary)

**Goal:** The user sees the last four weeks' curated spots for their city so they can revisit favourites or share past spots with friends.

**Preconditions:**
- User is authenticated (UC-001)
- At least one prior week's spot exists in Firebase for the user's city

**Postconditions:**
- User has seen a scrollable list of up to four past weekly spots with venue name, date, visit count, and comment count

**Main Success Scenario:**
1. User scrolls down on the spot detail screen (UC-004) or taps "History" in the navigation
2. App calls Backend Service with user city and a request for the last four weeks
3. Backend calls UC-010 (Fetch User's 4-Week History)
4. Backend returns an ordered list of up to four records: week start date, venue name, address, photo thumbnail, friend visit count, total checkin count
5. App renders the list as a scrollable card stack, newest first
6. User taps a past venue card to see its full detail (read-only; same layout as UC-004 but with historical data)
7. User can share the venue link from the detail screen

**Extensions:**
- 3a. Fewer than four weeks of data exist (new user or new city): App shows only the available weeks; no placeholder cards shown
- 3b. Network request fails: App displays previously cached history with a staleness warning

**Related:** UC-004 (parent screen), UC-010 (backend operation), UC-006 (social signals on each history card)

---

## UC-006: See Social Signals (Friends Visited, Checkin Count)

**Actor(s):** User (primary), Firebase DB (secondary)

**Goal:** The user sees how many friends or colleagues have visited each weekly spot, and how many total people checked in, so they feel social motivation to attend or revisit.

**Preconditions:**
- User is authenticated (UC-001)
- At least one checkin record exists in Firebase for the spot being viewed
- User has at least one social connection in the app (friends / contacts; optional for MVP — shows total count if no friends)

**Postconditions:**
- User can see a numeric count ("8 people checked in", "3 friends visited") on the spot card

**Main Success Scenario:**
1. App is displaying the spot detail screen (UC-004) or a history card (UC-005)
2. App requests social signal data from Backend Service for the venue and week
3. Backend queries Firebase for checkin records associated with the venue and week window
4. Backend returns: total checkin count, and (if friend graph is available) count of the user's friends who checked in
5. App displays inline on the spot card: "12 people checked in · 3 friends"
6. User can tap the signal to see a list of friends who visited (first names only, privacy-safe)

**Extensions:**
- 4a. No checkins for this venue yet: App displays "Be the first to check in"
- 4b. Friend graph not available (MVP): App displays total checkin count only — no friend-specific breakdown
- 6a. User taps to see friend list but has no friends in app: Prompt to invite contacts

**Related:** UC-004, UC-005 (displayed within these screens), UC-007 (checkins feed into this signal)

---

## UC-007: Checkin to Current Spot (Optional)

**Actor(s):** User (primary), Firebase DB (secondary)

**Goal:** The user records their physical presence at this week's spot so they appear in the social signals for their friends and can see who else is currently there.

**Preconditions:**
- User is authenticated (UC-001)
- User has location permission (UC-002)
- User's current GPS location is within a reasonable radius of the venue (configurable; default 500m)
- This week's spot exists in Firebase (UC-008 completed)

**Postconditions:**
- Checkin record written to Firebase: user ID, venue ID, week ID, timestamp, city
- Social signal count for this venue incremented by 1
- User sees "X people are here now" on the spot screen

**Main Success Scenario:**
1. User is at (or near) the current week's spot and taps "I'm Here" on the spot detail screen (UC-004)
2. App reads the user's current GPS location
3. App calls Backend Service with user ID, venue ID, and current location
4. Backend validates that the user's location is within 500m of the venue coordinates
5. Backend writes a checkin record to Firebase
6. Backend queries all active checkins at this venue within the last 2 hours
7. Backend returns the live attendee count to the app
8. App updates the spot screen: "8 people are here now" and shows the user's own checkin as confirmed

**Extensions:**
- 4a. User is more than 500m from the venue: Backend returns a "too far" error; app displays "You need to be at the venue to check in — get closer and try again"
- 4b. User already checked in today at this venue: Backend returns a duplicate error; app displays "You're already checked in here today" with the original checkin time
- 2a. Location unavailable: App displays "Location unavailable — please enable location services to check in" and surfaces the iOS settings link

**Related:** UC-002 (location required), UC-004 (entry point), UC-006 (social signals updated by this)

---

## UC-008: Run Weekly Scheduled Job (Select Spot, Send Push + Email)

**Actor(s):** Backend Service / Java Lambda (primary), Google Places API (secondary), Firebase DB (secondary), Admin/Curation (secondary)

**Goal:** Every Thursday, the Lambda automatically selects one curated venue per active city, stores it in Firebase, and notifies all eligible users via push and email.

**Preconditions:**
- Scheduled trigger fires (e.g., EventBridge cron: Thursday 08:00 local city time)
- At least one user with a known city and valid device token exists in Firebase
- Google Places API key is available and under quota
- Last 4 weeks of venue history exists in Firebase (used to prevent repeats)

**Postconditions:**
- A new weekly spot record is written to Firebase for each active city
- All eligible users in each city have received a push notification via APNs
- All eligible users with an email address have received an email digest
- Job execution result (success / partial failure) is logged to Firebase

**Main Success Scenario:**
1. EventBridge triggers the WeeklySpotLambda on schedule
2. Lambda reads all distinct active cities from Firebase (cities where at least one user checked location in the last 14 days)
3. For each city, Lambda reads the last 4 weeks of selected venues from Firebase (to build an exclusion list)
4. Lambda calls Google Places API: `nearbySearch` with the city's centre coordinates, type = `cafe|restaurant|bar`, ranked by `prominence`
5. Google Places API returns up to 20 candidate venues
6. Lambda filters out any venue in the 4-week exclusion list
7. Lambda applies curation rules (Admin-configured): minimum rating ≥ 4.0, minimum review count ≥ 50, open on weekdays
8. Lambda selects the top-ranked remaining venue as this week's spot
9. Lambda writes the selected venue to Firebase: city, week ID (ISO week number), venue ID, venue name, address, Google Places photo URL, rating, hours
10. Lambda fetches all eligible users for the city from Firebase (authenticated, location shared, push token present)
11. Lambda sends a push notification to each user's APNs device token: "This week's [city] spot: [venue name]"
12. Lambda sends an email digest to each user with an email address on file
13. Lambda writes a job completion log entry to Firebase with: timestamp, city, venue selected, push count sent, email count sent, any errors

**Extensions:**
- 5a. Google Places API returns an error (quota exceeded, network timeout): Lambda logs the error, skips this city for this week, and sends an admin alert via email; all other cities continue processing
- 6a. All candidate venues are in the 4-week exclusion list: Lambda expands search radius by 50% and retries once; if still no candidates, selects the least-recently-used venue from the exclusion list with a "returning favourite" label
- 8a. Admin has manually overridden the venue for this city via the admin panel: Lambda uses the override venue and skips steps 4–7 for that city
- 11a. APNs returns invalid device token: Lambda removes the token from Firebase and skips that user; all other users still receive the push

**Related:** UC-003 (push delivery to user), UC-009 (location data used at step 2), UC-010 (exclusion list read at step 3)

---

## UC-009: Receive Location Updates from Clients

**Actor(s):** Backend Service / Java Lambda (primary), Firebase DB (secondary), iOS App (secondary)

**Goal:** The backend receives periodic location updates from the iOS app and stores the user's most recent city so the weekly job (UC-008) can assign users to the correct city.

**Preconditions:**
- User is authenticated (UC-001) and has granted location permission (UC-002)
- iOS app is running in foreground or background with "Always" location permission

**Postconditions:**
- Firebase contains an updated `lastKnownLocation` record for the user: latitude, longitude, city name, timestamp
- If the detected city differs from the user's previous city, Firebase is updated and the user will receive the next week's notification for the new city

**Main Success Scenario:**
1. iOS CoreLocation delivers a significant-location-change event to the app (approximately every 500m or on city change)
2. App sends a POST request to the Backend Service with: user ID (from session token), latitude, longitude, device timestamp
3. Backend validates the session token
4. Backend performs a reverse-geocode (using Google Places or a system geocoder) to resolve latitude/longitude to a city name
5. Backend writes or updates the user's `lastKnownLocation` document in Firebase: city, coordinates, timestamp
6. If the city has changed since the last update, Backend also updates the user's `homeCity` field in their profile document
7. Backend returns HTTP 200 to the app; no UI change is shown to the user

**Extensions:**
- 3a. Session token invalid or expired: Backend returns 401; app prompts user to re-authenticate (UC-001)
- 4a. Reverse geocode fails: Backend stores raw coordinates without a city name; UC-008 will skip this user until city is resolved on the next successful update
- 1a. App does not have "Always" location permission (only "When In Use"): Location updates are only sent while the app is open; city detection may be delayed; no error is shown

**Related:** UC-002 (permission prerequisite), UC-008 (consumes city data at step 2)

---

## UC-010: Fetch User's 4-Week History

**Actor(s):** Backend Service / Java Lambda (primary), Firebase DB (secondary)

**Goal:** The backend retrieves the ordered list of the four most recent weekly spots for a given city so the iOS app can display the history panel (UC-005) and the weekly job can check for duplicates (UC-008).

**Preconditions:**
- Caller has a valid session token (for client-initiated calls) OR is the internal weekly job Lambda (for UC-008)
- At least one weekly spot record exists in Firebase for the requested city

**Postconditions:**
- Caller receives an ordered list of up to 4 weekly spot records: week ID, venue name, address, photo URL, checkin count, friend visit count

**Main Success Scenario:**
1. Caller (iOS app or internal Lambda) sends a request to the Backend Service with: city name, requesting user ID (for friend signal enrichment), page size = 4
2. Backend validates the session token (client calls) or verifies the internal Lambda invocation signature
3. Backend queries Firebase collection `weeklySpots` where `city == [city]`, ordered by `weekId` descending, limit 4
4. Firebase returns up to 4 records
5. Backend enriches each record with checkin counts from the `checkins` collection
6. Backend optionally enriches with friend visit counts if the user's friend graph is available
7. Backend returns the enriched list to the caller as a JSON array

**Extensions:**
- 3a. No records found for this city: Backend returns an empty array with HTTP 200; caller handles the empty state gracefully
- 3b. Firebase query times out: Backend returns HTTP 503; iOS app displays cached history with a staleness warning; Lambda (UC-008) treats this as a failed exclusion check and logs the error
- 6a. Friend graph service unavailable: Backend returns records without friend enrichment; social signal shows total count only (see UC-006 Extension 4b)

**Related:** UC-005 (client display), UC-008 (duplicate exclusion check), UC-006 (social signals derived from this data)
