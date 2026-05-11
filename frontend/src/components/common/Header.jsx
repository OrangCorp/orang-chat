// Header.jsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar, Toolbar, Box, Button, TextField, InputAdornment, Popper, Paper,
  List, ListItem, ListItemButton, ListItemAvatar, ListItemText, Avatar,
  CircularProgress, ClickAwayListener, IconButton, Menu, MenuItem,
  Typography, Divider, Badge, Tooltip, Stack
} from '@mui/material';
import {
  Search as SearchIcon, Chat as ChatIcon, Person as PersonIcon,
  Settings as SettingsIcon, Logout as LogoutIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
  Notifications as NotificationsIcon,
  Check as CheckIcon, Close as CloseIcon,
  Message as MessageIcon,
  ThumbUp as ReactionIcon,
  PersonAdd as PersonAddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Group as GroupIcon,
  AlternateEmail as MentionIcon,
  DoneAll as MarkReadIcon,
  ClearAll as ClearAllIcon
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import userService from '../../services/userService';
import messageService from '../../services/messageService';
import notificationService from '../../services/notificationService';
import logoImg from '../../assets/logo.png';
import { emitConversationCreated } from '../../utils/conversationEvents';

// Helper to get icon based on type
const getIconForType = (type) => {
  switch (type) {
    case 'NEW_MESSAGE':
    case 'new_message': return <MessageIcon />;
    case 'REACTION':
    case 'reaction': return <ReactionIcon />;
    case 'MENTION':
    case 'mention': return <MentionIcon />;
    case 'GROUP_ADDED':
    case 'group_added':
    case 'GROUP_REMOVED':
    case 'group_removed':
    case 'ADMIN_PROMOTED':
    case 'ADMIN_DEMOTED':
    case 'GROUP_UPDATED': return <GroupIcon />;
    case 'CONTACT_REQUEST':
    case 'contact_request': return <PersonAddIcon />;
    case 'DIRECT_CONVERSATION_CREATED':
    case 'direct_chat_created': return <ChatIcon />;
    case 'MESSAGE_DELETED':
    case 'message_deleted': return <DeleteIcon />;
    case 'MESSAGE_EDITED':
    case 'message_edited': return <EditIcon />;
    default: return <MessageIcon />;
  }
};

// Helper to format time
const formatNotificationTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  
  return date.toLocaleDateString();
};

