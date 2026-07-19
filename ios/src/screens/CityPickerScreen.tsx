import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useAuthStore } from '../store/authStore';
import { apiClient_methods } from '../services/api';

const CITIES = [
  'Seattle',
  'Tacoma',
  'Bainbridge Island',
];

export default function CityPickerScreen() {
  const navigation = useNavigation();
  const { user } = useAuthStore();
  const [selectedCity, setSelectedCity] = useState<string | null>(user?.city ?? null);
  const [loading, setLoading] = useState(false);

  const isChanging = !!user?.city;

  const handleSelectCity = async () => {
    if (!selectedCity) {
      Alert.alert('Error', 'Please select a city');
      return;
    }

    try {
      setLoading(true);
      await apiClient_methods.updateUser({ city: selectedCity });

      useAuthStore.setState(state => ({
        user: state.user ? { ...state.user, city: selectedCity } : null,
      }));

      if (isChanging && navigation.canGoBack()) {
        navigation.goBack();
      }
    } catch (error: any) {
      console.error('Failed to update city:', error);
      Alert.alert('Error', error.response?.data?.error || 'Failed to update city');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <ScrollView style={styles.contentContainer}>
        <View style={styles.header}>
          <Text style={styles.title}>
            {isChanging ? 'Change Your City' : 'Select Your City'}
          </Text>
          <Text style={styles.subtitle}>
            We'll show you weekly spots in your area
          </Text>
        </View>

        <View style={styles.citiesContainer}>
          {CITIES.map((city) => (
            <TouchableOpacity
              key={city}
              style={[
                styles.cityButton,
                selectedCity === city && styles.cityButtonSelected,
              ]}
              onPress={() => setSelectedCity(city)}
              disabled={loading}
            >
              <Text
                style={[
                  styles.cityButtonText,
                  selectedCity === city && styles.cityButtonTextSelected,
                ]}
              >
                {city}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <TouchableOpacity
          style={[
            styles.continueButton,
            (!selectedCity || selectedCity === user?.city) && styles.continueButtonDisabled,
          ]}
          onPress={handleSelectCity}
          disabled={!selectedCity || loading || selectedCity === user?.city}
        >
          {loading ? (
            <ActivityIndicator size="small" color="#fff" />
          ) : (
            <Text style={styles.continueButtonText}>
              {isChanging ? 'Save' : 'Continue'}
            </Text>
          )}
        </TouchableOpacity>

        {isChanging && (
          <TouchableOpacity
            style={styles.cancelButton}
            onPress={() => navigation.goBack()}
            disabled={loading}
          >
            <Text style={styles.cancelButtonText}>Cancel</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  contentContainer: {
    flex: 1,
    padding: 20,
  },
  header: {
    marginTop: 20,
    marginBottom: 40,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1F2937',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#6B7280',
    lineHeight: 24,
  },
  citiesContainer: {
    gap: 12,
  },
  cityButton: {
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    backgroundColor: '#f9fafb',
    alignItems: 'center',
  },
  cityButtonSelected: {
    borderColor: '#6B4CE6',
    backgroundColor: '#6B4CE6',
  },
  cityButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
  },
  cityButtonTextSelected: {
    color: '#fff',
  },
  footer: {
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  continueButton: {
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 8,
    backgroundColor: '#6B4CE6',
    alignItems: 'center',
    justifyContent: 'center',
  },
  continueButtonDisabled: {
    backgroundColor: '#D1D5DB',
  },
  continueButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  cancelButton: {
    marginTop: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#6B7280',
    fontSize: 16,
    fontWeight: '500',
  },
});
