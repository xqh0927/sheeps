import { useEffect, useState } from 'react';
import { Card, CardContent, Typography, Stack, Switch, FormControlLabel, CircularProgress } from '@mui/material';
import { getGameModes, updateConfig } from '../api/admin';
import { useAuth } from '../store/auth';
import { useFeedback } from './feedback';
import { extractError } from '../api/client';

/**
 * 游戏模式开关卡片：无尽生存模式（默认关）/ 对战模式（默认关）。
 * 经 PUT /api/admin/config 写 gamemode_*。
 */
export default function GameModeSwitch() {
  const canWrite = useAuth((s) => s.canWrite());
  const { show, Feedback } = useFeedback();

  // 无尽生存模式开关状态（来自服务端配置）
  const [endless, setEndless] = useState(false);
  // 对战模式开关状态（来自服务端配置）
  const [battle, setBattle] = useState(false);
  // loading：初始化拉取配置中
  const [loading, setLoading] = useState(true);
  // saving：开关保存中，防止重复提交并禁用 Switch
  const [saving, setSaving] = useState(false);

  // 拉取游戏模式配置：getGameModes → 回填 endless/battle；失败经 useFeedback 提示
  const load = () => {
    setLoading(true);
    getGameModes()
      .then((d) => {
        setEndless(d.endless);
        setBattle(d.battle);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setLoading(false));
  };

  // 挂载时拉取一次游戏模式配置；无依赖、无定时器/订阅，无需清理
  useEffect(() => {
    load();
  }, []); // eslint-disable-line

  // 切换开关：校验写权限 → updateConfig(key, 'on'|'off') 写服务端 → 本地乐观更新对应状态；
  // 失败经 useFeedback 提示，finally 释放 saving
  const toggle = async (key: 'gamemode_endless' | 'gamemode_battle', next: boolean) => {
    if (!canWrite) return;
    setSaving(true);
    try {
      await updateConfig(key, next ? 'on' : 'off');
      show('已保存', 'success');
      if (key === 'gamemode_endless') setEndless(next);
      else setBattle(next);
    } catch (e) {
      show(extractError(e), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card elevation={2} sx={{ mb: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
          游戏模式开关
        </Typography>
        {loading ? (
          <CircularProgress size={24} />
        ) : (
          <Stack spacing={1}>
            <FormControlLabel
              control={
                <Switch
                  checked={endless}
                  disabled={!canWrite || saving}
                  onChange={(e) => toggle('gamemode_endless', e.target.checked)}
                />
              }
              label="无尽生存模式（默认关闭）"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={battle}
                  disabled={!canWrite || saving}
                  onChange={(e) => toggle('gamemode_battle', e.target.checked)}
                />
              }
              label="对战模式（默认关闭）"
            />
          </Stack>
        )}
      </CardContent>
      {Feedback}
    </Card>
  );
}
