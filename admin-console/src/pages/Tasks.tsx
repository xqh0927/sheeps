import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listTasks, createTask, updateTask, deleteTask } from '../api/admin';

const columns: ColumnDef[] = [
  { key: 'id', label: '任务 ID', sortable: true },
  { key: 'name', label: '名称', sortable: true },
  { key: 'target_count', label: '目标次数', sortable: true },
  { key: 'points_reward', label: '积分奖励', sortable: true },
];

const fields: FieldDef[] = [
  // task.id 是字符串主键，新建时必须提供
  { name: 'id', label: '任务 ID（如 PLAY_3_GAMES）', required: true },
  { name: 'name', label: '名称', required: true, multilingual: true },
  { name: 'description', label: '描述', type: 'textarea', required: true, multilingual: true },
  { name: 'target_count', label: '目标次数', type: 'number', required: true },
  { name: 'points_reward', label: '积分奖励', type: 'number', required: true },
];

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
