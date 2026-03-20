import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class ChatService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.connectionPromise = null;
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
      // Use native WebSocket - NO SOCKJS!
      this.stompClient = new Client({
        brokerURL: 'ws://localhost:8080/ws', // Direct WebSocket URL
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (msg) => console.log('STOMP:', msg),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Connected to WebSocket');
          this.connected = true;
          resolve(this);
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.connected = false;
          this.connectionPromise = null;
          reject(frame);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.connected = false;
          this.connectionPromise = null;
          reject(event);
        },
        onDisconnect: () => {
          console.log('Disconnected from WebSocket');
          this.connected = false;
          this.connectionPromise = null;
        }
      });

      this.stompClient.activate();
    });

    return this.connectionPromise;
  }

  // Disconnect
  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.deactivate();
      this.connected = false;
      this.connectionPromise = null;
      this.subscriptions.clear();
    }
  }

  // Subscribe to private messages for current user
  subscribeToPrivateMessages(userId, callback) {
    this.connect().then(() => {
      const destination = `/user/${userId}/queue/messages`;
      
      if (this.subscriptions.has(destination)) {
        this.subscriptions.get(destination).unsubscribe();
      }

      const subscription = this.stompClient.subscribe(destination, (message) => {
        const chatMessage = JSON.parse(message.body);
        callback(chatMessage);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to private messages:', err));
  }

  // Subscribe to group messages
  subscribeToGroup(groupId, callback) {
    this.connect().then(() => {
      const destination = `/topic/group/${groupId}`;
      
      if (this.subscriptions.has(destination)) {
        this.subscriptions.get(destination).unsubscribe();
      }

      const subscription = this.stompClient.subscribe(destination, (message) => {
        const chatMessage = JSON.parse(message.body);
        callback(chatMessage);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to group:', err));
  }

  // Send a message
  sendMessage(message) {
    if (!this.connected) {
      console.error('Not connected to WebSocket');
      return;
    }

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        id: message.id || crypto.randomUUID(),
        senderId: message.senderId,
        recipientId: message.recipientId,
        content: message.content,
        type: message.type || 'TEXT',
        timestamp: new Date().toISOString()
      })
    });
  }

  // Send typing indicator
  sendTyping(senderId, recipientId, isTyping = true) {
    if (!this.connected) return;

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        id: crypto.randomUUID(),
        senderId: senderId,
        recipientId: recipientId,
        content: isTyping ? 'typing...' : '',
        type: 'TYPING',
        timestamp: new Date().toISOString()
      })
    });
  }

  // Unsubscribe from a destination
  unsubscribe(destination) {
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  // Check connection status
  isConnected() {
    return this.connected;
  }
}

// Export as singleton
export default new ChatService();