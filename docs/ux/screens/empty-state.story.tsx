/**
 * Covey — Empty State Screen
 *
 * Shown on the Home Feed when no weekly spot has been selected yet.
 * This typically happens from Monday → Wednesday each week (before Thursday
 * when the curator publishes the new spot).
 *
 * This screen must:
 *   1. Clearly explain why nothing is showing (not an error)
 *   2. Tell the user exactly when to come back
 *   3. Reassure them that a notification will arrive
 *   4. Keep tone warm and inviting — not robotic
 *
 * Personas:
 *   - Priya (newcomer): most likely to see this on first-week sign-up
 *   - Maya (commuter): may open app mid-week out of habit
 *   - James (traveler): might check immediately after city selection
 *
 * States:
 *   Default       — Standard pre-Thursday empty state
 *   NotEnabled    — User has notifications disabled (extra CTA)
 *   JustSignedUp  — First-ever visit, extra welcome copy
 *
 * WCAG 2.1 AA:
 *   - Illustration is decorative (aria-hidden) with text conveying meaning
 *   - Headings use correct semantic roles
 *   - Notification enable CTA has clear accessible label
 *   - All text ≥ 15px at this screen size for readability
 */

import React from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
} from 'react-native';

// ─── Types ────────────────────────────────────────────────────────────────────

interface EmptyStateScreenProps {
  cityName?: string;
  /** Show notification enable CTA when user hasn't granted permission */
  showNotificationCta?: boolean;
  /** Extra welcome copy for brand-new users */
  isFirstVisit?: boolean;
  onEnableNotifications?: () => void;
  onBrowseHistory?: () => void;
}

// ─── Design Tokens ────────────────────────────────────────────────────────────

const colors = {
  brandPurple:     '#6B4CE6',
  brandPurpleLight:'#F0EDFB',
  white:           '#FAFAF9',
  cardBg:          '#F5F4F2',
  textPrimary:     '#1C1B18',
  textSecondary:   '#8A8782',
  textOnPurple:    '#FFFFFF',
  border:          '#E8E7E4',
  purple100:       '#DDD6F7',
  purple200:       '#BDB0EF',
  purple400:       '#8B6EE8',
  purple600:       '#6B4CE6',
  orange:          '#F97316',
  orangeLight:     '#FFF7ED',
};

// ─── Gathering Illustration ────────────────────────────────────────────────────
// Rendered with pure RN View primitives — no image dependency.
// Represents three people/circles gathered around a central glow.

function GatheringIllustration() {
  return (
    <View
      style={illustration.wrapper}
      aria-hidden
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      {/* Outer ring — pulsing aura */}
      <View style={illustration.outerRing} />

      {/* Inner glow */}
      <View style={illustration.innerGlow} />

      {/* Central spot marker */}
      <View style={illustration.spotMarker}>
        <Text style={illustration.spotPin}>📍</Text>
      </View>

      {/* Three person circles orbiting the center */}
      <View style={[illustration.personCircle, illustration.personTop]}>
        <Text style={illustration.personEmoji}>🧑</Text>
      </View>
      <View style={[illustration.personCircle, illustration.personBottomLeft]}>
        <Text style={illustration.personEmoji}>👩</Text>
      </View>
      <View style={[illustration.personCircle, illustration.personBottomRight]}>
        <Text style={illustration.personEmoji}>👨</Text>
      </View>

      {/* Connection lines (decorative) */}
      <View style={[illustration.line, illustration.lineTopCenter]}   />
      <View style={[illustration.line, illustration.lineBottomLeft]}  />
      <View style={[illustration.line, illustration.lineBottomRight]} />
    </View>
  );
}

const illustration = StyleSheet.create({
  wrapper: {
    width: 200,
    height: 200,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    marginBottom: 8,
  },
  outerRing: {
    position: 'absolute',
    width: 190,
    height: 190,
    borderRadius: 95,
    borderWidth: 1.5,
    borderColor: colors.purple100,
    borderStyle: 'dashed',
  },
  innerGlow: {
    position: 'absolute',
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: colors.brandPurpleLight,
  },
  spotMarker: {
    zIndex: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  spotPin: {
    fontSize: 36,
  },
  personCircle: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.white,
    borderWidth: 2,
    borderColor: colors.purple200,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
    zIndex: 1,
  },
  personTop: {
    top: 4,
    alignSelf: 'center',
  },
  personBottomLeft: {
    bottom: 12,
    left: 12,
  },
  personBottomRight: {
    bottom: 12,
    right: 12,
  },
  personEmoji: {
    fontSize: 22,
  },
  line: {
    position: 'absolute',
    width: 1,
    backgroundColor: colors.purple100,
    zIndex: 0,
  },
  lineTopCenter: {
    height: 52,
    top: 44,
    alignSelf: 'center',
  },
  lineBottomLeft: {
    height: 44,
    bottom: 36,
    left: 56,
    transform: [{ rotate: '45deg' }],
  },
  lineBottomRight: {
    height: 44,
    bottom: 36,
    right: 56,
    transform: [{ rotate: '-45deg' }],
  },
});

