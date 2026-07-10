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

  const [endless, setEndless] = useState(false);
  const [battle, setBattle] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

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

  useEffect(() => {
    load();
  }, []); // eslint-disable-line

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
