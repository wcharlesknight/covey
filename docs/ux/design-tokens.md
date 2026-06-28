# Covey Design Tokens

Design system foundation for the Covey iOS app. All values follow a 4px base grid and target WCAG 2.1 AA contrast ratios.

---

## Color Palette

### Brand Primitives

| Token | Hex | Usage |
|-------|-----|-------|
| `covey-purple-50` | `#F0EDFB` | Light background tint, hover states |
| `covey-purple-100` | `#DDD6F7` | Subtle fills, skeleton loaders |
| `covey-purple-200` | `#BDB0EF` | Disabled text on light backgrounds |
| `covey-purple-400` | `#8B6EE8` | Secondary actions, icons |
| `covey-purple-600` | `#6B4CE6` | **Brand primary** — CTAs, active states |
| `covey-purple-700` | `#5A3DC4` | Pressed/active primary button |
| `covey-purple-900` | `#2D1E6B` | Dark mode primary surface |

### Warm Neutrals

| Token | Hex | Usage |
|-------|-----|-------|
| `warm-white` | `#FAFAF9` | App background (light mode) |
| `warm-gray-50` | `#F5F4F2` | Card background |
| `warm-gray-100` | `#E8E7E4` | Dividers, borders |
| `warm-gray-300` | `#C2C0BB` | Placeholder text |
| `warm-gray-500` | `#8A8782` | Secondary text, captions |
| `warm-gray-700` | `#4A4845` | Body text |
| `warm-gray-900` | `#1C1B18` | Headings, primary text |

### Semantic Colors

| Token | Hex | Meaning |
|-------|-----|---------|
| `success-light` | `#D1FAE5` | Yes RSVP background tint |
| `success-base` | `#22C55E` | Yes RSVP selected state (green) |
| `success-dark` | `#15803D` | Yes RSVP pressed, text on light bg |
| `warning-light` | `#FEF9C3` | Maybe RSVP background tint |
| `warning-base` | `#EAB308` | Maybe RSVP selected state (amber) |
| `warning-dark` | `#A16207` | Maybe RSVP pressed, text on light bg |
| `neutral-light` | `#F3F4F6` | Nope RSVP background tint |
| `neutral-base` | `#6B7280` | Nope RSVP selected state (gray) |
| `neutral-dark` | `#374151` | Nope RSVP pressed, text |
| `error-light` | `#FEE2E2` | Error state background |
| `error-base` | `#EF4444` | Error icons, destructive actions |
| `error-dark` | `#B91C1C` | Error text on light background |

### Dark Mode Overrides

| Light Token | Dark Equivalent | Dark Hex |
|-------------|-----------------|----------|
| `warm-white` | `dark-bg` | `#0F0E0D` |
| `warm-gray-50` | `dark-card-bg` | `#1E1D1A` |
| `warm-gray-900` | `dark-text-primary` | `#F5F4F2` |
| `warm-gray-700` | `dark-text-body` | `#C2C0BB` |
| `warm-gray-500` | `dark-text-secondary` | `#8A8782` |
| `covey-purple-600` | `dark-brand-primary` | `#8B6EE8` (lighter for contrast) |

---

## Typography Scale

Base font: **SF Pro** (system font on iOS). Line heights are in multiples of 4px.

### Type Ramp

| Token | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `display-xl` | 32px | 700 Bold | 40px | Screen hero headings (empty state) |
| `display-lg` | 28px | 700 Bold | 36px | Home feed venue name |
| `heading-md` | 22px | 600 SemiBold | 28px | Section headings ("This week's spot") |
| `heading-sm` | 18px | 600 SemiBold | 24px | Card sub-headings, dialog titles |
| `body-lg` | 17px | 400 Regular | 24px | Primary body copy (iOS default) |
| `body-md` | 15px | 400 Regular | 20px | Secondary body, descriptions |
| `body-sm` | 13px | 400 Regular | 18px | Tertiary info, counts |
| `caption` | 12px | 400 Regular | 16px | Legal, timestamps, footnotes |
| `label-lg` | 17px | 600 SemiBold | 22px | Button labels (primary) |
| `label-md` | 15px | 600 SemiBold | 20px | Button labels (secondary), tags |
| `label-sm` | 13px | 500 Medium | 18px | Badge labels, counts |

