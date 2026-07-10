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
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Stack,
  Tooltip,
} from '@mui/material';
import { Add, Edit, Save, Cached } from '@mui/icons-material';
import { getConfig, updateConfig, clearCache } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';
import GameModeSwitch from '../components/GameModeSwitch';

interface ConfigRow {
  key: string;
  value: string;
}

/** 系统配置 Key 的中文说明映射 */
const CONFIG_LABELS: Record<string, string> = {
  'level_2_unlock_points': '第2关解锁所需积分',
  'level_3_unlock_points': '第3关解锁所需积分',
  'level_4_unlock_points': '第4关解锁所需积分',
  'sign_rewards': '签到奖励积分（逗号分隔，7天）',
  'gamemode_stage': '闯关模式开关（on/off）',
  'gamemode_endless': '无尽生存模式开关（on/off）',
  'gamemode_battle': '对战模式开关（on/off）',
  'i18n_dual_write': 'i18n双写开关（已废弃）',
};

export default function Config() {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<ConfigRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<ConfigRow | null>(null);
  const [creating, setCreating] = useState(false);
  const [formKey, setFormKey] = useState('');
  const [formValue, setFormValue] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true);
    getConfig()
      .then((d) => setRows(d.list))
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []); // eslint-disable-line

  const openEdit = (row: ConfigRow) => {
    setFormKey(row.key);
    setFormValue(row.value);
    setEditing(row);
  };

  const openCreate = () => {
    setFormKey('');
    setFormValue('');
    setCreating(true);
  };

  const handleSave = async () => {
    if (!formKey.trim()) {
      show('请填写配置 key', 'warning');
      return;
    }
    setBusy(true);
    try {
      await updateConfig(formKey.trim(), formValue);
      show('已保存', 'success');
      setEditing(null);
      setCreating(false);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          系统配置
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<Cached />}
            disabled={!canWrite}
            onClick={async () => {
              try {
                const res = await clearCache();
                show(`已清空 ${res.deleted} 个缓存键`, 'success');
              } catch (e) {
                show(extractError(e), 'error');
              }
            }}
          >
            清空缓存
          </Button>
          <Button variant="contained" startIcon={<Add />} disabled={!canWrite} onClick={openCreate}>
            新增配置
          </Button>
        </Stack>
      </Stack>

      <GameModeSwitch />

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>配置 Key</TableCell>
                <TableCell>说明</TableCell>
                <TableCell>配置 Value</TableCell>
                {canWrite && <TableCell align="right">操作</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 5 }} color="text.secondary">
                    暂无配置
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r.key}>
                    <TableCell>{r.key}</TableCell>
                    <TableCell>{CONFIG_LABELS[r.key] || '-'}</TableCell>
                    <TableCell>
                      <Box component="span" sx={{ wordBreak: 'break-all' }}>{r.value}</Box>
                    </TableCell>
                    {canWrite && (
                      <TableCell align="right">
                        <Tooltip title="编辑">
                          <IconButton size="small" onClick={() => openEdit(r)}>
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      <Dialog open={editing !== null || creating} onClose={() => { setEditing(null); setCreating(false); }} maxWidth="sm" fullWidth>
        <DialogTitle>{creating ? '新增配置' : `编辑配置 · ${formKey}`}</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          <TextField
            fullWidth
            margin="normal"
            label="配置 Key"
            value={formKey}
            onChange={(e) => setFormKey(e.target.value)}
            disabled={!creating || busy}
          />
          <TextField
            fullWidth
            margin="normal"
            label="配置 Value"
            value={formValue}
            onChange={(e) => setFormValue(e.target.value)}
            disabled={busy}
            multiline
            minRows={2}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setEditing(null); setCreating(false); }} disabled={busy}>
            取消
          </Button>
          <Button variant="contained" startIcon={<Save />} onClick={handleSave} disabled={busy}>
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
