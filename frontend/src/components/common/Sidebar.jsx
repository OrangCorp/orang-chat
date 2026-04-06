// Sidebar.js - Fixed for new participants structure
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
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
  Menu as MenuIcon,
  Chat as ChatIcon,
  Group as GroupIcon
} from '@mui/icons-material';
import messageService from '../../services/messageService';
import userService from '../../services/userService';
import { useAuth } from '../../context/AuthContext';

const drawerWidth = 280;
const miniDrawerWidth = 72;

const Sidebar = () => {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(true);
  const [profiles, setProfiles] = useState(new Map());
  const { user } = useAuth();
  const navigate = useNavigate();
  const { chatId } = useParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  useEffect(() => {
    if (isMobile) {
      setOpen(false);
    } else {
      const savedState = localStorage.getItem('sidebar-open');
      if (savedState !== null) {
        setOpen(savedState === 'true');
      } else {
        setOpen(true);
      }
    }
  }, [isMobile]);

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
      const data = await messageService.getConversations();
      setConversations(data);
      setError(null);
      await fetchAllProfiles(data);
    } catch (err) {
      setError('Failed to load conversations');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchAllProfiles = async (conversationsData) => {
    // Get all unique user IDs from direct conversations
    // participants is now an array of objects with userId and role
    const userIds = conversationsData
      .filter(conv => conv.type === 'DIRECT')
      .flatMap(conv => conv.participants)
      .filter(participant => participant.userId !== user?.id)
      .map(participant => participant.userId);
    
    // Remove duplicates
    const uniqueUserIds = [...new Set(userIds)];
    
    if (uniqueUserIds.length > 0) {
      try {
        console.log('sidebar ids: ', uniqueUserIds);
        const profileMap = await userService.getProfiles(uniqueUserIds);
        
        const profilesMap = new Map();
        profileMap.forEach((profile, userId) => {
          if (profile) {
            profilesMap.set(userId, profile);
          }
        });
        setProfiles(profilesMap);
      } catch (err) {
        console.error('Failed to fetch profiles:', err);
      }
    }
  };

  useEffect(() => {
    const updateProfiles = async () => {
      const userIds = conversations
        .filter(conv => conv.type === 'DIRECT')
        .flatMap(conv => conv.participants)
        .filter(participant => participant.userId !== user?.id)
        .map(participant => participant.userId);
      
      const uniqueUserIds = [...new Set(userIds)];
      
      if (uniqueUserIds.length > 0) {
        const profileMap = await userService.getProfiles(uniqueUserIds);
        const profilesMap = new Map();
        profileMap.forEach((profile, userId) => {
          if (profile) {
            profilesMap.set(userId, profile);
          }
        });
        setProfiles(profilesMap);
      }
    };
    
    if (conversations.length > 0 && user) {
      updateProfiles();
    }
  }, [conversations, user]);

  const handleChatClick = (conversationId) => {
    navigate(`/chat/${conversationId}`);
    if (isMobile) {
      setOpen(false);
    }
  };

  const toggleDrawer = () => {
    setOpen(!open);
  };

  const getConversationDisplay = (conv) => {
    if (conv.type === 'DIRECT') {
      // Find the other participant (not the current user)
      const otherParticipant = conv.participants.find(p => p.userId !== user?.id);
      if (!otherParticipant) return { name: 'Unknown', avatar: null, id: null, online: false };
      
      const otherId = otherParticipant.userId;
      const profile = profiles.get(otherId);
      const displayName = profile?.displayName;
      const avatarUrl = profile?.avatarUrl;
      const online = profile?.online || false;
      
      const name = displayName || 'Unknown User';
      
      return { name, avatar: avatarUrl, id: otherId, online, type: 'DIRECT' };
    } else {
      // Group chat - participants is array of objects
      const participantCount = conv.participants?.length || 0;
      const displayName = conv.name || `Group (${participantCount})`;
      
      return { 
        name: displayName, 
        avatar: null, 
        id: conv.id, 
        online: false, 
        type: 'GROUP',
        participantCount
      };
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

  const getLastMessagePreview = (conv) => {
    if (!conv.lastMessagePreview) {
      return '';
    }
    return conv.lastMessagePreview;
  };

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
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

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress size={24} />
        </Box>
      )}

      {error && (
        <Box sx={{ p: 2 }}>
          <Typography color="error" variant="body2" align="center" component="div">
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

      {!loading && !error && conversations.length === 0 && (
        <Box sx={{ p: 2, textAlign: 'center', mt: 4 }}>
          <ChatIcon sx={{ fontSize: 32, color: 'text.disabled', mb: 2 }} />
          <Typography variant="body1" color="text.secondary" component="div" gutterBottom>
            No chats yet
          </Typography>
          <Typography variant="body2" color="text.secondary" component="div">
            Start a conversation to see it here
          </Typography>
        </Box>
      )}

      {!loading && conversations.length > 0 && (
        <List sx={{ flex: 1, overflow: 'auto', pt: 0 }}>
          {conversations.map((conv) => {
            const { name, avatar, id: otherId, online, type } = getConversationDisplay(conv);
            const isSelected = conv.id === chatId;
            const lastMessageTime = getLastMessageTime(conv);
            const lastMessagePreview = getLastMessagePreview(conv);
            const unreadCount = conv.unreadCount || 0;

            if (!open) {
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
                          {!avatar && (type === 'GROUP' ? <GroupIcon /> : getAvatarLetter(name))}
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
                    py: 1.5,
                    px: 2
                  }}
                >
                  <ListItemAvatar sx={{ minWidth: 56 }}>
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
                  
                  <Box sx={{ 
                    flex: 1,
                    minWidth: 0,
                    ml: 1
                  }}>
                    <Box sx={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'space-between',
                      mb: 0.5
                    }}>
                      <Typography
                        component="div"
                        variant="body2"
                        sx={{ 
                          fontWeight: isSelected || unreadCount > 0 ? 'bold' : 'normal',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          flex: 1
                        }}
                      >
                        {name}
                      </Typography>
                      {lastMessageTime && (
                        <Typography 
                          variant="caption" 
                          color="text.secondary"
                          component="span"
                          sx={{ ml: 1, flexShrink: 0 }}
                        >
                          {lastMessageTime}
                        </Typography>
                      )}
                    </Box>
                    
                    {lastMessagePreview && (
                      <Box sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'space-between'
                      }}>
                        <Typography
                          component="div"
                          variant="caption"
                          color="text.secondary"
                          sx={{ 
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            flex: 1
                          }}
                        >
                          {lastMessagePreview}
                        </Typography>
                        {unreadCount > 0 && (
                          <Badge
                            badgeContent={unreadCount}
                            color="error"
                            sx={{
                              ml: 1,
                              flexShrink: 0,
                              '& .MuiBadge-badge': {
                                fontSize: '0.65rem',
                                height: 18,
                                minWidth: 18,
                                position: 'relative',
                                transform: 'none',
                              }
                            }}
                          />
                        )}
                      </Box>
                    )}
                    
                    {!lastMessagePreview && unreadCount > 0 && (
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Badge
                          badgeContent={unreadCount}
                          color="error"
                          sx={{
                            '& .MuiBadge-badge': {
                              fontSize: '0.65rem',
                              height: 18,
                              minWidth: 18,
                              position: 'relative',
                              transform: 'none',
                            }
                          }}
                        />
                      </Box>
                    )}
                  </Box>
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
        <Toolbar />
        {drawerContent}
      </Drawer>
    </>
  );
};

export default Sidebar;