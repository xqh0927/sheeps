import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import SkinProducts from './pages/SkinProducts';
import PropProducts from './pages/PropProducts';
import Notices from './pages/Notices';
import Tasks from './pages/Tasks';
import Levels from './pages/Levels';
import Config from './pages/Config';
import AuditLogs from './pages/AuditLogs';
import AppVersions from './pages/AppVersions';
import I18nManager from './pages/I18nManager';
import LeaderboardManager from './pages/LeaderboardManager';

/**
 * 应用根路由表（声明式路由）。
 *
 * 鉴权关系说明：
 * - `/login` 为公开路由，任何人可访问，无需登录态。
 * - 以 `"/"` 为父路由的整组子路由统一被 `<ProtectedRoute>` 包裹：
 *   其内部先做一次前端登录态拦截，未登录会被重定向到 `/login`；
 *   通过后再渲染 `<Layout>`（含顶栏、侧边栏与 `<Outlet/>`），由 Layout 输出各业务页面。
 * - 子路由 `dashboard/users/skin-products/...` 的访问权限由后端真正把关
 *   （requireAdmin / assertCanWrite / assertSuper），前端仅做体验层拦截。
 * - 兜底：访问 `"/"` 与任意未匹配路径 `"*"` 均重定向到 `/dashboard`
 *   （未登录时 ProtectedRoute 会进一步拦截并跳 `/login`）。
 */
export default function App() {
  return (
    <Routes>
      {/* 公开路由：登录页，无需鉴权 */}
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        {/* 默认子路由：访问根路径时重定向到概览页 */}
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="users" element={<Users />} />
        <Route path="skin-products" element={<SkinProducts />} />
        <Route path="prop-products" element={<PropProducts />} />
        {/* 历史兼容路由：旧的 shop-items 重定向到卡片皮肤管理 */}
        <Route path="shop-items" element={<Navigate to="/skin-products" replace />} />
        <Route path="notices" element={<Notices />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="levels" element={<Levels />} />
        <Route path="app-versions" element={<AppVersions />} />
        <Route path="i18n" element={<I18nManager />} />
        <Route path="leaderboards" element={<LeaderboardManager />} />
        <Route path="config" element={<Config />} />
        <Route path="audit-logs" element={<AuditLogs />} />
      </Route>
      {/* 兜底路由：任意未匹配路径重定向到概览页 */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
