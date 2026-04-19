// services/notificationService.js

const API_BASE_URL = '/api';

const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

class NotificationService {
  constructor() {
    this.vapidPublicKey = null;
  }

  async getVapidPublicKey() {
    if (this.vapidPublicKey) return this.vapidPublicKey;
    const response = await fetch(`${API_BASE_URL}/push/vapid-public-key`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch VAPID public key');
    const data = await response.json();
    this.vapidPublicKey = data.publicKey;
    return this.vapidPublicKey;
  }

  async subscribe(subscription) {
    const response = await fetch(`${API_BASE_URL}/push/subscribe`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(subscription)
    });
    if (!response.ok) throw new Error('Failed to subscribe');
    return response.json();
  }

  async unsubscribe(endpoint) {
    const response = await fetch(`${API_BASE_URL}/push/unsubscribe?endpoint=${encodeURIComponent(endpoint)}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to unsubscribe');
  }

  async getSubscriptions() {
    const response = await fetch(`${API_BASE_URL}/push/subscriptions`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch subscriptions');
    return response.json();
  }

  async muteConversation(conversationId, until = null) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/notifications/mute`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ until })
    });
    if (!response.ok) {
        // Try to get error message, but handle empty response
        try {
        const error = await response.json();
        throw new Error(error.message || 'Failed to mute conversation');
        } catch {
        throw new Error(`Failed to mute conversation (${response.status})`);
        }
    }
    // Success - no need to return anything
    }


  async unmuteConversation(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/notifications/unmute`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to unmute conversation');
  }

  async getNotificationPreferences(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/notifications`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch notification preferences');
    return response.json();
  }

  // Permission and subscription management
  async requestPermission() {
    if (!('Notification' in window)) {
      throw new Error('This browser does not support notifications');
    }
    const permission = await Notification.requestPermission();
    return permission === 'granted';
  }

  async getCurrentSubscription() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return null;
    }
    const registration = await navigator.serviceWorker.ready;
    return registration.pushManager.getSubscription();
  }

  async subscribeToPush() {
    const vapidKey = await this.getVapidPublicKey();
    const registration = await navigator.serviceWorker.ready;
    
    let subscription = await registration.pushManager.getSubscription();
    if (subscription) {
      // Already subscribed, maybe update backend?
      return subscription;
    }

    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.urlBase64ToUint8Array(vapidKey)
    });

    // Save to backend
    await this.subscribe(subscription);
    return subscription;
  }

  async unsubscribeFromPush() {
    const subscription = await this.getCurrentSubscription();
    if (subscription) {
      await subscription.unsubscribe();
      await this.unsubscribe(subscription.endpoint);
    }
  }

  urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/\-/g, '+')
      .replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }
}

export default new NotificationService();