import { listShopItems, PageResult } from '../api/admin';

/**
 * 通用「全量拉取 + 前缀过滤 + 切片」wrapper。
 *
 * 管理后台希望按 item_type 前缀（SKIN_* / 非 SKIN_*）把 shop_items 拆成两个独立页面展示，
 * 但服务端 genericList 对 pageSize 有硬上限（PAGE_SIZE_MAX=100），单页请求拿不到全量。
 * 因此这里改为「循环分页拉取所有页 → 前端按 keep 谓词过滤 → 自行切片」，
 * 对 CrudPage 暴露一个 1-based 的本地分页 fetcher。
 *
 * 这样无论后端 pageSize 上限是多少、表数据是否膨胀，都不会因单页截断而丢数据。
 *
 * @param keep 行级保留谓词：返回 true 表示该行属于当前分区。
 * @returns (page, pageSize, keyword?) => Promise<PageResult<any>>
 *          page 为 1-based（CrudPage 内部从 0 起，调用时传 page+1）。
 */
export function makeShopItemsFetcher(
  keep: (row: any) => boolean
): (page: number, pageSize: number, keyword?: string) => Promise<PageResult<any>> {
  return async (page: number, pageSize: number, keyword?: string) => {
    // 后端 pageSize 硬上限为 100，这里按该上限循环拉取直到累计达到服务端 total
    const FETCH_PAGE_SIZE = 100;
    let all: any[] = [];
    let serverTotal = 0;
    let p = 1;
    // 防御性上限：避免异常情况下死循环（100 页 × 100 = 10000 条已远超商品目录量级）
    while (p <= 100) {
      const resp = await listShopItems(p, FETCH_PAGE_SIZE, keyword ?? '');
      serverTotal = resp.total ?? 0;
      const batch = resp.list ?? [];
      all = all.concat(batch);
      // 拉满全量，或遇到空页（异常/越界）即停止
      if (all.length >= serverTotal || batch.length === 0) break;
      p++;
    }
    // 全量拉取完成后，再按分区谓词 client 端过滤；keyword 已随每次请求下发，故这里不再二次匹配。
    const filtered = all.filter(keep);
    const start = (page - 1) * pageSize; // page 是 1-based
    const slice = filtered.slice(start, start + pageSize);
    return { success: true, list: slice, total: filtered.length, page, pageSize };
  };
}
