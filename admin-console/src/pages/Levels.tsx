import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listLevels, createLevel, updateLevel, deleteLevel } from '../api/admin';

const columns: ColumnDef[] = [
  { key: 'level_id', label: '关卡 ID' },
  { key: 'difficulty', label: '难度', render: (r: any) => (r.difficulty ?? '-') },
  {
    key: 'layout_data',
    label: '布局数据',
    render: (r: any) => (
      <span style={{ display: 'inline-block', maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {typeof r.layout_data === 'string' ? r.layout_data.slice(0, 60) : '-'}
      </span>
    ),
  },
];

const fields: FieldDef[] = [
  // levels.level_id 是整数主键，新建时必须提供
  { name: 'level_id', label: '关卡 ID（整数）', type: 'number', required: true },
  { name: 'difficulty', label: '难度', type: 'number' },
  { name: 'layout_data', label: '布局数据（JSON 字符串）', type: 'textarea', required: true },
];

export default function Levels() {
  return (
    <CrudPage
      title="关卡管理"
      idField="level_id"
      columns={columns}
      fields={fields}
      fetcher={listLevels}
      creator={createLevel}
      updater={updateLevel}
      deleter={deleteLevel}
    />
  );
}
