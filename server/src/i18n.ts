import { Env } from './types';

/**
 * 方案 B · 多语言归一化核心模块
 *
 * 将原本散落在各业务表「宽列」(name/_en/_tw/_ja/_ko …) 的多语言值，统一收敛到
 * 单表 `i18n_strings`（str_key = {module}.{entity_ref}.{field}）。
 *
 * 设计要点：
 *  - 读路径（shop / task / notice / update）优先从 i18n_strings 取值；
 *  - 命中缺失时自动回退「基列 / 宽列」原值（resolveI18n 的 fallback），保证零空白；
 *  - 返回给客户端的 JSON 字段名完全不变（name/description/title/content/update_log），
 *    Android 客户端零改动；
 *  - 结果按 `i18n_${module}_${locale}` 缓存到 KV（TTL 300s）；
 *  - ensureI18nSeeded 作为 ETL 二次兜底（migrateSchema 首次部署自动调用）。
 */

/** 系统支持的全部 locale（固定顺序：zh → en → tw → ja → ko） */
export type LocaleCode = 'zh' | 'en' | 'tw' | 'ja' | 'ko';

/** 系统支持的全部 locale（数组形式，用于遍历） */
export const I18N_LOCALES: LocaleCode[] = ['zh', 'en', 'tw', 'ja', 'ko'];

/**
 * locale → 请求体字段后缀映射（单一来源，前后端必须保持一致）。
 * zh 为默认语言，无后缀；其余语言追加对应后缀。
 */
export const LOCALE_SUFFIX: Record<LocaleCode, string> = {
  zh: '',
  en: '_en',
  tw: '_tw',
  ja: '_ja',
  ko: '_ko',
};

/** 多语言字段值：按语言给出的映射（缺失语言视为空占位） */
export type LocaleValues = Partial<Record<LocaleCode, string | null>>;

/**
 * 字段值（用于 seedI18nForEntity 的入参）：
 *  - 传 `string`      → 仅 zh（旧调用 / 向后兼容），其余语言空占位；
 *  - 传 `LocaleValues` → 按语言给值；
 *  - `null` / `undefined` → 整体当作空（zh=''），其余空占位。
 */
export type I18nFieldValue = string | LocaleValues | null | undefined;

/** 单语言结果的 key→value 映射 */
export type I18nMap = Map<string, string>;

/** i18n KV 缓存 TTL（秒） */
const I18N_KV_TTL = 300;

/** KV 缓存键 */
function i18nCacheKey(module: string, locale: string): string {
  return `i18n_${module}_${locale}`;
}

/**
 * 批量读取某模块某语言的 i18n 值，返回 str_key -> value 的 Map。
 * - 默认语言（zh，或空后缀）返回空 Map，由调用方回退宽列；
 * - 结果缓存到 KV（TTL 300s），降低 D1 读压力。
 */
export async function getI18nBatch(env: Env, module: string, locale: string): Promise<I18nMap> {
  const map: I18nMap = new Map();
  // zh（默认语言）不查 i18n 表，调用方直接用基列值即可
  if (!locale || locale === 'zh') return map;

  const cacheKey = i18nCacheKey(module, locale);
  try {
    const cached = await env.SHEEPS_CACHE.get(cacheKey);
    if (cached) {
      const parsed = JSON.parse(cached) as Record<string, string>;
      for (const k of Object.keys(parsed)) map.set(k, parsed[k]);
      return map;
    }
  } catch (_) {
    /* 缓存解析失败忽略，回源查库 */
  }

  const rows = await env.DB.prepare(
    'SELECT str_key, value FROM i18n_strings WHERE module = ? AND locale = ?'
  )
    .bind(module, locale)
    .all<{ str_key: string; value: string }>();

  for (const r of rows.results) {
    if (r.value !== null && r.value !== undefined) map.set(r.str_key, r.value);
  }

  // 异步回填 KV（不阻塞主流程）
  try {
    await env.SHEEPS_CACHE.put(cacheKey, JSON.stringify(Object.fromEntries(map)), { expirationTtl: I18N_KV_TTL });
  } catch (_) {
    /* KV 回写失败不阻塞 */
  }

  return map;
}

/**
 * 单行读取：优先 i18n_strings，否则回退 fallback（基列宽列值）。
 */
export async function getI18n(
  env: Env,
  module: string,
  strKey: string,
  locale: string,
  fallback: string | null
): Promise<string | null> {
  if (!locale || locale === 'zh') return fallback;
  const map = await getI18nBatch(env, module, locale);
  const v = map.get(strKey);
  return v !== undefined ? v : fallback;
}

/**
 * 命中取 i18n 值，否则回退 fallback（基列/宽列值）。
 * 用于已持有 batch Map 的逐条解析场景（shop / task / notice 列表）。
 */
