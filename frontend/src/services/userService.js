const API_BASE_URL = '/api/users'; // Through gateway

// Helper for auth headers
const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

// Profile API
export const profileService = {
  // Get user profile
  getProfile: async (userId) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/profile`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch profile');
    return response.json();
  },

  // Create profile (usually happens automatically after registration)
  createProfile: async (userId, displayName) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/profile?displayName=${encodeURIComponent(displayName)}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to create profile');
    return response.json();
  },

  // Update profile
  updateProfile: async (userId, profileData) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/profile`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(profileData)
    });
    if (!response.ok) throw new Error('Failed to update profile');
    return response.json();
  },

  // Search users by display name
  searchUsers: async (query) => {
    const response = await fetch(`${API_BASE_URL}/search?query=${encodeURIComponent(query)}`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to search users');
    return response.json();
  },

  // Update online status
  setOnlineStatus: async (userId, status = true) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/online?status=${status}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to update online status');
    return true;
  }
};

// Contacts API
export const contactService = {
  // Get all contacts for user
  getContacts: async (userId) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/contacts`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to fetch contacts');
    return response.json();
  },

  // Add a contact
  addContact: async (userId, contactUserId) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/contacts/${contactUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to add contact');
    return response.json();
  },

  // Remove a contact
  removeContact: async (userId, contactUserId) => {
    const response = await fetch(`${API_BASE_URL}/${userId}/contacts/${contactUserId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to remove contact');
    return true;
  }
};

// Combined service for easy imports
export const userService = {
  ...profileService,
  ...contactService,
  
  // Helper to get multiple profiles at once
  getProfiles: async (userIds) => {
    // Since there's no batch endpoint, we'll fetch individually
    // In production, you might want to add a batch endpoint
    const promises = userIds.map(id => profileService.getProfile(id).catch(() => null));
    const profiles = await Promise.all(promises);
    return profiles.filter(Boolean);
  },

  // Helper to get display name for a user
  getDisplayName: async (userId) => {
    try {
      const profile = await profileService.getProfile(userId);
      return profile.displayName;
    } catch {
      return userId.slice(0, 8); // Fallback to truncated ID
    }
  }
};