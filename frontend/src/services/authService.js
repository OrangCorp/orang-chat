// AuthService.js - Fixed initialization order
import { Client } from '@stomp/stompjs';

class AuthService {
  constructor() {
    if (AuthService.instance) {
      return AuthService.instance;
    }

    this.accessToken = null;
    this.refreshToken = null;
    this.userInfo = null;
    
    // Token refresh configuration
    this.refreshTimer = null;
    this.refreshThresholdMs = 60000; // Refresh 1 minute before expiry
    this.tokenExpiryTime = null;
    
    // STOMP client configuration
    this.stompClient = null;
    this.heartbeatInterval = null;
    this.heartbeatDelayMs = 30000; // Send heartbeat every 30 seconds
    this.isActive = true;
    this.activityListeners = [];
    this.isConnecting = false;
    
    // API endpoints
    this.apiBaseUrl = '/api';
    
    AuthService.instance = this;
  }

  // ==================== Initialization ====================
  
  async initialize() {
    // Only load tokens from storage, DON'T auto-connect
    this.loadTokensFromStorage();
    
    
    // If we have tokens, schedule refresh but DON'T auto-connect WebSocket yet
    if (this.refreshToken && this.accessToken) {
      this.scheduleTokenRefresh();
      // Note: WebSocket will be connected only when explicitly called
      // (e.g., after login or when user navigates to chat)
    }
  }

  loadTokensFromStorage() {
    if (typeof localStorage !== 'undefined') {
      const storedAccessToken = localStorage.getItem('accessToken');
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedUserInfo = localStorage.getItem('userInfo');
      const storedExpiry = localStorage.getItem('tokenExpiryTime');
      
      if (storedAccessToken && storedRefreshToken) {
        this.accessToken = storedAccessToken;
        this.refreshToken = storedRefreshToken;
        this.userInfo = storedUserInfo ? JSON.parse(storedUserInfo) : null;
        this.tokenExpiryTime = storedExpiry ? parseInt(storedExpiry) : null;
        
        console.log('Tokens loaded from storage');
      }
    }
  }

  // ==================== Authentication Methods ====================

  async login(email, password) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Login failed');
      }

      const authResponse = await response.json();
      this.handleAuthResponse(authResponse);
      // Don't auto-connect WebSocket here - let the app decide when to connect
      return authResponse;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  async register(email, password, displayName) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password, displayName }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Registration failed');
      }

      const authResponse = await response.json();
      this.handleAuthResponse(authResponse);
      return authResponse;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  }

  async logout() {
    try {
      if (this.accessToken) {
        await fetch(`${this.apiBaseUrl}/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
          },
        });
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      this.clearAuth();
    }
  }

  handleAuthResponse(authResponse) {
    // Store tokens
    this.accessToken = authResponse.accessToken;
    this.refreshToken = authResponse.refreshToken;
    this.userInfo = {
      userId: authResponse.userId,
      email: authResponse.email,
      displayName: authResponse.displayName,
    };
    
    // Calculate expiry time (convert expiresIn from seconds to milliseconds)
    this.tokenExpiryTime = Date.now() + (authResponse.expiresIn * 1000);
    
    // Store in localStorage
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('accessToken', this.accessToken);
      localStorage.setItem('refreshToken', this.refreshToken);
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
      localStorage.setItem('tokenExpiryTime', this.tokenExpiryTime.toString());
      console.log('Tokens stored in localStorage');
    }
    
    // Schedule token refresh
    this.scheduleTokenRefresh();
  }

  async refreshAccessToken() {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      console.log('Refreshing access token...');
      const response = await fetch(`${this.apiBaseUrl}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      });

      if (!response.ok) {
        throw new Error('Token refresh failed');
      }

      const authResponse = await response.json();
      this.handleAuthResponse(authResponse);
      console.log('Token refreshed successfully');
      return authResponse;
    } catch (error) {
      console.error('Token refresh error:', error);
      this.clearAuth();
      throw error;
    }
  }

  scheduleTokenRefresh() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    if (this.tokenExpiryTime) {
      const timeUntilExpiry = this.tokenExpiryTime - Date.now();
      const refreshTime = Math.max(0, timeUntilExpiry - this.refreshThresholdMs);
      
      console.log(`Scheduling token refresh in ${Math.round(refreshTime / 1000)} seconds`);
      
      this.refreshTimer = setTimeout(() => {
        this.refreshAccessToken().catch(error => {
          console.error('Scheduled token refresh failed:', error);
        });
      }, refreshTime);
    }
  }


  // ==================== Public API Methods with Token Management ====================

  async authenticatedFetch(url, options = {}) {
    // Check if token is about to expire
    if (this.tokenExpiryTime && (this.tokenExpiryTime - Date.now()) < this.refreshThresholdMs) {
      console.log('Token about to expire, refreshing...');
      await this.refreshAccessToken();
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.accessToken}`,
      ...options.headers,
    };

    const response = await fetch(url, {
      ...options,
      headers,
    });

    // If unauthorized, try to refresh token and retry once
    if (response.status === 401) {
      console.log('Received 401, attempting token refresh...');
      try {
        await this.refreshAccessToken();
        // Retry with new token
        headers['Authorization'] = `Bearer ${this.accessToken}`;
        const retryResponse = await fetch(url, {
          ...options,
          headers,
        });
        return retryResponse;
      } catch (error) {
        this.clearAuth();
        throw new Error('Session expired. Please login again.');
      }
    }

    // Track activity on API calls
    this.handleUserActivity();

    return response;
  }

  async get(url) {
    return this.authenticatedFetch(url, { method: 'GET' });
  }

  async post(url, data) {
    return this.authenticatedFetch(url, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async put(url, data) {
    return this.authenticatedFetch(url, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async delete(url) {
    return this.authenticatedFetch(url, { method: 'DELETE' });
  }

  
  isAuthenticated() {
    return !!this.accessToken && !!this.refreshToken;
  }

  getAccessToken() {
    return this.accessToken;
  }

  getRefreshToken() {
    return this.refreshToken;
  }

  getUserInfo() {
    return this.userInfo;
  }

  getTokenExpiryTime() {
    return this.tokenExpiryTime;
  }

  clearAuth() {
    console.log('Clearing authentication data');
    this.accessToken = null;
    this.refreshToken = null;
    this.userInfo = null;
    this.tokenExpiryTime = null;
    
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
    
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userInfo');
      localStorage.removeItem('tokenExpiryTime');
      console.log('Tokens removed from localStorage');
    }
  }

  destroy() {
    if (typeof document !== 'undefined') {
    }
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
  }
}

// Create and export singleton instance
const authService = new AuthService();

// Initialize only in browser environment, but DON'T auto-connect WebSocket
if (typeof window !== 'undefined') {
  // Only load tokens, don't connect WebSocket
  authService.initialize().catch(console.error);
}

export default authService;