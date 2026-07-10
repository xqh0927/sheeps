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
  Button,
  IconButton,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Stack,
  Tooltip,
  MenuItem,
} from '@mui/material';
import { Add, Edit, Delete, Search } from '@mui/icons-material';
import { listI18n, createI18n, updateI18n, deleteI18n, I18nRow } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from '../components/feedback';

const MODULES = [
  { value: 'shop_items', label: '道具/皮肤' },
  { value: 'task', label: '任务' },
  { value: 'notice', label: '公告' },
  { value: 'app_version', label: '版本日志' },
  { value: 'system', label: '系统文案' },
  { value: 'gamemode', label: '游戏模式' },
];

const LOCALES = [
  { value: 'zh', label: '简体中文' },
  { value: 'en', label: 'English' },
  { value: 'tw', label: '繁体中文' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' },
];

const moduleLabel = (m: string) => MODULES.find((x) => x.value === m)?.label || m;

/** 从 str_key 解析 entity_ref 与 field */
function parseStrKey(strKey: string, module: string): { entity: string; field: string } {
  const prefix = `${module}.`;
  if (!strKey.startsWith(prefix)) return { entity: '-', field: strKey };
  const rest = strKey.slice(prefix.length);
  const idx = rest.lastIndexOf('.');
  if (idx < 0) return { entity: rest, field: '-' };
  return { entity: rest.slice(0, idx), field: rest.slice(idx + 1) };
}

/**
 * 客户端按 str_key 聚合后的分组结构。
 * values 以 locale 为键保存该语言下的单条记录，便于编辑/删除时定位 id。
 */
interface I18nGroup {
  str_key: string;
  module: string;
  category: string | null;
  entity_label: string | null;
  values: Record<string, I18nRow>;
  latestUpdatedAt: number | null;
}

/** 将后端按条返回的 I18nRow 列表聚合为以 str_key 为维度的分组 */
function groupRows(rows: I18nRow[]): I18nGroup[] {
  const map = new Map<string, I18nGroup>();
  for (const r of rows) {
    let g = map.get(r.str_key);
    if (!g) {
      g = {
        str_key: r.str_key,
        module: r.module,
        category: r.category,
        entity_label: r.entity_label ?? null,
        values: {},
        latestUpdatedAt: r.updated_at,
      };
      map.set(r.str_key, g);
    }
    g.values[r.locale] = r;
    if (r.updated_at && (g.latestUpdatedAt === null || r.updated_at > g.latestUpdatedAt)) {
      g.latestUpdatedAt = r.updated_at;
    }
  }
  return Array.from(map.values());
}

/**
 * 按字段（name / description）做客户端过滤。
 * 从每行 str_key 解析出 field，并与其 module 校验；当 selectedField 非空且不匹配时丢弃该行。
 * 注意：这是前端展示过滤，不改变后端分页请求与 total。
 */
function filterRowsByField(rows: I18nRow[], selectedField: string): I18nRow[] {
  if (!selectedField) return rows;
  return rows.filter((r) => {
    const { field } = parseStrKey(r.str_key, r.module);
    return field === selectedField;
  });
}

export default function I18nManager() {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  const [rows, setRows] = useState<I18nRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);

  const [module, setModule] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  // 字段筛选器：'' 表示全部，'name' / 'description' 仅显示对应字段的行（默认只显示 name）
  const [field, setField] = useState('name');

  // 新增（单条记录：str_key + 一个 locale）
  const [creating, setCreating] = useState(false);
  const [createForm, setCreateForm] = useState<Record<string, string>>({
    str_key: '',
    module: 'system',
    locale: 'zh',
    value: '',
  });

  // 编辑（整行：一个 str_key 下的所有语言）
  const [editGroup, setEditGroup] = useState<I18nGroup | null>(null);
  const [editValues, setEditValues] = useState<Record<string, string>>({});

  const [deleteGroup, setDeleteGroup] = useState<I18nGroup | null>(null);
  const [busy, setBusy] = useState(false);

  /** 当前页的记录按 str_key 聚合后的分组（已按字段筛选器做前端过滤） */
  const groups = useMemo(() => groupRows(filterRowsByField(rows, field)), [rows, field]);

  /** 客户端分页：对 groups 切片，count 基于聚合后的组数 */
  const paginatedGroups = useMemo(
    () => groups.slice(page * pageSize, (page + 1) * pageSize),
    [groups, page, pageSize]
  );

  const load = useCallback(() => {
    setLoading(true);
    // 后端不再分页，返回全部匹配行；前端按 str_key 聚合后做客户端分页。
    listI18n({ module: module || undefined, keyword: keyword || undefined })
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [module, keyword, show]);

  useEffect(() => {
    load();
  }, [load]);

  const applyKeyword = (raw: string) => {
    setKeyword(raw.trim() || '');
    setPage(0);
  };

  // ============ 新增 ============
  const openCreate = () => {
    setCreateForm({ str_key: '', module: 'system', locale: 'zh', value: '' });
    setCreating(true);
  };

  const handleCreate = async () => {
    if (!createForm.str_key.trim() || !createForm.locale.trim() || !createForm.module.trim()) {
      show('请填写 str_key / locale / module', 'warning');
      return;
    }
    setBusy(true);
    try {
      await createI18n({
        str_key: createForm.str_key.trim(),
        locale: createForm.locale.trim(),
        module: createForm.module.trim(),
        value: createForm.value,
      });
      show('已创建', 'success');
      setCreating(false);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  // ============ 编辑（整行） ============
  const openEdit = (g: I18nGroup) => {
    const vals: Record<string, string> = {};
    LOCALES.forEach((l) => {
      vals[l.value] = g.values[l.value]?.value ?? '';
    });
    setEditValues(vals);
    setEditGroup(g);
  };

  const handleSaveEdit = async () => {
    if (!editGroup) return;
    setBusy(true);
    try {
      const updates: Promise<unknown>[] = [];
      for (const l of LOCALES) {
        const row = editGroup.values[l.value];
        if (!row) continue; // 仅更新已存在的语言记录
        const next = editValues[l.value] ?? '';
        if (next !== (row.value ?? '')) {
          updates.push(updateI18n(row.id, { value: next }));
        }
      }
      await Promise.all(updates);
      show(updates.length > 0 ? '已保存' : '无变更', updates.length > 0 ? 'success' : 'info');
      setEditGroup(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  // ============ 删除（整行：组内所有语言） ============
  const handleDeleteGroup = async () => {
    if (!deleteGroup) return;
    setBusy(true);
    try {
      const ids = Object.values(deleteGroup.values).map((r) => r.id);
      await Promise.all(ids.map((id) => deleteI18n(id)));
      show('已删除', 'success');
      setDeleteGroup(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  // 表头列数（用于空数据 / 加载中的 colSpan 计算）
  const colCount = 4 + LOCALES.length + 1 + (canWrite ? 1 : 0);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} spacing={2}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          多语言管理
        </Typography>
        <Button variant="contained" startIcon={<Add />} disabled={!canWrite} onClick={openCreate}>
          新增键
        </Button>
      </Stack>

      <Stack direction="row" spacing={1.5} sx={{ mb: 2 }} flexWrap="wrap">
        <TextField
          select
          size="small"
          label="模块"
          value={module}
          onChange={(e) => {
            setModule(e.target.value);
            setPage(0);
          }}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">全部</MenuItem>
          {MODULES.map((m) => (
            <MenuItem key={m.value} value={m.value}>
              {m.label}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          size="small"
          label="字段"
          value={field}
          onChange={(e) => setField(e.target.value)}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">全部</MenuItem>
          <MenuItem value="name">name</MenuItem>
          <MenuItem value="description">description</MenuItem>
        </TextField>
        <TextField
          size="small"
          placeholder="搜索 str_key / 文案"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && applyKeyword(keywordInput)}
          InputProps={{ endAdornment: <Search fontSize="small" /> }}
          sx={{ minWidth: 240 }}
        />
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>模块</TableCell>
                <TableCell>实体(ref)</TableCell>
                <TableCell>字段</TableCell>
                <TableCell>实体名称</TableCell>
                {LOCALES.map((l) => (
                  <TableCell key={l.value}>{l.label}</TableCell>
                ))}
                <TableCell>更新时间</TableCell>
                {canWrite && <TableCell align="right">操作</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={colCount} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : paginatedGroups.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={colCount} align="center" sx={{ py: 5 }} style={{ color: '#999' }}>
                    暂无数据
                  </TableCell>
                </TableRow>
              ) : (
                paginatedGroups.map((g) => {
                  const { entity, field } = parseStrKey(g.str_key, g.module);
                  const configured = LOCALES.filter((l) => g.values[l.value]).length;
                  return (
                    <TableRow key={g.str_key}>
                      <TableCell>{moduleLabel(g.module)}</TableCell>
                      <TableCell>{entity}</TableCell>
                      <TableCell>{field}</TableCell>
                      <TableCell>{g.entity_label || <span style={{ color: '#999' }}>—</span>}</TableCell>
                      {LOCALES.map((l) => (
                        <TableCell key={l.value} sx={{ maxWidth: 200, wordBreak: 'break-all' }}>
                          {g.values[l.value]?.value || <span style={{ color: '#999' }}>—</span>}
                        </TableCell>
                      ))}
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <span>{g.latestUpdatedAt ? new Date(g.latestUpdatedAt).toLocaleString() : '—'}</span>
                          {canWrite && (
                            <Chip size="small" variant="outlined" label={`${configured}/${LOCALES.length}`} />
                          )}
                        </Stack>
                      </TableCell>
                      {canWrite && (
                        <TableCell align="right">
                          <Tooltip title="编辑">
                            <IconButton size="small" onClick={() => openEdit(g)}>
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="删除">
                            <IconButton size="small" color="error" onClick={() => setDeleteGroup(g)}>
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={groups.length}
          page={page}
          onPageChange={(_, p) => setPage(p)}
          rowsPerPage={pageSize}
          onRowsPerPageChange={(e) => {
            setPageSize(Number(e.target.value));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50, 100]}
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} / ${count} 组`}
        />
      </Card>

      {/* 新增弹窗（单条记录） */}
      <Dialog
        open={creating}
        onClose={() => !busy && setCreating(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>新增 i18n 键</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          <TextField
            fullWidth
            margin="normal"
            label="str_key（{module}.{entity_ref}.{field}）"
            value={createForm.str_key}
            onChange={(e) => setCreateForm((p) => ({ ...p, str_key: e.target.value }))}
          />
          <Stack direction="row" spacing={2}>
            <TextField
              select
              fullWidth
              margin="normal"
              label="module"
              value={createForm.module}
              onChange={(e) => setCreateForm((p) => ({ ...p, module: e.target.value }))}
            >
              {MODULES.map((m) => (
                <MenuItem key={m.value} value={m.value}>
                  {m.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              fullWidth
              margin="normal"
              label="locale"
              value={createForm.locale}
              onChange={(e) => setCreateForm((p) => ({ ...p, locale: e.target.value }))}
            >
              {LOCALES.map((l) => (
                <MenuItem key={l.value} value={l.value}>
                  {l.label}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
          <TextField
            fullWidth
            margin="normal"
            label="文案(value)"
            value={createForm.value}
            multiline
            minRows={3}
            onChange={(e) => setCreateForm((p) => ({ ...p, value: e.target.value }))}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => !busy && setCreating(false)} disabled={busy}>
            取消
          </Button>
          <Button variant="contained" onClick={handleCreate} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 编辑弹窗（整行：多语言并列） */}
      <Dialog
        open={editGroup !== null}
        onClose={() => !busy && setEditGroup(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>编辑 · {editGroup?.str_key}</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            模块：{editGroup ? `${moduleLabel(editGroup.module)}（${editGroup.module}）` : ''}
          </Typography>
          {LOCALES.map((l) => {
            const has = !!editGroup?.values[l.value];
            return (
              <TextField
                key={l.value}
                fullWidth
                margin="normal"
                label={l.label}
                value={editValues[l.value] ?? ''}
                disabled={!has}
                helperText={has ? '' : '该语言尚未配置，可通过「新增」补充'}
                multiline
                minRows={2}
                onChange={(e) => setEditValues((p) => ({ ...p, [l.value]: e.target.value }))}
              />
            );
          })}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => !busy && setEditGroup(null)} disabled={busy}>
            取消
          </Button>
          <Button variant="contained" onClick={handleSaveEdit} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 删除确认（整行：删除该 str_key 下全部语言） */}
      <Dialog open={!!deleteGroup} onClose={() => !busy && setDeleteGroup(null)}>
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>
          确定要删除「{deleteGroup?.str_key}」下的全部语言记录吗？共 {deleteGroup ? Object.keys(deleteGroup.values).length : 0} 条，此操作不可撤销。
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteGroup(null)} disabled={busy}>
            取消
          </Button>
          <Button color="error" variant="contained" onClick={handleDeleteGroup} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '删除'}
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
