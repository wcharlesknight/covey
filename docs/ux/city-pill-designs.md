# City Pill Design Options

Two pills in the HomeScreen header:
- **Left pill** — which city you're *browsing* (the feed)
- **Right pill** — which city you get *notifications* for

Both currently look identical and the 🔔 emoji renders as `?` on device. All proposals below use `Ionicons` from `@expo/vector-icons` — verified reliable on iOS/Android.

**Both pills should stay separate** (not a single segmented switcher) — the point of the feature is showing two different cities simultaneously. A switcher hides one state at a time.

---

## Option 1 — Icon + Color (Most Distinct)

Eye icon + purple for browsing. Bell icon + amber for notifications. Maximum visual separation at a glance.

```
[ 👁  Bainbridge Island › ]   [ 🔔 Bainbridge Island › ]
     purple/lavender                  amber/cream
```

```tsx
// Viewing pill
<TouchableOpacity style={styles.viewingPill} onPress={showCityPicker}>
  <Ionicons name="eye-outline" size={14} color="#6B4CE6" />
  <Text style={styles.viewingPillText}>{viewedCity} ›</Text>
</TouchableOpacity>

// Notification pill
<TouchableOpacity style={styles.notifPill} onPress={() => navigation.navigate('ChangeCity')}>
  <Ionicons name="notifications-outline" size={14} color="#D97706" />
  <Text style={styles.notifPillText}>{user?.city ?? 'Set city'} ›</Text>
</TouchableOpacity>

viewingPill: {
  flexDirection: 'row', alignItems: 'center', gap: 6,
  backgroundColor: '#EDE9FE', borderRadius: 20,
  paddingVertical: 6, paddingHorizontal: 12,
},
viewingPillText: { fontSize: 14, fontWeight: '600', color: '#6B4CE6' },

notifPill: {
  flexDirection: 'row', alignItems: 'center', gap: 6,
  backgroundColor: '#FEF3E2', borderRadius: 20,
  paddingVertical: 6, paddingHorizontal: 12,
},
notifPillText: { fontSize: 14, fontWeight: '600', color: '#D97706' },
```

**Pros:** Immediately scannable; icon + color creates redundant signal (good for accessibility).  
**Cons:** Amber introduces a second brand color.

---

## Option 2 — Filled vs Outlined (Brand-Aligned)

Both stay purple. Viewing pill is solid (primary action); notification pill is outlined (secondary). Minimal divergence from current design.

```
[ Bainbridge Island › ]   [ Bainbridge Island › ]
   solid purple fill          purple outline/border
```

```tsx
// Viewing pill (filled)
<TouchableOpacity style={styles.viewingPill} onPress={showCityPicker}>
  <Ionicons name="eye-outline" size={14} color="#fff" />
  <Text style={styles.viewingPillText}>{viewedCity} ›</Text>
</TouchableOpacity>

// Notification pill (outlined)
<TouchableOpacity style={styles.notifPill} onPress={() => navigation.navigate('ChangeCity')}>
  <Ionicons name="notifications-outline" size={14} color="#6B4CE6" />
  <Text style={styles.notifPillText}>{user?.city ?? 'Set city'} ›</Text>
</TouchableOpacity>

viewingPill: {
  flexDirection: 'row', alignItems: 'center', gap: 6,
  backgroundColor: '#6B4CE6', borderRadius: 20,
  paddingVertical: 6, paddingHorizontal: 12,
},
viewingPillText: { fontSize: 14, fontWeight: '600', color: '#fff' },

notifPill: {
  flexDirection: 'row', alignItems: 'center', gap: 6,
  backgroundColor: 'transparent', borderRadius: 20,
  borderWidth: 1.5, borderColor: '#6B4CE6',
  paddingVertical: 6, paddingHorizontal: 12,
},
notifPillText: { fontSize: 14, fontWeight: '600', color: '#6B4CE6' },
```

**Pros:** Stays entirely within existing brand palette; no new colors to maintain.  
**Cons:** Subtler distinction — requires icons to carry the differentiation.

---

## Option 3 — Labels Above, Same Pill Style

Small uppercase labels ("BROWSE" / "ALERTS") float above two identical pills. Adds context without changing the pill design itself.

```
BROWSE              ALERTS
[ Bainbridge › ]   [ Bainbridge › ]
```

```tsx
<View style={styles.pillRow}>
  <View style={styles.pillGroup}>
    <Text style={styles.pillLabel}>BROWSE</Text>
    <TouchableOpacity style={styles.pill} onPress={showCityPicker}>
      <Ionicons name="eye-outline" size={14} color="#6B4CE6" />
      <Text style={styles.pillText}>{viewedCity} ›</Text>
    </TouchableOpacity>
  </View>

  <View style={styles.pillGroup}>
    <Text style={styles.pillLabel}>ALERTS</Text>
    <TouchableOpacity style={styles.pill} onPress={() => navigation.navigate('ChangeCity')}>
      <Ionicons name="notifications-outline" size={14} color="#6B4CE6" />
      <Text style={styles.pillText}>{user?.city ?? 'Set city'} ›</Text>
    </TouchableOpacity>
  </View>
</View>

pillRow: { flexDirection: 'row', gap: 12, marginBottom: 8 },
pillGroup: { gap: 3 },
pillLabel: {
  fontSize: 10, fontWeight: '700', color: '#9CA3AF',
  textTransform: 'uppercase', letterSpacing: 0.5,
},
pill: {
  flexDirection: 'row', alignItems: 'center', gap: 6,
  backgroundColor: '#EDE9FE', borderRadius: 20,
  paddingVertical: 6, paddingHorizontal: 12,
},
pillText: { fontSize: 14, fontWeight: '600', color: '#6B4CE6' },
```

**Pros:** Zero ambiguity; labels make the function explicit without relying on icon recognition.  
**Cons:** More vertical space — pushes the date line down.

---

## Option 4 — Icon-Only Pills (Compact)

Icons only in the pill, city name as text next to it, no chevron. Cleanest and most modern-feeling.

```
[ 👁 Bainbridge Island ]   [ 🔔 Bainbridge Island ]
```

Same as Option 1 but without the `›` arrow. The pill itself is the affordance for tapping.

**Pros:** Most compact; lets city name breathe.  
**Cons:** Without `›` the tappability is less obvious.

---

## Recommended

**Option 2 (filled vs outlined)** is the lowest-effort fix with the best result — it stays on-brand, swaps the broken emoji for a reliable Ionicons component, and clearly distinguishes the two pills without introducing new colors. Add `eye-outline` to the viewing pill while you're in there.

### Icon reference
| Purpose | Ionicons name |
|---|---|
| Viewing / browsing | `eye-outline` |
| Notifications | `notifications-outline` |
| Location (alternative) | `location-outline` |
