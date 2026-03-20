import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initializeAuth = async () => {
      const token = localStorage.getItem('accessToken');
      const savedUser = localStorage.getItem('user');
      
      if (token && savedUser) {
        setUser(JSON.parse(savedUser));
      }
      setLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error('Invalid email or password');
        } else if (response.status === 400) {
          throw new Error(data.message || 'Validation failed');
        } else {
          throw new Error('Login failed. Please try again.');
        }
      }

      const userData = {
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        accessToken: data.accessToken,
        tokenType: data.tokenType,
      };

      setUser(userData);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('user', JSON.stringify(userData));
      
      setLoading(false);
      return true;
      
    } catch (err) {
      setError(err.message);
      setLoading(false);
      return false;
    }
  };

  const logout = () => {
    setUser(null);
    setError(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
  };

  const signup = async (email, password, displayName) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: email,
          password: password,
          displayName: displayName,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        if (response.status === 409) {
          throw new Error('Email already registered');
        } else if (response.status === 400) {
          if (data.errors) {
            const errorMessages = Object.entries(data.errors)
              .map(([field, message]) => `${field}: ${message}`)
              .join(', ');
            throw new Error(errorMessages);
          } else {
            throw new Error(data.message || 'Registration failed');
          }
        } else {
          throw new Error('Registration failed. Please try again.');
        }
      }

      const newUser = {
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
        accessToken: data.accessToken,
        tokenType: data.tokenType,
      };

      setUser(newUser);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('user', JSON.stringify(newUser));
      
      setLoading(false);
      return true;
      
    } catch (err) {
      setError(err.message);
      setLoading(false);
      return false;
    }
  };

  const fetchWithAuth = async (url, options = {}) => {
    const token = localStorage.getItem('accessToken');
    
    return fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      error,
      login,
      logout,
      signup,
      fetchWithAuth,
      isAuthenticated: !!user
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