// 页面 B 组：卡片皮肤管理（实体 = ShopItem 的子集，item_type 以 SKIN_ 开头）
// 复用 CrudPage，列表由 makeShopItemsFetcher 过滤出皮肤道具
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

// 表格列定义：name/item_type/points_price/stock 可排序；image_url 渲染封面，group 渲染系列分组
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

// 表单字段定义：name/description 为 multilingual → 由 CrudPage 渲染 MultilingualField；
// image_url 为 image 类型（展示用，实际卡面由 SkinTilesManager 维护），group 为系列分组下拉
const fields: FieldDef[] = [
  { name: 'name', label: '名称', required: true, multilingual: true },
  { name: 'description', label: '描述', type: 'textarea', multilingual: true },
  { name: 'item_type', label: '皮肤类型', type: 'select', options: SKIN_ITEM_TYPES, required: true },
  { name: 'points_price', label: '积分价格', type: 'number', required: true },
  { name: 'stock', label: '库存', type: 'number', nullable: true },
  { name: 'image_url', label: '封面（由卡面管理维护）', type: 'image' },
  { name: 'group', label: '系列分组', type: 'select', options: GROUP_OPTIONS, nullable: true },
];

/**
 * 卡片皮肤管理页。
 * 复用 CrudPage 承载皮肤（SKIN_* ShopItem）列表与增删改查；
 * 行内「卡面管理」打开 SkinTilesManager 批量上传 12 张卡面，保存后通过 refreshKey 强制刷新列表。
 */
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
