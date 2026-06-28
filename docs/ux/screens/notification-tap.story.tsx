/**
 * Covey — Notification Deep Link Screen
 *
 * Documents and simulates the complete push notification → app experience.
 * A user receives a weekly Thursday notification with the new venue name and
 * taps it. The app must navigate directly to the Home Feed with the current
 * spot pre-scrolled into view.
 *
 * Two launch contexts:
 *   Cold launch  — App was not running. OS relaunches it with the notification payload.
 *   Warm launch  — App was in background. OS foregrounds it with the payload.
 *
 * Notification payload (APNs):
 * {
 *   "aps": {
 *     "alert": {
 *       "title": "This week's Seattle spot",
 *       "body": "The Mayflower — Tap to RSVP"
 *     },
 *     "badge": 1,
 *     "sound": "default"
 *   },
 *   "covey": {
 *     "type": "weekly_spot",
 *     "venueId": "mayflower-001",
 *     "cityId": "seattle"
 *   }
 * }
 *
 * Deep link handling:
 *   - URL scheme: covey://feed?city=seattle&venueId=mayflower-001
 *   - Both Universal Links and custom scheme supported
 *   - Navigation: Replace root navigator with Feed screen (no back button)
 *
 * States documented:
 *   NotificationBanner   — Lock screen / banner preview (iOS system UI mock)
 *   ColdLaunchSplash     — Splash/loading between notification tap and feed load
 *   WarmLaunchFeed       — Instant transition to feed (app was backgrounded)
 *   FeedHighlighted      — Feed loaded with venue card visually emphasised
 *
 * Personas:
 *   Maya (commuter)  — primary; taps from lock screen on commute
 *   James (traveler) — secondary; notification lands in a new timezone
 *   Priya (newcomer) — awaiting first spot; most excited to tap
 *
 * WCAG 2.1 AA:
 *   - Notification banner text readable at 12pt minimum
 *   - Loading state announced via accessibilityLiveRegion
 *   - Deep-linked screen has correct heading hierarchy
 */

import React, { useState, useEffect, useRef } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import {
  View,
  Text,
  TouchableOpacity,
  Animated,
  Easing,
  StyleSheet,
  SafeAreaView,
  ActivityIndicator,
} from 'react-native';

// ─── Types ────────────────────────────────────────────────────────────────────

type LaunchState =
  | 'notification-banner'  // iOS lock screen banner visible
  | 'cold-splash'          // App launching from scratch
  | 'warm-transition'      // App foregrounding (very brief)
  | 'feed-loaded';         // Feed visible, venue card highlighted

interface NotificationPayload {
  title: string;
  body: string;
  venueName: string;
  cityName: string;
  venueId: string;
}

interface NotificationTapScreenProps {
  /** Controlled launch state for documentation */
  launchState?: LaunchState;
  /** Notification payload */
  payload?: NotificationPayload;
  /** True = cold launch sequence; False = warm launch (skip splash) */
  isColdLaunch?: boolean;
  /** Auto-advance through states (for interactive demo) */
  autoPlay?: boolean;
  onTapNotification?: () => void;
}

// ─── Sample Data ──────────────────────────────────────────────────────────────

const SAMPLE_PAYLOAD: NotificationPayload = {
  title: "This week's Seattle spot",
  body: "The Mayflower — Tap to RSVP",
  venueName: "The Mayflower",
  cityName: "Seattle",
  venueId: "mayflower-001",
};

// ─── Design Tokens ────────────────────────────────────────────────────────────

const colors = {
  brandPurple:    '#6B4CE6',
  brandPurpleL:   '#F0EDFB',
  white:          '#FAFAF9',
  cardBg:         '#F5F4F2',
  textPrimary:    '#1C1B18',
  textSecondary:  '#8A8782',
  textOnPurple:   '#FFFFFF',
  border:         '#E8E7E4',
  // iOS notification mock colors
  notifBg:        'rgba(242,242,247,0.92)',  // iOS frosted glass approximation
  notifBorder:    'rgba(210,210,220,0.6)',
  notifTitle:     '#000000',
  notifBody:      '#3C3C43',
  // Highlight ring around venue card
  highlightRing:  '#6B4CE6',
  successBase:    '#22C55E',
  successLight:   '#D1FAE5',
};

