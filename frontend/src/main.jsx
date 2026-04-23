// main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

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
}

navigator.serviceWorker.addEventListener('message', (event) => {
  const { type, url } = event.data || {};
  console.log('📨 SW message:', type);
  
  switch (type) {
    case 'contact_request':
      window.dispatchEvent(new CustomEvent('refresh-notifications'));
      break;
    case 'new_message':
    case 'reaction':
    case 'mention':
    case 'group_added':
      // These are handled by WebSocket in Chat component
      break;
    default:
      console.log('Unknown notification type:', type);
  }
  
  // If notification was clicked, navigate
  if (type === 'NOTIFICATION_CLICKED' && url) {
    window.location.href = url;
  }
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);