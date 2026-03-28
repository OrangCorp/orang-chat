// MainLayout.jsx
import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';
import Header from '../components/common/Header';
import Sidebar from '../components/common/Sidebar';

const MainLayout = () => {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Header />
      <Sidebar />
      <Box 
        component="main" 
        sx={{ 
          flexGrow: 1, 
          mt: '64px', // Match header height
          overflow: 'hidden' // Prevent unnecessary scrolling
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
};

export default MainLayout;