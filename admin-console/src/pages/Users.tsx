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
  MenuItem,
} from '@mui/material';
import { Search, Add, Block, CheckCircle, Edit, ShoppingBag, DeleteForever } from '@mui/icons-material';
import { listUsers, adjustPoints, banUser, renameUser, deleteUser, setAccountRole, createAccount, AdminUserRow, listUserItems, updateUserItems, listShopItems } from '../api/admin';
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

const ROLES = [
  { value: 'user', label: '普通用户' },
  { value: 'super', label: '超级管理员' },
  { value: 'operator', label: '运营' },
  { value: 'readonly', label: '只读' },
];
const ROLE_LABEL: Record<string, string> = Object.fromEntries(ROLES.map((r) => [r.value, r.label]));
const ADMIN_ROLES = ROLES.filter((r) => r.value !== 'user');

export default function Users() {
  const canWrite = useAuth((s) => s.canWrite());
  const isSuper = useAuth((s) => s.isSuper());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<AdminUserRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [loading, setLoading] = useState(true);

  const [assetTarget, setAssetTarget] = useState<AdminUserRow | null>(null);
  const [assetForm, setAssetForm] = useState<Record<string, number>>({});

  const [pointsTarget, setPointsTarget] = useState<AdminUserRow | null>(null);
  const [pointsAmount, setPointsAmount] = useState('');
  const [pointsReason, setPointsReason] = useState('');
  const [renameTarget, setRenameTarget] = useState<AdminUserRow | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [removeTarget, setRemoveTarget] = useState<AdminUserRow | null>(null);
  const [busy, setBusy] = useState(false);

  const [newAdminOpen, setNewAdminOpen] = useState(false);
  const [newAdminPhone, setNewAdminPhone] = useState('');
  const [newAdminRole, setNewAdminRole] = useState('operator');
  const [newAdminPwd, setNewAdminPwd] = useState('');

  const [skinDefs, setSkinDefs] = useState<{ type: string; label: string }[]>([]);
  const [skinsLoading, setSkinsLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    listUsers(page + 1, pageSize, keyword.trim(), roleFilter)
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [page, pageSize, keyword, roleFilter, show]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    let alive = true;
    setSkinsLoading(true);
    listShopItems(1, 200)
      .then((r) => {
        if (!alive) return;
        const defs = (r.list || [])
          .filter((x: any) => typeof x.item_type === 'string' && x.item_type.startsWith('SKIN_'))
          .map((x: any) => ({
            type: x.item_type,
            label: String(x.name || x.item_type).replace(/\s*\(卡牌皮肤\)$/, ''),
          }));
        setSkinDefs(defs);
      })
      .catch(() => { if (alive) setSkinDefs([]); })
      .finally(() => { if (alive) setSkinsLoading(false); });
    return () => { alive = false; };
  }, []);

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

  const handleRemove = async () => {
    if (!removeTarget) return;
    setBusy(true);
    try {
      await deleteUser(removeTarget.id);
      show('用户已移除', 'success');
      setRemoveTarget(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
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
      skinDefs.forEach(s => initialForm[s.type] = 0);

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

  const handleCreateAdmin = async () => {
    if (!newAdminPhone.trim()) return;
    setBusy(true);
    try {
      await createAccount(newAdminPhone.trim(), newAdminRole, newAdminPwd.trim() || undefined);
      show('管理员已创建', 'success');
      setNewAdminOpen(false);
      setNewAdminPhone('');
      setNewAdminPwd('');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" gap={1}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          用户管理
        </Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          <TextField
            select
            size="small"
            label="角色筛选"
            value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
            sx={{ minWidth: 130 }}
          >
            <MenuItem value="">全部</MenuItem>
            {ROLES.map((r) => (
              <MenuItem key={r.value} value={r.value}>{r.label}</MenuItem>
            ))}
          </TextField>
          <TextField
            size="small"
            placeholder="手机号/昵称搜索"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && (setPage(0), load())}
            InputProps={{ endAdornment: <Search fontSize="small" /> }}
          />
          {isSuper && (
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => { setNewAdminPhone(''); setNewAdminRole('operator'); setNewAdminPwd(''); setNewAdminOpen(true); }}
            >
              新增管理员
            </Button>
          )}
        </Stack>
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
                      {isSuper ? (
                        <TextField
                          select
                          size="small"
                          value={r.role}
                          onChange={async (e) => {
                            const newRole = e.target.value;
                            try {
                              await setAccountRole(r.id, newRole);
                              show('角色已更新', 'success');
                              load();
                            } catch (err) {
                              show(extractError(err), 'error');
                            }
                          }}
                          sx={{ minWidth: 110 }}
                        >
                          {ROLES.map((r2) => (
                            <MenuItem key={r2.value} value={r2.value}>{r2.label}</MenuItem>
                          ))}
                        </TextField>
                      ) : (
                        <Chip size="small" label={ROLE_LABEL[r.role] || r.role} color={r.role === 'readonly' ? 'default' : 'primary'} />
                      )}
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
                      <IconButton
                        size="small"
                        disabled={!canWrite}
                        color="error"
                        onClick={() => setRemoveTarget(r)}
                        title="移除用户"
                      >
                        <DeleteForever fontSize="small" />
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
            {skinsLoading ? (
              <Grid item xs={12}>
                <CircularProgress size={20} />
              </Grid>
            ) : skinDefs.length === 0 ? (
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">暂无可配置皮肤</Typography>
              </Grid>
            ) : (
              skinDefs.map(s => (
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
              ))
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssetTarget(null)} disabled={busy}>取消</Button>
          <Button variant="contained" onClick={handleSaveAssets} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 移除用户二次确认弹窗 */}
      <Dialog open={!!removeTarget} onClose={() => setRemoveTarget(null)}>
        <DialogTitle>移除用户 · {removeTarget?.phone}</DialogTitle>
        <DialogContent sx={{ minWidth: 360 }}>
          <Typography color="error" sx={{ fontWeight: 600 }}>
            此操作将永久删除该用户及其全部数据（积分、背包、关卡进度、签到、兑换记录、排行榜等），且不可恢复！
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRemoveTarget(null)} disabled={busy}>取消</Button>
          <Button variant="contained" color="error" disabled={busy} onClick={handleRemove}>确认移除</Button>
        </DialogActions>
      </Dialog>

      {/* 新增管理员弹窗 */}
      <Dialog open={newAdminOpen} onClose={() => setNewAdminOpen(false)}>
        <DialogTitle>新增管理员账户</DialogTitle>
        <DialogContent sx={{ minWidth: 360 }}>
          <TextField
            fullWidth
            margin="normal"
            label="手机号"
            value={newAdminPhone}
            onChange={(e) => setNewAdminPhone(e.target.value)}
            autoFocus
          />
          <TextField
            fullWidth
            margin="normal"
            select
            label="角色"
            value={newAdminRole}
            onChange={(e) => setNewAdminRole(e.target.value)}
          >
            {ADMIN_ROLES.map((r) => (
              <MenuItem key={r.value} value={r.value}>{r.label}</MenuItem>
            ))}
          </TextField>
          <TextField
            fullWidth
            margin="normal"
            label="初始密码（留空则随机生成）"
            type="password"
            value={newAdminPwd}
            onChange={(e) => setNewAdminPwd(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNewAdminOpen(false)} disabled={busy}>取消</Button>
          <Button
            variant="contained"
            disabled={busy || !newAdminPhone.trim()}
            onClick={handleCreateAdmin}
          >
            {busy ? <CircularProgress size={18} color="inherit" /> : '创建'}
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
