// 页面 B 组：任务管理（实体 = Task，字符串主键 id，如 PLAY_3_GAMES）
// 列表/创建/更新/删除分别映射 api/admin 的 listTasks / createTask / updateTask / deleteTask
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listTasks, createTask, updateTask, deleteTask } from '../api/admin';

// 表格列定义：全部字段可排序（id/name/target_count/points_reward）
const columns: ColumnDef[] = [
  { key: 'id', label: '任务 ID', sortable: true },
  { key: 'name', label: '名称', sortable: true },
  { key: 'target_count', label: '目标次数', sortable: true },
  { key: 'points_reward', label: '积分奖励', sortable: true },
];

// 表单字段定义：name/description 为 multilingual → 由 CrudPage 渲染 MultilingualField 多语言输入
const fields: FieldDef[] = [
  // task.id 是字符串主键，新建时必须提供
  { name: 'id', label: '任务 ID（如 WIN_3_GAMES）', required: true },
  { name: 'name', label: '名称', required: true, multilingual: true },
  { name: 'description', label: '描述', type: 'textarea', required: true, multilingual: true },
  { name: 'target_count', label: '目标次数', type: 'number', required: true },
  { name: 'points_reward', label: '积分奖励', type: 'number', required: true },
];

/**
 * 任务管理页。
 * 通过 CrudPage 承载任务的列表、搜索、排序、增删改查；
 * name/description 为 multilingual 字段，由 CrudPage 自动切换 MultilingualField 多语言录入。
 */
export default function Tasks() {
  return (
    <CrudPage
      title="任务管理"
      idField="id"
      columns={columns}
      fields={fields}
      fetcher={listTasks}
      creator={createTask}
      updater={updateTask}
      deleter={deleteTask}
      searchable
      searchPlaceholder="搜索任务名称/ID"
      deleteNameField="name"
      sortable
    />
  );
}
