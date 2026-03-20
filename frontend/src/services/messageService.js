const API_BASE_URL = '/api'; // Using proxy

// Helper for auth headers
const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

// Conversations API
export const conversationService = {
  // Get all conversations for current user
  getConversations: async () => {
    const response = await fetch(`${API_BASE_URL}/conversations`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch conversations');
    return response.json();
  },

  // Get or create direct chat with another user
  getOrCreateDirectChat: async (targetUserId) => {
    const response = await fetch(`${API_BASE_URL}/conversations/direct/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to create direct chat');
    return response.json();
  },

  // Create a group chat
  createGroupChat: async (name, participantIds) => {
    const response = await fetch(`${API_BASE_URL}/conversations/group`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ name, participantIds })
    });
    if (!response.ok) throw new Error('Failed to create group chat');
    return response.json();
  }
};

// Messages API
export const messageService = {
  // Get message history for a conversation (with pagination)
  getMessages: async (conversationId, page = 0, size = 50) => {
    const response = await fetch(
      `${API_BASE_URL}/messages/${conversationId}?page=${page}&size=${size}`,
      { headers: getHeaders() }
    );
    if (!response.ok) throw new Error('Failed to fetch messages');
    return response.json(); // Returns Page object with content, totalPages, etc.
  }
};
