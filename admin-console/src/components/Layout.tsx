import { Suspense, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Box, CircularProgress, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon,
  ListItemText, Stack, Tooltip, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import {
  AssignmentOutlined, CampaignOutlined, CollectionsOutlined, ExtensionOutlined,
  GridViewOutlined, HistoryOutlined, LeaderboardOutlined, LogoutRounded, MenuRounded,
  PeopleAltOutlined, SettingsOutlined, SpaceDashboardOutlined, SystemUpdateAltOutlined,
  TranslateOutlined,
} from '@mui/icons-material';
import { useAuth } from '../store/auth';
import { setSessionExpiredHandler } from '../api/client';

const railWidth = 236;
const navigation = [
  { title: '工作台', items: [
    { to: '/dashboard', label: '运营概览', icon: SpaceDashboardOutlined },
    { to: '/users', label: '用户管理', icon: PeopleAltOutlined },
    { to: '/leaderboards', label: '排行榜', icon: LeaderboardOutlined },
  ] },
  { title: '游戏内容', items: [
    { to: '/levels', label: '关卡', icon: GridViewOutlined },
    { to: '/tasks', label: '任务', icon: AssignmentOutlined },
    { to: '/notices', label: '公告', icon: CampaignOutlined },
  ] },
  { title: '商城', items: [
    { to: '/skin-products', label: '卡片皮肤', icon: CollectionsOutlined },
    { to: '/prop-products', label: '道具', icon: ExtensionOutlined },
  ] },
  { title: '系统', items: [
    { to: '/app-versions', label: 'App 版本', icon: SystemUpdateAltOutlined },
    { to: '/i18n', label: '多语言', icon: TranslateOutlined },
    { to: '/config', label: '系统配置', icon: SettingsOutlined },
    { to: '/audit-logs', label: '审计日志', icon: HistoryOutlined },
  ] },
];
const roleNames = { super: '超级管理员', operator: '运营', readonly: '只读', user: '玩家' };

export default function Layout() {
  const { user, logout, isSuper } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const desktop = useMediaQuery(useTheme().breakpoints.up('md'));
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => { logout(); navigate('/login', { replace: true }); });
  }, [logout, navigate]);
  useEffect(() => { setOpen(false); }, [pathname, desktop]);

  const name = user?.username || user?.phone || '';
  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh' }}>
      <Drawer variant={desktop ? 'permanent' : 'temporary'} open={desktop || open}
        onClose={() => setOpen(false)} PaperProps={{ component: 'aside', 'aria-label': '工作区' }}
        sx={{ width: desktop ? railWidth : 0, flexShrink: 0, '& .MuiDrawer-paper': {
          width: railWidth, bgcolor: '#f9fbf8', borderRightColor: 'divider',
        } }}>
        <Stack direction="row" alignItems="center" spacing={1.25} sx={{ px: 2.5, pt: 3, pb: 2.25 }}>
          <Box component="img" src="/ic_launcher_round.webp" alt="" sx={{ width: 42, height: 42 }} />
          <Box>
            <Typography fontSize={17} lineHeight={1.4} fontWeight={750}>秘境消消乐</Typography>
            <Typography fontSize={12} color="text.secondary">管理后台</Typography>
          </Box>
        </Stack>
        <Box component="nav" aria-label="主导航" sx={{ flex: 1, overflowY: 'auto', p: '0 14px 16px' }}>
          {navigation.map(group => (
            <Box key={group.title}>
              <Typography component="h2" sx={{ color: 'text.secondary', px: 1.5, mt: 1.8, mb: .6, fontSize: 11, letterSpacing: '.08em', fontWeight: 650 }}>
                {group.title}
              </Typography>
              <List disablePadding>
                {group.items.filter(item => item.to !== '/audit-logs' || isSuper()).map(item => (
                  <ListItemButton key={item.to} component={NavLink} to={item.to} end
                    selected={pathname === item.to} onClick={() => setOpen(false)}
                    sx={{ px: 1.5, py: .5, minHeight: 38, my: .15, borderRadius: '9px', color: '#41534b',
                      '&.Mui-selected, &.Mui-selected:hover': { bgcolor: '#def0e5', color: '#12543b' } }}>
                    <ListItemIcon sx={{ minWidth: 32, color: 'inherit' }}><item.icon fontSize="small" /></ListItemIcon>
                    <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: 14, fontWeight: pathname === item.to ? 700 : 500 }} />
                  </ListItemButton>
                ))}
              </List>
            </Box>
          ))}
        </Box>
        <Divider />
        <Stack direction="row" alignItems="center" spacing={1} sx={{ p: 1.75 }}>
          <Box aria-hidden sx={{ width: 34, height: 34, bgcolor: '#ddede2', color: '#235940', borderRadius: '50%', display: 'grid', placeItems: 'center', fontWeight: 700 }}>
            {Array.from(name)[0]}
          </Box>
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography noWrap fontWeight={650} fontSize={13}>{name}</Typography>
            <Typography fontSize={12} color="text.secondary">{user && roleNames[user.role]}</Typography>
          </Box>
          <Tooltip title="退出登录"><IconButton size="small" aria-label="退出登录" onClick={() => {
            logout(); navigate('/login', { replace: true });
          }}><LogoutRounded fontSize="small" /></IconButton></Tooltip>
        </Stack>
      </Drawer>
      <Box sx={{ minWidth: 0, flex: 1 }}>
        {!desktop && <Stack direction="row" alignItems="center" spacing={1} sx={{
          position: 'sticky', top: 0, zIndex: 10, bgcolor: 'background.paper', borderBottom: 1,
          borderColor: 'divider', minHeight: 54, px: 1.25,
        }}>
          <IconButton aria-label="打开主导航" aria-expanded={open} onClick={() => setOpen(true)}><MenuRounded /></IconButton>
          <Typography fontWeight={700}>秘境消消乐</Typography>
          <Typography color="text.secondary" fontSize={13}>管理后台</Typography>
        </Stack>}
        <Box component="main" sx={{ width: '100%', maxWidth: 1540, mx: 'auto', p: { xs: 2, sm: 3, lg: 4 } }}>
          <Suspense fallback={<Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
            <CircularProgress size={26} aria-label="正在加载页面" />
          </Box>}>
            <Outlet />
          </Suspense>
        </Box>
      </Box>
    </Box>
  );
}
