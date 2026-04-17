// AuthService.js - Simplified with just two boolean flags
class AuthService {
  constructor() {
    if (AuthService.instance) {
      return AuthService.instance;
    }

    this.attemptedAuth = false;  // Have we tried to authenticate?
    this.isAuthenticated = false; // Are we actually authenticated?

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
    console.log('AuthService: Starting initialization...');
    this.loadTokensFromStorage();
    this.attemptedAuth = true;
    console.log('AuthService: Initialization complete, isAuthenticated:', this.isAuthenticated);
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
          this.refreshAccessToken()
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
    this.attemptedAuth = true;
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
      this.isAuthenticated = false;
      throw error;
    }
  }

  async register(email, password, displayName) {
    this.attemptedAuth = true;
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

      const registrationResponse = await response.json();
      // Don't set isAuthenticated yet - need email verification
      return registrationResponse;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  }

  async verifyEmail(email, code) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/verify-email`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, code }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Verification failed');
      }

      const authResponse = await response.json();
      this.handleAuthResponse(authResponse);
      return authResponse;
    } catch (error) {
      console.error('Email verification error:', error);
      throw error;
    }
  }

  async resendVerification(email) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/resend-verification`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) {
        throw new Error('Failed to resend verification code');
      }

      return true;
    } catch (error) {
      console.error('Resend verification error:', error);
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

  async refreshAccessToken() {
    if (!this.refreshToken) {
      this.isAuthenticated = false;
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
      this.isAuthenticated = false;
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

  getAccessToken() {
    return this.accessToken;
  }

  getRefreshToken() {
    return this.refreshToken;
  }

  getUserInfo() {
    return this.userInfo;
  }

  clearAuth() {
    console.log('Clearing authentication data');
    this.accessToken = null;
    this.refreshToken = null;
    this.userInfo = null;
    this.tokenExpiryTime = null;
    this.isAuthenticated = false;
    
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