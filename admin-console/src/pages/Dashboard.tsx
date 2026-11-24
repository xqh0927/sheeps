import { useEffect, useState } from 'react';
import { Alert, Button, Skeleton } from '@mui/material';
import { RefreshRounded } from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { getStats, Stats } from '../api/admin';
import { extractError } from '../api/client';
import './Dashboard.css';

type Metric = { key: keyof Stats; label: string; unit: string };

const accountMetrics: Metric[] = [
  { key: 'banned_count', label: '已封禁用户', unit: '人' },
  { key: 'exchange_total', label: '累计兑换记录', unit: '笔' },
  { key: 'points_total', label: '累计发放积分', unit: '分' },
];

const contentMetrics: Metric[] = [
  { key: 'notice_count', label: '公告', unit: '条' },
  { key: 'shop_item_count', label: '商品', unit: '种' },
  { key: 'task_count', label: '任务', unit: '项' },
  { key: 'level_count', label: '关卡', unit: '关' },
];

const dailyMetrics = [
  {
    key: 'today_signup', label: '新增用户', unit: '人',
    context: { key: 'users_total', label: '用户总数', unit: '人' },
  },
  {
    key: 'endless_play_count', label: '无尽成绩记录', unit: '条',
    context: { key: 'endless_max_score', label: '无尽榜最高分', unit: '分' },
  },
] satisfies (Metric & { context: Metric })[];

const numberFormat = new Intl.NumberFormat('zh-CN');
const timeFormat = new Intl.DateTimeFormat('zh-CN', {
  month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
});

function MetricValue({ value, unit }: { value: number | undefined; unit: string }) {
  return value === undefined ? (
    <Skeleton className="ops-value-skeleton" aria-hidden="true" width="3ch" />
  ) : (
    <>{numberFormat.format(value)}<span className="ops-unit">{unit}</span></>
  );
}

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [request, setRequest] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getStats().then((data) => {
      if (!active) return;
      setStats(data);
      setUpdatedAt(new Date());
    }).catch((reason) => {
      if (active) setError(extractError(reason));
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [request]);

  function refresh() {
    setLoading(true);
    setError('');
    setRequest((previous) => previous + 1);
  }

  return (
    <div className="ops-dashboard">
      <header className="ops-header">
        <div>
          <h1>运营概览</h1>
          <div className="ops-update" role="status">
            {loading ? (stats ? '正在刷新统计数据…' : '正在加载统计数据…') :
              updatedAt ? <>上次更新 <time dateTime={updatedAt.toISOString()}>{timeFormat.format(updatedAt)}</time></> : '尚未取得统计数据'}
          </div>
        </div>
        <Button className="ops-refresh" variant="outlined" startIcon={<RefreshRounded />} onClick={refresh} disabled={loading}>
          {error && !stats ? '重新加载' : '刷新数据'}
        </Button>
      </header>

      {error && (
        <Alert severity="error">
          {stats ? '刷新失败，当前显示上次取得的数据。' : '统计数据加载失败。'}{error}
        </Alert>
      )}

      {(stats || loading) && (
        <div className="ops-sections" aria-busy={loading}>
          <section className="ops-today" aria-labelledby="ops-today-title">
            <header className="ops-today-header">
              <h2 id="ops-today-title">今日动态</h2>
              <span>按服务器自然日统计</span>
            </header>
            <div className="ops-today-metrics">
              {dailyMetrics.map(({ key, label, unit, context }) => (
                <dl className="ops-daily-metric" key={key}>
                  <dt>{label}</dt>
                  <dd className="ops-daily-value"><MetricValue value={stats?.[key]} unit={unit} /></dd>
                  <dt className="ops-context-label">{context.label}</dt>
                  <dd className="ops-context-value"><MetricValue value={stats?.[context.key]} unit={context.unit} /></dd>
                </dl>
              ))}
            </div>
          </section>

          <div className="ops-detail-grid">
            <section className="ops-panel" aria-labelledby="ops-content-title">
              <h2 id="ops-content-title">内容配置</h2>
              <dl className="ops-inventory">
                {contentMetrics.map(({ key, label, unit }) => (
                  <div key={key}>
                    <dt>{label}</dt>
                    <dd><MetricValue value={stats?.[key]} unit={unit} /></dd>
                  </div>
                ))}
              </dl>
            </section>

            <section className="ops-panel" aria-labelledby="ops-account-title">
              <header className="ops-panel-header">
                <h2 id="ops-account-title">用户与积分</h2>
                <Link to="/users">用户管理 <span aria-hidden="true">→</span></Link>
              </header>
              <dl className="ops-ledger">
                {accountMetrics.map(({ key, label, unit }) => (
                  <div key={key}>
                    <dt>{label}</dt>
                    <dd><MetricValue value={stats?.[key]} unit={unit} /></dd>
                  </div>
                ))}
              </dl>
            </section>
          </div>
        </div>
      )}
    </div>
  );
}
