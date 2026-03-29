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


  // Get incoming contact requests (where current user is the contact)
  async getIncomingContactRequests(userId, forceRefresh = false) {
    const key = `incomingContacts:${userId}`;
    
    if (!forceRefresh && this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key);
    }
    
    return this.#dedupeRequest(key, async () => {
      const response = await fetch(`${API_BASE_URL}/${userId}/contacts`, {
        headers: getHeaders()
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch contacts');
      }
      
      const contacts = await response.json();
      
      // Filter to only PENDING requests where contactUserId matches current user?
      // Actually, the API might return all contacts, we need to see the structure.
      // Based on the ContactResponse schema:
      // - userId: the owner of the contact list
      // - contactUserId: the person they added
      // - status: PENDING/ACCEPTED/BLOCKED
      
      // For incoming requests, we want contacts where:
      // 1. The request was sent to current user (contactUserId === currentUserId)
      // 2. Status is PENDING
      
      // But wait - the endpoint is GET /api/users/{userId}/contacts
      // This returns contacts for userId (the owner of the contact list)
      // So to see who added YOU, you'd need to fetch contacts where contactUserId === your ID
      // That's not directly supported by this endpoint.
      
      // Alternative approach: Store all contacts and filter client-side
      // Or, your backend might have a separate endpoint for incoming requests
      
      // For now, let's store all contacts and we'll filter in the component
      this.incomingRequestsCache = this.incomingRequestsCache || new Map();
      this.incomingRequestsCache.set(userId, contacts);
      
      return contacts;
    });
  }

  // Accept a contact request (update status to ACCEPTED)
  async acceptContactRequest(userId, contactUserId) {
    // This might be a PUT or PATCH endpoint - adjust based on your API
    // Assuming you have an endpoint to update contact status
    const response = await fetch(`${API_BASE_URL}/${userId}/contacts/${contactUserId}/accept`, {
      method: 'PUT',
      headers: getHeaders()
    });
    
    if (!response.ok) {
      throw new Error('Failed to accept contact request');
    }
    
    // Clear caches
    this.contactCache.delete(userId);
    if (this.incomingRequestsCache) {
      this.incomingRequestsCache.delete(userId);
    }
    
    return true;
  }

  // Reject/decline a contact request
  async rejectContactRequest(userId, contactUserId) {
    // Or use DELETE if that removes the pending request
    const response = await fetch(`${API_BASE_URL}/${userId}/contacts/${contactUserId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    
    if (!response.ok) {
      throw new Error('Failed to reject contact request');
    }
    
    // Clear caches
    this.contactCache.delete(userId);
    if (this.incomingRequestsCache) {
      this.incomingRequestsCache.delete(userId);
    }
    
    return true;
  }

  // Add a contact
  async addContact(userId, contactUserId) {
    const response = await fetch(
      `${API_BASE_URL}/${userId}/contacts/${contactUserId}`,
      {
        method: 'POST',
        headers: getHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to add contact');
    }
    
    const newContact = await response.json();
    
    // Invalidate contacts cache (force refresh on next get)
    this.contactCache.delete(userId);
    
    // Cache the new contact's profile
    this.profileCache.set(contactUserId, {
      userId: contactUserId,
      displayName: newContact.displayName,
      avatarUrl: newContact.avatarUrl,
      online: newContact.online,
    });
    
    return newContact;
  }

  // Remove a contact
  async removeContact(userId, contactUserId) {
    const response = await fetch(
      `${API_BASE_URL}/${userId}/contacts/${contactUserId}`,
      {
        method: 'DELETE',
        headers: getHeaders()
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to remove contact');
    }
    
    // Invalidate contacts cache
    this.contactCache.delete(userId);
    
    return true;
  }

  // Get contact status for a specific user
  async getContactStatus(userId, contactUserId) {
    const contacts = await this.getContacts(userId);
    const contact = contacts.find(c => c.contactUserId === contactUserId);
    return contact?.status || null;
  }
}

// Export singleton instance
const userService = new UserService();
export default userService;