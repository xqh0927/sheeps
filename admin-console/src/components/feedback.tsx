import { useState, useCallback } from 'react';
import { Snackbar, Alert } from '@mui/material';

type Severity = 'success' | 'error' | 'info' | 'warning';

/**
 * 轻量级全局反馈 Hook：用于向用户弹出一次性 Toast 提示（成功/错误/信息/警告）。
 *
 * 返回值：
 * - `show(text, severity?)`：命令式触发提示；severity 默认 'info'。
 * - `Feedback`：需渲染在组件树中的 Snackbar 节点（3.2s 自动关闭，可手动关闭）。
 *
 * 副作用：仅在调用 show 时通过 setState 改变自身 msg，无全局订阅/定时器泄漏。
 */
export function useFeedback() {
  // 当前展示的提示内容；null 表示无提示（不渲染 Snackbar）
  const [msg, setMsg] = useState<{ text: string; severity: Severity } | null>(null);

  // 触发提示；依赖稳定（[]），可在不同渲染周期安全复用
  const show = useCallback((text: string, severity: Severity = 'info') => {
    setMsg({ text, severity });
  }, []);

  const Feedback = msg ? (
    <Snackbar
      open
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      autoHideDuration={3200}
      onClose={() => setMsg(null)}
    >
      <Alert severity={msg.severity} variant="filled" onClose={() => setMsg(null)}>
        {msg.text}
      </Alert>
    </Snackbar>
  ) : null;

  return { show, Feedback };
}
