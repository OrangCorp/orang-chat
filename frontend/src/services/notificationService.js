// services/notificationService.js

const API_BASE_URL = import.meta.env.DEV ? '/api' : 'http://localhost:8080/api';

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
      const vapidKey = await this.getVapidPublicKey();
      const registration = await navigator.serviceWorker.ready;
      
      let subscription = await registration.pushManager.getSubscription();
      
      if (!subscription) {
        console.log('Creating new browser subscription...');
        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: this.urlBase64ToUint8Array(vapidKey)
        });
      }
      
      console.log('📱 Sending subscription to backend...');
      console.log('Endpoint:', subscription.endpoint);
      
      const token = localStorage.getItem('accessToken');
      const response = await fetch(`${API_BASE_URL}/push/subscribe`, {
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
          expirationTime: subscription.expirationTime || null
        })
      });
      
      console.log('Backend response status:', response.status);
      
      if (!response.ok) {
        const text = await response.text();
        console.error('Backend subscription failed:', response.status, text);
        throw new Error(`Subscribe failed: ${response.status}`);
      }
      
      const result = await response.json();
      console.log('✅ Backend subscription saved:', result.id);
      return subscription;
    } catch (error) {
      console.error('❌ subscribeToPush failed:', error);
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
    console.log('🔔 Initializing push notifications...');
    
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      console.log('Push not supported');
      return;
    }
    
    try {
      // Check if we have a browser subscription
      const registration = await navigator.serviceWorker.ready;
      const existingSubscription = await registration.pushManager.getSubscription();
      
      if (existingSubscription) {
        // We have a browser subscription - make sure backend knows about it
        console.log('📱 Browser subscription exists, syncing to backend...');
        await this.subscribeToPush();
        this._initialized = true;
        return;
      }
      
      // No subscription yet - create one
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        await this.subscribeToPush();
        console.log('✅ Push notifications initialized successfully');
        this._initialized = true;
      } else {
        console.log('Notification permission denied');
      }
    } catch (error) {
      console.error('Push initialization failed:', error);
    }
  }
}

export default new NotificationService();