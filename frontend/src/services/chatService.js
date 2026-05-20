// services/chatService.js
import { Client } from '@stomp/stompjs';

const isDev = import.meta.env.DEV;
const log = (...args) => isDev && console.log(...args);
const logError = (...args) => isDev && console.error(...args);
const logWarn = (...args) => isDev && console.warn(...args);

const getBrokerURL = () => {
  if (import.meta.env.DEV) {
    return 'ws://localhost:8080/ws';
  }
  return '/ws';
};

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
    this.heartbeatThrottleMs = 1*60*1000;
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
        brokerURL: getBrokerURL(),
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (msg) => log('Chat STOMP:', msg),
        reconnectDelay: 5000,
        heartbeatIncoming: 0,
        heartbeatOutgoing: 0,
        onConnect: () => {
          log('Chat service connected');
          this.connected = true;
          resolve(this);
        },
        onStompError: (frame) => {
          logError('Chat STOMP error:', frame);
          this.connected = false;
          this.connectionPromise = null;
          token = localStorage.getItem('accessToken');
          reject(frame);
        },
        onWebSocketError: (event) => {
          logError('Chat WebSocket error:', event);
          this.connected = false;
          this.connectionPromise = null;
          token = localStorage.getItem('accessToken');
          reject(event);
        },
        onDisconnect: () => {
          log('Chat service disconnected');
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

  sendHeartbeat(force = false) {
    if (!this.connected) {
      logWarn('Cannot send heartbeat - not connected');
      return;
    }

    const now = Date.now();
    
    if (!force && (now - this.lastHeartbeatSent < this.heartbeatThrottleMs)) {
      return;
    }
    
    this.stompClient.publish({
      destination: '/app/presence.heartbeat',
      body: JSON.stringify({
        timestamp: new Date().toISOString()
      })
    });

    this.lastHeartbeatSent = now;
  }

  sendMessage(messagePayload) {
    if (!this.connected) {
      logError('Chat service not connected');
      return;
    }

    const { senderId, recipientId, conversationId, content, type, attachmentIds, replyToMessageId } = messagePayload;

    const formattedMessage = {
      messageId: crypto.randomUUID(),
      senderId,
      content,
      type,
      timestamp: new Date().toISOString()
    };

    if (type === 'DIRECT') {
      if (!recipientId) {
        logError('recipientId is required for DIRECT messages');
        return;
      }
      formattedMessage.recipientId = recipientId;
    } else if (type === 'GROUP') {
      if (!conversationId) {
        logError('conversationId is required for GROUP messages');
        return;
      }
      formattedMessage.conversationId = conversationId;
    }

    if (attachmentIds && attachmentIds.length > 0) {
      formattedMessage.attachmentIds = attachmentIds;
    }

    if (replyToMessageId) {
      formattedMessage.replyToMessageId = replyToMessageId;
    }

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(formattedMessage)
    });

    this.sendHeartbeat(true);
  }

  sendTyping(typingMessage) {
    if (!this.connected) return;
    
    typingMessage.content = 'typing...';
    log('📤 Sending typing indicator', typingMessage);
    
    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(typingMessage)
    });

    this.sendHeartbeat();
  }

  subscribeToUserQueue(callback) {
    this.connect().then(() => {
      const destination = '/user/queue/messages';
      log('📡 Subscribing to private queue:', destination);
      
      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        log('📨 Received message:', data);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => logError('Failed to subscribe to private queue:', err));
  }

  subscribeToPrivateMessages(callback) {
    this.subscribeToUserQueue(callback);
  }

  subscribeToNotificationCount(callback) {
    this.connect().then(() => {
      const destination = '/user/queue/notifications/unread';
      log('📡 Subscribing to notification count:', destination);
      
      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => logError('Failed to subscribe to notification count:', err));
  }

  subscribeToGroup(groupId, callback) {
    this.connect().then(() => {
      const destination = `/topic/group.${groupId}`;
      
      if (this.subscriptions.has(destination)) {
        this.subscriptions.get(destination).unsubscribe();
      }

      const subscription = this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        log('📨 Received group message:', data);
        callback(data);
      });

      this.subscriptions.set(destination, subscription);
    }).catch(err => logError('Failed to subscribe to group:', err));
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