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

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="users" element={<Users />} />
        <Route path="skin-products" element={<SkinProducts />} />
        <Route path="prop-products" element={<PropProducts />} />
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
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
