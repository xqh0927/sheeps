// 页面 B 组：App 版本管理（实体 = AppVersion，整数主键 version_code）
// 列表/创建/更新/删除分别映射 listAppVersions / createAppVersion / updateAppVersion / deleteAppVersion
// uploadApk 用于上传 APK 文件到 R2，返回 URL 回填 download_url
import CrudPage, { ColumnDef, FieldDef } from '../components/CrudPage';
import { uploadApk, listAppVersions, createAppVersion, updateAppVersion, deleteAppVersion } from '../api/admin';
import { Chip } from '@mui/material';

// 状态下拉选项：0=草稿 / 1=已发布 / 2=已下线（与后端枚举对应）
const STATUS_OPTIONS = [
  { label: '草稿', value: '0' },
  { label: '已发布', value: '1' },
  { label: '已下线', value: '2' },
];

// 是否强制更新下拉选项：0=否 / 1=是
const FORCE_OPTIONS = [
  { label: '否', value: '0' },
  { label: '是', value: '1' },
];

// 状态展示映射：数字 → Chip 颜色与中文文案
const STATUS_CHIP: Record<number, 'default' | 'success' | 'error'> = {
  0: 'default',
  1: 'success',
  2: 'error',
};
const STATUS_TEXT: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已下线' };

/** 上传 APK 到 R2，返回 URL 回填 download_url */
const uploadApkFile = async (file: File): Promise<string> => {
  const fd = new FormData();
  fd.append('file', file);
  const r = await uploadApk(fd);
  return r.url;
};

// 状态展示映射：数字 → Chip 颜色与中文文案
const columns: ColumnDef[] = [
  { key: 'version_code', label: '版本号', sortable: true },
  { key: 'version_name', label: '版本名称' },
  {
    key: 'download_url',
    label: '下载链接',
    render: (row: any) => {
      const url = row.download_url || row.apk_url;
      if (!url) return <span style={{ color: '#999' }}>—</span>;
      return (
        <a href={url} target="_blank" rel="noreferrer">
          {url}
        </a>
      );
    },
  },
  {
    key: 'status',
    label: '状态',
    render: (row: any) => {
      const s = Number(row.status) || 0;
      return <Chip size="small" color={STATUS_CHIP[s]} label={STATUS_TEXT[s]} />;
    },
  },
  { key: 'apk_url', label: '原始 APK', render: (row: any) => (row.apk_url ? <span style={{ fontSize: 12 }}>{row.apk_url}</span> : '—') },
  { key: 'is_force_update', label: '强制更新', render: (row: any) => (Number(row.is_force_update) === 1 ? '是' : '否') },
  { key: 'release_time', label: '发布时间', render: (row: any) => (row.release_time ? new Date(row.release_time).toLocaleString() : '—') },
  { key: 'created_at', label: '创建时间', render: (row: any) => (row.created_at ? new Date(row.created_at).toLocaleString() : '—') },
];

const fields: FieldDef[] = [
  { name: 'version_code', label: '版本号(version_code)', type: 'number', required: true },
  { name: 'version_name', label: '版本名称', type: 'text', required: true },
  { name: 'apk_url', label: '原始 APK 链接', type: 'text' },
  {
    name: 'download_url',
    label: '下载链接（外部URL，或上传APK）',
    type: 'text',
    upload: uploadApkFile,
  },
  { name: 'update_log', label: '更新日志', type: 'textarea', multilingual: true },
  { name: 'is_force_update', label: '强制更新', type: 'select', options: FORCE_OPTIONS },
  { name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS },
  { name: 'created_at', label: '创建时间(ms)', type: 'number', hideOnCreate: true, nullable: true },
];

/**
 * App 版本管理页。
 * 通过 CrudPage 承载版本列表、搜索、增删改查；
 * download_url 字段支持上传 APK（uploadApkFile）回填 R2 URL，update_log 为 multilingual 字段。
 */
export default function AppVersions() {
  return (
    <CrudPage
      title="App 版本管理"
      idField="version_code"
      columns={columns}
      fields={fields}
      fetcher={listAppVersions}
      creator={createAppVersion}
      updater={updateAppVersion}
      deleter={deleteAppVersion}
      searchable
      searchPlaceholder="搜索版本号/名称"
    />
  );
}
