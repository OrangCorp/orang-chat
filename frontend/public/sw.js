// public/sw.js - Silent push notifications (no popups, in-app only)
let pendingNotifications = [];

self.addEventListener('push', event => {
  console.log('📨 Push event received!');
  
  let data = {};
  try {
    data = event.data?.json() || {};
    console.log('📦 Push payload:', JSON.stringify(data));
  } catch (e) {
    console.error('Failed to parse push data:', e);
  }
  
  const notificationData = {
    type: data.type || data.data?.type || 'default',
    title: data.title || 'New Notification',
    body: data.body || 'You have a new notification',
    icon: data.icon || '/logo192.png',
    url: data.url || data.data?.url || '/',
    conversationId: data.conversationId || data.data?.conversationId,
    messageId: data.messageId || data.data?.messageId,
    timestamp: Date.now()
  };
  
  console.log('📋 Processed notification:', notificationData.type);
  
  // Store for missed notifications
  pendingNotifications.push(notificationData);
  
  // IMMEDIATELY forward to all open app windows (in-app only, no popup)
  self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
    console.log(`📤 Forwarding notification to ${clients.length} open windows`);
    clients.forEach(client => {
      client.postMessage(notificationData);
    });
  });
  
  // NO showNotification - silent mode, in-app only
});

// Handle messages from the app
self.addEventListener('message', event => {
  console.log('📬 SW received message:', event.data?.type);
  
  if (event.data?.type === 'CHECK_MISSED') {
    const count = pendingNotifications.length;
    if (count > 0) {
      console.log(`📤 Sending ${count} pending notifications to app`);
      const latest = {};
      pendingNotifications.forEach(n => {
        latest[n.type] = n;
      });
      Object.values(latest).forEach(notification => {
        event.source.postMessage(notification);
      });
      pendingNotifications = [];
    } else {
      console.log('No pending notifications');
    }
  }
});

// Handle notification click - not needed since we don't show popups
// but keeping it in case the backend sends requireInteraction
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({
            type: 'NOTIFICATION_CLICKED',
            url: urlToOpen
          });
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }
    })
  );
});

console.log('🔥 Service Worker loaded and ready!');