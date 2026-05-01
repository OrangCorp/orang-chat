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
  navigator.serviceWorker.addEventListener('message', (event) => {
    const { type, url } = event.data || {};
    console.log('📨 SW message:', type);

    switch (type) {
      case 'contact_request':
        window.dispatchEvent(new CustomEvent('refresh-notifications'));
        break;
      case 'new_message':
        window.dispatchEvent(new CustomEvent('new_message', { detail: event.data }));
        break;
      case 'reaction':
        window.dispatchEvent(new CustomEvent('reaction', { detail: event.data }));
        break;
      case 'mention':
        window.dispatchEvent(new CustomEvent('mention', { detail: event.data }));
        break;
      case 'group_added':
        // Forward to Header via custom event
        window.dispatchEvent(new CustomEvent('sw-message', { detail: event.data }));
        break;
      case 'NOTIFICATION_CLICKED':
        if (url) {
          window.location.href = url;
        }
        break;
      default:
        console.log('Unknown notification type:', type);
    }
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);