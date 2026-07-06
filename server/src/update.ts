import { Env, GitHubRelease, GitHubReleaseAsset, AppUpdatePayload, ApkStatus } from './types';

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
 * 当 GitHub API 不可用时，从 D1 数据库中查找并校验更新
 */
export async function getDatabaseAppUpdate(env: Env, currentCode: number, lang: string): Promise<AppUpdatePayload> {
  const logCol = lang ? `COALESCE(update_log_${lang}, update_log)` : 'update_log';
  const versions = await env.DB.prepare(
    `SELECT version_code, version_name, apk_url, ${logCol} as update_log, is_force_update FROM app_version WHERE version_code > ? ORDER BY version_code DESC LIMIT 5`
  ).bind(currentCode).all<any>();

  if (versions.results && versions.results.length > 0) {
    for (const release of versions.results) {
      if (release.apk_url) {
        const exists = await checkApkExists(release.apk_url); // 确保链接有效
        if (exists) {
          return {
            has_update: true,
            version_name: release.version_name,
            apk_url: release.apk_url,
            update_log: release.update_log,
            force_update: release.is_force_update === 1
          };
        }
      }
    }
  }
  return { has_update: false };
}

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

export function findApkAsset(assets?: GitHubReleaseAsset[]): GitHubReleaseAsset | null {
  return assets?.find(asset =>
    Boolean(asset.browser_download_url) && Boolean(asset.name?.toLowerCase().endsWith('.apk'))
  ) ?? null;
}

export function isForceUpdateRelease(body?: string): boolean {
  if (!body) return false;
  return /\[(force|force_update|强制更新|強制更新)\]|force_update\s*[:=]\s*true/i.test(body);
}

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

export function clearApkCache() {
  apkExistenceCache.clear();
}