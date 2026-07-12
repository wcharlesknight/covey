import { create } from 'zustand';
import {
  getAuth,
  signInWithCredential,
  OAuthProvider,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  updateProfile,
} from 'firebase/auth';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import * as AppleAuthentication from 'expo-apple-authentication';
import * as Google from 'expo-auth-session/providers/google';
import * as SecureStore from 'expo-secure-store';
import { initializeApiClient } from '../services/api';
import { getFirestoreInstance } from '../services/firebase';

interface User {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  city: string | null;
}

interface AuthStore {
  user: User | null;
  isInitializing: boolean;
  isLoading: boolean;
  error: string | null;
  initializeAuth: () => Promise<void>;
  signUpWithEmail: (email: string, password: string, displayName: string) => Promise<void>;
  signInWithEmail: (email: string, password: string) => Promise<void>;
  signInWithApple: () => Promise<void>;
  signInWithGoogle: (idToken: string, accessToken: string) => Promise<void>;
  signOut: () => Promise<void>;
  clearError: () => void;
}

const ensureUserRecord = async (firebaseUser: any) => {
  if (!firebaseUser) return;

  try {
    const db = getFirestoreInstance();
    if (!db) {
      console.warn('Firestore not initialized yet, skipping user record creation');
      return;
    }

    const userRef = doc(db, 'users', firebaseUser.uid);
    const userSnap = await getDoc(userRef);

    if (!userSnap.exists()) {
      await setDoc(userRef, {
        uid: firebaseUser.uid,
        email: firebaseUser.email,
        displayName: firebaseUser.displayName || '',
        photoURL: firebaseUser.photoURL || '',
        city: '',
        createdAt: new Date().toISOString(),
      });
      console.log('✅ Created user record in Firestore:', firebaseUser.uid);
    } else {
      console.log('✅ User record already exists:', firebaseUser.uid);
    }
  } catch (error) {
    console.error('Failed to create user record:', error);
  }
};

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  isInitializing: true,
  isLoading: false,
  error: null,

  initializeAuth: async () => {
    try {
      const auth = getAuth();

      // Initialize API client
      await initializeApiClient();

      // Set up auth state listener
      onAuthStateChanged(auth, (firebaseUser) => {
        if (firebaseUser) {
          set({
            user: {
              uid: firebaseUser.uid,
              email: firebaseUser.email,
              displayName: firebaseUser.displayName,
              photoURL: firebaseUser.photoURL,
              city: null,
            },
            isInitializing: false,
            error: null,
          });
        } else {
          set({
            user: null,
            isInitializing: false,
          });
        }
      });
    } catch (error: any) {
      set({
        error: error.message,
        isInitializing: false,
      });
    }
  },

  signUpWithEmail: async (email: string, password: string, displayName: string) => {
    set({ isLoading: true, error: null });
    try {
      const auth = getAuth();
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);

      if (displayName) {
        await updateProfile(userCredential.user, { displayName });
      }

      await ensureUserRecord(userCredential.user);

      set({
        user: {
          uid: userCredential.user.uid,
          email: userCredential.user.email,
          displayName: userCredential.user.displayName,
          photoURL: userCredential.user.photoURL,
          city: null,
        },
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || 'Failed to sign up',
        isLoading: false,
      });
      throw error;
    }
  },

  signInWithEmail: async (email: string, password: string) => {
    set({ isLoading: true, error: null });
    try {
      const auth = getAuth();
      const userCredential = await signInWithEmailAndPassword(auth, email, password);

      await ensureUserRecord(userCredential.user);

      set({
        user: {
          uid: userCredential.user.uid,
          email: userCredential.user.email,
          displayName: userCredential.user.displayName,
          photoURL: userCredential.user.photoURL,
          city: null,
        },
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || 'Failed to sign in',
        isLoading: false,
      });
      throw error;
    }
  },

  signInWithApple: async () => {
    set({ isLoading: true, error: null });
    try {
      const credential = await AppleAuthentication.signInAsync({
        requestedScopes: [
          AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
          AppleAuthentication.AppleAuthenticationScope.EMAIL,
        ],
      });

      const auth = getAuth();
      const provider = new OAuthProvider('apple.com');
      const authCredential = provider.credential({
        idToken: credential.identityToken as string,
        rawNonce: (credential as any).nonce,
      });

      const userCredential = await signInWithCredential(auth, authCredential);

      await ensureUserRecord(userCredential.user);

      set({
        user: {
          uid: userCredential.user.uid,
          email: userCredential.user.email,
          displayName: userCredential.user.displayName,
          photoURL: userCredential.user.photoURL,
          city: null,
        },
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || 'Failed to sign in with Apple',
        isLoading: false,
      });
      throw error;
    }
  },

  signInWithGoogle: async (idToken: string, accessToken: string) => {
    set({ isLoading: true, error: null });
    try {
      const auth = getAuth();
      const provider = new GoogleAuthProvider();
      const credential = GoogleAuthProvider.credential(idToken, accessToken);

      const userCredential = await signInWithCredential(auth, credential);

      await ensureUserRecord(userCredential.user);

      set({
        user: {
          uid: userCredential.user.uid,
          email: userCredential.user.email,
          displayName: userCredential.user.displayName,
          photoURL: userCredential.user.photoURL,
          city: null,
        },
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || 'Failed to sign in with Google',
        isLoading: false,
      });
      throw error;
    }
  },

  signOut: async () => {
    set({ isLoading: true, error: null });
    try {
      const auth = getAuth();
      await signOut(auth);
      set({
        user: null,
        isLoading: false,
      });
    } catch (error: any) {
      set({
        error: error.message || 'Failed to sign out',
        isLoading: false,
      });
      throw error;
    }
  },

  clearError: () => set({ error: null }),
}));
