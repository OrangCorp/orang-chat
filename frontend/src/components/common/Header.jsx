// Header.jsx (updated with browser notifications for contact requests)
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
  Check as CheckIcon, Close as CloseIcon
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import userService from '../../services/userService';
import messageService from '../../services/messageService';
import logoImg from '../../assets/logo.png';

const Header = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  // ... search state (unchanged)
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const searchAnchorRef = useRef(null);
  const searchTimeoutRef = useRef(null);

  // Profile menu (unchanged)
  const [profileMenuAnchor, setProfileMenuAnchor] = useState(null);
  const [currentUserProfile, setCurrentUserProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);

  // Notifications state
  const [notificationsAnchor, setNotificationsAnchor] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  
  // Ref to keep track of seen request IDs across polling
  const seenRequestIds = useRef(new Set());

  useEffect(() => {
    const handleRequestResolved = (event) => {
      const { contactId, userId } = event.detail;
      
      // Remove the notification from state
      setNotifications(prev => {
        const filtered = prev.filter(n => n.id !== contactId && n.userId !== userId);
        
        // Update unread count
        const newUnreadCount = filtered.length;
        setUnreadCount(newUnreadCount);
        
        // Remove from seen set
        seenRequestIds.current.delete(contactId);
        
        return filtered;
      });
      
      console.log('Contact request resolved from profile:', contactId);
    };
    
    window.addEventListener('contact-request-resolved', handleRequestResolved);
    
    return () => {
      window.removeEventListener('contact-request-resolved', handleRequestResolved);
    };
  }, []);


  // Load current user profile (unchanged)
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

  // Request notification permission on mount
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  // Show browser notification for new contact request
  const showContactRequestNotification = (request) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      const displayName = request.displayName || 'Someone';
      const title = `New contact request`;
      const options = {
        body: `${displayName} wants to connect with you`,
        icon: request.avatarUrl || '/logo192.png',
        badge: '/badge.png',
        tag: `contact-request-${request.id}`,
        requireInteraction: true,
        data: { url: window.location.origin }
      };
      
      const notification = new Notification(title, options);
      notification.onclick = () => {
        window.focus();
      };
    }
  };

  // Load notifications (incoming contact requests)
  const loadNotifications = useCallback(async () => {
    if (!user?.id) return;
    console.log('loading notifications...');
    try {
      setNotificationsLoading(true);
      const incomingRequests = await userService.getIncomingRequests(true);

      const formatted = incomingRequests.map(req => ({
        id: req.id,
        userId: req.requesterId,
        displayName: req.requesterDisplayName,
        avatarUrl: req.requesterAvatarUrl,
        status: req.status,
        createdAt: req.createdAt,
        read: false,
        direction: 'incoming'
      }));

      setNotifications(formatted);
      setUnreadCount(formatted.filter(n => !n.read).length);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setNotificationsLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    const handleRequestResolved = () => {
      // Refresh notifications immediately
      loadNotifications();
    };
    
    window.addEventListener('contact-request-resolved', handleRequestResolved);
    
    return () => {
      window.removeEventListener('contact-request-resolved', handleRequestResolved);
    };
  }, [loadNotifications]);

  // Initial load and periodic refresh (every 30s)
  useEffect(() => {
    if (!user?.id) return;
    
    loadNotifications();
    console.log('scheduling load notifications');
    
    const interval = setInterval(loadNotifications, 30000);
    
    return () => clearInterval(interval);
  }, [loadNotifications]);

  // Logout (unchanged)
  const handleLogout = () => {
    userService.clearCache();
    logout();
    navigate('/login');
  };

  // Profile menu handlers (unchanged)
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

  // Notifications menu
  const handleNotificationsOpen = (e) => {
    setNotificationsAnchor(e.currentTarget);
    // Mark all as "read" visually (we don't persist read status for contact requests)
    setUnreadCount(0);
  };
  const handleNotificationsClose = () => setNotificationsAnchor(null);

  const handleAcceptRequest = async (notification) => {
    try {
      await userService.acceptContactRequest(notification.id);
      setNotifications(prev => prev.filter(n => n.id !== notification.id));
      seenRequestIds.current.delete(notification.id);
      setUnreadCount(prev => Math.max(0, prev - 1));
      
      // Notify that contact status changed (for profile page)
      window.dispatchEvent(new CustomEvent('contact-status-changed', { 
        detail: { userId: notification.userId, status: 'ACCEPTED' } 
      }));
      
      // Also dispatch resolved event for consistency
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
      setNotifications(prev => prev.filter(n => n.id !== notification.id));
      seenRequestIds.current.delete(notification.id);
      setUnreadCount(prev => Math.max(0, prev - 1));
      
      // Notify that contact status changed (for profile page)
      window.dispatchEvent(new CustomEvent('contact-status-changed', { 
        detail: { userId: notification.userId, status: null } 
      }));
      
      // Also dispatch resolved event for consistency
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId: notification.id, userId: notification.userId } 
      }));
    } catch (error) {
      console.error('Failed to decline request:', error);
    }
  };

  // Search handlers (unchanged)
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
      <Menu anchorEl={notificationsAnchor} open={Boolean(notificationsAnchor)} onClose={handleNotificationsClose} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }} PaperProps={{ sx: { mt: 1, width: 380, maxHeight: 500, borderRadius: 2, overflow: 'hidden' } }}>
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
          <Typography variant="subtitle1" fontWeight="bold">Notifications</Typography>
        </Box>
        <Divider />
        {notificationsLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress size={32} /></Box>
        ) : notifications.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}><Typography color="text.secondary">No notifications</Typography></Box>
        ) : (
          <List sx={{ p: 0 }}>
            {notifications.map((notification) => (
              <React.Fragment key={notification.id}>
                <ListItem sx={{ py: 2, px: 2 }}>
                  <ListItemAvatar>
                    <Avatar src={notification.avatarUrl}>{notification.displayName?.charAt(0).toUpperCase()}</Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={<Typography variant="body2" fontWeight="medium"><strong>{notification.displayName}</strong> sent you a request</Typography>}
                    secondary={<Typography variant="caption" color="text.secondary">Pending approval • {notification.createdAt && new Date(notification.createdAt).toLocaleDateString()}</Typography>}
                  />
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Tooltip title="Accept">
                      <IconButton size="small" color="success" onClick={() => handleAcceptRequest(notification)} sx={{ bgcolor: 'success.light', '&:hover': { bgcolor: 'success.main' } }}>
                        <CheckIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Decline">
                      <IconButton size="small" color="error" onClick={() => handleDeclineRequest(notification)} sx={{ bgcolor: 'error.light', '&:hover': { bgcolor: 'error.main' } }}>
                        <CloseIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
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