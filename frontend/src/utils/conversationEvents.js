// utils/conversationEvents.js
export const CONVERSATION_CREATED = 'conversation-created';
export const CONVERSATION_UPDATED = 'conversation-updated';

export const emitConversationCreated = (conversation) => {
  window.dispatchEvent(new CustomEvent(CONVERSATION_CREATED, { detail: conversation }));
};

export const emitConversationUpdated = (conversationId) => {
  window.dispatchEvent(new CustomEvent(CONVERSATION_UPDATED, { detail: { conversationId } }));
};