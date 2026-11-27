import { FormEvent, useEffect, useState } from 'react';
import {
  Box, Button, Card, CircularProgress, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material';
import { LevelHealthRow, listLevelHealth, PageResult } from '../api/admin';
import { extractError } from '../api/client';

const percent = new Intl.NumberFormat('zh-CN', { style: 'percent', maximumFractionDigits: 1 });
const emptyFilters = { level: '', version: '' };

function RateCell({ rate, numerator, denominator }: {
  rate: number | null; numerator: number; denominator: number;
}) {
  return (
    <TableCell align="right">
      <Typography variant="body2" sx={{ fontWeight: 600 }} color={rate === null ? 'text.secondary' : 'text.primary'}>
        {rate === null ? '无样本' : percent.format(rate)}
      </Typography>
      <Typography variant="caption" color="text.secondary">{numerator} / {denominator}</Typography>
    </TableCell>
  );
}

export default function LevelHealth() {
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [reload, setReload] = useState(0);
  const [result, setResult] = useState<PageResult<LevelHealthRow> | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setResult(null);
    setError('');
    listLevelHealth({
      page: page + 1, pageSize, level_id: filters.level ? Number(filters.level) : undefined,
      layout_version: filters.version,
    }, controller.signal).then((data) => {
      if (controller.signal.aborted) return;
      const lastPage = Math.max(0, Math.ceil(data.total / pageSize) - 1);
      if (page > lastPage) setPage(lastPage);
      else setResult(data);
    }).catch((err) => {
      if (!controller.signal.aborted) setError(extractError(err));
    });
    return () => controller.abort();
  }, [page, pageSize, filters, reload]);

  const applyFilters = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setFilters({ level: draft.level, version: draft.version.trim() });
  };

  const resetFilters = () => {
    setDraft(emptyFilters);
    setFilters(emptyFilters);
    setPage(0);
  };

  return (
    <Box>
      <Typography component="h1" variant="h5">关卡体检</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, mb: 1 }}>
        按关号和棋盘版本统计。百分比下方显示命中局数 / 样本局数。
      </Typography>
      <Box component="details" sx={{ mb: 2.5, fontSize: 14, color: 'text.secondary', '& summary': { cursor: 'pointer', color: 'primary.main' } }}>
        <summary>统计口径</summary>
        <Box component="ul" sx={{ pl: 3, mb: 0, lineHeight: 1.85 }}>
          <li>首通率按玩家在该关、该版本的首次开局计算。分母为已结算的首次开局，分子为其中通关的局数。</li>
          <li>败后重试率按失败局计算。同一玩家此后再次开局挑战同一关，即计作重试。换版重试归入失败时的版本。</li>
          <li>道具使用率按已结算局计算，一局内使用一个或多个道具都计作一局。</li>
          <li>主动重开时，已结算的弃局计入道具使用率的样本。若该局是首次开局，也计入首通率的样本。败后重试率仅统计判负的对局。</li>
          <li>开局数包含尚未结算的开局。只有带棋盘版本的开局记录才纳入统计。</li>
        </Box>
      </Box>
      <Card>
        <Stack component="form" onSubmit={applyFilters} direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5} sx={{ p: 2 }} alignItems={{ sm: 'center' }}>
          <TextField label="关号" placeholder="全部关卡" size="small" type="number"
            inputProps={{ min: 1, step: 1, max: Number.MAX_SAFE_INTEGER }}
            value={draft.level} onChange={(e) => setDraft({ ...draft, level: e.target.value })}
            sx={{ width: { sm: 135 } }} />
          <TextField label="棋盘版本" placeholder="完整版本号" size="small" inputProps={{ maxLength: 128 }}
            value={draft.version} onChange={(e) => setDraft({ ...draft, version: e.target.value })} />
          <Stack direction="row" spacing={1}>
            <Button type="submit" variant="contained">筛选</Button>
            <Button onClick={resetFilters} disabled={!draft.level && !draft.version && !filters.level && !filters.version}>重置</Button>
          </Stack>
        </Stack>
        <TableContainer>
          <Table aria-label="关卡体检" size="small" sx={{ minWidth: 590 }}>
            <TableHead>
              <TableRow>
                <TableCell>关号</TableCell>
                <TableCell>棋盘版本</TableCell>
                <TableCell align="right">开局数</TableCell>
                <TableCell align="right">已结算</TableCell>
                <TableCell align="right">首通率</TableCell>
                <TableCell align="right">败后重试率</TableCell>
                <TableCell align="right">道具使用率</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {result?.list.length ? result.list.map((row) => (
                <TableRow hover key={`${row.level_id}:${row.layout_version}`}>
                  <TableCell>{row.level_id}</TableCell>
                  <TableCell sx={{ maxWidth: 240, overflowWrap: 'anywhere' }}>{row.layout_version}</TableCell>
                  <TableCell align="right">{row.attempts}</TableCell>
                  <TableCell align="right">{row.settled_attempts}</TableCell>
                  <RateCell rate={row.first_win_rate} numerator={row.first_wins} denominator={row.first_settled_attempts} />
                  <RateCell rate={row.retry_rate} numerator={row.retried_failures} denominator={row.failed_attempts} />
                  <RateCell rate={row.item_use_rate} numerator={row.item_attempts} denominator={row.settled_attempts} />
                </TableRow>
              )) : (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ height: 130 }}>
                    {error ? (
                      <Stack alignItems="center" spacing={1}>
                        <Typography role="alert" variant="body2">体检数据加载失败。{error}</Typography>
                        <Button size="small" onClick={() => setReload((v) => v + 1)}>重新加载</Button>
                      </Stack>
                    ) : result ? (
                      <Typography role="status" color="text.secondary" variant="body2">
                        {filters.level || filters.version ? '当前筛选条件下暂无样本' : '暂无带棋盘版本的开局样本'}
                      </Typography>
                    ) : <CircularProgress size={24} aria-label="正在加载体检数据" />}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        {result && result.total > 0 && (
          <TablePagination component="div" count={result.total} page={page} rowsPerPage={pageSize}
            onPageChange={(_, next) => setPage(next)}
            rowsPerPageOptions={[10, 20, 50]} labelRowsPerPage="每页条数"
            onRowsPerPageChange={(e) => { setPageSize(Number(e.target.value)); setPage(0); }}
            labelDisplayedRows={({ from, to, count }) => `${from}–${to} / 共 ${count} 条`}
            getItemAriaLabel={(type) => ({ first: '第一页', last: '最后一页', next: '下一页', previous: '上一页' }[type])}
            sx={{ '& .MuiTablePagination-toolbar': { flexWrap: 'wrap', px: 1 } }} />
        )}
      </Card>
    </Box>
  );
}
