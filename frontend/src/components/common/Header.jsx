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
  IconButton
} from '@mui/material';
import { Search as SearchIcon, Chat as ChatIcon } from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
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

  const handleLogout = () => {
    logout();
    navigate('/login');
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
                                {result.isOnline ? (
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

        {/* Logout Button */}
        <Button 
          variant="contained"
          color="secondary"
          onClick={handleLogout}
          sx={{ 
            borderRadius: '20px 8px 20px 8px',
            px: 3,
            textTransform: 'none',
            fontWeight: 'bold'
          }}
        >
          Logout
        </Button>
      </Toolbar>
    </AppBar>
  );
};

export default Header;