import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listNotices, createNotice, updateNotice, deleteNotice } from '../api/admin';
import { Chip } from '@mui/material';

const NOTICE_TYPES = ['ACTIVITY', 'UPDATE', 'MAINTENANCE', 'NOTICE'].map((v) => ({ label: v, value: v }));

const columns: ColumnDef[] = [
  { key: 'title', label: '标题' },
  { key: 'type', label: '类型', render: (r: any) => <Chip size="small" label={r.type} color="info" /> },
  { key: 'content', label: '内容', render: (r: any) => <span style={{ display: 'inline-block', maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.content}</span> },
];

const fields: FieldDef[] = [
  { name: 'title', label: '标题', required: true },
  { name: 'content', label: '内容', type: 'textarea', required: true },
  { name: 'type', label: '类型', type: 'select', options: NOTICE_TYPES, required: true },
];

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
    />
  );
}
