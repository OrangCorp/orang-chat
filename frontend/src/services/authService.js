// AuthService.js - Simplified with just two boolean flags

const isDev = import.meta.env.DEV;
const log = (...args) => isDev && console.log(...args);
const logError = (...args) => isDev && console.error(...args);

class AuthService {
  constructor() {
    if (AuthService.instance) {
      return AuthService.instance;
    }

    this.attemptedAuth = false;
    this.isAuthenticated = false;

    this.accessToken = null;
    this.refreshToken = null;
    this.userInfo = null;
    
    this.refreshTimer = null;
    this.refreshThresholdMs = 60000;
    this.tokenExpiryTime = null;
    
    this.apiBaseUrl = '/api';
    
    AuthService.instance = this;
  }

  async initialize() {
    log('AuthService: Starting initialization...');
    this.loadTokensFromStorage();
    this.attemptedAuth = true;
    log('AuthService: Initialization complete, isAuthenticated:', this.isAuthenticated);
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
          log('Stored access token is expired, attempting to refresh...');
          this.refreshAccessToken()
            .then(() => log('Successfully refreshed token on load'))
            .catch(() => {
              this.isAuthenticated = false;
              this.clearAuth();
            });
        } else {
          log('Tokens loaded from storage, expires in', 
            Math.round((this.tokenExpiryTime - Date.now()) / 1000), 'seconds');
          this.scheduleTokenRefresh();
        }
      } else {
        this.isAuthenticated = false;
      }
    }
  }

  async login(email, password) {
    this.attemptedAuth = true;
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
      this.isAuthenticated = false;
      throw error;
    }
  }

  async register(email, password, displayName) {
    this.attemptedAuth = true;
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, displayName }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Registration failed');
      }

      return await response.json();
    } catch (error) {
      throw error;
    }
  }

  async verifyEmail(email, code) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/verify-email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
      throw error;
    }
  }

  async resendVerification(email) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) throw new Error('Failed to resend verification code');
      return true;
    } catch (error) {
      throw error;
    }
  }

  async logout() {
    try {
      if (this.accessToken) {
        await fetch(`${this.apiBaseUrl}/auth/logout`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${this.accessToken}` },
        });
      }
    } catch (error) {
      logError('Logout error:', error);
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
      log('Tokens stored in localStorage');
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
      log('Refreshing access token...');
      const response = await fetch(`${this.apiBaseUrl}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      });

      if (!response.ok) throw new Error('Token refresh failed');

      const authResponse = await response.json();
      this.handleAuthResponse(authResponse);
      log('Token refreshed successfully');
      return authResponse;
    } catch (error) {
      logError('Token refresh error:', error);
      this.isAuthenticated = false;
      this.clearAuth();
      throw error;
    }
  }

  scheduleTokenRefresh() {
    if (this.refreshTimer) clearTimeout(this.refreshTimer);

    if (this.tokenExpiryTime) {
      const timeUntilExpiry = this.tokenExpiryTime - Date.now();
      const refreshTime = Math.max(0, timeUntilExpiry - this.refreshThresholdMs);
      
      log(`Scheduling token refresh in ${Math.round(refreshTime / 1000)} seconds`);
      
      this.refreshTimer = setTimeout(() => {
        this.refreshAccessToken().catch(() => {});
      }, refreshTime);
    }
  }

  getAccessToken() { return this.accessToken; }
  getRefreshToken() { return this.refreshToken; }
  getUserInfo() { return this.userInfo; }

  clearAuth() {
    log('Clearing authentication data');
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
      log('Tokens removed from localStorage');
    }
  }

  destroy() {
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
  }

  async forgotPassword(email) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      if (!response.ok) throw new Error('Failed to send reset email');
      return true;
    } catch (error) {
      logError('Forgot password error:', error);
      throw error;
    }
  }

  async validateResetToken(token) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/reset-password/validate?token=${encodeURIComponent(token)}`, {
        method: 'GET',
      });
      return response.ok;
    } catch (error) {
      logError('Validate token error:', error);
      return false;
    }
  }

  async resetPassword(token, newPassword) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword }),
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to reset password');
      }
      return true;
    } catch (error) {
      logError('Reset password error:', error);
      throw error;
    }
  }
}

const authService = new AuthService();

if (typeof window !== 'undefined') {
  authService.initialize().catch(() => {});
}

export default authService;