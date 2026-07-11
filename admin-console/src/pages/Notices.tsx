// 页面 B 组：公告管理（实体 = Notice，主键 id）
// 列表/创建/更新/删除分别映射 api/admin 的 listNotices / createNotice / updateNotice / deleteNotice
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listNotices, createNotice, updateNotice, deleteNotice } from '../api/admin';
import { Chip } from '@mui/material';

// 公告类型枚举（ACTIVITY/UPDATE/MAINTENANCE/NOTICE），作为下拉选项与表头渲染
const NOTICE_TYPES = ['ACTIVITY', 'UPDATE', 'MAINTENANCE', 'NOTICE'].map((v) => ({ label: v, value: v }));

// 表格列定义：title 与 type 可排序；type 用 Chip 展示，content 超长截断预览
const columns: ColumnDef[] = [
  { key: 'title', label: '标题', sortable: true },
  { key: 'type', label: '类型', sortable: true, render: (r: any) => <Chip size="small" label={r.type} color="info" /> },
  { key: 'content', label: '内容', render: (r: any) => <span style={{ display: 'inline-block', maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.content}</span> },
];

// 表单字段定义：title/content 标记 multilingual → 由 CrudPage 渲染 MultilingualField 多语言输入
const fields: FieldDef[] = [
  { name: 'title', label: '标题', required: true, multilingual: true },
  { name: 'content', label: '内容', type: 'textarea', required: true, multilingual: true },
  { name: 'type', label: '类型', type: 'select', options: NOTICE_TYPES, required: true },
];

/**
 * 公告管理页。
 * 通过 CrudPage 承载公告的列表、搜索、排序、增删改查；
 * title/content 为 multilingual 字段，由 CrudPage 自动切换 MultilingualField 多语言录入。
 */
export default function Notices() {
  return (
    <CrudPage
      title="公告管理"
      idField="id"
      columns={columns}
      fields={fields}
      fetcher={listNotices}
      creator={createNotice}
      updater={updateNotice}
      deleter={deleteNotice}
      searchable
      searchPlaceholder="搜索公告标题"
      deleteNameField="title"
      sortable
    />
  );
}
