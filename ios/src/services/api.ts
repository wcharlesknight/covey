import axios, { AxiosInstance } from 'axios';
import { getAuthInstance } from './firebase';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL || 'https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev';

let apiClient: AxiosInstance | null = null;

export const initializeApiClient = async () => {
  apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Add auth token interceptor
  apiClient.interceptors.request.use(
    async (config) => {
      const auth = getAuthInstance();
      if (auth?.currentUser) {
        const token = await auth.currentUser.getIdToken();
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Handle response errors
  apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        // Token expired or invalid - auth store will handle logout
      }
      return Promise.reject(error);
    }
  );

  return apiClient;
};

export const getApiClient = () => {
  if (!apiClient) {
    throw new Error('API client not initialized');
  }
  return apiClient;
};

// API methods
export const apiClient_methods = {
  // User endpoints
  getUser: () => getApiClient().get('/me'),
  updateUser: (data: any) => getApiClient().patch('/me', data),

  // Feed endpoints
  getFeed: () => getApiClient().get('/me/feed'),

  // RSVP endpoints
  submitRsvp: (inviteId: string, status: 'yes' | 'no' | 'interested') =>
    getApiClient().post(`/invites/${inviteId}/rsvp`, { status }),

  // Push token registration
  registerPushToken: (token: string) =>
    getApiClient().post('/push-tokens', { token }),

  // Weekly job (for testing)
  triggerWeeklyJob: () =>
    getApiClient().post('/weekly-job', {}),
};
