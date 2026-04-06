// services/ChatService.js
import { Client } from '@stomp/stompjs';

class ChatService {
  constructor() {
    if (ChatService.instance) {
      return ChatService.instance;
    }
    
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.connectionPromise = null;
    ChatService.instance = this;
  }

  connect() {
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      return Promise.reject('No access token');
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      this.stompClient = new Client({
        brokerURL: 'ws://localhost:8083/ws',
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (msg) => console.log('Chat STOMP:', msg),
        reconnectDelay: 5000,
        //heartbeatIncoming: 0,  // Disable default heartbeat
        //heartbeatOutgoing: 0,  // Use custom heartbeat via presence service
        onConnect: () => {
          console.log('Chat service connected');
          this.connected = true;
          resolve(this);
        },
        onStompError: (frame) => {
          console.error('Chat STOMP error:', frame);
          this.connected = false;
          this.connectionPromise = null;
          reject(frame);
        },
        onWebSocketError: (event) => {
          console.error('Chat WebSocket error:', event);
          this.connected = false;
          this.connectionPromise = null;
          reject(event);
        },
        onDisconnect: () => {
          console.log('Chat service disconnected');
          this.connected = false;
          this.connectionPromise = null;
        }
      });

      this.stompClient.activate();
    });

    return this.connectionPromise;
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.deactivate();
      this.connected = false;
      this.connectionPromise = null;
      this.subscriptions.clear();
    }
  }

  // Send a message (updated for new API)
  sendMessage(messagePayload) {
    if (!this.connected) {
      console.error('Chat service not connected');
      return;
    }

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(messagePayload)
    });
  }

  // Subscribe to group messages
  subscribeToGroup(groupId, callback) {
    this.connect().then(() => {
      const destination = `/topic/group/${groupId}`;
      
      if (this.subscriptions.has(destination)) {
        this.subscriptions.get(destination).unsubscribe();
      }

      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to group:', err));
  }

  // Subscribe to user queue for private messages
  subscribeToUserQueue(callback) {
    this.connect().then(() => {
      const destination = `/user/queue/messages`;
      
      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    });
  }

  unsubscribe(destination) {
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  isConnected() {
    return this.connected;
  }
}

export default new ChatService();