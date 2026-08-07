import React, { useEffect, useRef } from 'react';
import { Linking } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer, useNavigationContainerRef } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { useAuthStore } from './src/store/authStore';
import { initializeFirebase } from './src/services/firebase';
import {
  requestPermissionAndRegister,
  addTokenRefreshListener,
  addForegroundListener,
  addTapListener,
} from './src/services/notifications';

import SignInScreen from './src/screens/SignInScreen';
import CityPickerScreen from './src/screens/CityPickerScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import SpotDetailScreen from './src/screens/SpotDetailScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

SplashScreen.preventAutoHideAsync();

const AuthStack = () => (
  <Stack.Navigator screenOptions={{ headerShown: false }}>
    <Stack.Screen name="SignIn" component={SignInScreen} />
  </Stack.Navigator>
);

const OnboardingStack = () => (
  <Stack.Navigator screenOptions={{ headerShown: false }}>
    <Stack.Screen name="CityPicker" component={CityPickerScreen} />
  </Stack.Navigator>
);

const AppTabs = () => (
  <Tab.Navigator
    screenOptions={{
      headerShown: true,
      tabBarStyle: {
        backgroundColor: '#fff',
        borderTopColor: '#e5e5e5',
      },
    }}
  >
    <Tab.Screen name="Home" component={HomeScreen} options={{ title: 'Weekly Spot' }} />
    <Tab.Screen name="Profile" component={ProfileScreen} options={{ title: 'Profile' }} />
  </Tab.Navigator>
);

// Root stack wraps tabs + modals so they can be navigated to from anywhere
const AppStack = () => (
  <Stack.Navigator>
    <Stack.Screen name="Tabs" component={AppTabs} options={{ headerShown: false }} />
    <Stack.Screen
      name="ChangeCity"
      component={CityPickerScreen}
      options={{ title: 'Change City', presentation: 'modal' }}
    />
    <Stack.Screen
      name="SpotDetail"
      component={SpotDetailScreen}
      options={{ headerShown: false, presentation: 'modal' }}
    />
  </Stack.Navigator>
);

export default function App() {
  const { user, isInitializing, initializeAuth } = useAuthStore();
  const navigationRef = useNavigationContainerRef<any>();
  const notificationListenersRef = useRef<any[]>([]);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        await initializeFirebase();
        await initializeAuth();
      } catch (e) {
        console.error('Failed to initialize app:', e);
      } finally {
        await SplashScreen.hideAsync();
      }
    };

    bootstrap();
  }, [initializeAuth]);

  // Handle deep links (covey://spot?spotId=X&city=Y&weekId=Z)
  useEffect(() => {
    const handleUrl = ({ url }: { url: string }) => {
      const match = url.match(/^covey:\/\/spot\?(.+)$/);
      if (!match) return;
      const params = Object.fromEntries(new URLSearchParams(match[1]));
      navigationRef.current?.navigate('SpotDetail', params);
    };

    const subscription = Linking.addEventListener('url', handleUrl);
    Linking.getInitialURL().then(url => { if (url) handleUrl({ url }); });

    return () => subscription.remove();
  }, []);

  // Register for push notifications once user is fully onboarded
  useEffect(() => {
    const hasCity = user?.city && user.city.length > 0;
    if (!user || !hasCity) return;

    requestPermissionAndRegister();

    const listeners = [
      addTokenRefreshListener(),
      addForegroundListener(),
      addTapListener((data) => {
        navigationRef.current?.navigate('SpotDetail', data);
      }),
    ];
    notificationListenersRef.current = listeners;

    return () => {
      listeners.forEach(l => l.remove());
    };
  }, [user?.uid, user?.city]);

  if (isInitializing) {
    return null;
  }

  const hasCity = user?.city && user.city.length > 0;

  return (
    <SafeAreaProvider>
      <NavigationContainer ref={navigationRef}>
        {!user ? (
          <AuthStack />
        ) : !hasCity ? (
          <OnboardingStack />
        ) : (
          <AppStack />
        )}
      </NavigationContainer>
      <StatusBar />
    </SafeAreaProvider>
  );
}
