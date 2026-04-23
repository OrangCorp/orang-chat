// public/sw.js - Updated to forward immediately
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
    contactId: data.contactId || data.data?.contactId,
    requesterId: data.requesterId || data.data?.requesterId,
    recipientId: data.recipientId || data.data?.recipientId,
    conversationId: data.conversationId || data.data?.conversationId,
    messageId: data.messageId || data.data?.messageId,
    timestamp: Date.now()
  };
  
  console.log('📋 Processed notification:', notificationData.type);
  
  // Store for missed notifications
  pendingNotifications.push(notificationData);
  
  // IMMEDIATELY forward to all open app windows
  self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
    console.log(`📤 Forwarding notification to ${clients.length} open windows`);
    clients.forEach(client => {
      client.postMessage(notificationData);
    });
  });
  
  // Show browser notification
  const options = {
    body: notificationData.body,
    icon: notificationData.icon,
    badge: '/badge.png',
    tag: `notification-${Date.now()}`,
    requireInteraction: true,
    data: notificationData
  };
  
  event.waitUntil(
    self.registration.showNotification(notificationData.title, options)
  );
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

// Handle notification click
self.addEventListener('notificationclick', event => {
  console.log('👆 Notification clicked');
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