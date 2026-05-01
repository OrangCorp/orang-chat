import { useState } from 'react';
import {
  Box, Paper, Typography, Button, Alert, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField
} from '@mui/material';
import { Mail as MailIcon, Lock as LockIcon } from '@mui/icons-material';
import authService from '../services/authService';
import { useAuth } from '../context/AuthContext';

const Settings = () => {
  const { user } = useAuth();
  
  // Forgot password dialog state
  const [forgotDialogOpen, setForgotDialogOpen] = useState(false);
  const [forgotEmail, setForgotEmail] = useState(user?.email || '');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState(null);
  const [forgotSuccess, setForgotSuccess] = useState(false);

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