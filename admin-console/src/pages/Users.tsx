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

/**
 * 用户管理（Users）页面组件。
 *
 * 路由：通常路径 `/users`。
 * 权限要求：
 * - 基础查看：已登录管理员即可。
 * - 用户写操作（积分/昵称/封禁/移除/资产）：需 `canWrite()`。
 * - 角色切换、新增管理员：仅超级管理员 `isSuper()` 可操作。
 *
 * 关键 State：
 * - `rows` / `total`：分页用户列表及总数（服务端分页）。
 * - `page` / `pageSize` / `keyword` / `roleFilter`：检索与分页条件。
 * - 各 `*Target`：当前操作的用户弹窗目标（积分/改名/资产/移除）。
 * - `newAdmin*`：新增管理员弹窗表单。
 * - `skinDefs`：从商城拉取的皮肤定义，用于资产弹窗渲染。
 *
 * 数据来源：依赖 listUsers（分页检索）、listShopItems（皮肤定义）、adjustPoints / banUser /
 * renameUser / deleteUser / updateUserItems / setAccountRole / createAccount 等写接口。
 */
export default function Users() {
  // 写入权限信号
  const canWrite = useAuth((s) => s.canWrite());
  // 超级管理员信号：控制角色切换与新增管理员入口
  const isSuper = useAuth((s) => s.isSuper());
  // 全局反馈组件
  const { show, Feedback } = useFeedback();

  // 当前页用户行
  const [rows, setRows] = useState<AdminUserRow[]>([]);
  // 服务端返回的总用户数（驱动分页器）
  const [total, setTotal] = useState(0);
  // 当前页码（0 基），传给后端的 page = page + 1
  const [page, setPage] = useState(0);
  // 每页条数
  const [pageSize, setPageSize] = useState(20);
  // 已确认的检索关键词（手机号/昵称），变更后触发 reload
  const [keyword, setKeyword] = useState('');
  // 角色筛选条件（'' 表示全部）
  const [roleFilter, setRoleFilter] = useState('');
  // 列表加载态
  const [loading, setLoading] = useState(true);

  // 资产（道具/皮肤）弹窗目标用户
  const [assetTarget, setAssetTarget] = useState<AdminUserRow | null>(null);
  // 资产表单：item_type -> 数量/拥有状态映射
  const [assetForm, setAssetForm] = useState<Record<string, number>>({});

  // 积分调整弹窗目标用户
  const [pointsTarget, setPointsTarget] = useState<AdminUserRow | null>(null);
  // 积分调整量（正加负减）
  const [pointsAmount, setPointsAmount] = useState('');
  // 积分调整原因（可选）
  const [pointsReason, setPointsReason] = useState('');
  // 改名弹窗目标用户
  const [renameTarget, setRenameTarget] = useState<AdminUserRow | null>(null);
  // 改名表单值
  const [renameValue, setRenameValue] = useState('');
  // 移除用户二次确认弹窗目标
  const [removeTarget, setRemoveTarget] = useState<AdminUserRow | null>(null);
  // 提交中态：统一禁用所有写操作按钮，防止重复提交
  const [busy, setBusy] = useState(false);

  // 新增管理员弹窗开关
  const [newAdminOpen, setNewAdminOpen] = useState(false);
  // 新增管理员表单：手机号
  const [newAdminPhone, setNewAdminPhone] = useState('');
  // 新增管理员表单：角色（默认运营）
  const [newAdminRole, setNewAdminRole] = useState('operator');
  // 新增管理员表单：初始密码（空则后端随机生成）
  const [newAdminPwd, setNewAdminPwd] = useState('');

  // 可配置皮肤定义列表（来自商城），驱动资产弹窗皮肤开关
  const [skinDefs, setSkinDefs] = useState<{ type: string; label: string }[]>([]);
  // 皮肤定义加载态
  const [skinsLoading, setSkinsLoading] = useState(true);

  // 拉取用户列表（服务端分页）：依赖页码/页大小/关键词/角色筛选，任一变化即重新检索
  // useCallback 缓存，供 useEffect 与主检索动作复用，避免无效重建
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

  // 页面挂载及依赖变化时重新加载列表
  useEffect(() => {
    load();
  }, [load]);

  // 拉取皮肤定义：组件挂载即请求一次（仅取 SKIN_ 前缀商品并清洗名称）
  // alive 标志防止卸载后 setState；清理函数将其置 false 以避免内存泄漏
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

  // 积分调整：校验非零数值后调用 adjustPoints（reason 可选），成功刷新列表
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

  // 封禁/解封：调用 banUser 并在成功后刷新列表（无 busy 锁，操作轻量且幂等）
  const handleBan = async (row: AdminUserRow, banned: boolean) => {
    try {
      await banUser(row.id, banned);
      show(banned ? '已封禁' : '已解封', 'success');
      load();
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  // 移除用户：二次确认后调用 deleteUser（硬删除，不可恢复），成功后刷新
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

  // 改名：校验非空后调用 renameUser，成功后刷新
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

  // 打开资产弹窗：拉取用户当前背包，并以 PROPS_DEFINITIONS + skinDefs 为骨架初始化表单（默认 0）
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

  // 保存资产：将表单转为 item_type/count 列表调用 updateUserItems（全量覆盖背包），成功后刷新
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

  // 新增管理员：仅超管可见入口；调用 createAccount，密码留空则后端随机生成
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
                      {/* 角色单元格：超管可下拉切换（调用 setAccountRole），其余用户仅以只读 Chip 展示 */}
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
