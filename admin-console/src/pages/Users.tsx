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
  TextField,
  Button,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Stack,
  FormControlLabel,
  Switch,
  Grid,
  FormLabel,
} from '@mui/material';
import { Search, Add, Block, CheckCircle, Edit, ShoppingBag } from '@mui/icons-material';
import { listUsers, adjustPoints, banUser, renameUser, AdminUserRow, listUserItems, updateUserItems } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';

const PROPS_DEFINITIONS = [
  { type: 'UNDO', label: '乾坤符 (Undo)' },
  { type: 'MOVEOUT', label: '缩地咒 (MoveOut)' },
  { type: 'SHUFFLE', label: '流沙契 (Shuffle)' },
  { type: 'REVIVE', label: '还魂丹 (Revive)' },
  { type: 'HINT', label: '天眼符 (Hint)' },
  { type: 'BOMB', label: '雷震子 (Bomb)' },
  { type: 'JOKER', label: '太极牌 (Joker)' },
  { type: 'DOUBLE_POINTS', label: '双倍符 (Double)' },
  { type: 'FREEZE', label: '冻结符 (Freeze)' }
];

const SKINS_DEFINITIONS = [
  { type: 'SKIN_INK', label: '水墨江山' },
  { type: 'SKIN_CYBER', label: '赛博霓虹' },
  { type: 'SKIN_HENAN', label: '河南·省味' },
  { type: 'SKIN_SICHUAN', label: '四川·省味' },
  { type: 'SKIN_SHUANG', label: '萌趣竞技' }
];

