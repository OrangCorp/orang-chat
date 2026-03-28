import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Box,
  Button,
  TextField,
  InputAdornment,
  Popper,
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemAvatar,
  ListItemText,
  ListItemSecondaryAction,
  Avatar,
  CircularProgress,
  ClickAwayListener,
  IconButton,
  Menu,
  MenuItem,
  Typography,
  Divider,
  Badge,
  Tooltip
} from '@mui/material';
import { 
  Search as SearchIcon, 
  Chat as ChatIcon,
  Person as PersonIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon
} from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import userService from '../../services/userService'; // Import the singleton
import { conversationService } from '../../services/messageService';
import logoImg from '../../assets/logo.png';

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
  
  // Profile menu state
  const [profileMenuAnchor, setProfileMenuAnchor] = useState(null);
  const [currentUserProfile, setCurrentUserProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);

  // Load current user's profile
  useEffect(() => {
    if (!user?.id) return;

    const loadUserProfile = async () => {
      try {
        setProfileLoading(true);
        // This will be cached automatically
        const profile = await userService.getProfile(user.id);
        setCurrentUserProfile(profile);
      } catch (error) {
        console.error('Failed to load user profile:', error);
        // Fallback to basic info from auth context
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

  const handleLogout = () => {
    // Clear user service cache on logout
    userService.clearCache();
    logout();
    navigate('/login');
  };

  const handleProfileMenuOpen = (event) => {
    setProfileMenuAnchor(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setProfileMenuAnchor(null);
  };

  const handleProfileClick = () => {
    handleProfileMenuClose();
    navigate('/profile/me');
  };

  const handleSettingsClick = () => {
    handleProfileMenuClose();
    navigate('/settings');
  };

  // Handle search with debounce
  useEffect(() => {
    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      setSearchOpen(false);
      return;
    }

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

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

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchQuery, user?.id]);

  const handleUserClick = (targetUser) => {
    setSearchOpen(false);
    setSearchQuery('');
    navigate(`/profile/${targetUser.userId}`);
  };

  const handleStartChat = async (e, targetUser) => {
    e.stopPropagation(); // Prevent triggering the parent click
    setSearchOpen(false);
    setSearchQuery('');
    
    try {
      const conversation = await conversationService.getOrCreateDirectChat(targetUser.userId);
      navigate(`/chat/${conversation.id}`);
    } catch (error) {
      console.error('Failed to start chat:', error);
    }
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const handleClickAway = () => {
    setSearchOpen(false);
  };

  // Get user's display name for profile
  const getDisplayName = () => {
    if (currentUserProfile?.displayName) return currentUserProfile.displayName;
    if (user?.username) return user.username;
    if (user?.email) return user.email.split('@')[0];
    return 'User';
  };

  // Get user's avatar initial
  const getAvatarInitial = () => {
    const name = getDisplayName();
    return name.charAt(0).toUpperCase();
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar>
        {/* Logo */}
        <Box sx={{ flexGrow: 1 }}>
          <img 
            src={logoImg} 
            alt="Logo" 
            style={{ height: '40px', cursor: 'pointer' }}
            onClick={() => navigate('/')}
          />
        </Box>

        {/* Search Bar */}
        <Box 
          ref={searchAnchorRef}
          sx={{ 
            position: 'relative',
            width: '350px',
            mx: 2
          }}
        >
          <TextField
            size="small"
            placeholder="Search users..."
            value={searchQuery}
            onChange={handleSearchChange}
            sx={{
              width: '100%',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              borderRadius: 1,
              '& .MuiInputBase-root': {
                color: 'white',
              },
              '& .MuiInputBase-input::placeholder': {
                color: 'rgba(255, 255, 255, 0.7)',
              },
              '& .MuiSvgIcon-root': {
                color: 'white',
              }
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
              endAdornment: searching && (
                <InputAdornment position="end">
                  <CircularProgress size={20} color="inherit" />
                </InputAdornment>
              )
            }}
          />

          {/* Search Results Dropdown */}
          {searchOpen && (
            <ClickAwayListener onClickAway={handleClickAway}>
              <Popper
                open={searchOpen}
                anchorEl={searchAnchorRef.current}
                placement="bottom-start"
                sx={{ width: searchAnchorRef.current?.clientWidth, zIndex: 1300 }}
              >
                <Paper elevation={4} sx={{ mt: 1, maxHeight: 400, overflow: 'auto' }}>
                  <List dense>
                    {searchResults.map((result) => (
                      <ListItem
                        key={result.userId}
                        disablePadding
                        secondaryAction={
                          <IconButton
                            edge="end"
                            color="primary"
                            onClick={(e) => handleStartChat(e, result)}
                            sx={{ mr: 1 }}
                          >
                            <ChatIcon />
                          </IconButton>
                        }
                      >
                        <ListItemButton onClick={() => handleUserClick(result)}>
                          <ListItemAvatar>
                            <Avatar src={result.avatarUrl}>
                              {result.displayName?.charAt(0).toUpperCase()}
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                            primary={result.displayName}
                            secondary={
                              <>
                                {result.online ? (
                                  <span style={{ color: '#4caf50' }}>● Online</span>
                                ) : (
                                  <span>Last seen {result.lastSeen ? new Date(result.lastSeen).toLocaleDateString() : 'recently'}</span>
                                )}
                              </>
                            }
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

        {/* User Profile Section */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {/* Profile Button with Avatar */}
          <Tooltip title="Profile & Settings">
            <Button
              onClick={handleProfileMenuOpen}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                textTransform: 'none',
                color: 'white',
                '&:hover': {
                  backgroundColor: 'rgba(255, 255, 255, 0.1)',
                },
                borderRadius: '20px',
                px: 2,
                py: 0.5
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
                invisible={!currentUserProfile?.online}
              >
                <Avatar 
                  src={currentUserProfile?.avatarUrl}
                  sx={{ 
                    width: 32, 
                    height: 32,
                    bgcolor: 'secondary.main'
                  }}
                >
                  {!profileLoading && getAvatarInitial()}
                  {profileLoading && <CircularProgress size={24} sx={{ color: 'white' }} />}
                </Avatar>
              </Badge>
              <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left' }}>
                <Typography variant="body2" sx={{ fontWeight: 'bold', lineHeight: 1.2 }}>
                  {getDisplayName()}
                </Typography>
                <Typography variant="caption" sx={{ opacity: 0.8 }}>
                  {currentUserProfile?.online ? 'Online' : 'Offline'}
                </Typography>
              </Box>
              <KeyboardArrowDownIcon sx={{ fontSize: 20 }} />
            </Button>
          </Tooltip>
        </Box>
      </Toolbar>

      {/* Profile Menu Dropdown */}
      <Menu
        anchorEl={profileMenuAnchor}
        open={Boolean(profileMenuAnchor)}
        onClose={handleProfileMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        PaperProps={{
          sx: {
            mt: 1,
            width: 280,
            borderRadius: 2,
            overflow: 'hidden'
          }
        }}
      >
        {/* Profile Header */}
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Badge
              color="success"
              variant="dot"
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              overlap="circular"
              invisible={!currentUserProfile?.online}
            >
              <Avatar 
                src={currentUserProfile?.avatarUrl}
                sx={{ 
                  width: 48, 
                  height: 48,
                  bgcolor: 'secondary.main',
                  border: '2px solid white'
                }}
              >
                {getAvatarInitial()}
              </Avatar>
            </Badge>
            <Box>
              <Typography variant="subtitle1" fontWeight="bold">
                {getDisplayName()}
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.9 }}>
                {currentUserProfile?.online ? 'Active now' : 'Offline'}
              </Typography>
            </Box>
          </Box>
        </Box>

        <Divider />

        {/* Menu Items */}
        <MenuItem onClick={handleProfileClick}>
          <PersonIcon sx={{ mr: 2, fontSize: 20 }} />
          <Typography variant="body2">My Profile</Typography>
        </MenuItem>
        
        <MenuItem onClick={handleSettingsClick}>
          <SettingsIcon sx={{ mr: 2, fontSize: 20 }} />
          <Typography variant="body2">Settings</Typography>
        </MenuItem>
        
        <Divider />
        
        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
          <LogoutIcon sx={{ mr: 2, fontSize: 20 }} />
          <Typography variant="body2">Logout</Typography>
        </MenuItem>
      </Menu>
    </AppBar>
  );
};

export default Header;