/**
 * 前端多语言常量（与 server/src/i18n.ts 的后端定义保持同步，独立副本）。
 *
 * 约定（前后端单一来源）：
 *  - 基础字段（zh，默认语言）无后缀：name / description / title / content / update_log；
 *  - 其它语言用后缀：{base}_en / {base}_tw / {base}_ja / {base}_ko；
 *  - 语言顺序固定 ['zh','en','tw','ja','ko']。
 */

/** 支持的全部 locale（固定顺序） */
export type LocaleCode = 'zh' | 'en' | 'tw' | 'ja' | 'ko';

/** 支持的全部 locale（数组形式，用于遍历渲染） */
export const I18N_LOCALES: LocaleCode[] = ['zh', 'en', 'tw', 'ja', 'ko'];

/**
 * locale → 请求体字段后缀映射（与后端 LOCALE_SUFFIX 完全一致）。
 * zh 为默认语言，无后缀。
 */
export const LOCALE_SUFFIX: Record<LocaleCode, string> = {
  zh: '',
  en: '_en',
  tw: '_tw',
  ja: '_ja',
  ko: '_ko',
};

/** 各 locale 的 UI 展示标签 */
export const LOCALE_LABELS: Record<LocaleCode, string> = {
  zh: '中文',
  en: 'English',
  tw: '繁體',
  ja: '日本語',
  ko: '한국어',
};
