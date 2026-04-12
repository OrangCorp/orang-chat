import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, IconButton, Avatar, CircularProgress,
  Button, Chip, Stack, Fab, InputAdornment, Divider, List, ListItem, ListItemText,
  ListItemAvatar,
} from '@mui/material';
import {
  Send as SendIcon, ArrowBack as ArrowBackIcon, Group as GroupIcon,
  Person as PersonIcon, KeyboardArrowDown as KeyboardArrowDownIcon,
  Search as SearchIcon, Close as CloseIcon, ArrowUpward, ArrowDownward,
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

  // Core state
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

  // Search state
  const [searchMode, setSearchMode] = useState('off'); // 'off' | 'results' | 'context'
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [contextData, setContextData] = useState(null); // { messages, targetMessageId, targetIndex, hasOlderMessages, hasNewerMessages }
  const [contextLoading, setContextLoading] = useState(false);

  // Helper: check scroll position
  const checkIfAtBottom = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const { scrollTop, scrollHeight, clientHeight } = container;
    return scrollTop + clientHeight >= scrollHeight - 10;
  }, []);

  const updateIsAtBottom = useCallback(() => {
    setIsAtBottom(checkIfAtBottom());
  }, [checkIfAtBottom]);

  // Scroll listener
  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;
    const handleScroll = () => updateIsAtBottom();
    container.addEventListener('scroll', handleScroll);
    updateIsAtBottom();
    return () => container.removeEventListener('scroll', handleScroll);
  }, [updateIsAtBottom]);

  useEffect(() => {
    updateIsAtBottom();
  }, [messages, typingUsers, searchMode, searchResults, contextData, updateIsAtBottom]);

  const scrollToBottom = useCallback((force = false) => {
    if (force || checkIfAtBottom()) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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
        
        const otherParticipant = found.type === 'DIRECT' 
          ? found.participants.find(p => p.userId !== user.id) 
          : null;
        setCurrentRecipient(otherParticipant ? otherParticipant.userId : found.id);

        const messagePage = await messageService.getMessages(chatId, 0, 50);
        setMessages(messagePage.content.reverse());
        setHasMore(!messagePage.last);
        setPage(0);

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

  // --- WebSocket (unchanged) ---
  useEffect(() => {
    if (!conversation || !user || !chatId) return;
    if (subscriptionSetupRef.current) return;
    subscriptionSetupRef.current = true;

    const setupWebSocket = async () => {
      try {
        await chatService.connect();
        setConnected(true);
        const handleMessage = async (message) => {
          if (message.type === 'TYPING') {
            // ... (typing handling unchanged)
            return;
          }
          if ((message.type === 'DIRECT' || message.type === 'GROUP') && message.senderId) {
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
    
    let recipientId;
    let messageType;
    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      recipientId = otherParticipant?.userId;
      messageType = 'DIRECT';
    } else {
      recipientId = conversation.id;
      messageType = 'GROUP';
    }
    
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    chatService.sendTyping(user.id, recipientId, false);
    const wasAtBottom = checkIfAtBottom();
    const tempMessage = {
      id: `temp-${Date.now()}`,
      senderId: user.id,
      recipientId,
      content,
      type: messageType,
      createdAt: new Date().toISOString()
    };
    setMessages(prev => [...prev, tempMessage]);
    if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
    chatService.sendMessage({ senderId: user.id, recipientId, content, type: messageType });
    setSending(false);
  };

  // Load more (normal chat)
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

  // --- Search handlers ---
  const handleSearchClick = async () => {
    if (!searchQuery.trim()) return;
    setSearchLoading(true);
    try {
      const results = await messageService.searchMessages(chatId, searchQuery.trim(), 0, 50);
      setSearchResults(results.content || results); // adjust based on actual response structure
      setSearchMode('results');
    } catch (err) {
      console.error('Search failed:', err);
      setError('Search failed. Please try again.');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleSearchResultClick = async (messageId) => {
    setContextLoading(true);
    try {
      const around = await messageService.getMessagesAround(chatId, messageId, 30);
      setContextData(around);
      setSearchMode('context');
      // Ensure participant profiles for context messages
      const uniqueIds = [...new Set(around.messages.map(m => m.senderId))];
      const missingProfiles = uniqueIds.filter(id => !participants[id] && id !== user.id);
      if (missingProfiles.length) {
        const profileMap = await userService.getProfiles(missingProfiles);
        const newProfiles = {};
        profileMap.forEach((profile, userId) => { newProfiles[userId] = profile; });
        setParticipants(prev => ({ ...prev, ...newProfiles }));
      }
    } catch (err) {
      console.error('Failed to load context:', err);
      setError('Could not load conversation context.');
    } finally {
      setContextLoading(false);
    }
  };

  const loadMoreContext = async (direction) => {
    if (!contextData || contextLoading) return;
    const edgeMessage = direction === 'older' 
      ? contextData.messages[0] 
      : contextData.messages[contextData.messages.length - 1];
    if (!edgeMessage) return;
    setContextLoading(true);
    try {
      // Use the same endpoint with edge message ID to get a new window
      const around = await messageService.getMessagesAround(chatId, edgeMessage.id, 30);
      // Merge, avoiding duplicates
      const existingIds = new Set(contextData.messages.map(m => m.id));
      const newMessages = around.messages.filter(m => !existingIds.has(m.id));
      let merged;
      if (direction === 'older') {
        merged = [...newMessages, ...contextData.messages];
      } else {
        merged = [...contextData.messages, ...newMessages];
      }
      setContextData({
        ...around,
        messages: merged,
        targetMessageId: contextData.targetMessageId, // keep original target
      });
    } catch (err) {
      console.error('Failed to load more context:', err);
    } finally {
      setContextLoading(false);
    }
  };

  const exitSearchMode = () => {
    setSearchMode('off');
    setSearchQuery('');
    setSearchResults([]);
    setContextData(null);
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

  const otherParticipant = conversation.type === 'DIRECT' 
    ? conversation.participants?.find(p => p.userId !== user.id) 
    : null;
  const otherId = otherParticipant?.userId;
  const otherProfile = otherId ? participants[otherId] : null;

  // Determine what to show in the message area
  const renderMessageArea = () => {
    if (searchMode === 'results') {
      return (
        <Box sx={{ p: 2 }}>
          <Typography variant="subtitle1" gutterBottom>
            Search results for "{searchQuery}"
          </Typography>
          {searchResults.length === 0 ? (
            <Typography color="text.secondary">No messages found.</Typography>
          ) : (
            <List>
              {searchResults.map((result) => (
                <ListItem
                  key={result.id}
                  button
                  onClick={() => handleSearchResultClick(result.id)}
                  sx={{
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    '&:hover': { bgcolor: 'action.hover' }
                  }}
                >
                  <ListItemAvatar>
                    <Avatar src={getAvatar(result.senderId)}>
                      {getDisplayName(result.senderId).charAt(0)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={getDisplayName(result.senderId)}
                    secondary={
                      <span
                        dangerouslySetInnerHTML={{
                          __html: result.highlightedContent || result.content
                        }}
                      />
                    }
                    secondaryTypographyProps={{ component: 'div' }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {getMessageTime(result)}
                  </Typography>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      );
    }

    if (searchMode === 'context' && contextData) {
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Box sx={{ p: 1, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', display: 'flex', gap: 1 }}>
            <Button
              size="small"
              startIcon={<ArrowUpward />}
              disabled={!contextData.hasOlderMessages || contextLoading}
              onClick={() => loadMoreContext('older')}
            >
              Older
            </Button>
            <Button
              size="small"
              startIcon={<ArrowDownward />}
              disabled={!contextData.hasNewerMessages || contextLoading}
              onClick={() => loadMoreContext('newer')}
            >
              Newer
            </Button>
            <Box flex={1} />
            <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
              Context view
            </Typography>
          </Box>
          <Box sx={{ flex: 1, overflowY: 'auto', p: 2, bgcolor: '#f5f5f5' }}>
            {contextData.messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isOwn={msg.senderId === user.id}
                senderName={getDisplayName(msg.senderId)}
                senderAvatar={getAvatar(msg.senderId)}
                time={getMessageTime(msg)}
                onAvatarClick={() => handleProfileClick(msg.senderId)}
                highlight={msg.id === contextData.targetMessageId}
              />
            ))}
          </Box>
        </Box>
      );
    }

    // Normal chat view
    return (
      <>
        {hasMore && (
          <Box sx={{ textAlign: 'center', my: 2 }}>
            <Button onClick={loadMore} disabled={loadingMore} size="small">
              {loadingMore ? <CircularProgress size={20} /> : 'Load older messages'}
            </Button>
          </Box>
        )}
        {messages.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Typography color="text.secondary">No messages yet. Start the conversation!</Typography>
          </Box>
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
      </>
    );
  };

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
        <Paper square elevation={1} sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
          <IconButton onClick={() => navigate('/')}><ArrowBackIcon /></IconButton>
          
          {searchMode !== 'off' ? (
            // Search/Context header - when actively searching or viewing context
            <>
              <IconButton onClick={exitSearchMode}><CloseIcon /></IconButton>
              <Typography variant="h6" sx={{ flex: 1 }}>
                {searchMode === 'results' ? 'Search Results' : 'Message Context'}
              </Typography>
            </>
          ) : (
            // Normal header with search bar
            <>
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
              
              {/* Search bar - always visible in normal mode */}
              <TextField
                size="small"
                placeholder="Search messages..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearchClick()}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton edge="end" onClick={handleSearchClick} disabled={searchLoading}>
                        {searchLoading ? <CircularProgress size={20} /> : <SearchIcon />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                sx={{ width: 220 }}
              />
            </>
          )}
        </Paper>

      {/* Messages container */}
      <Box ref={messagesContainerRef} sx={{ flex: 1, overflowY: 'auto', p: 2, bgcolor: '#f5f5f5', display: 'flex', flexDirection: 'column' }}>
        {renderMessageArea()}
        <div ref={messagesEndRef} />
      </Box>

      {/* Input (hidden in search/context modes) */}
      {searchMode === 'off' && (
        <Paper elevation={3} sx={{ p: 2 }}>
          <form onSubmit={handleSend}>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField fullWidth size="small" placeholder="Type a message..." value={input} onChange={handleTyping} disabled={sending} multiline maxRows={4}
                onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(e); } }} />
              <IconButton type="submit" color="primary" disabled={!input.trim() || sending} sx={{ alignSelf: 'flex-end' }}><SendIcon /></IconButton>
            </Box>
          </form>
        </Paper>
      )}
    </Box>
  );
};

export default Chat;