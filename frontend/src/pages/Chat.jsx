import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, IconButton, Avatar, CircularProgress,
  Button, Chip, Stack, InputAdornment, List, ListItem, ListItemText,
  ListItemAvatar,
} from '@mui/material';
import {
  Send as SendIcon, ArrowBack as ArrowBackIcon, Group as GroupIcon,
  Person as PersonIcon, Search as SearchIcon, Close as CloseIcon,
  ArrowUpward, ArrowDownward,
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
  const subscriptionSetupRef = useRef(false);
  const typingTimersRef = useRef(new Map());
  const typingCooldownRef = useRef(false);

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
  const [isAtBottom, setIsAtBottom] = useState(true);

  // Search state
  const [searchMode, setSearchMode] = useState('off');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [contextData, setContextData] = useState(null);
  const [contextLoading, setContextLoading] = useState(false);

  const checkIfAtBottom = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const { scrollTop, scrollHeight, clientHeight } = container;
    return scrollTop + clientHeight >= scrollHeight - 10;
  }, []);

  const updateIsAtBottom = useCallback(() => {
    setIsAtBottom(checkIfAtBottom());
  }, [checkIfAtBottom]);

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

  // Reset typing state when chat changes
  useEffect(() => {
    typingCooldownRef.current = false;
    typingTimersRef.current.forEach(clearTimeout);
    typingTimersRef.current.clear();
    setTypingUsers(new Set());
  }, [chatId]);

  // Load conversation & messages
  useEffect(() => {
    if (!chatId || !user) return;
    const loadData = async () => {
      setLoading(true);
      try {
        const convs = await messageService.getConversations();
        const found = convs.find(c => c.id === chatId);
        if (!found) throw new Error('Conversation not found');
        setConversation(found);

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

  useEffect(() => {
    return () => {
      typingTimersRef.current.forEach(clearTimeout);
      typingTimersRef.current.clear();
    };
  }, []);
  
  // WebSocket setup
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
            const { senderId } = message;

            // Clear existing timer for this user
            if (typingTimersRef.current.has(senderId)) {
              clearTimeout(typingTimersRef.current.get(senderId));
              typingTimersRef.current.delete(senderId);
            }

            // Add user to typing set
            setTypingUsers(prev => {
              const next = new Set(prev);
              next.add(senderId);
              return next;
            });

            // Auto-remove after 5 seconds
            const timer = setTimeout(() => {
              setTypingUsers(prev => {
                const next = new Set(prev);
                next.delete(senderId);
                return next;
              });
              typingTimersRef.current.delete(senderId);
            }, 5000);

            typingTimersRef.current.set(senderId, timer);
            return;
          }

          if ((message.type === 'DIRECT' || message.type === 'GROUP') && message.senderId) {
            // Remove typing indicator when a message is sent
            if (typingTimersRef.current.has(message.senderId)) {
              clearTimeout(typingTimersRef.current.get(message.senderId));
              typingTimersRef.current.delete(message.senderId);
              setTypingUsers(prev => {
                const next = new Set(prev);
                next.delete(message.senderId);
                return next;
              });
            }
            if (message.senderId !== user.id) {
              await userService.getProfile(message.senderId).catch(() => null);
            }
          }
          
          const messageWithId = { ...message, id: message.id || `ws-${Date.now()}` };
          const wasAtBottom = checkIfAtBottom();
          
          // Check if this message is from current user and we already have a temp version
          setMessages(prev => {
            // If it's my message and it's a group message, check for duplicates
            if (message.senderId === user.id && message.type === 'GROUP') {
              // Look for a temp message with matching content sent in the last few seconds
              const duplicateIndex = prev.findIndex(m => 
                m.id?.startsWith('temp-') && 
                m.content === message.content &&
                m.senderId === user.id &&
                Date.now() - new Date(m.createdAt).getTime() < 5000
              );
              
              if (duplicateIndex !== -1) {
                // Replace temp message with real one
                const newMessages = [...prev];
                newMessages[duplicateIndex] = messageWithId;
                return newMessages;
              }
            }
            
            // Check for duplicate real messages (in case temp was already replaced)
            const existingRealMessage = prev.findIndex(m => 
              m.id === message.id || 
              (m.id && !m.id.startsWith('temp-') && m.content === message.content && m.senderId === message.senderId && Math.abs(new Date(m.createdAt) - new Date(message.createdAt)) < 1000)
            );
            
            if (existingRealMessage !== -1) {
              // Message already exists, don't add duplicate
              return prev;
            }
            
            // Otherwise just add the message
            return [...prev, messageWithId];
          });
          
          if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
        };
        
        // Subscribe based on conversation type
        if (conversation.type === 'DIRECT') {
          chatService.subscribeToPrivateMessages(handleMessage);
        } else if (conversation.type === 'GROUP') {
          chatService.subscribeToGroup(conversation.id, handleMessage);
        }
      } catch (err) { 
        console.error('WebSocket failed:', err); 
      }
    };
    setupWebSocket();
    
    return () => {
      subscriptionSetupRef.current = false;
      if (conversation) {
        if (conversation.type === 'DIRECT') {
          chatService.unsubscribe('/user/queue/messages');
        } else if (conversation.type === 'GROUP') {
          chatService.unsubscribe(`/topic/group/${conversation.id}`);
        }
      }
    };
  }, [conversation, user, chatId, scrollToBottom, checkIfAtBottom]);

  // Send typing event
  const sendTyping = useCallback(() => {
    if (!chatService.isConnected()) return;
    if (typingCooldownRef.current) return;

    const typingMessage = {
      senderId: user.id,
      type: 'TYPING',
      timestamp: new Date().toISOString()
    };

    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      typingMessage.recipientId = otherParticipant?.userId;
    } else {
      typingMessage.conversationId = conversation.id;
    }

    chatService.sendTyping(typingMessage);
    typingCooldownRef.current = true;

    // Reset cooldown after 4 seconds
    setTimeout(() => {
      typingCooldownRef.current = false;
    }, 4000);
  }, [conversation, user]);

  const handleTyping = (e) => {
    const value = e.target.value;
    setInput(value);
    if (!conversation || value.length === 0) return;
    sendTyping();
  };

  // Send message
  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim() || sending || !conversation) return;
    const content = input.trim();
    setInput('');
    setSending(true);
    
    const messagePayload = {
      senderId: user.id,
      content,
      type: conversation.type
    };

    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      messagePayload.recipientId = otherParticipant?.userId;
    } else {
      messagePayload.conversationId = conversation.id;
    }
    
    const wasAtBottom = checkIfAtBottom();
    const tempMessage = {
      id: `temp-${Date.now()}`,
      senderId: user.id,
      content,
      type: conversation.type,
      ...(conversation.type === 'DIRECT' 
        ? { recipientId: messagePayload.recipientId }
        : { conversationId: messagePayload.conversationId }
      ),
      createdAt: new Date().toISOString()
    };
    
    setMessages(prev => [...prev, tempMessage]);
    if (wasAtBottom) setTimeout(() => scrollToBottom(true), 50);
    
    chatService.sendMessage(messagePayload);
    setSending(false);
  };

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

  // Search handlers
  const handleSearchClick = async () => {
    if (!searchQuery.trim()) return;
    setSearchLoading(true);
    try {
      const results = await messageService.searchMessages(chatId, searchQuery.trim(), 0, 50);
      setSearchResults(results.content || results);
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
      const around = await messageService.getMessagesAround(chatId, edgeMessage.id, 30);
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
        targetMessageId: contextData.targetMessageId,
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
  
  const formatLastSeen = (lastSeen) => {
    if (!lastSeen) return 'Never';
    
    const lastSeenDate = new Date(lastSeen);
    const now = new Date();
    const diffMs = now - lastSeenDate;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    
    return lastSeenDate.toLocaleDateString();
  };
  
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
      </>
    );
  };

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', flexDirection: 'column' }}>
      <Paper square elevation={1} sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
        <IconButton onClick={() => navigate('/')}><ArrowBackIcon /></IconButton>
        
        {searchMode !== 'off' ? (
          <>
            <IconButton onClick={exitSearchMode}><CloseIcon /></IconButton>
            <Typography variant="h6" sx={{ flex: 1 }}>
              {searchMode === 'results' ? 'Search Results' : 'Message Context'}
            </Typography>
          </>
        ) : (
          <>
            <IconButton 
              onClick={() => handleProfileClick(
                conversation.type === 'DIRECT' ? otherId : conversation.id
              )} 
              sx={{ p: 0 }}
            >
              <Avatar 
                src={conversation.type === 'DIRECT' ? otherProfile?.avatarUrl : null} 
                sx={{ width: 48, height: 48 }}
              >
                {conversation.type === 'GROUP' 
                  ? <GroupIcon /> 
                  : (otherProfile?.displayName?.charAt(0)?.toUpperCase() || <PersonIcon />)
                }
              </Avatar>
            </IconButton>
            
            <Box sx={{ flex: 1 }}>
              <Typography 
                variant="h6" 
                component="span" 
                onClick={() => handleProfileClick(
                  conversation.type === 'DIRECT' ? otherId : conversation.id
                )} 
                sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
              >
                {conversation.type === 'GROUP' 
                  ? conversation.name || 'Group Chat' 
                  : otherProfile?.displayName || 'Unknown User'
                }
              </Typography>
              
              {conversation.type === 'DIRECT' ? (
                <Stack direction="row" spacing={1} alignItems="center">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Box 
                      sx={{ 
                        width: 8, 
                        height: 8, 
                        borderRadius: '50%', 
                        bgcolor: otherProfile?.online ? 'success.main' : 'text.disabled',
                        animation: otherProfile?.online ? 'pulse 1.5s infinite' : 'none',
                        '@keyframes pulse': {
                          '0%': { opacity: 1 },
                          '50%': { opacity: 0.5 },
                          '100%': { opacity: 1 }
                        }
                      }} 
                    />
                    <Typography variant="caption" color={otherProfile?.online ? 'success.main' : 'text.disabled'}>
                      {otherProfile?.online ? 'Online' : 'Offline'}
                    </Typography>
                  </Box>
                  {!otherProfile?.online && otherProfile?.lastSeen && (
                    <Typography variant="caption" color="text.secondary">
                      Last seen: {formatLastSeen(otherProfile.lastSeen)}
                    </Typography>
                  )}
                </Stack>
              ) : (
                <Typography variant="caption" color="text.secondary">
                  &nbsp;&nbsp;{conversation.participants?.length || 0} members
                </Typography>
              )}
            </Box>
            
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
        {renderMessageArea()}
        <div ref={messagesEndRef} />
      </Box>

      {searchMode === 'off' && (
        <Box 
          sx={{ 
            minHeight: '40px',
            px: 2, 
            py: typingUsers.size > 0 ? 1 : 0,
            bgcolor: 'background.paper',
            borderTop: 1,
            borderColor: 'divider',
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            transition: 'padding 0.2s ease'
          }}
        >
          {typingUsers.size > 0 && (
            <>
              <Box sx={{ display: 'flex', gap: 0.5 }}>
                {Array.from(typingUsers).slice(0, 3).map(userId => (
                  <Avatar 
                    key={userId}
                    src={getAvatar(userId)} 
                    sx={{ width: 20, height: 20 }}
                  >
                    {getDisplayName(userId).charAt(0)}
                  </Avatar>
                ))}
              </Box>
              <Typography variant="body2" color="text.secondary" fontStyle="italic">
                {Array.from(typingUsers).map(id => getDisplayName(id)).join(', ')} 
                {typingUsers.size === 1 ? ' is' : ' are'} typing
              </Typography>
            </>
          )}
        </Box>
      )}

      {searchMode === 'off' && (
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
      )}
    </Box>
  );
};

export default Chat;