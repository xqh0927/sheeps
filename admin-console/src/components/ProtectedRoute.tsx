import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../store/auth';
import { Box, CircularProgress } from '@mui/material';

/**
 * 路由守卫：未登录跳 /login；已登录则渲染 children。
 * 仅做前端体验拦截，真正权限防线在后端（requireAdmin / assertCanWrite / assertSuper）。
 */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = useAuth((s) => s.token);
  const user = useAuth((s) => s.user);
  const location = useLocation();

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
