import { useState, useMemo } from 'react';
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { Button, Chip } from '@mui/material';
import {
  listShopItems,
  createShopItem,
  updateShopItem,
  deleteShopItem,
} from '../api/admin';
import ItemIconManager from '../components/ItemIconManager';
import { makeShopItemsFetcher } from '../utils/filteredShopItemsFetcher';
import { PROP_ITEM_TYPES } from '../constants/shopItemTypes';

const columns: ColumnDef[] = [
  { key: 'name', label: '名称', sortable: true },
  {
    key: 'item_type',
    label: '类型',
    sortable: true,
    render: (r: any) => <Chip size="small" label={r.item_type} />,
  },
  { key: 'points_price', label: '积分价', sortable: true },
  { key: 'stock', label: '库存', sortable: true },
  {
    key: 'image_url',
    label: '图标',
    render: (r: any) =>
      r.image_url ? (
        <img
          src={r.image_url}
          alt=""
          style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 6 }}
        />
      ) : (
        <Chip size="small" label="未设置" variant="outlined" />
      ),
  },
];

const fields: FieldDef[] = [
  { name: 'name', label: '名称', required: true, multilingual: true },
  { name: 'description', label: '描述', type: 'textarea', multilingual: true },
  { name: 'item_type', label: '道具类型', type: 'select', options: PROP_ITEM_TYPES, required: true },
  { name: 'points_price', label: '积分价格', type: 'number', required: true },
  { name: 'stock', label: '库存', type: 'number', nullable: true },
  { name: 'image_url', label: '图标（由图标管理维护）', type: 'image' },
];

export default function PropProducts() {
  // fetcher 必须用 useMemo 固定引用，否则 CrudPage 会因依赖变化而无限重拉。
  // 道具 = 非 SKIN_* 分区。
  const fetcher = useMemo(
    () => makeShopItemsFetcher((r) => typeof r.item_type === 'string' && !r.item_type.startsWith('SKIN_')),
    []
  );

  // 行操作弹窗目标（ShopItem 行数据）
  const [iconTarget, setIconTarget] = useState<any | null>(null);
  // 强制 CrudPage 重新挂载以刷新列表（保存图标后 image_url 已变更）
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <>
      <CrudPage
        key={refreshKey}
        title="道具管理"
        idField="id"
        columns={columns}
        fields={fields}
        fetcher={fetcher}
        creator={createShopItem}
        updater={updateShopItem}
        deleter={deleteShopItem}
        searchable
        searchPlaceholder="搜索道具名称/类型"
        deleteNameField="name"
        sortable
        rowActions={(row: any) => (
          <Button size="small" variant="outlined" onClick={() => setIconTarget(row)}>
            图标管理
          </Button>
        )}
      />

      <ItemIconManager
        open={!!iconTarget}
        itemType={iconTarget?.item_type}
        initialUrl={iconTarget?.image_url}
        onClose={() => setIconTarget(null)}
        onSaved={() => setRefreshKey((k) => k + 1)}
      />
    </>
  );
}
