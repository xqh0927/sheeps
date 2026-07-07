import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import ShopItems from './pages/ShopItems';
import Notices from './pages/Notices';
import Tasks from './pages/Tasks';
import Levels from './pages/Levels';
import Config from './pages/Config';
import Accounts from './pages/Accounts';
import AuditLogs from './pages/AuditLogs';

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
        <Route path="shop-items" element={<ShopItems />} />
        <Route path="notices" element={<Notices />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="levels" element={<Levels />} />
        <Route path="config" element={<Config />} />
        <Route path="accounts" element={<Accounts />} />
        <Route path="audit-logs" element={<AuditLogs />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
