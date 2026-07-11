import { useEffect, useState, useCallback, useMemo } from 'react';
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
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  CircularProgress,
  Stack,
  Alert,
  Button,
} from '@mui/material';
import { listAuditLogs, AuditRow } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';

/** 审计操作 action 的中文说明映射 */
const ACTION_LABELS: Record<string, string> = {
  'LOGIN_SUCCESS': '登录成功',
  'UPDATE_APP_VERSION': '更新App版本',
  'CREATE_APP_VERSION': '创建App版本',
  'SAVE_ITEM_ICON': '保存道具图标',
  'SAVE_SKIN_TILES': '保存皮肤卡面',
  'CREATE_LEVEL': '创建关卡',
  'UPDATE_LEVEL': '更新关卡',
  'DELETE_LEVEL': '删除关卡',
  'CREATE_NOTICE': '创建公告',
  'UPDATE_NOTICE': '更新公告',
  'DELETE_NOTICE': '删除公告',
  'CREATE_TASK': '创建任务',
  'UPDATE_TASK': '更新任务',
  'DELETE_TASK': '删除任务',
  'CREATE_SHOP_ITEM': '创建商品',
  'UPDATE_SHOP_ITEM': '更新商品',
  'DELETE_SHOP_ITEM': '删除商品',
  'ADJUST_POINTS': '调整积分',
  'BAN_USER': '封禁用户',
  'UNBAN_USER': '解封用户',
  'RENAME_USER': '重命名用户',
  'DELETE_USER': '删除用户',
  'UPDATE_USER_ITEMS': '更新用户资产',
  'UPDATE_CONFIG': '更新配置',
  'CREATE_ADMIN': '创建管理员',
  'UPDATE_ADMIN_ROLE': '更新管理员角色',
  'DISABLE_ADMIN': '禁用管理员',
  'CREATE_LEADERBOARD': '创建排行榜记录',
  'UPDATE_LEADERBOARD': '更新排行榜记录',
  'DELETE_LEADERBOARD': '删除排行榜记录',
  'CREATE_I18N': '创建多语言条目',
  'UPDATE_I18N': '更新多语言条目',
  'DELETE_I18N': '删除多语言条目',
};

/** 反向映射：中文 → 英文 action，用于搜索时将中文输入转为英文传给后端 */
const ACTION_LABELS_REVERSE: Record<string, string> = {};
Object.entries(ACTION_LABELS).forEach(([en, zh]) => {
  ACTION_LABELS_REVERSE[zh] = en;
});

/** 判断字符串是否包含中文字符 */
function containsChinese(s: string): boolean {
  return /[\u4e00-\u9fff]/.test(s);
}

/** 将中文 action 输入反向映射为英文 action；若无匹配则原样返回 */
function resolveActionInput(input: string): string {
  const trimmed = input.trim();
  if (!trimmed) return '';
  if (!containsChinese(trimmed)) return trimmed;
  // 精确匹配
  if (ACTION_LABELS_REVERSE[trimmed]) return ACTION_LABELS_REVERSE[trimmed];
  // 模糊匹配：查找包含该中文的任意条目
  for (const [zh, en] of Object.entries(ACTION_LABELS_REVERSE)) {
    if (zh.includes(trimmed)) return en;
  }
  return trimmed;
}

/**
 * 操作审计日志（AuditLogs）页面组件。
 *
 * 路由：通常路径 `/audit`。
 * 权限要求：仅超级管理员 `isSuper()` 可访问；非超管直接渲染无权限提示并短路返回。
 * 关键 State：
 * - `rows` / `total`：服务端分页的审计记录及总数。
 * - `page` / `pageSize` / `action` / `adminId` / `fromDate` / `toDate`：筛选与分页条件。
 * - `detail`：当前展开查看的审计详情弹窗目标。
 *
 * 数据来源：依赖 listAuditLogs；action 输入支持中文，经 resolveActionInput 反向映射为英文枚举传给后端。
 */
