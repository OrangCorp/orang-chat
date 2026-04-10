import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#F4991A',
    },
    secondary: {
      main: '#344F1F',
    },
    background: {
      default: '#F2EAD3',
      paper: '#F2EAD3',
      form: '#F2EAD3',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

export default theme;