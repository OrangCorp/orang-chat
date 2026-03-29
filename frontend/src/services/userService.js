// services/userService.js

const API_BASE_URL = '/api/users'; // Through gateway

// Helper for auth headers
const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

class UserService {
  constructor() {
    // Singleton instance
    if (UserService.instance) {
      return UserService.instance;
    }
    
    // Caches
    this.profileCache = new Map();     // userId -> profile object
    this.contactCache = new Map();     // userId -> contacts array
    this.pendingRequests = new Map();   // userId -> Promise (to prevent duplicate requests)
    
    UserService.instance = this;
  }

  // Clear all caches (useful for logout)
  clearCache() {
    this.profileCache.clear();
    this.contactCache.clear();
    this.pendingRequests.clear();
  }

  // Clear cache for a specific user
  clearUserCache(userId) {
    this.profileCache.delete(userId);
    this.contactCache.delete(userId);
  }

  // Generic method to handle deduplication of requests
  async #dedupeRequest(key, requestFn) {
    // If there's already a pending request for this key, return that promise
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key);
    }

    // Create the request promise
    const promise = requestFn().finally(() => {
      // Clean up after request completes (success or error)
      this.pendingRequests.delete(key);
    });

    // Store the promise
    this.pendingRequests.set(key, promise);
    
    return promise;
  }

  // ========== Profile Methods ==========

  // Get user profile with caching
  async getProfile(userId, forceRefresh = false) {
    // Return from cache if available and not forcing refresh
    if (!forceRefresh && this.profileCache.has(userId)) {
      return this.profileCache.get(userId);
    }

    const key = `profile:${userId}`;
    
    return this.#dedupeRequest(key, async () => {
      const response = await fetch(`${API_BASE_URL}/${userId}/profile`, {
        headers: getHeaders()
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch profile for user ${userId}`);
      }
      
      const profile = await response.json();
      
      // Store in cache
      this.profileCache.set(userId, profile);
      
      return profile;
    });
  }

  // Get multiple profiles at once (with individual caching)
  async getProfiles(userIds, forceRefresh = false) {
    const uniqueUserIds = [...new Set(userIds)]; // Remove duplicates
    
    // Get profiles (cached ones will return immediately)
    const promises = uniqueUserIds.map(userId => 
      this.getProfile(userId, forceRefresh).catch(() => null)
    );
    
    const profiles = await Promise.all(promises);
    
    // Return as a map for easy lookup
    const profileMap = new Map();
    profiles.forEach(profile => {
      if (profile) {
        profileMap.set(profile.userId, profile);
      }
    });
    
    return profileMap;
  }

  // Get display name quickly (returns from cache if available)
  async getDisplayName(userId) {
    try {
      const profile = await this.getProfile(userId);
      return profile.displayName;
    } catch (error) {
      console.warn(`Could not get display name for ${userId}:`, error);
      return userId.slice(0, 8); // Fallback to truncated ID
    }
  }

  // Create profile (usually happens automatically after registration)
  async createProfile(userId, displayName) {
    const response = await fetch(
      `${API_BASE_URL}/${userId}/profile?displayName=${encodeURIComponent(displayName)}`,
      {
        method: 'POST',
        headers: getHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to create profile');
    }
    
    const profile = await response.json();
    
    // Update cache
    this.profileCache.set(userId, profile);
    
    return profile;
  }

  // Update profile
  async updateProfile(userId, profileData) {
    const response = await fetch(`${API_BASE_URL}/${userId}/profile`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(profileData)
    });
    
    if (!response.ok) {
      throw new Error('Failed to update profile');
    }
    
    const updatedProfile = await response.json();
    
    // Update cache with new data
    this.profileCache.set(userId, updatedProfile);
    
    return updatedProfile;
  }

  // Update online status
  async setOnlineStatus(userId, status = true) {
    const response = await fetch(
      `${API_BASE_URL}/${userId}/online?status=${status}`,
      {
        method: 'POST',
        headers: getHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to update online status');
    }
    
    // Update cached profile if it exists
    if (this.profileCache.has(userId)) {
      const cachedProfile = this.profileCache.get(userId);
      cachedProfile.online = status;
      this.profileCache.set(userId, cachedProfile);
    }
    
    return true;
  }

  // Search users (doesn't cache results by default, as search is dynamic)
  async searchUsers(query) {
    const response = await fetch(
      `${API_BASE_URL}/search?query=${encodeURIComponent(query)}`,
      {
        headers: getHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to search users');
    }
    
    const results = await response.json();
    
    // Cache any profiles we got from search
    results.forEach(profile => {
      this.profileCache.set(profile.userId, profile);
    });
    
    return results;
  }

  // ========== Contact Methods ==========

  // Get all contacts for a user
  async getContacts(userId, forceRefresh = false) {
    // Return from cache if available and not forcing refresh
    if (!forceRefresh && this.contactCache.has(userId)) {
      return this.contactCache.get(userId);
    }

    const key = `contacts:${userId}`;
    
    return this.#dedupeRequest(key, async () => {
      const response = await fetch(`${API_BASE_URL}/${userId}/contacts`, {
        headers: getHeaders()
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch contacts');
      }
      
      const contacts = await response.json();
      
      // Store in cache
      this.contactCache.set(userId, contacts);
      
      // Also cache individual profiles for each contact
      contacts.forEach(contact => {
        if (contact.userId) {
          this.profileCache.set(contact.contactUserId, {
            userId: contact.contactUserId,
            displayName: contact.displayName,
            avatarUrl: contact.avatarUrl,
            online: contact.online,
            // Note: bio and lastSeen might not be in contact response
          });
        }
      });
      
      return contacts;
    });
  }

// ========== Contact Methods ==========

  // Get all accepted contacts for current user
  async getContacts(forceRefresh = false) {
    if (!forceRefresh && this.contactCache.has('contacts')) {
      return this.contactCache.get('contacts');
    }

    const response = await fetch(`/api/contacts`, {
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to fetch contacts');

    const contacts = await response.json();
    this.contactCache.set('contacts', contacts);

    // Cache profiles for quick lookup
    contacts.forEach(c => {
      const otherUserId = c.requesterId === this.currentUserId ? c.recipientId : c.requesterId;
      if (!this.profileCache.has(otherUserId)) {
        this.profileCache.set(otherUserId, { userId: otherUserId });
      }
    });

    return contacts;
  }

  // Send a contact request
  async sendContactRequest(targetUserId) {
    const response = await fetch(`/api/contacts/request/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to send contact request');

    const contact = await response.json();

    // Invalidate contacts cache
    this.contactCache.delete('contacts');
    return contact;
  }

  // Accept a contact request by contactId
  async acceptContactRequest(contactId) {
    const response = await fetch(`/api/contacts/${contactId}/accept`, {
      method: 'POST',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to accept contact request');

    this.contactCache.delete('contacts');
    return true;
  }

  // Reject a contact request by contactId
  async rejectContactRequest(contactId) {
    const response = await fetch(`/api/contacts/${contactId}/reject`, {
      method: 'POST',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to reject contact request');

    this.contactCache.delete('contacts');
    return true;
  }

  // Cancel a sent request
  async cancelContactRequest(contactId) {
    const response = await fetch(`/api/contacts/${contactId}/cancel`, {
      method: 'POST',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to cancel contact request');

    this.contactCache.delete('contacts');
    return true;
  }

  // Remove an accepted contact
  async removeContact(contactId) {
    const response = await fetch(`/api/contacts/${contactId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to remove contact');

    this.contactCache.delete('contacts');
    return true;
  }

  // Block a user
  async blockUser(targetUserId) {
    const response = await fetch(`/api/contacts/block/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to block user');

    this.contactCache.delete('contacts');
    return true;
  }

  // Unblock a user
  async unblockUser(targetUserId) {
    const response = await fetch(`/api/contacts/block/${targetUserId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to unblock user');

    this.contactCache.delete('contacts');
    return true;
  }

  // Get incoming requests
  async getIncomingRequests() {
    const response = await fetch(`/api/contacts/pending/incoming`, {
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to fetch incoming requests');
    return await response.json();
  }

  // Get outgoing requests
  async getOutgoingRequests() {
    const response = await fetch(`/api/contacts/pending/outgoing`, {
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to fetch outgoing requests');
    return await response.json();
  }

  // Get blocked users
  async getBlockedUsers() {
    const response = await fetch(`/api/contacts/blocked`, {
      headers: getHeaders()
    });

    if (!response.ok) throw new Error('Failed to fetch blocked users');
    return await response.json();
  }

}

// Export singleton instance
const userService = new UserService();
export default userService;