export default function AuditLogs() {
  // 超级管理员信号：控制整页可访问性与数据加载
  const isSuper = useAuth((s) => s.isSuper());
  // 全局反馈组件
  const { show, Feedback } = useFeedback();

  // 当前页审计记录
  const [rows, setRows] = useState<AuditRow[]>([]);
  // 服务端返回的总记录数（驱动分页器）
  const [total, setTotal] = useState(0);
  // 当前页码（0 基），传给后端 page = page + 1
  const [page, setPage] = useState(0);
  // 每页条数
  const [pageSize, setPageSize] = useState(20);
  // 列表加载态
  const [loading, setLoading] = useState(true);

  // 操作类型输入（支持中文，加载前经 resolveActionInput 映射为英文枚举）
  const [action, setAction] = useState('');
  // 管理员 ID 筛选
  const [adminId, setAdminId] = useState('');
  // 起始日期（YYYY-MM-DD），加载时转毫秒并作为区间下界
  const [fromDate, setFromDate] = useState('');
  // 结束日期（YYYY-MM-DD），加载时转毫秒 + 一天以含当天，作为区间上界
  const [toDate, setToDate] = useState('');

  // 审计详情弹窗目标记录
  const [detail, setDetail] = useState<AuditRow | null>(null);

  // 拉取审计日志（服务端分页）：合并各筛选条件；from/to 为时间区间（含当天），action 经中文→英文映射
  const load = useCallback(() => {
    setLoading(true);
    const from = fromDate ? new Date(fromDate).getTime() : undefined;
    const to = toDate ? new Date(toDate).getTime() + 86400000 : undefined;
    const resolvedAction = resolveActionInput(action);
    listAuditLogs({ page: page + 1, pageSize, action: resolvedAction || undefined, admin_id: adminId.trim() || undefined, from, to })
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [page, pageSize, action, adminId, fromDate, toDate, show]);

  // 仅超管加载数据；依赖 load / isSuper
  useEffect(() => {
    if (isSuper) load();
  }, [load, isSuper]);

  /** 筛选：先校验起止日期合理性，再重置到第 0 页并重新加载 */
  const handleSearch = () => {
    if (fromDate && toDate && new Date(fromDate).getTime() > new Date(toDate).getTime()) {
      show('起始日期不能晚于结束日期', 'warning');
      return;
    }
    setPage(0);
    load();
  };

  // 输入框回车触发筛选
  const handleEnter = (e: any) => {
    if (e.key === 'Enter') handleSearch();
  };

  // 非超级管理员：短路返回无权限提示，不渲染数据区域
  if (!isSuper) {
    return (
      <Box>
        <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>操作审计日志</Typography>
        <Alert severity="warning">仅超级管理员可访问此页面。</Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>操作审计日志</Typography>

      <Stack direction="row" spacing={1.5} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
        <TextField size="small" label="操作类型" value={action} onChange={(e) => setAction(e.target.value)} onKeyDown={handleEnter} />
        <TextField size="small" label="管理员 ID" value={adminId} onChange={(e) => setAdminId(e.target.value)} onKeyDown={handleEnter} />
        <TextField size="small" label="起始日期" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} InputLabelProps={{ shrink: true }} onKeyDown={handleEnter} />
        <TextField size="small" label="结束日期" type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} InputLabelProps={{ shrink: true }} onKeyDown={handleEnter} />
        <Button variant="outlined" onClick={handleSearch}>筛选</Button>
        <Button onClick={() => { setAction(''); setAdminId(''); setFromDate(''); setToDate(''); setPage(0); }}>重置</Button>
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>时间</TableCell>
                <TableCell>管理员</TableCell>
                <TableCell>角色</TableCell>
                <TableCell>操作</TableCell>
                <TableCell>对象</TableCell>
                <TableCell>IP</TableCell>
                <TableCell align="right">详情</TableCell>
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
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }} color="text.secondary">暂无记录</TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{new Date(r.created_at).toLocaleString()}</TableCell>
                    <TableCell>{r.admin_phone}</TableCell>
                    <TableCell><Chip size="small" label={r.admin_role} /></TableCell>
                    <TableCell><Chip size="small" label={ACTION_LABELS[r.action] || r.action} color="info" /></TableCell>
                    <TableCell>{r.target_type ? `${r.target_type}:${r.target_id}` : '-'}</TableCell>
                    <TableCell>{r.source_ip}</TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => setDetail(r)}>查看</Button>
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

      <Dialog open={!!detail} onClose={() => setDetail(null)} maxWidth="md" fullWidth>
        <DialogTitle>审计详情 · {detail ? (ACTION_LABELS[detail.action] || detail.action) : ''}</DialogTitle>
        <DialogContent>
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', background: '#f5f5f5', padding: 12, borderRadius: 8 }}>
            {detail ? JSON.stringify(
              {
                admin: { id: detail.admin_id, phone: detail.admin_phone, role: detail.admin_role },
                target: { type: detail.target_type, id: detail.target_id },
                before: safeParse(detail.before_snapshot),
                after: safeParse(detail.after_snapshot),
                source_ip: detail.source_ip,
                user_agent: detail.user_agent,
                created_at: new Date(detail.created_at).toLocaleString(),
              },
              null,
              2
            ) : ''}
          </pre>
        </DialogContent>
      </Dialog>

      {Feedback}
    </Box>
  );
}

// 安全解析审计快照字符串：成功返回对象，失败或非空时回退原始字符串
function safeParse(s: string | null): any {
  if (!s) return null;
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}
