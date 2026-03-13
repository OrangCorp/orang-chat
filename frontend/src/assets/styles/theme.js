import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#f99500',
    },
    secondary: {
      main: '#1a7610',
    },
    background: {
      default: '#e8dfcb',     // Page background
      paper: '#e8dfcb',        // Card/Paper background
      // You can add custom colors too
      form: '#e8dfcb',         // Custom - for your form background
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

export default theme;