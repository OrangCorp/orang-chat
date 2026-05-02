import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Paper, TextField, Button, Typography, Box, Alert, CircularProgress } from '@mui/material';
import authService from '../services/authService';

const Signup = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    displayName: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(null);

  const validate = () => {
    const newErrors = {};
    
    if (!formData.displayName || formData.displayName.length < 2) {
      newErrors.displayName = 'Name must be at least 2 characters';
    }
    
    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(formData.email)) {
      newErrors.email = 'Invalid email address';
    }
    
    if (!formData.password || formData.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }
    
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError(null);
    
    if (!validate()) return;
    
    setLoading(true);
    try {
      const response = await authService.register(
        formData.email, 
        formData.password, 
        formData.displayName
      );
      navigate('/verify-email', { state: { email: response.email } });
    } catch (err) {
      setApiError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 4, width: '100%', borderRadius: 4 }}>
      <Typography variant="h4" align="center" gutterBottom>
        Sign Up
      </Typography>
      
      {apiError && <Alert severity="error" sx={{ mb: 2 }}>{apiError}</Alert>}
      
      <form onSubmit={handleSubmit}>
        <TextField
          name="displayName"
          label="Display Name"
          fullWidth
          margin="normal"
          value={formData.displayName}
          onChange={handleChange}
          error={!!errors.displayName}
          helperText={errors.displayName}
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
        />
        
        <TextField
          name="email"
          label="Email"
          type="email"
          fullWidth
          margin="normal"
          value={formData.email}
          onChange={handleChange}
          error={!!errors.email}
          helperText={errors.email}
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
        />
        
        <TextField
          name="password"
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          value={formData.password}
          onChange={handleChange}
          error={!!errors.password}
          helperText={errors.password}
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
        />
        
        <TextField
          name="confirmPassword"
          label="Confirm Password"
          type="password"
          fullWidth
          margin="normal"
          value={formData.confirmPassword}
          onChange={handleChange}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword}
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '20px 8px 20px 8px' } }}
        />
        
        <Button 
          type="submit" 
          variant="contained"
          size="large"
          disabled={loading}
          sx={{ 
            mt: 3,
            borderRadius: '20px 8px 20px 8px',
            display: 'block',
            mx: 'auto',
            px: 4,
            minWidth: '120px'
          }}
        >
          {loading ? <CircularProgress size={24} /> : 'Sign Up'}
        </Button>
      </form>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2">
          Already have an account?{' '}
          <Button color="primary" onClick={() => navigate('/login')} sx={{ textTransform: 'none' }}>
            Log In
          </Button>
        </Typography>
      </Box>
      
      <Box sx={{ mt: 1, textAlign: 'center' }}>
        <Button color="secondary" onClick={() => navigate('/verify-email')} sx={{ textTransform: 'none' }}>
          Already have a code? Verify Email
        </Button>
      </Box>
    </Paper>
  );
};

export default Signup;