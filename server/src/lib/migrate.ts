import { Env } from '../types';
import { writeAuditChanges } from './audit';

/**
 * 存量库回填 helper（由 index.ts 的 migrateSchema 调用）。
 *
 * 设计原则：
 *  - 全部幂等：子表「该父键无记录才插入」或「先判定再插」，可重复执行。
 *  - 失败不阻断：每个回填独立 try/catch，单表失败不影响其余迁移。
 */

/**
 * 关卡 tiles 回填：从 levels.layout_data 旧列解析写入 level_tiles。
 * 已存在 tile 的关卡跳过（NOT EXISTS 子查询幂等）。
 */
export async function backfillLevels(env: Env): Promise<void> {
  try {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO level_tiles
        (level_id, tile_index, tile_id, x, y, z, "type", is_blind, sealed_count, seal_unlock_threshold)
       SELECT l.level_id,
              CAST(j.key AS INTEGER),
              json_extract(j.value, '$.id'),
              json_extract(j.value, '$.x'),
              json_extract(j.value, '$.y'),
              json_extract(j.value, '$.z'),
              json_extract(j.value, '$.type'),
              CASE WHEN json_extract(j.value, '$.isBlind') THEN 1 ELSE 0 END,
              json_extract(j.value, '$.sealedCount'),
              json_extract(j.value, '$.sealUnlockThreshold')
       FROM levels l, json_each(l.layout_data) j`
    ).run();
  } catch (e) {
    console.error('backfillLevels failed:', e);
  }
}

/**
 * 备份 save_data 回填：
 *  - unlocked_levels[] → backup_unlocked_levels
 *  - items[] → backup_save_items
 *  - points 标量 → backup_save_log.points（仅当为 NULL）
 * 均按 NOT EXISTS 幂等。
 */
export async function backfillBackups(env: Env): Promise<void> {
  try {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO backup_unlocked_levels (backup_id, level_id)
       SELECT b.id, CAST(json_extract(j.value, '$') AS INTEGER)
       FROM backup_save_log b, json_each(json_extract(b.save_data, '$.unlocked_levels')) j`
    ).run();
  } catch (e) {
    console.error('backfillBackups(unlocked_levels) failed:', e);
  }
  try {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO backup_save_items (backup_id, item_type, count)
       SELECT b.id, json_extract(j.value, '$.item_type'), json_extract(j.value, '$.count')
       FROM backup_save_log b, json_each(json_extract(b.save_data, '$.items')) j`
    ).run();
  } catch (e) {
    console.error('backfillBackups(items) failed:', e);
  }
  try {
    await env.DB.prepare(
      `UPDATE backup_save_log SET points = CAST(json_extract(save_data, '$.points') AS INTEGER)
       WHERE points IS NULL`
    ).run();
  } catch (e) {
    console.error('backfillBackups(points) failed:', e);
  }
}

/**
 * 审计快照回填：解析 admin_audit_log 旧 before/after_snapshot，
 * 复用 writeAuditChanges 写入 admin_audit_changes（保证编码一致）。
 * 仅回填「有快照但尚无 KV 子表记录」的行（幂等）。
 */
export async function backfillAudit(env: Env): Promise<void> {
  try {
    const rows = await env.DB.prepare(
      `SELECT id, before_snapshot, after_snapshot FROM admin_audit_log
       WHERE (before_snapshot IS NOT NULL OR after_snapshot IS NOT NULL)
         AND NOT EXISTS (SELECT 1 FROM admin_audit_changes WHERE change_id = admin_audit_log.id)`
    ).all();
    for (const row of rows.results as any[]) {
      let before: any;
      let after: any;
      if (row.before_snapshot != null) {
        try {
          before = JSON.parse(row.before_snapshot);
        } catch {
          before = row.before_snapshot;
        }
      }
      if (row.after_snapshot != null) {
        try {
          after = JSON.parse(row.after_snapshot);
        } catch {
          after = row.after_snapshot;
        }
      }
      await writeAuditChanges(env, row.id, before, after);
    }
  } catch (e) {
    console.error('backfillAudit failed:', e);
  }
}
