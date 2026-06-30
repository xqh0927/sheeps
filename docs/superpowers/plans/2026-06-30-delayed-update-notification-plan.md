# 延迟更新提示直至构建完成与玩法说明更新 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在 Cloudflare Worker 端通过轻量级 HEAD 请求以及内存缓存机制检查 APK 是否已经构建成功，避免向用户推送 404 下载链接；同时更新项目 README 核心文档和 Android 端的对局玩法指南。

**架构：** 
1. 在 Server 端引入 `apkExistenceCache` 内存缓存，记录已检测过的 APK URL 可达状态（成功缓存 24 小时，失败缓存 1 分钟）。
2. 在更新检测中，查询所有比客户端当前版本高的历史记录，自顶向下进行检测，返回第一个可用的构建版本。
3. 对 Cloudflare Worker 更新逻辑及数据库 Mock 调用编写 Node.js 单元测试并确保通过。
4. 更新项目 README 说明和 App 端的 `GameGuideDialog.kt` 玩法引导。

**技术栈：** TypeScript, Cloudflare Workers, Node.js Test Runner, Kotlin, Jetpack Compose.

---

### 任务 1：测试与实现 APK 可用性检查与缓存逻辑

**文件：**
- 修改：`server/src/index.ts`
- 修改：`server/test/update.test.js`

- [ ] **步骤 1：在 `server/test/update.test.js` 中编写 checkApkExists 的失败测试**
  在测试文件中，编写测试以检验 `checkApkExists` 能否在不同的 fetch 响应（200 或 404）下返回正确的值，并能正确利用缓存。
  ```javascript
  // 临时在测试中模拟全局 fetch 以便独立运行
  const originalFetch = globalThis.fetch;
  test('checkApkExists fetches URL status and caches result', async () => {
    let callCount = 0;
    globalThis.fetch = async (url, init) => {
      callCount++;
      if (url === 'https://example.com/exist.apk') {
        return { status: 200 };
      }
      return { status: 404 };
    };

    const { checkApkExists, clearApkCache } = require('../.tmp-test/index.js');
    if (clearApkCache) clearApkCache();

    // 第一次请求：应该调用 fetch 并返回 true
    const ok = await checkApkExists('https://example.com/exist.apk');
    assert.equal(ok, true);
    assert.equal(callCount, 1);

    // 第二次请求：应该命中缓存，不会再次调用 fetch
    const okCached = await checkApkExists('https://example.com/exist.apk');
    assert.equal(okCached, true);
    assert.equal(callCount, 1);

    // 请求一个不存在的：应该返回 false
    const notOk = await checkApkExists('https://example.com/404.apk');
    assert.equal(notOk, false);
    assert.equal(callCount, 2);

    globalThis.fetch = originalFetch;
  });
  ```

- [ ] **步骤 2：运行测试验证编译/运行失败**
  由于目前尚未在 `server/src/index.ts` 中实现且导出 `checkApkExists`，测试会编译错误或运行失败。
  在 `server` 目录下运行：
  `npm run test`
  确认出现报错。

