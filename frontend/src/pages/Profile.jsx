import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Paper,
  Avatar,
  Typography,
  Box,
  Button,
  Divider,
  CircularProgress,
  Chip,
  Grid,
  Alert,
  IconButton,
  Tooltip,
  Snackbar
} from '@mui/material';
import {
  Chat as ChatIcon,
  Person as PersonIcon,
  CalendarToday as CalendarIcon,
  Edit as EditIcon,
  ArrowBack as ArrowBackIcon,
  CheckCircle as CheckCircleIcon,
  Circle as CircleIcon,
  MoreVert as MoreVertIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService'; // Import the singleton
import { conversationService } from '../services/messageService';

const Profile = () => {
  const { userId } = useParams();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();
  
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [startingChat, setStartingChat] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

  // Determine if viewing own profile or someone else's
  const isOwnProfile = !userId || userId === 'me' || userId === currentUser?.id;
  const profileUserId = isOwnProfile ? currentUser?.id : userId;

  useEffect(() => {
    if (!profileUserId) return;

    const loadProfile = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Use the cached user service - profiles are automatically cached
        const data = await userService.getProfile(profileUserId);
        setProfile(data);
      } catch (err) {
        console.error('Failed to load profile:', err);
        setError(err.message || 'Failed to load profile');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [profileUserId]);

  // Handle manual refresh (force fetch from API)
  const handleRefresh = async () => {
    if (!profileUserId) return;
    
    try {
      setRefreshing(true);
      // Force refresh by bypassing cache
      const data = await userService.getProfile(profileUserId, true);
      setProfile(data);
      setSnackbar({
        open: true,
        message: 'Profile refreshed successfully',
        severity: 'success'
      });
    } catch (err) {
      console.error('Failed to refresh profile:', err);
      setSnackbar({
        open: true,
        message: 'Failed to refresh profile',
        severity: 'error'
      });
    } finally {
      setRefreshing(false);
    }
  };

  const handleStartChat = async () => {
    if (!profileUserId || isOwnProfile) return;
    
    try {
      setStartingChat(true);
      const conversation = await conversationService.getOrCreateDirectChat(profileUserId);
      navigate(`/chat/${conversation.id}`);
    } catch (err) {
      console.error('Failed to start chat:', err);
      setSnackbar({
        open: true,
        message: 'Failed to start chat. Please try again.',
        severity: 'error'
      });
    } finally {
      setStartingChat(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return 'Unknown';
    }
  };

  const formatLastSeen = (lastSeen) => {
    if (!lastSeen) return 'Never';
    try {
      const date = new Date(lastSeen);
      const now = new Date();
      const diffMinutes = Math.floor((now - date) / 60000);
      
      if (diffMinutes < 1) return 'Just now';
      if (diffMinutes < 60) return `${diffMinutes} minutes ago`;
      if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} hours ago`;
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'Unknown';
    }
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  if (loading) {
    return (
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <CircularProgress />
          <Typography sx={{ mt: 2 }} color="text.secondary">
            Loading profile...
          </Typography>
        </Paper>
      </Container>
    );
  }

  if (error || !profile) {
    return (
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Alert severity="error" sx={{ mb: 2 }}>
            {error || 'Profile not found'}
          </Alert>
          <Button variant="contained" onClick={() => navigate('/')}>
            Go Home
          </Button>
        </Paper>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      {/* Header with back button and refresh */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <IconButton onClick={() => navigate('/')} sx={{ bgcolor: 'background.paper' }}>
          <ArrowBackIcon />
        </IconButton>
        
        {isOwnProfile && (
          <Tooltip title="Refresh profile">
            <IconButton 
              onClick={handleRefresh} 
              disabled={refreshing}
              sx={{ bgcolor: 'background.paper' }}
            >
              {refreshing ? <CircularProgress size={24} /> : <CheckCircleIcon />}
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {/* Profile Header Card */}
      <Paper 
        elevation={3} 
        sx={{ 
          p: 4,
          borderRadius: 4,
          background: profile.online 
            ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
            : 'linear-gradient(135deg, #6c757d 0%, #495057 100%)',
          color: 'white',
          mb: 3,
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Decorative elements */}
        <Box
          sx={{
            position: 'absolute',
            top: -20,
            right: -20,
            width: 150,
            height: 150,
            borderRadius: '50%',
            bgcolor: 'rgba(255,255,255,0.1)',
            pointerEvents: 'none'
          }}
        />
        
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap' }}>
          <Avatar
            src={profile.avatarUrl}
            sx={{
              width: 120,
              height: 120,
              border: '4px solid white',
              bgcolor: 'secondary.main',
              fontSize: '3rem',
              boxShadow: '0 8px 16px rgba(0,0,0,0.2)'
            }}
          >
            {profile.displayName?.charAt(0).toUpperCase() || <PersonIcon sx={{ fontSize: 60 }} />}
          </Avatar>
          
          <Box sx={{ flex: 1 }}>
            <Typography variant="h4" gutterBottom fontWeight="bold">
              {profile.displayName}
            </Typography>
            
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
              <Chip
                icon={profile.online ? <CircleIcon sx={{ fontSize: 12 }} /> : <CircleIcon />}
                label={profile.online ? 'Online' : 'Offline'}
                size="small"
                sx={{
                  bgcolor: profile.online ? 'success.main' : 'text.disabled',
                  color: 'white',
                  '& .MuiChip-icon': { 
                    color: 'white',
                    fontSize: 12
                  }
                }}
              />
              
              {profile.lastSeen && !profile.online && (
                <Chip
                  label={`Last seen: ${formatLastSeen(profile.lastSeen)}`}
                  size="small"
                  sx={{ 
                    bgcolor: 'rgba(255,255,255,0.2)', 
                    color: 'white',
                    '& .MuiChip-icon': { color: 'white' }
                  }}
                />
              )}
              
              {/* This would be for join date if available */}
              {profile.createdAt && (
                <Chip
                  icon={<CalendarIcon />}
                  label={`Joined ${formatDate(profile.createdAt)}`}
                  size="small"
                  sx={{ 
                    bgcolor: 'rgba(255,255,255,0.2)', 
                    color: 'white',
                    '& .MuiChip-icon': { color: 'white' }
                  }}
                />
              )}
            </Box>
          </Box>
          
          {!isOwnProfile && (
            <Button
              variant="contained"
              startIcon={<ChatIcon />}
              onClick={handleStartChat}
              disabled={startingChat}
              sx={{
                borderRadius: '20px 8px 20px 8px',
                px: 3,
                py: 1,
                bgcolor: 'white',
                color: 'primary.main',
                '&:hover': {
                  bgcolor: 'rgba(255,255,255,0.9)',
                }
              }}
            >
              {startingChat ? 'Starting...' : 'Message'}
            </Button>
          )}
        </Box>
      </Paper>

      {/* Profile Details Card */}
      <Paper elevation={3} sx={{ p: 4, borderRadius: 4 }}>
        <Grid container spacing={3}>
          {/* Bio Section */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom color="primary" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon /> About
            </Typography>
            <Typography 
              variant="body1" 
              paragraph 
              sx={{ 
                minHeight: '60px',
                bgcolor: 'action.hover',
                p: 2,
                borderRadius: 2,
                fontStyle: !profile.bio ? 'italic' : 'normal',
                color: !profile.bio ? 'text.secondary' : 'text.primary'
              }}
            >
              {profile.bio || 'No bio yet. Click edit to add one!'}
            </Typography>
            <Divider sx={{ my: 2 }} />
          </Grid>

          {/* Profile Details Grid */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Display Name
            </Typography>
            <Typography variant="body1" gutterBottom sx={{ fontWeight: 500 }}>
              {profile.displayName}
            </Typography>
          </Grid>

          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              User ID
            </Typography>
            <Typography 
              variant="body2" 
              sx={{ 
                fontFamily: 'monospace',
                bgcolor: 'action.hover',
                p: 0.5,
                borderRadius: 1,
                display: 'inline-block'
              }}
            >
              {profile.userId || profileUserId}
            </Typography>
          </Grid>

          {/* Online Status Details */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Status
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {profile.online ? (
                <CheckCircleIcon sx={{ color: 'success.main', fontSize: 20 }} />
              ) : (
                <CircleIcon sx={{ color: 'text.disabled', fontSize: 20 }} />
              )}
              <Typography variant="body1">
                {profile.online ? 'Active now' : formatLastSeen(profile.lastSeen)}
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          {/* Action Buttons */}
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
              <Button
                variant="outlined"
                onClick={() => navigate('/')}
                sx={{ borderRadius: '8px' }}
              >
                Back to Chats
              </Button>
              
              {!isOwnProfile && (
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<ChatIcon />}
                  onClick={handleStartChat}
                  disabled={startingChat}
                  sx={{
                    borderRadius: '20px 8px 20px 8px',
                    px: 4,
                    py: 1
                  }}
                >
                  {startingChat ? 'Starting chat...' : 'Start Conversation'}
                </Button>
              )}

              {isOwnProfile && (
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<EditIcon />}
                  onClick={() => navigate('/profile/edit')}
                  sx={{
                    borderRadius: '20px 8px 20px 8px',
                    px: 4,
                    py: 1
                  }}
                >
                  Edit Profile
                </Button>
              )}
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Profile;