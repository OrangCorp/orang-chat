import { createContext, useContext } from 'react';

// Create context with default value
const ThemeContext = createContext();

// Custom hook for using theme context
export const useTheme = () => {
  const context = useContext(ThemeContext);
  return context;
};

// Provider component
export const ThemeContextProvider = ({ children }) => {
  // Empty for now - just passing children through
  return (
    <ThemeContext.Provider value={{}}>
      {children}
    </ThemeContext.Provider>
  );
};