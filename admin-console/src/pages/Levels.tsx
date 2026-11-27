// 页面 B 组：关卡管理（实体 = Level，整数主键 level_id）
// 列表/创建/更新/删除分别映射 api/admin 的 listLevels / createLevel / updateLevel / deleteLevel
import { useState } from 'react';
import { Box, Tab, Tabs } from '@mui/material';
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listLevels, createLevel, updateLevel, deleteLevel } from '../api/admin';
import LevelHealth from './LevelHealth';

// 表格列定义：level_id 与 difficulty 可排序；layout_data 为 JSON 字符串，列表超长截断预览
const columns: ColumnDef[] = [
  { key: 'level_id', label: '关卡 ID', sortable: true },
  { key: 'difficulty', label: '难度', sortable: true, render: (r: any) => (r.difficulty ?? '-') },
  { key: 'attempts', label: '闯关次数', sortable: true },
  { key: 'avg_score', label: '平均积分', sortable: true, render: (r: any) => Math.round(r.avg_score || 0) },
  { key: 'total_items_used', label: '使用道具数', sortable: true },
  { key: 'avg_time_ms', label: '平均用时', sortable: true, render: (r: any) => r.avg_time_ms ? `${(r.avg_time_ms / 1000).toFixed(1)}秒` : '-' },
  {
    key: 'layout_data',
    label: '布局数据',
    render: (r: any) => (
      <span style={{ display: 'inline-block', maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {typeof r.layout_data === 'string' ? r.layout_data.slice(0, 40) : '-'}
      </span>
    ),
  },
];

const fields: FieldDef[] = [
  // levels.level_id 是整数主键，新建时必须提供
  { name: 'level_id', label: '关卡 ID（整数）', type: 'number', required: true },
  { name: 'difficulty', label: '难度', type: 'number', nullable: true },
  { name: 'layout_data', label: '布局数据（JSON 字符串）', type: 'textarea', required: true },
];

export default function Levels() {
  const [tab, setTab] = useState<'list' | 'health'>('list');
  return (
    <>
      <Tabs value={tab} onChange={(_, value) => setTab(value)} aria-label="关卡管理视图"
        sx={{ mb: 2.5, borderBottom: 1, borderColor: 'divider' }}>
        <Tab id="level-list-tab" value="list" label="关卡列表" aria-controls="level-list-panel" />
        <Tab id="level-health-tab" value="health" label="关卡体检" aria-controls="level-health-panel" />
      </Tabs>
      <Box role="tabpanel" id="level-list-panel" aria-labelledby="level-list-tab" hidden={tab !== 'list'}>
        <CrudPage
          title="关卡管理"
          idField="level_id"
          columns={columns}
          fields={fields}
          fetcher={listLevels}
          creator={createLevel}
          updater={updateLevel}
          deleter={deleteLevel}
          getDeleteConfirmText={(row: any) => `确定要删除「关卡#${row.level_id}」吗？此操作不可撤销。`}
          sortable
        />
      </Box>
      <Box role="tabpanel" id="level-health-panel" aria-labelledby="level-health-tab" hidden={tab !== 'health'}>
        {tab === 'health' && <LevelHealth />}
      </Box>
    </>
  );
}
