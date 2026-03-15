import { AppBar, Toolbar, Box, Button } from '@mui/material';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import logoImg from '../../assets/logo.png';

const Header = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar>
        <Box sx={{ flexGrow: 1 }}>
          <img 
            src={logoImg} 
            alt="Logo" 
            style={{ height: '40px' }}
          />
        </Box>
        <Button 
          variant="contained"
          color="secondary"
          onClick={handleLogout}
          sx={{ 
            borderRadius: '20px 8px 20px 8px',
            px: 3,
            textTransform: 'none',
            fontWeight: 'bold'
          }}
        >
          Logout
        </Button>
      </Toolbar>
    </AppBar>
  );
};

export default Header;