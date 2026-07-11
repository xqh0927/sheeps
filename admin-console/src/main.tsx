import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import App from './App';
import { useAuth } from './store/auth';

// 应用启动即同步恢复登录态：从 localStorage 读取 token/user，
// 保证整页刷新后用户会话不丢失（须在渲染前执行，避免首屏闪烁未登录态）
useAuth.getState().initFromStorage();

// 全局 MUI 主题：定义品牌主色（紫）、辅助色（琥珀），统一圆角为 10px
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#6a4c93' },
    secondary: { main: '#b07d3c' },
  },
  shape: { borderRadius: 10 },
});

// 挂载入口：
// 1) React.StrictMode —— 开发期双重调用副作用以暴露隐患
// 2) ThemeProvider —— 向全树注入主题；CssBaseline 统一浏览器默认样式
// 3) BrowserRouter —— 提供 HTML5 history 路由上下文，App 内的 <Routes> 依赖此环境
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