const Header = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  // Search state
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const searchAnchorRef = useRef(null);
  const searchTimeoutRef = useRef(null);

  // Profile menu
  const [profileMenuAnchor, setProfileMenuAnchor] = useState(null);
  const [currentUserProfile, setCurrentUserProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);

  // Notifications state
  const [notificationsAnchor, setNotificationsAnchor] = useState(null);
  const [inbox, setInbox] = useState([]);
  const [inboxLoading, setInboxLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  // Load current user profile
  useEffect(() => {
    if (!user?.id) return;
    const loadUserProfile = async () => {
      try {
        setProfileLoading(true);
        const profile = await userService.getProfile(user.id);
        setCurrentUserProfile(profile);
      } catch {
        setCurrentUserProfile({
          userId: user.id,
          displayName: user.username || user.email?.split('@')[0] || 'User',
          avatarUrl: null,
          online: true
        });
      } finally {
        setProfileLoading(false);
      }
    };
    loadUserProfile();
  }, [user]);

  // ------------------------------------------------------------------
  // Notification formatting (same logic as old push notifications)
  // ------------------------------------------------------------------
  const formatNotification = useCallback(async (data) => {
    const { type, title, body, url, conversationId, actorId } = data || {};
    
    const formatted = {
      ...data,
      title: title || 'Notification',
      body: body || '',
      titlePlacement: 'prefix',
      url: url?.replace('/conversations/', '/chat/') || '/',
      senderName: null,
      avatarUrl: null,
    };
    
    // Handle CONTACT_REQUEST - fetch requester profile
    if (type === 'CONTACT_REQUEST' || type === 'contact_request') {
      if (actorId) {
        try {
          const profile = await userService.getProfile(actorId);
          formatted.senderName = profile?.displayName || 'Unknown';
          formatted.avatarUrl = profile?.avatarUrl;
          formatted.title = formatted.senderName;
          formatted.body = 'sent you a contact request';
        } catch (e) {
          formatted.title = 'Someone';
          formatted.body = 'sent you a contact request';
        }
      }
      return formatted;
    }
    
    if (!conversationId) return formatted;
    
    try {
      const conversations = await messageService.getConversations();
      const conversation = conversations.find(c => c.id === conversationId);
      
      if (!conversation) return formatted;
      
      if (conversation.type === 'DIRECT') {
        const otherParticipant = conversation.participants?.find(p => p.userId !== user?.id);
        if (otherParticipant) {
          const profile = await userService.getProfile(otherParticipant.userId);
          formatted.senderName = profile?.displayName || 'Unknown';
          formatted.avatarUrl = profile?.avatarUrl;
          
          switch (type) {
            case 'new_message':
            case 'NEW_MESSAGE':
              formatted.title = formatted.senderName;
              formatted.body = 'sent you a message';
              break;
            case 'direct_chat_created':
            case 'DIRECT_CONVERSATION_CREATED':
              formatted.title = formatted.senderName;
              formatted.body = 'started a conversation with you';
              break;
            case 'reaction':
            case 'REACTION':
              formatted.title = formatted.senderName;
              formatted.body = 'reacted to your message';
              break;
            case 'mention':
            case 'MENTION':
              formatted.title = formatted.senderName;
              formatted.body = 'mentioned you';
              break;
          }
        }
      } else if (conversation.type === 'GROUP') {
        const groupName = conversation.name || 'Group Chat';
        
        switch (type) {
          case 'new_message':
          case 'NEW_MESSAGE':
            formatted.body = 'New message in';
            formatted.title = groupName;
            formatted.titlePlacement = 'suffix';
            break;
          case 'group_added':
          case 'member_added':
          case 'GROUP_ADDED':
            formatted.body = 'You were added to';
            formatted.title = groupName;
            formatted.titlePlacement = 'suffix';
            break;
          case 'reaction':
          case 'REACTION':
            formatted.title = groupName;
            formatted.body = 'New reaction';
            formatted.titlePlacement = 'suffix';
            break;
          case 'mention':
          case 'MENTION':
            formatted.title = groupName;
            formatted.body = 'You were mentioned';
            formatted.titlePlacement = 'suffix';
            break;
        }
      }
    } catch (e) {
      console.debug('Could not fetch conversation info for notification:', e);
    }
    
    return formatted;
  }, [user?.id]);

  // ------------------------------------------------------------------
  // Inbox fetching
  // ------------------------------------------------------------------
  const fetchInbox = async () => {
    setInboxLoading(true);
    try {
      const data = await notificationService.getInbox(0, 20);
      const formatted = await Promise.all(
        data.content.map(notif => formatNotification(notif))
      );
      setInbox(formatted);
    } catch (e) {
      console.error('Failed to fetch inbox', e);
    } finally {
      setInboxLoading(false);
    }
  };

  // ------------------------------------------------------------------
  // Unread count - REST + WebSocket
  // ------------------------------------------------------------------
  useEffect(() => {
    if (!user?.id) return;

    // Fetch initial unread count
    notificationService.getUnreadCount()
      .then(setUnreadCount)
      .catch(err => console.error('Failed to fetch unread count:', err));
  }, [user?.id]);

  // ------------------------------------------------------------------
  // Refresh on push notifications
  // ------------------------------------------------------------------
  useEffect(() => {
    const handleSWMessage = async (event) => {
      const data = event.detail || event.data;
      if (!data) return;
      
      const notifTypes = [
        'new_message', 'reaction', 'mention', 
        'group_added', 'member_added', 'direct_chat_created',
        'message_deleted', 'message_edited', 'contact_request'
      ];
      
      if (notifTypes.includes(data.type)) {
        try {
          const count = await notificationService.getUnreadCount();
          setUnreadCount(count);
        } catch (e) { /* ignore */ }
        
        if (notificationsAnchorRef.current) {
          fetchInbox();
        }
      }
    };
    
    // Also listen for contact request events
    const handleRefresh = () => {
      notificationService.getUnreadCount()
        .then(setUnreadCount)
        .catch(() => {});
    };
    
    window.addEventListener('sw-message', handleSWMessage);
    window.addEventListener('refresh-notifications', handleRefresh);
    window.addEventListener('contact-request-resolved', handleRefresh);
    
    return () => {
      window.removeEventListener('sw-message', handleSWMessage);
      window.removeEventListener('refresh-notifications', handleRefresh);
      window.removeEventListener('contact-request-resolved', handleRefresh);
    };
  }, [user?.id]);

  // Ref for checking if notifications panel is open (used in the event listener above)
  const notificationsAnchorRef = useRef(null);
  useEffect(() => {
    notificationsAnchorRef.current = notificationsAnchor;
  }, [notificationsAnchor]);

  // ------------------------------------------------------------------
  // Notification actions
  // ------------------------------------------------------------------
  const handleNotificationsOpen = (e) => {
    setNotificationsAnchor(e.currentTarget);
    fetchInbox();
  };

  const handleNotificationsClose = () => {
    setNotificationsAnchor(null);
  };

  const handleMarkRead = async (notif) => {
    try {
      const updated = await notificationService.markAsRead(notif.id);
      setInbox(prev => prev.map(n => n.id === updated.id ? { ...n, read: true, readAt: updated.readAt } : n));
      if (!notif.read) {
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (e) {
      console.error('Mark read failed:', e);
    }
  };

  const handleDelete = async (notif) => {
    try {
      await notificationService.deleteNotification(notif.id);
      setInbox(prev => prev.filter(n => n.id !== notif.id));
      if (!notif.read) {
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (e) {
      console.error('Delete failed:', e);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllRead();
      setInbox(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (e) {
      console.error('Mark all read failed:', e);
    }
  };

  const handleClearAll = async () => {
    try {
      await notificationService.clearAllNotifications();
      setInbox([]);
      setUnreadCount(0);
    } catch (e) {
      console.error('Clear all failed:', e);
    }
  };

  const handleNotificationClick = async (notif) => {
    if (!notif.read) {
      await handleMarkRead(notif);
    }
    handleNotificationsClose();
    
    if (notif.conversationId) {
      const url = `/chat/${notif.conversationId}` + (notif.messageId ? `?highlight=${notif.messageId}` : '');
      navigate(url);
    } else if (notif.type === 'CONTACT_REQUEST' || notif.type === 'contact_request') {
      navigate('/contacts');
    } else {
      navigate('/');
    }
  };

  // ------------------------------------------------------------------
  // Contact request handling (accept/decline)
  // ------------------------------------------------------------------
  const handleAcceptRequest = async (notif) => {
    try {
      const requests = await userService.getIncomingRequests(true);
      const request = requests.find(r => r.requesterId === notif.actorId);
      
      if (request) {
        await userService.acceptContactRequest(request.id);
        await notificationService.deleteNotification(notif.id);
        setInbox(prev => prev.filter(n => n.id !== notif.id));
        if (!notif.read) setUnreadCount(prev => Math.max(0, prev - 1));
        window.dispatchEvent(new CustomEvent('contact-request-resolved'));
      }
    } catch (e) {
      console.error('Accept failed:', e);
    }
  };

  const handleDeclineRequest = async (notif) => {
    try {
      const requests = await userService.getIncomingRequests(true);
      const request = requests.find(r => r.requesterId === notif.actorId);
      
      if (request) {
        await userService.rejectContactRequest(request.id);
        await notificationService.deleteNotification(notif.id);
        setInbox(prev => prev.filter(n => n.id !== notif.id));
        if (!notif.read) setUnreadCount(prev => Math.max(0, prev - 1));
        window.dispatchEvent(new CustomEvent('contact-request-resolved'));
      }
    } catch (e) {
      console.error('Decline failed:', e);
    }
  };

  // ------------------------------------------------------------------
  // Logout, Profile menu, Search
  // ------------------------------------------------------------------
  const handleLogout = () => {
    userService.clearCache();
    logout();
    navigate('/login');
  };

  const handleProfileMenuOpen = (e) => setProfileMenuAnchor(e.currentTarget);
  const handleProfileMenuClose = () => setProfileMenuAnchor(null);
  const handleProfileClick = () => {
    handleProfileMenuClose();
    navigate('/profile/me');
  };
  const handleSettingsClick = () => {
    handleProfileMenuClose();
    navigate('/settings');
  };

  // Search handlers
  useEffect(() => {
    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      setSearchOpen(false);
      return;
    }
    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
    searchTimeoutRef.current = setTimeout(async () => {
      try {
        setSearching(true);
        const results = await userService.searchUsers(searchQuery);
        const filtered = results.filter(r => r.userId !== user?.id);
        setSearchResults(filtered);
        setSearchOpen(filtered.length > 0);
      } catch (error) {
        console.error('Search failed:', error);
      } finally {
        setSearching(false);
      }
    }, 300);
    return () => clearTimeout(searchTimeoutRef.current);
  }, [searchQuery, user?.id]);

  const handleUserClick = (targetUser) => {
    setSearchOpen(false);
    setSearchQuery('');
    navigate(`/profile/${targetUser.userId}`);
  };

  const handleStartChat = async (e, targetUser) => {
    e.stopPropagation();
    setSearchOpen(false);
    setSearchQuery('');
    try {
      const conversation = await messageService.getOrCreateDirectChat(targetUser.userId);
      emitConversationCreated(conversation); // Add this line
      navigate(`/chat/${conversation.id}`);
    } catch (error) {
      console.error('Failed to start chat:', error);
    }
  };

  const getDisplayName = () => currentUserProfile?.displayName || user?.username || user?.email?.split('@')[0] || 'User';
  const getAvatarInitial = () => getDisplayName().charAt(0).toUpperCase();

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar>
        <Box sx={{ flexGrow: 1 }}>
          <img src={logoImg} alt="Logo" style={{ height: 40, cursor: 'pointer' }} onClick={() => navigate('/')} />
        </Box>

        {/* Search */}
        <Box ref={searchAnchorRef} sx={{ position: 'relative', width: 350, mx: 2 }}>
          <TextField
            size="small"
            placeholder="Search users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{ width: '100%', backgroundColor: 'rgba(255,255,255,0.15)', borderRadius: 1, '& .MuiInputBase-root': { color: 'white' }, '& .MuiInputBase-input::placeholder': { color: 'rgba(255,255,255,0.7)' }, '& .MuiSvgIcon-root': { color: 'white' } }}
            InputProps={{
              startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment>,
              endAdornment: searching && <InputAdornment position="end"><CircularProgress size={20} color="inherit" /></InputAdornment>
            }}
          />
          {searchOpen && (
            <ClickAwayListener onClickAway={() => setSearchOpen(false)}>
              <Popper open={searchOpen} anchorEl={searchAnchorRef.current} placement="bottom-start" sx={{ width: searchAnchorRef.current?.clientWidth, zIndex: 1300 }}>
                <Paper elevation={4} sx={{ mt: 1, maxHeight: 400, overflow: 'auto' }}>
                  <List dense>
                    {searchResults.map((result) => (
                      <ListItem key={result.userId} disablePadding secondaryAction={
                        <IconButton edge="end" color="primary" onClick={(e) => handleStartChat(e, result)}><ChatIcon /></IconButton>
                      }>
                        <ListItemButton onClick={() => handleUserClick(result)}>
                          <ListItemAvatar><Avatar src={result.avatarUrl}>{result.displayName?.charAt(0).toUpperCase()}</Avatar></ListItemAvatar>
                          <ListItemText
                            primary={result.displayName}
                            secondary={result.online ? <span style={{ color: '#4caf50' }}>● Online</span> : <span>Last seen {result.lastSeen ? new Date(result.lastSeen).toLocaleDateString() : 'recently'}</span>}
                          />
                        </ListItemButton>
                      </ListItem>
                    ))}
                  </List>
                </Paper>
              </Popper>
            </ClickAwayListener>
          )}
        </Box>

        {/* User actions */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Tooltip title="Notifications">
            <IconButton color="inherit" onClick={handleNotificationsOpen} sx={{ '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}>
              <Badge badgeContent={unreadCount} color="error"><NotificationsIcon /></Badge>
            </IconButton>
          </Tooltip>

          <Tooltip title="Profile & Settings">
            <Button onClick={handleProfileMenuOpen} sx={{ display: 'flex', alignItems: 'center', gap: 1, textTransform: 'none', color: 'white', borderRadius: 20, px: 2, py: 0.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}>
              <Avatar src={currentUserProfile?.avatarUrl} sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
                {!profileLoading && getAvatarInitial()}
                {profileLoading && <CircularProgress size={24} sx={{ color: 'white' }} />}
              </Avatar>
              <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left' }}>
                <Typography variant="body2" sx={{ fontWeight: 'bold', lineHeight: 1.2 }}>{getDisplayName()}</Typography>
              </Box>
              <KeyboardArrowDownIcon sx={{ fontSize: 20 }} />
            </Button>
          </Tooltip>
        </Box>
      </Toolbar>

      {/* Notifications dropdown */}
      <Menu
        anchorEl={notificationsAnchor}
        open={Boolean(notificationsAnchor)}
        onClose={handleNotificationsClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { mt: 1, width: 450, maxHeight: 600, borderRadius: 2, overflow: 'hidden' } }}
      >
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1" fontWeight="bold">Notifications</Typography>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Mark all read">
              <IconButton size="small" color="inherit" onClick={handleMarkAllRead}>
                <MarkReadIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Clear all">
              <IconButton size="small" color="inherit" onClick={handleClearAll}>
                <ClearAllIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        </Box>
        <Divider />
        
        {inboxLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress size={32} />
          </Box>
        ) : inbox.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography color="text.secondary">No notifications</Typography>
          </Box>
        ) : (
          <List sx={{ p: 0 }}>
            {inbox.map((notif) => (
              <React.Fragment key={notif.id}>
                <ListItem
                  sx={{ 
                    py: 2, px: 2, cursor: 'pointer', 
                    bgcolor: notif.read ? 'transparent' : 'action.hover',
                    '&:hover': { bgcolor: 'action.selected' }
                  }}
                  onClick={() => handleNotificationClick(notif)}
                  secondaryAction={
                    <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
                      {(notif.type === 'CONTACT_REQUEST' || notif.type === 'contact_request') && (
                        <>
                          <Tooltip title="Accept">
                            <IconButton 
                              size="small" 
                              color="success"
                              onClick={(e) => { e.stopPropagation(); handleAcceptRequest(notif); }}
                            >
                              <CheckIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Decline">
                            <IconButton 
                              size="small" 
                              color="error"
                              onClick={(e) => { e.stopPropagation(); handleDeclineRequest(notif); }}
                            >
                              <CloseIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </>
                      )}
                      {!notif.read && (
                        <Tooltip title="Mark read">
                          <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleMarkRead(notif); }}>
                            <CheckIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      <Tooltip title="Delete">
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDelete(notif); }}>
                          <CloseIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  }
                >
                  <ListItemAvatar>
                    <Avatar src={notif.avatarUrl} sx={{ bgcolor: notif.avatarUrl ? 'transparent' : (notif.read ? 'grey.400' : 'primary.main') }}>
                      {!notif.avatarUrl && getIconForType(notif.type)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Typography variant="body2" fontWeight={notif.read ? 'normal' : 'bold'}>
                        {notif.titlePlacement === 'suffix' ? (
                          <>{notif.body} <strong>{notif.title}</strong></>
                        ) : (
                          <><strong>{notif.title}</strong> {notif.body}</>
                        )}
                        {notif.groupCount > 1 && (
                          <Badge 
                            badgeContent={notif.groupCount} 
                            color="primary" 
                            sx={{ ml: 1.5, '& .MuiBadge-badge': { fontSize: '0.65rem', position: 'static', transform: 'none' } }} 
                          />
                        )}
                      </Typography>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary">
                        {formatNotificationTime(notif.createdAt || notif.timestamp)}
                      </Typography>
                    }
                  />
                </ListItem>
                <Divider />
              </React.Fragment>
            ))}
          </List>
        )}
      </Menu>

      {/* Profile menu */}
      <Menu anchorEl={profileMenuAnchor} open={Boolean(profileMenuAnchor)} onClose={handleProfileMenuClose} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }} PaperProps={{ sx: { mt: 1, width: 280, borderRadius: 2, overflow: 'hidden' } }}>
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white', display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar src={currentUserProfile?.avatarUrl} sx={{ width: 48, height: 48, bgcolor: 'secondary.main', border: '2px solid white' }}>{getAvatarInitial()}</Avatar>
          <Box>
            <Typography variant="subtitle1" fontWeight="bold">{getDisplayName()}</Typography>
          </Box>
        </Box>
        <Divider />
        <MenuItem onClick={handleProfileClick}><PersonIcon sx={{ mr: 2, fontSize: 20 }} /><Typography variant="body2">My Profile</Typography></MenuItem>
        <MenuItem onClick={handleSettingsClick}><SettingsIcon sx={{ mr: 2, fontSize: 20 }} /><Typography variant="body2">Settings</Typography></MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}><LogoutIcon sx={{ mr: 2, fontSize: 20 }} /><Typography variant="body2">Logout</Typography></MenuItem>
      </Menu>
    </AppBar>
  );
};

export default Header;