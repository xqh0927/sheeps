import { Env, AuditChangeRow } from '../types';

/**
 * 审计快照拆分 helper。
 *
 * 设计：原 admin_audit_log.before_snapshot / after_snapshot 两个 JSON 文本列
 * 拆分为 KV 子表 admin_audit_changes(change_id, field, old_val, new_val)。
 * 每次写审计时，把 before/after 对象按字段拆开，值统一 JSON.stringify 入库；
 * 读取时按 change_id 聚合、JSON.parse 重组回 before/after 对象，再 JSON.stringify
 * 回填字符串契约（前端 AuditLogs.tsx 的 safeParse 行为不变）。
 */

/**
 * 将一次审计的 before/after 对象按字段写入 admin_audit_changes 子表。
 *
 * 约定：
 *  - 值统一 JSON.stringify 入库，保留原始类型（数字/布尔/字符串/嵌套对象）。
 *  - SQL NULL 的 old_val/new_val 表示「该字段在对应快照中不存在」；
 *    字符串 "null" 表示值为 null（二者可区分）。
 *
 * @param env D1 实例
 * @param changeId 主表 admin_audit_log 的 id（已写入，作为外键）
 * @param before 变更前快照对象（可为 undefined / 非对象，此时不写 before 侧字段）
 * @param after 变更后快照对象（同上）
 */
export async function writeAuditChanges(
  env: Env,
  changeId: number,
  before: any,
  after: any
): Promise<void> {
  const beforeObj: Record<string, any> =
    before && typeof before === 'object' ? before : {};
  const afterObj: Record<string, any> =
    after && typeof after === 'object' ? after : {};

  // 取两侧字段并集，保证「字段仅在一侧出现」也能完整记录
  const fields = new Set<string>([
    ...Object.keys(beforeObj),
    ...Object.keys(afterObj),
  ]);
  if (fields.size === 0) return;

  const statements = Array.from(fields).map((field) => {
    // 仅当值为 undefined 时落 SQL NULL（表示「该字段在对应快照中不存在」）；
    // 真实 null 值经 JSON.stringify 变为 "null" 字符串，重组后仍为 null，二者可区分。
    const rawOld = field in beforeObj ? beforeObj[field] : undefined;
    const rawNew = field in afterObj ? afterObj[field] : undefined;
    const oldVal = rawOld === undefined ? null : JSON.stringify(rawOld);
    const newVal = rawNew === undefined ? null : JSON.stringify(rawNew);
    return env.DB.prepare(
      `INSERT INTO admin_audit_changes (change_id, field, old_val, new_val) VALUES (?, ?, ?, ?)`
    ).bind(changeId, field, oldVal, newVal);
  });

  // 多行写入原子提交
  await env.DB.batch(statements);
}

/**
 * 按 change_id 列表聚合 admin_audit_changes，重组为 before/after 快照字符串。
 *
 * @returns Map<change_id, { before: string|null, after: string|null }>
 *   仅包含「存在 KV 子表记录」的 change_id。无子表记录（旧数据或纯 undefined 快照）不在 Map 中，
 *   调用方应回退使用 admin_audit_log 的 deprecated 列值（可能为 JSON 字符串或 null）。
 */
export async function reassembleAuditSnapshots(
  env: Env,
  changeIds: number[]
): Promise<Map<number, { before: string | null; after: string | null }>> {
  const result = new Map<number, { before: string | null; after: string | null }>();
  if (!changeIds.length) return result;

  const placeholders = changeIds.map(() => '?').join(',');
  const rows = await env.DB.prepare(
    `SELECT change_id, field, old_val, new_val FROM admin_audit_changes WHERE change_id IN (${placeholders})`
  )
    .bind(...changeIds)
    .all();

  // 按 change_id 聚合出 before/after 对象
  const groups = new Map<number, { beforeObj: Record<string, any>; afterObj: Record<string, any> }>();
  for (const r of rows.results as unknown as AuditChangeRow[]) {
    if (!groups.has(r.change_id)) {
      groups.set(r.change_id, { beforeObj: {}, afterObj: {} });
    }
    const group = groups.get(r.change_id)!;
    if (r.old_val !== null && r.old_val !== undefined) {
      try {
        group.beforeObj[r.field] = JSON.parse(r.old_val);
      } catch {
        // 兜底：非 JSON 文本原样保留
        group.beforeObj[r.field] = r.old_val;
      }
    }
    if (r.new_val !== null && r.new_val !== undefined) {
      try {
        group.afterObj[r.field] = JSON.parse(r.new_val);
      } catch {
        group.afterObj[r.field] = r.new_val;
      }
    }
  }

  for (const [changeId, group] of groups) {
    result.set(changeId, {
      before: JSON.stringify(group.beforeObj),
      after: JSON.stringify(group.afterObj),
    });
  }
  return result;
}
