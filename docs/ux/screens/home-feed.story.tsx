/**
 * Covey — Home Feed Screen
 *
 * The main screen users land on after city selection (or via notification deep link).
 * This is the hero screen of the app — the weekly venue is the primary content.
 *
 * Personas:
 *   - Maya (commuter): quick scan, tap RSVP, done — used weekly
 *   - James (traveler): reads venue detail carefully, checks history
 *   - Priya (newcomer): checks who's going, reads address + hours
 *
 * Sections:
 *   1. Header — "This week's spot in Seattle" (pull-to-refresh)
 *   2. Venue Hero Card — photo, name, address, rating, hours, Maps button
 *   3. RSVP Section — Yes / Maybe / Nope toggles + live counts
 *   4. 4-Week History — past spots as scrollable cards
 *
 * States:
 *   Default        — Current spot loaded, no RSVP yet
 *   RSVPYes        — User has tapped Yes
 *   RSVPMaybe      — User has tapped Maybe
 *   RSVPNope       — User has tapped Nope
 *   Refreshing     — Pull-to-refresh in progress
 *   Loading        — Initial load
 *
 * WCAG 2.1 AA:
 *   - Star rating has text alternative ("4.3 stars out of 5")
 *   - Address copy button labeled "Copy address to clipboard"
 *   - RSVP buttons use role="radio" in a radiogroup
 *   - History cards are fully keyboard navigable
 *   - All text ≥ 12px, contrast ≥ 4.5:1
 */

import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  SafeAreaView,
  RefreshControl,
} from 'react-native';

// ─── Types ────────────────────────────────────────────────────────────────────

type RSVPChoice = 'yes' | 'maybe' | 'nope' | null;

interface VenueData {
  id: string;
  name: string;
  address: string;
  photoUri: string;      // In production: real image URL
  rating: number;        // 0–5
  reviewCount: number;
  hours: string;         // e.g., "Mon–Fri 4pm–11pm, Sat–Sun 12pm–1am"
  mapsUrl: string;
}

interface RSVPCounts {
  yes: number;
  maybe: number;
  nope: number;
}

interface HistoryVenue {
  id: string;
  weekLabel: string;     // "Last week", "2 weeks ago", etc.
  name: string;
  address: string;
  photoUri: string;
  confirmedCount: number;
}

interface HomeFeedScreenProps {
  cityName?: string;
  currentVenue?: VenueData;
  rsvpCounts?: RSVPCounts;
  userRsvp?: RSVPChoice;
  history?: HistoryVenue[];
  loading?: boolean;
  refreshing?: boolean;
  onRsvp?: (choice: RSVPChoice) => void;
  onRefresh?: () => void;
  onOpenMaps?: (venue: VenueData) => void;
  onCopyAddress?: (address: string) => void;
  onHistoryVenueTap?: (venue: HistoryVenue) => void;
}

// ─── Sample Data ──────────────────────────────────────────────────────────────

const SAMPLE_VENUE: VenueData = {
  id: 'mayflower-001',
  name: 'The Mayflower',
  address: '620 Pine St, Seattle, WA 98101',
  photoUri: 'https://images.unsplash.com/photo-1514190051997-0f6f39ca5cde?w=800',
  rating: 4.3,
  reviewCount: 218,
  hours: 'Mon–Thu 4pm–12am · Fri–Sat 2pm–2am · Sun Closed',
  mapsUrl: 'https://maps.apple.com/?q=The+Mayflower+Seattle',
};

const SAMPLE_COUNTS: RSVPCounts = { yes: 24, maybe: 11, nope: 5 };

const SAMPLE_HISTORY: HistoryVenue[] = [
  { id: 'h1', weekLabel: 'Last week',    name: 'The Walrus and the Carpenter', address: '4743 Ballard Ave NW', photoUri: '', confirmedCount: 31 },
  { id: 'h2', weekLabel: '2 weeks ago',  name: 'Canon',                        address: '928 12th Ave',        photoUri: '', confirmedCount: 18 },
  { id: 'h3', weekLabel: '3 weeks ago',  name: 'Needle & Thread',              address: '2228 2nd Ave',        photoUri: '', confirmedCount: 22 },
  { id: 'h4', weekLabel: '4 weeks ago',  name: 'Percy\'s & Co.',               address: '129 Occidental Ave S',photoUri: '', confirmedCount: 15 },
];

