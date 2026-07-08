import { useEffect, useState } from 'react';
import { Grid, Card, CardContent, Typography, Box, CircularProgress } from '@mui/material';
import {
  People,
  PersonAdd,
  SwapHoriz,
  Stars,
  Campaign,
  Block,
  Store,
  Assignment,
  GridView,
  Timeline,
  EmojiEvents,
} from '@mui/icons-material';
import { getStats, Stats } from '../api/admin';
import { extractError } from '../api/client';
import { useFeedback } from '../components/feedback';

const STAT_CARDS: { key: keyof Stats; label: string; icon: JSX.Element; color: string }[] = [
  { key: 'users_total', label: '用户总数', icon: <People />, color: '#6a4c93' },
  { key: 'today_signup', label: '今日新增', icon: <PersonAdd />, color: '#2e7d32' },
  { key: 'exchange_total', label: '兑换次数', icon: <SwapHoriz />, color: '#1565c0' },
  { key: 'points_total', label: '积分发放总量', icon: <Stars />, color: '#ef6c00' },
  { key: 'notice_count', label: '公告数', icon: <Campaign />, color: '#c2185b' },
  { key: 'banned_count', label: '已封禁用户', icon: <Block />, color: '#b71c1c' },
  { key: 'shop_item_count', label: '商品数', icon: <Store />, color: '#00838f' },
  { key: 'task_count', label: '任务数', icon: <Assignment />, color: '#5e35b1' },
  { key: 'level_count', label: '关卡数', icon: <GridView />, color: '#827717' },
  { key: 'endless_play_count', label: '今日无尽挑战次数', icon: <Timeline />, color: '#00695c' },
  { key: 'endless_max_score', label: '无尽模式历史最高分', icon: <EmojiEvents />, color: '#ad1457' },
];

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const { show, Feedback } = useFeedback();

  useEffect(() => {
    let alive = true;
    getStats()
      .then((d) => {
        if (alive) setStats(d);
      })
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [show]);

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 700 }}>
        数据概览
      </Typography>
      {loading && !stats ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={2.5}>
          {STAT_CARDS.map((c) => (
            <Grid item xs={12} sm={6} md={4} key={c.key}>
              <Card elevation={2}>
                <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box
                    sx={{
                      width: 52,
                      height: 52,
                      borderRadius: 2,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fff',
                      bgcolor: c.color,
                    }}
                  >
                    {c.icon}
                  </Box>
                  <Box>
                    <Typography variant="h5" sx={{ fontWeight: 700 }}>
                      {stats ? (stats[c.key] as number) : '-'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {c.label}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
      {Feedback}
    </Box>
  );
}
