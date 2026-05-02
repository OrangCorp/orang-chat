import { Box, Typography, Button, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import logoImg from '../assets/logo.png';

const NotFound = () => {
  const navigate = useNavigate();

  return (
    <Box 
      sx={{ 
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: '#f5f5f5',
        p: 3
      }}
    >
      <Paper 
        elevation={3}
        sx={{
          maxWidth: 500,
          width: '100%',
          p: 5,
          borderRadius: 4,
          textAlign: 'center'
        }}
      >
        <Box 
          component="img"
          src={logoImg}
          alt="Logo"
          sx={{
            width: 120,
            height: 'auto',
            mb: 3
          }}
        />
        
        <Typography 
          variant="h1" 
          sx={{ 
            fontSize: '6rem',
            fontWeight: 'bold',
            color: 'primary.main',
            lineHeight: 1,
            mb: 2
          }}
        >
          404
        </Typography>
        
        <Typography 
          variant="h5" 
          sx={{ 
            fontWeight: 'medium',
            mb: 2
          }}
        >
          Page Not Found
        </Typography>
        
        <Typography 
          variant="body1" 
          color="text.secondary"
          sx={{ mb: 4 }}
        >
          Oops! The page you're looking for doesn't exist or has been moved.
        </Typography>
        
        <Button
          variant="contained"
          size="large"
          startIcon={<HomeIcon />}
          onClick={() => navigate('/')}
          sx={{
            borderRadius: '20px 8px 20px 8px',
            px: 4,
            py: 1
          }}
        >
          Back to Homepage
        </Button>
      </Paper>
    </Box>
  );
};

export default NotFound;