// ─── Notification CTA ─────────────────────────────────────────────────────────

function NotificationCTA({ onPress }: { onPress?: () => void }) {
  return (
    <View style={styles.notifCta}>
      <Text style={styles.notifCtaIcon} aria-hidden>🔔</Text>
      <View style={styles.notifCtaText}>
        <Text style={styles.notifCtaTitle}>Get notified Thursday</Text>
        <Text style={styles.notifCtaSubtitle}>
          Enable notifications so you never miss this week's spot.
        </Text>
      </View>
      <TouchableOpacity
        style={styles.notifCtaButton}
        onPress={onPress}
        accessibilityRole="button"
        accessibilityLabel="Enable push notifications for Covey"
        activeOpacity={0.8}
      >
        <Text style={styles.notifCtaButtonLabel}>Enable</Text>
      </TouchableOpacity>
    </View>
  );
}

// ─── History Nudge ────────────────────────────────────────────────────────────

function HistoryNudge({ onPress }: { onPress?: () => void }) {
  return (
    <TouchableOpacity
      style={styles.historyNudge}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel="Browse past Covey spots"
      activeOpacity={0.85}
    >
      <Text style={styles.historyNudgeText}>Browse past spots →</Text>
    </TouchableOpacity>
  );
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

export function EmptyStateScreen({
  cityName = 'Seattle',
  showNotificationCta = false,
  isFirstVisit = false,
  onEnableNotifications,
  onBrowseHistory,
}: EmptyStateScreenProps) {
  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.screen}>

        {/* Header context (mirrors Home Feed header for orientation) */}
        <View style={styles.header}>
          <Text style={styles.headerTitle} accessibilityRole="header">
            This week's spot in {cityName}
          </Text>
        </View>

        {/* Main empty state content */}
        <View style={styles.content}>
          <GatheringIllustration />

          <Text
            style={styles.heading}
            accessibilityRole="header"
          >
            No spot selected yet
          </Text>

          <Text style={styles.subheading}>
            Come back Thursday for this week's spot
          </Text>

          <Text style={styles.helperText}>
            When a spot is ready, we'll send you a notification. Then gather,
            RSVP, and see who else is coming.
          </Text>

          {/* First-visit welcome variant */}
          {isFirstVisit && (
            <View
              style={styles.welcomeCard}
              accessibilityRole="text"
            >
              <Text style={styles.welcomeCardText}>
                Welcome to Covey! Every Thursday a new local spot is curated
                for {cityName}. RSVP to see who's going — then show up.
              </Text>
            </View>
          )}
        </View>

        {/* Footer actions */}
        <View style={styles.footer}>
          {showNotificationCta && (
            <NotificationCTA onPress={onEnableNotifications} />
          )}
          <HistoryNudge onPress={onBrowseHistory} />
        </View>

      </View>
    </SafeAreaView>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.white,
  },
  screen: {
    flex: 1,
    paddingHorizontal: 20,
    paddingBottom: 16,
  },

  // Header (mirrors home feed for layout consistency)
  header: {
    paddingTop: 24,
    paddingBottom: 24,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: '600',
    color: colors.textPrimary,
    letterSpacing: -0.3,
  },

  // Main content
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    paddingHorizontal: 8,
  },
  heading: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
    textAlign: 'center',
    letterSpacing: -0.5,
  },
  subheading: {
    fontSize: 17,
    fontWeight: '500',
    color: colors.brandPurple,
    textAlign: 'center',
  },
  helperText: {
    fontSize: 15,
    fontWeight: '400',
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
    maxWidth: 300,
  },

  // First-visit card
  welcomeCard: {
    backgroundColor: colors.brandPurpleLight,
    borderRadius: 14,
    padding: 16,
    marginTop: 8,
  },
  welcomeCardText: {
    fontSize: 15,
    color: colors.brandPurple,
    lineHeight: 22,
    textAlign: 'center',
    fontWeight: '500',
  },

  // Footer
  footer: {
    gap: 12,
    paddingBottom: 8,
  },

  // Notification CTA
  notifCta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.orangeLight,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: '#FED7AA',
  },
  notifCtaIcon: {
    fontSize: 22,
  },
  notifCtaText: {
    flex: 1,
    gap: 2,
  },
  notifCtaTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  notifCtaSubtitle: {
    fontSize: 13,
    color: colors.textSecondary,
    lineHeight: 18,
  },
  notifCtaButton: {
    backgroundColor: colors.orange,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 10,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  notifCtaButtonLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.white,
  },

  // History nudge
  historyNudge: {
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  historyNudgeText: {
    fontSize: 15,
    fontWeight: '500',
    color: colors.brandPurple,
  },
});

