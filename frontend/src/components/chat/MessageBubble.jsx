import { useState, useEffect, useRef } from 'react';
import { Box, Paper, Typography, Avatar, IconButton, TextField, Button, Menu, MenuItem, Dialog, DialogTitle, List, ListItem, ListItemAvatar, ListItemText } from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddReactionIcon from '@mui/icons-material/AddReaction';
import attachmentService from '../../services/attachmentService';
import {
  Videocam as VideocamIcon,
  PictureAsPdf as PictureAsPdfIcon,
  Description as DescriptionIcon,
  Article as ArticleIcon,
  Image as ImageIcon,
} from '@mui/icons-material';

const THUMBNAILS_ENABLED = true;

// Reaction emoji mapping
const REACTION_EMOJI = {
  LIKE: '👍',
  HEART: '❤️',
  LAUGH: '😂',
  WOW: '😮',
  SAD: '😢',
  ANGRY: '😠',
  ORANG: '🍊',
};

const MessageBubble = ({
  message,
  isOwn,
  senderName,
  senderAvatar,
  time,
  onAvatarClick,
  highlight,
  onEdit,
  onDelete,
  onReaction,
  participants,
}) => {
  const [editing, setEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content);
  const [reactionMenuAnchor, setReactionMenuAnchor] = useState(null);
  const [reactionsDialogOpen, setReactionsDialogOpen] = useState(false);

  const attachments = message.attachments || [];
  const reactionCounts = message.reactionCounts || {};
  const totalReactions = Object.values(reactionCounts).reduce((sum, count) => sum + count, 0);
  const reactionEntries = Object.entries(reactionCounts).filter(([, count]) => count > 0);
  const shownReactions = reactionEntries.slice(0, 3);
  const remainingCount = reactionEntries.length - 3;
  const reactionList = Array.isArray(message.reactions) ? message.reactions : [];

  const startEdit = () => setEditing(true);
  const saveEdit = () => {
    if (editContent.trim() && editContent !== message.content) {
      onEdit?.(message.id, editContent.trim());
    }
    setEditing(false);
  };
  const cancelEdit = () => setEditing(false);

  const handleDelete = () => {
    if (window.confirm('Delete this message?')) onDelete?.(message.id);
  };

  const handleReactionSelect = (type) => {
    onReaction?.(message.id, type);
    setReactionMenuAnchor(null);
  };

  const handleReactionsClick = () => {
    if (reactionList.length > 0) {
      setReactionsDialogOpen(true);
    }
  };

  if (message.type === 'SYSTEM') {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
        <Paper elevation={1} sx={{ bgcolor: highlight ? 'warning.light' : (isOwn ? 'primary.light' : 'secondary.light'), p: 1.5, color: 'text.secondary', borderRadius: '16px', maxWidth: '80%' }}>
          <Typography variant="caption">{message.content}</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: isOwn ? 'flex-end' : 'flex-start', mb: 1 }}>
      {/* Action buttons on the LEFT for own messages */}
      {isOwn && !editing && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mr: 0.5, alignSelf: 'center' }}>
          <IconButton size="small" onClick={startEdit}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" onClick={handleDelete} sx={{ color: 'error.main' }}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      )}

      {!isOwn && (
        <IconButton onClick={() => onAvatarClick?.()} sx={{ p: 0, mr: 1, alignSelf: 'flex-end' }} size="small">
          <Avatar src={senderAvatar} sx={{ width: 32, height: 32, cursor: 'pointer' }}>
            {senderName?.charAt(0)?.toUpperCase()}
          </Avatar>
        </IconButton>
      )}

      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isOwn ? 'flex-end' : 'flex-start', maxWidth: '70%' }}>
        <Paper 
          elevation={highlight ? 3 : 1} 
          sx={{ 
            p: 1.5, 
            bgcolor: isOwn ? 'primary.light' : 'secondary.light', 
            color: 'white', 
            borderRadius: isOwn ? '18px 4px 18px 18px' : '4px 18px 18px 18px', 
            wordBreak: 'break-word',
            ...(highlight && {
              fontWeight: 'bold',
              border: '2px solid',
              borderColor: isOwn ? 'secondary.main' : 'primary.main',
              boxShadow: 3,
            })
          }}
        >
          {editing ? (
            <Box>
              <TextField
                size="small"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                multiline
                autoFocus
                fullWidth
                variant="standard"
                InputProps={{ style: { color: 'white' } }}
                sx={{ mb: 1 }}
              />
              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                <Button size="small" onClick={cancelEdit} sx={{ color: 'white', border: '1px solid rgba(255,255,255,0.5)' }}>Cancel</Button>
                <Button size="small" variant="contained" onClick={saveEdit} sx={{ bgcolor: 'rgba(255,255,255,0.3)' }}>Save</Button>
              </Box>
            </Box>
          ) : (
            <>
              {message.content && (
                <Typography variant="body2" sx={{ fontWeight: highlight ? 'bold' : 'normal' }}>
                  {message.content}
                </Typography>
              )}
              {attachments.length > 0 && (
                <Box sx={{ mt: message.content ? 1 : 0 }}>
                  {attachments.map((att, idx) => (
                    <AttachmentItem key={att.id || idx} attachment={att} />
                  ))}
                </Box>
              )}
            </>
          )}
        </Paper>

        {/* Reaction summary row */}
        {totalReactions > 0 && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              mt: 0.25,
              px: 1,
              py: 0.25,
              bgcolor: 'rgba(0,0,0,0.05)',
              borderRadius: 4,
              cursor: reactionList.length > 0 ? 'pointer' : 'default',
            }}
            onClick={handleReactionsClick}
          >
            {shownReactions.map(([type, count]) => (
              <Box key={type} sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
                <Typography variant="caption">{REACTION_EMOJI[type]}</Typography>
                {count > 1 && <Typography variant="caption" sx={{ fontSize: '0.6rem' }}>{count}</Typography>}
              </Box>
            ))}
            {remainingCount > 0 && (
              <Typography variant="caption" sx={{ fontSize: '0.6rem', color: 'text.secondary' }}>
                +{remainingCount} more
              </Typography>
            )}
          </Box>
        )}

        {/* Name, time, and add-reaction button */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5, mx: 1 }}>
          {!isOwn && (
            <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.65rem', cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }} onClick={() => onAvatarClick?.()}>
              {senderName}
            </Typography>
          )}
          <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.65rem' }}>
            {time}
          </Typography>
          {/* Add reaction button */}
          <IconButton size="small" onClick={(e) => setReactionMenuAnchor(e.currentTarget)}>
            <AddReactionIcon fontSize="inherit" />
          </IconButton>
        </Box>
      </Box>

      {/* Reaction picker menu */}
      <Menu anchorEl={reactionMenuAnchor} open={Boolean(reactionMenuAnchor)} onClose={() => setReactionMenuAnchor(null)}>
        <Box sx={{ display: 'flex', px: 1, py: 0.5, gap: 0.5 }}>
          {Object.entries(REACTION_EMOJI).map(([type, emoji]) => (
            <IconButton key={type} onClick={() => handleReactionSelect(type)} sx={{ fontSize: '1.2rem' }}>
              {emoji}
            </IconButton>
          ))}
        </Box>
      </Menu>

      {/* Reactions detail dialog */}
      <Dialog open={reactionsDialogOpen} onClose={() => setReactionsDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Reactions</DialogTitle>
        <List>
          {reactionList.map((reaction, idx) => {
            const profile = participants?.[reaction.userId];
            const displayName = profile?.displayName || reaction.userId?.slice(0, 8) || 'Unknown';
            const avatarUrl = profile?.avatarUrl;
            return (
              <ListItem key={reaction.id || idx}>
                <ListItemAvatar>
                  <Avatar src={avatarUrl}>
                    {displayName.charAt(0).toUpperCase()}
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body2">{REACTION_EMOJI[reaction.reactionType] || reaction.reactionType}</Typography>
                      <Typography variant="body2">{displayName}</Typography>
                    </Box>
                  }
                />
              </ListItem>
            );
          })}
        </List>
      </Dialog>
    </Box>
  );
};

