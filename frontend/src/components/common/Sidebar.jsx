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
  Tooltip
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { conversationService } from '../../services/messageService';
import { userService } from '../../services/userService';
import { useAuth } from '../../context/AuthContext';

const drawerWidth = 240;

const Sidebar = () => {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userCache, setUserCache] = useState({}); // Cache for user profiles
  const { user } = useAuth();
  const navigate = useNavigate();
  const { chatId } = useParams();

  useEffect(() => {
    loadConversations();
  }, []);

  useEffect(() => {
    // Fetch profiles for all participants we don't have in cache
    const fetchMissingProfiles = async () => {
      const missingUserIds = new Set();
      conversations.forEach(conv => {
        if (conv.type === 'DIRECT') {
          const otherId = conv.participantIds.find(id => id !== user?.id);
          if (otherId && !userCache[otherId]) {
            missingUserIds.add(otherId);
          }
        }
      });

      if (missingUserIds.size === 0) return;

      const ids = Array.from(missingUserIds);
      // Fetch profiles individually (no batch endpoint yet)
      const profiles = await Promise.all(
        ids.map(id => userService.getProfile(id).catch(() => null))
      );

      const newCache = { ...userCache };
      ids.forEach((id, index) => {
        if (profiles[index]) {
          newCache[id] = profiles[index];
        } else {
          // Fallback
          newCache[id] = { displayName: id.slice(0, 8), userId: id };
        }
      });
      setUserCache(newCache);
    };

    if (conversations.length > 0 && user) {
      fetchMissingProfiles();
    }
  }, [conversations, user, userCache]);

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
  };


  const getConversationDisplay = (conv) => {
    if (conv.type === 'DIRECT') {
      const otherId = conv.participantIds.find(id => id !== user?.id);
      if (!otherId) return { name: 'Unknown', avatar: '?' };
      const profile = userCache[otherId];
      const displayName = profile?.displayName || otherId.slice(0, 8);
      const avatarUrl = profile?.avatarUrl;
      return { name: displayName, avatar: avatarUrl, id: otherId };
    } else {
      // Group chat
      return { name: conv.name || 'Unnamed Group', avatar: null, id: conv.id };
    }
  };

  const getAvatarLetter = (name) => {
    return name ? name.charAt(0).toUpperCase() : '?';
  };

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: {
          width: drawerWidth,
          boxSizing: 'border-box',
          bgcolor: 'background.paper',
          borderRight: '1px solid',
          borderColor: 'divider'
        },
      }}
    >
      <Toolbar /> {/* Spacer for header */}

      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="subtitle2" color="text.secondary">
          Chats
        </Typography>
      </Box>

      <Divider />

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress size={24} />
        </Box>
      )}

      {error && (
        <Box sx={{ p: 2 }}>
          <Typography color="error" variant="body2">
            {error}
          </Typography>
        </Box>
      )}

      {!loading && !error && conversations.length === 0 && (
        <Box sx={{ p: 2, textAlign: 'center' }}>
          <Typography variant="body1" color="text.secondary" gutterBottom>
            No chats yet
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Click the + to start a new conversation
          </Typography>
        </Box>
      )}

      {!loading && conversations.length > 0 && (
        <List sx={{ pt: 0 }}>
          {conversations.map((conv) => {
            const { name, avatar, id: otherId } = getConversationDisplay(conv);
            const isSelected = conv.id === chatId;

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
                    }
                  }}
                >
                  <ListItemAvatar>
                    <Avatar
                      src={avatar}
                      sx={{
                        bgcolor: conv.type === 'GROUP' ? 'secondary.main' : 'primary.main',
                        width: 40,
                        height: 40
                      }}
                    >
                      {!avatar && getAvatarLetter(name)}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={name}
                    secondary={conv.lastMessagePreview || 'Click to view chat'}
                    primaryTypographyProps={{
                      noWrap: true,
                      variant: 'body2',
                      fontWeight: isSelected ? 'bold' : 'normal'
                    }}
                    secondaryTypographyProps={{
                      noWrap: true,
                      variant: 'caption'
                    }}
                  />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      )}
    </Drawer>
  );
};

export default Sidebar;