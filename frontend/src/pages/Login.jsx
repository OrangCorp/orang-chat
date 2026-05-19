import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Paper, TextField, Button, Typography, Box, Alert } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [formData, setFormData] = useState({ email: '', password: '' });

  const validate = () => {
    const newErrors = {};
    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.password) newErrors.password = 'Password is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setApiError(null);
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError(null);
    
    if (!validate()) return;

    try {
      const success = await login(formData.email, formData.password);
      if (success) {
        navigate('/');
      }
    } catch (err) {
      // Only show generic message, never log to console
      setApiError('Login failed. Please try again.');
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 4, width: '100%', borderRadius: 4 }}>
      <Typography variant="h4" align="center" gutterBottom>
        Login
      </Typography>
      
      {apiError && <Alert severity="error" sx={{ mb: 2 }}>{apiError}</Alert>}
      
      <form onSubmit={onSubmit}>
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
        
        <Button 
          type="submit" 
          variant="contained"
          size="large"
          sx={{ 
            mt: 3,
            borderRadius: '20px 8px 20px 8px',
            display: 'block',
            mx: 'auto',
            px: 4,
            minWidth: '100px'
          }}
          disabled={loading}
        >
          {loading ? 'Logging in...' : 'Login'}
        </Button>
      </form>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2">
          Don't have an account?{' '}
          <Button color="primary" onClick={() => navigate('/signup')} sx={{ textTransform: 'none' }}>
            Sign up
          </Button>
        </Typography>
      </Box>
      
      <Box sx={{ mt: 1, textAlign: 'center' }}>
        <Button color="secondary" onClick={() => navigate('/verify-email')} sx={{ textTransform: 'none' }}>
          Verify Email
        </Button>
      </Box>
    </Paper>
  );
};

export default Login;