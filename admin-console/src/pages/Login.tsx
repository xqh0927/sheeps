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
import { Visibility, VisibilityOff, LockOutlined } from '@mui/icons-material';
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
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #6a4c93 0%, #b07d3c 100%)',
        p: 2,
      }}
    >
      <Card sx={{ width: 380, maxWidth: '100%' }}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <LockOutlined sx={{ fontSize: 48, color: 'primary.main' }} />
            <Typography variant="h5" sx={{ mt: 1, fontWeight: 700 }}>
              管理后台登录
            </Typography>
            <Typography variant="body2" color="text.secondary">
              秘境消消乐 · 运营控制台
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="手机号"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              margin="normal"
              autoFocus
            />
            <TextField
              fullWidth
              label="密码"
              type={showPwd ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPwd((v) => !v)} edge="end" aria-label="toggle-password">
                      {showPwd ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 3 }}
            >
              {loading ? '登录中…' : '登录'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
