import { useState, useEffect, useCallback, useRef } from 'react';
import chatService from '../services/chatService';
import { useAuth } from '../context/AuthContext';

export const useChat = (conversationId, conversationType = 'DIRECT') => {
  const [messages, setMessages] = useState([]);
  const [typingUsers, setTypingUsers] = useState(new Set());
  const [connected, setConnected] = useState(false);
  const { user } = useAuth();
  
  // Use ref to track typing timeout
  const typingTimeoutRef = useRef(null);

  // Connect to WebSocket
  useEffect(() => {
    chatService.connect()
      .then(() => setConnected(true))
      .catch(err => console.error('Connection failed:', err));

    return () => {
      chatService.disconnect();
    };
  }, []);

  // Subscribe to messages
  useEffect(() => {
    if (!connected || !user || !conversationId) return;

    if (conversationType === 'DIRECT') {
      // For private messages, subscribe to your own queue
      chatService.subscribeToPrivateMessages(user.id, (message) => {
        // Only show messages for this conversation
        if (message.senderId === conversationId || message.recipientId === conversationId) {
          setMessages(prev => [...prev, message]);
        }

        // Handle typing indicators
        if (message.type === 'TYPING') {
          setTypingUsers(prev => {
            const newSet = new Set(prev);
            if (message.content === 'typing...') {
              newSet.add(message.senderId);
            } else {
              newSet.delete(message.senderId);
            }
            return newSet;
          });
        }
      });
    } else {
      // For groups, subscribe to the group topic
      chatService.subscribeToGroup(conversationId, (message) => {
        setMessages(prev => [...prev, message]);
        
        // Handle typing in groups
        if (message.type === 'TYPING') {
          setTypingUsers(prev => {
            const newSet = new Set(prev);
            if (message.content === 'typing...') {
              newSet.add(message.senderId);
            } else {
              newSet.delete(message.senderId);
            }
            return newSet;
          });
        }
      });
    }

    return () => {
      // Cleanup subscriptions
      if (conversationType === 'DIRECT') {
        chatService.unsubscribe(`/user/${user.id}/queue/messages`);
      } else {
        chatService.unsubscribe(`/topic/group/${conversationId}`);
      }
    };
  }, [connected, user, conversationId, conversationType]);

  // Send a message
  const sendMessage = useCallback((content) => {
    if (!user || !conversationId) return;

    const message = {
      id: crypto.randomUUID(),
      senderId: user.id,
      recipientId: conversationId,
      content,
      type: 'TEXT'
    };

    chatService.sendMessage(message);
    // Optimistically add to UI
    setMessages(prev => [...prev, message]);
  }, [user, conversationId]);

  // Send typing indicator
  const sendTyping = useCallback((isTyping) => {
    if (!user || !conversationId) return;

    // Clear previous timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    chatService.sendTyping(user.id, conversationId, isTyping);

    // Auto-send stop typing after 2 seconds
    if (isTyping) {
      typingTimeoutRef.current = setTimeout(() => {
        chatService.sendTyping(user.id, conversationId, false);
      }, 2000);
    }
  }, [user, conversationId]);

  return {
    messages,
    typingUsers: Array.from(typingUsers),
    connected,
    sendMessage,
    sendTyping
  };
};