// AuthService.js - With initialization tracking
import { Client } from '@stomp/stompjs';

class AuthService {
  constructor() {
    if (AuthService.instance) {
      return AuthService.instance;
    }

    this.isAuthenticated = false;
    this.isInitialized = false;  // NEW: Track if initial auth check is complete
    this.initializationPromise = null;  // NEW: Promise for initialization

    this.accessToken = null;
    this.refreshToken = null;
    this.userInfo = null;
    
    // Token refresh configuration
    this.refreshTimer = null;
    this.refreshThresholdMs = 60000;
    this.tokenExpiryTime = null;
    
    // API endpoints
    this.apiBaseUrl = '/api';
    
    AuthService.instance = this;
  }

  // ==================== Initialization ====================
  
  async initialize() {
    // Return existing promise if already initializing
    if (this.initializationPromise) {
      return this.initializationPromise;
    }
    
    this.initializationPromise = this._doInitialize();
    return this.initializationPromise;
  }
  
  async _doInitialize() {
    try {
      console.log('AuthService: Starting initialization...');
      this.loadTokensFromStorage();
      
      // Wait for any pending token refresh to complete
      if (this.refreshPromise) {
        console.log('AuthService: Waiting for token refresh to complete...');
        await this.refreshPromise;
      }
      
      this.isInitialized = true;
      console.log('AuthService: Initialization complete, isAuthenticated:', this.isAuthenticated);
    } catch (error) {
      console.error('AuthService: Initialization failed:', error);
      this.isInitialized = true; // Still mark as initialized even on failure
      this.isAuthenticated = false;
    } finally {
      this.initializationPromise = null;
    }
  }

  loadTokensFromStorage() {
    if (typeof localStorage !== 'undefined') {
      const storedAccessToken = localStorage.getItem('accessToken');
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedUserInfo = localStorage.getItem('userInfo');
      const storedExpiry = localStorage.getItem('tokenExpiryTime');
      
      if (storedAccessToken && storedRefreshToken) {
        const tokenExpiryTime = storedExpiry ? parseInt(storedExpiry) : null;
        const isExpired = tokenExpiryTime && Date.now() >= tokenExpiryTime - 30000;
        
        this.accessToken = storedAccessToken;
        this.refreshToken = storedRefreshToken;
        this.userInfo = storedUserInfo ? JSON.parse(storedUserInfo) : null;
        this.tokenExpiryTime = tokenExpiryTime;
        this.isAuthenticated = true;
        
        if (isExpired) {
          console.log('Stored access token is expired, attempting to refresh...');
          this.refreshAccessToken(true)
            .then(() => {
              console.log('Successfully refreshed token on load');
            })
            .catch((error) => {
              console.error('Failed to refresh token on load, clearing auth:', error);
              this.isAuthenticated = false;
              this.clearAuth();
            });
        } else {
          console.log('Tokens loaded from storage, expires in', 
            Math.round((this.tokenExpiryTime - Date.now()) / 1000), 'seconds');
          this.scheduleTokenRefresh();
        }
      } else {
        this.isAuthenticated = false;
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
      this.isAuthenticated = false;
      this.clearAuth();
    }
  }

  handleAuthResponse(authResponse) {
    this.accessToken = authResponse.accessToken;
    this.refreshToken = authResponse.refreshToken;
    this.userInfo = {
      userId: authResponse.userId,
      email: authResponse.email,
      displayName: authResponse.displayName,
    };

    this.tokenExpiryTime = Date.now() + (authResponse.expiresIn * 1000);
    
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('accessToken', this.accessToken);
      localStorage.setItem('refreshToken', this.refreshToken);
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
      localStorage.setItem('tokenExpiryTime', this.tokenExpiryTime.toString());
      console.log('Tokens stored in localStorage');
    }

    this.isAuthenticated = true;
    this.scheduleTokenRefresh();
  }

  refreshPromise = null;
  
  async refreshAccessToken(silentFail = false) {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }
    
    // Prevent multiple simultaneous refresh attempts
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = this._doRefreshAccessToken(silentFail);
    
    try {
      return await this.refreshPromise;
    } finally {
      this.refreshPromise = null;
    }
  }
  
  async _doRefreshAccessToken(silentFail) {
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
      
      if (!silentFail) {
        this.isAuthenticated = false;
        this.clearAuth();
      }
      
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
        this.refreshAccessToken(false).catch(error => {
          console.error('Scheduled token refresh failed:', error);
        });
      }, refreshTime);
    }
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
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
  }
}

// Create and export singleton instance
const authService = new AuthService();

// Initialize in browser environment
if (typeof window !== 'undefined') {
  authService.initialize().catch(console.error);
}

export default authService;