// ─── Sub-components ───────────────────────────────────────────────────────────

/**
 * Mock iOS notification banner — approximates the system UI for documentation.
 * Not a replacement for actual notification previews on device.
 */
function NotificationBanner({
  payload,
  onTap,
  visible,
}: {
  payload: NotificationPayload;
  onTap?: () => void;
  visible: boolean;
}) {
  const slideAnim = useRef(new Animated.Value(-120)).current;

  useEffect(() => {
    Animated.timing(slideAnim, {
      toValue: visible ? 0 : -120,
      duration: 350,
      easing: Easing.bezier(0, 0, 0.2, 1),
      useNativeDriver: true,
    }).start();
  }, [visible]);

  return (
    <Animated.View
      style={[styles.notifBanner, { transform: [{ translateY: slideAnim }] }]}
      accessibilityRole="alert"
      accessibilityLabel={`Push notification: ${payload.title}. ${payload.body}. Tap to open.`}
    >
      <View style={styles.notifBannerInner}>
        {/* App icon placeholder */}
        <View style={styles.notifAppIcon} accessibilityElementsHidden>
          <View style={styles.notifAppIconCircles}>
            <View style={[styles.notifCircle, { backgroundColor: '#BDB0EF', left: 0 }]} />
            <View style={[styles.notifCircle, { backgroundColor: '#6B4CE6', left: 6, zIndex: 1 }]} />
            <View style={[styles.notifCircle, { backgroundColor: '#8B6EE8', left: 12 }]} />
          </View>
        </View>

        {/* Notification text */}
        <View style={styles.notifTextBlock}>
          <View style={styles.notifHeader}>
            <Text style={styles.notifAppName} aria-hidden>COVEY</Text>
            <Text style={styles.notifTime} aria-hidden>now</Text>
          </View>
          <Text style={styles.notifTitle}>{payload.title}</Text>
          <Text style={styles.notifBody} numberOfLines={2}>{payload.body}</Text>
        </View>
      </View>

      {/* Tap area */}
      <TouchableOpacity
        style={StyleSheet.absoluteFill}
        onPress={onTap}
        accessibilityRole="button"
        accessibilityLabel="Tap to open Covey and view this week's spot"
        activeOpacity={0.85}
      />
    </Animated.View>
  );
}

/**
 * Cold launch splash — shown while the app initialises from scratch.
 * Maximum duration: ~1.5s on modern devices.
 */
function ColdLaunchSplash() {
  return (
    <SafeAreaView style={styles.splashScreen}>
      <View
        style={styles.splashContent}
        accessibilityLiveRegion="polite"
        accessibilityLabel="Covey is loading. Just a moment."
      >
        {/* Logo */}
        <View style={styles.splashLogoRow}>
          <View style={[styles.splashCircle, { backgroundColor: '#BDB0EF', left: 0 }]} />
          <View style={[styles.splashCircle, { backgroundColor: '#6B4CE6', left: 20, zIndex: 1 }]} />
          <View style={[styles.splashCircle, { backgroundColor: '#8B6EE8', left: 40 }]} />
        </View>
        <Text style={styles.splashWordmark}>covey</Text>
        <ActivityIndicator
          size="small"
          color={colors.brandPurple}
          style={styles.splashSpinner}
        />
        <Text style={styles.splashSubtext}>Loading this week's spot…</Text>
      </View>
    </SafeAreaView>
  );
}

/**
 * Brief warm-launch transition indicator — app was in background,
 * foregrounding takes < 300ms so this is rarely visible.
 */
function WarmTransition() {
  const opacAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(opacAnim, {
      toValue: 1,
      duration: 200,
      useNativeDriver: true,
    }).start();
  }, []);

  return (
    <Animated.View style={[styles.warmOverlay, { opacity: opacAnim }]}>
      <Text style={styles.warmLabel} accessibilityLiveRegion="polite">
        Opening Covey…
      </Text>
    </Animated.View>
  );
}

