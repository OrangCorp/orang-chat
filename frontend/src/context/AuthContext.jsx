import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true); // Start true to check storage

  // Check for saved user on initial load
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false); // Done checking storage
  }, []);

  // Placeholder functions - just return true/false for now
  const login = async (email, password) => {
    setLoading(true);
    
    // TEMP: Simulate successful login
    const userData = { 
      id: 1, 
      name: 'Test User', 
      email,
      // You could add more test data here
      avatar: 'https://i.pravatar.cc/150?u=1',
      createdAt: new Date().toISOString()
    };
    
    // Save to state AND localStorage
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
    
    setLoading(false);
    return true;
  };

  const logout = () => {
    // Clear from state AND localStorage
    setUser(null);
    localStorage.removeItem('user');
  };

  const signup = async (userData) => {
    setLoading(true);
    
    // TEMP: Simulate successful signup
    const newUser = { 
      id: Date.now(), // Simple unique ID
      ...userData,
      createdAt: new Date().toISOString()
    };
    
    // Save to state AND localStorage
    setUser(newUser);
    localStorage.setItem('user', JSON.stringify(newUser));
    
    setLoading(false);
    return true;
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      logout,
      signup,
      isAuthenticated: !!user // Helper boolean
    }}>
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