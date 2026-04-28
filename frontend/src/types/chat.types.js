export const MessageType = {
  TEXT: 'TEXT',
  IMAGE: 'IMAGE',
  FILE: 'FILE',
  TYPING: 'TYPING',
  SYSTEM: 'SYSTEM'
};

/**
 * ChatMessage format:
 * {
 *   id: "uuid",
 *   senderId: "uuid",
 *   recipientId: "uuid", // For DIRECT: other user, for GROUP: groupId
 *   content: "message text",
 *   type: "TEXT" | "IMAGE" | "FILE" | "TYPING" | "SYSTEM",
 *   timestamp: "2026-03-18T12:00:00"
 * }
 */