/**
 * Simplified home feed with the venue card highlighted to show the
 * deep link destination. Full detail is in home-feed.story.tsx.
 */
function DeepLinkedFeed({
  payload,
  highlight,
}: {
  payload: NotificationPayload;
  highlight: boolean;
}) {
  const pulseAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (highlight) {
      Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1, duration: 800, useNativeDriver: false }),
          Animated.timing(pulseAnim, { toValue: 0, duration: 800, useNativeDriver: false }),
        ]),
        { iterations: 3 }
      ).start();
    }
  }, [highlight]);

  const borderColor = pulseAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['transparent', colors.highlightRing],
  });

  return (
    <SafeAreaView style={styles.feedScreen}>
      <View style={styles.feedContent}>
        {/* Screen header */}
        <Text style={styles.feedHeader} accessibilityRole="header">
          This week's spot in {payload.cityName}
        </Text>

        {/* Highlighted venue card */}
        <Animated.View
          style={[
            styles.feedVenueCard,
            highlight && { borderColor, borderWidth: 2 },
          ]}
          accessibilityLabel={`${payload.venueName} — this week's curated spot in ${payload.cityName}`}
        >
          {/* Photo placeholder */}
          <View style={styles.feedVenuePhoto}>
            <Text style={styles.feedVenuePhotoLabel} aria-hidden>[ Venue Photo ]</Text>
          </View>

          <View style={styles.feedVenueInfo}>
            <Text style={styles.feedVenueName}>{payload.venueName}</Text>
            <Text style={styles.feedVenueAddress}>620 Pine St, Seattle, WA 98101</Text>

            {/* RSVP row — simplified */}
            <View style={styles.feedRsvpRow}>
              <TouchableOpacity
                style={[styles.feedRsvpButton, styles.feedRsvpButtonYes]}
                accessibilityRole="button"
                accessibilityLabel="RSVP Yes"
              >
                <Text style={styles.feedRsvpLabelYes}>Yes</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.feedRsvpButton}
                accessibilityRole="button"
                accessibilityLabel="RSVP Maybe"
              >
                <Text style={styles.feedRsvpLabel}>Maybe</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.feedRsvpButton}
                accessibilityRole="button"
                accessibilityLabel="RSVP Nope"
              >
                <Text style={styles.feedRsvpLabel}>Nope</Text>
              </TouchableOpacity>
            </View>

            <Text style={styles.feedCounts}>24 confirmed · 11 interested · 5 not going</Text>
          </View>
        </Animated.View>

        {/* Deep link source indicator (dev/demo only) */}
        {highlight && (
          <View style={styles.deepLinkBadge}>
            <Text style={styles.deepLinkBadgeText}>
              Opened via notification · {payload.venueName}
            </Text>
          </View>
        )}
      </View>
    </SafeAreaView>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export function NotificationTapScreen({
  launchState = 'notification-banner',
  payload = SAMPLE_PAYLOAD,
  isColdLaunch = false,
  autoPlay = false,
  onTapNotification,
}: NotificationTapScreenProps) {
  const [state, setState] = useState<LaunchState>(launchState);

  // Auto-play sequence for demo
  useEffect(() => {
    if (!autoPlay) return;
    const steps: { delay: number; state: LaunchState }[] = isColdLaunch
      ? [
          { delay: 0,    state: 'notification-banner' },
          { delay: 2500, state: 'cold-splash'         },
          { delay: 4500, state: 'feed-loaded'         },
        ]
      : [
          { delay: 0,    state: 'notification-banner' },
          { delay: 2500, state: 'warm-transition'     },
          { delay: 3200, state: 'feed-loaded'         },
        ];

    const timers = steps.map(({ delay, state: s }) =>
      setTimeout(() => setState(s), delay)
    );
    return () => timers.forEach(clearTimeout);
  }, [autoPlay, isColdLaunch]);

  const handleTapNotification = () => {
    onTapNotification?.();
    if (autoPlay) return; // Let auto-play handle it
    setState(isColdLaunch ? 'cold-splash' : 'warm-transition');
    setTimeout(() => setState('feed-loaded'), isColdLaunch ? 1800 : 600);
  };

  return (
    <View style={styles.root}>
      {/* Background — simulate device/OS context */}
      {state === 'notification-banner' && (
        <View style={styles.lockScreen}>
          <Text style={styles.lockTime} aria-hidden>2:48 PM</Text>
          <Text style={styles.lockDate} aria-hidden>Thursday, 27 June</Text>

          <NotificationBanner
            payload={payload}
            onTap={handleTapNotification}
            visible={state === 'notification-banner'}
          />
        </View>
      )}

      {state === 'cold-splash' && <ColdLaunchSplash />}

      {state === 'warm-transition' && (
        <View style={styles.warmWrapper}>
          <DeepLinkedFeed payload={payload} highlight={false} />
          <WarmTransition />
        </View>
      )}

      {state === 'feed-loaded' && (
        <DeepLinkedFeed payload={payload} highlight />
      )}

      {/* State label for Storybook navigation */}
      <View style={styles.stateIndicator} pointerEvents="none">
        <Text style={styles.stateIndicatorText}>
          {state === 'notification-banner' && 'Notification banner'}
          {state === 'cold-splash'         && 'Cold launch splash'}
          {state === 'warm-transition'     && 'Warm foregrounding'}
          {state === 'feed-loaded'         && 'Feed loaded (deep link)'}
        </Text>
      </View>
    </View>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  root: {
    flex: 1,
    position: 'relative',
  },

  // Lock screen mock
  lockScreen: {
    flex: 1,
    backgroundColor: '#1A1035',
    alignItems: 'center',
    paddingTop: 80,
  },
  lockTime: {
    fontSize: 72,
    fontWeight: '200',
    color: '#FFFFFF',
    letterSpacing: -2,
  },
  lockDate: {
    fontSize: 17,
    color: 'rgba(255,255,255,0.85)',
    marginBottom: 32,
  },

  // Notification banner
  notifBanner: {
    width: '92%',
    backgroundColor: colors.notifBg,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.notifBorder,
    paddingHorizontal: 16,
    paddingVertical: 14,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 12,
    elevation: 8,
  },
  notifBannerInner: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  notifAppIcon: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: '#F0EDFB',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  notifAppIconCircles: {
    width: 30,
    height: 20,
    position: 'relative',
  },
  notifCircle: {
    width: 16,
    height: 16,
    borderRadius: 8,
    position: 'absolute',
    top: 2,
  },
  notifTextBlock: {
    flex: 1,
    gap: 2,
  },
  notifHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 2,
  },
  notifAppName: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.notifBody,
    letterSpacing: 0.3,
  },
  notifTime: {
    fontSize: 12,
    color: colors.notifBody,
  },
  notifTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.notifTitle,
    lineHeight: 20,
  },
  notifBody: {
    fontSize: 15,
    color: colors.notifBody,
    lineHeight: 20,
  },

  // Cold splash
  splashScreen: {
    flex: 1,
    backgroundColor: colors.white,
  },
  splashContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  splashLogoRow: {
    width: 60,
    height: 36,
    position: 'relative',
    marginBottom: 4,
  },
  splashCircle: {
    width: 28,
    height: 28,
    borderRadius: 14,
    position: 'absolute',
    top: 4,
  },
  splashWordmark: {
    fontSize: 36,
    fontWeight: '700',
    color: colors.textPrimary,
    letterSpacing: -1,
  },
  splashSpinner: {
    marginTop: 8,
  },
  splashSubtext: {
    fontSize: 15,
    color: colors.textSecondary,
  },

  // Warm transition
  warmWrapper: {
    flex: 1,
    position: 'relative',
  },
  warmOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(250,250,249,0.6)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  warmLabel: {
    fontSize: 15,
    color: colors.textSecondary,
    fontWeight: '500',
  },

  // Feed screen
  feedScreen: {
    flex: 1,
    backgroundColor: colors.white,
  },
  feedContent: {
    flex: 1,
    paddingHorizontal: 20,
    paddingTop: 24,
    gap: 16,
  },
  feedHeader: {
    fontSize: 22,
    fontWeight: '600',
    color: colors.textPrimary,
    letterSpacing: -0.3,
  },
  feedVenueCard: {
    borderRadius: 20,
    overflow: 'hidden',
    backgroundColor: colors.white,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 4,
  },
  feedVenuePhoto: {
    width: '100%',
    height: 200,
    backgroundColor: '#C2C0BB',
    alignItems: 'center',
    justifyContent: 'center',
  },
  feedVenuePhotoLabel: {
    fontSize: 13,
    color: '#FAFAF9',
  },
  feedVenueInfo: {
    padding: 20,
    gap: 10,
  },
  feedVenueName: {
    fontSize: 24,
    fontWeight: '700',
    color: colors.textPrimary,
    letterSpacing: -0.5,
  },
  feedVenueAddress: {
    fontSize: 15,
    color: colors.textSecondary,
  },
  feedRsvpRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
  feedRsvpButton: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.cardBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  feedRsvpButtonYes: {
    backgroundColor: colors.successLight,
  },
  feedRsvpLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  feedRsvpLabelYes: {
    fontSize: 14,
    fontWeight: '600',
    color: '#15803D',
  },
  feedCounts: {
    fontSize: 13,
    color: colors.textSecondary,
    textAlign: 'center',
  },

  // Deep link badge
  deepLinkBadge: {
    backgroundColor: colors.brandPurpleL,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 8,
    alignSelf: 'center',
  },
  deepLinkBadgeText: {
    fontSize: 13,
    color: colors.brandPurple,
    fontWeight: '500',
    textAlign: 'center',
  },

  // State indicator
  stateIndicator: {
    position: 'absolute',
    bottom: 16,
    left: 16,
    right: 16,
    alignItems: 'center',
  },
  stateIndicatorText: {
    fontSize: 12,
    color: colors.textSecondary,
    backgroundColor: colors.cardBg,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
    overflow: 'hidden',
  },
});

