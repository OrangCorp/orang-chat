// services/chatService.js
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
        brokerURL: 'ws://localhost:8080/ws',
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (msg) => console.log('Chat STOMP:', msg),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
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

  // Send a chat message
  sendMessage(messagePayload) {
    if (!this.connected) {
      console.error('Chat service not connected');
      return;
    }

    // Format the message with the correct type (DIRECT or GROUP)
    const formattedMessage = {
      senderId: messagePayload.senderId,
      recipientId: messagePayload.recipientId,
      content: messagePayload.content,
      type: messagePayload.type, // Should be 'DIRECT' or 'GROUP'
      timestamp: new Date().toISOString()
    };

    console.log('📤 Sending message:', formattedMessage);

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(formattedMessage)
    });
  }

  // Send typing indicator
  sendTyping(senderId, recipientId) {
    if (!this.connected) return;
    
    const typingMessage = {
      senderId: senderId,
      recipientId: recipientId,
      content:'typing...',
      type: 'TYPING',
      timestamp: new Date().toISOString()
    };
    
    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(typingMessage)
    });
  }

  // Subscribe to user's private message queue
  subscribeToUserQueue(callback) {
    this.connect().then(() => {
      const destination = `/user/queue/messages`;
      console.log('📡 Subscribing to private queue:', destination);
      
      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        console.log('📨 Received message:', data);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to private queue:', err));
  }

  // Alias for backward compatibility
  subscribeToPrivateMessages(callback) {
    this.subscribeToUserQueue(callback);
  }

  // Subscribe to group topic
  subscribeToGroup(groupId, callback) {
    this.connect().then(() => {
      const destination = `/topic/group/${groupId}`;
      
      if (this.subscriptions.has(destination)) {
        this.subscriptions.get(destination).unsubscribe();
      }

      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        console.log('📨 Received group message:', data);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to group:', err));
  }

  // Unsubscribe from a destination
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