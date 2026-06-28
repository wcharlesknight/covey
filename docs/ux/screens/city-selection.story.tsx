/**
 * Covey — City Selection Screen
 *
 * Shown immediately after first sign-in (or from Settings to change city).
 * Persona: All three — but the primary scenario is Priya (newcomer, first launch)
 *          and James (traveler, switching cities per trip).
 *
 * States documented:
 *   Default      — No city selected; all cards in unselected state
 *   Selected     — One city card highlighted; confirm button active
 *   Loading      — Confirm tapped, navigating to feed
 *   Error        — Failed to persist city selection (network)
 *
 * WCAG 2.1 AA:
 *   - City cards use role="radio" + checked state for screen reader clarity
 *   - Selected state achieves contrast via border + background fill (not color alone)
 *   - Confirm button disabled until a selection is made (communicated via accessibilityState)
 *   - Minimum card height 64px (well above 44px tap target)
 */

import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  StyleSheet,
  SafeAreaView,
} from 'react-native';

// ─── Types ────────────────────────────────────────────────────────────────────

interface City {
  id: string;
  name: string;
  state: string;
  /** Approximate member count — social proof */
  memberCount: number;
}

interface CitySelectionScreenProps {
  /** List of available cities to display */
  cities?: City[];
  /** Pre-selected city id (for controlled stories) */
  selectedCityId?: string | null;
  /** Loading state after confirm is tapped */
  loading?: boolean;
  /** Error message if city persistence fails */
  errorMessage?: string;
  /** Called when user taps confirm with the selected city */
  onConfirm?: (city: City) => void;
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const DEFAULT_CITIES: City[] = [
  { id: 'seattle', name: 'Seattle', state: 'WA', memberCount: 1240 },
  { id: 'tacoma', name: 'Tacoma', state: 'WA', memberCount: 380 },
  { id: 'bellevue', name: 'Bellevue', state: 'WA', memberCount: 510 },
  { id: 'olympia', name: 'Olympia', state: 'WA', memberCount: 120 },
];

// ─── Design Tokens ────────────────────────────────────────────────────────────

const colors = {
  brandPurple:     '#6B4CE6',
  brandPurpleLight:'#F0EDFB',
  brandPurpleBorder:'#BDB0EF',
  white:           '#FAFAF9',
  cardBg:          '#F5F4F2',
  textPrimary:     '#1C1B18',
  textSecondary:   '#8A8782',
  textOnPurple:    '#FFFFFF',
  border:          '#E8E7E4',
  errorLight:      '#FEE2E2',
  errorBase:       '#EF4444',
  disabledBg:      '#E8E7E4',
  disabledText:    '#C2C0BB',
};

// ─── Sub-components ───────────────────────────────────────────────────────────

interface CityCardProps {
  city: City;
  selected: boolean;
  onSelect: (city: City) => void;
}

function CityCard({ city, selected, onSelect }: CityCardProps) {
  const formattedCount = city.memberCount >= 1000
    ? `${(city.memberCount / 1000).toFixed(1)}k`
    : `${city.memberCount}`;

  return (
    <TouchableOpacity
      style={[styles.cityCard, selected && styles.cityCardSelected]}
      onPress={() => onSelect(city)}
      accessibilityRole="radio"
      accessibilityLabel={`${city.name}, ${city.state} — ${formattedCount} gatherers`}
      accessibilityState={{ checked: selected }}
      activeOpacity={0.8}
    >
      {/* Left: city name + state */}
      <View style={styles.cityCardLeft}>
        <Text style={[styles.cityName, selected && styles.cityNameSelected]}>
          {city.name}
        </Text>
        <Text style={[styles.cityState, selected && styles.cityStateSelected]}>
          {city.state} · {formattedCount} gatherers
        </Text>
      </View>

      {/* Right: selection indicator */}
      <View style={[styles.radioRing, selected && styles.radioRingSelected]}>
        {selected && <View style={styles.radioDot} />}
      </View>
    </TouchableOpacity>
  );
}

function ConfirmButton({
  enabled,
  loading,
  onPress,
}: {
  enabled: boolean;
  loading: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={[styles.confirmButton, !enabled && styles.confirmButtonDisabled]}
      onPress={onPress}
      disabled={!enabled || loading}
      accessibilityRole="button"
      accessibilityLabel={loading ? 'Confirming city…' : 'Continue to feed'}
      accessibilityState={{ disabled: !enabled, busy: loading }}
      activeOpacity={0.85}
    >
      {loading ? (
        <ActivityIndicator size="small" color={colors.textOnPurple} />
      ) : (
        <Text style={[styles.confirmButtonLabel, !enabled && styles.confirmButtonLabelDisabled]}>
          Continue
        </Text>
      )}
    </TouchableOpacity>
  );
}

// ─── Main Screen Component ────────────────────────────────────────────────────

export function CitySelectionScreen({
  cities = DEFAULT_CITIES,
  selectedCityId = null,
  loading = false,
  errorMessage,
  onConfirm,
}: CitySelectionScreenProps) {
  const [internalSelected, setInternalSelected] = useState<string | null>(
    selectedCityId
  );

  // Support both controlled (via prop) and uncontrolled (internal state) modes
  const activeId = selectedCityId !== undefined ? selectedCityId : internalSelected;
  const activeCity = cities.find((c) => c.id === activeId) ?? null;

  const handleSelect = (city: City) => {
    setInternalSelected(city.id);
  };

  const handleConfirm = () => {
    if (activeCity) onConfirm?.(activeCity);
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.screen}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.heading} accessibilityRole="header">
            Where are you gathering?
          </Text>
          <Text style={styles.subheading}>
            We'll show you the weekly spot for your city.
          </Text>
        </View>

