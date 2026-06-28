import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { apiClient_methods } from '../services/api';

interface WeeklySpot {
  id: string;
  venue: {
    name: string;
    address: string;
    lat: number;
    lng: number;
  };
  date: string;
  rsvpCounts: {
    yes: number;
    no: number;
    interested: number;
  };
  userRsvp?: 'yes' | 'no' | 'interested';
}

const HomeScreen = () => {
  const [feed, setFeed] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFeed();
  }, []);

  const loadFeed = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient_methods.getFeed();
      setFeed(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRsvp = async (inviteId: string, status: 'yes' | 'no' | 'interested') => {
    try {
      await apiClient_methods.submitRsvp(inviteId, status);
      loadFeed(); // Reload feed to get updated RSVP counts
      Alert.alert('Success', `RSVP recorded as ${status}`);
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
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.weeklyTitle}>This Week's Spot</Text>
        <Text style={styles.date}>{feed?.current?.date}</Text>
      </View>

      {feed?.current && (
        <View style={styles.spotCard}>
          <Text style={styles.venueName}>{feed.current.venue?.name}</Text>
          <Text style={styles.venueAddress}>{feed.current.venue?.address}</Text>

          <View style={styles.rsvpSection}>
            <View style={styles.rsvpCountContainer}>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.yes || 0}</Text>
                <Text style={styles.rsvpLabel}>Yes</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.interested || 0}</Text>
                <Text style={styles.rsvpLabel}>Interested</Text>
              </View>
              <View style={styles.rsvpCount}>
                <Text style={styles.rsvpNumber}>{feed.current.rsvpCounts?.no || 0}</Text>
                <Text style={styles.rsvpLabel}>No</Text>
              </View>
            </View>

            <View style={styles.rsvpButtonContainer}>
              {['yes', 'interested', 'no'].map((status) => (
                <TouchableOpacity
                  key={status}
                  style={[
                    styles.rsvpButton,
                    feed.current.userRsvp === status && styles.rsvpButtonActive,
                  ]}
                  onPress={() =>
                    handleRsvp(feed.current.id, status as 'yes' | 'no' | 'interested')
                  }
                >
                  <Text
                    style={[
                      styles.rsvpButtonText,
                      feed.current.userRsvp === status && styles.rsvpButtonTextActive,
                    ]}
                  >
                    {status.charAt(0).toUpperCase() + status.slice(1)}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </View>
      )}

      {feed?.history && feed.history.length > 0 && (
        <View style={styles.historySection}>
          <Text style={styles.historySectionTitle}>Past Spots</Text>
          {feed.history.map((spot: WeeklySpot) => (
            <View key={spot.id} style={styles.historyCard}>
              <Text style={styles.historyVenueName}>{spot.venue?.name}</Text>
              <Text style={styles.historyDate}>{spot.date}</Text>
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
  },
  header: {
    marginBottom: 20,
  },
  weeklyTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 4,
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
    marginBottom: 8,
  },
  venueAddress: {
    fontSize: 14,
    color: '#6B7280',
    marginBottom: 20,
  },
  rsvpSection: {
    gap: 16,
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
  historySection: {
    marginBottom: 20,
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
    padding: 12,
    marginBottom: 8,
  },
  historyVenueName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1F2937',
  },
  historyDate: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 4,
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