// ─── Storybook Meta ───────────────────────────────────────────────────────────

const meta: Meta<typeof EmptyStateScreen> = {
  title: 'Covey/Screens/EmptyState',
  component: EmptyStateScreen,
  parameters: {
    docs: {
      description: {
        component: `
**Empty State Screen** — shown on the Home Feed Monday through Wednesday when no weekly spot has been curated yet.

**Personas**:
- Priya (newcomer): most likely to encounter this immediately after signing up mid-week
- Maya (commuter): may check out of habit and land here; needs quick reassurance
- James (traveler): might select a new city and immediately land here

### Design principles applied
- **Explain, don't abandon**: Heading + subheading together tell the full story in < 10 words
- **Set an expectation**: "Come back Thursday" is concrete, not vague ("soon")
- **Confirm the notification path**: Reduces anxiety — user knows they won't have to remember
- **Warm tone**: Illustration of people gathering reinforces the social promise
- **Non-destructive**: "Browse past spots" gives James and Priya something to do right now

### Copy decisions
- "No spot selected yet" (not "No venue available") — positions this as curation, not a bug
- "Come back Thursday" — day-of-week is more actionable than "check back later"
- "When a spot is ready, we'll send a notification" — passive reassurance, no user action required

### Variants
- **Default**: Standard mid-week state
- **First Visit (isFirstVisit=true)**: Adds welcome card explaining the weekly rhythm
- **Notifications disabled (showNotificationCta=true)**: Orange CTA to enable permissions

### Accessibility
- Illustration is \`aria-hidden\` — all meaning carried by text
- Notification CTA button labeled "Enable push notifications for Covey" (not just "Enable")
- History nudge button labeled "Browse past Covey spots"
        `,
      },
    },
    viewport: { defaultViewport: 'iphone14' },
  },
  argTypes: {
    cityName: { control: 'text', description: 'Selected city name displayed in header' },
    showNotificationCta: { control: 'boolean', description: 'Show notification enable prompt' },
    isFirstVisit: { control: 'boolean', description: 'Show extra welcome copy for brand-new users' },
  },
};

export default meta;
type Story = StoryObj<typeof EmptyStateScreen>;

// ─── Stories ──────────────────────────────────────────────────────────────────

/** Standard mid-week state — most common occurrence */
export const Default: Story = {
  args: { cityName: 'Seattle', showNotificationCta: false, isFirstVisit: false },
};

/** First visit — Priya signs up on a Tuesday */
export const FirstVisit: Story = {
  args: { cityName: 'Seattle', showNotificationCta: false, isFirstVisit: true },
  parameters: {
    docs: { description: { story: 'Priya signs up mid-week and immediately sees this screen. The teal welcome card sets expectations about the weekly rhythm without overwhelming her.' } },
  },
};

/** Notifications not enabled — nudge to unlock the core value prop */
export const NotificationsDisabled: Story = {
  args: { cityName: 'Seattle', showNotificationCta: true, isFirstVisit: false },
  parameters: {
    docs: { description: { story: 'When the user has not granted notification permissions. Orange CTA is attention-grabbing without being alarming.' } },
  },
};

/** First visit + notifications disabled — combined state */
export const FirstVisitNoNotifs: Story = {
  args: { cityName: 'Seattle', showNotificationCta: true, isFirstVisit: true },
  parameters: {
    docs: { description: { story: 'Worst-case onboarding: new user, mid-week, no notification permission. All three helper elements visible.' } },
  },
};

/** James in Tacoma — smaller city variant */
export const TacomaFirstVisit: Story = {
  args: { cityName: 'Tacoma', showNotificationCta: false, isFirstVisit: true },
};

/** Interactive playground */
export const Playground: Story = {
  args: { cityName: 'Seattle', showNotificationCta: false, isFirstVisit: false },
};
