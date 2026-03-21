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
  Grid
} from '@mui/material';
import {
  Chat as ChatIcon,
  Person as PersonIcon,
  CalendarToday as CalendarIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { conversationService } from '../services/messageService';

const Profile = () => {
  const { userId } = useParams();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();
  
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [startingChat, setStartingChat] = useState(false);

  // Determine if viewing own profile or someone else's
  const isOwnProfile = !userId || userId === 'me' || userId === currentUser?.id;
  const profileUserId = isOwnProfile ? currentUser?.id : userId;

  useEffect(() => {
    if (!profileUserId) return;

    const loadProfile = async () => {
      try {
        setLoading(true);
        const data = await userService.getProfile(profileUserId);
        setProfile(data);
        setError(null);
      } catch (err) {
        setError('Failed to load profile');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [profileUserId]);

  const handleStartChat = async () => {
    if (!profileUserId || isOwnProfile) return;
    
    try {
      setStartingChat(true);
      const conversation = await conversationService.getOrCreateDirectChat(profileUserId);
      navigate(`/chat/${conversation.id}`);
    } catch (err) {
      console.error('Failed to start chat:', err);
      setError('Failed to start chat');
    } finally {
      setStartingChat(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <CircularProgress />
        </Paper>
      </Container>
    );
  }

  if (error || !profile) {
    return (
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="error" gutterBottom>
            {error || 'Profile not found'}
          </Typography>
          <Button variant="contained" onClick={() => navigate('/')}>
            Go Home
          </Button>
        </Paper>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4 }}>
      <Paper 
        elevation={3} 
        sx={{ 
          p: 4,
          borderRadius: 4,
          bgcolor: 'secondary.main',
          color: 'white',
          mb: 3
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
          <Avatar
            src={profile.avatarUrl}
            sx={{
              width: 120,
              height: 120,
              border: '4px solid white',
              bgcolor: 'secondary.main',
              fontSize: '3rem'
            }}
          >
            {profile.displayName?.charAt(0).toUpperCase()}
          </Avatar>
          
          <Box sx={{ flex: 1 }}>
            <Typography variant="h4" gutterBottom fontWeight="bold">
              {profile.displayName}
            </Typography>
            
            <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
              <Chip
                icon={<PersonIcon />}
                label={profile.isOnline ? 'Online' : 'Offline'}
                size="small"
                sx={{
                  bgcolor: profile.isOnline ? 'success.main' : 'text.secondary',
                  color: 'white',
                  '& .MuiChip-icon': { color: 'white' }
                }}
              />
              <Chip
                icon={<CalendarIcon />}
                label={`Joined ${formatDate(profile.createdAt)}`}
                size="small"
                sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }}
              />
            </Box>
          </Box>
        </Box>
      </Paper>

      <Paper elevation={3} sx={{ p: 4, borderRadius: 4 }}>
        <Grid container spacing={3}>
          {/* Bio Section */}
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom color="primary">
              About
            </Typography>
            <Typography variant="body1" paragraph sx={{ minHeight: '60px' }}>
              {profile.bio || 'No bio yet.'}
            </Typography>
            <Divider sx={{ my: 2 }} />
          </Grid>

          {/* Profile Details */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Display Name
            </Typography>
            <Typography variant="body1" gutterBottom>
              {profile.displayName}
            </Typography>
          </Grid>


          {/*<Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Last Seen
            </Typography>
            <Typography variant="body1">
              {profile.lastSeen ? new Date(profile.lastSeen).toLocaleString() : 'Never'}
            </Typography>
          </Grid>*/}

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          {/* Action Buttons */}
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button
                variant="outlined"
                onClick={() => navigate('/')}
              >
                Back
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
                    px: 4
                  }}
                >
                  {startingChat ? 'Starting chat...' : 'Start Chat'}
                </Button>
              )}

              {isOwnProfile && (
                <Button
                  variant="contained"
                  color="primary"
                  onClick={() => {/* Open edit mode later */}}
                  sx={{
                    borderRadius: '20px 8px 20px 8px',
                    px: 4
                  }}
                >
                  Edit Profile
                </Button>
              )}
            </Box>
          </Grid>
        </Grid>
      </Paper>
    </Container>
  );
};

export default Profile;