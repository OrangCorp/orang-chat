import React, { useState } from 'react';
import {
  Box,
  Paper,
  TextField,
  IconButton,
  Avatar,
  Typography,
  AppBar,
  Toolbar,
  Badge,
  InputAdornment,
} from '@mui/material';
import {
  Send as SendIcon,
  AttachFile as AttachFileIcon,
  EmojiEmotions as EmojiIcon,
  MoreVert as MoreVertIcon,
} from '@mui/icons-material';

const ChatInterface = () => {
  const [messages, setMessages] = useState([
    {
      id: 1,
      text: "Hey! How's your project going?",
      sender: 'Alice',
      timestamp: '10:30 AM',
      isMe: false,
      avatar: 'A',
    },
    {
      id: 2,
      text: "It's going great! Just working on this chat interface.",
      sender: 'Me',
      timestamp: '10:31 AM',
      isMe: true,
      avatar: 'Me',
    },
    {
      id: 3,
      text: 'Awesome! Are you using Material-UI?',
      sender: 'Alice',
      timestamp: '10:32 AM',
      isMe: false,
      avatar: 'A',
    },
  ]);

  const [newMessage, setNewMessage] = useState('');

  const handleSendMessage = () => {
    if (newMessage.trim()) {
      const message = {
        id: messages.length + 1,
        text: newMessage,
        sender: 'Me',
        timestamp: new Date().toLocaleTimeString([], { 
          hour: '2-digit', 
          minute: '2-digit' 
        }),
        isMe: true,
        avatar: 'Me',
      };
      setMessages([...messages, message]);
      setNewMessage('');
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <Box sx={{ 
      height: '100vh', 
      display: 'flex', 
      flexDirection: 'column',
      maxWidth: '800px',
      margin: '0 auto',
      border: '1px solid #e0e0e0',
    }}>
      {/* Header */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <Badge
            color="success"
            variant="dot"
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
          >
            <Avatar sx={{ bgcolor: 'primary.main' }}>A</Avatar>
          </Badge>
          <Box sx={{ ml: 2, flex: 1 }}>
            <Typography variant="subtitle1">Alice Johnson</Typography>
            <Typography variant="caption" color="text.secondary">
              Online
            </Typography>
          </Box>
          <IconButton>
            <MoreVertIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      {/* Messages Area */}
      <Box sx={{ 
        flex: 1, 
        overflow: 'auto', 
        p: 2,
        bgcolor: '#f5f5f5',
        display: 'flex',
        flexDirection: 'column',
      }}>
        {messages.map((message) => (
          <Box
            key={message.id}
            sx={{
              display: 'flex',
              justifyContent: message.isMe ? 'flex-end' : 'flex-start',
              mb: 2,
            }}
          >
            <Box
              sx={{
                display: 'flex',
                flexDirection: message.isMe ? 'row-reverse' : 'row',
                alignItems: 'flex-end',
                maxWidth: '70%',
              }}
            >
              {!message.isMe && (
                <Avatar 
                  sx={{ 
                    width: 32, 
                    height: 32, 
                    mr: 1,
                    bgcolor: 'secondary.main',
                    fontSize: '0.875rem',
                  }}
                >
                  {message.avatar}
                </Avatar>
              )}
              <Paper
                elevation={1}
                sx={{
                  p: 1.5,
                  bgcolor: message.isMe ? 'primary.main' : 'background.paper',
                  color: message.isMe ? 'white' : 'text.primary',
                  borderRadius: 2,
                  borderTopRightRadius: message.isMe ? 2 : 16,
                  borderTopLeftRadius: !message.isMe ? 2 : 16,
                  borderBottomRightRadius: message.isMe ? 4 : 16,
                  borderBottomLeftRadius: !message.isMe ? 4 : 16,
                }}
              >
                <Typography variant="body2">{message.text}</Typography>
                <Typography 
                  variant="caption" 
                  sx={{ 
                    display: 'block',
                    textAlign: 'right',
                    mt: 0.5,
                    color: message.isMe ? 'rgba(255,255,255,0.7)' : 'text.disabled',
                  }}
                >
                  {message.timestamp}
                </Typography>
              </Paper>
            </Box>
          </Box>
        ))}
      </Box>

      {/* Input Area */}
      <Paper elevation={3} sx={{ p: 2 }}>
        <TextField
          fullWidth
          multiline
          maxRows={4}
          placeholder="Type a message..."
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          variant="outlined"
          size="small"
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <IconButton size="small" edge="start">
                  <AttachFileIcon />
                </IconButton>
                <IconButton size="small">
                  <EmojiIcon />
                </IconButton>
              </InputAdornment>
            ),
            endAdornment: (
              <InputAdornment position="end">
                <IconButton 
                  color="primary" 
                  onClick={handleSendMessage}
                  disabled={!newMessage.trim()}
                  edge="end"
                >
                  <SendIcon />
                </IconButton>
              </InputAdornment>
            ),
            sx: {
              '& .MuiInputBase-input': {
                pr: 1,
              },
            },
          }}
        />
      </Paper>
    </Box>
  );
};

export default ChatInterface;