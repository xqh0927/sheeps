import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { listShopItems, createShopItem, updateShopItem, deleteShopItem } from '../api/admin';
import { Chip } from '@mui/material';

const ITEM_TYPES = [
  'UNDO', 'MOVEOUT', 'SHUFFLE', 'REVIVE', 'HINT', 'BOMB', 'JOKER', 'DOUBLE_POINTS',
  'SKIN_INK', 'SKIN_CYBER', 'SKIN_HENAN', 'SKIN_SICHUAN', 'SKIN_SHUANG',
].map((v) => ({ label: v, value: v }));

const columns: ColumnDef[] = [
  { key: 'name', label: '名称' },
  { key: 'item_type', label: '类型', render: (r: any) => <Chip size="small" label={r.item_type} /> },
  { key: 'points_price', label: '积分价' },
  { key: 'stock', label: '库存' },
  { key: 'image_url', label: '图标', render: (r: any) => (r.image_url ? <a href={r.image_url} target="_blank" rel="noreferrer">链接</a> : '-') },
];

const fields: FieldDef[] = [
  { name: 'name', label: '名称', required: true },
  { name: 'description', label: '描述', type: 'textarea' },
  { name: 'item_type', label: '道具类型', type: 'select', options: ITEM_TYPES, required: true },
  { name: 'points_price', label: '积分价格', type: 'number', required: true },
  { name: 'stock', label: '库存', type: 'number' },
  { name: 'image_url', label: '图标 URL' },
];

export default function ShopItems() {
  return (
    <CrudPage
      title="商品管理"
      idField="id"
      columns={columns}
      fields={fields}
      fetcher={listShopItems}
      creator={createShopItem}
      updater={updateShopItem}
      deleter={deleteShopItem}
    />
  );
}
