import React, { useEffect, useRef } from 'react';
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

// Root stack wraps tabs + CityPicker modal so city can be changed from Profile
const AppStack = () => (
  <Stack.Navigator>
    <Stack.Screen name="Tabs" component={AppTabs} options={{ headerShown: false }} />
    <Stack.Screen
      name="ChangeCity"
      component={CityPickerScreen}
      options={{ title: 'Change City', presentation: 'modal' }}
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

  // Register for push notifications once user is fully onboarded
  useEffect(() => {
    const hasCity = user?.city && user.city.length > 0;
    if (!user || !hasCity) return;

    requestPermissionAndRegister();

    const listeners = [
      addTokenRefreshListener(),
      addForegroundListener(),
      addTapListener(() => {
        navigationRef.current?.navigate('Tabs', { screen: 'Home' });
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
