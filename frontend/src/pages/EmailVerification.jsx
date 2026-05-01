import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Paper, TextField, Button, Typography, Box, Alert, CircularProgress } from '@mui/material';
import authService from '../services/authService';

const EmailVerification = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const initialEmail = location.state?.email || '';
  
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState(null);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [codeError, setCodeError] = useState('');
  const [emailError, setEmailError] = useState('');

  const validate = () => {
    let isValid = true;
    
    if (!email) {
      setEmailError('Email is required');
      isValid = false;
    } else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(email)) {
      setEmailError('Invalid email address');
      isValid = false;
    } else {
      setEmailError('');
    }
    
    if (!code) {
      setCodeError('Verification code is required');
      isValid = false;
    } else if (!/^\d{6}$/.test(code)) {
      setCodeError('Code must be exactly 6 digits');
      isValid = false;
    } else {
      setCodeError('');
    }
    
    return isValid;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    
    if (!validate()) return;
    
    setLoading(true);
    try {
      await authService.verifyEmail(email, code);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Invalid verification code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (!email) {
      setEmailError('Email is required');
      return;
    }
    
    if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(email)) {
      setEmailError('Invalid email address');
      return;
    }
    
    setEmailError('');
    setResending(true);
    setError(null);
    setResendSuccess(false);
    
    try {
      await authService.resendVerification(email);
      setResendSuccess(true);
    } catch (err) {
      setError('Failed to resend verification code. Please try again.');
    } finally {
      setResending(false);
    }
  };

  return (
    <Paper 
      elevation={3} 
      sx={{ 
        p: 4, 
        width: '100%',
        maxWidth: 400,
        borderRadius: 4
      }}
    >
      <Typography variant="h5" align="center" gutterBottom>
        Verify Your Email
      </Typography>
      
      {initialEmail ? (
        <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
          We've sent a 6-digit verification code to:
          <br />
          <strong>{initialEmail}</strong>
        </Typography>
      ) : (
        <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
          Enter your email and the 6-digit verification code
        </Typography>
      )}
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      {resendSuccess && (
        <Alert severity="success" sx={{ mb: 2 }}>
          A new verification code has been sent!
        </Alert>
      )}
      
      <form onSubmit={handleSubmit}>
        {!initialEmail && (
          <TextField
            label="Email"
            type="email"
            fullWidth
            margin="normal"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={!!emailError}
            helperText={emailError}
            sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: '20px 8px 20px 8px',
              }
            }}
          />
        )}
        
        <TextField
          label="Verification Code"
          placeholder="000000"
          fullWidth
          margin="normal"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          error={!!codeError}
          helperText={codeError}
          inputProps={{ maxLength: 6 }}
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: '20px 8px 20px 8px',
            }
          }}
        />
        
        <Button 
          type="submit" 
          variant="contained"
          size="large"
          fullWidth
          sx={{ 
            mt: 3,
            borderRadius: '20px 8px 20px 8px',
          }}
          disabled={loading}
        >
          {loading ? <CircularProgress size={24} /> : 'Verify Email'}
        </Button>
      </form>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Didn't receive the code?{' '}
          <Button 
            color="primary" 
            onClick={handleResendCode}
            disabled={resending}
            sx={{ textTransform: 'none' }}
          >
            {resending ? 'Sending...' : 'Resend Code'}
          </Button>
        </Typography>
      </Box>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Button 
          color="primary" 
          onClick={() => navigate('/login')}
          sx={{ textTransform: 'none' }}
        >
          Back to Login
        </Button>
      </Box>
    </Paper>
  );
};

export default EmailVerification;