// AuthContext.js - Fixed infinite loop
import { createContext, useContext, useState, useEffect, useRef } from 'react';
import authService from '../services/authService';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [webSocketConnected, setWebSocketConnected] = useState(false);
  
  // Use ref to track if we've already checked auth to prevent loops
  const authCheckRef = useRef(false);

  useEffect(() => {
    const initializeAuth = async () => {
      // Prevent multiple initializations
      if (authCheckRef.current) return;
      authCheckRef.current = true;
      
      // Just load tokens, don't connect WebSocket yet
      if (authService.isAuthenticated()) {
        const userInfo = authService.getUserInfo();
        if (userInfo) {
          setUser({
            id: userInfo.userId,
            email: userInfo.email,
            displayName: userInfo.displayName,
            accessToken: authService.getAccessToken(),
            tokenType: 'Bearer',
          });
        }
      }
      setLoading(false);
    };

    initializeAuth();

    // Set up an interval to check if token is still valid
    // But don't cause re-renders unnecessarily
    const checkAuthInterval = setInterval(() => {
      const isAuth = authService.isAuthenticated();
      if (!isAuth && user) {
        // Only update state if user exists but auth is gone
        setUser(null);
        localStorage.removeItem('user');
        setWebSocketConnected(false);
      }
    }, 30000); // Check every 30 seconds instead of 5

    return () => {
      clearInterval(checkAuthInterval);
    };
  }, []); // Empty dependency array - only run once on mount

  // Separate effect for token refresh listener
  useEffect(() => {
    // Listen for token refresh events to update user state
    const handleTokenRefresh = () => {
      if (user) {
        setUser(prevUser => ({
          ...prevUser,
          accessToken: authService.getAccessToken(),
        }));
        
        // Update stored user
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const userData = JSON.parse(storedUser);
          userData.accessToken = authService.getAccessToken();
          localStorage.setItem('user', JSON.stringify(userData));
        }
      }
    };

    // You'll need to add an event emitter to authService for this
    // For now, we can use a simple interval to sync token
    const tokenSyncInterval = setInterval(() => {
      if (user && authService.getAccessToken() !== user.accessToken) {
        handleTokenRefresh();
      }
    }, 5000);

    return () => {
      clearInterval(tokenSyncInterval);
    };
  }, [user]); // Only depends on user, but won't cause loops because we're not setting user unconditionally

  const connectWebSocket = async () => {
    if (!user) {
      console.warn('Cannot connect WebSocket: No user authenticated');
      return false;
    }
    
    if (webSocketConnected) {
      console.log('WebSocket already connected');
      return true;
    }
    
    try {
      await authService.connectWebSocket();
      setWebSocketConnected(true);
      return true;
    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
      setWebSocketConnected(false);
      return false;
    }
  };

  const disconnectWebSocket = () => {
    authService.disconnectWebSocket();
    setWebSocketConnected(false);
  };

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    
    try {
      const authResponse = await authService.login(email, password);
      
      const userData = {
        id: authResponse.userId,
        email: authResponse.email,
        displayName: authResponse.displayName,
        accessToken: authResponse.accessToken,
        tokenType: authResponse.tokenType || 'Bearer',
      };

      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
      
      setLoading(false);
      return true;
      
    } catch (err) {
      setError(err.message);
      setLoading(false);
      return false;
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setUser(null);
      setError(null);
      setWebSocketConnected(false);
      localStorage.removeItem('user');
    }
  };

  const signup = async (email, password, displayName) => {
    setLoading(true);
    setError(null);
    
    try {
      const authResponse = await authService.register(email, password, displayName);
      
      const newUser = {
        id: authResponse.userId,
        email: authResponse.email,
        displayName: authResponse.displayName,
        accessToken: authResponse.accessToken,
        tokenType: authResponse.tokenType || 'Bearer',
      };

      setUser(newUser);
      localStorage.setItem('user', JSON.stringify(newUser));
      
      setLoading(false);
      return true;
      
    } catch (err) {
      // Parse error message properly
      let errorMessage = 'Registration failed. Please try again.';
      
      if (err.message.includes('Email already registered')) {
        errorMessage = 'Email already registered';
      } else if (err.message.includes('Validation')) {
        errorMessage = err.message;
      } else if (err.message.includes('displayName')) {
        errorMessage = 'Invalid display name';
      } else if (err.message.includes('password')) {
        errorMessage = 'Password must be at least 8 characters';
      }
      
      setError(errorMessage);
      setLoading(false);
      return false;
    }
  };

  const fetchWithAuth = async (url, options = {}) => {
    try {
      const response = await authService.authenticatedFetch(url, options);
      return response;
    } catch (error) {
      console.error('Fetch with auth error:', error);
      throw error;
    }
  };

  const sendChatMessage = (messagePayload) => {
    authService.sendChatMessage(messagePayload);
  };

  const sendHeartbeat = () => {
    authService.sendHeartbeat();
  };

  const subscribeToTopic = (destination, callback) => {
    return authService.subscribeToTopic(destination, callback);
  };

  // Memoize the context value to prevent unnecessary re-renders
  const contextValue = {
    user,
    loading,
    error,
    webSocketConnected,
    login,
    logout,
    signup,
    fetchWithAuth,
    sendChatMessage,
    sendHeartbeat,
    subscribeToTopic,
    connectWebSocket,
    disconnectWebSocket,
    isAuthenticated: !!user,
    getAccessToken: () => authService.getAccessToken(),
    refreshToken: () => authService.refreshAccessToken(),
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};