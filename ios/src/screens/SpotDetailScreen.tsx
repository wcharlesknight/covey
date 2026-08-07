import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Linking,
  Alert,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { apiClient_methods } from '../services/api';

type SpotDetailParams = {
  SpotDetail: { spotId?: string; weekId?: string; city?: string };
};

interface Venue {
  name: string;
  address: string;
  lat: number;
  lng: number;
}

interface RsvpCounts {
  yes: number;
  no: number;
  interested: number;
}

interface WeeklySpot {
  id: string;
  venue: Venue;
  date: string;
  rsvpCounts: RsvpCounts;
  userRsvp?: 'yes' | 'no' | 'interested';
}

const RSVP_OPTIONS: { status: 'yes' | 'no' | 'interested'; label: string }[] = [
  { status: 'yes', label: 'Going' },
  { status: 'interested', label: 'Maybe' },
  { status: 'no', label: 'Skip' },
];

const renderStars = (rating: number) => {
  const full = Math.floor(rating);
  const half = rating - full >= 0.5;
  return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(5 - full - (half ? 1 : 0));
};

export default function SpotDetailScreen() {
  const navigation = useNavigation<any>();
  const route = useRoute<RouteProp<SpotDetailParams, 'SpotDetail'>>();
  const { city } = route.params ?? {};

  const [spot, setSpot] = useState<WeeklySpot | null>(null);
  const [loading, setLoading] = useState(true);
  const [rsvpSubmitting, setRsvpSubmitting] = useState(false);

  useEffect(() => {
    loadSpot();
  }, []);

  const loadSpot = async () => {
    try {
      const response = await apiClient_methods.getFeed();
      const items: any[] = Array.isArray(response.data) ? response.data : [];
      const item = (route.params?.weekId
        ? items.find((i: any) => i.invite?.weekId === route.params?.weekId)
        : null) ?? items[0] ?? null;

      if (item) {
        const statusMap: Record<string, 'yes' | 'no' | 'interested' | undefined> = {
          YES: 'yes', NO: 'no', INTERESTED: 'interested', INVITED: undefined,
        };
        setSpot({
          id: item.invite.id,
          venue: {
            name: item.spot.venueName,
            address: item.spot.venueAddress,
            lat: 0,
            lng: 0,
          },
          date: new Date(item.spot.weekStartDate).toISOString(),
          rsvpCounts: { yes: 0, no: 0, interested: 0 },
          userRsvp: statusMap[item.invite.status],
        });
      }
    } catch (err: any) {
      Alert.alert('Error', err.response?.data?.error || 'Failed to load spot');
    } finally {
      setLoading(false);
    }
  };

  const handleRsvp = useCallback(async (newStatus: 'yes' | 'no' | 'interested') => {
    if (!spot || rsvpSubmitting) return;
    const prevRsvp = spot.userRsvp;
    if (prevRsvp === newStatus) return;

    const newCounts = { ...spot.rsvpCounts };
    if (prevRsvp) newCounts[prevRsvp] = Math.max(0, newCounts[prevRsvp] - 1);
    newCounts[newStatus] += 1;

    setSpot(s => s ? { ...s, userRsvp: newStatus, rsvpCounts: newCounts } : s);
    setRsvpSubmitting(true);

    try {
      await apiClient_methods.submitRsvp(spot.id, newStatus);
    } catch (err: any) {
      setSpot(s => s ? { ...s, userRsvp: prevRsvp, rsvpCounts: spot.rsvpCounts } : s);
      Alert.alert('Error', err.response?.data?.error || 'Failed to submit RSVP');
    } finally {
      setRsvpSubmitting(false);
    }
  }, [spot, rsvpSubmitting]);

  const openMaps = () => {
    if (!spot?.venue) return;
    const q = encodeURIComponent(spot.venue.address || spot.venue.name);
    Linking.openURL(`maps://maps.apple.com/?q=${q}`).catch(() =>
      Linking.openURL(`https://maps.apple.com/?q=${q}`)
    );
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.headerCity}>{city ?? 'This Week'}</Text>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.closeButton}>
          <Text style={styles.closeText}>✕</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#6B4CE6" />
        </View>
      ) : !spot ? (
        <View style={styles.center}>
          <Text style={styles.emptyTitle}>No spot yet this week</Text>
          <Text style={styles.emptySubtitle}>Check back Friday!</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.card}>
            <Text style={styles.venueName}>{spot.venue?.name}</Text>

            <View style={styles.ratingRow}>
              <Text style={styles.stars}>{renderStars(4.7)}</Text>
              <Text style={styles.ratingText}>4.7</Text>
            </View>

            <View style={styles.divider} />

            <View style={styles.addressRow}>
              <Text style={styles.pinEmoji}>📍</Text>
              <Text style={styles.address}>{spot.venue?.address}</Text>
            </View>

            <TouchableOpacity style={styles.mapsButton} onPress={openMaps} activeOpacity={0.8}>
              <Text style={styles.mapsButtonText}>Open in Maps</Text>
              <Text style={styles.mapsArrow}>→</Text>
            </TouchableOpacity>

            <View style={styles.divider} />

            <Text style={styles.rsvpHeading}>Who's going?</Text>

            <View style={styles.rsvpCounts}>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{spot.rsvpCounts?.yes ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Going</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{spot.rsvpCounts?.interested ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Maybe</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{spot.rsvpCounts?.no ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Skip</Text>
              </View>
            </View>

            <View style={styles.rsvpButtons}>
              {RSVP_OPTIONS.map(({ status, label }) => (
                <TouchableOpacity
                  key={status}
                  style={[
                    styles.rsvpButton,
                    spot.userRsvp === status && styles.rsvpButtonActive,
                    rsvpSubmitting && styles.rsvpButtonDisabled,
                  ]}
                  onPress={() => handleRsvp(status)}
                  disabled={rsvpSubmitting}
                >
                  <Text style={[
                    styles.rsvpButtonText,
                    spot.userRsvp === status && styles.rsvpButtonTextActive,
                  ]}>
                    {label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  headerCity: {
    fontSize: 16,
    fontWeight: '700',
    color: '#6B4CE6',
  },
  closeButton: {
    padding: 4,
  },
  closeText: {
    fontSize: 18,
    color: '#9CA3AF',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#9CA3AF',
  },
  content: {
    padding: 20,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  venueName: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1F2937',
    marginBottom: 8,
  },
  ratingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 16,
  },
  stars: {
    fontSize: 16,
    color: '#F59E0B',
    letterSpacing: 1,
  },
  ratingText: {
    fontSize: 14,
    color: '#6B7280',
    fontWeight: '600',
  },
  divider: {
    height: 1,
    backgroundColor: '#f3f4f6',
    marginVertical: 16,
  },
  addressRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    marginBottom: 16,
  },
  pinEmoji: {
    fontSize: 16,
    marginTop: 1,
  },
  address: {
    flex: 1,
    fontSize: 15,
    color: '#4B5563',
    lineHeight: 22,
  },
  mapsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#6B4CE6',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 20,
  },
  mapsButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  mapsArrow: {
    fontSize: 18,
    color: '#fff',
  },
  rsvpHeading: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 16,
  },
  rsvpCounts: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 16,
  },
  rsvpCount: {
    alignItems: 'center',
  },
  rsvpNumber: {
    fontSize: 28,
    fontWeight: '700',
    color: '#6B4CE6',
  },
  rsvpLabel: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 2,
  },
  rsvpButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  rsvpButton: {
    flex: 1,
    paddingVertical: 11,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
  },
  rsvpButtonActive: {
    backgroundColor: '#6B4CE6',
    borderColor: '#6B4CE6',
  },
  rsvpButtonDisabled: {
    opacity: 0.6,
  },
  rsvpButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6B7280',
  },
  rsvpButtonTextActive: {
    color: '#fff',
  },
});
