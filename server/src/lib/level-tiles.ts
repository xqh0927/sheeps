import { Env, TileData, LevelTileRow } from '../types';

/**
 * 关卡 tile 读写 helper。
 *
 * 设计：原 levels.layout_data（JSON 字符串）拆分为 level_tiles 子表。
 * 读取时按 level_id 聚合、重组为 TileData[]，再 JSON.stringify 回填 layout_data 字符串契约
 * （前端 Levels.tsx 行为不变）。
 */

/**
 * 写入某关卡的 tile 集合（先删后插，对单关卡幂等）。
 *
 * @param env D1 实例
 * @param levelId 关卡 ID
 * @param tiles 关卡方块数组（按数组顺序，tile_index 从 0 起）
 */
export async function writeLevelTiles(
  env: Env,
  levelId: number,
  tiles: TileData[]
): Promise<void> {
  // 先删后插：保证重复写入不产生重复行
  await env.DB.prepare('DELETE FROM level_tiles WHERE level_id = ?').bind(levelId).run();
  if (!tiles || tiles.length === 0) return;

  const statements = tiles.map((tile, index) =>
    env.DB.prepare(
      `INSERT INTO level_tiles
        (level_id, tile_index, tile_id, x, y, z, "type", is_blind, sealed_count, seal_unlock_threshold)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      levelId,
      index,
      tile.id,
      tile.x,
      tile.y,
      tile.z,
      tile.type,
      tile.isBlind ? 1 : 0,
      tile.sealedCount,
      tile.sealUnlockThreshold ?? null
    )
  );
  await env.DB.batch(statements);
}

/**
 * 批量读取多关卡 tile，按 level_id 聚合。
 *
 * @param env D1 实例
 * @param levelIds 关卡 ID 列表
 * @returns Map<level_id, TileData[]>（每个关卡内按 tile_index 升序）
 */
export async function readLevelTilesByLevels(
  env: Env,
  levelIds: number[]
): Promise<Map<number, TileData[]>> {
  const result = new Map<number, TileData[]>();
  if (!levelIds.length) return result;

  const placeholders = levelIds.map(() => '?').join(',');
  const rows = await env.DB.prepare(
    `SELECT * FROM level_tiles WHERE level_id IN (${placeholders}) ORDER BY level_id, tile_index ASC`
  )
    .bind(...levelIds)
    .all();

  for (const r of rows.results as unknown as LevelTileRow[]) {
    const tile: TileData = {
      id: r.tile_id,
      x: r.x,
      y: r.y,
      z: r.z,
      type: r.type,
      isBlind: r.is_blind === 1,
      sealedCount: r.sealed_count,
      sealUnlockThreshold: r.seal_unlock_threshold ?? undefined,
    };
    if (!result.has(r.level_id)) result.set(r.level_id, []);
    result.get(r.level_id)!.push(tile);
  }
  return result;
}

/**
 * 将 TileData[] 重组为原 layout_data JSON 字符串（前端契约）。
 */
export function assembleLayoutData(tiles: TileData[]): string {
  return JSON.stringify(tiles ?? []);
}

/**
 * 解析 layout_data（JSON 字符串或已解析对象）为 TileData[]。
 * 解析失败抛出错误，由调用方决定返回 400。
 */
export function parseLayoutData(raw: unknown): TileData[] {
  if (typeof raw === 'string') return JSON.parse(raw) as TileData[];
  if (Array.isArray(raw)) return raw as TileData[];
  throw new Error('layout_data 格式非法');
}
