// Add custom matchers or setup for tests
import '@testing-library/jest-native/extend-expect';

// Mock Firebase (optional - can be removed if testing with real Firebase)
jest.mock('./src/services/firebase', () => ({
  initializeFirebase: jest.fn().mockResolvedValue({}),
  getAuthInstance: jest.fn(),
  getFirestoreInstance: jest.fn(),
}));

// Mock Expo modules
jest.mock('expo-apple-authentication', () => ({
  signInAsync: jest.fn(),
}));

jest.mock('expo-auth-session/providers/google', () => ({
  useAuthRequest: jest.fn(() => [null, null, jest.fn()]),
}));

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));
