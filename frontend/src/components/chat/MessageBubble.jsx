import { Box, Paper, Typography } from '@mui/material';

const MessageBubble = ({ message, isOwn, senderName }) => {
  const isSystem = message.type === 'SYSTEM';

  return (
    <Box sx={{ display: 'flex', justifyContent: isOwn ? 'flex-end' : 'flex-start', mb: 2 }}>
      {!isOwn && !isSystem && (
        <Typography variant="caption" sx={{ mr: 1, alignSelf: 'flex-end', color: 'text.secondary' }}>
          {senderName}
        </Typography>
      )}
      <Paper
        elevation={1}
        sx={{
          p: 1.5,
          maxWidth: '70%',
          bgcolor: isSystem ? 'grey.100' : (isOwn ? 'primary.light' : 'white'),
          color: isSystem ? 'text.secondary' : (isOwn ? 'white' : 'text.primary'),
          borderRadius: isOwn ? '20px 8px 8px 20px' : '8px 20px 20px 8px',
          wordBreak: 'break-word'
        }}
      >
        <Typography variant="body2">{message.content}</Typography>
        <Typography variant="caption" sx={{ display: 'block', textAlign: 'right', mt: 0.5, opacity: 0.8 }}>
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </Typography>
      </Paper>
    </Box>
  );
};

export default MessageBubble;