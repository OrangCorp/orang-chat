import { useState, useEffect } from 'react';
import {
  Box, Paper, Typography, Button, Alert, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Switch, FormControlLabel, Divider, Chip
} from '@mui/material';
import { 
  Mail as MailIcon, 
  Lock as LockIcon, 
  Notifications as NotificationsIcon,
  NotificationsOff as NotificationsOffIcon 
} from '@mui/icons-material';
import authService from '../services/authService';
import notificationService from '../services/notificationService';
import { useAuth } from '../context/AuthContext';

const Settings = () => {
  const { user } = useAuth();
  
  // Forgot password dialog state
  const [forgotDialogOpen, setForgotDialogOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState(user?.email || '');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState(null);
  const [forgotSuccess, setForgotSuccess] = useState(false);

  // Push notifications state
  const [pushEnabled, setPushEnabled] = useState(false);
  const [pushLoading, setPushLoading] = useState(false);
  const [pushError, setPushError] = useState(null);
  const [pushStatus, setPushStatus] = useState('disabled'); // 'disabled' | 'enabled' | 'unsupported'

  // Check push notification status on mount
  useEffect(() => {
    checkPushStatus();
  }, []);

  const checkPushStatus = async () => {
    if (!('Notification' in window) || !('serviceWorker' in navigator) || !('PushManager' in window)) {
      setPushStatus('unsupported');
      return;
    }

    try {
      const permission = Notification.permission;
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      
      if (permission === 'granted' && subscription) {
        setPushEnabled(true);
        setPushStatus('enabled');
      } else if (permission === 'denied') {
        setPushEnabled(false);
        setPushStatus('disabled');
      } else {
        setPushEnabled(false);
        setPushStatus('disabled');
      }
    } catch (err) {
      console.debug('Could not check push status:', err);
      setPushStatus('unsupported');
    }
  };

  const handlePushToggle = async (e) => {
    const enable = e.target.checked;
    setPushLoading(true);
    setPushError(null);

    try {
      if (enable) {
        // Request permission and subscribe
        const granted = await notificationService.requestPermission();
        if (!granted) {
          setPushError('Notification permission was denied. Please enable it in your browser settings.');
          setPushEnabled(false);
          return;
        }
        await notificationService.subscribeToPush();
        setPushEnabled(true);
        setPushStatus('enabled');
      } else {
        // Unsubscribe from push
        await notificationService.unsubscribeFromPush();
        setPushEnabled(false);
        setPushStatus('disabled');
      }
    } catch (err) {
      console.error('Failed to toggle push notifications:', err);
      setPushError(err.message || 'Failed to update notification settings');
      setPushEnabled(!enable);
    } finally {
      setPushLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    setForgotError(null);
    setForgotSuccess(false);
    
    if (!forgotEmail) {
      setForgotError('Email is required');
      return;
    }
    
    if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(forgotEmail)) {
      setForgotError('Invalid email address');
      return;
    }
    
    setForgotLoading(true);
    try {
      await authService.forgotPassword(forgotEmail);
      setForgotSuccess(true);
      setTimeout(() => {
        setForgotDialogOpen(false);
        setForgotSuccess(false);
        setForgotEmail('');
      }, 2000);
    } catch (err) {
      setForgotError('Failed to send reset email. Please try again.');
    } finally {
      setForgotLoading(false);
    }
  };

  return (
    <Box sx={{ p: 3, maxWidth: 600, mx: 'auto' }}>
      <Paper elevation={3} sx={{ borderRadius: 4, overflow: 'hidden' }}>
        <Box sx={{ p: 3, bgcolor: 'primary.main', color: 'white' }}>
          <Typography variant="h5" fontWeight="bold">Settings</Typography>
          <Typography variant="body2" sx={{ opacity: 0.9 }}>
            Manage your account settings
          </Typography>
        </Box>
        
        <Box sx={{ p: 3 }}>
          {/* Notifications Section */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <NotificationsIcon color="primary" />
            <Typography variant="h6">Notifications</Typography>
          </Box>
          
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Receive push notifications for new messages, reactions, mentions, and contact requests.
            You can disable these at any time.
          </Typography>
          
          {pushStatus === 'unsupported' ? (
            <Alert severity="info" sx={{ mb: 2 }}>
              Push notifications are not supported in your browser. Please use a modern browser like Chrome or Firefox.
            </Alert>
          ) : (
            <>
              {pushError && (
                <Alert severity="error" sx={{ mb: 2 }}>{pushError}</Alert>
              )}
              
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                <Box>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={pushEnabled}
                        onChange={handlePushToggle}
                        disabled={pushLoading || Notification.permission === 'denied'}
                      />
                    }
                    label={
                      <Typography variant="body1">
                        {pushLoading ? 'Updating...' : pushEnabled ? 'Push notifications enabled' : 'Push notifications disabled'}
                      </Typography>
                    }
                  />
                  <Typography variant="caption" color="text.secondary" sx={{ ml: 4, display: 'block' }}>
                    {pushEnabled 
                      ? 'You will receive notifications for new messages, reactions, and contact requests.'
                      : Notification.permission === 'denied'
                        ? 'Notifications are blocked in your browser settings. Please update your browser permissions to enable them.'
                        : 'Enable to receive notifications even when you\'re not actively using the app.'
                    }
                  </Typography>
                </Box>
                <Chip 
                  icon={pushEnabled ? <NotificationsIcon /> : <NotificationsOffIcon />}
                  label={pushEnabled ? 'Active' : 'Inactive'}
                  color={pushEnabled ? 'success' : 'default'}
                  size="small"
                  variant="outlined"
                />
              </Box>
            </>
          )}

          <Divider sx={{ my: 3 }} />

          {/* Security Section */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <LockIcon color="primary" />
            <Typography variant="h6">Security</Typography>
          </Box>
          
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Need to change your password? We'll send you an email with a link to reset it.
          </Typography>
          
          <Button
            variant="outlined"
            startIcon={<MailIcon />}
            onClick={() => setForgotDialogOpen(true)}
            sx={{ borderRadius: '20px 8px 20px 8px' }}
          >
            Send Password Reset Email
          </Button>
        </Box>
      </Paper>
      
      {/* Forgot Password Dialog */}
      <Dialog 
        open={forgotDialogOpen} 
        onClose={() => !forgotLoading && setForgotDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Reset Password</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Enter your email address and we'll send you a link to reset your password.
          </Typography>
          
          {forgotError && <Alert severity="error" sx={{ mb: 2 }}>{forgotError}</Alert>}
          {forgotSuccess && <Alert severity="success" sx={{ mb: 2 }}>Reset email sent! Check your inbox.</Alert>}
          
          <TextField
            label="Email"
            type="email"
            fullWidth
            margin="normal"
            value={forgotEmail}
            onChange={(e) => setForgotEmail(e.target.value)}
            disabled={forgotLoading || forgotSuccess}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button 
            onClick={() => setForgotDialogOpen(false)}
            disabled={forgotLoading}
          >
            Cancel
          </Button>
          <Button 
            variant="contained"
            onClick={handleForgotPassword}
            disabled={forgotLoading || forgotSuccess}
            sx={{ borderRadius: '20px 8px 20px 8px' }}
          >
            {forgotLoading ? <CircularProgress size={24} /> : 'Send'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Settings;