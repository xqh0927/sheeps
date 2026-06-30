# 延迟更新提示直至构建完成设计方案 (Delayed Update Notification Design)

本方案旨在解决当新版本发布时，用户由于 GitHub Actions 构建尚未完成而收到更新弹窗，点击下载导致 404 错误的问题。

## 需求背景
1. 目前执行本地发布脚本 `release.js` 会立即修改数据库中的版本记录并推送到 GitHub。
2. 数据库插入是瞬间完成的，而 GitHub Actions 编译打包 APK 需要几分钟。
3. 如果此时用户打开 App，App 会收到 D1 数据库里已存在的最新版本号，弹出更新对话框。
4. 用户点击下载，但 GitHub Release 对应的 APK 资源尚未上传成功，导致 404 下载失败。

## 设计方案
在 Cloudflare Worker 服务器的 `/api/app/check-update` 接口中，针对数据库中比当前客户端版本高的记录，通过轻量级 HTTP `HEAD` 请求检测其 `apk_url` 是否可用（即返回 200 OK）。

### 1. 缓存层设计
在服务器内存中建立 `apkExistenceCache` 缓存，以避免对 GitHub 造成高频访问从而触发速率限制或增加延迟：
- **存在 (200 OK)**：缓存 24 小时（因已发布的 APK 极少会下架/修改）。
- **不存在 (404/网络异常)**：缓存 60 秒（保证处于构建状态 of APK 能在构建完成后 1 分钟内被检测到）。

### 2. 多版本过滤
如果数据库中最新版本未构建完成（返回 404），则向下查找较新的已构建版本。
例如：
- 客户端当前版本：`1`
- 数据库最高版本：`3` (未构建完)
- 数据库次高版本：`2` (已构建完)
- 接口应当过滤版本 `3`，返回版本 `2` 的更新提示。

---

## 模块细节

### 核心检查逻辑
```typescript
interface ApkStatus {
  exists: boolean;
  checkedAt: number;
}

const apkExistenceCache = new Map<string, ApkStatus>();
const CACHE_TTL_SUCCESS = 24 * 60 * 60 * 1000; // 24小时
const CACHE_TTL_FAILURE = 60 * 1000; // 60秒

async function checkApkExists(url: string): Promise<boolean> {
  const now = Date.now();
  const cached = apkExistenceCache.get(url);
  if (cached) {
    const ttl = cached.exists ? CACHE_TTL_SUCCESS : CACHE_TTL_FAILURE;
    if (now - cached.checkedAt < ttl) {
      return cached.exists;
    }
  }

  try {
    // Cloudflare Workers fetch 默认跟随 302 重定向
    const response = await fetch(url, {
      method: 'HEAD',
      headers: {
        'User-Agent': 'sheeps-update-checker'
      }
    });
    
    const exists = response.status === 200;
    apkExistenceCache.set(url, { exists, checkedAt: now });
    return exists;
  } catch {
    apkExistenceCache.set(url, { exists: false, checkedAt: now });
    return false;
  }
}
```

### 数据库查询升级
在 `getDatabaseAppUpdate` 中进行多版本匹配：
```typescript
async function getDatabaseAppUpdate(env: Env, currentCode: number): Promise<AppUpdatePayload> {
  const versions = await env.DB.prepare(
    'SELECT version_code, version_name, apk_url, update_log, is_force_update FROM app_version WHERE version_code > ? ORDER BY version_code DESC LIMIT 5'
  ).bind(currentCode).all<{ version_code: number; version_name: string; apk_url: string; update_log: string; is_force_update: number }>();

  if (versions.results && versions.results.length > 0) {
    for (const release of versions.results) {
      if (release.apk_url) {
        const exists = await checkApkExists(release.apk_url);
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
```

---

## 验证计划

### 自动化验证
- 在 `server/test/update.test.js` 中增加 mock 测试以验证 `checkApkExists` 函数与 `getDatabaseAppUpdate` 的行为：
  1. 测试 `checkApkExists` 能正确使用缓存。
  2. 测试 `getDatabaseAppUpdate` 能够正确忽略未完成构建的版本，并返回最近已成功的更新。

### 手动验证
- 将服务部署到本地并启动。
- 模拟 D1 数据库中插入一条带 404 APK 链接的高版本记录。
- 请求更新接口，确认不返回该 404 版本更新。
- 模拟将链接变为可正常访问的链接（或等待缓存过期后使其可访问），再次请求接口，确认能正确弹出更新提示。