        {/* City list — wrapped in a radio group for accessibility */}
        <ScrollView
          style={styles.listScroll}
          contentContainerStyle={styles.listContent}
          accessibilityRole="radiogroup"
          accessibilityLabel="Select your city"
          showsVerticalScrollIndicator={false}
        >
          {cities.map((city) => (
            <CityCard
              key={city.id}
              city={city}
              selected={activeId === city.id}
              onSelect={handleSelect}
            />
          ))}
        </ScrollView>

        {/* Footer: error + confirm */}
        <View style={styles.footer}>
          {errorMessage && (
            <View
              style={styles.errorBanner}
              accessibilityRole="alert"
              accessibilityLiveRegion="assertive"
            >
              <Text style={styles.errorText}>{errorMessage}</Text>
            </View>
          )}

          <ConfirmButton
            enabled={!!activeCity}
            loading={loading}
            onPress={handleConfirm}
          />
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
    paddingTop: 24,
    paddingBottom: 16,
  },

  // Header
  header: {
    marginBottom: 24,
  },
  heading: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
    letterSpacing: -0.5,
    marginBottom: 8,
  },
  subheading: {
    fontSize: 15,
    fontWeight: '400',
    color: colors.textSecondary,
    lineHeight: 20,
  },

  // City list
  listScroll: {
    flex: 1,
  },
  listContent: {
    gap: 12,
    paddingBottom: 16,
  },
  cityCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.cardBg,
    borderRadius: 14,
    paddingHorizontal: 16,
    paddingVertical: 18,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  cityCardSelected: {
    backgroundColor: colors.brandPurpleLight,
    borderColor: colors.brandPurple,
  },
  cityCardLeft: {
    flex: 1,
    gap: 4,
  },
  cityName: {
    fontSize: 17,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  cityNameSelected: {
    color: colors.brandPurple,
  },
  cityState: {
    fontSize: 13,
    fontWeight: '400',
    color: colors.textSecondary,
  },
  cityStateSelected: {
    color: colors.brandPurple,
    opacity: 0.85,
  },

  // Radio indicator
  radioRing: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioRingSelected: {
    borderColor: colors.brandPurple,
  },
  radioDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.brandPurple,
  },

  // Footer
  footer: {
    gap: 12,
    paddingTop: 12,
  },
  confirmButton: {
    height: 52,
    borderRadius: 14,
    backgroundColor: colors.brandPurple,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmButtonDisabled: {
    backgroundColor: colors.disabledBg,
  },
  confirmButtonLabel: {
    fontSize: 17,
    fontWeight: '600',
    color: colors.textOnPurple,
  },
  confirmButtonLabelDisabled: {
    color: colors.disabledText,
  },

  // Error
  errorBanner: {
    backgroundColor: colors.errorLight,
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  errorText: {
    fontSize: 15,
    color: colors.errorBase,
    fontWeight: '500',
    textAlign: 'center',
  },
});