- [ ] **步骤 3：在 `server/src/index.ts` 中编写实现**
  在 `server/src/index.ts` 的前部（或适当位置）定义缓存并实现 `checkApkExists` 以及清理缓存的辅助函数：
  ```typescript
  // 新增 APK 缓存
  export interface ApkStatus {
    exists: boolean;
    checkedAt: number;
  }
  
  export const apkExistenceCache = new Map<string, ApkStatus>();
  const CACHE_TTL_SUCCESS = 24 * 60 * 60 * 1000; // 24小时
  const CACHE_TTL_FAILURE = 60 * 1000; // 60秒
  
  export function clearApkCache() {
    apkExistenceCache.clear();
  }
  
  export async function checkApkExists(url: string): Promise<boolean> {
    const now = Date.now();
    const cached = apkExistenceCache.get(url);
    if (cached) {
      const ttl = cached.exists ? CACHE_TTL_SUCCESS : CACHE_TTL_FAILURE;
      if (now - cached.checkedAt < ttl) {
        return cached.exists;
      }
    }
  
    try {
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

- [ ] **步骤 4：运行测试验证通过**
  在 `server` 目录下运行：
  `npm run test`
  确认测试成功通过。

- [ ] **步骤 5：Commit**
  ```bash
  git add server/src/index.ts server/test/update.test.js
  git commit -m "feat(server): implement checkApkExists helper with in-memory caching"
  ```

---

### 任务 2：测试并升级 `getDatabaseAppUpdate` 的多版本检测逻辑

**文件：**
- 修改：`server/src/index.ts`
- 修改：`server/test/update.test.js`

- [ ] **步骤 1：在 `server/test/update.test.js` 中编写 getDatabaseAppUpdate 的单元测试**
  在测试中，我们需要 mock `env.DB` 模拟 D1 数据库的行为，以及 mock 全局 `fetch`，测试当存在多个高版本时：
  1. 最高版本还在构建中（APK 返回 404）
  2. 次高版本已经完成（APK 返回 200）
  此时应能够过滤高版本，正确返回次高版本的更新响应。
  ```javascript
  test('getDatabaseAppUpdate filters out unbuilt releases and returns the latest available', async () => {
    const { getDatabaseAppUpdate, clearApkCache } = require('../.tmp-test/index.js');
    if (clearApkCache) clearApkCache();

    // Mock D1 数据库
    const mockDb = {
      prepare(query) {
        return {
          bind(currentCode) {
            return {
              async all() {
                // 模拟返回两个更高版本，v3.apk 未成功上传，v2.apk 已上传
                return {
                  results: [
                    { version_code: 3, version_name: '1.0.2', apk_url: 'https://example.com/v3.apk', update_log: 'v3更新', is_force_update: 0 },
                    { version_code: 2, version_name: '1.0.1', apk_url: 'https://example.com/v2.apk', update_log: 'v2更新', is_force_update: 0 }
                  ]
                };
              }
            };
          }
        };
      }
    };

    const originalFetch = globalThis.fetch;
    globalThis.fetch = async (url) => {
      if (url === 'https://example.com/v2.apk') {
        return { status: 200 };
      }
      return { status: 404 }; // v3.apk 404
    };

    const env = { DB: mockDb };
    // 当前客户端版本为 1，数据库有 2 和 3
    const updateResult = await getDatabaseAppUpdate(env, 1);
    
    assert.equal(updateResult.has_update, true);
    assert.equal(updateResult.version_name, '1.0.1'); // 应返回 v2 版本
    assert.equal(updateResult.apk_url, 'https://example.com/v2.apk');
    assert.equal(updateResult.update_log, 'v2更新');

    globalThis.fetch = originalFetch;
  });
  ```

- [ ] **步骤 2：运行测试验证失败**
  在 `server` 目录下运行：
  `npm run test`
  确认测试因旧实现不支持过滤和多版本查询而失败。

- [ ] **步骤 3：在 `server/src/index.ts` 中升级 `getDatabaseAppUpdate` 逻辑**
  替换旧的仅查询 `LIMIT 1` 的实现，升级为多版本查询并逐个检测：
  ```typescript
  export async function getDatabaseAppUpdate(env: Env, currentCode: number): Promise<AppUpdatePayload> {
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

- [ ] **步骤 4：运行测试验证通过**
  在 `server` 目录下运行：
  `npm run test`
  确认所有测试（包括新写的测试）全部通过。

- [ ] **步骤 5：Commit**
  ```bash
  git add server/src/index.ts server/test/update.test.js
  git commit -m "feat(server): upgrade getDatabaseAppUpdate to check multiple version URLs"
  ```

---

### 任务 3：更新 README.md 与 Android 玩法指南说明

**文件：**
- 修改：`README.md`
- 修改：`app/feature_menu/src/main/java/com/example/sheeps/menu/ui/dialogs/GameGuideDialog.kt`

- [ ] **步骤 1：更新 `README.md`**
  在 `README.md` 的 "项目亮点与核心功能" 或相关地方，说明本次修复：
  - 更新版本发布策略，增加 Cloudflare Worker 端的 APK 延迟检测与内存缓存机制，彻底解决 404 问题。
  - 在“玩法说明”或功能列表中，增加多人对战模式中关于连击攻击与三大诅咒法术系统（迷雾咒、锁槽咒、封印咒）的细化说明。

- [ ] **步骤 2：更新 `GameGuideDialog.kt` 玩法说明**
  在 `GameGuideDialog.kt` 的 LazyColumn 底部（即在“商城说明”之后），新增卡牌：
  ```kotlin
  // 5. 天命对决多人对局与法术诅咒
  item {
      GuideSectionCard(
          title = "五、天命对决 (多人竞技法术)",
          description = "对决模式下，每次熔炼消除卡牌将充盈能量（上限 10 点）。\n迷雾咒（3能）：令对手战场黑雾弥漫，需点击探照驱散（6秒）。\n锁槽咒（6能）：封锁对手最后 1 个消除槽，降至 6 格（8秒），槽满立即判负。\n封印咒（10能）：瞬间给对手所有当前可点击的暴露牌附加封印，迟滞其消除。",
          icon = { StarCanvas() } // 可以复用 StarCanvas 或定义合适的 Canvas
      )
  }
  ```

- [ ] **步骤 3：手动检查和编译**
  检查 Android 项目能否成功编译：
  ```powershell
  # 在根目录运行 gradle 编译 Feature_menu
  ./app/gradlew.bat -p app :feature_menu:assembleRelease --no-daemon
  ```
  确认编译正常，没有语法或导包错误。

- [ ] **步骤 4：Commit**
  ```bash
  git add README.md app/feature_menu/src/main/java/com/example/sheeps/menu/ui/dialogs/GameGuideDialog.kt
  git commit -m "docs: update README and GameGuideDialog to include multiplayer spell rules and delayed update detection explanation"
  ```
