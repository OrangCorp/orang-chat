import { Outlet } from 'react-router-dom';
import { Container, Box } from '@mui/material';
import loginBg from '../assets/login-bg.webp';

const AuthLayout = () => {
  return (
    <Box sx={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundImage: `url(${loginBg})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'relative'
    }}>
      <Container 
        maxWidth="xs"
        sx={{ 
          position: 'relative', 
          zIndex: 2 
        }}
      >
        {/* Remove the inner Box - Container already centers its children */}
        <Outlet />
      </Container>
    </Box>
  );
};

export default AuthLayout;