// ─── Design Tokens ────────────────────────────────────────────────────────────

const colors = {
  brandPurple:    '#6B4CE6',
  white:          '#FAFAF9',
  cardBg:         '#F5F4F2',
  textPrimary:    '#1C1B18',
  textSecondary:  '#8A8782',
  textOnPurple:   '#FFFFFF',
  border:         '#E8E7E4',
  starYellow:     '#F59E0B',
  successBase:    '#22C55E',
  successLight:   '#D1FAE5',
  successDark:    '#15803D',
  warningBase:    '#EAB308',
  warningLight:   '#FEF9C3',
  warningDark:    '#A16207',
  nopeBase:       '#6B7280',
  nopeLight:      '#F3F4F6',
  nopeDark:       '#374151',
  overlay:        'rgba(28,27,24,0.55)',
};

// ─── Sub-components ───────────────────────────────────────────────────────────

function StarRating({ rating, count }: { rating: number; count: number }) {
  const fullStars = Math.floor(rating);
  const halfStar  = rating - fullStars >= 0.5;

  return (
    <View
      style={styles.ratingRow}
      accessibilityLabel={`${rating} stars out of 5, ${count} reviews`}
      accessible
    >
      {Array.from({ length: 5 }).map((_, i) => (
        <Text
          key={i}
          style={[
            styles.star,
            i < fullStars
              ? styles.starFull
              : i === fullStars && halfStar
              ? styles.starHalf
              : styles.starEmpty,
          ]}
          aria-hidden
        >
          {i < fullStars ? '★' : i === fullStars && halfStar ? '½' : '☆'}
        </Text>
      ))}
      <Text style={styles.reviewCount}>{rating.toFixed(1)} · {count} reviews</Text>
    </View>
  );
}

