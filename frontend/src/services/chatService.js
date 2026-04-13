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
    this.lastHeartbeatSent = 0;
    this.heartbeatThrottleMs = 1*60*1000; // 1 minute
    ChatService.instance = this;
  }

  connect() {
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    let token = localStorage.getItem('accessToken');
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
        heartbeatIncoming: 0,
        heartbeatOutgoing: 0,
        onConnect: () => {
          console.log('Chat service connected');
          this.connected = true;
          resolve(this);
        },
        onStompError: (frame) => {
          console.error('Chat STOMP error:', frame);
          this.connected = false;
          this.connectionPromise = null;
          token = localStorage.getItem('accessToken');
          reject(frame);
        },
        onWebSocketError: (event) => {
          console.error('Chat WebSocket error:', event);
          this.connected = false;
          this.connectionPromise = null;
          token = localStorage.getItem('accessToken');
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

  // Send heartbeat to update presence
  sendHeartbeat(force = false) {
    if (!this.connected) {
      console.warn('Cannot send heartbeat - not connected');
      return;
    }

    const now = Date.now();
    
    // Throttle heartbeats to once every 10 minutes unless forced
    if (!force && (now - this.lastHeartbeatSent < this.heartbeatThrottleMs)) {
      //console.log('Heartbeat throttled - last sent', Math.round((now - this.lastHeartbeatSent) / 1000), 'seconds ago');
      return;
    }

    //console.log('📡 Sending presence heartbeat', now - this.lastHeartbeatSent, this.heartbeatThrottleMs);
    
    this.stompClient.publish({
      destination: '/app/presence.heartbeat',
      body: JSON.stringify({
        timestamp: new Date().toISOString()
      })
    });

    this.lastHeartbeatSent = now;
  }

  // Send a chat message (handles both DIRECT and GROUP types)
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

    console.log('📤 Sending message:', formattedMessage);

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(formattedMessage)
    });

    // Send heartbeat on message activity (forced)
    this.sendHeartbeat();
  }

  // Send typing indicator
  sendTyping(typingMessage) {
    if (!this.connected) return;
    
    console.log('📤 Sending typing indicator');
    
    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(typingMessage)
    });

    // Send heartbeat on typing activity (forced)
    this.sendHeartbeat();
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
      const destination = `/topic/group.${groupId}`;
      
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