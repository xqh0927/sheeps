import { useEffect, useState, useRef, useCallback, useMemo, ReactNode } from 'react';
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
import { Add, Edit, Delete, Search } from '@mui/icons-material';
import { PageResult } from '../api/admin';
import { extractError } from '../api/client';
import { useAuth } from '../store/auth';
import { useFeedback } from './feedback';
import MultilingualField from './MultilingualField';
import { I18N_LOCALES, LOCALE_SUFFIX, LOCALE_LABELS } from '../constants/locales';

/** 表单字段定义 */
export interface FieldDef {
  name: string;
  label: string;
  type?: 'text' | 'number' | 'textarea' | 'select' | 'image';
  options?: { label: string; value: string }[];
  required?: boolean;
  fullWidth?: boolean;
  hideOnCreate?: boolean;
  /**
   * 空值（'' / null / undefined）时是否显式传 null。
   * 默认 false：空值省略不传（保持原值）。
   */
  nullable?: boolean;
  /**
   * 仅 type:'image' 生效：提供该函数后渲染「上传图片」按钮，
   * 选中文件即调用它拿到 URL 回填到该字段（预览）。不提供则仅展示预览（只读）。
   */
  upload?: (file: File) => Promise<string>;
  /**
   * 多语言字段：新建态渲染 5 语言输入框并强制 5 语言全非空才能提交；
   * 编辑态退化为单 zh 输入（沿用既有逻辑）。仅对基础字段（name/description/title/content/update_log）生效。
   */
  multilingual?: boolean;
}

/** 表格列定义 */
export interface ColumnDef {
  key: string;
  label: string;
  render?: (row: any) => ReactNode;
  /** 该列是否支持客户端排序（需 CrudPage 的 sortable 为 true） */
  sortable?: boolean;
}

/** 排序状态（仅客户端、仅当前页） */
interface SortState {
  field: string;
  order: 'asc' | 'desc';
}

/** CrudPage 属性契约 */
export interface CrudPageProps {
  title: string;
  idField: string;
  columns: ColumnDef[];
  fields: FieldDef[];
  fetcher: (page: number, pageSize: number, keyword?: string) => Promise<PageResult<any>>;
  creator: (body: Record<string, any>) => Promise<any>;
  updater: (id: string | number, body: Record<string, any>) => Promise<any>;
  deleter: (id: string | number) => Promise<any>;
  /** 是否展示删除按钮（默认 true） */
  deletable?: boolean;
  /** 是否渲染搜索框（默认 false） */
  searchable?: boolean;
  /** 搜索框占位文本 */
  searchPlaceholder?: string;
  /** 输入防抖毫秒数（默认 400；回车立即触发，不引 lodash） */
  searchDebounceMs?: number;
  /** 删除确认中用于展示的记录名字段名 */
  deleteNameField?: string;
  /** 自定义删除确认正文，优先级高于 deleteNameField */
  getDeleteConfirmText?: (row: any) => string;
  /** 是否启用列排序（默认 false，P2） */
  sortable?: boolean;
  /** 初始排序列 */
  initialSort?: SortState;
  /** 行操作槽：返回挂载在「操作」列中的自定义节点（如「卡面管理」「图标管理」按钮） */
  rowActions?: (row: any) => ReactNode;
}

