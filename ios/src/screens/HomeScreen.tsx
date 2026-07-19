import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
  Linking,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useAuthStore } from '../store/authStore';
import { apiClient_methods } from '../services/api';

interface Venue {
  name: string;
  address: string;
  description?: string;
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

interface Feed {
  current: WeeklySpot | null;
  history: WeeklySpot[];
}

const openMaps = (venue: Venue) => {
  const query = encodeURIComponent(venue.address || venue.name);
  Linking.openURL(`maps://maps.apple.com/?q=${query}`).catch(() =>
    Linking.openURL(`https://maps.apple.com/?q=${query}`)
  );
};

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

const RSVP_OPTIONS: { status: 'yes' | 'no' | 'interested'; label: string }[] = [
  { status: 'yes', label: 'Going' },
  { status: 'interested', label: 'Maybe' },
  { status: 'no', label: 'Skip' },
];

const HomeScreen = () => {
  const navigation = useNavigation<any>();
  const { user } = useAuthStore();
  const [feed, setFeed] = useState<Feed | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFeed();
  }, []);

  const loadFeed = async () => {
    try {
      setError(null);
      const response = await apiClient_methods.getFeed();
      setFeed(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load feed');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadFeed();
  }, []);

  const handleRsvp = async (inviteId: string, status: 'yes' | 'no' | 'interested') => {
    try {
      await apiClient_methods.submitRsvp(inviteId, status);
      loadFeed();
    } catch (err: any) {
      Alert.alert('Error', err.response?.data?.error || 'Failed to submit RSVP');
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#6B4CE6" />
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centerContainer}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={loadFeed}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={onRefresh}
          tintColor="#6B4CE6"
        />
      }
    >
      <View style={styles.header}>
        <Text style={styles.weeklyTitle}>This Week's Spot</Text>
        <TouchableOpacity
          style={styles.cityPill}
          onPress={() => navigation.navigate('ChangeCity')}
        >
          <Text style={styles.cityPillText}>{user?.city ?? 'Set city'}</Text>
          <Text style={styles.cityPillChevron}> ›</Text>
        </TouchableOpacity>
        {feed?.current && (
          <Text style={styles.date}>{formatDate(feed.current.date)}</Text>
        )}
      </View>

      {feed?.current ? (
        <View style={styles.spotCard}>
          <Text style={styles.venueName}>{feed.current.venue?.name}</Text>

          <TouchableOpacity
            style={styles.addressRow}
            onPress={() => openMaps(feed.current!.venue)}
            activeOpacity={0.7}
          >
            <Text style={styles.venueAddress}>{feed.current.venue?.address}</Text>
            <Text style={styles.mapsLink}>Open in Maps ›</Text>
          </TouchableOpacity>

          {feed.current.venue?.description ? (
            <Text style={styles.venueDescription}>{feed.current.venue.description}</Text>
          ) : null}

          <View style={styles.rsvpSection}>
            <View style={styles.rsvpCountContainer}>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.yes ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Going</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.interested ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Maybe</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.no ?? 0}</Text>
                <Text style={styles.rsvpLabel}>Skip</Text>
              </View>
            </View>

            <View style={styles.rsvpButtonContainer}>
              {RSVP_OPTIONS.map(({ status, label }) => (
                <TouchableOpacity
                  key={status}
                  style={[
                    styles.rsvpButton,
                    feed.current!.userRsvp === status && styles.rsvpButtonActive,
                  ]}
                  onPress={() => handleRsvp(feed.current!.id, status)}
                >
                  <Text
                    style={[
                      styles.rsvpButtonText,
                      feed.current!.userRsvp === status && styles.rsvpButtonTextActive,
                    ]}
                  >
                    {label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </View>
      ) : (
        <View style={styles.emptyCard}>
          <Text style={styles.emptyTitle}>No spot picked yet</Text>
          <Text style={styles.emptySubtitle}>
            This week's spot for {user?.city ?? 'your city'} hasn't been selected yet.
            Check back Friday!
          </Text>
        </View>
      )}

      {feed?.history && feed.history.length > 0 && (
        <View style={styles.historySection}>
          <Text style={styles.historySectionTitle}>Past Spots</Text>
          {feed.history.map((spot) => (
            <View key={spot.id} style={styles.historyCard}>
              <View style={styles.historyCardHeader}>
                <Text style={styles.historyVenueName}>{spot.venue?.name}</Text>
                <Text style={styles.historyDate}>{formatDate(spot.date)}</Text>
              </View>
              {spot.venue?.address ? (
                <Text style={styles.historyAddress}>{spot.venue.address}</Text>
              ) : null}
              <View style={styles.historyRsvpRow}>
                <Text style={styles.historyRsvpItem}>
                  {spot.rsvpCounts?.yes ?? 0} going
                </Text>
                <Text style={styles.historyRsvpDot}>·</Text>
                <Text style={styles.historyRsvpItem}>
                  {spot.rsvpCounts?.interested ?? 0} maybe
                </Text>
                <Text style={styles.historyRsvpDot}>·</Text>
                <Text style={styles.historyRsvpItem}>
                  {spot.rsvpCounts?.no ?? 0} skip
                </Text>
              </View>
            </View>
          ))}
        </View>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 16,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  header: {
    marginBottom: 20,
  },
  weeklyTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 8,
  },
  cityPill: {
    flexDirection: 'row',
    alignSelf: 'flex-start',
    alignItems: 'center',
    backgroundColor: '#EDE9FE',
    borderRadius: 20,
    paddingVertical: 4,
    paddingHorizontal: 12,
    marginBottom: 8,
  },
  cityPillText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6B4CE6',
  },
  cityPillChevron: {
    fontSize: 16,
    color: '#6B4CE6',
  },
  date: {
    fontSize: 14,
    color: '#6B7280',
  },
  spotCard: {
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    padding: 20,
    marginBottom: 24,
    borderLeftWidth: 4,
    borderLeftColor: '#6B4CE6',
  },
  venueName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 6,
  },
  addressRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 8,
    marginBottom: 12,
  },
  venueAddress: {
    flex: 1,
    fontSize: 14,
    color: '#6B7280',
  },
  mapsLink: {
    fontSize: 13,
    color: '#6B4CE6',
    fontWeight: '600',
    flexShrink: 0,
  },
  venueDescription: {
    fontSize: 14,
    color: '#4B5563',
    lineHeight: 20,
    marginBottom: 16,
  },
  rsvpSection: {
    gap: 16,
    marginTop: 4,
  },
  rsvpCountContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  rsvpCount: {
    alignItems: 'center',
  },
  rsvpNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#6B4CE6',
  },
  rsvpLabel: {
    fontSize: 12,
    color: '#6B7280',
    marginTop: 4,
  },
  rsvpButtonContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  rsvpButton: {
    flex: 1,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    alignItems: 'center',
    backgroundColor: '#f3f4f6',
  },
  rsvpButtonActive: {
    backgroundColor: '#6B4CE6',
    borderColor: '#6B4CE6',
  },
  rsvpButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6B7280',
  },
  rsvpButtonTextActive: {
    color: 'white',
  },
  emptyCard: {
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    padding: 32,
    marginBottom: 24,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: 17,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#6B7280',
    textAlign: 'center',
    lineHeight: 20,
  },
  historySection: {
    marginBottom: 32,
  },
  historySectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 12,
  },
  historyCard: {
    backgroundColor: '#f9fafb',
    borderRadius: 8,
    padding: 14,
    marginBottom: 8,
  },
  historyCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 4,
  },
  historyVenueName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1F2937',
    flex: 1,
    marginRight: 8,
  },
  historyDate: {
    fontSize: 12,
    color: '#9CA3AF',
    flexShrink: 0,
  },
  historyAddress: {
    fontSize: 12,
    color: '#6B7280',
    marginBottom: 6,
  },
  historyRsvpRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 2,
  },
  historyRsvpItem: {
    fontSize: 12,
    color: '#9CA3AF',
  },
  historyRsvpDot: {
    fontSize: 12,
    color: '#D1D5DB',
  },
  errorText: {
    fontSize: 16,
    color: '#EF4444',
    marginBottom: 16,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#6B4CE6',
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  retryButtonText: {
    color: 'white',
    fontWeight: '600',
  },
});

export default HomeScreen;