**Minimum readable size**: 12px (caption). No text below this size anywhere in the app.

### Dynamic Type Support

All text must support iOS Dynamic Type scaling. Use semantic text styles:

| Token | iOS Semantic Style |
|-------|--------------------|
| `display-xl` | `.largeTitle` |
| `heading-md` | `.title2` |
| `heading-sm` | `.title3` |
| `body-lg` | `.body` |
| `body-md` | `.callout` |
| `body-sm` | `.subheadline` |
| `caption` | `.caption` |
| `label-lg` | `.headline` |

---

## Spacing Scale

All spacing values are multiples of 4px.

| Token | Value | Usage |
|-------|-------|-------|
| `space-1` | 4px | Micro gap (icon + label) |
| `space-2` | 8px | Tight padding (badge inner) |
| `space-3` | 12px | Small component padding |
| `space-4` | 16px | Standard inner padding |
| `space-5` | 20px | Card inner padding |
| `space-6` | 24px | Section gap, generous padding |
| `space-8` | 32px | Large section separation |
| `space-10` | 40px | XL section separation |
| `space-12` | 48px | Screen-level vertical rhythm |
| `space-16` | 64px | Hero spacing |

### Safe Areas

| Token | Value | Usage |
|-------|-------|-------|
| `safe-top` | 59px | iPhone Dynamic Island safe area (top) |
| `safe-bottom` | 34px | Home indicator safe area (bottom) |
| `screen-h-pad` | 20px | Horizontal screen edge padding |
| `screen-v-pad` | 16px | Vertical content padding |

---

## Component Sizes

All interactive elements meet the 44px minimum tap target requirement (WCAG 2.5.5).

### Buttons

| Variant | Height | Padding H | Border Radius | Font Token |
|---------|--------|-----------|---------------|------------|
| Primary | 52px | 24px | 14px | `label-lg` |
| Secondary | 48px | 20px | 12px | `label-lg` |
| Ghost | 44px | 16px | 10px | `label-md` |
| RSVP Toggle | 48px | 20px | 24px (pill) | `label-md` |
| Sign In Social | 56px | 20px | 14px | `label-lg` |
| Icon button (tap target) | 44px | 10px | 22px | — |

### Cards

| Variant | Padding | Border Radius | Shadow |
|---------|---------|---------------|--------|
| Venue Hero Card | 0px (full bleed image) + 20px content | 20px | `shadow-md` |
| History Card | 12px | 14px | `shadow-sm` |
| City Selection Card | 16px | 14px | `shadow-sm` |

### Inputs

| Type | Height | Border Radius | Font Token |
|------|--------|---------------|------------|
| Text field | 48px | 12px | `body-lg` |
| Search field | 44px | 22px (pill) | `body-md` |

### Images

| Context | Dimensions | Border Radius |
|---------|------------|---------------|
| Venue hero photo | 100% width × 300px height | Top: 20px, Bottom: 0 |
| History card thumbnail | 64px × 64px | 10px |
| Venue photo full-screen | 100% width × 240px | 0 |

---

## Elevation / Shadow Scale

| Token | CSS Shadow | Usage |
|-------|-----------|-------|
| `shadow-sm` | `0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)` | History cards, subtle lift |
| `shadow-md` | `0 4px 12px rgba(0,0,0,0.12), 0 2px 4px rgba(0,0,0,0.08)` | Venue hero card, modals |
| `shadow-lg` | `0 10px 30px rgba(0,0,0,0.18)` | Floating toasts, overlays |

---

## Motion / Animation

Covey uses subtle animations that respect `prefers-reduced-motion`.

