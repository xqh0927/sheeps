import { listShopItems, PageResult } from '../api/admin';

/**
 * 通用「全量拉取 + 前缀过滤 + 切片」wrapper。
 *
 * 由于 listShopItems 是服务端分页接口，而管理后台希望按 item_type 前缀
 * （SKIN_* / 非 SKIN_*）拆成两个独立页面展示，这里统一先全量拉取（单页上限 1000），
 * 再在前端按 keep 谓词过滤并自行切片，对 CrudPage 暴露一个 1-based 的本地分页 fetcher。
 *
 * @param keep 行级保留谓词：返回 true 表示该行属于当前分区。
 * @returns (page, pageSize, keyword?) => Promise<PageResult<any>>
 *          page 为 1-based（CrudPage 内部从 0 起，调用时传 page+1）。
 */
export function makeShopItemsFetcher(
  keep: (row: any) => boolean
): (page: number, pageSize: number, keyword?: string) => Promise<PageResult<any>> {
  return async (page: number, pageSize: number, keyword?: string) => {
    const resp = await listShopItems(1, 1000, keyword ?? '');
    const all = resp.list ?? [];
    const filtered = all.filter(keep);
    const start = (page - 1) * pageSize; // page 是 1-based
    const slice = filtered.slice(start, start + pageSize);
    return { ...resp, list: slice, total: filtered.length };
  };
}
