import { useState, useEffect } from 'react';
import { Box, Paper, Typography, Avatar, IconButton } from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import attachmentService from '../../services/attachmentService';

const MessageBubble = ({ message, isOwn, senderName, senderAvatar, time, onAvatarClick, highlight }) => {
  if (!message) return null;
  
  const isSystem = message.type === 'SYSTEM';
  const displayTime = time || 'Just now';
  const attachments = message.attachments || [];

  if (isSystem) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
        <Paper elevation={1} sx={{ bgcolor: highlight ? 'warning.light' : (isOwn ? 'primary.light' : 'secondary.light'), p: 1.5, color: 'text.secondary', borderRadius: '16px', maxWidth: '80%' }}>
          <Typography variant="caption">{message.content}</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: isOwn ? 'flex-end' : 'flex-start', mb: 2 }}>
      {!isOwn && (
        <IconButton onClick={() => onAvatarClick?.()} sx={{ p: 0, mr: 1, alignSelf: 'flex-end' }} size="small">
          <Avatar src={senderAvatar} sx={{ width: 32, height: 32, cursor: 'pointer' }}>
            {senderName?.charAt(0)?.toUpperCase()}
          </Avatar>
        </IconButton>
      )}
      
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isOwn ? 'flex-end' : 'flex-start', maxWidth: '70%' }}>
        <Paper elevation={1} sx={{ p: 1.5, bgcolor: isOwn ? 'primary.light' : 'secondary.light', color: 'white', borderRadius: isOwn ? '18px 4px 18px 18px' : '4px 18px 18px 18px', wordBreak: 'break-word' }}>
          {message.content && <Typography variant="body2">{message.content}</Typography>}
          
          {attachments.length > 0 && (
            <Box sx={{ mt: message.content ? 1 : 0 }}>
              {attachments.map((att, idx) => (
                <AttachmentItem key={att.id || idx} attachment={att} />
              ))}
            </Box>
          )}
        </Paper>
        
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5, mx: 1 }}>
          {!isOwn && (
            <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.65rem', cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }} onClick={() => onAvatarClick?.()}>
              {senderName}
            </Typography>
          )}
          <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.65rem' }}>
            {displayTime}
          </Typography>
        </Box>
      </Box>
      
      {isOwn && <Box sx={{ width: 40 }} />}
    </Box>
  );
};

// Separate component for attachment to handle async download URL
const AttachmentItem = ({ attachment }) => {
  const [downloadUrl, setDownloadUrl] = useState(null);
  
  useEffect(() => {
    if (attachment.id && !attachment.uploading) {
      // The download URL is just a path, not a promise
      setDownloadUrl(attachmentService.getDownloadUrl(attachment.id));
    }
  }, [attachment.id, attachment.uploading]);

  if (attachment.uploading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5, opacity: 0.6 }}>
        <InsertDriveFileIcon fontSize="small" />
        <Typography variant="caption" sx={{ fontStyle: 'italic' }}>
          {attachment.fileName} (uploading...)
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
      {attachment.thumbnailAvailable ? (
        <img 
          src={attachmentService.getThumbnailUrl(attachment.id)} 
          alt={attachment.fileName}
          style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 4 }}
        />
      ) : (
        <InsertDriveFileIcon fontSize="small" />
      )}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="caption" sx={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'white' }}>
          {attachment.fileName}
        </Typography>
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.65rem' }}>
          {attachment.fileSize ? `${(attachment.fileSize / 1024).toFixed(1)} KB` : ''}
        </Typography>
      </Box>
      {downloadUrl && (
        <IconButton 
          size="small" 
          component="a" 
          href={downloadUrl} 
          target="_blank"
          sx={{ color: 'white' }}
        >
          <DownloadIcon fontSize="small" />
        </IconButton>
      )}
    </Box>
  );
};

export default MessageBubble;