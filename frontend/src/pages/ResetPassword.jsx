import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Paper, TextField, Button, Typography, Box, Alert, CircularProgress } from '@mui/material';
import { Lock as LockIcon } from '@mui/icons-material';
import authService from '../services/authService';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');
  
  const [isValidating, setIsValidating] = useState(true);
  const [isValid, setIsValid] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [passwordError, setPasswordError] = useState('');

  useEffect(() => {
    const validateToken = async () => {
      if (!token) {
        setIsValid(false);
        setIsValidating(false);
        return;
      }
      
      try {
        const valid = await authService.validateResetToken(token);
        setIsValid(valid);
      } catch (err) {
        setIsValid(false);
      } finally {
        setIsValidating(false);
      }
    };
    
    validateToken();
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setPasswordError('');
    
    if (newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters');
      return;
    }
    
    if (newPassword !== confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }
    
    setLoading(true);
    try {
      await authService.resetPassword(token, newPassword);
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      setError(err.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  if (isValidating) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!isValid) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', p: 3 }}>
        <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%', borderRadius: 4, textAlign: 'center' }}>
          <Typography variant="h5" color="error" gutterBottom>
            Invalid or Expired Link
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            This password reset link is invalid or has expired.
          </Typography>
          <Button 
            variant="contained" 
            onClick={() => navigate('/login')}
            sx={{ borderRadius: '20px 8px 20px 8px' }}
          >
            Go to Login
          </Button>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', p: 3 }}>
      <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%', borderRadius: 4 }}>
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Box sx={{ 
            width: 60, 
            height: 60, 
            borderRadius: '50%', 
            bgcolor: 'primary.main', 
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mx: 'auto',
            mb: 2
          }}>
            <LockIcon sx={{ color: 'white', fontSize: 30 }} />
          </Box>
          <Typography variant="h5" gutterBottom>
            Reset Password
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Enter your new password below
          </Typography>
        </Box>
        
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 2 }}>Password reset successfully! Redirecting to login...</Alert>}
        {passwordError && <Alert severity="error" sx={{ mb: 2 }}>{passwordError}</Alert>}
        
        <form onSubmit={handleSubmit}>
          <TextField
            label="New Password"
            type="password"
            fullWidth
            margin="normal"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={success}
            helperText="At least 8 characters"
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
          />
          
          <TextField
            label="Confirm New Password"
            type="password"
            fullWidth
            margin="normal"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={success}
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
          />
          
          <Button
            type="submit"
            variant="contained"
            fullWidth
            size="large"
            disabled={loading || success}
            sx={{ 
              mt: 3,
              borderRadius: '20px 8px 20px 8px',
            }}
          >
            {loading ? <CircularProgress size={24} /> : 'Reset Password'}
          </Button>
        </form>
      </Paper>
    </Box>
  );
};

export default ResetPassword;