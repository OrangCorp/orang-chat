import { Box, Paper, Typography, Avatar, IconButton } from '@mui/material';

const MessageBubble = ({ message, isOwn, senderName, senderAvatar, time, onAvatarClick }) => {
  if (!message) return null;
  
  const isSystem = message.type === 'SYSTEM';
  const displayTime = time || 'Just now';

  // For system messages, just show a centered message
  if (isSystem) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
        <Paper
          elevation={0}
          sx={{
            p: 1,
            bgcolor: 'grey.100',
            color: 'text.secondary',
            borderRadius: '16px',
            maxWidth: '80%'
          }}
        >
          <Typography variant="caption">{message.content}</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ 
      display: 'flex', 
      justifyContent: isOwn ? 'flex-end' : 'flex-start',
      mb: 2
    }}>
      {/* Avatar on the left (only for received messages) */}
      {!isOwn && (
        <IconButton 
          onClick={() => onAvatarClick?.()} 
          sx={{ p: 0, mr: 1, alignSelf: 'flex-end' }}
          size="small"
        >
          <Avatar 
            src={senderAvatar} 
            sx={{ width: 32, height: 32, cursor: 'pointer' }}
          >
            {senderName?.charAt(0)?.toUpperCase()}
          </Avatar>
        </IconButton>
      )}
      
      {/* Message content */}
      <Box sx={{ 
        display: 'flex', 
        flexDirection: 'column',
        alignItems: isOwn ? 'flex-end' : 'flex-start',
        maxWidth: '70%'
      }}>
        <Paper
          elevation={1}
          sx={{
            p: 1.5,
            bgcolor: isOwn ? 'primary.light' : 'secondary.light',
            color: 'white',
            borderRadius: isOwn 
              ? '18px 4px 18px 18px'  // Own messages: asymmetric corners
              : '4px 18px 18px 18px', // Others: asymmetric corners
            wordBreak: 'break-word'
          }}
        >
          <Typography variant="body2">{message.content || 'Empty message'}</Typography>
        </Paper>
        
        {/* Name and time on the same line */}
        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center',
          gap: 1,
          mt: 0.5,
          mx: 1
        }}>
          {/* Name (only for received messages) */}
          {!isOwn && (
            <Typography 
              variant="caption" 
              sx={{ 
                color: 'text.secondary',
                fontSize: '0.65rem',
                cursor: 'pointer',
                '&:hover': {
                  textDecoration: 'underline'
                }
              }}
              onClick={() => onAvatarClick?.()}
            >
              {senderName}
            </Typography>
          )}
          
          {/* Time */}
          <Typography 
            variant="caption" 
            sx={{ 
              color: 'text.secondary',
              fontSize: '0.65rem'
            }}
          >
            {displayTime}
          </Typography>
        </Box>
      </Box>
      
      {/* Empty spacer for own messages to maintain alignment */}
      {isOwn && <Box sx={{ width: 40 }} />}
    </Box>
  );
};

export default MessageBubble;