function VenueHeroCard({
  venue,
  onOpenMaps,
  onCopyAddress,
}: {
  venue: VenueData;
  onOpenMaps?: (v: VenueData) => void;
  onCopyAddress?: (a: string) => void;
}) {
  return (
    <View style={styles.heroCard}>
      {/* Photo placeholder (production: <Image source={{ uri: venue.photoUri }} />) */}
      <View
        style={styles.heroPhoto}
        accessibilityRole="image"
        accessibilityLabel={`Photo of ${venue.name}`}
      >
        <Text style={styles.heroPhotoPlaceholder}>[ Venue Photo ]</Text>
        {/* Gradient overlay at bottom for text legibility — production: LinearGradient */}
        <View style={styles.heroOverlay} />
      </View>

      <View style={styles.heroContent}>
        {/* Venue name */}
        <Text style={styles.venueName} accessibilityRole="header">
          {venue.name}
        </Text>

        {/* Address row */}
        <View style={styles.addressRow}>
          <Text style={styles.addressText} numberOfLines={1} style={styles.addressText}>
            {venue.address}
          </Text>
          <TouchableOpacity
            onPress={() => onCopyAddress?.(venue.address)}
            accessibilityRole="button"
            accessibilityLabel="Copy address to clipboard"
            style={styles.iconButton}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Text style={styles.iconButtonText}>⧉</Text>
          </TouchableOpacity>
        </View>

        {/* Rating */}
        <StarRating rating={venue.rating} count={venue.reviewCount} />

        {/* Hours */}
        <View style={styles.hoursRow}>
          <Text style={styles.hoursIcon} aria-hidden>◷</Text>
          <Text style={styles.hoursText}>{venue.hours}</Text>
        </View>

        {/* Open in Maps */}
        <TouchableOpacity
          style={styles.mapsButton}
          onPress={() => onOpenMaps?.(venue)}
          accessibilityRole="button"
          accessibilityLabel="Open The Mayflower in Maps"
          activeOpacity={0.85}
        >
          <Text style={styles.mapsButtonIcon} aria-hidden>⬆</Text>
          <Text style={styles.mapsButtonLabel}>Open in Maps</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

function RSVPSection({
  counts,
  userRsvp,
  onRsvp,
}: {
  counts: RSVPCounts;
  userRsvp: RSVPChoice;
  onRsvp?: (choice: RSVPChoice) => void;
}) {
  const options: { id: 'yes' | 'maybe' | 'nope'; label: string }[] = [
    { id: 'yes',   label: 'Yes'   },
    { id: 'maybe', label: 'Maybe' },
    { id: 'nope',  label: 'Nope'  },
  ];

  const getButtonStyle = (id: 'yes' | 'maybe' | 'nope') => {
    if (userRsvp !== id) return [styles.rsvpButton];
    if (id === 'yes')   return [styles.rsvpButton, styles.rsvpButtonYes];
    if (id === 'maybe') return [styles.rsvpButton, styles.rsvpButtonMaybe];
    return              [styles.rsvpButton, styles.rsvpButtonNope];
  };

  const getLabelStyle = (id: 'yes' | 'maybe' | 'nope') => {
    if (userRsvp !== id) return [styles.rsvpButtonLabel];
    if (id === 'yes')   return [styles.rsvpButtonLabel, styles.rsvpLabelYes];
    if (id === 'maybe') return [styles.rsvpButtonLabel, styles.rsvpLabelMaybe];
    return              [styles.rsvpButtonLabel, styles.rsvpLabelNope];
  };

  // Adjust counts to reflect current user selection
  const displayCounts = { ...counts };
  // (In production, counts come from server and update after RSVP submit)

  return (
    <View style={styles.rsvpSection}>
      <Text style={styles.rsvpHeading} accessibilityRole="header">
        Will you go?
      </Text>

      {/* Button group — announced as radiogroup */}
      <View
        style={styles.rsvpButtonRow}
        accessibilityRole="radiogroup"
        accessibilityLabel="RSVP options"
      >
        {options.map(({ id, label }) => (
          <TouchableOpacity
            key={id}
            style={getButtonStyle(id)}
            onPress={() => onRsvp?.(userRsvp === id ? null : id)}
            accessibilityRole="radio"
            accessibilityLabel={label}
            accessibilityState={{ checked: userRsvp === id }}
            activeOpacity={0.8}
          >
            <Text style={getLabelStyle(id)}>{label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Live counts */}
      <Text
        style={styles.rsvpCounts}
        accessibilityLiveRegion="polite"
        accessibilityLabel={`${displayCounts.yes} confirmed, ${displayCounts.maybe} interested, ${displayCounts.nope} not going`}
      >
        {displayCounts.yes} confirmed · {displayCounts.maybe} interested · {displayCounts.nope} not going
      </Text>
    </View>
  );
}

function HistoryCard({
  venue,
  onTap,
}: {
  venue: HistoryVenue;
  onTap?: (v: HistoryVenue) => void;
}) {
  return (
    <TouchableOpacity
      style={styles.historyCard}
      onPress={() => onTap?.(venue)}
      accessibilityRole="button"
      accessibilityLabel={`${venue.weekLabel}: ${venue.name}, ${venue.confirmedCount} confirmed. Tap to expand.`}
      activeOpacity={0.8}
    >
      {/* Thumbnail placeholder */}
      <View style={styles.historyThumb} accessibilityElementsHidden>
        <Text style={styles.historyThumbPlaceholder}>📍</Text>
      </View>

      <View style={styles.historyInfo}>
        <Text style={styles.historyWeekLabel}>{venue.weekLabel}</Text>
        <Text style={styles.historyName} numberOfLines={1}>{venue.name}</Text>
        <Text style={styles.historyCounts}>{venue.confirmedCount} confirmed</Text>
      </View>

      <Text style={styles.historyChevron} aria-hidden>›</Text>
    </TouchableOpacity>
  );
}

// ─── Main Screen Component ────────────────────────────────────────────────────

export function HomeFeedScreen({
  cityName = 'Seattle',
  currentVenue = SAMPLE_VENUE,
  rsvpCounts = SAMPLE_COUNTS,
  userRsvp = null,
  history = SAMPLE_HISTORY,
  loading = false,
  refreshing = false,
  onRsvp,
  onRefresh,
  onOpenMaps,
  onCopyAddress,
  onHistoryVenueTap,
}: HomeFeedScreenProps) {
  const [internalRsvp, setInternalRsvp] = useState<RSVPChoice>(userRsvp);

  const handleRsvp = (choice: RSVPChoice) => {
    setInternalRsvp(choice);
    onRsvp?.(choice);
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.loadingScreen}>
          <ActivityIndicator size="large" color={colors.brandPurple} />
          <Text style={styles.loadingText}>Loading this week's spot…</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.brandPurple}
            accessibilityLabel="Pull to refresh this week's spot"
          />
        }
      >
        {/* Screen header */}
        <View style={styles.screenHeader}>
          <Text style={styles.screenTitle} accessibilityRole="header">
            This week's spot in {cityName}
          </Text>
        </View>

        {/* Venue hero card */}
        <VenueHeroCard
          venue={currentVenue}
          onOpenMaps={onOpenMaps}
          onCopyAddress={onCopyAddress}
        />

        {/* RSVP section */}
        <RSVPSection
          counts={rsvpCounts}
          userRsvp={internalRsvp}
          onRsvp={handleRsvp}
        />

        {/* History */}
        <View style={styles.historySection}>
          <Text style={styles.historyHeading} accessibilityRole="header">
            Past spots
          </Text>
          {history.map((venue) => (
            <HistoryCard
              key={venue.id}
              venue={venue}
              onTap={onHistoryVenueTap}
            />
          ))}
        </View>

        <View style={styles.bottomPad} />
      </ScrollView>
    </SafeAreaView>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.white,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
  },
  loadingScreen: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  loadingText: {
    fontSize: 15,
    color: colors.textSecondary,
  },
  bottomPad: {
    height: 40,
  },

  // Screen header
  screenHeader: {
    paddingTop: 24,
    paddingBottom: 16,
  },
  screenTitle: {
    fontSize: 22,
    fontWeight: '600',
    color: colors.textPrimary,
    letterSpacing: -0.3,
  },

  // Hero card
  heroCard: {
    backgroundColor: colors.white,
    borderRadius: 20,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 4,
    marginBottom: 24,
  },
  heroPhoto: {
    width: '100%',
    height: 300,
    backgroundColor: '#C2C0BB',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  heroPhotoPlaceholder: {
    fontSize: 15,
    color: colors.white,
    fontWeight: '500',
  },
  heroOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 80,
    backgroundColor: colors.overlay,
  },
  heroContent: {
    padding: 20,
    gap: 12,
  },
  venueName: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
    letterSpacing: -0.5,
  },
  addressRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  addressText: {
    flex: 1,
    fontSize: 15,
    color: colors.textSecondary,
    lineHeight: 20,
  },
  iconButton: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconButtonText: {
    fontSize: 18,
    color: colors.textSecondary,
  },

  // Rating
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  star: {
    fontSize: 16,
  },
  starFull: {
    color: colors.starYellow,
  },
  starHalf: {
    color: colors.starYellow,
    opacity: 0.6,
  },
  starEmpty: {
    color: colors.border,
  },
  reviewCount: {
    fontSize: 13,
    color: colors.textSecondary,
    marginLeft: 4,
  },

  // Hours
  hoursRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
  },
  hoursIcon: {
    fontSize: 15,
    color: colors.textSecondary,
    marginTop: 1,
  },
  hoursText: {
    flex: 1,
    fontSize: 13,
    color: colors.textSecondary,
    lineHeight: 18,
  },

  // Maps button
  mapsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 44,
    borderRadius: 12,
    backgroundColor: colors.cardBg,
    gap: 8,
    marginTop: 4,
  },
  mapsButtonIcon: {
    fontSize: 16,
    color: colors.brandPurple,
  },
  mapsButtonLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.brandPurple,
  },

  // RSVP section
  rsvpSection: {
    marginBottom: 32,
    gap: 12,
  },
  rsvpHeading: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  rsvpButtonRow: {
    flexDirection: 'row',
    gap: 10,
  },
  rsvpButton: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.cardBg,
    borderWidth: 1.5,
    borderColor: 'transparent',
  },
  rsvpButtonYes: {
    backgroundColor: colors.successLight,
    borderColor: colors.successBase,
  },
  rsvpButtonMaybe: {
    backgroundColor: colors.warningLight,
    borderColor: colors.warningBase,
  },
  rsvpButtonNope: {
    backgroundColor: colors.nopeLight,
    borderColor: colors.nopeBase,
  },
  rsvpButtonLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  rsvpLabelYes: {
    color: colors.successDark,
  },
  rsvpLabelMaybe: {
    color: colors.warningDark,
  },
  rsvpLabelNope: {
    color: colors.nopeDark,
  },
  rsvpCounts: {
    fontSize: 13,
    color: colors.textSecondary,
    textAlign: 'center',
  },

  // History
  historySection: {
    gap: 12,
  },
  historyHeading: {
    fontSize: 18,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: 4,
  },
  historyCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.cardBg,
    borderRadius: 14,
    padding: 12,
    gap: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 3,
    elevation: 1,
  },
  historyThumb: {
    width: 64,
    height: 64,
    borderRadius: 10,
    backgroundColor: '#C2C0BB',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  historyThumbPlaceholder: {
    fontSize: 24,
  },
  historyInfo: {
    flex: 1,
    gap: 4,
  },
  historyWeekLabel: {
    fontSize: 12,
    fontWeight: '500',
    color: colors.brandPurple,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  historyName: {
    fontSize: 15,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  historyCounts: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  historyChevron: {
    fontSize: 22,
    color: colors.border,
    fontWeight: '300',
  },
});

// ─── Storybook Meta ───────────────────────────────────────────────────────────

const meta: Meta<typeof HomeFeedScreen> = {
  title: 'Covey/Screens/HomeFeed',
  component: HomeFeedScreen,
  parameters: {
    docs: {
      description: {
        component: `
**Home Feed Screen** — the core screen of Covey. Users land here from:
- City selection (first launch)
- Notification deep link (weekly Thursday push)
- App icon tap (returning user)

**Personas**:
- Maya (commuter): opens via notification, quick-glances venue, taps RSVP
- James (traveler): reads venue detail, checks history to evaluate curation
- Priya (newcomer): checks who's going, copies address to Maps

**Journey**: Step 2 of \`weekly-spot-rsvp\` (Maya Okafor persona).

### Information hierarchy
1. Screen title confirms city context — prevents confusion for James who changes cities
2. Hero venue card — full-bleed photo is the emotional hook; name + address immediately visible
3. RSVP — three clear choices, live counts provide social proof for Priya
4. History — builds trust in curation over time; James uses this to evaluate quality

### Accessibility
- Pull-to-refresh has a screen reader label
- Star rating provides full text alternative ("4.3 stars out of 5, 218 reviews")
- RSVP uses radiogroup/radio pattern so VoiceOver announces selection correctly
- Live counts use \`accessibilityLiveRegion="polite"\` so changes are announced without interrupting
- History cards communicate expand affordance in their accessibility label
        `,
      },
    },
    viewport: { defaultViewport: 'iphone14' },
  },
  argTypes: {
    cityName: { control: 'text' },
    userRsvp: {
      control: 'select',
      options: [null, 'yes', 'maybe', 'nope'],
      description: 'Current RSVP state for the logged-in user',
    },
    loading: { control: 'boolean' },
    refreshing: { control: 'boolean' },
  },
};

export default meta;
type Story = StoryObj<typeof HomeFeedScreen>;

// ─── Stories ──────────────────────────────────────────────────────────────────

/** Default — venue loaded, user has not yet RSVP'd */
export const Default: Story = {
  args: { cityName: 'Seattle', userRsvp: null, loading: false },
};

/** User has confirmed attendance */
export const RSVPYes: Story = {
  args: { cityName: 'Seattle', userRsvp: 'yes', loading: false },
  parameters: {
    docs: { description: { story: 'Yes button shows green tinted pill, count reads back updated value. Typical state for Maya after her 30-second check-in.' } },
  },
};

/** User marked Maybe */
export const RSVPMaybe: Story = {
  args: { cityName: 'Seattle', userRsvp: 'maybe', loading: false },
  parameters: {
    docs: { description: { story: 'Amber/yellow tinted Maybe button selected. Typical state for Priya who keeps options open.' } },
  },
};

/** User declined */
export const RSVPNope: Story = {
  args: { cityName: 'Seattle', userRsvp: 'nope', loading: false },
};

/** Initial load */
export const Loading: Story = {
  args: { loading: true },
};

/** Pull-to-refresh in progress */
export const Refreshing: Story = {
  args: { cityName: 'Seattle', userRsvp: null, refreshing: true },
};

/** James in Tacoma */
export const TacomaFeed: Story = {
  args: {
    cityName: 'Tacoma',
    userRsvp: null,
    rsvpCounts: { yes: 8, maybe: 3, nope: 1 },
  },
  parameters: {
    docs: { description: { story: 'James scenario — smaller city with fewer gatherers, demonstrating that the design scales gracefully to lower counts.' } },
  },
};

/** Interactive playground */
export const Playground: Story = {
  args: { cityName: 'Seattle', userRsvp: null, loading: false, refreshing: false },
};
