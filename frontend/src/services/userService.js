// services/userService.js
const API_BASE_URL = '/api'; // Through gateway

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
    if (UserService.instance) {
      return UserService.instance;
    }
    
    // Caches
    this.profileCache = new Map();     // userId -> ProfileResponse
    this.contactCache = new Map();     // 'contacts' -> array, 'incoming' -> array, etc.
    this.pendingRequests = new Map();   // key -> Promise
    
    // Online status refresh timer
    this.statusRefreshTimer = null;
    this.statusRefreshIntervalMs = 2 * 60 * 1000; // 15 minutes
    
    UserService.instance = this;
    
    // Start the refresh timer
    this.startStatusRefreshTimer();
  }

  startStatusRefreshTimer() {
    if (this.statusRefreshTimer) {
      clearInterval(this.statusRefreshTimer);
    }
    
    console.log(`Starting online status refresh every ${this.statusRefreshIntervalMs / 60000} minutes`);
    
    this.statusRefreshTimer = setInterval(() => {
      this.refreshAllOnlineStatuses().catch(err => {
        console.error('Failed to refresh online statuses:', err);
      });
    }, this.statusRefreshIntervalMs);
  }

  stopStatusRefreshTimer() {
    if (this.statusRefreshTimer) {
      clearInterval(this.statusRefreshTimer);
      this.statusRefreshTimer = null;
    }
  }

  async refreshAllOnlineStatuses() {
    const cachedUserIds = Array.from(this.profileCache.keys());
    
    if (cachedUserIds.length === 0) {
      console.log('No cached users to refresh status for');
      return;
    }
    
    // Filter out the current user's own ID
    const currentUserId = this.getCurrentUserId();
    const otherUserIds = cachedUserIds.filter(id => id !== currentUserId);
    
    if (otherUserIds.length === 0) {
      return;
    }
    
    console.log(`Refreshing online status for ${otherUserIds.length} cached users`);
    
    try {
      const statuses = await this.getBatchUserStatus(otherUserIds);
      
      let updatedCount = 0;
      for (const status of statuses) {
        const cachedProfile = this.profileCache.get(status.userId);
        if (cachedProfile) {
          const wasOnline = cachedProfile.online;
          cachedProfile.online = status.online;
          cachedProfile.lastSeen = status.lastSeen;
          this.profileCache.set(status.userId, cachedProfile);
          
          if (wasOnline !== status.online) {
            updatedCount++;
          }
        }
      }
      
      console.log(`Online status refresh complete. ${updatedCount} users changed status.`);
      
      // Dispatch event so components can update
      window.dispatchEvent(new CustomEvent('online-status-updated'));
      
    } catch (error) {
      console.error('Failed to refresh online statuses:', error);
    }
  }

  async refreshOnlineStatusForUsers(userIds) {
    if (!userIds || userIds.length === 0) {
      return;
    }
    
    const uniqueIds = [...new Set(userIds)];
    const currentUserId = this.getCurrentUserId();
    const otherUserIds = uniqueIds.filter(id => id !== currentUserId);
    
    if (otherUserIds.length === 0) {
      return;
    }
    
    try {
      const statuses = await this.getBatchUserStatus(otherUserIds);
      
      for (const status of statuses) {
        const cachedProfile = this.profileCache.get(status.userId);
        if (cachedProfile) {
          cachedProfile.online = status.online;
          cachedProfile.lastSeen = status.lastSeen;
          this.profileCache.set(status.userId, cachedProfile);
        }
      }
      
      window.dispatchEvent(new CustomEvent('online-status-updated'));
      
    } catch (error) {
      console.error('Failed to refresh online status for specific users:', error);
    }
  }

  clearCache() {
    this.profileCache.clear();
    this.contactCache.clear();
    this.pendingRequests.clear();
  }

  clearUserCache(userId) {
    this.profileCache.delete(userId);
  }

  async #dedupeRequest(key, requestFn) {
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key);
    }
    const promise = requestFn().finally(() => {
      this.pendingRequests.delete(key);
    });
    this.pendingRequests.set(key, promise);
    return promise;
  }

  // ========== Profile Methods ==========

  async getProfile(userId, forceRefresh = false) {
    if (!forceRefresh && this.profileCache.has(userId)) {
      return this.profileCache.get(userId);
    }
    const key = `profile:${userId}`;
    return this.#dedupeRequest(key, async () => {
      const response = await fetch(`${API_BASE_URL}/users/${userId}/profile`, {
        headers: getHeaders()
      });
      if (!response.ok) throw new Error(`Failed to fetch profile for user ${userId}`);
      const profile = await response.json();
      this.profileCache.set(userId, profile);
      return profile;
    });
  }

  async getProfiles(userIds, forceRefresh = false) {
    const uniqueIds = [...new Set(userIds)];
    const results = await Promise.all(
      uniqueIds.map(id => this.getProfile(id, forceRefresh).catch(() => null))
    );
    const map = new Map();
    results.forEach(p => { if (p) map.set(p.userId, p); });
    return map;
  }

  async createProfile(userId, displayName) {
    const response = await fetch(
      `${API_BASE_URL}/users/${userId}/profile?displayName=${encodeURIComponent(displayName)}`,
      { method: 'POST', headers: getHeaders() }
    );
    if (!response.ok) throw new Error('Failed to create profile');
    const profile = await response.json();
    this.profileCache.set(userId, profile);
    return profile;
  }

  async updateProfile(userId, updateData) {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/profile`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(updateData)
    });
    if (!response.ok) throw new Error('Failed to update profile');
    const updated = await response.json();
    this.profileCache.set(userId, updated);
    return updated;
  }

  async searchUsers(query) {
    const response = await fetch(`${API_BASE_URL}/users/search?query=${encodeURIComponent(query)}`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to search users');
    const results = await response.json(); // Array of ProfileResponse
    results.forEach(p => this.profileCache.set(p.userId, p));
    return results;
  }

  // Batch fetch user summaries (UserSummaryDto)
  async getBatchUserSummaries(userIds) {
    const uniqueIds = [...new Set(userIds)];
    const response = await fetch(`${API_BASE_URL}/users/batch`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ userIds: uniqueIds })
    });
    if (!response.ok) throw new Error('Failed to fetch batch user summaries');
    const map = await response.json(); // Map<UUID, UserSummaryDto>
    // Optionally cache each summary as a lightweight profile
    for (const [id, summary] of Object.entries(map)) {
      if (!this.profileCache.has(id)) {
        this.profileCache.set(id, {
          userId: summary.userId,
          displayName: summary.displayName,
          avatarUrl: summary.avatarUrl,
          online: summary.isOnline
        });
      }
    }
    return map;
  }

  // ========== Contact Methods ==========

  // Get accepted contacts for current user
  async getContacts(forceRefresh = false) {
    const cacheKey = 'contacts';
    if (!forceRefresh && this.contactCache.has(cacheKey)) {
      return this.contactCache.get(cacheKey);
    }
    const response = await fetch(`${API_BASE_URL}/contacts`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch contacts');
    const contacts = await response.json(); // Array of ContactResponse
    this.contactCache.set(cacheKey, contacts);
    // Pre-cache profiles of contacts
    for (const contact of contacts) {
      const otherId = contact.requesterId === this.getCurrentUserId() ? contact.recipientId : contact.requesterId;
      if (!this.profileCache.has(otherId)) {
        this.profileCache.set(otherId, { userId: otherId });
      }
    }
    return contacts;
  }

  async sendContactRequest(targetUserId) {
    const response = await fetch(`${API_BASE_URL}/contacts/request/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to send contact request');
    const contact = await response.json();
    this.contactCache.delete('contacts');
    this.contactCache.delete('incoming');
    this.contactCache.delete('outgoing');
    return contact;
  }

  async acceptContactRequest(contactId) {
    const response = await fetch(`${API_BASE_URL}/contacts/${contactId}/accept`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to accept contact request');
    this.contactCache.clear();
    return true;
  }

  async rejectContactRequest(contactId) {
    const response = await fetch(`${API_BASE_URL}/contacts/${contactId}/reject`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to reject contact request');
    this.contactCache.delete('incoming');
    return true;
  }

  async cancelContactRequest(contactId) {
    const response = await fetch(`${API_BASE_URL}/contacts/${contactId}/cancel`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to cancel contact request');
    this.contactCache.delete('outgoing');
    return true;
  }

  async removeContact(contactId) {
    const response = await fetch(`${API_BASE_URL}/contacts/${contactId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to remove contact');
    this.contactCache.delete('contacts');
    return true;
  }

  async blockUser(targetUserId) {
    const response = await fetch(`${API_BASE_URL}/contacts/block/${targetUserId}`, {
      method: 'POST',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to block user');
    this.contactCache.delete('contacts');
    this.contactCache.delete('blocked');
    return true;
  }

  async unblockUser(targetUserId) {
    const response = await fetch(`${API_BASE_URL}/contacts/block/${targetUserId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to unblock user');
    this.contactCache.delete('contacts');
    this.contactCache.delete('blocked');
    return true;
  }

  async getIncomingRequests(forceRefresh = false) {
    const cacheKey = 'incoming';
    if (!forceRefresh && this.contactCache.has(cacheKey)) {
      return this.contactCache.get(cacheKey);
    }
    const response = await fetch(`${API_BASE_URL}/contacts/pending/incoming`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch incoming requests');
    const requests = await response.json();
    this.contactCache.set(cacheKey, requests);
    return requests;
  }

  async getOutgoingRequests(forceRefresh = false) {
    const cacheKey = 'outgoing';
    if (!forceRefresh && this.contactCache.has(cacheKey)) {
      return this.contactCache.get(cacheKey);
    }
    const response = await fetch(`${API_BASE_URL}/contacts/pending/outgoing`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch outgoing requests');
    const requests = await response.json();
    this.contactCache.set(cacheKey, requests);
    return requests;
  }

  async getBlockedUsers(forceRefresh = false) {
    const cacheKey = 'blocked';
    if (!forceRefresh && this.contactCache.has(cacheKey)) {
      return this.contactCache.get(cacheKey);
    }
    const response = await fetch(`${API_BASE_URL}/contacts/blocked`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch blocked users');
    const blocked = await response.json();
    this.contactCache.set(cacheKey, blocked);
    return blocked;
  }

  // ========== Presence Methods ==========

  async getUserStatus(userId) {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/status`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch user status');
    return response.json(); // UserStatusResponse
  }

  async getBatchUserStatus(userIds) {
    const response = await fetch(`${API_BASE_URL}/users/status/batch`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ userIds })
    });
    if (!response.ok) throw new Error('Failed to fetch batch status');
    return response.json(); // List<UserStatusResponse>
  }

  async getLastSeen(userId) {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/last-seen`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch last seen');
    return response.json(); // LastSeenResponse
  }

  async getUserSessions(userId) {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/sessions`, { headers: getHeaders() });
    if (!response.ok) throw new Error('Failed to fetch user sessions');
    return response.json(); // List<SessionInfoResponse>
  }

  async terminateSession(userId, sessionId) {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to terminate session');
    return true;
  }

  // Helper to get current user ID (from auth context or localStorage)
  getCurrentUserId() {
    const userInfo = localStorage.getItem('userInfo');
    if (userInfo) {
      try {
        return JSON.parse(userInfo).userId;
      } catch (e) {}
    }
    return null;
  }
  
  // Clean up timer when needed (optional - for testing)
  destroy() {
    this.stopStatusRefreshTimer();
  }
}

export default new UserService();