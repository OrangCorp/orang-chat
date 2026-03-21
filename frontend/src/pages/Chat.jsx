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
  Button
} from '@mui/material';
import {
  Send as SendIcon,
  ArrowBack as ArrowBackIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { conversationService, messageService } from '../services/messageService';
import { userService } from '../services/userService';
import chatService from '../services/chatService';
import MessageBubble from '../components/chat/MessageBubble';

const Chat = () => {
  const { chatId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const subscriptionSetupRef = useRef(false);

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
        setMessages(messagePage.content);
        setHasMore(!messagePage.last);
        setPage(0);

        // Fetch profiles for all participants
        const participantIds = found.participantIds || [];
        const profiles = {};
        await Promise.all(participantIds.map(async (pid) => {
          if (pid !== user.id) {
            try {
              const profile = await userService.getProfile(pid);
              profiles[pid] = profile;
            } catch (e) {
              profiles[pid] = { displayName: pid.slice(0, 8) };
            }
          }
        }));
        setParticipants(profiles);

        // Scroll to bottom after messages load
        setTimeout(scrollToBottom, 100);
      } catch (err) {
        console.error('Failed to load chat:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [chatId, user]);

  // WebSocket connection and subscriptions
  useEffect(() => {
    if (!conversation || !user || !chatId) return;
    if (subscriptionSetupRef.current) return;
    subscriptionSetupRef.current = true;

    const setupWebSocket = async () => {
      try {
        await chatService.connect();
        setConnected(true);

        const handleMessage = (message) => {
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
          
          // Check if message belongs to this conversation
          // For direct: sender or recipient should be the other user
          // For group: recipient should be group ID

          const messageWithId = {
            ...message,
            id: message.id || `ws-${Date.now()}-${Math.random()}`
          };
          
          
          // Add message to state
          setMessages(prev => {
            console.log(message);
            console.log(prev);
            return [...prev, messageWithId];
          });
          scrollToBottom();
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

  // Scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Load more messages (pagination)
  const loadMore = async () => {
    if (!hasMore || loadingMore) return;
    setLoadingMore(true);
    try {
      const nextPage = page + 1;
      const messagePage = await messageService.getMessages(chatId, nextPage, 50);
      setMessages(prev => [...messagePage.content, ...prev]);
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
    scrollToBottom();

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

  // Get display name for a user
  const getDisplayName = (userId) => {
    if (userId === user.id) return 'You';
    return participants[userId]?.displayName || userId.slice(0, 8);
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
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !conversation) {
    return (
      <Box sx={{ p: 3 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="error">{error || 'Conversation not found'}</Typography>
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

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper square elevation={1} sx={{ p: 2, display: 'flex', alignItems: 'center' }}>
        <IconButton onClick={() => navigate('/')} sx={{ mr: 2 }}>
          <ArrowBackIcon />
        </IconButton>
        <Avatar 
          src={conversation.type === 'DIRECT' ? participants[otherParticipantId]?.avatarUrl : null}
          sx={{ mr: 2 }}
        >
          {conversation.type === 'GROUP' ? 'G' : 
            (participants[otherParticipantId]?.displayName?.charAt(0) || '?')}
        </Avatar>
        <Typography variant="h6">
          {conversation.type === 'GROUP' 
            ? conversation.name || 'Group Chat' 
            : participants[otherParticipantId]?.displayName || 'Unknown User'}
        </Typography>
        {conversation.type === 'DIRECT' && participants[otherParticipantId]?.isOnline && (
          <Typography variant="caption" sx={{ ml: 2, color: 'success.main' }}>
            ● Online
          </Typography>
        )}
        <Typography variant="caption" sx={{ ml: 2 }}>
          {connected ? '🟢 Connected' : '🔴 Disconnected'}
        </Typography>
      </Paper>

      {/* Messages area */}
      <Box sx={{ flex: 1, overflowY: 'auto', p: 2, bgcolor: '#efe4c3' }}>
        {hasMore && (
          <Box sx={{ textAlign: 'center', my: 2 }}>
            <Button onClick={loadMore} disabled={loadingMore}>
              {loadingMore ? <CircularProgress size={24} /> : 'Load older messages'}
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
                key={index}
                message={msg}
                isOwn={msg.senderId === user.id}
                senderName={getDisplayName(msg.senderId)}
                time={getMessageTime(msg)}
              />
            ))}
          </Box>
        )}
        
        {typingUsers.size > 0 && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'left', mt: 1, fontStyle: 'italic' }}>
            {Array.from(typingUsers).map(id => getDisplayName(id)).join(', ')} {typingUsers.size === 1 ? 'is' : 'are'} typing...
          </Typography>
        )}
        <div ref={messagesEndRef} />
      </Box>

      {/* Input area */}
      <Paper square elevation={3} sx={{ p: 2 }}>
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
    </Box>
  );
};

export default Chat;