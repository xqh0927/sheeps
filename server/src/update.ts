/**
 * update.ts — App 版本更新检测模块
 *
 * 业务上下文：客户端启动时携带 version_code 调用 `/api/app/check-update`
 * （路由见 handlers/system.ts），服务端据此判定是否需要升级并下发 APK 直链与更新日志。
 *
 * 数据源：
 *  - 优先 GitHub Release（mapGitHubReleaseToUpdate 等纯函数负责映射，由调用方组合）；
 *  - 兜底「方案 B」：GitHub 不可用时回退 D1 `app_version` 表（getDatabaseAppUpdate），并经 i18n 本地化。
 *
 * 性能：GitHub Release 响应与 APK 存在性结果均做内存/KV 缓存，降低外网请求频率与限流风险。
 *
 * @module update
 */
import { Env, GitHubRelease, GitHubReleaseAsset, AppUpdatePayload, ApkStatus } from './types';
import { getI18nBatch, resolveI18n } from './i18n';

// Github 接口及缓存策略
const GITHUB_RELEASES_API = 'https://api.github.com/repos/xqh0927/sheeps-releases/releases/latest';
const GITHUB_RELEASE_CACHE_TTL_MS = 5 * 60 * 1000; // API 响应缓存 5 分钟

// 内存缓存对象，减少对 Github 的频繁网络请求 (防限流)
let githubReleaseCache: { expiresAt: number; release: GitHubRelease | null } | null = null;
export const apkExistenceCache = new Map<string, ApkStatus>();

/** 验证 APK 下载链接是否存活 (使用 HEAD 请求优化性能) */
export async function checkApkExists(url: string): Promise<boolean> {
  const now = Date.now();
  const cached = apkExistenceCache.get(url);
  // 命中缓存则直接返回
  if (cached) {
    const ttl = cached.exists ? 24 * 60 * 60 * 1000 : 60 * 1000;
    if (now - cached.checkedAt < ttl) return cached.exists;
  }

  try {
    // 仅请求 Header，不下载文件主体
    const response = await fetch(url, { method: 'HEAD', headers: { 'User-Agent': 'sheeps-update-checker' } });
    const exists = response.status === 200;
    apkExistenceCache.set(url, { exists, checkedAt: now });
    return exists;
  } catch {
    apkExistenceCache.set(url, { exists: false, checkedAt: now });
    return false;
  }
}

/** * 数据库兜底更新检查逻辑 
 * 当 GitHub API 不可用时，从 D1 数据库中查找并校验更新。
 *
 * 方案 B 改造：
 *  - 下载链接优先 download_url，回退 apk_url（COALESCE(download_url, apk_url)）；
 *  - 仅 status=1（已发布）的版本参与更新判定；
 *  - update_log 经 i18n 读取（缺值回退基列宽列）。
 */
export async function getDatabaseAppUpdate(env: Env, currentCode: number, lang: string): Promise<AppUpdatePayload> {
  const i18nMap = await getI18nBatch(env, 'app_version', lang);
  const versions = await env.DB.prepare(
    `SELECT version_code, version_name, apk_url, download_url, update_log, status, is_force_update
     FROM app_version
     WHERE version_code > ? AND status = 1
     ORDER BY version_code DESC LIMIT 5`
  ).bind(currentCode).all<any>();

  if (versions.results && versions.results.length > 0) {
    for (const release of versions.results) {
      const downloadUrl = release.download_url || release.apk_url;
      if (downloadUrl) {
        const exists = await checkApkExists(downloadUrl); // 确保链接有效
        if (exists) {
          const updateLog = resolveI18n(i18nMap, `app_version.${release.version_code}.update_log`, release.update_log);
          return {
            has_update: true,
            version_name: release.version_name,
            apk_url: downloadUrl,
            update_log: updateLog ?? '',
            force_update: release.is_force_update === 1
          };
        }
      }
    }
  }
  return { has_update: false };
}

/**
 * 将 GitHub Release 标签（如 `v1.2.3`）解析为数值版本号。
 *
 * 编码规则：取 major/minor/patch 三段，映射为 `major*10000 + minor*100 + patch`，
 * 以便与客户端 version_code 直接做大小比较。
 *
 * @param tagName Release 标签名（可带或不带 `v` 前缀）
 * @returns 数值版本号；格式非法或缺失时返回 null
 */
export function parseReleaseVersionCode(tagName?: string): number | null {
  if (!tagName) return null;

  const semanticMatch = tagName.match(/v?(\d+)(?:\.(\d+))?(?:\.(\d+))?/i);
  if (!semanticMatch) return null;

  const major = Number.parseInt(semanticMatch[1], 10);
  const minor = semanticMatch[2] ? Number.parseInt(semanticMatch[2], 10) : 0;
  const patch = semanticMatch[3] ? Number.parseInt(semanticMatch[3], 10) : 0;

  if (!Number.isFinite(major) || !Number.isFinite(minor) || !Number.isFinite(patch)) {
    return null;
  }

  return major * 10000 + minor * 100 + patch;
}

/**
 * 从 GitHub Release 资源列表中筛选 APK 安装包附件。
 *
 * @param assets Release 的 assets 数组（可能为空或 undefined）
 * @returns 第一个以 `.apk` 结尾且含下载地址的资源；无匹配时返回 null
 */
export function findApkAsset(assets?: GitHubReleaseAsset[]): GitHubReleaseAsset | null {
  return assets?.find(asset =>
    Boolean(asset.browser_download_url) && Boolean(asset.name?.toLowerCase().endsWith('.apk'))
  ) ?? null;
}

/**
 * 依据 Release 说明文本判定是否为「强制更新」。
 *
 * 匹配规则（不区分大小写）：包含 `[force]` / `[force_update]` / `[强制更新]` 标签，
 * 或 `force_update: true` / `force_update = true` 写法。
 *
 * @param body Release 的 body 说明文本
 * @returns 是否强制更新
 */
export function isForceUpdateRelease(body?: string): boolean {
  if (!body) return false;
  return /\[(force|force_update|强制更新|強制更新)\]|force_update\s*[:=]\s*true/i.test(body);
}

/**
 * 将单个 GitHub Release 映射为客户端更新负载。
 *
 * 过滤规则：跳过 draft 版本；versionCode 必须存在且严格大于 currentCode；且必须存在 APK 资源。
 *
 * @param release GitHub Release 对象
 * @param currentCode 客户端当前数值版本号
 * @returns 需要更新时返回 has_update=true 的 AppUpdatePayload；否则 has_update=false
 */
export function mapGitHubReleaseToUpdate(release: GitHubRelease, currentCode: number): AppUpdatePayload | null {
  if (release.draft) return null;

  const versionCode = parseReleaseVersionCode(release.tag_name);
  const apkAsset = findApkAsset(release.assets);
  if (!versionCode || !apkAsset?.browser_download_url || versionCode <= currentCode) {
    return { has_update: false };
  }

  return {
    has_update: true,
    version_name: release.name || release.tag_name,
    apk_url: apkAsset.browser_download_url,
    update_log: release.body || '发现新版本，建议更新以获得更好的游戏体验。',
    force_update: isForceUpdateRelease(release.body)
  };
}

/**
 * 清空 APK 存在性的内存缓存。
 * 当 Release 信息或 APK 直链发生变更时调用，强制下次请求重新探测链接存活状态。
 */
export function clearApkCache() {
  apkExistenceCache.clear();
}