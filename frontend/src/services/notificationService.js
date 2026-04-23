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

  // notificationService.js - Add this method
  handleNotificationPayload(payload) {
    // Called when push notification is received
    // This handles the payload format from the backend
    return {
      type: payload.type || 'default',
      title: payload.title || 'New Notification',
      body: payload.body || '',
      icon: payload.icon || '/logo192.png',
      badge: payload.badge || '/badge.png',
      tag: payload.tag || 'default',
      requireInteraction: payload.requireInteraction || false,
      data: {
        url: payload.url || '/',
        conversationId: payload.conversationId,
        messageId: payload.messageId,
        type: payload.type,
        contactId: payload.contactId,
        requesterId: payload.requesterId
      }
    };
  }

  async getCurrentSubscription() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return null;
    }
    const registration = await navigator.serviceWorker.ready;
    return registration.pushManager.getSubscription();
  }

  // notificationService.js - Fix the subscribeToPush method
  async subscribeToPush() {
    try {
      // 1. Get VAPID public key
      const vapidKey = await this.getVapidPublicKey();
      console.log('🔑 Got VAPID key');
      
      // 2. Get service worker registration
      const registration = await navigator.serviceWorker.ready;
      console.log('👷 Service worker ready');
      
      // 3. Create browser push subscription
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(vapidKey)
      });
      console.log('📱 Browser subscription created:', subscription.endpoint);
      
      // 4. Save to backend - this is the part that's failing!
      const token = localStorage.getItem('accessToken');
      console.log('🔐 Using token:', token ? token.substring(0, 20) + '...' : 'NO TOKEN');
      
      const response = await fetch('/api/push/subscribe', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          endpoint: subscription.endpoint,
          keys: {
            p256dh: btoa(String.fromCharCode.apply(null, new Uint8Array(subscription.getKey('p256dh')))),
            auth: btoa(String.fromCharCode.apply(null, new Uint8Array(subscription.getKey('auth'))))
          },
          expirationTime: subscription.expirationTime
        })
      });
      
      console.log('📡 Subscribe response status:', response.status);
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Failed to save subscription:', response.status, errorText);
        throw new Error(`Failed to save subscription: ${response.status}`);
      }
      
      const result = await response.json();
      console.log('✅ Subscription saved to backend:', result);
      return subscription;
    } catch (error) {
      console.error('❌ Push subscription failed:', error);
      throw error;
    }
  }

  // Helper to convert VAPID key
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

  async unsubscribeFromPush() {
    const subscription = await this.getCurrentSubscription();
    if (subscription) {
      await subscription.unsubscribe();
      await this.unsubscribe(subscription.endpoint);
    }
  }


  async initialize() {
    // Only initialize if we haven't already
    if (this._initialized) return;
    this._initialized = true;
    
    console.log('🔔 Initializing push notifications...');
    
    // Check if browser supports push
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      console.log('Push notifications not supported in this browser');
      return;
    }
    
    // Check if already subscribed
    const existingSubscription = await this.getCurrentSubscription();
    if (existingSubscription) {
      console.log('Already subscribed to push notifications');
      return;
    }
    
    // Request permission and subscribe
    try {
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        await this.subscribeToPush();
        console.log('✅ Push notifications initialized successfully');
      } else {
        console.log('Notification permission denied');
      }
    } catch (error) {
      console.error('Failed to initialize push notifications:', error);
    }
  }
}

export default new NotificationService();