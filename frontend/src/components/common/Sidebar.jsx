import { Drawer, Toolbar, Box, Typography } from '@mui/material';

const drawerWidth = 240;

const Sidebar = () => {
  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
      }}
    >
      <Toolbar /> {/* Spacer for header */}
      <Box sx={{ p: 2 }}>
        <Typography variant="body1" color="text.secondary">
          No chats yet
        </Typography>
      </Box>
    </Drawer>
  );
};

export default Sidebar;