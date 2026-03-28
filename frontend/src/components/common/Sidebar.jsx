import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Drawer,
  Toolbar,
  Box,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Divider,
  CircularProgress,
  IconButton,
  Tooltip,
  Badge,
  useTheme,
  useMediaQuery,
  Stack,
  Button
} from '@mui/material';
import { 
  Add as AddIcon, 
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
  Menu as MenuIcon,
  Chat as ChatIcon,
  Group as GroupIcon,
  Person as PersonIcon
} from '@mui/icons-material';
import { conversationService } from '../../services/messageService';
import userService from '../../services/userService'; // Import the singleton
import { useAuth } from '../../context/AuthContext';

const drawerWidth = 280;
const miniDrawerWidth = 72;

const Sidebar = () => {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(true); // State for drawer open/close
  const { user } = useAuth();
  const navigate = useNavigate();
  const { chatId } = useParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  // Auto-close drawer on mobile
  useEffect(() => {
    if (isMobile) {
      setOpen(false);
    } else {
      // Restore from localStorage on desktop
      const savedState = localStorage.getItem('sidebar-open');
      if (savedState !== null) {
        setOpen(savedState === 'true');
      } else {
        setOpen(true); // Default open on desktop
      }
    }
  }, [isMobile]);

  // Save drawer state to localStorage
  useEffect(() => {
    if (!isMobile) {
      localStorage.setItem('sidebar-open', open);
    }
  }, [open, isMobile]);

  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async () => {
    try {
      setLoading(true);
      const data = await conversationService.getConversations();
      setConversations(data);
      setError(null);
    } catch (err) {
      setError('Failed to load conversations');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleChatClick = (conversationId) => {
    navigate(`/chat/${conversationId}`);
    if (isMobile) {
      setOpen(false); // Close drawer on mobile after navigation
    }
  };


  const toggleDrawer = () => {
    setOpen(!open);
  };

  const getConversationDisplay = (conv) => {
    if (conv.type === 'DIRECT') {
      const otherId = conv.participantIds.find(id => id !== user?.id);
      if (!otherId) return { name: 'Unknown', avatar: null, id: null, online: false };
      
      // Profile is automatically cached by user service
      const profile = userService.profileCache.get(otherId);
      const displayName = profile?.displayName || otherId.slice(0, 8);
      const avatarUrl = profile?.avatarUrl;
      const online = profile?.online || false;
      
      return { name: displayName, avatar: avatarUrl, id: otherId, online, type: 'DIRECT' };
    } else {
      // Group chat
      return { name: conv.name || 'Unnamed Group', avatar: null, id: conv.id, online: false, type: 'GROUP' };
    }
  };

  const getAvatarLetter = (name) => {
    return name ? name.charAt(0).toUpperCase() : '?';
  };

  const getLastMessageTime = (conv) => {
    if (!conv.lastMessageTimestamp) return '';
    try {
      const date = new Date(conv.lastMessageTimestamp);
      const now = new Date();
      const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
      
      if (diffDays === 0) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      } else if (diffDays === 1) {
        return 'Yesterday';
      } else if (diffDays < 7) {
        return date.toLocaleDateString([], { weekday: 'short' });
      } else {
        return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
      }
    } catch {
      return '';
    }
  };

  // Pre-cache profiles for all conversations
  useEffect(() => {
    const preCacheProfiles = async () => {
      const userIds = conversations
        .filter(conv => conv.type === 'DIRECT')
        .map(conv => conv.participantIds.find(id => id !== user?.id))
        .filter(id => id && !userService.profileCache.has(id));
      
      if (userIds.length === 0) return;
      
      // Batch load profiles (they'll be cached automatically)
      await userService.getProfiles(userIds);
    };

    if (conversations.length > 0 && user) {
      preCacheProfiles();
    }
  }, [conversations, user]);

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header with toggle button */}
      <Box sx={{ 
        p: 2, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        minHeight: 64
      }}>
        {open ? (
          <>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Chats
            </Typography>
            <Stack direction="row" spacing={1}>
              <Tooltip title="Collapse">
                <IconButton size="small" onClick={toggleDrawer}>
                  <ChevronLeftIcon />
                </IconButton>
              </Tooltip>
            </Stack>
          </>
        ) : (
          <Box sx={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
            <Tooltip title="Expand">
              <IconButton onClick={toggleDrawer}>
                <ChevronRightIcon />
              </IconButton>
            </Tooltip>
          </Box>
        )}
      </Box>

      <Divider />

      {/* Loading State */}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress size={24} />
        </Box>
      )}

      {/* Error State */}
      {error && (
        <Box sx={{ p: 2 }}>
          <Typography color="error" variant="body2" align="center">
            {error}
          </Typography>
          <Button 
            size="small" 
            onClick={loadConversations} 
            sx={{ mt: 1, display: 'block', mx: 'auto' }}
          >
            Retry
          </Button>
        </Box>
      )}

      {/* Empty State */}
      {!loading && !error && conversations.length === 0 && (
        <Box sx={{ p: 2, textAlign: 'center', mt: 4 }}>
          <ChatIcon sx={{ fontSize: 32, color: 'text.disabled', mb: 2 }} />
          <Typography variant="body1" color="text.secondary" gutterBottom>
            No chats yet
          </Typography>
        </Box>
      )}

      {/* Conversations List */}
      {!loading && conversations.length > 0 && (
        <List sx={{ flex: 1, overflow: 'auto', pt: 0 }}>
          {conversations.map((conv) => {
            const { name, avatar, id: otherId, online, type } = getConversationDisplay(conv);
            const isSelected = conv.id === chatId;
            const lastMessageTime = getLastMessageTime(conv);
            const unreadCount = conv.unreadCount || 0;

            if (!open) {
              // Mini view - only show icons
              return (
                <ListItem key={conv.id} disablePadding sx={{ justifyContent: 'center' }}>
                  <Tooltip title={name} placement="right">
                    <ListItemButton
                      selected={isSelected}
                      onClick={() => handleChatClick(conv.id)}
                      sx={{
                        justifyContent: 'center',
                        borderRadius: 2,
                        mx: 1,
                        mb: 0.5,
                        width: 'auto',
                        '&.Mui-selected': {
                          bgcolor: 'primary.light',
                          '&:hover': {
                            bgcolor: 'primary.light',
                          }
                        }
                      }}
                    >
                      <Badge
                        color="success"
                        variant="dot"
                        anchorOrigin={{
                          vertical: 'bottom',
                          horizontal: 'right',
                        }}
                        overlap="circular"
                        invisible={type !== 'DIRECT' || !online}
                      >
                        <Avatar
                          src={avatar}
                          sx={{
                            bgcolor: type === 'GROUP' ? 'secondary.main' : 'primary.main',
                            width: 40,
                            height: 40
                          }}
                        >
                          {!avatar && getAvatarLetter(name)}
                        </Avatar>
                      </Badge>
                      {unreadCount > 0 && (
                        <Badge
                          badgeContent={unreadCount}
                          color="error"
                          sx={{
                            '& .MuiBadge-badge': {
                              top: -5,
                              right: -5,
                            }
                          }}
                        />
                      )}
                    </ListItemButton>
                  </Tooltip>
                </ListItem>
              );
            }

            // Expanded view - show full conversation item
            return (
              <ListItem key={conv.id} disablePadding>
                <ListItemButton
                  selected={isSelected}
                  onClick={() => handleChatClick(conv.id)}
                  sx={{
                    '&.Mui-selected': {
                      bgcolor: 'primary.light',
                      '&:hover': {
                        bgcolor: 'primary.light',
                      }
                    },
                    py: 1.5
                  }}
                >
                  <ListItemAvatar>
                    <Badge
                      color="success"
                      variant="dot"
                      anchorOrigin={{
                        vertical: 'bottom',
                        horizontal: 'right',
                      }}
                      overlap="circular"
                      invisible={type !== 'DIRECT' || !online}
                    >
                      <Avatar
                        src={avatar}
                        sx={{
                          bgcolor: type === 'GROUP' ? 'secondary.main' : 'primary.main',
                          width: 48,
                          height: 48
                        }}
                      >
                        {!avatar && (type === 'GROUP' ? <GroupIcon /> : getAvatarLetter(name))}
                      </Avatar>
                    </Badge>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <Typography
                          variant="body2"
                          fontWeight={isSelected || unreadCount > 0 ? 'bold' : 'normal'}
                          noWrap
                          sx={{ maxWidth: '150px' }}
                        >
                          {name}
                        </Typography>
                        {lastMessageTime && (
                          <Typography variant="caption" color="text.secondary">
                            {lastMessageTime}
                          </Typography>
                        )}
                      </Box>
                    }
                    secondary={
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          noWrap
                          sx={{ maxWidth: '150px' }}
                        >
                          {conv.lastMessagePreview || 'No messages yet'}
                        </Typography>
                        {unreadCount > 0 && (
                          <Badge
                            badgeContent={unreadCount}
                            color="error"
                            sx={{
                              '& .MuiBadge-badge': {
                                fontSize: '0.65rem',
                                height: 18,
                                minWidth: 18,
                              }
                            }}
                          />
                        )}
                      </Box>
                    }
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      )}
    </Box>
  );

  return (
    <>
      {/* Floating menu button for mobile when drawer is closed */}
      {isMobile && !open && (
        <IconButton
          onClick={toggleDrawer}
          sx={{
            position: 'fixed',
            left: 16,
            bottom: 16,
            zIndex: 1200,
            bgcolor: 'primary.main',
            color: 'white',
            '&:hover': {
              bgcolor: 'primary.dark',
            },
            boxShadow: 3
          }}
        >
          <MenuIcon />
        </IconButton>
      )}

      <Drawer
        variant={isMobile ? "temporary" : "permanent"}
        open={open}
        onClose={() => isMobile && setOpen(false)}
        sx={{
          width: open ? drawerWidth : miniDrawerWidth,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: {
            width: open ? drawerWidth : miniDrawerWidth,
            boxSizing: 'border-box',
            bgcolor: 'background.paper',
            borderRight: '1px solid',
            borderColor: 'divider',
            transition: theme.transitions.create('width', {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.enteringScreen,
            }),
            overflowX: 'hidden',
          },
        }}
      >
        <Toolbar /> {/* Spacer for header */}
        {drawerContent}
      </Drawer>
    </>
  );
};

export default Sidebar;