// ─── Storybook Meta ───────────────────────────────────────────────────────────

const meta: Meta<typeof NotificationTapScreen> = {
  title: 'Covey/Flows/NotificationDeepLink',
  component: NotificationTapScreen,
  parameters: {
    docs: {
      description: {
        component: `
**Notification Deep Link Flow** — simulates the complete Thursday notification → app experience.

**Primary persona**: Maya (commuter) — taps notification from lock screen on the bus. She expects the venue card to be the first thing she sees, with no additional navigation required.

### The notification payload

\`\`\`json
{
  "aps": {
    "alert": {
      "title": "This week's Seattle spot",
      "body": "The Mayflower — Tap to RSVP"
    },
    "badge": 1,
    "sound": "default"
  },
  "covey": {
    "type": "weekly_spot",
    "venueId": "mayflower-001",
    "cityId": "seattle"
  }
}
\`\`\`

### Cold launch sequence (app not running)

1. OS receives APNs payload, stores it
2. User taps banner → OS launches app with \`launchOptions[UIApplication.LaunchOptionsKey.remoteNotification]\`
3. App reads payload on launch, skips onboarding, navigates directly to Feed
4. Splash screen shown during ~1.5s init time
5. Feed loads with venue card visually highlighted

### Warm launch sequence (app in background)

1. User taps banner → OS foregrounds app
2. App receives payload via \`application(_:didReceiveRemoteNotification:)\`
3. Navigation to Feed is near-instant (< 300ms)
4. Feed loads with venue card highlighted (no splash needed)

### Navigation rules
- Deep link REPLACES root navigator — no back button to a previous screen
- If user has not selected a city, navigate to City Selection first (preserves payload for after selection)
- If app token is expired, navigate to Sign In (deep link executes after re-auth)

### Venue card highlight
- Animated border pulses 3 times at 800ms interval (purple → transparent → purple)
- Communicates "this is why the app opened"
- Auto-dismissed after 3 pulse cycles
- \`prefers-reduced-motion\`: single static highlight ring, no pulse

### Accessibility
- Notification banner carries \`accessibilityRole="alert"\` and full text alternative
- Cold launch spinner has \`accessibilityLiveRegion="polite"\` so VoiceOver announces loading state
- Highlighted card announced with full context: "The Mayflower — this week's curated spot in Seattle"
        `,
      },
    },
    viewport: { defaultViewport: 'iphone14' },
  },
  argTypes: {
    launchState: {
      control: 'select',
      options: ['notification-banner', 'cold-splash', 'warm-transition', 'feed-loaded'],
      description: 'Show a specific point in the launch sequence',
    },
    isColdLaunch: {
      control: 'boolean',
      description: 'Cold launch (app was not running) vs warm launch (app in background)',
    },
    autoPlay: {
      control: 'boolean',
      description: 'Auto-advance through the full launch sequence',
    },
  },
};

