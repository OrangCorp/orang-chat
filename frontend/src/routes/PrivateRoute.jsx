import { Navigate } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import authService from '../services/authService';
import { useState, useEffect } from 'react';

const PrivateRoute = ({ children }) => {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    // Wait for auth service to attempt authentication
    const checkReady = () => {
      if (authService.attemptedAuth) {
        setReady(true);
      } else {
        setTimeout(checkReady, 100);
      }
    };
    checkReady();
  }, []);

  if (!ready) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return authService.isAuthenticated ? children : <Navigate to="/login" replace />;
};

export default PrivateRoute;