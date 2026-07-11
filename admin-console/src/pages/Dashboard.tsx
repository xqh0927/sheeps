import { useEffect, useState } from 'react';
import { Grid, Card, CardContent, Typography, Box, CircularProgress, IconButton, Stack } from '@mui/material';
import {
  Refresh,
  People,
  PersonAdd,
  Block,
  Campaign,
  Store,
  Assignment,
  GridView,
  SwapHoriz,
  Stars,
  Timeline,
  EmojiEvents,
} from '@mui/icons-material';
import { getStats, Stats } from '../api/admin';
import { extractError } from '../api/client';
import { useFeedback } from '../components/feedback';

interface StatCardDef {
  key: keyof Stats;
  label: string;
  icon: JSX.Element;
  color: string;
}

interface StatGroupDef {
  title: string;
  icon: JSX.Element;
  cards: StatCardDef[];
}

// 按「用户 / 内容 / 经济 / 无尽模式」四组重排（均来自 Stats 既有字段，无新增后端字段）
const GROUPS: StatGroupDef[] = [
  {
    title: '用户',
    icon: <People />,
    cards: [
      { key: 'users_total', label: '用户总数', icon: <People />, color: '#6a4c93' },
      { key: 'today_signup', label: '今日新增', icon: <PersonAdd />, color: '#2e7d32' },
      { key: 'banned_count', label: '已封禁用户', icon: <Block />, color: '#b71c1c' },
    ],
  },
  {
    title: '内容',
    icon: <Campaign />,
    cards: [
      { key: 'notice_count', label: '公告数', icon: <Campaign />, color: '#c2185b' },
      { key: 'shop_item_count', label: '商品数', icon: <Store />, color: '#00838f' },
      { key: 'task_count', label: '任务数', icon: <Assignment />, color: '#5e35b1' },
      { key: 'level_count', label: '关卡数', icon: <GridView />, color: '#827717' },
    ],
  },
  {
    title: '经济',
    icon: <Stars />,
    cards: [
      { key: 'exchange_total', label: '兑换次数', icon: <SwapHoriz />, color: '#1565c0' },
      { key: 'points_total', label: '积分发放总量', icon: <Stars />, color: '#ef6c00' },
    ],
  },
  {
    title: '无尽模式',
    icon: <Timeline />,
    cards: [
      { key: 'endless_play_count', label: '今日无尽挑战次数', icon: <Timeline />, color: '#00695c' },
      { key: 'endless_max_score', label: '无尽模式历史最高分', icon: <EmojiEvents />, color: '#ad1457' },
    ],
  },
];

/**
 * 数据概览（Dashboard）页面组件。
 *
 * 路由：通常路径 `/dashboard`，登录后默认落地页。
 * 权限要求：任意已登录管理员均可访问（只读展示，无写入操作）。
 * 关键 State：
 * - `stats`：服务端聚合统计（Stats），初始 null 表示尚未加载。
 * - `loading`：首屏加载态（控制整体骨架/loading）。
 * - `refreshing`：手动刷新态（不影响已展示数据的占位，仅控制刷新按钮 loading）。
 *
 * 数据来源：依赖 `api/admin.getStats()` 拉取全站聚合指标，前端按 GROUPS 配置分组渲染。
 */
export default function Dashboard() {
  // 服务端聚合统计结果；null 表示首屏尚未返回
  const [stats, setStats] = useState<Stats | null>(null);
  // 首屏加载态（loading 且尚无数据时显示整页 loading）
  const [loading, setLoading] = useState(true);
  // 手动刷新态（用于在右上角按钮展示刷新进度，区别于首屏 loading）
  const [refreshing, setRefreshing] = useState(false);
  // 全局反馈组件：show() 用于错误提示，Feedback 为挂载节点
  const { show, Feedback } = useFeedback();

  // 首屏加载：组件挂载即拉取统计
  // 依赖 [show]：仅 feedback 实例变化时重新执行；使用 alive 标志避免组件卸载后的 setState（防止内存泄漏）
  // 清理函数将 alive 置 false，确保异步回调在卸载后不再更新已卸载组件
  useEffect(() => {
    let alive = true;
    setLoading(true);
    getStats()
      .then((d) => {
        if (alive) setStats(d);
      })
      .catch((e) => {
        if (alive) show(extractError(e), 'error');
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [show]);

  // 手动刷新：复用 getStats 拉取最新数据并替换 stats；刷新期间刷新按钮显示进度
  const handleRefresh = () => {
    setRefreshing(true);
    getStats()
      .then((d) => setStats(d))
      .catch((e) => show(extractError(e), 'error'))
      .finally(() => setRefreshing(false));
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          数据概览
        </Typography>
        <IconButton onClick={handleRefresh} disabled={refreshing || loading} color="primary" title="刷新">
          {refreshing ? <CircularProgress size={20} /> : <Refresh />}
        </IconButton>
      </Stack>

      {loading && !stats ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box>
          {GROUPS.map((group) => (
            <Box key={group.title} sx={{ mb: 4 }}>
              {/* 分区标题 + 图标徽标 */}
              <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    borderRadius: 1.5,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    background: 'linear-gradient(135deg, #455a64, #78909c)',
                  }}
                >
                  {group.icon}
                </Box>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  {group.title}
                </Typography>
              </Stack>

              <Grid container spacing={2.5}>
                {group.cards.map((c) => (
                  <Grid item xs={12} sm={6} md={4} key={c.key}>
                    <Card
                      elevation={2}
                      sx={{
                        transition: 'transform 0.18s ease, box-shadow 0.18s ease',
                        '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 },
                      }}
                    >
                      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        {/* 图标徽标（渐变） */}
                        <Box
                          sx={{
                            width: 52,
                            height: 52,
                            borderRadius: 2,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            background: `linear-gradient(135deg, ${c.color}, ${c.color}cc)`,
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
            </Box>
          ))}
        </Box>
      )}
      {Feedback}
    </Box>
  );
}
