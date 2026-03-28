import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  IconButton,
  Avatar,
  CircularProgress,
  Button,
  Chip,
  Stack,
  Fab,
} from '@mui/material';
import {
  Send as SendIcon,
  ArrowBack as ArrowBackIcon,
  Group as GroupIcon,
  Person as PersonIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { conversationService, messageService } from '../services/messageService';
import userService from '../services/userService';
import chatService from '../services/chatService';
import MessageBubble from '../components/chat/MessageBubble';

const Chat = () => {
  const { chatId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const subscriptionSetupRef = useRef(false);
  const shouldAutoScrollRef = useRef(false);

  // State
  const [conversation, setConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const [participants, setParticipants] = useState({});
  const [typingUsers, setTypingUsers] = useState(new Set());
  const [connected, setConnected] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [currentRecipient, setCurrentRecipient] = useState(null);
  const [isAtBottom, setIsAtBottom] = useState(true);

  // Load conversation details and initial messages
  useEffect(() => {
    if (!chatId || !user) return;

    const loadData = async () => {
      setLoading(true);
      setError(null);
      setMessages([]);
      
      try {
        // Get conversation details
        const convs = await conversationService.getConversations();
        const found = convs.find(c => c.id === chatId);
        if (!found) {
          setError('Conversation not found');
          return;
        }
        setConversation(found);

        // Set current recipient for filtering
        const recipient = found.type === 'DIRECT' 
          ? found.participantIds.find(id => id !== user.id)
          : found.id;
        setCurrentRecipient(recipient);

        // Load messages from REST API
        const messagePage = await messageService.getMessages(chatId, 0, 50);
        setMessages(messagePage.content.reverse());
        setHasMore(!messagePage.last);
        setPage(0);

        // Fetch profiles for all participants using the new batch method
        const participantIds = found.participantIds || [];
        
        if (participantIds.length > 0) {
          // Get all profiles at once (cached automatically)
          const profileMap = await userService.getProfiles(participantIds);
          
          // Convert Map to object for easier access
          const profiles = {};
          profileMap.forEach((profile, userId) => {
            profiles[userId] = profile;
          });
          setParticipants(profiles);
        }

        // Scroll to bottom after messages load (always initially)
        setTimeout(() => scrollToBottom(true), 100);
      } catch (err) {
        console.error('Failed to load chat:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [chatId, user]);

  // Scroll listener on messages container
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = container;
      const atBottom = scrollTop + clientHeight >= scrollHeight - 10; // threshold
      setIsAtBottom(atBottom);
    };

    container.addEventListener('scroll', handleScroll);
    // Initial check
    handleScroll();

    return () => container.removeEventListener('scroll', handleScroll);
  }, [messagesContainerRef.current]);

  // Effect to handle auto-scroll after new messages
  useEffect(() => {
    if (shouldAutoScrollRef.current) {
      scrollToBottom(true);
      shouldAutoScrollRef.current = false;
    }
  }, [messages]);

  // WebSocket connection and subscriptions
  useEffect(() => {
    if (!conversation || !user || !chatId) return;
    if (subscriptionSetupRef.current) return;
    subscriptionSetupRef.current = true;

    const setupWebSocket = async () => {
      try {
        await chatService.connect();
        setConnected(true);

        const handleMessage = async (message) => {
          console.log('📨 Received:', message);
          
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
            return;
          }
          
          // For new messages, ensure we have the sender's profile cached
          if (message.type === 'CHAT' && message.senderId && message.senderId !== user.id) {
            try {
              await userService.getProfile(message.senderId);
            } catch (err) {
              console.warn('Could not pre-cache sender profile:', err);
            }
          }
          
          const messageWithId = {
            ...message,
            id: message.id || `ws-${Date.now()}-${Math.random()}`
          };
          
          // Capture scroll state before adding message
          const wasAtBottom = checkIfAtBottom();
          if (wasAtBottom) {
            shouldAutoScrollRef.current = true;
          }
          
          // Add message to state
          setMessages(prev => [...prev, messageWithId]);
        };

        // Subscribe based on conversation type
        if (conversation.type === 'DIRECT') {
          chatService.subscribeToPrivateMessages(handleMessage);
        } else {
          chatService.subscribeToGroup(conversation.id, handleMessage);
        }
      } catch (err) {
        console.error('WebSocket connection failed:', err);
      }
    };

    setupWebSocket();

    return () => {
      subscriptionSetupRef.current = false;
      // Cleanup subscriptions
      if (conversation) {
        if (conversation.type === 'DIRECT') {
          chatService.unsubscribe(`/user/queue/messages`);
        } else {
          chatService.unsubscribe(`/topic/group/${conversation.id}`);
        }
      }
    };
  }, [conversation, user, chatId]);

  // Helper: check if user is at bottom of messages container
  const checkIfAtBottom = () => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const { scrollTop, scrollHeight, clientHeight } = container;
    return scrollTop + clientHeight >= scrollHeight - 10;
  };

  // Scroll to bottom, optionally force regardless of current position
  const scrollToBottom = (force = false) => {
    if (force || checkIfAtBottom()) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Load more messages (pagination)
  const loadMore = async () => {
    if (!hasMore || loadingMore) return;
    setLoadingMore(true);
    try {
      const nextPage = page + 1;
      const messagePage = await messageService.getMessages(chatId, nextPage, 50);
      
      // Get unique user IDs from older messages to pre-cache profiles
      const olderMessages = messagePage.content;
      const uniqueSenderIds = [...new Set(olderMessages.map(msg => msg.senderId))];
      
      // Pre-cache profiles for older messages (silent, don't block UI)
      Promise.all(uniqueSenderIds.map(id => userService.getProfile(id).catch(() => null)));
      
      setMessages(prev => [...olderMessages, ...prev]);
      setHasMore(!messagePage.last);
      setPage(nextPage);
    } catch (err) {
      console.error('Failed to load more messages:', err);
    } finally {
      setLoadingMore(false);
    }
  };

  // Send message
  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim() || sending || !conversation) return;

    const messageContent = input.trim();
    setInput('');
    setSending(true);

    const recipientId = conversation.type === 'DIRECT' 
      ? conversation.participantIds.find(id => id !== user.id) 
      : conversation.id;

    // Capture scroll state before adding message
    const wasAtBottom = checkIfAtBottom();
    if (wasAtBottom) {
      shouldAutoScrollRef.current = true;
    }

    // Create a temporary message for local display
    const tempMessage = {
      id: `temp-${Date.now()}`,
      senderId: user.id,
      recipientId: recipientId,
      content: messageContent,
      type: 'CHAT',
      createdAt: new Date().toISOString()
    };
    
    // Add to local messages immediately
    setMessages(prev => [...prev, tempMessage]);

    // Send via WebSocket
    chatService.sendMessage({
      senderId: user.id,
      recipientId: recipientId,
      content: messageContent,
      type: 'CHAT'
    });

    setSending(false);
  };

  // Handle typing indicator
  const handleTyping = (e) => {
    const value = e.target.value;
    setInput(value);

    if (!conversation) return;

    const recipientId = conversation.type === 'DIRECT' 
      ? conversation.participantIds.find(id => id !== user.id) 
      : conversation.id;

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    if (value.length > 0) {
      chatService.sendTyping(user.id, recipientId, true);
      typingTimeoutRef.current = setTimeout(() => {
        chatService.sendTyping(user.id, recipientId, false);
      }, 2000);
    } else {
      chatService.sendTyping(user.id, recipientId, false);
    }
  };

  // Get display name for a user (with cache)
  const getDisplayName = (userId) => {
    if (userId === user.id) return 'You';
    const profile = participants[userId];
    if (profile?.displayName) return profile.displayName;
    // Fallback to cached value from user service if not in participants state
    return userId.slice(0, 8);
  };

  // Get avatar for a user
  const getAvatar = (userId) => {
    if (userId === user.id) return null;
    return participants[userId]?.avatarUrl || null;
  };

  // Get online status
  const isUserOnline = (userId) => {
    if (userId === user.id) return true;
    return participants[userId]?.online || false;
  };

  // Get formatted time
  const getMessageTime = (msg) => {
    const date = msg.createdAt || msg.timestamp;
    if (!date) return 'Just now';
    try {
      return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return 'Just now';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !conversation) {
    return (
      <Box sx={{ p: 3 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="error" gutterBottom>
            {error || 'Conversation not found'}
          </Typography>
          <Button variant="contained" onClick={() => navigate('/')} sx={{ mt: 2 }}>
            Go Home
          </Button>
        </Paper>
      </Box>
    );
  }

  const otherParticipantId = conversation.type === 'DIRECT' 
    ? conversation.participantIds?.find(id => id !== user.id) 
    : null;

  const otherParticipantProfile = otherParticipantId ? participants[otherParticipantId] : null;

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper square elevation={1} sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
        <IconButton onClick={() => navigate('/')}>
          <ArrowBackIcon />
        </IconButton>
        
        <Avatar 
          src={conversation.type === 'DIRECT' ? otherParticipantProfile?.avatarUrl : null}
          sx={{ width: 48, height: 48 }}
        >
          {conversation.type === 'GROUP' ? 
            <GroupIcon /> : 
            (otherParticipantProfile?.displayName?.charAt(0)?.toUpperCase() || <PersonIcon />)}
        </Avatar>
        
        <Box sx={{ flex: 1 }}>
          <Typography variant="h6">
            {conversation.type === 'GROUP' 
              ? conversation.name || 'Group Chat' 
              : otherParticipantProfile?.displayName || 'Unknown User'}
          </Typography>
          {conversation.type === 'DIRECT' && (
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="caption" color={isUserOnline(otherParticipantId) ? 'success.main' : 'text.disabled'}>
                {isUserOnline(otherParticipantId) ? '● Online' : '○ Offline'}
              </Typography>
              {otherParticipantProfile?.lastSeen && !isUserOnline(otherParticipantId) && (
                <Typography variant="caption" color="text.secondary">
                  Last seen: {new Date(otherParticipantProfile.lastSeen).toLocaleString()}
                </Typography>
              )}
            </Stack>
          )}
        </Box>
        
        <Chip 
          label={connected ? 'Connected' : 'Disconnected'} 
          color={connected ? 'success' : 'error'}
          size="small"
          variant="outlined"
        />
      </Paper>

      {/* Messages area */}
      <Box 
        ref={messagesContainerRef}
        sx={{ 
          flex: 1, 
          overflowY: 'auto', 
          p: 2, 
          bgcolor: '#f5f5f5',
          display: 'flex',
          flexDirection: 'column'
        }}
      >
        {hasMore && (
          <Box sx={{ textAlign: 'center', my: 2 }}>
            <Button 
              onClick={loadMore} 
              disabled={loadingMore}
              size="small"
            >
              {loadingMore ? <CircularProgress size={20} /> : 'Load older messages'}
            </Button>
          </Box>
        )}
        
        {messages.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Typography color="text.secondary">
              No messages yet. Start the conversation!
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
            {messages.map((msg, index) => (
              <MessageBubble
                key={msg.id || index}
                message={msg}
                isOwn={msg.senderId === user.id}
                senderName={getDisplayName(msg.senderId)}
                senderAvatar={getAvatar(msg.senderId)}
                time={getMessageTime(msg)}
              />
            ))}
          </Box>
        )}
        
        {typingUsers.size > 0 && (
          <Typography 
            variant="caption" 
            color="text.secondary" 
            sx={{ 
              display: 'block', 
              textAlign: 'left', 
              mt: 1, 
              fontStyle: 'italic',
              pl: 2
            }}
          >
            {Array.from(typingUsers).map(id => getDisplayName(id)).join(', ')} 
            {typingUsers.size === 1 ? ' is' : ' are'} typing...
          </Typography>
        )}
        <div ref={messagesEndRef} />
      </Box>

      {/* Input area */}
      <Paper elevation={3} sx={{ p: 2 }}>
        <form onSubmit={handleSend}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              fullWidth
              size="small"
              placeholder="Type a message..."
              value={input}
              onChange={handleTyping}
              disabled={sending}
              multiline
              maxRows={4}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSend(e);
                }
              }}
            />
            <IconButton
              type="submit"
              color="primary"
              disabled={!input.trim() || sending}
              sx={{ alignSelf: 'flex-end' }}
            >
              <SendIcon />
            </IconButton>
          </Box>
        </form>
      </Paper>

      {/* Scroll to bottom button */}
      {!isAtBottom && (
        <Fab
          color="primary"
          size="small"
          sx={{ position: 'fixed', bottom: 80, right: 20, zIndex: 1000 }}
          onClick={() => scrollToBottom(true)}
        >
          <KeyboardArrowDownIcon />
        </Fab>
      )}
    </Box>
  );
};

export default Chat;