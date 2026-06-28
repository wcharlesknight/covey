import { initializeApp } from 'firebase/app';
import {
  getAuth,
  signInWithCustomToken,
  User,
  Auth,
} from 'firebase/auth';

interface FirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
}

export const createTestFirebaseApp = (config: FirebaseConfig) => {
  return initializeApp(config, 'test-app');
};

export const signInWithTestToken = async (
  auth: Auth,
  customToken: string
): Promise<User> => {
  const credential = await signInWithCustomToken(auth, customToken);
  return credential.user;
};

export const generateMockUser = (overrides?: Partial<User>): Partial<User> => {
  return {
    uid: 'test-uid-123',
    email: 'test@example.com',
    displayName: 'Test User',
    emailVerified: true,
    isAnonymous: false,
    metadata: {
      creationTime: new Date().toISOString(),
      lastSignInTime: new Date().toISOString(),
    },
    ...overrides,
  };
};
