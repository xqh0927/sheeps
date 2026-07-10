import { useEffect, useState, useCallback } from 'react';
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
  TablePagination,
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
  Tabs,
  Tab,
  Chip,
} from '@mui/material';
import { Add, Edit, Delete } from '@mui/icons-material';
import {
  listLeaderboard,
  createLeaderboardRow,
  updateLeaderboardRow,
  deleteLeaderboardRow,
  getGameModes,
  LeaderboardRow,
} from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';
import UserPicker from '../components/UserPicker';

interface FormState {
  user_id: string;
  username: string;
  level_id: string;
  score: string;
  clear_time_ms: string;
}

const emptyForm: FormState = { user_id: '', username: '', level_id: '0', score: '0', clear_time_ms: '0' };

export default function LeaderboardManager() {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  const [tab, setTab] = useState(0); // 0=闯关榜 1=无尽生存榜
  const [rows, setRows] = useState<LeaderboardRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [disabled, setDisabled] = useState(false);
  const [modes, setModes] = useState<{ stage: boolean; endless: boolean }>({ stage: true, endless: false });

  const [levelFilter, setLevelFilter] = useState('');
  const [dialog, setDialog] = useState<null | { mode: 'create' | 'edit'; row?: LeaderboardRow }>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<LeaderboardRow | null>(null);

  const loadModes = useCallback(() => {
    getGameModes()
      .then((d) => setModes(d))
      .catch(() => { /* 忽略，使用默认 */ });
  }, []);

  const load = useCallback(() => {
    const gameMode = tab;
    // 无尽榜且模式关闭：不查，展示禁用提示
    if (gameMode === 1 && !modes.endless) {
      setRows([]);
      setTotal(0);
      setDisabled(true);
      setLoading(false);
      return;
    }
    setDisabled(false);
    setLoading(true);
    listLeaderboard({
      game_mode: gameMode,
      page: page + 1,
      pageSize,
      level_id: levelFilter ? Number(levelFilter) : undefined,
    })
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
        setDisabled(false);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [tab, modes.endless, page, pageSize, levelFilter, show]);

  useEffect(() => {
    loadModes();
  }, [loadModes]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setForm(emptyForm);
    setDialog({ mode: 'create' });
  };

  const openEdit = (row: LeaderboardRow) => {
    setForm({
      user_id: String(row.user_id),
      username: row.username || '',
      level_id: String(row.level_id),
      score: String(row.score),
      clear_time_ms: String(row.clear_time_ms),
    });
    setDialog({ mode: 'edit', row });
  };

  const handleSave = async () => {
    if (!form.user_id) {
      show('请选择用户', 'warning');
      return;
    }
    const body = {
      user_id: form.user_id,
      level_id: Number(form.level_id) || 0,
      score: Number(form.score) || 0,
      clear_time_ms: Number(form.clear_time_ms) || 0,
      game_mode: tab,
    };
    setBusy(true);
    try {
      if (dialog?.mode === 'edit' && dialog.row) {
        await updateLeaderboardRow(dialog.row.id, {
          score: body.score,
          clear_time_ms: body.clear_time_ms,
          level_id: body.level_id,
        });
        show('已保存', 'success');
      } else {
        await createLeaderboardRow(body);
        show('已添加', 'success');
      }
      setDialog(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await deleteLeaderboardRow(deleteTarget.id);
      show('已删除', 'success');
      setDeleteTarget(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'username', label: '用户' },
    { key: 'level_id', label: '关卡' },
    { key: 'score', label: '分数' },
    { key: 'clear_time_ms', label: '耗时(ms)' },
    { key: 'achieved_at', label: '达成时间' },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} spacing={2}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          排行榜管理
        </Typography>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <TextField
            size="small"
            label="关卡筛选"
            placeholder="关卡ID"
            value={levelFilter}
            onChange={(e) => { setLevelFilter(e.target.value); setPage(0); }}
            sx={{ minWidth: 140 }}
          />
          <Button variant="contained" startIcon={<Add />} disabled={!canWrite || disabled} onClick={openCreate}>
            手动新增
          </Button>
        </Stack>
      </Stack>

      <Tabs value={tab} onChange={(_, v) => { setTab(v); setPage(0); }} sx={{ mb: 2 }}>
        <Tab label="关卡榜" />
        <Tab label="无尽生存榜" disabled={!modes.endless} />
      </Tabs>

      {tab === 1 && !modes.endless && (
        <Chip color="warning" label="无尽生存模式未开启（在「系统配置 → 游戏模式开关」中开启后可用）" sx={{ mb: 2 }} />
      )}

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {columns.map((c) => (
                  <TableCell key={c.key}>{c.label}</TableCell>
                ))}
                {canWrite && <TableCell align="right">操作</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 5 }} style={{ color: '#999' }}>
                    {disabled ? '该模式未开启' : '暂无数据'}
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.id}</TableCell>
                    <TableCell>{r.username || r.user_id}</TableCell>
                    <TableCell>{r.level_id}</TableCell>
                    <TableCell>{r.score}</TableCell>
                    <TableCell>{r.clear_time_ms}</TableCell>
                    <TableCell>{r.achieved_at ? new Date(r.achieved_at).toLocaleString() : '—'}</TableCell>
                    {canWrite && (
                      <TableCell align="right">
                        <Tooltip title="编辑">
                          <IconButton size="small" onClick={() => openEdit(r)}>
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="删除">
                          <IconButton size="small" color="error" onClick={() => setDeleteTarget(r)}>
                            <Delete fontSize="small" />
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
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_, p) => setPage(p)}
          rowsPerPage={pageSize}
          onRowsPerPageChange={(e) => { setPageSize(Number(e.target.value)); setPage(0); }}
          rowsPerPageOptions={[10, 20, 50, 100]}
        />
      </Card>

      {/* 新增 / 编辑弹窗 */}
      <Dialog open={dialog !== null} onClose={() => !busy && setDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{dialog?.mode === 'edit' ? '编辑分数' : '手动新增排行榜行'}</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          <Box sx={{ mb: 1 }}>
            <UserPicker
              value={form.user_id}
              initialLabel={form.username}
              label="用户（手机号/昵称搜索）"
              onChange={(userId, username) => setForm((p) => ({ ...p, user_id: userId, username: username || '' }))}
            />
          </Box>
          <TextField
            fullWidth
            margin="normal"
            type="number"
            label="关卡 ID（无尽榜填 0）"
            value={form.level_id}
            onChange={(e) => setForm((p) => ({ ...p, level_id: e.target.value }))}
          />
          <TextField
            fullWidth
            margin="normal"
            type="number"
            label="分数"
            value={form.score}
            onChange={(e) => setForm((p) => ({ ...p, score: e.target.value }))}
          />
          <TextField
            fullWidth
            margin="normal"
            type="number"
            label="耗时(ms)"
            value={form.clear_time_ms}
            onChange={(e) => setForm((p) => ({ ...p, clear_time_ms: e.target.value }))}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)} disabled={busy}>取消</Button>
          <Button variant="contained" onClick={handleSave} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 删除确认 */}
      <Dialog open={!!deleteTarget} onClose={() => !busy && setDeleteTarget(null)}>
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>
          确定要删除「用户 {deleteTarget?.username || deleteTarget?.user_id} · 分数 {deleteTarget?.score}」的排行榜记录吗？此操作不可撤销。
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={busy}>取消</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '删除'}
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
