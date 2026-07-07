import { useEffect, useState, ReactNode, useCallback } from 'react';
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
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Stack,
  Tooltip,
} from '@mui/material';
import { Add, Edit, Delete } from '@mui/icons-material';
import { PageResult } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from './feedback';

export interface FieldDef {
  name: string;
  label: string;
  type?: 'text' | 'number' | 'textarea' | 'select';
  options?: { label: string; value: string }[];
  required?: boolean;
  fullWidth?: boolean;
  hideOnCreate?: boolean;
}

export interface ColumnDef {
  key: string;
  label: string;
  render?: (row: any) => ReactNode;
}

interface CrudPageProps {
  title: string;
  idField: string;
  columns: ColumnDef[];
  fields: FieldDef[];
  fetcher: (page: number, pageSize: number) => Promise<PageResult<any>>;
  creator: (body: Record<string, any>) => Promise<any>;
  updater: (id: string | number, body: Record<string, any>) => Promise<any>;
  deleter: (id: string | number) => Promise<any>;
  /** 是否展示删除按钮（默认 true） */
  deletable?: boolean;
}

export default function CrudPage({
  title,
  idField,
  columns,
  fields,
  fetcher,
  creator,
  updater,
  deleter,
  deletable = true,
}: CrudPageProps) {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);

  const [editing, setEditing] = useState<any | null>(null); // null=关闭, {} = 新建, object = 编辑
  const [form, setForm] = useState<Record<string, any>>({});
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<any | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    fetcher(page + 1, pageSize)
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [fetcher, page, pageSize, show]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    const init: Record<string, any> = {};
    fields.forEach((f) => {
      if (!f.hideOnCreate) init[f.name] = f.type === 'number' ? '' : '';
    });
    setForm(init);
    setEditing({});
  };

  const openEdit = (row: any) => {
    const init: Record<string, any> = {};
    fields.forEach((f) => {
      init[f.name] = row[f.name] ?? (f.type === 'number' ? '' : '');
    });
    setForm(init);
    setEditing(row);
  };

  const handleSubmit = async () => {
    // 校验必填
    for (const f of fields) {
      if (f.required && (form[f.name] === undefined || form[f.name] === '')) {
        show(`请填写${f.label}`, 'warning');
        return;
      }
    }
    const body: Record<string, any> = {};
    fields.forEach((f) => {
      const v = form[f.name];
      if (v === undefined || v === '') return;
      body[f.name] = f.type === 'number' ? Number(v) : v;
    });

    setBusy(true);
    try {
      if (editing && editing[idField] !== undefined) {
        await updater(editing[idField], body);
        show('已保存', 'success');
      } else {
        const r = await creator(body);
        if (r?.initialPassword) show(`创建成功，初始密码：${r.initialPassword}`, 'success');
        else show('创建成功', 'success');
      }
      setEditing(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleter(deleteTarget[idField]);
      show('已删除', 'success');
      setDeleteTarget(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    }
  };

  const createFields = fields.filter((f) => !f.hideOnCreate);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          {title}
        </Typography>
        <Button variant="contained" startIcon={<Add />} disabled={!canWrite} onClick={openCreate}>
          新建
        </Button>
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {columns.map((c) => (
                  <TableCell key={c.key}>{c.label}</TableCell>
                ))}
                {(canWrite) && <TableCell align="right">操作</TableCell>}
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
                  <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 5 }} color="text.secondary">
                    暂无数据
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((r) => (
                  <TableRow key={r[idField]}>
                    {columns.map((c) => (
                      <TableCell key={c.key}>
                        {c.render ? c.render(r) : String(r[c.key] ?? '-')}
                      </TableCell>
                    ))}
                    {canWrite && (
                      <TableCell align="right">
                        <Tooltip title="编辑">
                          <IconButton size="small" onClick={() => openEdit(r)}>
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        {deletable && (
                          <Tooltip title="删除">
                            <IconButton size="small" color="error" onClick={() => setDeleteTarget(r)}>
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
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

      {/* 新建/编辑弹窗 */}
      <Dialog open={editing !== null} onClose={() => setEditing(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing && editing[idField] !== undefined ? `编辑 · #${editing[idField]}` : '新建'}</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          {(editing ? fields : createFields).map((f) => {
            const common = {
              key: f.name,
              fullWidth: true,
              margin: 'normal' as const,
              label: f.label + (f.required ? ' *' : ''),
              value: form[f.name] ?? '',
              onChange: (e: any) => setForm((prev) => ({ ...prev, [f.name]: e.target.value })),
              disabled: busy,
            };
            if (f.type === 'textarea') return <TextField {...common} multiline minRows={3} />;
            if (f.type === 'select')
              return (
                <TextField {...common} select SelectProps={{ native: true }}>
                  {f.options?.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </TextField>
              );
            return <TextField {...common} type={f.type === 'number' ? 'number' : 'text'} />;
          })}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditing(null)} disabled={busy}>
            取消
          </Button>
          <Button variant="contained" onClick={handleSubmit} disabled={busy}>
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {/* 删除确认 */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>确定要删除该记录吗？此操作不可撤销。</DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>取消</Button>
          <Button color="error" variant="contained" onClick={handleDelete}>
            删除
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