// ─── Storybook Meta ───────────────────────────────────────────────────────────

const meta: Meta<typeof CitySelectionScreen> = {
  title: 'Covey/Screens/CitySelection',
  component: CitySelectionScreen,
  parameters: {
    docs: {
      description: {
        component: `
**City Selection Screen** — shown after sign-in on first launch and accessible from Settings.

**Personas**:
- Priya (newcomer) — selects Seattle once on first launch
- James (traveler) — returns here whenever he arrives in a new city

**Journey**: Step 3 of \`first-launch-onboard\` (Priya Nair persona).

### Design decisions
- Radio card pattern (not a dropdown) — clearer affordance on mobile, larger tap targets
- Member count shown per city — social proof, helps James evaluate curation quality
- Confirm button disabled until selection — prevents accidental empty submission
- Scrollable list — future-proofs for more cities without layout change

### Accessibility
- Card group uses \`accessibilityRole="radiogroup"\` and each card is \`role="radio"\`
- Selected state communicated via border + background (not color alone) per WCAG 1.4.1
- Confirm button \`accessibilityState.disabled\` matches visual disabled state
        `,
      },
    },
    viewport: { defaultViewport: 'iphone14' },
  },
  argTypes: {
    selectedCityId: {
      control: 'select',
      options: [null, 'seattle', 'tacoma', 'bellevue', 'olympia'],
      description: 'Currently selected city ID',
    },
    loading: {
      control: 'boolean',
      description: 'Loading state after confirm is tapped',
    },
    errorMessage: {
      control: 'text',
      description: 'Error message displayed when city save fails',
    },
  },
};

export default meta;
type Story = StoryObj<typeof CitySelectionScreen>;

// ─── Stories ──────────────────────────────────────────────────────────────────

/** No city selected — confirm button disabled */
export const Default: Story = {
  args: {
    selectedCityId: null,
    loading: false,
  },
};

/** Seattle selected — confirm button active */
export const SeattleSelected: Story = {
  args: {
    selectedCityId: 'seattle',
    loading: false,
  },
  parameters: {
    docs: {
      description: { story: 'Seattle card shows purple border + tinted background. Confirm button becomes active. This is the expected state for Priya (new Seattle resident) and Maya (commuter).' },
    },
  },
};

/** Tacoma selected — alternate city */
export const TacomaSelected: Story = {
  args: {
    selectedCityId: 'tacoma',
    loading: false,
  },
};

/** Confirm tapped — loading spinner in button */
export const Confirming: Story = {
  args: {
    selectedCityId: 'seattle',
    loading: true,
  },
  parameters: {
    docs: {
      description: { story: 'City confirmed, app is persisting the choice and navigating to the feed. Button shows a spinner and is non-interactive.' },
    },
  },
};

/** Persistence failed — error banner shown */
export const NetworkError: Story = {
  args: {
    selectedCityId: 'seattle',
    loading: false,
    errorMessage: 'Could not save your city. Check your connection and try again.',
  },
  parameters: {
    docs: {
      description: { story: 'Error state if the API call fails. User can retry by tapping Continue again.' },
    },
  },
};

/** Interactive playground */
export const Playground: Story = {
  args: {
    selectedCityId: null,
    loading: false,
    errorMessage: undefined,
  },
};