export default function Users() {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<AdminUserRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  const [assetTarget, setAssetTarget] = useState<AdminUserRow | null>(null);
  const [assetForm, setAssetForm] = useState<Record<string, number>>({});

  const [pointsTarget, setPointsTarget] = useState<AdminUserRow | null>(null);
  const [pointsAmount, setPointsAmount] = useState('');
  const [pointsReason, setPointsReason] = useState('');
  const [renameTarget, setRenameTarget] = useState<AdminUserRow | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    listUsers(page + 1, pageSize, keyword.trim())
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [page, pageSize, keyword, show]);

  useEffect(() => {
    load();
  }, [load]);

  const handleAdjust = async () => {
    if (!pointsTarget) return;
    const amount = Number(pointsAmount);
    if (!Number.isFinite(amount) || amount === 0) {
      show('请输入非零的调整量', 'warning');
      return;
    }
    setBusy(true);
    try {
      await adjustPoints(pointsTarget.id, amount, pointsReason.trim() || undefined);
      show('积分调整成功', 'success');
      setPointsTarget(null);
      setPointsAmount('');
      setPointsReason('');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleBan = async (row: AdminUserRow, banned: boolean) => {
    try {
      await banUser(row.id, banned);
      show(banned ? '已封禁' : '已解封', 'success');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  const handleRename = async () => {
    if (!renameTarget || !renameValue.trim()) return;
    setBusy(true);
    try {
      await renameUser(renameTarget.id, renameValue.trim());
      show('昵称已更新', 'success');
      setRenameTarget(null);
      setRenameValue('');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleOpenAssets = async (user: AdminUserRow) => {
    setAssetTarget(user);
    try {
      const resp = await listUserItems(user.id);
      const initialForm: Record<string, number> = {};
      PROPS_DEFINITIONS.forEach(p => initialForm[p.type] = 0);
      SKINS_DEFINITIONS.forEach(s => initialForm[s.type] = 0);
      
      resp.list.forEach(row => {
        initialForm[row.item_type] = row.count;
      });
      setAssetForm(initialForm);
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  const handleSaveAssets = async () => {
    if (!assetTarget) return;
    setBusy(true);
    try {
      const itemsPayload = Object.entries(assetForm).map(([item_type, count]) => ({
        item_type,
        count
      }));
      await updateUserItems(assetTarget.id, itemsPayload);
      show('用户资产背包已更新', 'success');
      setAssetTarget(null);
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
          用户管理
        </Typography>
        <TextField
          size="small"
          placeholder="手机号/昵称搜索"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setPage(0), load())}
          InputProps={{ endAdornment: <Search fontSize="small" /> }}
        />
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>手机号</TableCell>
                <TableCell>昵称</TableCell>
                <TableCell>积分</TableCell>
                <TableCell>角色</TableCell>
                <TableCell>状态</TableCell>
                <TableCell>注册时间</TableCell>
                <TableCell align="right">操作</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }} color="text.secondary">
                    暂无数据
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.phone}</TableCell>
                    <TableCell>{r.username}</TableCell>
                    <TableCell>{r.points}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.role} color={r.role === 'readonly' ? 'default' : 'primary'} />
                    </TableCell>
                    <TableCell>
                      {r.is_banned === 1 ? (
                        <Chip size="small" label="已封禁" color="error" />
                      ) : (
                        <Chip size="small" label="正常" color="success" />
                      )}
                    </TableCell>
                    <TableCell>{r.created_at ? new Date(r.created_at).toLocaleString() : '-'}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" disabled={!canWrite} onClick={() => { setPointsTarget(r); setPointsAmount(''); setPointsReason(''); }} title="调整积分">
                        <Add fontSize="small" />
                      </IconButton>
                      <IconButton size="small" disabled={!canWrite} onClick={() => { setRenameTarget(r); setRenameValue(r.username); }} title="改昵称">
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton size="small" disabled={!canWrite} onClick={() => handleOpenAssets(r)} title="道具与皮肤">
                        <ShoppingBag fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        disabled={!canWrite}
                        color={r.is_banned === 1 ? 'success' : 'error'}
                        onClick={() => handleBan(r, r.is_banned !== 1)}
                        title={r.is_banned === 1 ? '解封' : '封禁'}
                      >
                        {r.is_banned === 1 ? <CheckCircle fontSize="small" /> : <Block fontSize="small" />}
                      </IconButton>
                    </TableCell>
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

      {/* 积分调整弹窗 */}
      <Dialog open={!!pointsTarget} onClose={() => setPointsTarget(null)}>
        <DialogTitle>调整积分 · {pointsTarget?.phone}</DialogTitle>
        <DialogContent sx={{ minWidth: 360 }}>
          <TextField
            fullWidth
            margin="normal"
            label="调整量（正加负减，不可为 0）"
            type="number"
            value={pointsAmount}
            onChange={(e) => setPointsAmount(e.target.value)}
            autoFocus
          />
          <TextField
            fullWidth
            margin="normal"
            label="原因（可选）"
            value={pointsReason}
            onChange={(e) => setPointsReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPointsTarget(null)}>取消</Button>
          <Button variant="contained" disabled={busy} onClick={handleAdjust}>
            确认
          </Button>
        </DialogActions>
      </Dialog>

      {/* 改昵称弹窗 */}
      <Dialog open={!!renameTarget} onClose={() => setRenameTarget(null)}>
        <DialogTitle>修改昵称 · {renameTarget?.phone}</DialogTitle>
        <DialogContent sx={{ minWidth: 360 }}>
          <TextField
            fullWidth
            margin="normal"
            label="新昵称"
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            autoFocus
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenameTarget(null)}>取消</Button>
          <Button variant="contained" disabled={busy} onClick={handleRename}>
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {/* 道具与皮肤资产管理 Dialog */}
      <Dialog open={!!assetTarget} onClose={() => setAssetTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>管理用户背包 · {assetTarget?.username || assetTarget?.phone}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <FormLabel component="legend" sx={{ fontWeight: 'bold', mb: 1, color: 'text.primary' }}>法宝道具 (张)</FormLabel>
            </Grid>
            {PROPS_DEFINITIONS.map(p => (
              <Grid item xs={6} key={p.type}>
                <TextField
                  label={p.label}
                  type="number"
                  size="small"
                  fullWidth
                  value={assetForm[p.type] ?? 0}
                  onChange={(e) => setAssetForm(prev => ({ ...prev, [p.type]: Math.max(0, parseInt(e.target.value, 10) || 0) }))}
                />
              </Grid>
            ))}
            
            <Grid item xs={12} sx={{ mt: 2 }}>
              <FormLabel component="legend" sx={{ fontWeight: 'bold', mb: 1, color: 'text.primary' }}>卡牌皮肤 (拥有状态)</FormLabel>
            </Grid>
            {SKINS_DEFINITIONS.map(s => (
              <Grid item xs={6} key={s.type}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={(assetForm[s.type] ?? 0) >= 1}
                      onChange={(e) => setAssetForm(prev => ({ ...prev, [s.type]: e.target.checked ? 1 : 0 }))}
                    />
                  }
                  label={s.label}
                />
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssetTarget(null)} disabled={busy}>取消</Button>
          <Button variant="contained" onClick={handleSaveAssets} disabled={busy}>保存</Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
