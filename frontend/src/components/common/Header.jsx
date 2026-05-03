// Header.jsx - Complete updated with proper notification handling
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar, Toolbar, Box, Button, TextField, InputAdornment, Popper, Paper,
  List, ListItem, ListItemButton, ListItemAvatar, ListItemText, Avatar,
  CircularProgress, ClickAwayListener, IconButton, Menu, MenuItem,
  Typography, Divider, Badge, Tooltip
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
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import userService from '../../services/userService';
import messageService from '../../services/messageService';
import logoImg from '../../assets/logo.png';

// Helper to format notification message based on type
const getNotificationText = (notif) => {
  const name = notif.senderName || notif.title || 'Someone';
  const body = notif.body || '';
  
  // Choose icon based on type
  let icon = <MessageIcon />;
  switch (notif.type) {
    case 'reaction': icon = <ReactionIcon />; break;
    case 'mention': icon = <MentionIcon />; break;
    case 'group_added': case 'member_added': icon = <GroupIcon />; break;
    case 'message_deleted': icon = <DeleteIcon />; break;
    case 'message_edited': icon = <EditIcon />; break;
    case 'contact_request': icon = <PersonAddIcon />; break;
  }
  
  return { title: name, body, icon };
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
  const [contactRequests, setContactRequests] = useState([]);
  const [pushNotifications, setPushNotifications] = useState([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);

  const unreadCount = contactRequests.length + pushNotifications.length;

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
  // Push notification handling
  // ------------------------------------------------------------------
  const addPushNotification = useCallback(async (data) => {
    const { type, title, body, url, conversationId } = data || {};
    
    let senderName = 'Someone';
    let avatarUrl = null;
    let displayTitle = title || 'Notification';
    let displayBody = body || '';
    
    if (conversationId) {
      try {
        // Fetch conversations to get participant info
        const conversations = await messageService.getConversations();
        const conversation = conversations.find(c => c.id === conversationId);
        
        if (conversation) {
          if (conversation.type === 'DIRECT') {
            // Find the other participant
            const otherParticipant = conversation.participants?.find(p => p.userId !== user?.id);
            if (otherParticipant) {
              // Get profile from cache or fetch it
              const profile = await userService.getProfile(otherParticipant.userId);
              senderName = profile?.displayName || 'Unknown';
              avatarUrl = profile?.avatarUrl;
              
              // Update notification text based on type
              switch (type) {
                case 'new_message':
                  displayTitle = senderName;
                  displayBody = 'sent you a message';
                  break;
                case 'reaction':
                  displayTitle = senderName;
                  displayBody = 'reacted to your message';
                  break;
                case 'mention':
                  displayTitle = senderName;
                  displayBody = 'mentioned you';
                  break;
                default:
                  displayTitle = senderName;
                  displayBody = body || 'sent a notification';
              }
            }
          } else if (conversation.type === 'GROUP') {
            // It's a group - show group name
            displayTitle = conversation.name || 'Group Chat';
            switch (type) {
              case 'new_message':
                displayBody = 'New message';
                break;
              case 'reaction':
                displayBody = 'New reaction';
                break;
              case 'mention':
                displayBody = 'You were mentioned';
                break;
              case 'group_added':
              case 'member_added':
                displayBody = 'You were added to this group';
                break;
              default:
                displayBody = body || 'New notification';
            }
          }
        }
      } catch (e) {
        console.debug('Could not fetch conversation info for notification:', e);
      }
    }
    
    // Fix URL: backend uses /conversations/ but frontend uses /chat/
    const chatUrl = url?.replace('/conversations/', '/chat/') || '/';
    
    const newNotif = {
      id: `push-${Date.now()}-${Math.random()}`,
      type: type || 'unknown',
      title: displayTitle,
      body: displayBody,
      conversationId,
      senderName,
      avatarUrl,
      url: chatUrl,
      timestamp: new Date().toISOString(),
    };
    
    setPushNotifications(prev => [newNotif, ...prev]);
  }, [user?.id]);

  // Listen for SW messages
  useEffect(() => {
    const handleSWMessage = async (event) => {
      const data = event.detail || event.data;
      if (!data) return;
      
      const { type } = data || {};
      const notifTypes = [
        'new_message', 'reaction', 'mention', 
        'group_added', 'member_added', 'new_chat',
        'message_deleted', 'message_edited'
      ];
      
      if (notifTypes.includes(type)) {
        await addPushNotification(data);
      }
    };

    // ONLY listen for custom event, NOT direct SW messages
    window.addEventListener('sw-message', handleSWMessage);
    
    // Check for missed notifications
    navigator.serviceWorker?.ready.then(reg => {
      if (reg.active) reg.active.postMessage({ type: 'CHECK_MISSED' });
    });

    return () => {
      window.removeEventListener('sw-message', handleSWMessage);
    };
  }, [addPushNotification]);

  const handleDismissPush = (id) => {
    setPushNotifications(prev => prev.filter(n => n.id !== id));
  };

  const handlePushClick = (notification) => {
    if (notification.url) navigate(notification.url);
    handleDismissPush(notification.id);
    setNotificationsAnchor(null);
  };

  // ------------------------------------------------------------------
  // Contact request handling
  // ------------------------------------------------------------------
  const loadContactRequests = useCallback(async () => {
    if (!user?.id) return;
    try {
      setNotificationsLoading(true);
      const incomingRequests = await userService.getIncomingRequests(true);

      const requesterIds = [...new Set(incomingRequests.map(r => r.requesterId))];
      let profileMap = new Map();
      if (requesterIds.length > 0) {
        try {
          profileMap = await userService.getProfiles(requesterIds);
        } catch (err) { /* ignore */ }
      }

      const formatted = incomingRequests.map(req => {
        const profile = profileMap.get(req.requesterId);
        return {
          id: req.id,
          userId: req.requesterId,
          displayName: profile?.displayName || 'Unknown User',
          avatarUrl: profile?.avatarUrl || null,
          status: req.status,
          createdAt: req.createdAt,
          type: 'contact_request'
        };
      });

      setContactRequests(formatted);
    } catch (error) {
      console.error('Failed to load contact requests:', error);
    } finally {
      setNotificationsLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    const handleRefresh = () => loadContactRequests();
    window.addEventListener('refresh-notifications', handleRefresh);
    window.addEventListener('contact-request-resolved', handleRefresh);
    return () => {
      window.removeEventListener('refresh-notifications', handleRefresh);
      window.removeEventListener('contact-request-resolved', handleRefresh);
    };
  }, [loadContactRequests]);

  useEffect(() => {
    if (user?.id) loadContactRequests();
  }, [user?.id, loadContactRequests]);

  const handleAcceptRequest = async (notification) => {
    try {
      await userService.acceptContactRequest(notification.id);
      setContactRequests(prev => prev.filter(n => n.id !== notification.id));
      window.dispatchEvent(new CustomEvent('contact-status-changed', { 
        detail: { userId: notification.userId, status: 'ACCEPTED' } 
      }));
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId: notification.id, userId: notification.userId } 
      }));
    } catch (error) {
      console.error('Failed to accept request:', error);
    }
  };

  const handleDeclineRequest = async (notification) => {
    try {
      await userService.rejectContactRequest(notification.id);
      setContactRequests(prev => prev.filter(n => n.id !== notification.id));
      window.dispatchEvent(new CustomEvent('contact-status-changed', { 
        detail: { userId: notification.userId, status: null } 
      }));
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId: notification.id, userId: notification.userId } 
      }));
    } catch (error) {
      console.error('Failed to decline request:', error);
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

  const handleNotificationsOpen = (e) => setNotificationsAnchor(e.currentTarget);
  const handleNotificationsClose = () => setNotificationsAnchor(null);

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
      navigate(`/chat/${conversation.id}`);
    } catch (error) {
      console.error('Failed to start chat:', error);
    }
  };

  const getDisplayName = () => currentUserProfile?.displayName || user?.username || user?.email?.split('@')[0] || 'User';
  const getAvatarInitial = () => getDisplayName().charAt(0).toUpperCase();

  // Combined notifications list
  const allNotifications = [
    ...contactRequests,
    ...pushNotifications,
  ].sort((a, b) => new Date(b.createdAt || b.timestamp) - new Date(a.createdAt || a.timestamp));

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
        PaperProps={{ sx: { mt: 1, width: 420, maxHeight: 500, borderRadius: 2, overflow: 'hidden' } }}
      >
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
          <Typography variant="subtitle1" fontWeight="bold">Notifications</Typography>
        </Box>
        <Divider />
        {notificationsLoading && contactRequests.length === 0 && pushNotifications.length === 0 ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress size={32} /></Box>
        ) : allNotifications.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}><Typography color="text.secondary">No notifications</Typography></Box>
        ) : (
          <List sx={{ p: 0 }}>
            {allNotifications.map((notif) => {
              const notifText = getNotificationText(notif);
              return (
                <React.Fragment key={notif.id}>
                  {notif.type === 'contact_request' ? (
                    <ListItem sx={{ py: 2, px: 2 }}>
                      <ListItemAvatar>
                        <Avatar src={notif.avatarUrl}>{notif.displayName?.charAt(0).toUpperCase()}</Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={<Typography variant="body2" fontWeight="medium"><strong>{notif.displayName}</strong> sent you a request</Typography>}
                        secondary={<Typography variant="caption" color="text.secondary">Pending approval • {notif.createdAt && new Date(notif.createdAt).toLocaleDateString()}</Typography>}
                      />
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        <Tooltip title="Accept">
                          <IconButton size="small" color="success" onClick={() => handleAcceptRequest(notif)} sx={{ bgcolor: 'success.light', '&:hover': { bgcolor: 'success.main' } }}>
                            <CheckIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Decline">
                          <IconButton size="small" color="error" onClick={() => handleDeclineRequest(notif)} sx={{ bgcolor: 'error.light', '&:hover': { bgcolor: 'error.main' } }}>
                            <CloseIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </ListItem>
                  ) : (
                    <ListItem
                      component="div"
                      onClick={() => handlePushClick(notif)}
                      sx={{ py: 2, px: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
                      secondaryAction={
                        <IconButton 
                          edge="end" 
                          size="small" 
                          onClick={(e) => { 
                            e.stopPropagation(); 
                            e.preventDefault();
                            handleDismissPush(notif.id); 
                          }}
                        >
                          <CloseIcon fontSize="small" />
                        </IconButton>
                      }
                    >
                      <ListItemAvatar>
                        <Avatar src={notif.avatarUrl}>
                          {notifText.icon}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Typography variant="body2" fontWeight="medium">
                            <strong>{notifText.title}</strong> {notifText.body}
                          </Typography>
                        }
                        secondary={
                          <Typography variant="caption" color="text.secondary">
                            {formatNotificationTime(notif.timestamp)}
                          </Typography>
                        }
                      />
                    </ListItem>
                  )}
                  <Divider />
                </React.Fragment>
              );
            })}
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