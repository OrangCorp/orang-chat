import { Box, Typography, Paper } from '@mui/material';

const Home = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography variant="h4" gutterBottom>
          Welcome to Orang Chat
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Select a chat or start a new conversation
        </Typography>
      </Paper>
    </Box>
  );
};

export default Home;