export function resolveI18n(
  map: I18nMap,
  strKey: string,
  fallback: string | null | undefined
): string | null | undefined {
  const v = map.get(strKey);
  if (v !== undefined && v !== null && v !== '') return v;
  return fallback;
}

// ============ ETL 兜底（ensureI18nSeeded）============

/** 模块级缓存：避免每次冷启动重复扫描 */
let i18nSeeded = false;

/**
 * 幂等 ETL 兜底（no-op）：宽列已 DROP，不再执行 ETL 播种。
 * 保留函数签名与模块级标志，保持 migrateSchema 调用兼容。
 */
export async function ensureI18nSeeded(env: Env): Promise<void> {
  if (i18nSeeded) return;
  i18nSeeded = true;
}

// ============ i18n 自动播种（创建实体时自动补全多语言行）============

/**
 * 为指定实体的字段在所有 5 种语言下自动创建 i18n_strings 行（幂等）。
 *
 * - module: 业务模块名（notice / task / shop_items / app_version）
 * - entityId: 实体主键（notice.id / task.id / shop_items.id / app_version.version_code）
 * - fields: 字段名 → 值的映射。
 *     · 传 `string`      → 仅落 zh（旧调用 / 向后兼容），en/tw/ja/ko 空占位；
 *     · 传 `LocaleValues` → 按语言给值，缺失语言 → 空占位（绝不报错/拒绝）；
 *     · `null` / `undefined` → 整体当作空（zh=''），其余空占位。
 *
 * 使用 INSERT OR IGNORE 保证重复调用幂等（已存在的 str_key+locale 组合不会覆盖）。
 *
 * 向后兼容说明：旧调用方（如 Android）只发 zh 一个基列值，
 * 此处仍只落 zh，其余语言空占位，行为与改造前完全一致，不会拒绝请求。
 */
export async function seedI18nForEntity(
  env: Env,
  module: string,
  entityId: string | number,
  fields: Record<string, I18nFieldValue>
): Promise<void> {
  const now = Date.now();
  const statements: D1PreparedStatement[] = [];

  for (const [field, raw] of Object.entries(fields)) {
    // 旧调用传 string（或 null/undefined）→ 仅 zh；新调用传 LocaleValues → 按语言
    const values: LocaleValues =
      typeof raw === 'string' || raw == null ? { zh: raw ?? '' } : raw;

    const strKey = `${module}.${entityId}.${field}`;
    for (const locale of I18N_LOCALES) {
      // 缺失语言 → 空占位（不报错）
      const value = values[locale] ?? '';
      statements.push(
        env.DB.prepare(
          `INSERT OR IGNORE INTO i18n_strings (str_key, locale, module, value, updated_at) VALUES (?, ?, ?, ?, ?)`
        ).bind(strKey, locale, module, value, now)
      );
    }
  }

  if (statements.length > 0) {
    try {
      await env.DB.batch(statements);
    } catch (e) {
      console.error(`seedI18nForEntity failed for ${module}.${entityId}:`, e);
    }
  }
}

/**
 * 从请求体按后缀提取某基础字段的 5 语言值。
 * - zh 读 `base`（无后缀）；
 * - 其它语言读 `base_en` / `base_tw` / `base_ja` / `base_ko`（与 LOCALE_SUFFIX 一致）。
 * 缺失 / undefined / null 的键当作空字符串，保证返回的 LocaleValues 5 语言齐全。
 */
export function localeValuesFromBody(body: Record<string, any>, base: string): LocaleValues {
  const out: LocaleValues = {};
  if (!body) return out;
  for (const loc of I18N_LOCALES) {
    const key = loc === 'zh' ? base : `${base}${LOCALE_SUFFIX[loc]}`;
    const v = body[key];
    out[loc] = v == null ? '' : String(v);
  }
  return out;
}

/**
 * 业务表 INSERT/UPDATE 前剔除「仅用于 i18n 传输的 locale 后缀键」，
 * 避免引用已 DROP 的不存在列（宽列归一化改造中已删除 *_en/_tw/_ja/_ko 列）。
 *
 * 注意：仅剔除以 `_en/_tw/_ja/_ko` 结尾的键；zh 基列（如 name / title）及
 * 其它业务列（如 item_type / points_price / version_code）原样保留。
 */
export function stripLocaleSuffixedKeys(body: Record<string, any>): Record<string, any> {
  const out: Record<string, any> = {};
  if (!body) return out;
  for (const [k, v] of Object.entries(body)) {
    const isSuffixed = I18N_LOCALES.some(
      (l) => l !== 'zh' && k.endsWith(LOCALE_SUFFIX[l])
    );
    if (!isSuffixed) out[k] = v;
  }
  return out;
}
