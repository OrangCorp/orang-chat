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
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemAvatar,
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
  Pending as PendingIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Contacts as ContactsIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService';
import messageService from '../services/messageService';
import { emitConversationCreated } from '../utils/conversationEvents';

// Conditional logging
const isDev = import.meta.env.DEV;
const log = (...args) => isDev && console.log(...args);

const Profile = () => {
  const { userId } = useParams();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

  const [contactStatus, setContactStatus] = useState(null);
  const [contactId, setContactId] = useState(null);
  const [isIncomingRequest, setIsIncomingRequest] = useState(false);
  const [checkingContact, setCheckingContact] = useState(true);

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editDisplayName, setEditDisplayName] = useState('');
  const [editBio, setEditBio] = useState('');
  const [editErrors, setEditErrors] = useState({ displayName: '', bio: '' });
  const [saving, setSaving] = useState(false);

  const [contactsDialogOpen, setContactsDialogOpen] = useState(false);
  const [contacts, setContacts] = useState([]);
  const [contactsLoading, setContactsLoading] = useState(false);

  const isOwnProfile = !userId || userId === 'me' || userId === currentUser?.id;
  const profileUserId = isOwnProfile ? currentUser?.id : userId;

  const loadProfile = async (forceRefresh = false) => {
    if (!profileUserId) return;
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getProfile(profileUserId, forceRefresh);
      setProfile(data);
    } catch (err) {
      setError(err.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const loadContacts = async () => {
    try {
      setContactsLoading(true);
      const contactsList = await userService.getContacts();
      const acceptedContacts = contactsList.filter(c => c.status === 'ACCEPTED');
      
      const contactIds = acceptedContacts.map(c => 
        c.requesterId === currentUser.id ? c.recipientId : c.requesterId
      );
      
      if (contactIds.length > 0) {
        await userService.getProfiles(contactIds);
      }
      
      setContacts(contactIds);
    } catch (err) {
      // Silent fail
    } finally {
      setContactsLoading(false);
    }
  };

  const handleOpenContactsDialog = () => {
    setContactsDialogOpen(true);
    loadContacts();
  };

  useEffect(() => {
    if (!profileUserId) return;
    loadProfile(true);
    checkContactStatus();
  }, [profileUserId]);

  useEffect(() => {
    if (!profileUserId) return;
    const handleOnlineStatusUpdated = () => {
      const cachedProfile = userService.profileCache.get(profileUserId);
      if (cachedProfile) setProfile(cachedProfile);
    };
    window.addEventListener('online-status-updated', handleOnlineStatusUpdated);
    return () => window.removeEventListener('online-status-updated', handleOnlineStatusUpdated);
  }, [profileUserId]);

  useEffect(() => {
      if (!profileUserId || isOwnProfile) return;
      
      const handleRefresh = () => {
        //console.log('🔄 Profile refresh event received');
        checkContactStatus();
      };
      
      window.addEventListener('contact-request-resolved', handleRefresh);
      window.addEventListener('refresh-notifications', handleRefresh);
      
      //console.log('👂 Profile listening for events for user:', profileUserId);
      
      return () => {
        window.removeEventListener('contact-request-resolved', handleRefresh);
        window.removeEventListener('refresh-notifications', handleRefresh);
      };
  }, [profileUserId, isOwnProfile]);

  const checkContactStatus = async () => {
    if (!profileUserId || isOwnProfile || !currentUser?.id) {
      setCheckingContact(false);
      return;
    }
    try {
      setCheckingContact(true);
      const [contacts, incomingRequests, outgoingRequests] = await Promise.all([
        userService.getContacts(true).catch(() => []),
        userService.getIncomingRequests(true).catch(() => []),
        userService.getOutgoingRequests(true).catch(() => [])
      ]);
      
      const contact = contacts.find(c => 
        c.requesterId === profileUserId || c.recipientId === profileUserId
      );
      
      const incomingRequest = incomingRequests.find(r => r.requesterId === profileUserId);
      const outgoingRequest = outgoingRequests.find(r => r.recipientId === profileUserId);
      
      if (incomingRequest) {
        setContactStatus('PENDING');
        setIsIncomingRequest(true);
        setContactId(incomingRequest.id);
      } else if (outgoingRequest) {
        setContactStatus('PENDING');
        setIsIncomingRequest(false);
        setContactId(outgoingRequest.id);
      } else if (contact) {
        setContactStatus(contact.status);
        setContactId(contact.id);
      } else {
        setContactStatus(null);
        setContactId(null);
        setIsIncomingRequest(false);
      }
    } catch (err) {
      setContactStatus(null);
    } finally {
      setCheckingContact(false);
    }
  };

  const handleStartChat = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      const conversation = await messageService.getOrCreateDirectChat(profileUserId);
      emitConversationCreated(conversation);
      navigate(`/chat/${conversation.id}`);
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to start chat', severity: 'error' });
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
      setSnackbar({ open: true, message: `Invitation sent to ${profile?.displayName}`, severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to send invitation', severity: 'error' });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRemoveContact = async () => {
    if (!contactId) return;
    try {
      setActionInProgress(true);
      await userService.removeContact(contactId);
      setContactStatus(null);
      setContactId(null);
      setSnackbar({ open: true, message: 'Contact removed', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to remove contact', severity: 'error' });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleAcceptRequest = async () => {
    if (!contactId) return;
    try {
      setActionInProgress(true);
      await userService.acceptContactRequest(contactId);
      setContactStatus('ACCEPTED');
      setSnackbar({ open: true, message: 'Contact request accepted', severity: 'success' });
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId, userId: profileUserId } 
      }));
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to accept request', severity: 'error' });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRejectRequest = async () => {
    if (!contactId) return;
    try {
      setActionInProgress(true);
      await userService.rejectContactRequest(contactId);
      setContactStatus(null);
      setContactId(null);
      setSnackbar({ open: true, message: 'Request rejected', severity: 'info' });
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId, userId: profileUserId } 
      }));
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to reject request', severity: 'error' });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleCancelRequest = async () => {
    if (!contactId) return;
    try {
      setActionInProgress(true);
      await userService.cancelContactRequest(contactId);
      setContactStatus(null);
      setContactId(null);
      setSnackbar({ open: true, message: 'Invitation cancelled', severity: 'info' });
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to cancel invitation', severity: 'error' });
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRefresh = async () => {
    await loadProfile(true);
    await checkContactStatus();
    setSnackbar({ open: true, message: 'Profile refreshed', severity: 'success' });
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
    if (!editDisplayName.trim()) { errors.displayName = 'Display name is required'; isValid = false; }
    else if (editDisplayName.length < 2) { errors.displayName = 'Display name must be at least 2 characters'; isValid = false; }
    else if (editDisplayName.length > 50) { errors.displayName = 'Display name must be 50 characters or less'; isValid = false; }
    if (editBio.length > 500) { errors.bio = 'Bio must be 500 characters or less'; isValid = false; }
    setEditErrors(errors);
    return isValid;
  };

  const handleSaveProfile = async () => {
    if (!validateEditForm()) return;
    try {
      setSaving(true);
      const updateData = {};
      if (editDisplayName !== profile.displayName) updateData.displayName = editDisplayName.trim();
      if (editBio !== (profile.bio || '')) updateData.bio = editBio.trim();
      if (Object.keys(updateData).length === 0) { setEditDialogOpen(false); return; }
      const updatedProfile = await userService.updateProfile(profileUserId, updateData);
      setProfile(updatedProfile);
      setSnackbar({ open: true, message: 'Profile updated', severity: 'success' });
      setEditDialogOpen(false);
    } catch (err) {
      setSnackbar({ open: true, message: err.message || 'Failed to update profile', severity: 'error' });
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
      return date.toLocaleDateString();
    } catch { return 'Unknown'; }
  };

  const renderButtons = () => {
    if (isOwnProfile) {
      return (
        <Button variant="contained" startIcon={<EditIcon />} onClick={handleOpenEditDialog} fullWidth>
          Edit Profile
        </Button>
      );
    }

    if (checkingContact) {
      return <Button variant="contained" disabled fullWidth><CircularProgress size={20} sx={{ mr: 1 }} />Checking...</Button>;
    }

    // Incoming request
    if (contactStatus === 'PENDING' && isIncomingRequest) {
      return (
        <Stack direction="row" spacing={1}>
          <Button variant="contained" color="success" startIcon={<CheckCircleIcon />} onClick={handleAcceptRequest} disabled={actionInProgress} fullWidth>Accept</Button>
          <Button variant="outlined" color="error" onClick={handleRejectRequest} disabled={actionInProgress} fullWidth>Reject</Button>
        </Stack>
      );
    }

    // Outgoing request
    if (contactStatus === 'PENDING' && !isIncomingRequest) {
      return (
        <Stack direction="row" spacing={1}>
          <Button variant="contained" disabled startIcon={<PendingIcon />} fullWidth sx={{ bgcolor: 'warning.main' }}>Invitation Sent</Button>
          <Button variant="outlined" color="error" onClick={handleCancelRequest} disabled={actionInProgress}>Cancel</Button>
        </Stack>
      );
    }

    // Accepted
    if (contactStatus === 'ACCEPTED') {
      return (
        <Stack direction="row" spacing={1}>
          <Button variant="contained" startIcon={<ChatIcon />} onClick={handleStartChat} disabled={actionInProgress} fullWidth>Chat</Button>
          <Button variant="outlined" color="error" startIcon={<PersonRemoveIcon />} onClick={handleRemoveContact} disabled={actionInProgress}>Remove</Button>
        </Stack>
      );
    }

    // No relationship
    return (
      <Stack direction="row" spacing={1}>
        <Button variant="contained" startIcon={<ChatIcon />} onClick={handleStartChat} disabled={actionInProgress} fullWidth>Chat</Button>
        <Button variant="outlined" startIcon={<PersonAddIcon />} onClick={handleInviteToContacts} disabled={actionInProgress} fullWidth>Invite</Button>
      </Stack>
    );
  };

  if (loading) return (
    <Container maxWidth="sm" sx={{ mt: 4 }}><Card sx={{ p: 4, textAlign: 'center' }}><CircularProgress /><Typography sx={{ mt: 2 }}>Loading profile...</Typography></Card></Container>
  );

  if (error || !profile) return (
    <Container maxWidth="sm" sx={{ mt: 4 }}><Card sx={{ p: 4 }}><Alert severity="error" sx={{ mb: 2 }}>{error || 'Profile not found'}</Alert><Button variant="contained" onClick={() => navigate('/')}>Go Home</Button></Card></Container>
  );

  return (
    <Container maxWidth="sm" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <IconButton onClick={() => navigate('/')}><ArrowBackIcon /></IconButton>
        <Tooltip title="Refresh"><IconButton onClick={handleRefresh} disabled={actionInProgress}>{actionInProgress ? <CircularProgress size={24} /> : <RefreshIcon />}</IconButton></Tooltip>
      </Box>

      <Card sx={{ borderRadius: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
            <Avatar src={profile.avatarUrl} sx={{ width: 80, height: 80, bgcolor: 'primary.main' }}>{profile.displayName?.charAt(0).toUpperCase()}</Avatar>
            <Box>
              <Typography variant="h5" fontWeight="bold">{profile.displayName}</Typography>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                <Chip icon={<CircleIcon sx={{ fontSize: 12 }} />} label={profile.online ? 'Online' : 'Offline'} size="small" color={profile.online ? 'success' : 'default'} variant="outlined" />
                {!profile.online && profile.lastSeen && <Typography variant="caption" color="text.secondary">Last seen: {formatLastSeen(profile.lastSeen)}</Typography>}
              </Stack>
            </Box>
          </Stack>

          <Divider sx={{ my: 2 }} />

          {contacts.length > 0 && (
            <>
              <Box sx={{ mb: 2 }}>
                <Button variant="outlined" startIcon={<ContactsIcon />} onClick={handleOpenContactsDialog} fullWidth size="small">
                  {isOwnProfile ? `My Contacts (${contacts.length})` : `View Contacts (${contacts.length})`}
                </Button>
              </Box>
              <Divider sx={{ my: 2 }} />
            </>
          )}

          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>Bio</Typography>
            <Typography variant="body1" sx={{ fontStyle: !profile.bio ? 'italic' : 'normal' }}>
              {profile.bio || (isOwnProfile ? 'Add a bio to let others know you!' : 'No bio provided')}
            </Typography>
          </Box>

          <Divider sx={{ my: 2 }} />
          <Box sx={{ mt: 2 }}>{renderButtons()}</Box>
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => !saving && setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            Edit Profile
            <IconButton onClick={() => setEditDialogOpen(false)} disabled={saving}><CloseIcon /></IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <TextField label="Display Name" value={editDisplayName} onChange={(e) => setEditDisplayName(e.target.value)} error={!!editErrors.displayName} helperText={editErrors.displayName || '2-50 characters'} fullWidth disabled={saving} />
            <TextField label="Bio" value={editBio} onChange={(e) => setEditBio(e.target.value)} error={!!editErrors.bio} helperText={editErrors.bio || `${editBio.length}/500 characters`} fullWidth multiline rows={4} disabled={saving} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 2, pt: 0 }}>
          <Button onClick={() => setEditDialogOpen(false)} disabled={saving}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveProfile} disabled={saving} startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}>{saving ? 'Saving...' : 'Save'}</Button>
        </DialogActions>
      </Dialog>

      {/* Contacts Dialog */}
      <Dialog open={contactsDialogOpen} onClose={() => setContactsDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {isOwnProfile ? 'My Contacts' : `${profile?.displayName}'s Contacts`}
          <IconButton size="small" onClick={() => setContactsDialogOpen(false)}><CloseIcon /></IconButton>
        </DialogTitle>
        <DialogContent dividers>
          {contactsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress size={32} /></Box>
          ) : contacts.length > 0 ? (
            <List sx={{ maxHeight: 400, overflow: 'auto' }}>
              {contacts.map((contactId) => {
                const prof = userService.profileCache.get(contactId);
                return (
                  <ListItem key={contactId} disablePadding>
                    <ListItemButton onClick={() => { setContactsDialogOpen(false); navigate(`/profile/${contactId}`); }}>
                      <ListItemAvatar><Avatar src={prof?.avatarUrl}>{prof?.displayName?.charAt(0).toUpperCase() || '?'}</Avatar></ListItemAvatar>
                      <ListItemText primary={prof?.displayName || 'Unknown'} secondary={prof?.online ? <Typography component="span" variant="caption" color="success.main">● Online</Typography> : 'Offline'} />
                    </ListItemButton>
                  </ListItem>
                );
              })}
            </List>
          ) : (
            <Box sx={{ p: 3, textAlign: 'center' }}><Typography color="text.secondary">No contacts</Typography></Box>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setContactsDialogOpen(false)}>Close</Button></DialogActions>
      </Dialog>

      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert onClose={() => setSnackbar({ ...snackbar, open: false })} severity={snackbar.severity} variant="filled">{snackbar.message}</Alert>
      </Snackbar>
    </Container>
  );
};

export default Profile;