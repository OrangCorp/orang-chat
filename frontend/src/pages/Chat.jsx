import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, IconButton, Avatar, CircularProgress,
  Button, Chip, Stack, Fab,
} from '@mui/material';
import {
  Send as SendIcon, ArrowBack as ArrowBackIcon, Group as GroupIcon,
  Person as PersonIcon, KeyboardArrowDown as KeyboardArrowDownIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import messageService from '../services/messageService';
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
  const typingIndicatorTimeoutsRef = useRef(new Map());

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

  // Helper: check scroll position
  const checkIfAtBottom = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const { scrollTop, scrollHeight, clientHeight } = container;
    return scrollTop + clientHeight >= scrollHeight - 10;
  }, []);

  // Update isAtBottom state
  const updateIsAtBottom = useCallback(() => {
    setIsAtBottom(checkIfAtBottom());
  }, [checkIfAtBottom]);

  // Scroll listener
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;
    const handleScroll = () => updateIsAtBottom();
    container.addEventListener('scroll', handleScroll);
    updateIsAtBottom(); // initial check
    return () => container.removeEventListener('scroll', handleScroll);
  }, [updateIsAtBottom]);

  // Re-evaluate after any render (DOM changes)
  useEffect(() => {
    updateIsAtBottom();
  }, [messages, typingUsers, updateIsAtBottom]);

  // Scroll to bottom
  const scrollToBottom = useCallback((force = false) => {
    if (force || checkIfAtBottom()) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      // After scroll, re-evaluate position
      setTimeout(updateIsAtBottom, 200);
    }
  }, [checkIfAtBottom, updateIsAtBottom]);

  // --- Load conversation & messages ---
  useEffect(() => {
    if (!chatId || !user) return;
    const loadData = async () => {
      setLoading(true);
      try {
        const convs = await messageService.getConversations();
        const found = convs.find(c => c.id === chatId);
        if (!found) throw new Error('Conversation not found');
        setConversation(found);
        
        // Get the other participant for direct chats (participants is now array of objects)
        const otherParticipant = found.type === 'DIRECT' 
          ? found.participants.find(p => p.userId !== user.id) 
          : null;
        setCurrentRecipient(otherParticipant ? otherParticipant.userId : found.id);

        const messagePage = await messageService.getMessages(chatId, 0, 50);
        setMessages(messagePage.content.reverse());
        setHasMore(!messagePage.last);
        setPage(0);

        // Get all participant userIds from the participants array
        const participantIds = found.participants?.map(p => p.userId) || [];
        if (participantIds.length) {
          const profileMap = await userService.getProfiles(participantIds);
          const profiles = {};
          profileMap.forEach((profile, userId) => { profiles[userId] = profile; });
          setParticipants(profiles);
        }

        setTimeout(() => scrollToBottom(true), 100);
      } catch (err) {
        console.error(err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [chatId, user, scrollToBottom]);

  // Cleanup typing timeouts
  useEffect(() => {
    return () => {
      typingIndicatorTimeoutsRef.current.forEach(clearTimeout);
      typingIndicatorTimeoutsRef.current.clear();
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    };
  }, []);

  // --- WebSocket ---
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
          if (message.type === 'TYPING') {
            if (typingIndicatorTimeoutsRef.current.has(message.senderId))
              clearTimeout(typingIndicatorTimeoutsRef.current.get(message.senderId));
            const wasAtBottom = checkIfAtBottom();
            setTypingUsers(prev => new Set(prev).add(message.senderId));
            if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
            const timeout = setTimeout(() => {
              setTypingUsers(prev => {
                const next = new Set(prev);
                next.delete(message.senderId);
                return next;
              });
              typingIndicatorTimeoutsRef.current.delete(message.senderId);
            }, 3000);
            typingIndicatorTimeoutsRef.current.set(message.senderId, timeout);
            return;
          }
          // CHAT message
          if (message.type === 'CHAT' && message.senderId) {
            if (typingIndicatorTimeoutsRef.current.has(message.senderId)) {
              clearTimeout(typingIndicatorTimeoutsRef.current.get(message.senderId));
              typingIndicatorTimeoutsRef.current.delete(message.senderId);
              setTypingUsers(prev => {
                const next = new Set(prev);
                next.delete(message.senderId);
                return next;
              });
            }
            if (message.senderId !== user.id) await userService.getProfile(message.senderId).catch(() => null);
          }
          const messageWithId = { ...message, id: message.id || `ws-${Date.now()}` };
          const wasAtBottom = checkIfAtBottom();
          setMessages(prev => [...prev, messageWithId]);
          if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
        };
        if (conversation.type === 'DIRECT') chatService.subscribeToPrivateMessages(handleMessage);
        else chatService.subscribeToGroup(conversation.id, handleMessage);
      } catch (err) { console.error('WebSocket failed:', err); }
    };
    setupWebSocket();
    return () => {
      subscriptionSetupRef.current = false;
      if (conversation) {
        if (conversation.type === 'DIRECT') chatService.unsubscribe(`/user/queue/messages`);
        else chatService.unsubscribe(`/topic/group/${conversation.id}`);
      }
    };
  }, [conversation, user, chatId, scrollToBottom, checkIfAtBottom]);

  // --- Typing indicator send ---
  const sendTyping = useCallback((userId, recipientId, isTyping) => {
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    if (isTyping) {
      chatService.sendTyping(userId, recipientId, true);
      typingTimeoutRef.current = setTimeout(() => {
        chatService.sendTyping(userId, recipientId, false);
      }, 3000);
    } else {
      chatService.sendTyping(userId, recipientId, false);
    }
  }, []);

  const handleTyping = (e) => {
    const value = e.target.value;
    setInput(value);
    if (!conversation) return;
    
    // Get recipient ID based on conversation type
    let recipientId;
    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      recipientId = otherParticipant?.userId;
    } else {
      recipientId = conversation.id;
    }
    
    if (value.length > 0) sendTyping(user.id, recipientId, true);
    else sendTyping(user.id, recipientId, false);
  };

  // --- Send message ---
  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim() || sending || !conversation) return;
    const content = input.trim();
    setInput('');
    setSending(true);
    
    // Get recipient ID based on conversation type
    let recipientId;
    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      recipientId = otherParticipant?.userId;
    } else {
      recipientId = conversation.id;
    }
    
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    chatService.sendTyping(user.id, recipientId, false);
    const wasAtBottom = checkIfAtBottom();
    const tempMessage = {
      id: `temp-${Date.now()}`,
      senderId: user.id,
      recipientId,
      content,
      type: 'CHAT',
      createdAt: new Date().toISOString()
    };
    setMessages(prev => [...prev, tempMessage]);
    if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
    chatService.sendMessage({ senderId: user.id, recipientId, content, type: 'CHAT' });
    setSending(false);
  };

  // Load more
  const loadMore = async () => {
    if (!hasMore || loadingMore) return;
    setLoadingMore(true);
    const container = messagesContainerRef.current;
    const prevHeight = container?.scrollHeight;
    const prevTop = container?.scrollTop;
    try {
      const next = page + 1;
      const msgPage = await messageService.getMessages(chatId, next, 50);
      const older = msgPage.content.reverse();
      const uniqueIds = [...new Set(older.map(m => m.senderId))];
      Promise.all(uniqueIds.map(id => userService.getProfile(id).catch(() => null)));
      setMessages(prev => [...older, ...prev]);
      setHasMore(!msgPage.last);
      setPage(next);
      setTimeout(() => {
        if (container && prevHeight) {
          const newHeight = container.scrollHeight;
          container.scrollTop = prevTop + (newHeight - prevHeight);
        }
        updateIsAtBottom();
      }, 100);
    } catch (err) { console.error(err); } finally { setLoadingMore(false); }
  };

  // --- Profile click etc. ---
  const handleProfileClick = (userId) => {
    if (userId === user.id) navigate('/profile');
    else navigate(`/profile/${userId}`);
  };
  
  const getDisplayName = (userId) => {
    if (userId === user.id) return 'You';
    const p = participants[userId];
    return p?.displayName || userId.slice(0, 8);
  };
  
  const getAvatar = (userId) => (userId === user.id ? null : participants[userId]?.avatarUrl);
  const isUserOnline = (userId) => (userId === user.id ? true : participants[userId]?.online || false);
  
  const getMessageTime = (msg) => {
    const d = msg.createdAt || msg.timestamp;
    if (!d) return 'Just now';
    try { return new Date(d).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); } catch { return 'Just now'; }
  };

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}><CircularProgress /></Box>;
  if (error || !conversation) return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="error">{error || 'Conversation not found'}</Typography>
        <Button variant="contained" onClick={() => navigate('/')} sx={{ mt: 2 }}>Go Home</Button>
      </Paper>
    </Box>
  );

  // Get other participant info for direct chats
  const otherParticipant = conversation.type === 'DIRECT' 
    ? conversation.participants?.find(p => p.userId !== user.id) 
    : null;
  const otherId = otherParticipant?.userId;
  const otherProfile = otherId ? participants[otherId] : null;

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper square elevation={1} sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
        <IconButton onClick={() => navigate('/')}><ArrowBackIcon /></IconButton>
        <IconButton onClick={() => handleProfileClick(otherId || conversation.id)} sx={{ p: 0 }}>
          <Avatar src={conversation.type === 'DIRECT' ? otherProfile?.avatarUrl : null} sx={{ width: 48, height: 48 }}>
            {conversation.type === 'GROUP' ? <GroupIcon /> : (otherProfile?.displayName?.charAt(0)?.toUpperCase() || <PersonIcon />)}
          </Avatar>
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h6" component="span" onClick={() => handleProfileClick(otherId || conversation.id)} sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}>
            {conversation.type === 'GROUP' ? conversation.name || 'Group Chat' : otherProfile?.displayName || 'Unknown User'}
          </Typography>
          {conversation.type === 'DIRECT' && (
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="caption" color={isUserOnline(otherId) ? 'success.main' : 'text.disabled'}>
                {isUserOnline(otherId) ? '● Online' : '○ Offline'}
              </Typography>
              {otherProfile?.lastSeen && !isUserOnline(otherId) && (
                <Typography variant="caption" color="text.secondary">Last seen: {new Date(otherProfile.lastSeen).toLocaleString()}</Typography>
              )}
            </Stack>
          )}
        </Box>
        <Chip label={connected ? 'Connected' : 'Disconnected'} color={connected ? 'success' : 'error'} size="small" variant="outlined" />
      </Paper>

      {/* Messages container */}
      <Box ref={messagesContainerRef} sx={{ flex: 1, overflowY: 'auto', p: 2, bgcolor: '#f5f5f5', display: 'flex', flexDirection: 'column' }}>
        {hasMore && (
          <Box sx={{ textAlign: 'center', my: 2 }}>
            <Button onClick={loadMore} disabled={loadingMore} size="small">{loadingMore ? <CircularProgress size={20} /> : 'Load older messages'}</Button>
          </Box>
        )}
        {messages.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 4 }}><Typography color="text.secondary">No messages yet. Start the conversation!</Typography></Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
            {messages.map((msg, i) => (
              <MessageBubble
                key={msg.id || i}
                message={msg}
                isOwn={msg.senderId === user.id}
                senderName={getDisplayName(msg.senderId)}
                senderAvatar={getAvatar(msg.senderId)}
                time={getMessageTime(msg)}
                onAvatarClick={() => handleProfileClick(msg.senderId)}
              />
            ))}
          </Box>
        )}
        {typingUsers.size > 0 && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'left', mt: 1, fontStyle: 'italic', pl: 2 }}>
            {Array.from(typingUsers).map(id => getDisplayName(id)).join(', ')} {typingUsers.size === 1 ? 'is' : 'are'} typing...
          </Typography>
        )}
        <div ref={messagesEndRef} />
      </Box>

      {/* Input */}
      <Paper elevation={3} sx={{ p: 2 }}>
        <form onSubmit={handleSend}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField fullWidth size="small" placeholder="Type a message..." value={input} onChange={handleTyping} disabled={sending} multiline maxRows={4}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(e); } }} />
            <IconButton type="submit" color="primary" disabled={!input.trim() || sending} sx={{ alignSelf: 'flex-end' }}><SendIcon /></IconButton>
          </Box>
        </form>
      </Paper>
    </Box>
  );
};

export default Chat;