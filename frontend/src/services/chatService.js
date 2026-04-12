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

    const { senderId, recipientId, conversationId, content, type } = messagePayload;

    // Build base message
    const formattedMessage = {
      senderId,
      content,
      type,
      timestamp: new Date().toISOString()
    };

    // Add the appropriate field based on message type
    if (type === 'DIRECT') {
      if (!recipientId) {
        console.error('recipientId is required for DIRECT messages');
        return;
      }
      formattedMessage.recipientId = recipientId;
    } else if (type === 'GROUP') {
      if (!conversationId) {
        console.error('conversationId is required for GROUP messages');
        return;
      }
      formattedMessage.conversationId = conversationId;
    }

    console.log('📤 Sending formatted message:', formattedMessage);

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
    this.connect().then(() => {
      // Private messages come to user-specific queue
      const destination = `/user/queue/messages`;
      
      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => console.error('Failed to subscribe to private queue:', err));
  }

  subscribeToGroup(groupId, callback) {
    this.connect().then(() => {
      // Based on GroupEventListener, it likely sends to a topic like this:
      const destination = `/topic/group.${groupId}`;  // Note the DOT, not slash
      
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