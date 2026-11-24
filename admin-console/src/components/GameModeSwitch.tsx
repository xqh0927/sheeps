import { useState } from 'react';
import { Card, CardContent, Typography, Stack, Switch, FormControlLabel, CircularProgress } from '@mui/material';
import { updateConfig } from '../api/admin';
import { useAuth } from '../store/auth';
import { useFeedback } from './feedback';
import { extractError } from '../api/client';

interface GameModeSwitchProps {
  endless: boolean;
  battle: boolean;
  loading: boolean;
  onSaved: () => void;
}

export default function GameModeSwitch({ endless, battle, loading, onSaved }: GameModeSwitchProps) {
  const isSuper = useAuth((s) => s.isSuper());
  const { show, Feedback } = useFeedback();
  const [saving, setSaving] = useState(false);

  const toggle = async (key: 'gamemode_endless' | 'gamemode_battle', next: boolean) => {
    if (!isSuper) return;
    setSaving(true);
    try {
      await updateConfig(key, next ? 'on' : 'off');
      show('已保存', 'success');
      onSaved();
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
                  disabled={!isSuper || saving}
                  onChange={(e) => toggle('gamemode_endless', e.target.checked)}
                />
              }
              label="无尽生存模式"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={battle}
                  disabled={!isSuper || saving}
                  onChange={(e) => toggle('gamemode_battle', e.target.checked)}
                />
              }
              label="对战模式"
            />
          </Stack>
        )}
      </CardContent>
      {Feedback}
    </Card>
  );
}
