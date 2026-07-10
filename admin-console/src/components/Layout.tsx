import { useEffect } from 'react';
import { Outlet, useNavigate, useLocation, NavLink } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  IconButton,
  Tooltip,
  Chip,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  Collections as CollectionsIcon,
  Extension as ExtensionIcon,
  Campaign as CampaignIcon,
  Assignment as AssignmentIcon,
  GridView as GridViewIcon,
  Settings as SettingsIcon,
  History as HistoryIcon,
  Logout as LogoutIcon,
  SystemUpdateAlt as SystemUpdateAltIcon,
  Translate as TranslateIcon,
  Leaderboard as LeaderboardIcon,
} from '@mui/icons-material';
import { useAuth } from '../store/auth';
import { setSessionExpiredHandler } from '../api/client';

const DRAWER_WIDTH = 230;

interface NavItem {
  to: string;
  label: string;
  icon: JSX.Element;
  superOnly?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: '概览', icon: <DashboardIcon /> },
  { to: '/users', label: '用户管理', icon: <PeopleIcon /> },
  { to: '/skin-products', label: '卡片皮肤管理', icon: <CollectionsIcon /> },
  { to: '/prop-products', label: '道具管理', icon: <ExtensionIcon /> },
  { to: '/notices', label: '公告管理', icon: <CampaignIcon /> },
  { to: '/tasks', label: '任务管理', icon: <AssignmentIcon /> },
  { to: '/levels', label: '关卡管理', icon: <GridViewIcon /> },
  { to: '/app-versions', label: 'App 版本', icon: <SystemUpdateAltIcon /> },
  { to: '/i18n', label: '多语言管理', icon: <TranslateIcon /> },
  { to: '/leaderboards', label: '排行榜管理', icon: <LeaderboardIcon /> },
  { to: '/config', label: '系统配置', icon: <SettingsIcon /> },
  { to: '/audit-logs', label: '审计日志', icon: <HistoryIcon />, superOnly: true },
];

const ROLE_LABEL: Record<string, string> = {
  super: '超级管理员',
  operator: '运营',
  readonly: '只读',
};

export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, isSuper } = useAuth();

  // 注册会话过期回调：refresh 失败 -> 清态跳登录
  useEffect(() => {
    setSessionExpiredHandler(() => {
      logout();
      navigate('/login', { replace: true });
    });
  }, [logout, navigate]);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            秘境消消乐 · 管理后台
          </Typography>
          {user && (
            <Chip
              size="small"
              color={isSuper() ? 'secondary' : 'default'}
              label={`${user.username || user.phone} · ${ROLE_LABEL[user.role] || user.role}`}
              sx={{ mr: 1 }}
            />
          )}
          <Tooltip title="退出登录">
            <IconButton color="inherit" onClick={handleLogout} aria-label="logout">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          <List>
            {NAV_ITEMS.filter((item) => !item.superOnly || isSuper()).map((item) => (
              <ListItemButton
                key={item.to}
                component={NavLink}
                to={item.to}
                selected={location.pathname.startsWith(item.to)}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
          <Divider />
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3, bgcolor: 'grey.50' }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
