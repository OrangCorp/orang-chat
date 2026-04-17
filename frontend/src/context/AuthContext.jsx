// AuthContext.js - Activity-based presence only
import { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import authService from '../services/authService';
import chatService from '../services/chatService';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const activityTimeoutRef = useRef(null);
  const lastHeartbeatRef = useRef(0);
  const HEARTBEAT_THROTTLE_MS = 60000; // Max one heartbeat per minute

  const syncAuthState = useCallback(() => {
    const authIsAuthenticated = authService.isAuthenticated;
    const userInfo = authService.getUserInfo();
    
    setIsAuthenticated(authIsAuthenticated);
    
    if (authIsAuthenticated && userInfo) {
      const newUser = {
        id: userInfo.userId,
        email: userInfo.email,
        displayName: userInfo.displayName,
        accessToken: authService.getAccessToken(),
        tokenType: 'Bearer',
      };
      
      // Only update if something actually changed
      setUser(prev => {
        if (!prev) return newUser;
        if (prev.id === newUser.id && 
            prev.accessToken === newUser.accessToken &&
            prev.displayName === newUser.displayName) {
          return prev; // No change
        }
        return newUser;
      });
    } else if (!authIsAuthenticated) {
      setUser(prev => prev ? null : prev);
      localStorage.removeItem('user');
    }
  }, []);

  const sendActivityHeartbeat = useCallback(() => {
    if (!isAuthenticated || !chatService.isConnected()) return;
    
    const now = Date.now();
    if (now - lastHeartbeatRef.current > HEARTBEAT_THROTTLE_MS) {
      chatService.sendHeartbeat(false);
      lastHeartbeatRef.current = now;
    }
  }, [isAuthenticated]);

  // Handle user activity
  const handleUserActivity = useCallback(() => {
    // Clear existing timeout
    if (activityTimeoutRef.current) {
      clearTimeout(activityTimeoutRef.current);
    }
    
    // Send heartbeat immediately on activity
    sendActivityHeartbeat();
    
    // Set timeout to mark as inactive after 5 minutes of no activity
    // (This doesn't send anything, just clears the timeout reference)
    activityTimeoutRef.current = setTimeout(() => {
      activityTimeoutRef.current = null;
    }, 300000);
  }, [sendActivityHeartbeat]);

  // Sync with authService periodically
  useEffect(() => {
    syncAuthState();
    const interval = setInterval(syncAuthState, 2000);
    return () => clearInterval(interval);
  }, [syncAuthState]);

  // Initialize chat service when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      chatService.connect().then(() => {
        // Send initial heartbeat on connection
        chatService.sendHeartbeat(true);
        lastHeartbeatRef.current = Date.now();
      }).catch(console.error);
      
      // Set up activity listeners
      const activityEvents = ['mousedown', 'keydown', 'scroll', 'touchstart'];
      activityEvents.forEach(event => {
        window.addEventListener(event, handleUserActivity);
      });
      
      // Also track visibility changes (tab focus)
      const handleVisibilityChange = () => {
        if (!document.hidden) {
          handleUserActivity();
        }
      };
      document.addEventListener('visibilitychange', handleVisibilityChange);
      
      return () => {
        activityEvents.forEach(event => {
          window.removeEventListener(event, handleUserActivity);
        });
        document.removeEventListener('visibilitychange', handleVisibilityChange);
        
        if (activityTimeoutRef.current) {
          clearTimeout(activityTimeoutRef.current);
        }
        
        chatService.disconnect();
      };
    }
  }, [isAuthenticated, handleUserActivity]);

  const login = async (email, password) => {
    const result = await authService.login(email, password);
    syncAuthState();
    return result;
  };

  const logout = async () => {
    await authService.logout();
    syncAuthState();
  };

  const signup = async (email, password, displayName) => {
    const result = await authService.register(email, password, displayName);
    syncAuthState();
    return result;
  };

  const contextValue = {
    user,
    isAuthenticated,
    login,
    logout,
    signup,
    getAccessToken: () => authService.getAccessToken(),
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