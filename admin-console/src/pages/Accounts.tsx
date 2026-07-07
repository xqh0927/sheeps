import { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TableContainer,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Stack,
  Alert,
} from '@mui/material';
import { Add, Block, CheckCircle } from '@mui/icons-material';
import { listAccounts, createAccount, setAccountRole, disableAccount, AccountRow } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';

const ROLE_OPTIONS = ['super', 'operator', 'readonly'];
const ROLE_LABEL: Record<string, string> = { super: '超级管理员', operator: '运营', readonly: '只读' };

export default function Accounts() {
  const isSuper = useAuth((s) => s.isSuper());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<AccountRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState('operator');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true);
    listAccounts()
      .then((d) => setRows(d.list))
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (isSuper) load();
  }, [isSuper]); // eslint-disable-line

  if (!isSuper) {
    return (
      <Box>
        <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>管理员账户管理</Typography>
        <Alert severity="warning">仅超级管理员可访问此页面。</Alert>
      </Box>
    );
  }

  const handleCreate = async () => {
    if (!phone.trim()) {
      show('请输入手机号', 'warning');
      return;
    }
    setBusy(true);
    try {
      const r = await createAccount(phone.trim(), role, password.trim() || undefined);
      if (r?.initialPassword) show(`创建成功，初始密码：${r.initialPassword}（请妥善转交）`, 'success');
      else show('创建成功', 'success');
      setCreating(false);
      setPhone('');
      setPassword('');
      setRole('operator');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleRole = async (row: AccountRow, newRole: string) => {
    try {
      await setAccountRole(row.id, newRole);
      show('角色已更新', 'success');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  const handleDisable = async (row: AccountRow, disabled: boolean) => {
    try {
      await disableAccount(row.id, disabled);
      show(disabled ? '已禁用' : '已启用', 'success');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>管理员账户管理</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreating(true)}>
          新增管理员
        </Button>
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>手机号</TableCell>
                <TableCell>昵称</TableCell>
                <TableCell>角色</TableCell>
                <TableCell>状态</TableCell>
                <TableCell>创建时间</TableCell>
                <TableCell align="right">操作</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.phone}</TableCell>
                    <TableCell>{r.username}</TableCell>
                    <TableCell>
                      <Chip size="small" label={ROLE_LABEL[r.role] || r.role} color={r.role === 'super' ? 'secondary' : 'primary'} />
                    </TableCell>
                    <TableCell>
                      {r.is_banned === 1 ? <Chip size="small" label="已禁用" color="error" /> : <Chip size="small" label="正常" color="success" />}
                    </TableCell>
                    <TableCell>{r.created_at ? new Date(r.created_at).toLocaleString() : '-'}</TableCell>
                    <TableCell align="right">
                      <TextField
                        select
                        size="small"
                        value={r.role}
                        SelectProps={{ native: true }}
                        onChange={(e) => handleRole(r, e.target.value)}
                        sx={{ mr: 1, minWidth: 110 }}
                      >
                        {ROLE_OPTIONS.map((o) => (
                          <option key={o} value={o}>{ROLE_LABEL[o]}</option>
                        ))}
                      </TextField>
                      <Button
                        size="small"
                        color={r.is_banned === 1 ? 'success' : 'error'}
                        startIcon={r.is_banned === 1 ? <CheckCircle /> : <Block />}
                        onClick={() => handleDisable(r, r.is_banned !== 1)}
                      >
                        {r.is_banned === 1 ? '启用' : '禁用'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      <Dialog open={creating} onClose={() => setCreating(false)} maxWidth="sm" fullWidth>
        <DialogTitle>新增管理员</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          <TextField fullWidth margin="normal" label="手机号" value={phone} onChange={(e) => setPhone(e.target.value)} autoFocus />
          <TextField
            fullWidth
            margin="normal"
            select
            label="角色"
            value={role}
            SelectProps={{ native: true }}
            onChange={(e) => setRole(e.target.value)}
          >
            {ROLE_OPTIONS.map((o) => (
              <option key={o} value={o}>{ROLE_LABEL[o]}</option>
            ))}
          </TextField>
          <TextField
            fullWidth
            margin="normal"
            label="初始密码（留空则系统随机生成）"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreating(false)} disabled={busy}>取消</Button>
          <Button variant="contained" onClick={handleCreate} disabled={busy}>创建</Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