| Token | Value | Usage |
|-------|-------|-------|
| `duration-fast` | 150ms | Button press feedback |
| `duration-normal` | 250ms | State transitions (RSVP toggle) |
| `duration-slow` | 400ms | Screen transitions, toast |
| `easing-standard` | `cubic-bezier(0.4, 0, 0.2, 1)` | Most transitions |
| `easing-decelerate` | `cubic-bezier(0.0, 0.0, 0.2, 1)` | Entering elements |
| `easing-accelerate` | `cubic-bezier(0.4, 0.0, 1, 1)` | Exiting elements |
| `spring-rsvp` | `spring(1, 100, 10, 0)` | RSVP button bounce (React Native Reanimated) |

**Reduced motion**: When `prefers-reduced-motion: reduce` is active, replace all animations with a simple 0ms opacity transition.

---

## Iconography

Icon library: **SF Symbols** (iOS native) for consistency and Dynamic Type scaling.

| Context | SF Symbol | Size |
|---------|-----------|------|
| Address copy | `doc.on.doc` | 16px |
| Open in Maps | `map` | 16px |
| Rating star | `star.fill` | 14px |
| Confirmed | `checkmark.circle.fill` | 16px |
| Clock (hours) | `clock` | 14px |
| Notification | `bell.fill` | 20px |
| Pull to refresh | `arrow.clockwise` | 20px |

---

## WCAG 2.1 AA Contrast Reference

| Foreground | Background | Ratio | Pass? |
|------------|------------|-------|-------|
| `warm-gray-900` (#1C1B18) | `warm-white` (#FAFAF9) | 17.5:1 | AA + AAA |
| `warm-gray-700` (#4A4845) | `warm-white` (#FAFAF9) | 7.8:1 | AA + AAA |
| `warm-gray-500` (#8A8782) | `warm-white` (#FAFAF9) | 3.6:1 | AA (large text) |
| White (#FFFFFF) | `covey-purple-600` (#6B4CE6) | 4.8:1 | AA |
| White (#FFFFFF) | `success-dark` (#15803D) | 6.1:1 | AA + AAA |
| `success-dark` (#15803D) | `success-light` (#D1FAE5) | 5.2:1 | AA |
| White (#FFFFFF) | `warning-dark` (#A16207) | 4.7:1 | AA |
| White (#FFFFFF) | `neutral-base` (#6B7280) | 4.6:1 | AA |

---

## TypeScript Token Export

```typescript
// tokens.ts — import into React Native StyleSheet or styled-components

export const colors = {
  brand: {
    purple50:  '#F0EDFB',
    purple100: '#DDD6F7',
    purple200: '#BDB0EF',
    purple400: '#8B6EE8',
    purple600: '#6B4CE6',   // primary
    purple700: '#5A3DC4',
    purple900: '#2D1E6B',
  },
  neutral: {
    white:    '#FAFAF9',
    gray50:   '#F5F4F2',
    gray100:  '#E8E7E4',
    gray300:  '#C2C0BB',
    gray500:  '#8A8782',
    gray700:  '#4A4845',
    gray900:  '#1C1B18',
  },
  semantic: {
    successLight: '#D1FAE5',
    successBase:  '#22C55E',
    successDark:  '#15803D',
    warningLight: '#FEF9C3',
    warningBase:  '#EAB308',
    warningDark:  '#A16207',
    neutralLight: '#F3F4F6',
    neutralBase:  '#6B7280',
    neutralDark:  '#374151',
    errorLight:   '#FEE2E2',
    errorBase:    '#EF4444',
    errorDark:    '#B91C1C',
  },
} as const;

export const spacing = {
  s1:  4,
  s2:  8,
  s3:  12,
  s4:  16,
  s5:  20,
  s6:  24,
  s8:  32,
  s10: 40,
  s12: 48,
  s16: 64,
} as const;

export const radii = {
  sm:   10,
  md:   14,
  lg:   20,
  pill: 9999,
} as const;

export const fontSizes = {
  displayXl: 32,
  displayLg: 28,
  headingMd: 22,
  headingSm: 18,
  bodyLg:    17,
  bodyMd:    15,
  bodySm:    13,
  caption:   12,
} as const;

export const fontWeights = {
  regular:   '400' as const,
  medium:    '500' as const,
  semiBold:  '600' as const,
  bold:      '700' as const,
};

export const duration = {
  fast:   150,
  normal: 250,
  slow:   400,
} as const;
```
