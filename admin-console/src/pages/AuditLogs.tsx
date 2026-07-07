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

export default function AuditLogs() {
  const isSuper = useAuth((s) => s.isSuper());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<AuditRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);

  const [action, setAction] = useState('');
  const [adminId, setAdminId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const [detail, setDetail] = useState<AuditRow | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    const from = fromDate ? new Date(fromDate).getTime() : undefined;
    const to = toDate ? new Date(toDate).getTime() + 86400000 : undefined;
    listAuditLogs({ page: page + 1, pageSize, action: action.trim() || undefined, admin_id: adminId.trim() || undefined, from, to })
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [page, pageSize, action, adminId, fromDate, toDate, show]);

  useEffect(() => {
    if (isSuper) load();
  }, [load, isSuper]);

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
        <TextField size="small" label="操作类型" value={action} onChange={(e) => setAction(e.target.value)} />
        <TextField size="small" label="管理员 ID" value={adminId} onChange={(e) => setAdminId(e.target.value)} />
        <TextField size="small" label="起始日期" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} InputLabelProps={{ shrink: true }} />
        <TextField size="small" label="结束日期" type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} InputLabelProps={{ shrink: true }} />
        <Button variant="outlined" onClick={() => { setPage(0); load(); }}>筛选</Button>
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
                    <TableCell><Chip size="small" label={r.action} color="info" /></TableCell>
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
        <DialogTitle>审计详情 · {detail?.action}</DialogTitle>
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

function safeParse(s: string | null): any {
  if (!s) return null;
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}
