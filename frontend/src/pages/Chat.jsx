import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, IconButton, Avatar, CircularProgress,
  Button, Chip, Stack, InputAdornment, List, ListItem, ListItemText,
  ListItemAvatar, Menu, MenuItem, Dialog, DialogTitle, DialogContent,
  DialogActions, Divider, Badge, Tooltip, Checkbox, ListItemButton
} from '@mui/material';
import {
  Send as SendIcon,
  ArrowBack as ArrowBackIcon,
  Group as GroupIcon,
  Person as PersonIcon,
  Search as SearchIcon,
  Close as CloseIcon,
  ArrowUpward,
  ArrowDownward,
  MoreVert as MoreVertIcon,
  People as PeopleIcon,
  Logout as LeaveIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  PersonAdd as PersonAddIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
  NotificationsOff as NotificationsOffIcon,
  KeyboardArrowUp as KeyboardArrowUpIcon,
  AttachFile as AttachmentIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import messageService from '../services/messageService';
import userService from '../services/userService';
import chatService from '../services/chatService';
import notificationService from '../services/notificationService';
import attachmentService from '../services/attachmentService';
import MessageBubble from '../components/chat/MessageBubble';
import { emitConversationUpdated } from '../utils/conversationEvents';

const getRoleLabel = (role) => {
  switch (role) {
    case 'ADMIN': return 'Admin';
    default: return 'Member';
  }
};

const getRoleColor = (role) => {
  switch (role) {
    case 'ADMIN': return 'warning';
    default: return 'default';
  }
};

const Chat = () => {
  const { chatId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const subscriptionSetupRef = useRef(false);
  const typingTimersRef = useRef(new Map());
  const typingCooldownRef = useRef(false);
  const fileInputRef = useRef(null);

  // Core state
  const [conversation, setConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const [participants, setParticipants] = useState({});
  const [typingUsers, setTypingUsers] = useState(new Set());
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

  // Group management state
  const [viewMode, setViewMode] = useState('chat');
  const [memberActionMenuAnchor, setMemberActionMenuAnchor] = useState(null);
  const [selectedMemberId, setSelectedMemberId] = useState(null);
  const [addMembersDialogOpen, setAddMembersDialogOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [newGroupName, setNewGroupName] = useState('');
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [memberSearchQuery, setMemberSearchQuery] = useState('');
  const [memberSearchResults, setMemberSearchResults] = useState([]);
  const [selectedUsersToAdd, setSelectedUsersToAdd] = useState(new Set());
  const [contacts, setContacts] = useState([]);
  const [contactsLoading, setContactsLoading] = useState(false);
  const [processingAction, setProcessingAction] = useState(false);

  const [muted, setMuted] = useState(false);
  const [muteLoading, setMuteLoading] = useState(false);

  const [selectedFiles, setSelectedFiles] = useState([]);
  const [uploadingFiles, setUploadingFiles] = useState(false);

  useEffect(() => {
    if (conversation?.id) loadMuteStatus();
  }, [conversation]);

  const loadMuteStatus = async () => {
    if (!conversation?.id) return;
    try {
      const prefs = await notificationService.getNotificationPreferences(conversation.id);
      setMuted(prefs.muted);
    } catch (err) {
      console.debug('No notification preferences found, defaulting to unmuted');
      setMuted(false);
    }
  };

  const toggleMute = async () => {
    setMuteLoading(true);
    try {
      if (muted) {
        await notificationService.unmuteConversation(conversation.id);
      } else {
        await notificationService.muteConversation(conversation.id, null);
      }
      setMuted(!muted);
    } catch (err) {
      console.error('Failed to toggle mute', err);
    } finally {
      setMuteLoading(false);
    }
  };

  const checkIfAtBottom = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    const { scrollTop, scrollHeight, clientHeight } = container;
    return scrollTop + clientHeight >= scrollHeight - 10;
  }, []);

  const updateIsAtBottom = useCallback(() => setIsAtBottom(checkIfAtBottom()), [checkIfAtBottom]);

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
  }, [messages, typingUsers, searchMode, searchResults, contextData, viewMode, updateIsAtBottom]);

  const scrollToBottom = useCallback((force = false) => {
    if (force || checkIfAtBottom()) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      setTimeout(updateIsAtBottom, 200);
    }
  }, [checkIfAtBottom, updateIsAtBottom]);

  useEffect(() => {
    typingCooldownRef.current = false;
    typingTimersRef.current.forEach(clearTimeout);
    typingTimersRef.current.clear();
    setTypingUsers(new Set());
    setViewMode('chat');
    setSelectedFiles([]);
  }, [chatId]);

  const loadConversationData = useCallback(async () => {
    if (!chatId || !user) return;
    setLoading(true);
    try {
      const convs = await messageService.getConversations();
      const found = convs.find(c => c.id === chatId);
      if (!found) throw new Error('Conversation not found');
      setConversation(found);

      const messagePage = await messageService.getMessages(chatId, 0, 50);
      console.log(messagePage);
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

      requestAnimationFrame(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
      });
    } catch (err) {
      console.error(err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [chatId, user]);

  const loadedChatIdRef = useRef(null);
  useEffect(() => {
    if (chatId !== loadedChatIdRef.current) {
      loadedChatIdRef.current = chatId;
      loadConversationData();
    }
  }, [chatId, loadConversationData]);

  useEffect(() => {
    return () => {
      typingTimersRef.current.forEach(clearTimeout);
      typingTimersRef.current.clear();
    };
  }, []);

  // =========================================================================
  // WebSocket setup – simplified: no temp messages, no GROUP self‑ignore
  // =========================================================================
  useEffect(() => {
    if (!conversation || !user || !chatId) return;
    if (subscriptionSetupRef.current) return;
    subscriptionSetupRef.current = true;

    const setupSubscriptions = async () => {
      try {
        if (!chatService.isConnected()) {
          console.log('Chat service not connected, connecting...');
          await chatService.connect();
        }

        const handleMessage = async (message) => {
          // Filter irrelevant conversations
          if (message.type === 'DIRECT') {
            const otherParticipant = conversation.participants?.find(p => p.userId !== user.id);
            const isRelevant =
              message.senderId === otherParticipant?.userId ||
              message.recipientId === otherParticipant?.userId;
            if (!isRelevant) return;
          }
          if (message.type === 'GROUP') {
            if (message.conversationId !== conversation.id) return;
          }

          // Typing indicators
          if (message.type === 'TYPING') {
            const { senderId } = message;
            if (senderId === user.id) return;
            if (conversation.type === 'DIRECT') {
              const otherParticipant = conversation.participants?.find(p => p.userId !== user.id);
              if (senderId !== otherParticipant?.userId) return;
            }
            if (typingTimersRef.current.has(senderId)) {
              clearTimeout(typingTimersRef.current.get(senderId));
              typingTimersRef.current.delete(senderId);
            }
            setTypingUsers(prev => {
              const next = new Set(prev);
              next.add(senderId);
              return next;
            });
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

          // Regular messages
          if ((message.type === 'DIRECT' || message.type === 'GROUP') && message.senderId) {
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

          // Fetch attachment details if needed
          if (message.attachmentIds?.length > 0) {
            try {
              const attachmentDetails = await Promise.all(
                message.attachmentIds.map(id =>
                  fetch(`/api/attachments/${id}`, {
                    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
                  }).then(r => r.json())
                )
              );
              message.attachments = attachmentDetails;
            } catch (err) {
              console.error('Failed to fetch attachment details:', err);
            }
          }

          const messageWithId = { ...message, id: message.messageId || `ws-${Date.now()}` };

          // No more GROUP self‑ignore – backend sends our own messages back now
          setMessages(prev => {
            const existingRealMessage = prev.findIndex(m => m.id === message.id);
            if (existingRealMessage !== -1) return prev;
            return [...prev, messageWithId];
          });

          const container = messagesContainerRef.current;
          if (container) {
            const { scrollTop, scrollHeight, clientHeight } = container;
            const isAtBottom = scrollTop + clientHeight >= scrollHeight - 10;
            if (isAtBottom) {
              setTimeout(() => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
            }
          }
        };

        if (conversation.type === 'DIRECT') {
          chatService.subscribeToPrivateMessages(handleMessage);
        } else {
          chatService.subscribeToGroup(conversation.id, handleMessage);
        }
      } catch (err) {
        console.error('WebSocket subscription failed:', err);
      }
    };

    setupSubscriptions();

    return () => {
      subscriptionSetupRef.current = false;
      if (conversation) {
        if (conversation.type === 'DIRECT') {
          chatService.unsubscribe('/user/queue/messages');
        } else {
          chatService.unsubscribe(`/topic/group/${conversation.id}`);
        }
      }
    };
  }, [conversation, user, chatId]);

  // =========================================================================
  // Send message via WebSocket (no temp messages, no self‑ignore)
  // =========================================================================
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
      typingMessage.recipientId = conversation.id;
      typingMessage.conversationId = conversation.id;
    }

    chatService.sendTyping(typingMessage);
    typingCooldownRef.current = true;
    setTimeout(() => { typingCooldownRef.current = false; }, 4000);
  }, [conversation, user]);

  const handleTyping = (e) => {
    const value = e.target.value;
    setInput(value);
    if (!conversation || value.length === 0) return;
    sendTyping();
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    setSelectedFiles(prev => [...prev, ...files]);
    e.target.value = '';
  };

  const removeSelectedFile = (index) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const handleSend = async (e) => {
    e.preventDefault();
    if ((!input.trim() && selectedFiles.length === 0) || sending || !conversation) return;
    const content = input.trim();
    setInput('');
    setSending(true);

    let uploadedIds = [];
    if (selectedFiles.length > 0) {
      setUploadingFiles(true);
      setSelectedFiles([]);
      for (const file of selectedFiles) {
        try {
          const response = await attachmentService.upload(file, conversation.id);
          uploadedIds.push(response);
        } catch (err) {
          console.error('File upload failed:', err);
          alert(`Failed to upload ${file.name}`);
        }
      }
      setUploadingFiles(false);
    }

    const messagePayload = {
      senderId: user.id,
      content: content || '',
      type: conversation.type,
      attachmentIds: uploadedIds.length > 0 ? uploadedIds.map(a => a.id) : undefined
    };

    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(p => p.userId !== user.id);
      messagePayload.recipientId = otherParticipant?.userId;
    } else {
      messagePayload.conversationId = conversation.id;
    }

    chatService.sendMessage(messagePayload);
    setSending(false);
  };

  const handleEditMessage = async (messageId, newContent) => {
    try {
      await messageService.editMessage(messageId, newContent);
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, content: newContent, edited: true } : m));
    } catch (err) {
      console.error('Edit failed:', err);
    }
  };

  const handleDeleteMessage = async (messageId) => {
    try {
      await messageService.deleteMessage(messageId);
      setMessages(prev => prev.filter(m => m.id !== messageId));
    } catch (err) {
      console.error('Delete failed:', err);
    }
  };

  const handleReaction = async (messageId, reactionType) => {
    try {
      await messageService.toggleReaction(messageId, reactionType);
      // Optionally re-fetch reactions (or update locally)
      const updatedReactions = await messageService.getReactions(messageId);
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, reactions: updatedReactions } : m));
    } catch (err) {
      console.error('Reaction failed:', err);
    }
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

  // Group management functions
  const getCurrentUserRole = () => {
    if (!conversation || !user) return 'MEMBER';
    const participant = conversation.participants?.find(p => p.userId === user.id);
    return participant?.role || 'MEMBER';
  };

  const isAdmin = () => getCurrentUserRole() === 'ADMIN';

  const handleMemberAction = async (action, targetUserId) => {
    setProcessingAction(true);
    try {
      switch (action) {
        case 'kick':
          await messageService.removeParticipant(conversation.id, targetUserId);
          break;
        case 'promote':
          await messageService.promoteParticipant(conversation.id, targetUserId);
          break;
        case 'demote':
          await messageService.demoteParticipant(conversation.id, targetUserId);
          break;
        default:
          break;
      }
      await loadConversationData();
    } catch (err) {
      console.error(`Failed to ${action} participant:`, err);
      alert(`Failed to ${action} participant. Please try again.`);
    } finally {
      setProcessingAction(false);
      setMemberActionMenuAnchor(null);
      setSelectedMemberId(null);
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm('Are you sure you want to leave this group?')) return;
    setProcessingAction(true);
    try {
      await messageService.leaveConversation(conversation.id);
      emitConversationUpdated(conversation.id);
      navigate('/');
    } catch (err) {
      console.error('Failed to leave group:', err);
      alert('Failed to leave group. Please try again.');
    } finally {
      setProcessingAction(false);
    }
  };

  const handleRenameGroup = async () => {
    if (!newGroupName.trim()) return;
    setProcessingAction(true);
    try {
      await messageService.renameConversation(conversation.id, newGroupName.trim());
      await loadConversationData();
      setRenameDialogOpen(false);
      setNewGroupName('');
    } catch (err) {
      console.error('Failed to rename group:', err);
      alert('Failed to rename group. Please try again.');
    } finally {
      setProcessingAction(false);
    }
  };

  const handleDeleteGroup = async () => {
    setProcessingAction(true);
    try {
      await messageService.deleteConversation(conversation.id);
      emitConversationUpdated(conversation.id);
      navigate('/');
    } catch (err) {
      console.error('Failed to delete group:', err);
      alert('Failed to delete group. Please try again.');
    } finally {
      setProcessingAction(false);
      setDeleteConfirmOpen(false);
    }
  };

  const handleDeleteDirectConversation = async () => {
    if (!window.confirm('Delete this conversation? This will remove it from your list.')) return;
    try {
      await messageService.deleteConversation(conversation.id);
      navigate('/');
    } catch (err) {
      console.error('Failed to delete conversation:', err);
      alert('Failed to delete conversation');
    }
  };

  const loadContactsForAdd = async () => {
    setContactsLoading(true);
    try {
      const contactsList = await userService.getContacts();
      const existingIds = new Set(conversation.participants?.map(p => p.userId) || []);
      const availableContacts = contactsList.filter(c => c.status === 'ACCEPTED' && !existingIds.has(c.userId));
      setContacts(availableContacts);
    } catch (err) {
      console.error('Failed to load contacts:', err);
    } finally {
      setContactsLoading(false);
    }
  };

  const handleOpenAddMembers = () => {
    setAddMembersDialogOpen(true);
    setSelectedUsersToAdd(new Set());
    setMemberSearchQuery('');
    setMemberSearchResults([]);
    loadContactsForAdd();
  };

  const searchUsersForAdd = async (query) => {
    if (!query.trim()) {
      setMemberSearchResults([]);
      return;
    }
    try {
      const results = await userService.searchUsers(query);
      const existingIds = new Set(conversation.participants?.map(p => p.userId) || []);
      const filtered = results.filter(u => u.userId !== user.id && !existingIds.has(u.userId));
      setMemberSearchResults(filtered);
    } catch (err) {
      console.error('Failed to search users:', err);
    }
  };

  const handleToggleUserToAdd = (userId) => {
    const newSelected = new Set(selectedUsersToAdd);
    if (newSelected.has(userId)) {
      newSelected.delete(userId);
    } else {
      newSelected.add(userId);
    }
    setSelectedUsersToAdd(newSelected);
  };

  const handleAddSelectedUsers = async () => {
    if (selectedUsersToAdd.size === 0) return;
    setProcessingAction(true);
    try {
      await messageService.addParticipants(conversation.id, Array.from(selectedUsersToAdd));
      await loadConversationData();
      setAddMembersDialogOpen(false);
    } catch (err) {
      console.error('Failed to add participants:', err);
      alert('Failed to add participants. Please try again.');
    } finally {
      setProcessingAction(false);
    }
  };

  // Render member list
  const renderMembersView = () => {
    const participantsList = conversation?.participants || [];
    
    const sortedParticipants = [...participantsList].sort((a, b) => {
      const roleOrder = { 'ADMIN': 0, 'MEMBER': 1 };
      const orderA = roleOrder[a.role] ?? 2;
      const orderB = roleOrder[b.role] ?? 2;
      if (orderA !== orderB) return orderA - orderB;
      const nameA = participants[a.userId]?.displayName || a.userId;
      const nameB = participants[b.userId]?.displayName || b.userId;
      return nameA.localeCompare(nameB);
    });

    return (
      <Box sx={{ p: 2, height: '100%', overflowY: 'auto' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">
            Members ({participantsList.length})
          </Typography>
          {isAdmin() && (
            <Button
              variant="outlined"
              startIcon={<PersonAddIcon />}
              onClick={handleOpenAddMembers}
              disabled={processingAction}
            >
              Add Members
            </Button>
          )}
        </Box>
        
        <List>
          {sortedParticipants.map((participant) => {
            const profile = participants[participant.userId];
            const displayName = profile?.displayName || participant.userId.slice(0, 8);
            const isCurrentUser = participant.userId === user.id;
            const role = participant.role;
            const canManage = isAdmin() && !isCurrentUser;
            
            return (
              <ListItem
                key={participant.userId}
                secondaryAction={
                  canManage && (
                    <IconButton
                      edge="end"
                      onClick={(e) => {
                        setSelectedMemberId(participant.userId);
                        setMemberActionMenuAnchor(e.currentTarget);
                      }}
                    >
                      <MoreVertIcon />
                    </IconButton>
                  )
                }
                sx={{
                  borderRadius: 1,
                  '&:hover': { bgcolor: 'action.hover' }
                }}
              >
                <ListItemAvatar>
                  <Avatar src={profile?.avatarUrl}>
                    {displayName.charAt(0).toUpperCase()}
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body1">
                        {displayName} {isCurrentUser && '(You)'}
                      </Typography>
                      <Chip
                        label={getRoleLabel(role)}
                        size="small"
                        color={getRoleColor(role)}
                        sx={{ height: 20, fontSize: '0.7rem' }}
                      />
                    </Box>
                  }
                  secondary={
                    profile?.online ? (
                      <Typography variant="caption" color="success.main">● Online</Typography>
                    ) : profile?.lastSeen ? (
                      <Typography variant="caption" color="text.secondary">
                        Last seen: {formatLastSeen(profile.lastSeen)}
                      </Typography>
                    ) : null
                  }
                />
              </ListItem>
            );
          })}
        </List>

        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'center' }}>
          <Button
            variant="outlined"
            color="error"
            startIcon={<LeaveIcon />}
            onClick={handleLeaveGroup}
            disabled={processingAction}
          >
            Leave Group
          </Button>
        </Box>

        {isAdmin() && (
          <Box sx={{ mt: 3 }}>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" gutterBottom>Admin Actions</Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Button
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => {
                  setNewGroupName(conversation.name || '');
                  setRenameDialogOpen(true);
                }}
                disabled={processingAction}
              >
                Rename Group
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => setDeleteConfirmOpen(true)}
                disabled={processingAction}
              >
                Delete Group
              </Button>
            </Stack>
          </Box>
        )}

        <Menu
          anchorEl={memberActionMenuAnchor}
          open={Boolean(memberActionMenuAnchor)}
          onClose={() => setMemberActionMenuAnchor(null)}
        >
          <MenuItem onClick={() => handleMemberAction('kick', selectedMemberId)}>
            <LeaveIcon sx={{ mr: 1 }} fontSize="small" />
            Remove from group
          </MenuItem>
          <MenuItem onClick={() => handleMemberAction('promote', selectedMemberId)}>
            <KeyboardArrowUpIcon sx={{ mr: 1 }} fontSize="small" />
            Promote to Admin
          </MenuItem>
          <MenuItem onClick={() => handleMemberAction('demote', selectedMemberId)}>
            <KeyboardArrowDownIcon sx={{ mr: 1 }} fontSize="small" />
            Demote to Member
          </MenuItem>
        </Menu>

        <Dialog open={addMembersDialogOpen} onClose={() => setAddMembersDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Add Members</DialogTitle>
          <DialogContent dividers>
            <TextField
              fullWidth
              size="small"
              placeholder="Search users..."
              value={memberSearchQuery}
              onChange={(e) => {
                setMemberSearchQuery(e.target.value);
                searchUsersForAdd(e.target.value);
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              sx={{ mb: 2 }}
            />
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              Selected: {selectedUsersToAdd.size}
            </Typography>
            <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
              {memberSearchQuery ? (
                memberSearchResults.length > 0 ? (
                  memberSearchResults.map((result) => (
                    <ListItem key={result.userId} disablePadding>
                      <ListItemButton onClick={() => handleToggleUserToAdd(result.userId)}>
                        <Checkbox checked={selectedUsersToAdd.has(result.userId)} size="small" />
                        <Avatar src={result.avatarUrl} sx={{ width: 32, height: 32, mr: 2 }}>
                          {result.displayName?.charAt(0).toUpperCase()}
                        </Avatar>
                        <ListItemText primary={result.displayName} />
                      </ListItemButton>
                    </ListItem>
                  ))
                ) : (
                  <Typography color="text.secondary" align="center" sx={{ py: 2 }}>
                    No users found
                  </Typography>
                )
              ) : (
                <>
                  <Typography variant="caption" color="text.secondary">
                    Your Contacts
                  </Typography>
                  {contactsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                      <CircularProgress size={24} />
                    </Box>
                  ) : contacts.length > 0 ? (
                    contacts.map((contact) => {
                      const profile = userService.profileCache.get(contact.userId);
                      return (
                        <ListItem key={contact.userId} disablePadding>
                          <ListItemButton onClick={() => handleToggleUserToAdd(contact.userId)}>
                            <Checkbox checked={selectedUsersToAdd.has(contact.userId)} size="small" />
                            <Avatar src={profile?.avatarUrl} sx={{ width: 32, height: 32, mr: 2 }}>
                              {profile?.displayName?.charAt(0).toUpperCase() || '?'}
                            </Avatar>
                            <ListItemText primary={profile?.displayName || 'Unknown User'} />
                          </ListItemButton>
                        </ListItem>
                      );
                    })
                  ) : (
                    <Typography color="text.secondary" align="center" sx={{ py: 2 }}>
                      No contacts available
                    </Typography>
                  )}
                </>
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setAddMembersDialogOpen(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleAddSelectedUsers}
              disabled={selectedUsersToAdd.size === 0 || processingAction}
            >
              {processingAction ? <CircularProgress size={24} /> : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog open={renameDialogOpen} onClose={() => setRenameDialogOpen(false)}>
          <DialogTitle>Rename Group</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              margin="dense"
              label="Group Name"
              fullWidth
              value={newGroupName}
              onChange={(e) => setNewGroupName(e.target.value)}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setRenameDialogOpen(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleRenameGroup}
              disabled={!newGroupName.trim() || processingAction}
            >
              Save
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
          <DialogTitle>Delete Group?</DialogTitle>
          <DialogContent>
            <Typography>
              Are you sure you want to permanently delete this group? This action cannot be undone.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteConfirmOpen(false)}>Cancel</Button>
            <Button
              variant="contained"
              color="error"
              onClick={handleDeleteGroup}
              disabled={processingAction}
            >
              Delete
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    );
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
    if (viewMode === 'members' && conversation.type === 'GROUP') {
      return renderMembersView();
    }

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
                key={msg.id || i}
                message={msg}
                isOwn={msg.senderId === user.id}
                senderName={getDisplayName(msg.senderId)}
                senderAvatar={getAvatar(msg.senderId)}
                time={getMessageTime(msg)}
                onAvatarClick={() => handleProfileClick(msg.senderId)}
                onEdit={handleEditMessage}
                onDelete={handleDeleteMessage}
                onReaction={handleReaction}
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
                onEdit={handleEditMessage}
                onDelete={handleDeleteMessage}
                onReaction={handleReaction}
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
                  {conversation.participants?.length || 0} members
                </Typography>
              )}
            </Box>

            {/* Delete button for DIRECT conversations only */}
            {conversation.type === 'DIRECT' && (
              <Tooltip title="Delete conversation">
                <IconButton onClick={handleDeleteDirectConversation} sx={{ color: 'error.main' }}>
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            )}

            <Tooltip title={muted ? 'Unmute notifications' : 'Mute notifications'}>
              <IconButton onClick={toggleMute} disabled={muteLoading}>
                <NotificationsOffIcon color={muted ? 'error' : 'inherit'} />
              </IconButton>
            </Tooltip>

            {conversation.type === 'GROUP' && (
              <Tooltip title={viewMode === 'chat' ? 'View Members' : 'Back to Chat'}>
                <IconButton 
                  onClick={() => setViewMode(viewMode === 'chat' ? 'members' : 'chat')}
                  color={viewMode === 'members' ? 'primary' : 'default'}
                >
                  <PeopleIcon />
                </IconButton>
              </Tooltip>
            )}
            
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
          p: viewMode === 'members' ? 0 : 2, 
          bgcolor: viewMode === 'members' ? 'background.paper' : '#f5f5f5', 
          display: 'flex', 
          flexDirection: 'column' 
        }}
      >
        {renderMessageArea()}
        {viewMode === 'chat' && <div ref={messagesEndRef} />}
      </Box>

      {searchMode === 'off' && viewMode === 'chat' && (
        <>
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

          <Paper elevation={3} sx={{ p: 2 }}>
            {selectedFiles.length > 0 && (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 1 }}>
                {selectedFiles.map((file, idx) => (
                  <Chip
                    key={idx}
                    label={file.name}
                    onDelete={() => removeSelectedFile(idx)}
                    size="small"
                    icon={<AttachmentIcon />}
                  />
                ))}
              </Box>
            )}
            <form onSubmit={handleSend}>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <IconButton
                  component="label"
                  disabled={sending || uploadingFiles}
                  color="primary"
                >
                  <AttachmentIcon />
                  <input
                    type="file"
                    hidden
                    multiple
                    onChange={handleFileSelect}
                    ref={fileInputRef}
                  />
                </IconButton>
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
                  disabled={(!input.trim() && selectedFiles.length === 0) || sending || uploadingFiles} 
                  sx={{ alignSelf: 'flex-end' }}
                >
                  {uploadingFiles ? <CircularProgress size={24} /> : <SendIcon />}
                </IconButton>
              </Box>
            </form>
          </Paper>
        </>
      )}
    </Box>
  );
};

export default Chat;