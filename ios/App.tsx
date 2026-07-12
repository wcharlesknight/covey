import React, { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { useAuthStore } from './src/store/authStore';
import { initializeFirebase } from './src/services/firebase';

import SignInScreen from './src/screens/SignInScreen';
import CityPickerScreen from './src/screens/CityPickerScreen';
import HomeScreen from './src/screens/HomeScreen';
import ProfileScreen from './src/screens/ProfileScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

SplashScreen.preventAutoHideAsync();

const AuthStack = ({ userCity }: { userCity?: string }) => (
  <Stack.Navigator screenOptions={{ headerShown: false }}>
    <Stack.Screen name="SignIn" component={SignInScreen} />
    {!userCity && <Stack.Screen name="CityPicker" component={CityPickerScreen} />}
  </Stack.Navigator>
);

const AppStack = () => (
  <Tab.Navigator
    screenOptions={{
      headerShown: true,
      tabBarStyle: {
        backgroundColor: '#fff',
        borderTopColor: '#e5e5e5',
      },
    }}
  >
    <Tab.Screen
      name="Home"
      component={HomeScreen}
      options={{
        title: 'Weekly Spot',
      }}
    />
    <Tab.Screen
      name="Profile"
      component={ProfileScreen}
      options={{
        title: 'Profile',
      }}
    />
  </Tab.Navigator>
);

export default function App() {
  const { user, isInitializing, initializeAuth } = useAuthStore();

  useEffect(() => {
    const bootstrap = async () => {
      try {
        // Initialize Firebase
        await initializeFirebase();
        // Initialize auth state
        await initializeAuth();
      } catch (e) {
        console.error('Failed to initialize app:', e);
      } finally {
        await SplashScreen.hideAsync();
      }
    };

    bootstrap();
  }, [initializeAuth]);

  if (isInitializing) {
    return null; // Splash screen still visible
  }

  const userCity = user?.city ?? undefined;
  const isUserOnboarded = user && userCity;

  return (
    <SafeAreaProvider>
      <NavigationContainer>
        {isUserOnboarded ? (
          <AppStack />
        ) : (
          <AuthStack userCity={userCity} />
        )}
      </NavigationContainer>
      <StatusBar />
    </SafeAreaProvider>
  );
}
