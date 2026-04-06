// services/MessageService.js
const API_BASE_URL = '/api';

const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

class MessageService {
  constructor() {
    if (MessageService.instance) {
      return MessageService.instance;
    }
    MessageService.instance = this;
  }

  // ==================== Conversations ====================
  
  async getConversations() {
    const response = await fetch(`${API_BASE_URL}/conversations`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch conversations');
    return response.json();
  }

  async getOrCreateDirectChat(targetUserId) {
    const response = await fetch(`${API_BASE_URL}/conversations/direct/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to create direct chat');
    return response.json();
  }

  async createGroupChat(name, participantIds) {
    const response = await fetch(`${API_BASE_URL}/conversations/group`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ name, participantIds })
    });
    if (!response.ok) throw new Error('Failed to create group chat');
    return response.json();
  }

  async addParticipants(conversationId, userIds) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/participants`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ userIds })
    });
    if (!response.ok) throw new Error('Failed to add participants');
    return response.json();
  }

  async removeParticipant(conversationId, userId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/participants/${userId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to remove participant');
  }

  async leaveConversation(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/leave`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to leave conversation');
  }

  async renameConversation(conversationId, name) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ name })
    });
    if (!response.ok) throw new Error('Failed to rename conversation');
    return response.json();
  }

  async promoteParticipant(conversationId, userId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/participants/${userId}/promote`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to promote participant');
    return response.json();
  }

  async demoteParticipant(conversationId, userId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/participants/${userId}/demote`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to demote participant');
    return response.json();
  }

  async deleteConversation(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to delete conversation');
  }

  // ==================== Messages ====================

  async getMessages(conversationId, page = 0, size = 50) {
    const response = await fetch(
      `${API_BASE_URL}/messages/${conversationId}?page=${page}&size=${size}`,
      { headers: getHeaders() }
    );
    if (!response.ok) throw new Error('Failed to fetch messages');
    return response.json();
  }

  async searchMessages(conversationId, query, page = 0, size = 50) {
    const response = await fetch(
      `${API_BASE_URL}/messages/${conversationId}/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
      { headers: getHeaders() }
    );
    if (!response.ok) throw new Error('Failed to search messages');
    return response.json();
  }

  async getMessagesAround(conversationId, messageId, size = 50) {
    const response = await fetch(
      `${API_BASE_URL}/messages/${conversationId}/around/${messageId}?size=${size}`,
      { headers: getHeaders() }
    );
    if (!response.ok) throw new Error('Failed to fetch messages around');
    return response.json();
  }

  async editMessage(messageId, content) {
    const response = await fetch(`${API_BASE_URL}/messages/${messageId}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ content })
    });
    if (!response.ok) throw new Error('Failed to edit message');
    return response.json();
  }

  async deleteMessage(messageId) {
    const response = await fetch(`${API_BASE_URL}/messages/${messageId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to delete message');
  }

  // ==================== Pinned Messages ====================

  async pinMessage(conversationId, messageId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/pins/${messageId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to pin message');
  }

  async unpinMessage(conversationId, messageId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/pins/${messageId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to unpin message');
  }

  async getPinnedMessages(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/pins`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch pinned messages');
    return response.json();
  }

  async isMessagePinned(conversationId, messageId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/pins/${messageId}`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to check pin status');
    return response.json();
  }

  // ==================== Read Receipts ====================

  async markAsRead(conversationId, messageId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/read/${messageId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to mark as read');
  }

  async getUnreadCount(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/read/unread-count`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get unread count');
    return response.json();
  }

  async getReadReceipt(conversationId) {
    const response = await fetch(`${API_BASE_URL}/conversations/${conversationId}/read`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get read receipt');
    return response.json();
  }

  // ==================== Reactions ====================

  async toggleReaction(messageId, reactionType) {
    const response = await fetch(`${API_BASE_URL}/messages/${messageId}/reactions/${reactionType}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to toggle reaction');
    return response.json();
  }

  async getReactions(messageId) {
    const response = await fetch(`${API_BASE_URL}/messages/${messageId}/reactions`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get reactions');
    return response.json();
  }

  async getMyReaction(messageId) {
    const response = await fetch(`${API_BASE_URL}/messages/${messageId}/reactions/mine`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get my reaction');
    return response.json();
  }
}

export default new MessageService();