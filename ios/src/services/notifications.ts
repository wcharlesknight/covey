import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import { apiClient_methods } from './api';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

export async function requestPermissionAndRegister(): Promise<void> {
  if (!Device.isDevice) {
    return;
  }

  const { status: existing } = await Notifications.getPermissionsAsync();
  let finalStatus = existing;

  if (existing !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    return;
  }

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'default',
      importance: Notifications.AndroidImportance.MAX,
    });
  }

  try {
    const tokenData = await Notifications.getExpoPushTokenAsync();
    await apiClient_methods.registerPushToken(tokenData.data);
  } catch (e) {
    console.warn('Failed to register push token:', e);
  }
}

export function addTokenRefreshListener(): Notifications.EventSubscription {
  return Notifications.addPushTokenListener(async ({ data: token }) => {
    try {
      await apiClient_methods.registerPushToken(token);
    } catch (e) {
      console.warn('Failed to re-register push token on refresh:', e);
    }
  });
}

export function addForegroundListener(): Notifications.EventSubscription {
  return Notifications.addNotificationReceivedListener(() => {
    // Foreground display handled by setNotificationHandler above
  });
}

export function addTapListener(
  onTap: () => void
): Notifications.EventSubscription {
  return Notifications.addNotificationResponseReceivedListener(() => {
    onTap();
  });
}
