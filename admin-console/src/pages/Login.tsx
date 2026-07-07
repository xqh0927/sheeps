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

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuth((s) => s.login);

  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: string } | null)?.from || '/dashboard';

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
