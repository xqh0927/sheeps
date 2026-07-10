import { useState, useMemo } from 'react';
import CrudPage, { FieldDef, ColumnDef } from '../components/CrudPage';
import { Button, Chip } from '@mui/material';
import {
  listShopItems,
  createShopItem,
  updateShopItem,
  deleteShopItem,
  SHOP_GROUPS,
} from '../api/admin';
import SkinTilesManager from '../components/SkinTilesManager';
import { makeShopItemsFetcher } from '../utils/filteredShopItemsFetcher';
import { SKIN_ITEM_TYPES } from '../constants/shopItemTypes';

const GROUP_OPTIONS = SHOP_GROUPS.map((g) => ({ label: g, value: g }));

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
    label: '封面',
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
  {
    key: 'group',
    label: '系列分组',
    render: (r: any) =>
      r.group ? <Chip size="small" color="primary" label={r.group} /> : '-',
  },
];

const fields: FieldDef[] = [
  { name: 'name', label: '名称', required: true, multilingual: true },
  { name: 'description', label: '描述', type: 'textarea', multilingual: true },
  { name: 'item_type', label: '皮肤类型', type: 'select', options: SKIN_ITEM_TYPES, required: true },
  { name: 'points_price', label: '积分价格', type: 'number', required: true },
  { name: 'stock', label: '库存', type: 'number', nullable: true },
  { name: 'image_url', label: '封面（由卡面管理维护）', type: 'image' },
  { name: 'group', label: '系列分组', type: 'select', options: GROUP_OPTIONS, nullable: true },
];

export default function SkinProducts() {
  // fetcher 必须用 useMemo 固定引用，否则 CrudPage 会因依赖变化而无限重拉。
  const fetcher = useMemo(
    () => makeShopItemsFetcher((r) => typeof r.item_type === 'string' && r.item_type.startsWith('SKIN_')),
    []
  );

  // 行操作弹窗目标（ShopItem 行数据）
  const [tilesTarget, setTilesTarget] = useState<any | null>(null);
  // 强制 CrudPage 重新挂载以刷新列表（保存卡面后 image_url / group 已变更）
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <>
      <CrudPage
        key={refreshKey}
        title="卡片皮肤管理"
        idField="id"
        columns={columns}
        fields={fields}
        fetcher={fetcher}
        creator={createShopItem}
        updater={updateShopItem}
        deleter={deleteShopItem}
        searchable
        searchPlaceholder="搜索皮肤名称/类型"
        deleteNameField="name"
        sortable
        rowActions={(row: any) => (
          <Button size="small" variant="outlined" onClick={() => setTilesTarget(row)}>
            卡面管理
          </Button>
        )}
      />

      <SkinTilesManager
        open={!!tilesTarget}
        skinType={tilesTarget ? tilesTarget.item_type.replace('SKIN_', '').toLowerCase() : ''}
        skinItemType={tilesTarget?.item_type}
        initialTiles={tilesTarget?.tiles ?? null}
        onClose={() => setTilesTarget(null)}
        onSaved={() => setRefreshKey((k) => k + 1)}
      />
    </>
  );
}
