import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Paper, TextField, Button, Typography, Box } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const { register, handleSubmit, formState: { errors } } = useForm();
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data) => {
    const success = await login(data.email, data.password);
    if (success) navigate('/');
  };

  return (
    <Paper 
      elevation={3} 
      sx={{ 
        p: 4, 
        width: '100%',
        borderRadius: 4
      }}
    >
      <Typography variant="h4" align="center" gutterBottom>
        Login
      </Typography>
      
      <form onSubmit={handleSubmit(onSubmit)}>
        <TextField
          {...register('email', { required: 'Email is required' })}
          label="Email"
          type="email"
          fullWidth
          margin="normal"
          error={!!errors.email}
          helperText={errors.email?.message}
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: '20px 8px 20px 8px',
            }
          }}
        />
        
        <TextField
          {...register('password', { required: 'Password is required' })}
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          error={!!errors.password}
          helperText={errors.password?.message}
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
          Don't have an account? :C{' '}
          <Button 
            color="primary" 
            onClick={() => navigate('/signup')}
            sx={{ textTransform: 'none' }}
          >
            Sign up
          </Button>
        </Typography>
      </Box>
      
      <Box sx={{ mt: 1, textAlign: 'center' }}>
        <Typography variant="body2">
          <Button 
            color="secondary" 
            onClick={() => navigate('/verify-email')}
            sx={{ textTransform: 'none' }}
          >
            Verify Email
          </Button>
        </Typography>
      </Box>
    </Paper>
  );
};

export default Login;