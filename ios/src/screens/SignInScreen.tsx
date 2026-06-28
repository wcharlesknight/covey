import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import * as AppleAuthentication from 'expo-apple-authentication';
import * as Google from 'expo-auth-session/providers/google';
import { useAuthStore } from '../store/authStore';

const SignInScreen = () => {
  const { signInWithApple, signInWithGoogle, isLoading, error } = useAuthStore();
  const [googleRequest, googleResponse, googlePromptAsync] = Google.useAuthRequest({
    clientId: process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID,
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
  });

  useEffect(() => {
    if (error) {
      Alert.alert('Sign In Error', error);
    }
  }, [error]);

  useEffect(() => {
    if (googleResponse?.type === 'success') {
      const { id_token, access_token } = googleResponse.params;
      signInWithGoogle(id_token, access_token).catch((err) => {
        Alert.alert('Google Sign In Error', err.message);
      });
    }
  }, [googleResponse]);

  const handleAppleSignIn = async () => {
    try {
      await signInWithApple();
    } catch (error: any) {
      // User likely cancelled
      if (error.code !== 'ERR_CANCELED') {
        Alert.alert('Apple Sign In Error', error.message);
      }
    }
  };

  const handleGoogleSignIn = async () => {
    try {
      await googlePromptAsync();
    } catch (error: any) {
      Alert.alert('Google Sign In Error', error.message);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Covey</Text>
        <Text style={styles.subtitle}>Find your weekly spot</Text>
      </View>

      <View style={styles.content}>
        <Text style={styles.description}>
          Sign in to discover local gathering spots and connect with your community.
        </Text>
      </View>

      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={[styles.button, styles.appleButton]}
          onPress={handleAppleSignIn}
          disabled={isLoading}
        >
          {isLoading ? (
            <ActivityIndicator color="white" />
          ) : (
            <Text style={styles.buttonText}>Continue with Apple</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.button, styles.googleButton]}
          onPress={handleGoogleSignIn}
          disabled={isLoading || !googleRequest}
        >
          {isLoading ? (
            <ActivityIndicator color="#1F2937" />
          ) : (
            <Text style={[styles.buttonText, styles.googleButtonText]}>
              Continue with Google
            </Text>
          )}
        </TouchableOpacity>
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          By signing in, you agree to our Terms of Service and Privacy Policy
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    paddingHorizontal: 20,
    paddingVertical: 40,
    justifyContent: 'space-between',
  },
  header: {
    marginTop: 40,
    marginBottom: 60,
  },
  title: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#6B4CE6',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#6B7280',
    fontWeight: '500',
  },
  content: {
    marginVertical: 40,
  },
  description: {
    fontSize: 16,
    color: '#4B5563',
    lineHeight: 24,
    marginBottom: 20,
  },
  buttonContainer: {
    gap: 12,
    marginBottom: 40,
  },
  button: {
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  appleButton: {
    backgroundColor: '#000',
  },
  googleButton: {
    backgroundColor: '#f3f4f6',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  buttonText: {
    fontSize: 16,
    fontWeight: '600',
    color: 'white',
  },
  googleButtonText: {
    color: '#1F2937',
  },
  footer: {
    marginBottom: 20,
  },
  footerText: {
    fontSize: 12,
    color: '#9CA3AF',
    textAlign: 'center',
    lineHeight: 18,
  },
});

export default SignInScreen;
