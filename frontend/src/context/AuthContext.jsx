import { createContext, useContext, useState } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  // Placeholder functions - just return true/false for now
  const login = async (email, password) => {
    // TEMP: Simulate successful login
    setUser({ id: 1, name: 'Test User', email });
    return true;
  };

  const logout = () => {
    setUser(null);
  };

  const signup = async (userData) => {
    // TEMP: Simulate successful signup
    setUser({ id: 1, ...userData });
    return true;
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      logout,
      signup
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);