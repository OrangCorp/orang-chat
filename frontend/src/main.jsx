// main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Register service worker for push notifications
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then((registration) => {
        console.log('Service Worker registered:', registration.scope);
      })
      .catch((error) => {
        console.error('Service Worker registration failed:', error);
      });
  });

  // Listen for messages from service worker
  // main.jsx - Keep the SW message listener but ONLY dispatch custom events
  navigator.serviceWorker.addEventListener('message', (event) => {
    const { type, url } = event.data || {};
    console.log('📨 SW message:', type, event.data);

    // Forward ALL push notification types as a single custom event
    const pushTypes = ['new_message', 'reaction', 'mention', 'group_added', 'member_added', 'direct_chat_created', 'message_deleted', 'message_edited'];
    
    if (pushTypes.includes(type)) {
      window.dispatchEvent(new CustomEvent('sw-message', { detail: event.data }));
    } else if (type === 'contact_request') {
      window.dispatchEvent(new CustomEvent('refresh-notifications'));
    } else if (type === 'NOTIFICATION_CLICKED' && url) {
      window.location.href = url;
    }
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);