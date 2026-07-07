import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import App from './App';
import { useAuth } from './store/auth';

// 从 localStorage 恢复登录态（页面刷新后不丢）
useAuth.getState().initFromStorage();

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#6a4c93' },
    secondary: { main: '#b07d3c' },
  },
  shape: { borderRadius: 10 },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>
);
