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
export async function getDatabaseAppUpdate(env: Env, currentCode: number): Promise<AppUpdatePayload> {
  const versions = await env.DB.prepare(
    'SELECT version_code, version_name, apk_url, update_log, is_force_update FROM app_version WHERE version_code > ? ORDER BY version_code DESC LIMIT 5'
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