// AttachmentItem
const AttachmentItem = ({ attachment }) => {
  const [thumbnailSrc, setThumbnailSrc] = useState(null);
  const retryTimeoutRef = useRef(null);

  const getFileInfo = (fileName) => {
    if (!fileName) return { icon: <InsertDriveFileIcon fontSize="small" />, type: 'file' };
    const ext = fileName.split('.').pop()?.toLowerCase();
    if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(ext)) {
      if (THUMBNAILS_ENABLED) return { icon: null, type: 'image' };
      return { icon: <ImageIcon fontSize="small" sx={{ color: '#4caf50' }} />, type: 'image' };
    }
    if (['mp4', 'mov', 'webm', 'mpeg'].includes(ext)) return { icon: <VideocamIcon fontSize="small" sx={{ color: '#ff5722' }} />, type: 'video' };
    if (ext === 'pdf') return { icon: <PictureAsPdfIcon fontSize="small" sx={{ color: '#f44336' }} />, type: 'pdf' };
    if (['doc', 'docx', 'odt'].includes(ext)) return { icon: <DescriptionIcon fontSize="small" sx={{ color: '#2196f3' }} />, type: 'document' };
    if (ext === 'txt') return { icon: <ArticleIcon fontSize="small" sx={{ color: '#9e9e9e' }} />, type: 'text' };
    return { icon: <InsertDriveFileIcon fontSize="small" />, type: 'file' };
  };

  const fileInfo = getFileInfo(attachment.fileName);

  useEffect(() => {
    let cancelled = false;
    
    const fetchThumbnail = async () => {
      if (!THUMBNAILS_ENABLED || fileInfo.type !== 'image' || !attachment.id || attachment.uploading) return;
      
      try {
        const url = await attachmentService.getThumbnailBlobUrl(attachment.id);
        if (!cancelled) setThumbnailSrc(url);
      } catch (err) {
        if (err?.status === 425) {
          // Not ready yet - silently retry
          if (!cancelled) {
            retryTimeoutRef.current = setTimeout(fetchThumbnail, 2000);
          }
        }
        // Other errors (404, 500) - just show icon, no retry
      }
    };
    
    fetchThumbnail();
    
    return () => {
      cancelled = true;
      if (retryTimeoutRef.current) {
        clearTimeout(retryTimeoutRef.current);
        retryTimeoutRef.current = null;
      }
    };
  }, [attachment.id, fileInfo.type, attachment.uploading]);

  useEffect(() => {
    return () => { if (thumbnailSrc) URL.revokeObjectURL(thumbnailSrc); };
  }, [thumbnailSrc]);

  if (attachment.uploading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5, opacity: 0.6 }}>
        <InsertDriveFileIcon fontSize="small" />
        <Typography variant="caption" sx={{ fontStyle: 'italic' }}>{attachment.fileName} (uploading...)</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
      {fileInfo.type === 'image' && thumbnailSrc ? (
        <img src={thumbnailSrc} alt={attachment.fileName} style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 4 }} />
      ) : (
        <Box sx={{ width: 48, height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'rgba(255,255,255,0.1)', borderRadius: 1 }}>
          {fileInfo.icon}
        </Box>
      )}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="caption" sx={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'white' }}>
          {attachment.fileName}
        </Typography>
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.65rem' }}>
          {attachment.fileSize ? `${(attachment.fileSize / 1024).toFixed(1)} KB` : ''}
          {fileInfo.type !== 'image' && fileInfo.type !== 'file' ? ` • ${fileInfo.type.toUpperCase()}` : ''}
        </Typography>
      </Box>
      <IconButton size="small" onClick={() => attachmentService.downloadFile(attachment.id, attachment.fileName)} sx={{ color: 'white' }}>
        <DownloadIcon fontSize="small" />
      </IconButton>
    </Box>
  );
};

export default MessageBubble;