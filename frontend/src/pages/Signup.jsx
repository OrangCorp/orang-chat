import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { Paper, TextField, Button, Typography, Box } from '@mui/material';
import { useAuth } from '../context/AuthContext';

const Signup = () => {
  const { register, handleSubmit, formState: { errors } } = useForm();
  const { signup, loading } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data) => {
    const success = await signup(data.email, data.password);
    if (success) navigate('/');
  };

  return (
    <Paper 
      elevation={3} 
      sx={{ 
        p: 4, 
        width: '100%',
        borderRadius: 4 // MUI spacing unit (4 = 32px)
        // Or use a specific value:
        // borderRadius: '20px'
        // Or extra rounded:
        // borderRadius: 8 // 64px - very rounded!
      }}
    >
      <Typography variant="h4" align="center" gutterBottom>
        Sign Up
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
              borderRadius: '20px 8px 20px 8px', // top-left, top-right, bottom-right, bottom-left
              // This makes top corners very rounded, bottom corners slightly rounded
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
              borderRadius: '20px 8px 20px 8px', // top-left, top-right, bottom-right, bottom-left
              // This makes top corners very rounded, bottom corners slightly rounded
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
            display: 'block',     // Makes margin auto work
            mx: 'auto',           // Horizontal margin auto = centered
            px: 4,                // Optional: add some horizontal padding
            minWidth: '100px'     // Optional: set a minimum width
          }}
          disabled={loading}
        >
          {loading ? 'Signing in...' : 'Sign Up'}
        </Button>
      </form>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2">
          Already have an account?{' '}
          <Button 
            color="primary" 
            onClick={() => navigate('/login')}
            sx={{ textTransform: 'none' }}
          >
            Log In
          </Button>
        </Typography>
      </Box>
    </Paper>
  );
};

export default Signup;