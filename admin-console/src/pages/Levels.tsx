// 页面 B 组：关卡管理（实体 = Level，整数主键 level_id）
// 列表/创建/更新/删除分别映射 api/admin 的 listLevels / createLevel / updateLevel / deleteLevel
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listLevels, createLevel, updateLevel, deleteLevel } from '../api/admin';

// 表格列定义：level_id 与 difficulty 可排序；layout_data 为 JSON 字符串，列表超长截断预览
const columns: ColumnDef[] = [
  { key: 'level_id', label: '关卡 ID', sortable: true },
  { key: 'difficulty', label: '难度', sortable: true, render: (r: any) => (r.difficulty ?? '-') },
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
  { name: 'difficulty', label: '难度', type: 'number', nullable: true },
  { name: 'layout_data', label: '布局数据（JSON 字符串）', type: 'textarea', required: true },
];

/**
 * 关卡管理页。
 * 通过 CrudPage 泛型组件承载列表、搜索、排序、增删改查表单，
 * 业务仅声明列与字段，所有数据流（loading/error/分页/提交）由 CrudPage 统一管理。
 */
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
      searchable
      searchPlaceholder="搜索关卡 ID"
      getDeleteConfirmText={(row: any) => `确定要删除「关卡#${row.level_id}」吗？此操作不可撤销。`}
      sortable
    />
  );
}
