import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Card,
  CardContent,
  Avatar,
  Typography,
  Box,
  Button,
  Divider,
  CircularProgress,
  Alert,
  Chip,
  Stack,
  Snackbar,
  IconButton,
  Tooltip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  Chat as ChatIcon,
  PersonAdd as PersonAddIcon,
  PersonRemove as PersonRemoveIcon,
  Edit as EditIcon,
  ArrowBack as ArrowBackIcon,
  Refresh as RefreshIcon,
  Circle as CircleIcon,
  Save as SaveIcon,
  Close as CloseIcon,
  Pending as PendingIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService';
import messageService from '../services/messageService';

const Profile = () => {
  const { userId } = useParams();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

  const [contactStatus, setContactStatus] = useState(null); // 'PENDING', 'ACCEPTED', null
  const [checkingContact, setCheckingContact] = useState(true);

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editDisplayName, setEditDisplayName] = useState('');
  const [editBio, setEditBio] = useState('');
  const [editErrors, setEditErrors] = useState({ displayName: '', bio: '' });
  const [saving, setSaving] = useState(false);

  const isOwnProfile = !userId || userId === 'me' || userId === currentUser?.id;
  const profileUserId = isOwnProfile ? currentUser?.id : userId;

  // Load profile
  const loadProfile = async (forceRefresh = false) => {
    if (!profileUserId) return;
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getProfile(profileUserId, forceRefresh);
      setProfile(data);
    } catch (err) {
      console.error('Failed to load profile:', err);
      setError(err.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  // Check contact status
  const checkContactStatus = async () => {
    if (!profileUserId || isOwnProfile || !currentUser?.id) {
      setCheckingContact(false);
      return;
    }

    try {
      setCheckingContact(true);
      const contacts = await userService.getContacts(currentUser.id);
      const contact = contacts.find(c => 
        c.requesterId === profileUserId || c.recipientId === profileUserId
      );
      setContactStatus(contact ? contact.status : null);
    } catch (err) {
      console.error('Failed to check contact status:', err);
      setContactStatus(null);
    } finally {
      setCheckingContact(false);
    }
  };

  useEffect(() => {
    if (!profileUserId) return;

    const init = async () => {
      await loadProfile(true);
      await checkContactStatus();
    };
    init();
  }, [profileUserId]);

  const handleStartChat = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      const conversation = await messageService.getOrCreateDirectChat(profileUserId);
      navigate(`/chat/${conversation.id}`);
    } catch (err) {
      console.error('Failed to start chat:', err);
      setSnackbar({
        open: true,
        message: 'Failed to start chat. Please try again.',
        severity: 'error'
      });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleInviteToContacts = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      await userService.sendContactRequest(profileUserId);
      setContactStatus('PENDING');
      setSnackbar({
        open: true,
        message: `Invitation sent to ${profile?.displayName}`,
        severity: 'success'
      });
    } catch (err) {
      console.error('Failed to invite:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to send invitation.',
        severity: 'error'
      });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRemoveContact = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      const contacts = await userService.getContacts(currentUser.id);
      const contact = contacts.find(c => 
        c.requesterId === profileUserId || c.recipientId === profileUserId
      );
      if (!contact) throw new Error('Contact not found');

      await userService.removeContact(contact.id);
      setContactStatus(null);
      setSnackbar({
        open: true,
        message: `${profile?.displayName} removed from contacts`,
        severity: 'success'
      });
    } catch (err) {
      console.error('Failed to remove contact:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to remove contact.',
        severity: 'error'
      });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRefresh = async () => {
    await loadProfile(true);
    await checkContactStatus();
    setSnackbar({
      open: true,
      message: 'Profile refreshed',
      severity: 'success'
    });
  };

  const handleOpenEditDialog = () => {
    setEditDisplayName(profile.displayName || '');
    setEditBio(profile.bio || '');
    setEditErrors({ displayName: '', bio: '' });
    setEditDialogOpen(true);
  };

  const validateEditForm = () => {
    const errors = { displayName: '', bio: '' };
    let isValid = true;

    if (!editDisplayName.trim()) {
      errors.displayName = 'Display name is required';
      isValid = false;
    } else if (editDisplayName.length < 2) {
      errors.displayName = 'Display name must be at least 2 characters';
      isValid = false;
    } else if (editDisplayName.length > 50) {
      errors.displayName = 'Display name must be 50 characters or less';
      isValid = false;
    }

    if (editBio.length > 500) {
      errors.bio = 'Bio must be 500 characters or less';
      isValid = false;
    }

    setEditErrors(errors);
    return isValid;
  };

  const handleSaveProfile = async () => {
    if (!validateEditForm()) return;

    try {
      setSaving(true);
      const updateData = {};
      if (editDisplayName !== profile.displayName) {
        updateData.displayName = editDisplayName.trim();
      }
      if (editBio !== (profile.bio || '')) {
        updateData.bio = editBio.trim();
      }
      if (Object.keys(updateData).length === 0) {
        setEditDialogOpen(false);
        return;
      }

      const updatedProfile = await userService.updateProfile(profileUserId, updateData);
      setProfile(updatedProfile);
      setSnackbar({
        open: true,
        message: 'Profile updated successfully',
        severity: 'success'
      });
      setEditDialogOpen(false);
    } catch (err) {
      console.error('Failed to update profile:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to update profile',
        severity: 'error'
      });
    } finally {
      setSaving(false);
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

  const handleCloseSnackbar = () => setSnackbar({ ...snackbar, open: false });

  const renderContactButton = () => {
    if (isOwnProfile) return null;

    if (checkingContact) {
      return (
        <Button variant="contained" disabled fullWidth>
          <CircularProgress size={20} sx={{ mr: 1 }} />
          Checking...
        </Button>
      );
    }

    if (contactStatus === 'ACCEPTED') {
      return (
        <Button
          variant="contained"
          color="error"
          startIcon={<PersonRemoveIcon />}
          onClick={handleRemoveContact}
          disabled={actionInProgress}
          fullWidth
        >
          {actionInProgress ? 'Removing...' : 'Remove Contact'}
        </Button>
      );
    }

    if (contactStatus === 'PENDING') {
      return (
        <Button
          variant="contained"
          disabled
          startIcon={<PendingIcon />}
          fullWidth
          sx={{ bgcolor: 'warning.main', '&.Mui-disabled': { bgcolor: 'warning.main', opacity: 0.7 } }}
        >
          Invitation Pending
        </Button>
      );
    }

    return (
      <Button
        variant="contained"
        color="secondary"
        startIcon={<PersonAddIcon />}
        onClick={handleInviteToContacts}
        disabled={actionInProgress}
        fullWidth
      >
        {actionInProgress ? 'Sending...' : 'Invite to Contacts'}
      </Button>
    );
  };

  if (loading) return (
    <Container maxWidth="sm" sx={{ mt: 4 }}>
      <Card sx={{ p: 4, textAlign: 'center', boxShadow: '0 8px 32px rgba(0,0,0,0.12)' }}>
        <CircularProgress />
        <Typography sx={{ mt: 2 }} color="text.secondary">Loading profile...</Typography>
      </Card>
    </Container>
  );

  if (error || !profile) return (
    <Container maxWidth="sm" sx={{ mt: 4 }}>
      <Card sx={{ p: 4, boxShadow: '0 8px 32px rgba(0,0,0,0.12)' }}>
        <Alert severity="error" sx={{ mb: 2 }}>{error || 'Profile not found'}</Alert>
        <Button variant="contained" onClick={() => navigate('/')}>Go Home</Button>
      </Card>
    </Container>
  );

  return (
    <Container maxWidth="sm" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <IconButton onClick={() => navigate('/')} sx={{ bgcolor: 'background.paper', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
          <ArrowBackIcon />
        </IconButton>
        <Tooltip title="Refresh profile">
          <IconButton onClick={handleRefresh} disabled={actionInProgress} sx={{ bgcolor: 'background.paper', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
            {actionInProgress ? <CircularProgress size={24} /> : <RefreshIcon />}
          </IconButton>
        </Tooltip>
      </Box>

      <Card sx={{ borderRadius: 3, overflow: 'hidden', boxShadow: '0 12px 48px rgba(0,0,0,0.15), 0 4px 12px rgba(0,0,0,0.08)', transition: 'box-shadow 0.3s ease-in-out', '&:hover': { boxShadow: '0 16px 56px rgba(0,0,0,0.2)' } }}>
        <CardContent sx={{ p: 3 }}>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
            <Avatar src={profile.avatarUrl} sx={{ width: 80, height: 80, bgcolor: 'primary.main', boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}>
              {profile.displayName?.charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ flex: 1 }}>
              <Typography variant="h5" fontWeight="bold">{profile.displayName}</Typography>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                <Chip icon={<CircleIcon sx={{ fontSize: 12 }} />} label={profile.online ? 'Online' : 'Offline'} size="small" color={profile.online ? 'success' : 'default'} variant="outlined" />
                {!profile.online && profile.lastSeen && (
                  <Typography variant="caption" color="text.secondary">Last seen: {formatLastSeen(profile.lastSeen)}</Typography>
                )}
              </Stack>
            </Box>
          </Stack>

          <Divider sx={{ my: 2 }} />

          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>Bio</Typography>
            <Typography variant="body1" sx={{ fontStyle: !profile.bio ? 'italic' : 'normal' }}>
              {profile.bio || (isOwnProfile ? 'Add a bio to let others know you!' : 'No bio provided')}
            </Typography>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
            {!isOwnProfile ? (
              <>
                <Button variant="contained" color="primary" startIcon={<ChatIcon />} onClick={handleStartChat} disabled={actionInProgress} fullWidth sx={{ boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                  {actionInProgress ? 'Starting...' : 'Chat'}
                </Button>
                {renderContactButton()}
              </>
            ) : (
              <Button variant="contained" color="primary" startIcon={<EditIcon />} onClick={handleOpenEditDialog} fullWidth sx={{ boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                Edit Profile
              </Button>
            )}
          </Stack>
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => !saving && setEditDialogOpen(false)} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 3, boxShadow: '0 24px 48px rgba(0,0,0,0.2)' } }}>
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            Edit Profile
            <IconButton onClick={() => setEditDialogOpen(false)} disabled={saving}><CloseIcon /></IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <TextField label="Display Name" value={editDisplayName} onChange={(e) => setEditDisplayName(e.target.value)} error={!!editErrors.displayName} helperText={editErrors.displayName || '2-50 characters'} fullWidth disabled={saving} inputProps={{ minLength: 2, maxLength: 50 }} />
            <TextField label="Bio" value={editBio} onChange={(e) => setEditBio(e.target.value)} error={!!editErrors.bio} helperText={editErrors.bio || `${editBio.length}/500 characters`} fullWidth multiline rows={4} disabled={saving} inputProps={{ maxLength: 500 }} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 2, pt: 0 }}>
          <Button onClick={() => setEditDialogOpen(false)} disabled={saving}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveProfile} disabled={saving} startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}>
            {saving ? 'Saving...' : 'Save Changes'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={handleCloseSnackbar} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} variant="filled">{snackbar.message}</Alert>
      </Snackbar>
    </Container>
  );
};

export default Profile;
