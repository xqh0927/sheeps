/**
 * QA 契约校验（Task C 可选项）：用真实 locales.ts 常量复刻 CrudPage.handleSubmit
 * 的多语言缺失判定谓词（src/components/CrudPage.tsx L221-225），验证
 * 「新建态 5 语言任一为空即拦截」以及前后端后缀键一致。
 * 运行：node --experimental-strip-types qa_multilingual_validation.mts
 */
import { I18N_LOCALES, LOCALE_SUFFIX } from './src/constants/locales.ts';

/** 复刻 CrudPage.handleSubmit 的缺失语言判定（L221-225） */
function getMissingLocales(form: Record<string, any>, base: string): string[] {
  return I18N_LOCALES.filter((loc) => {
    const key = loc === 'zh' ? base : `${base}${LOCALE_SUFFIX[loc]}`;
    const v = form[key];
    return v === undefined || v === null || String(v).trim() === '';
  });
}

let failed = 0;
function check(name: string, cond: boolean) {
  if (!cond) { failed++; console.error('  FAIL:', name); }
  else console.log('  PASS:', name);
}

console.log('[multilingual create-state validation predicate]');
// 1. 5 语言全非空 → 通过（missing 为空，handleSubmit 不会 return 拦截）
check('all 5 filled -> no missing (would pass)', getMissingLocales({
  name: '苹果', name_en: 'Apple', name_tw: '蘋果', name_ja: 'リンゴ', name_ko: '사과',
}, 'name').length === 0);

// 2. 缺 en → 拦截（missing 含 en）
check('missing en -> blocked', getMissingLocales({
  name: '苹果', name_tw: '蘋果', name_ja: 'リンゴ', name_ko: '사과',
}, 'name').join(',') === 'en');

// 3. 缺 zh(基列) → 拦截（missing 含 zh）
check('missing zh -> blocked', getMissingLocales({
  name_en: 'Apple', name_tw: '蘋果', name_ja: 'リンゴ', name_ko: '사과',
}, 'name').join(',') === 'zh');

// 4. 纯空格视为空 → 拦截（String.trim()）
check('whitespace-only en -> blocked', getMissingLocales({
  name: '苹果', name_en: '   ', name_tw: '蘋果', name_ja: 'リンゴ', name_ko: '사과',
}, 'name').join(',') === 'en');

// 5. 前后端契约：后缀键组合与后端 LOCALE_SUFFIX 完全一致
check('suffix keys == name,name_en,name_tw,name_ja,name_ko',
  I18N_LOCALES.map((l) => (l === 'zh' ? 'name' : `name${LOCALE_SUFFIX[l]}`)).join(',') ===
  'name,name_en,name_tw,name_ja,name_ko');

console.log(failed === 0 ? '\nRESULT: ALL PASS' : `\nRESULT: ${failed} FAILED`);
process.exit(failed === 0 ? 0 : 1);
