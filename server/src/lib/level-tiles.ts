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
export function replaceLevelTileStatements(
  env: Env,
  levelId: number,
  tiles: TileData[]
): D1PreparedStatement[] {
  const remove = env.DB.prepare('DELETE FROM level_tiles WHERE level_id = ?').bind(levelId);
  // One INSERT handles a full board without hundreds of statements in a D1 batch.
  const insert = env.DB.prepare(`
    INSERT INTO level_tiles (level_id, tile_index, tile_id, x, y, z, "type", is_blind, sealed_count, seal_unlock_threshold)
    SELECT ?, j.key, j.value ->> '$.id', j.value ->> '$.x', j.value ->> '$.y', j.value ->> '$.z',
           j.value ->> '$.type', COALESCE(j.value ->> '$.isBlind', 0), COALESCE(j.value ->> '$.sealedCount', 0),
           j.value ->> '$.sealUnlockThreshold'
    FROM json_each(?) AS j
  `).bind(levelId, JSON.stringify(tiles));
  return [remove, insert];
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
  const [legacy, normalized] = await env.DB.batch([
    // SELECT * also works on new databases where the legacy layout_data column is absent.
    env.DB.prepare(`SELECT l.* FROM levels AS l WHERE l.level_id IN (${placeholders})
      AND NOT EXISTS (SELECT 1 FROM level_tiles t WHERE t.level_id = l.level_id)`).bind(...levelIds),
    env.DB.prepare(`SELECT * FROM level_tiles WHERE level_id IN (${placeholders})
      ORDER BY level_id, tile_index`).bind(...levelIds),
  ]);
  for (const row of legacy.results as { level_id: number; layout_data?: string }[]) {
    if (!row.layout_data) continue;
    try {
      result.set(row.level_id, parseLayoutData(row.layout_data));
    } catch {
      // An invalid archived layout must not prevent the player from entering a generated level.
    }
  }
  for (const r of normalized.results as unknown as LevelTileRow[]) {
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
  const values: unknown = typeof raw === 'string' ? JSON.parse(raw) : raw;
  if (!Array.isArray(values)) throw new Error('layout_data 应为数组');
  const ids = new Set<string>();
  return values.map(tile => {
    if (!tile || typeof tile.id !== 'string' || !tile.id || ids.has(tile.id) ||
        !Number.isFinite(tile.x) || !Number.isFinite(tile.y) || !Number.isInteger(tile.z) ||
        !Number.isInteger(tile.type) || (tile.isBlind != null && typeof tile.isBlind !== 'boolean') ||
        (tile.sealedCount != null && !Number.isInteger(tile.sealedCount)) ||
        (tile.sealUnlockThreshold != null && !Number.isInteger(tile.sealUnlockThreshold))) {
      throw new Error('布局包含无效卡牌');
    }
    ids.add(tile.id);
    return { id: tile.id, x: tile.x, y: tile.y, z: tile.z, type: tile.type,
      isBlind: tile.isBlind ?? false, sealedCount: tile.sealedCount ?? 0,
      sealUnlockThreshold: tile.sealUnlockThreshold ?? undefined };
  });
}

// Mirrors Android GameEngine's 46-unit tiles and single-precision arithmetic.
// Rounding matters for layouts at the 1% coverage boundary.
const TILE_SIZE = 46;
const BLOCKING_AREA = Math.fround(TILE_SIZE * TILE_SIZE * Math.fround(0.01));

function overlapLength(offset: number): number {
  return Math.max(0, Math.fround(TILE_SIZE - Math.fround(Math.abs(offset) * TILE_SIZE)));
}

function canTouchAtOpening(tiles: TileData[], index: number): boolean {
  const tile = tiles[index];
  if (tile.sealedCount > 0) return false;
  let covered = 0;
  const interceptors: { x: number; y: number }[] = [];
  const stripTops = new Set([0]);
  for (let i = 0; i < tiles.length; i++) {
    const other = tiles[i];
    // The View hit-tests higher layers first, then uses original array order for ties.
    if (other.z < tile.z || (other.z === tile.z && i >= index)) continue;
    const x = Math.fround(Math.fround(other.x) - Math.fround(tile.x));
    const y = Math.fround(Math.fround(other.y) - Math.fround(tile.y));
    if (Math.abs(x) >= 1 || Math.abs(y) >= 1) continue;
    if (other.z > tile.z) {
      const area = overlapLength(x) * overlapLength(y);
      covered = Math.fround(covered + Math.fround(area));
      if (covered > BLOCKING_AREA) return false;
    }
    if (x === 0 && y === 0) return false;
    interceptors.push({ x, y });
    const edge = y > 0 ? y : y + 1;
    if (edge > 0 && edge < 1) stripTops.add(edge);
  }

  // RectF includes top/left and excludes bottom/right. Within each horizontal
  // strip every equal-sized interceptor covers from the left or the right edge.
  for (const top of stripTops) {
    let left = 0;
    let right = 1;
    for (const other of interceptors) {
      if (top < other.y || top >= other.y + 1) continue;
      if (other.x <= 0) left = Math.max(left, other.x + 1);
      else right = Math.min(right, other.x);
      if (left >= right) break;
    }
    if (left < right) return true;
  }
  return false;
}

/** New custom layouts must support triples and an opening click. An empty layout uses the generator. */
export function levelPublicationError(tiles: TileData[]): string | null {
  if (tiles.length === 0) return null;
  const counts = new Map<number, number>();
  for (const tile of tiles) {
    for (const field of ['z', 'type', 'sealedCount'] as const) {
      if (tile[field] < -2147483648 || tile[field] > 2147483647) {
        return `卡牌 ${tile.id} 的 ${field} 超出 Android 整数范围`;
      }
    }
    if (!Number.isFinite(Math.fround(tile.x)) || !Number.isFinite(Math.fround(tile.y))) {
      return `卡牌 ${tile.id} 的坐标超出 Android 浮点数范围`;
    }
    counts.set(tile.type, (counts.get(tile.type) ?? 0) + 1);
  }
  for (const [type, count] of counts) {
    if (count % 3 !== 0) return `花色 ${type} 有 ${count} 张牌，数量必须是 3 的倍数`;
  }
  const hasOpening = tiles.some((_, index) => canTouchAtOpening(tiles, index));
  return hasOpening ? null : '至少需要一张未封印且可点击的起手牌';
}