/**
 * 通用 CRUD 页面外壳组件（表格 + 分页 + 新建/编辑弹窗 + 删除确认 + 可选搜索/排序）。
 *
 * 设计意图：各业务管理页（用户、皮肤、道具……）只需传入字段定义（fields）、
 * 列定义（columns）与四个数据操作函数（fetcher/creator/updater/deleter）即可复用。
 *
 * 数据流向：
 * - 列表数据来源于 `fetcher`（服务端分页），渲染到 Table；
 * - 表单数据由本地 `form` state 承载，提交时经校验与字段契约转换后调用 creator/updater；
 * - `useFeedback` 提供全局 Toast，所有成功/失败提示统一经 `show` 输出。
 *
 * 副作用与清理：组件挂载时加载首屏数据；卸载时清理搜索防抖定时器，避免内存泄漏。
 *
 * @param props 见 {@link CrudPageProps}
 */
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
  searchable = false,
  searchPlaceholder,
  searchDebounceMs = 400,
  deleteNameField,
  getDeleteConfirmText,
  sortable = false,
  initialSort,
  rowActions,
}: CrudPageProps) {
  // 写权限：来自 auth store 的 canWrite()，决定「新建/编辑/删除」按钮是否可用
  const canWrite = useAuth((s) => s.canWrite());
  // 全局 Toast：show 触发提示，Feedback 为需渲染的 Snackbar 节点
  const { show, Feedback } = useFeedback();

  // —— 列表与分页状态 ——
  const [rows, setRows] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);

  // —— 表单/弹窗状态 ——
  // editing：null=弹窗关闭，{} = 新建态，object = 编辑态（携带行数据）
  const [editing, setEditing] = useState<any | null>(null); // null=关闭, {} = 新建, object = 编辑
  const [form, setForm] = useState<Record<string, any>>({});
  // busy：提交/创建进行中，禁用表单与按钮
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<any | null>(null);
  const [deleting, setDeleting] = useState(false); // 删除期间禁用删除/取消按钮
  const [uploading, setUploading] = useState(false); // 图片字段上传中

  // 搜索：keywordInput 为输入框即时值；keyword 为防抖后透传给 fetcher 的激活值
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState<string | undefined>(undefined);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 排序（仅客户端、仅当前页内）
  const [sortState, setSortState] = useState<SortState | null>(
    sortable && initialSort ? { field: initialSort.field, order: initialSort.order } : null
  );

  // 拉取当前页数据：fetcher 为 1-based 页码（故 page+1）。
  // 依赖 [fetcher, page, pageSize, keyword, show]：分页/搜索变化时自动重新拉取；
  // 闭包稳定，避免每次渲染都重建函数触发无限刷新
  const load = useCallback(() => {
    setLoading(true);
    fetcher(page + 1, pageSize, keyword)
      .then((d) => {
        setRows(d.list);
        setTotal(d.total);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  }, [fetcher, page, pageSize, keyword, show]);

  // 首屏加载与分页/搜索变化时的自动重载（依赖 load，load 变化即重拉）
  useEffect(() => {
    load();
  }, [load]);

  // 卸载时清理防抖定时器
  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  /** 应用关键词：trim 后为空则透传 undefined；重置到第 0 页 */
  const applyKeyword = useCallback((raw: string) => {
    setKeyword(raw.trim() || undefined);
    setPage(0);
  }, []);

  const handleSearchChange = (e: any) => {
    const value = e.target.value;
    setKeywordInput(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => applyKeyword(value), searchDebounceMs);
  };

  /** 回车立即触发（取消防抖定时器） */
  const handleSearchEnter = () => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    applyKeyword(keywordInput);
  };

  const openCreate = () => {
    const init: Record<string, any> = {};
    fields.forEach((f) => {
      init[f.name] = f.type === 'number' ? '' : '';
      // 多语言字段：初始化 5 语言空值（zh 即 f.name 本身，其余语言用后缀键）
      if (f.multilingual) {
        for (const loc of I18N_LOCALES) {
          if (loc === 'zh') continue;
          init[`${f.name}${LOCALE_SUFFIX[loc]}`] = '';
        }
      }
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
    const isCreate = !(editing && editing[idField] !== undefined);
    // 校验必填
    for (const f of fields) {
      // 多语言字段（新建态）：强制 5 语言全非空；任一为空 → 拦截并红字提示
      if (f.multilingual && isCreate) {
        const missing = I18N_LOCALES.filter((loc) => {
          const key = loc === 'zh' ? f.name : `${f.name}${LOCALE_SUFFIX[loc]}`;
          const v = form[key];
          return v === undefined || v === null || String(v).trim() === '';
        });
        if (missing.length > 0) {
          const labels = missing.map((l) => LOCALE_LABELS[l]).join('、');
          show(`请完整填写 5 种语言：${f.label}（缺失：${labels}）`, 'warning');
          return;
        }
        continue;
      }
      if (f.required && (form[f.name] === undefined || form[f.name] === '')) {
        show(`请填写${f.label}`, 'warning');
        return;
      }
    }
    // 构建提交体（数字空值显式契约）
    const body: Record<string, any> = {};
    for (const f of fields) {
      // 图片字段由行内弹窗/上传按钮维护，submit 时不随表单提交（避免覆盖封面镜像）
      if (f.type === 'image') continue;
      // 多语言字段（新建态）：组装 5 语言键（base + base_en/_tw/_ja/_ko）
      if (f.multilingual && isCreate) {
        for (const loc of I18N_LOCALES) {
          const key = loc === 'zh' ? f.name : `${f.name}${LOCALE_SUFFIX[loc]}`;
          const raw = form[key];
          if (raw === undefined || raw === '') continue;
          body[key] = raw;
        }
        continue;
      }
      const raw = form[f.name];
      if (f.type === 'number') {
        // 空值：'' / null / undefined → 默认省略；nullable 且空 → 显式传 null
        if (raw === '' || raw === null || raw === undefined) {
          if (f.nullable) body[f.name] = null;
          continue;
        }
        const num = Number(raw);
        if (!Number.isFinite(num)) {
          show(`「${f.label}」必须是有效数字`, 'warning');
          return;
        }
        body[f.name] = num;
        continue;
      }
      if (raw === undefined || raw === '') continue;
      body[f.name] = raw;
    }

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
    setDeleting(true);
    try {
      await deleter(deleteTarget[idField]);
      show('已删除', 'success');
      setDeleteTarget(null);
      load();
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setDeleting(false);
    }
  };

  /** 删除确认正文：getDeleteConfirmText > deleteNameField 模板 > 默认文案 */
  const getDeleteText = (): string => {
    if (!deleteTarget) return '';
    if (getDeleteConfirmText) return getDeleteConfirmText(deleteTarget);
    if (deleteNameField) return `确定要删除「${deleteTarget[deleteNameField]}」吗？此操作不可撤销。`;
    return '确定要删除该记录吗？此操作不可撤销。';
  };

  const handleSort = (field: string) => {
    if (!sortable) return;
    setSortState((prev) => {
      if (!prev || prev.field !== field) return { field, order: 'asc' };
      if (prev.order === 'asc') return { field, order: 'desc' };
      return { field, order: 'asc' };
    });
  };

  /** 客户端排序：仅对当前页 rows 排序，不触发 fetcher、不影响 total */
  const displayRows = useMemo(() => {
    if (!sortable || !sortState || rows.length === 0) return rows;
    const sorted = [...rows].sort((a, b) => {
      const av = a[sortState.field];
      const bv = b[sortState.field];
      if (av === bv) return 0;
      if (av === null || av === undefined) return 1;
      if (bv === null || bv === undefined) return -1;
      let cmp: number;
      if (typeof av === 'number' && typeof bv === 'number') cmp = av - bv;
      else cmp = String(av).localeCompare(String(bv), 'zh-Hans-CN');
      return sortState.order === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [rows, sortable, sortState]);

  const createFields = fields.filter((f) => !f.hideOnCreate);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} spacing={2}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          {title}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          {searchable && (
            <TextField
              size="small"
              placeholder={searchPlaceholder ?? '搜索'}
              value={keywordInput}
              onChange={handleSearchChange}
              onKeyDown={(e) => e.key === 'Enter' && handleSearchEnter()}
              InputProps={{ endAdornment: <Search fontSize="small" /> }}
              sx={{ minWidth: 240 }}
            />
          )}
          <Button variant="contained" startIcon={<Add />} disabled={!canWrite} onClick={openCreate}>
            新建
          </Button>
        </Box>
      </Stack>

      <Card elevation={2}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {columns.map((c) => {
                  const isSortable = sortable && c.sortable;
                  const active = isSortable && sortState?.field === c.key;
                  return (
                    <TableCell
                      key={c.key}
                      sortDirection={active ? sortState!.order : false}
                      sx={isSortable ? { cursor: 'pointer', userSelect: 'none' } : undefined}
                      onClick={isSortable ? () => handleSort(c.key) : undefined}
                    >
                      {c.label}
                      {active ? (sortState!.order === 'asc' ? ' ▲' : ' ▼') : ''}
                    </TableCell>
                  );
                })}
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
                  <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 5 }} color="text.secondary">
                    暂无数据
                  </TableCell>
                </TableRow>
              ) : (
                displayRows.map((r) => (
                  <TableRow key={r[idField]}>
                    {columns.map((c) => (
                      <TableCell key={c.key}>
                        {c.render ? c.render(r) : String(r[c.key] ?? '-')}
                      </TableCell>
                    ))}
                    {canWrite && (
                      <TableCell align="right">
                        {rowActions && (
                          <Box component="span" sx={{ mr: 1 }}>
                            {rowActions(r)}
                          </Box>
                        )}
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
      <Dialog open={editing !== null} onClose={() => !busy && setEditing(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing && editing[idField] !== undefined ? `编辑 · #${editing[idField]}` : '新建'}</DialogTitle>
        <DialogContent sx={{ mt: 1 }}>
          {(editing ? fields : createFields).map((f) => {
            const isCreate = !(editing && editing[idField] !== undefined);
            // 多语言字段 · 新建态：渲染 5 语言输入框（强制必填）；编辑态退化为单 zh（走下方既有逻辑）
            if (f.multilingual && isCreate) {
              return (
                <MultilingualField
                  key={f.name}
                  base={f.name}
                  label={f.label}
                  form={form}
                  onChange={setForm}
                  required
                  multiline={f.type === 'textarea'}
                  disabled={busy}
                />
              );
            }
            const common = {
              key: f.name,
              fullWidth: true,
              margin: 'normal' as const,
              label: f.label + (f.required ? ' *' : ''),
              value: form[f.name] ?? '',
              onChange: (e: any) => setForm((prev) => ({ ...prev, [f.name]: e.target.value })),
              disabled: busy,
            };
            if (f.type === 'image') {
              const url = (form[f.name] as string) || '';
              return (
                <Box key={f.name} sx={{ mt: 1, mb: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    {f.label}
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 1 }}>
                    {url ? (
                      <img
                        src={url}
                        alt={f.label}
                        style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 8, border: '1px solid #eee' }}
                      />
                    ) : (
                      <Box
                        sx={{
                          width: 56,
                          height: 56,
                          border: '1px dashed',
                          borderColor: 'divider',
                          borderRadius: 1,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <Typography variant="caption" color="text.secondary">
                          无
                        </Typography>
                      </Box>
                    )}
                    {f.upload && (
                      <Button variant="outlined" component="label" disabled={busy || uploading}>
                        {uploading ? '上传中…' : '上传图片'}
                        <input
                          hidden
                          type="file"
                          accept="image/png,image/webp,image/jpeg"
                          onChange={async (e: any) => {
                            const file: File | undefined = e.target.files?.[0];
                            if (!file) return;
                            setUploading(true);
                            try {
                              const u = await f.upload!(file);
                              setForm((prev) => ({ ...prev, [f.name]: u }));
                              show('上传成功', 'success');
                            } catch (err) {
                              show(extractError(err), 'error');
                            } finally {
                              setUploading(false);
                              e.target.value = '';
                            }
                          }}
                        />
                      </Button>
                    )}
                  </Box>
                  {!f.upload && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                      （由行内「卡面管理 / 图标管理」维护）
                    </Typography>
                  )}
                </Box>
              );
            }
            if (f.type === 'textarea') return <TextField {...common} multiline minRows={3} />;
            // 文本/数字字段也可挂载「上传文件」按钮（如 APK 上传回填 URL），由 f.upload 提供
            if ((f.type === 'text' || f.type === 'number') && f.upload) {
              return (
                <Box key={f.name} sx={{ mt: 1, mb: 1 }}>
                  <TextField {...common} type={f.type === 'number' ? 'number' : 'text'} />
                  <Button
                    variant="outlined"
                    component="label"
                    sx={{ mt: 1 }}
                    disabled={busy || uploading}
                  >
                    {uploading ? '上传中…' : '上传文件'}
                    <input
                      hidden
                      type="file"
                      onChange={async (e: any) => {
                        const file: File | undefined = e.target.files?.[0];
                        if (!file) return;
                        setUploading(true);
                        try {
                          const u = await f.upload!(file);
                          setForm((prev) => ({ ...prev, [f.name]: u }));
                          show('上传成功', 'success');
                        } catch (err) {
                          show(extractError(err), 'error');
                        } finally {
                          setUploading(false);
                          e.target.value = '';
                        }
                      }}
                    />
                  </Button>
                </Box>
              );
            }
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
          <Button onClick={() => setEditing(null)} disabled={busy}>取消</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={busy}>
            {busy ? <CircularProgress size={18} color="inherit" /> : '保存'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* 删除确认 */}
      <Dialog open={!!deleteTarget} onClose={() => !deleting && setDeleteTarget(null)}>
        <DialogTitle>确认删除</DialogTitle>
        <DialogContent>{getDeleteText()}</DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={deleting}>取消</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={deleting}>
            {deleting ? <CircularProgress size={18} color="inherit" /> : '删除'}
          </Button>
        </DialogActions>
      </Dialog>

      {Feedback}
    </Box>
  );
}
