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

/**
 * 系统配置（Config）页面组件。
 *
 * 路由：通常路径 `/config`。
 * 权限要求：仅 `canWrite()` 为 true 的管理员可执行新增/编辑/清空缓存；只读用户仅可查看。
 * 关键 State：
 * - `rows`：配置键值对列表（来自 getConfig）。
 * - `editing` / `creating`：当前编辑/新增弹窗的目标行（null 表示关闭）。
 * - `formKey` / `formValue`：弹窗表单字段。
 * - `busy`：提交中态，禁用表单与按钮防止重复提交。
 *
 * 数据来源：依赖 `api/admin.getConfig`、`updateConfig`、`clearCache`，数据变更后调用 load() 重新拉取。
 */
export default function Config() {
  // 写入权限信号：来自 auth store 的 canWrite()，控制按钮禁用与表单可编辑性
  const canWrite = useAuth((s) => s.canWrite());
  // 全局反馈组件
  const { show, Feedback } = useFeedback();

  // 配置行列表（key/value 对）
  const [rows, setRows] = useState<ConfigRow[]>([]);
  // 首屏/重载加载态
  const [loading, setLoading] = useState(true);
  // 当前正在编辑的配置行；非空即打开编辑弹窗
  const [editing, setEditing] = useState<ConfigRow | null>(null);
  // 新增模式开关
  const [creating, setCreating] = useState(false);
  // 表单：配置 key（编辑时锁定，仅新增可填）
  const [formKey, setFormKey] = useState('');
  // 表单：配置 value
  const [formValue, setFormValue] = useState('');
  // 提交中态：保存期间禁用所有操作，防止重复提交
  const [busy, setBusy] = useState(false);

  // 拉取配置列表：调用 getConfig 并用 loading 包裹，异常归一化为错误提示
  const load = () => {
    setLoading(true);
    getConfig()
      .then((d) => setRows(d.list))
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  };

  // 首屏加载：组件挂载时拉取一次配置（空依赖，仅执行一次）
  useEffect(() => {
    load();
  }, []); // eslint-disable-line

  // 打开编辑：把目标行的 key/value 灌入表单
  const openEdit = (row: ConfigRow) => {
    setFormKey(row.key);
    setFormValue(row.value);
    setEditing(row);
  };

  // 打开新增：清空表单并进入新增模式
  const openCreate = () => {
    setFormKey('');
    setFormValue('');
    setCreating(true);
  };

  // 保存：校验 key 非空后调用 updateConfig 提交；成功后关闭弹窗并重新拉取
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
          {/* 清空缓存：仅 canWrite 可操作；调用 clearCache 并反馈清空键数量 */}
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
