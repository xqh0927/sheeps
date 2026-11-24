import loginTilesAvif from '../assets/login-tiles.avif';
import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { Visibility, VisibilityOff, ArrowForwardRounded } from '@mui/icons-material';
import { useAuth } from '../store/auth';
import { extractError } from '../api/client';

/**
 * 登录页面组件。
 *
 * 路由：受路由守卫保护前的入口页（通常路径 `/login`）。
 * 权限要求：无需登录即可访问；登录成功后由 `useAuth().login` 写入全局会话态并跳转。
 * 关键 State：
 * - `phone` / `password`：登录表单输入（手机号 + 密码）。
 * - `showPwd`：密码明文/密文切换。
 * - `error`：后端返回的登录错误文案（空字符串表示无错误）。
 * - `loading`：提交中的加载态，用于禁用按钮、防止重复提交。
 */
export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  // 从全局 auth store 取出登录动作；该动作成功后会持久化会话态（token 等）
  const login = useAuth((s) => s.login);

  // 表单受控输入：手机号
  const [phone, setPhone] = useState('');
  // 表单受控输入：密码
  const [password, setPassword] = useState('');
  // 密码可见性开关（仅本地 UI 状态，不影响数据流）
  const [showPwd, setShowPwd] = useState(false);
  // 登录失败时的错误提示文案（由 extractError 从异常中归一化得到）
  const [error, setError] = useState('');
  // 提交加载态：提交期间置 true，按钮禁用并阻止重复提交
  const [loading, setLoading] = useState(false);

  // 登录后回跳地址：优先取路由守卫传入的 from，否则回退到数据概览页
  const from = (location.state as { from?: string } | null)?.from || '/dashboard';

  // 表单提交：本地校验必填后调用 auth.login 发起登录
  // 成功则跳转回目标页（replace 避免回退到登录页）；失败将异常归一化为文案并进入 error 态
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!phone.trim() || !password) {
      setError('请输入手机号和密码');
      return;
    }
    setLoading(true);
    try {
      await login(phone.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100dvh', display: 'grid', placeItems: 'center', bgcolor: 'background.default', p: { xs: 2, md: 5 } }}>
      <Card sx={{ width: '100%', maxWidth: 990, display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
        borderRadius: '20px', overflow: 'hidden', boxShadow: '0 16px 60px #1631250e' }}>
        <Box sx={{ position: 'relative', minHeight: { xs: 160, sm: 215, md: 545 }, bgcolor: '#071b1d',
          overflow: 'hidden' }}>
          <picture>
            <source srcSet={loginTilesAvif} type="image/avif" />
            <Box component="img" src="/login-tiles.jpg" alt="" sx={{
              position: 'absolute', inset: 0, width: '100%', height: '100%',
              objectFit: 'cover', objectPosition: { xs: 'center 52%', md: 'center' },
            }} />
          </picture>
          <Box aria-hidden sx={{ position: 'absolute', inset: 0,
            background: 'linear-gradient(0deg, #061718cc, transparent 55%)', pointerEvents: 'none' }} />
          <Box sx={{ position: 'absolute', bottom: { xs: 16, md: 30 }, left: { xs: 22, md: 30 }, color: 'white' }}>
            <Typography fontWeight={700} fontSize={{ xs: 23, md: 30 }}>秘境消消乐</Typography>
            <Typography fontSize={14}>运营管理后台</Typography>
          </Box>
        </Box>
        <CardContent sx={{ alignSelf: 'center', p: { xs: 3, sm: 5, md: 6 }, '&:last-child': { pb: { xs: 3, sm: 5, md: 6 } } }}>
          <Typography variant="h5" component="h1" sx={{ mb: 3 }}>管理员登录</Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={handleSubmit} sx={{ display: 'grid', gap: 2.5 }}>
            <TextField fullWidth label="手机号" type="tel" autoComplete="username" value={phone}
              onChange={(e) => setPhone(e.target.value)} required />
            <TextField fullWidth label="密码" autoComplete="current-password" type={showPwd ? 'text' : 'password'}
              value={password} onChange={(e) => setPassword(e.target.value)} required
              InputProps={{ endAdornment: <InputAdornment position="end">
                <IconButton onClick={() => setShowPwd(!showPwd)} edge="end" aria-label={showPwd ? '隐藏密码' : '显示密码'}>
                  {showPwd ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment> }} />
            <Button type="submit" variant="contained" disabled={loading} size="large"
              endIcon={<ArrowForwardRounded />} sx={{ mt: 1, minHeight: 46 }}>
              {loading ? '登录中…' : '登录'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