export default meta;
type Story = StoryObj<typeof NotificationTapScreen>;

// ─── Stories ──────────────────────────────────────────────────────────────────

/** Lock screen with notification banner — Maya sees this on the bus */
export const NotificationBannerState: Story = {
  name: 'Notification Banner',
  args: { launchState: 'notification-banner', isColdLaunch: false, autoPlay: false },
  parameters: {
    docs: { description: { story: 'iOS lock screen with Covey notification banner. Tapping the banner opens the app. The notification copy leads with the venue name to drive taps.' } },
  },
};

/** Cold launch splash — first 1.5 seconds when app was not running */
export const ColdLaunchSplashState: Story = {
  name: 'Cold Launch Splash',
  args: { launchState: 'cold-splash', isColdLaunch: true, autoPlay: false },
  parameters: {
    docs: { description: { story: 'App was killed. OS relaunches it on notification tap. Splash is shown for ~1.5 seconds while the app initialises. VoiceOver announces "Covey is loading".' } },
  },
};

/** Warm launch — app foregrounding (< 300ms, rarely visible) */
export const WarmLaunchState: Story = {
  name: 'Warm Launch Transition',
  args: { launchState: 'warm-transition', isColdLaunch: false, autoPlay: false },
  parameters: {
    docs: { description: { story: 'App was backgrounded. Foregrounding is near-instant. This semi-transparent overlay is visible for ~300ms before the feed renders.' } },
  },
};

