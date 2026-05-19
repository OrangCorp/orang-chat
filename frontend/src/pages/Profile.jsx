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
  Menu,
  MenuItem,
  ListItemIcon,
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
  Block as BlockIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  MoreVert as MoreVertIcon,
  Contacts as ContactsIcon,
  Search as SearchIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService';
import messageService from '../services/messageService';
import { emitConversationCreated } from '../utils/conversationEvents';

const Profile = () => {
  const { userId } = useParams();
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

  const [contactStatus, setContactStatus] = useState(null); // 'PENDING', 'ACCEPTED', 'BLOCKED', null
  const [contactId, setContactId] = useState(null);
  const [isBlockedByOther, setIsBlockedByOther] = useState(false);
  const [isIncomingRequest, setIsIncomingRequest] = useState(false);
  const [checkingContact, setCheckingContact] = useState(true);

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editDisplayName, setEditDisplayName] = useState('');
  const [editBio, setEditBio] = useState('');
  const [editErrors, setEditErrors] = useState({ displayName: '', bio: '' });
  const [saving, setSaving] = useState(false);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);

  const isOwnProfile = !userId || userId === 'me' || userId === currentUser?.id;
  const profileUserId = isOwnProfile ? currentUser?.id : userId;

  const [contactsDialogOpen, setContactsDialogOpen] = useState(false);
  const [contacts, setContacts] = useState([]);
  const [contactsLoading, setContactsLoading] = useState(false);

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
      console.error('Failed to load contacts:', err);
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

    const init = async () => {
      await loadProfile(true);
      await checkContactStatus();
      await loadContacts(); // Add this line
    };
    init();
  }, [profileUserId]);

  useEffect(() => {
    if (!profileUserId) return;

    const handleOnlineStatusUpdated = () => {
      // Check if the updated user is the one we're viewing
      // Force a refresh of the profile data from cache
      const cachedProfile = userService.profileCache.get(profileUserId);
      if (cachedProfile) {
        setProfile(cachedProfile);
        console.log('Profile online status updated from cache');
      }
    };

    window.addEventListener('online-status-updated', handleOnlineStatusUpdated);

    return () => {
      window.removeEventListener('online-status-updated', handleOnlineStatusUpdated);
    };
  }, [profileUserId]);

  useEffect(() => {
    if (!profileUserId || isOwnProfile) return;

    const handleContactStatusChanged = (event) => {
      const { userId, status } = event.detail;
      if (userId === profileUserId) {
        console.log('Contact status changed for profile user:', status);
        checkContactStatus();
        setSnackbar({
          open: true,
          message: `Contact status updated`,
          severity: 'info'
        });
      }
    };

    window.addEventListener('contact-status-changed', handleContactStatusChanged);

    return () => {
      window.removeEventListener('contact-status-changed', handleContactStatusChanged);
    };
  }, [profileUserId, isOwnProfile]);

  useEffect(() => {
    if (!profileUserId || isOwnProfile) return;

    const handleContactRequestReceived = (event) => {
      const { requesterId } = event.detail;
      // If the request is from the user we're currently viewing, refresh status
      if (requesterId === profileUserId) {
        console.log('Contact request received from profile user, refreshing...');
        checkContactStatus();
      }
    };

    window.addEventListener('contact-request-received', handleContactRequestReceived);

    return () => {
      window.removeEventListener('contact-request-received', handleContactRequestReceived);
    };
  }, [profileUserId, isOwnProfile]);

  // Check contact status
  const checkContactStatus = async () => {
    if (!profileUserId || isOwnProfile || !currentUser?.id) {
      setCheckingContact(false);
      return;
    }

    try {
      setCheckingContact(true);
      
      // Get pending requests separately to determine direction
      const [contacts, blockedUsers, incomingRequests, outgoingRequests] = await Promise.all([
        userService.getContacts(currentUser.id),
        userService.getBlockedUsers().catch(() => []),
        userService.getIncomingRequests().catch(() => []),
        userService.getOutgoingRequests().catch(() => [])
      ]);
      
      // Check regular contacts (ACCEPTED)
      const contact = contacts.find(c => 
        c.requesterId === profileUserId || c.recipientId === profileUserId
      );
      
      // Check if blocked
      const blocked = blockedUsers.find(b => 
        b.requesterId === profileUserId || b.recipientId === profileUserId
      );
      
      // Check pending requests
      const incomingRequest = incomingRequests.find(r => r.requesterId === profileUserId);
      const outgoingRequest = outgoingRequests.find(r => r.recipientId === profileUserId);
      
      if (blocked) {
        if (blocked.blockedBy === currentUser.id) {
          setContactStatus('BLOCKED_BY_ME');
        } else {
          setContactStatus('BLOCKED_BY_OTHER');
          setIsBlockedByOther(true);
        }
        setContactId(blocked.id);
      } else if (incomingRequest) {
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
        setIsBlockedByOther(false);
        setIsIncomingRequest(false);
      }
    } catch (err) {
      console.error('Failed to check contact status:', err);
      setContactStatus(null);
      setIsBlockedByOther(false);
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
      emitConversationCreated(conversation);
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
    if (!profileUserId || isOwnProfile || !contactId) return;
    try {
      setActionInProgress(true);
      await userService.removeContact(contactId);
      setContactStatus(null);
      setContactId(null);
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
      setConfirmDialogOpen(false);
    }
  };

  const handleBlockUser = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      await userService.blockUser(profileUserId);
      setContactStatus('BLOCKED_BY_ME');
      setSnackbar({
        open: true,
        message: `${profile?.displayName} has been blocked`,
        severity: 'success'
      });
      // Refresh contact status to get the contact ID
      await checkContactStatus();
    } catch (err) {
      console.error('Failed to block user:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to block user.',
        severity: 'error'
      });
    } finally {
      setActionInProgress(false);
      setConfirmDialogOpen(false);
      setMenuAnchor(null);
    }
  };

  const handleUnblockUser = async () => {
    if (!profileUserId || isOwnProfile) return;
    try {
      setActionInProgress(true);
      await userService.unblockUser(profileUserId);
      setContactStatus(null);
      setIsBlockedByOther(false);
      setSnackbar({
        open: true,
        message: `${profile?.displayName} has been unblocked`,
        severity: 'success'
      });
      await checkContactStatus();
    } catch (err) {
      console.error('Failed to unblock user:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to unblock user.',
        severity: 'error'
      });
    } finally {
      setActionInProgress(false);
      setConfirmDialogOpen(false);
      setMenuAnchor(null);
    }
  };

  const handleAcceptRequest = async () => {
    if (!contactId) return;
    try {
      setActionInProgress(true);
      await userService.acceptContactRequest(contactId);
      setContactStatus('ACCEPTED');
      setSnackbar({
        open: true,
        message: `You are now contacts with ${profile?.displayName}`,
        severity: 'success'
      });
      
      // Notify header that this request was accepted
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId, userId: profileUserId } 
      }));
    } catch (err) {
      console.error('Failed to accept request:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to accept request.',
        severity: 'error'
      });
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
      setSnackbar({
        open: true,
        message: `Request from ${profile?.displayName} rejected`,
        severity: 'info'
      });
      
      // Notify header that this request was rejected
      window.dispatchEvent(new CustomEvent('contact-request-resolved', { 
        detail: { contactId, userId: profileUserId } 
      }));
    } catch (err) {
      console.error('Failed to reject request:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to reject request.',
        severity: 'error'
      });
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
      setSnackbar({
        open: true,
        message: `Invitation to ${profile?.displayName} cancelled`,
        severity: 'info'
      });
    } catch (err) {
      console.error('Failed to cancel request:', err);
      setSnackbar({
        open: true,
        message: err.message || 'Failed to cancel request.',
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

  const handleMenuOpen = (e) => setMenuAnchor(e.currentTarget);
  const handleMenuClose = () => setMenuAnchor(null);

  const openConfirmDialog = (action) => {
    setConfirmAction(action);
    setConfirmDialogOpen(true);
    handleMenuClose();
  };

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

    // Blocked by other user - cannot interact
    if (isBlockedByOther || contactStatus === 'BLOCKED_BY_OTHER') {
      return (
        <Button
          variant="contained"
          disabled
          fullWidth
          sx={{ bgcolor: 'grey.500' }}
        >
          Cannot interact with this user
        </Button>
      );
    }

    // Blocked by me
    if (contactStatus === 'BLOCKED_BY_ME') {
      return (
        <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
          <Button
            variant="contained"
            color="warning"
            onClick={() => openConfirmDialog('unblock')}
            disabled={actionInProgress}
            fullWidth
          >
            Unblock User
          </Button>
          <Button
            variant="outlined"
            onClick={handleMenuOpen}
            disabled={actionInProgress}
            sx={{ minWidth: '48px' }}
          >
            <MoreVertIcon />
          </Button>
        </Stack>
      );
    }

    // Incoming request - show Accept/Reject
    if (contactStatus === 'PENDING' && isIncomingRequest) {
      return (
        <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
          <Button
            variant="contained"
            color="success"
            startIcon={<CheckCircleIcon />}
            onClick={handleAcceptRequest}
            disabled={actionInProgress}
            fullWidth
          >
            Accept
          </Button>
          <Button
            variant="outlined"
            color="error"
            startIcon={<CancelIcon />}
            onClick={handleRejectRequest}
            disabled={actionInProgress}
            fullWidth
          >
            Reject
          </Button>
        </Stack>
      );
    }

    // Outgoing request - show Cancel button
    if (contactStatus === 'PENDING' && !isIncomingRequest) {
      return (
        <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
          <Button
            variant="contained"
            disabled
            startIcon={<PendingIcon />}
            fullWidth
            sx={{ bgcolor: 'warning.main', '&.Mui-disabled': { bgcolor: 'warning.main', opacity: 0.7 } }}
          >
            Invitation Sent
          </Button>
          <Button
            variant="outlined"
            color="error"
            onClick={handleCancelRequest}
            disabled={actionInProgress}
            sx={{ minWidth: '48px' }}
          >
            Cancel
          </Button>
        </Stack>
      );
    }

    // Accepted contact - show Chat and Contact buttons side by side
    if (contactStatus === 'ACCEPTED') {
      return (
        <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
          <Button
            variant="contained"
            color="primary"
            startIcon={<ChatIcon />}
            onClick={handleStartChat}
            disabled={actionInProgress}
            fullWidth
          >
            Chat
          </Button>
          <Button
            variant="outlined"
            color="error"
            startIcon={<PersonRemoveIcon />}
            onClick={() => openConfirmDialog('remove')}
            disabled={actionInProgress}
            fullWidth
          >
            Remove
          </Button>
          <Button
            variant="outlined"
            onClick={handleMenuOpen}
            disabled={actionInProgress}
            sx={{ minWidth: '48px' }}
          >
            <MoreVertIcon />
          </Button>
        </Stack>
      );
    }

    // No contact relationship - show Chat and Invite buttons side by side
    return (
      <Stack direction="row" spacing={1} sx={{ width: '100%' }}>
        <Button
          variant="contained"
          color="primary"
          startIcon={<ChatIcon />}
          onClick={handleStartChat}
          disabled={actionInProgress}
          fullWidth
        >
          Chat
        </Button>
        <Button
          variant="outlined"
          color="secondary"
          startIcon={<PersonAddIcon />}
          onClick={handleInviteToContacts}
          disabled={actionInProgress}
          fullWidth
        >
          Invite
        </Button>
        <Button
          variant="outlined"
          onClick={handleMenuOpen}
          disabled={actionInProgress}
          sx={{ minWidth: '48px' }}
        >
          <MoreVertIcon />
        </Button>
      </Stack>
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

          {contacts.length > 0 && (
            <>
              <Box sx={{ mb: 2 }}>
                <Button
                  variant="outlined"
                  startIcon={<ContactsIcon />}
                  onClick={handleOpenContactsDialog}
                  fullWidth
                  size="small"
                >
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

          <Box sx={{ mt: 2 }}>
            {!isOwnProfile ? (
              renderContactButton()
            ) : (
              <Button variant="contained" color="primary" startIcon={<EditIcon />} onClick={handleOpenEditDialog} fullWidth sx={{ boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                Edit Profile
              </Button>
            )}
          </Box>
        </CardContent>
      </Card>

      {/* Action Menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
      >
        {contactStatus === 'ACCEPTED' && (
          <MenuItem onClick={() => openConfirmDialog('remove')}>
            <ListItemIcon><PersonRemoveIcon fontSize="small" /></ListItemIcon>
            Remove Contact
          </MenuItem>
        )}
        {contactStatus !== 'BLOCKED_BY_ME' && !isBlockedByOther && (
          <MenuItem onClick={() => openConfirmDialog('block')}>
            <ListItemIcon><BlockIcon fontSize="small" /></ListItemIcon>
            Block User
          </MenuItem>
        )}
      </Menu>

      {/* Confirmation Dialog */}
      <Dialog open={confirmDialogOpen} onClose={() => setConfirmDialogOpen(false)}>
        <DialogTitle>
          {confirmAction === 'block' && 'Block User?'}
          {confirmAction === 'unblock' && 'Unblock User?'}
          {confirmAction === 'remove' && 'Remove Contact?'}
        </DialogTitle>
        <DialogContent>
          <Typography>
            {confirmAction === 'block' && `Are you sure you want to block ${profile?.displayName}? They will not be able to contact you.`}
            {confirmAction === 'unblock' && `Are you sure you want to unblock ${profile?.displayName}? They will be able to contact you again.`}
            {confirmAction === 'remove' && `Are you sure you want to remove ${profile?.displayName} from your contacts?`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDialogOpen(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            color={confirmAction === 'block' ? 'error' : 'primary'}
            onClick={() => {
              if (confirmAction === 'block') handleBlockUser();
              else if (confirmAction === 'unblock') handleUnblockUser();
              else if (confirmAction === 'remove') handleRemoveContact();
            }}
            disabled={actionInProgress}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>

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

      <Dialog 
        open={contactsDialogOpen} 
        onClose={() => setContactsDialogOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3 } }}
      >
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {isOwnProfile ? 'My Contacts' : `${profile?.displayName}'s Contacts`}
          <IconButton size="small" onClick={() => setContactsDialogOpen(false)}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        
        <DialogContent dividers>
          {contactsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress size={32} />
            </Box>
          ) : contacts.length > 0 ? (
            <List sx={{ maxHeight: 400, overflow: 'auto' }}>
              {contacts.map((contactId) => {
                const profile = userService.profileCache.get(contactId);
                return (
                  <ListItem key={contactId} disablePadding>
                    <ListItemButton onClick={() => {
                      setContactsDialogOpen(false);
                      navigate(`/profile/${contactId}`);
                    }}>
                      <ListItemAvatar>
                        <Avatar src={profile?.avatarUrl}>
                          {profile?.displayName?.charAt(0).toUpperCase() || '?'}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText 
                        primary={profile?.displayName || 'Unknown User'}
                        secondary={
                          profile?.online 
                            ? <Typography component="span" variant="caption" color="success.main">● Online</Typography>
                            : <Typography component="span" variant="caption" color="text.secondary">Offline</Typography>
                        }
                      />
                    </ListItemButton>
                  </ListItem>
                );
              })}
            </List>
          ) : (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography color="text.secondary">
                {isOwnProfile ? 'No contacts yet' : 'No contacts to display'}
              </Typography>
            </Box>
          )}
        </DialogContent>
        
        <DialogActions>
          <Button onClick={() => setContactsDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Profile;