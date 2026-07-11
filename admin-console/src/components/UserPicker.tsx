import { useState, useRef, useEffect } from 'react';
import { TextField, Paper, Box, ListItemButton, CircularProgress } from '@mui/material';
import { searchUsers } from '../api/admin';
import { extractError } from '../api/client';
import { useFeedback } from './feedback';

interface UserPickerProps {
  /** 已选中的用户 id（受控） */
  value?: string;
  /** 选中回调：返回 user_id 与 username */
  onChange: (userId: string, username?: string) => void;
  /** 编辑回填时传入已选用户名（仅展示用） */
  initialLabel?: string;
  label?: string;
}

interface UserHit {
  id: string;
  username: string;
  phone: string;
}

/**
 * 用户选择器：输入手机号 / 昵称 → 调 GET /api/admin/users/search 反查，
 * 下拉选用户回填 id。供排行榜手动改分使用。
 */
export default function UserPicker({ value, onChange, initialLabel, label = '用户（手机号/昵称搜索）' }: UserPickerProps) {
  const { show, Feedback } = useFeedback();
  // 输入框文本（受控），初始回填 initialLabel
  const [query, setQuery] = useState(initialLabel || '');
  // 搜索结果列表
  const [results, setResults] = useState<UserHit[]>([]);
  // 下拉面板是否展开
  const [open, setOpen] = useState(false);
  // loading：搜索请求中
  const [loading, setLoading] = useState(false);
  // 防抖定时器引用，避免输入过程中频繁请求
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 编辑回填：initialLabel 变化时若当前无输入则补回填显；
  // 清理函数在卸载时清除未触发的 debounce 定时器，避免内存泄漏
  useEffect(() => {
    if (initialLabel && !query) setQuery(initialLabel);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [initialLabel]); // eslint-disable-line

  // 执行搜索：空查询直接关闭；否则调 searchUsers（GET /api/admin/users/search）反查用户，
  // loading 态经 useFeedback 提示错误
  const doSearch = (q: string) => {
    if (!q.trim()) {
      setResults([]);
      setOpen(false);
      return;
    }
    setLoading(true);
    searchUsers(q)
      .then((d) => {
        setResults(d.list || []);
        setOpen(true);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  };

  // 输入变化：更新 query、展开面板，并以 300ms 防抖触发 doSearch，避免每次按键都请求
  const handleChange = (e: any) => {
    const v = e.target.value;
    setQuery(v);
    setOpen(true);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => doSearch(v), 300);
  };

  // 选中用户：回填展示文本并回调 onChange(user_id, username) 给父组件
  const select = (u: UserHit) => {
    setQuery(`${u.username}（${u.phone}）`);
    setOpen(false);
    onChange(u.id, u.username);
  };

  return (
    <Box sx={{ position: 'relative', width: '100%' }}>
      <TextField
        fullWidth
        size="small"
        label={label}
        value={query}
        onChange={handleChange}
        placeholder="输入手机号或昵称搜索"
        onFocus={() => results.length > 0 && setOpen(true)}
      />
      {open && (
        loading ? (
          <Paper sx={{ position: 'absolute', zIndex: 20, mt: 0.5, p: 1, width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'center' }}>
              <CircularProgress size={18} />
            </Box>
          </Paper>
        ) : results.length > 0 ? (
          <Paper sx={{ position: 'absolute', zIndex: 20, mt: 0.5, width: '100%', maxHeight: 240, overflow: 'auto' }}>
            {results.map((u) => (
              <ListItemButton key={u.id} onClick={() => select(u)}>
                <Box>
                  <Box sx={{ fontWeight: 600 }}>{u.username}</Box>
                  <Box sx={{ fontSize: 12, color: 'text.secondary' }}>
                    {u.phone} · {u.id}
                  </Box>
                </Box>
              </ListItemButton>
            ))}
          </Paper>
        ) : null
      )}
      {Feedback}
    </Box>
  );
}