/** Feed loaded with highlight — the destination */
export const FeedLoadedHighlighted: Story = {
  name: 'Feed Loaded (Highlighted)',
  args: { launchState: 'feed-loaded', isColdLaunch: false, autoPlay: false },
  parameters: {
    docs: { description: { story: 'Final state — feed is loaded, venue card pulses with a purple ring to indicate it is the reason the app opened. Pulse runs 3 times then stops.' } },
  },
};

/** Full cold launch auto-play sequence */
export const ColdLaunchAutoPlay: Story = {
  name: 'Auto-Play: Cold Launch',
  args: { launchState: 'notification-banner', isColdLaunch: true, autoPlay: true },
  parameters: {
    docs: { description: { story: 'Auto-advances through: Notification banner (2.5s) → Cold splash (2s) → Feed loaded. Simulates Maya tapping from a cold start.' } },
  },
};

/** Full warm launch auto-play sequence */
export const WarmLaunchAutoPlay: Story = {
  name: 'Auto-Play: Warm Launch',
  args: { launchState: 'notification-banner', isColdLaunch: false, autoPlay: true },
  parameters: {
    docs: { description: { story: 'Auto-advances through: Notification banner (2.5s) → Warm transition (700ms) → Feed loaded. Most common case for returning users.' } },
  },
};

/** Interactive playground */
export const Playground: Story = {
  args: { launchState: 'notification-banner', isColdLaunch: false, autoPlay: false },
};
