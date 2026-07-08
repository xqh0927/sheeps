const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');
const https = require('https');

// 引入代理库（如果没安装则降级为直连）
let HttpsProxyAgent;
try { HttpsProxyAgent = require('https-proxy-agent').HttpsProxyAgent; } catch { }

// 填写你本地的 HTTP/SOCKS 代理地址（已为你默认填好 10808）
const PROXY_URL = process.env.HTTPS_PROXY || process.env.HTTP_PROXY || 'http://127.0.0.1:10808';

// ─── 管理后台 HTTP API 配置（公告发布走后台 API，版本仍用 wrangler） ───
// 可通过环境变量覆盖，或直接替换占位常量后运行
const ADMIN_API_BASE = process.env.ADMIN_API_BASE || 'https://api.xqh.cc.cd';
const ADMIN_PHONE = process.env.ADMIN_PHONE || '13800000000';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';

/**
 * 判断错误是否属于「确定性 / 不可重试」错误。
 * 这类错误（如主键/唯一约束冲突、已存在/已声明等）重试必然再次失败，
 * 应直接终止以避免无意义的退避与重试。
 */
function isNonRetryable(err) {
    const m = (err && err.message) || '';
    return /UNIQUE constraint|SQLITE_CONSTRAINT|constraint failed|has already been declared|already exists/i.test(m);
}

/**
 * 带有指数退避的通用异步重试函数
 */
async function withRetry(fn, retries = 3, delay = 2000, actionName = "任务") {
    for (let i = 1; i <= retries; i++) {
        try {
            return await fn();
        } catch (err) {
            // 确定性错误（如 D1 主键冲突）立即失败，不再重试，直接抛出根因
            if (isNonRetryable(err)) {
                throw new Error(`[${actionName}] 不可重试的错误，立即终止: ${err.message}`);
            }
            console.warn(`⚠️ [${actionName}] 第 ${i}/${retries} 次失败: ${err.message || err}`);
            if (i === retries) {
                throw new Error(`[${actionName}] 失败次数已达上限，终止执行！`);
            }
            await new Promise(res => setTimeout(res, delay * i));
        }
    }
}

/**
 * 使用 Google Translate 免费接口翻译文本 (通过代理出墙 + 修复参数传入)
 * 繁体中文代码为: zh-TW
 */
function translateText(text, targetLang) {
    return new Promise((resolve, reject) => {
        const encoded = encodeURIComponent(text);
        const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${targetLang}&dt=t&q=${encoded}`;

        const options = {};
        if (HttpsProxyAgent && PROXY_URL) {
            options.agent = new HttpsProxyAgent(PROXY_URL);
        }

        https.get(url, options, (res) => {
            if (res.statusCode !== 200) {
                return reject(new Error(`HTTP 状态码异常: ${res.statusCode}`));
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const result = JSON.parse(data);
                    const translated = result[0].map(seg => seg[0]).join('');
                    resolve(translated);
                } catch (e) {
                    reject(new Error(`翻译结果 JSON 解析失败: ${e.message}`));
                }
            });
        }).on('error', (err) => reject(err));
    });
}

/**
 * 调用管理后台登录接口，返回可用于后续请求的 Bearer Token。
 * 非 2xx 或未返回 token 时抛错（携带响应文本便于排查）。
 */
async function adminLogin() {
    const loginUrl = `${ADMIN_API_BASE}/api/admin/login`;
    const resp = await fetch(loginUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: ADMIN_PHONE, password: ADMIN_PASSWORD }),
    });
    const text = await resp.text();
    if (!resp.ok) {
        throw new Error(`登录接口返回 HTTP ${resp.status}: ${text}`);
    }
    let json;
    try {
        json = JSON.parse(text);
    } catch (e) {
        throw new Error(`登录接口响应非 JSON: ${text}`);
    }
    // 兼容响应直接返回 token，或嵌套于 data.token 两种情况
    const token = json.token || (json.data && json.data.token);
    if (!token) {
        throw new Error(`登录响应中未包含 token: ${text}`);
    }
    return token;
}

/**
 * 调用管理后台公告创建接口，将公告写入 D1（真理来源）。
 * 非 2xx 时抛错（携带响应文本便于排查），成功返回解析后的 JSON。
 */
async function createNoticeViaApi(token, payload) {
    const url = `${ADMIN_API_BASE}/api/admin/notices`;
    const resp = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
    });
    const text = await resp.text();
    if (!resp.ok) {
        throw new Error(`公告创建接口返回 HTTP ${resp.status}: ${text}`);
    }
    try {
        return JSON.parse(text);
    } catch (e) {
        throw new Error(`公告创建接口响应非 JSON: ${text}`);
    }
}

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const gradlePath = path.join(__dirname, 'app', 'app', 'build.gradle.kts');
const serverDir = path.join(__dirname, 'server');

function run() {
    if (!fs.existsSync(gradlePath)) {
        console.error(`错误: 找不到 build.gradle.kts 文件，预期路径为: ${gradlePath}`);
        process.exit(1);
    }

    const originalGradleContent = fs.readFileSync(gradlePath, 'utf8');
    const codeMatch = originalGradleContent.match(/versionCode\s*=\s*(\d+)/);
    const nameMatch = originalGradleContent.match(/versionName\s*=\s*"([^"]+)"/);

    if (!codeMatch || !nameMatch) {
        console.error("错误: 无法在 build.gradle.kts 中解析出 versionCode 或 versionName！");
        process.exit(1);
    }

    const currentCode = parseInt(codeMatch[1], 10);
    const currentName = nameMatch[1];

    console.log(`\n======================================`);
    console.log(`当前 App 版本信息:`);
    console.log(`  versionCode = ${currentCode}`);
    console.log(`  versionName = "${currentName}"`);
    console.log(`======================================\n`);

    const R2_PUBLIC_BASE = 'https://file.xqh.cc.cd';
    const nextCode = currentCode + 1;
    // 正则定位末尾数字自增，兼容 1.0.6 和 1.0.6-beta1
    const nextName = currentName.replace(/(\d+)(?=[^\d]*$)/, (m) => parseInt(m, 10) + 1);

    rl.question(`请输入新的 versionCode [回车默认: ${nextCode}]: `, (codeAns) => {
        let newCode = codeAns.trim() ? parseInt(codeAns.trim(), 10) : nextCode;

        rl.question(`请输入新的 versionName [回车默认: "${nextName}"]: `, (nameAns) => {
            const newName = nameAns.trim() ? nameAns.trim() : nextName;

            rl.question(`请输入本次版本更新的说明日志 (update_log): `, (logAns) => {
                const updateLog = logAns.trim() ? logAns.trim() : "常规性能优化与问题修复";

                console.log(`\n准备执行以下【安全发布流水线】:`);
                console.log(`1. 自动翻译升级日志（包含繁体中文、英文、日文、韩文）`);
                console.log(`2. 更新云端 D1 数据库（仅写入版本号）`);
                console.log(`3. 调用管理后台 API 发布多语言公告 + 刷新 KV 边缘缓存`);
                console.log(`4. 确认云端全部成功后，更新 gradle 文件并 Push 代码触发编译`);

                rl.question(`\n确认开始发布？(y/n): `, async (confirm) => {
                    if (confirm.toLowerCase() !== 'y' && confirm.toLowerCase() !== 'yes') {
                        console.log("发布已取消。");
                        rl.close();
                        process.exit(0);
                    }

                    // ─── 管理后台凭据守卫：缺少凭据则拒绝继续 ───
                    if (!ADMIN_PHONE || !ADMIN_PASSWORD) {
                        console.error(`\n❌ 缺少管理后台登录凭据！`);
                        console.error(`   请通过环境变量设置 ADMIN_PHONE / ADMIN_PASSWORD。`);
                        console.error(`   例如: ADMIN_PHONE=138xxxx ADMIN_PASSWORD=xxxx node release.js`);
                        rl.close();
                        process.exit(1);
                    }

                    let isGradleUpdated = false;
                    const tempSqlPath = path.join(__dirname, 'temp_release_query.sql');
                    const tempKvPath = path.join(__dirname, 'temp_kv_payload.json');

                    // ─── Git 前置安全校验 ───
                    console.log("➔ 正在校验 Git 工作区...");
                    try {
                        const currentBranch = execSync('git rev-parse --abbrev-ref HEAD', { stdio: 'pipe' }).toString().trim();
                        if (currentBranch !== 'main') {
                            console.error(`\n❌ 当前在 [${currentBranch}] 分支，发版必须在 [main] 分支！`);
                            rl.close(); process.exit(1);
                        }
                        // 先拉取远程最新代码，避免 push 时发生冲突导致 D1 已写入
                        execSync('git pull origin main --rebase', { stdio: 'pipe', timeout: 30000 });
                        console.log("   ✓ 分支检查通过，已同步远程最新代码");
                    } catch (gitErr) {
                        console.error(`\n❌ Git 前置检查失败: ${gitErr.message}`);
                        console.error("   请先解决冲突或切换回 main 分支后再试。");
                        rl.close(); process.exit(1);
                    }

                    try {
                        // ─── 第一步：网络翻译 (加入重试 + 降级兜底 + 增加繁体中文) ───
                        console.log("\n➔ [1/4] 正在连接 Google Translate 自动翻译版本日志...");
                        const versionTitle = `v${newName} 版本更新 / Version Update`;

                        const safeTranslate = async (text, targetLang, actionName) => {
                            try {
                                return await withRetry(() => translateText(text, targetLang), 3, 1000, actionName);
                            } catch (err) {
                                console.warn(`   ⚠️ [降级提示] ${actionName} 无法连接，将自动回退使用原始中文！`);
                                return text;
                            }
                        };

                        // ⚠️ 修复：增加了 'zh-TW' 繁体中文翻译
                        const [titleTw, titleEn, titleJa, titleKo, contentTw, contentEn, contentJa, contentKo] = await Promise.all([
                            safeTranslate(versionTitle, 'zh-TW', "翻译标题-繁体"),
                            safeTranslate(versionTitle, 'en', "翻译标题-英文"),
                            safeTranslate(versionTitle, 'ja', "翻译标题-日文"),
                            safeTranslate(versionTitle, 'ko', "翻译标题-韩文"),
                            safeTranslate(updateLog, 'zh-TW', "翻译日志-繁体"),
                            safeTranslate(updateLog, 'en', "翻译日志-英文"),
                            safeTranslate(updateLog, 'ja', "翻译日志-日文"),
                            safeTranslate(updateLog, 'ko', "翻译日志-韩文")
                        ]);
                        console.log("   ✔ 多语言（含繁体中文）文本处理完毕");

                        // ─── 第二步：更新 D1 数据库 ───
                        console.log("➔ [2/4] 正在更新云端 D1 数据库（版本号）...");
                        const apkUrl = `${R2_PUBLIC_BASE}/sheeps_${newName}.apk`;
                        const now = Date.now();
                        const escSql = (s) => s.replace(/'/g, "''");

                        // ─── 发版前先查询 D1 当前最大 version_code（权威基准，避免盲插主键冲突） ───
                        // 根因：原逻辑直接拿 newCode（= gradle versionCode+1 或交互输入）做 INSERT 主键，
                        // 若 D1 已存在该值（调试写入 / 上次发版 D1 成功而后继步骤失败导致 gradle 回滚留孤儿记录）
                        // 必触发 UNIQUE constraint failed: app_version.version_code。
                        let dbMaxCode = 0;
                        try {
                            const maxOut = execSync(
                                `npx wrangler d1 execute my-app-db --remote --command="SELECT MAX(version_code) AS m FROM app_version"`,
                                { cwd: serverDir, stdio: 'pipe', timeout: 30000 }
                            ).toString();
                            // 输出可能是 JSON 形如 {"result":[{"results":[{"m":109}]}]} 或 "m": null，用正则稳健提取
                            const m = maxOut.match(/"m"\s*:\s*(\d+)/);
                            if (m) {
                                dbMaxCode = parseInt(m[1], 10);
                            } else if (/"m"\s*:\s*null/i.test(maxOut)) {
                                dbMaxCode = 0; // 表为空，无记录
                            } else {
                                dbMaxCode = 0;
                            }
                        } catch (dbErr) {
                            // 查询失败不中断发版，降级为以 gradle/newCode 为准，仅给出告警
                            dbMaxCode = 0;
                            console.warn(`   ⚠️ [容错] 查询 D1 当前最大 version_code 失败，将以 gradle 推断值继续: ${dbErr.message}`);
                        }

                        // 以 D1 真实最大值 +1 与 gradle 推断值取较大者，作为本次实际写入的 version_code
                        const effectiveNewCode = Math.max(dbMaxCode + 1, newCode);
                        if (effectiveNewCode !== newCode) {
                            console.warn(`⚠️ D1 当前最大 version_code=${dbMaxCode}，为避免主键冲突，本次 version_code 将使用 ${effectiveNewCode}（gradle 推断值为 ${newCode}）`);
                            console.warn(`   并已同步回写 gradle（第四步版本文件改写将使用 ${effectiveNewCode}）`);
                        }
                        // 统一后续 SQL 与第四步 gradle 改写实际使用的 code 为 effectiveNewCode
                        newCode = effectiveNewCode;

                        // 仅写入 app_version 表；公告改由管理后台 API 写入，不再用 wrangler 写 notice 表
                        const sqlContent = [
                            `INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES (${newCode}, '${escSql(newName)}', '${escSql(apkUrl)}', '${escSql(updateLog)}', 0, ${now});`
                        ].join('\n');

                        fs.writeFileSync(tempSqlPath, sqlContent, 'utf8');

                        await withRetry(() => {
                            return new Promise((res, rej) => {
                                try {
                                    execSync(`npx wrangler d1 execute my-app-db --remote --file="${tempSqlPath}"`, { cwd: serverDir, stdio: 'pipe', timeout: 30000 });
                                    res();
                                } catch (e) { rej(e); }
                            });
                        }, 3, 2000, "D1 数据库远程更新");

                        console.log("   ✔ D1 数据库更新成功！");

                        // ─── 第三步：调用管理后台 API 发布公告 + 刷新 KV 边缘缓存 ───
                        console.log("➔ [3/4] 正在调用管理后台 API 发布多语言公告 + 刷新 KV 边缘缓存...");

                        // 3.1 先通过后台 API 将公告写入 D1（真理来源）
                        const token = await withRetry(() => adminLogin(), 3, 2000, "管理后台登录");
                        console.log("   ✔ 管理后台登录成功，已获取访问令牌");

                        // 公告请求体：透传列，后端 genericCreate 会直接以 body 的 key 作为列名 INSERT
                        const noticeBody = {
                            title: versionTitle,
                            content: updateLog,
                            type: 'update',
                            created_at: now,
                            title_tw: titleTw, title_en: titleEn, title_ja: titleJa, title_ko: titleKo,
                            content_tw: contentTw, content_en: contentEn, content_ja: contentJa, content_ko: contentKo,
                        };
                        await withRetry(() => createNoticeViaApi(token, noticeBody), 3, 2000, "公告 API 写入 D1");
                        console.log("   ✔ 公告已通过管理后台 API 写入 D1（真理来源）");

                        // 3.2 刷新 KV 边缘缓存（键名必须使用 _v2 后缀，与客户端读端对齐）
                        const kvNsId = '784104ac67eb4f3c83a92e9dcc91b673';

                        // 各语言公告项：默认 lang='' 用中文；tw/en/ja/ko 用对应翻译结果
                        const noticesByLang = {
                            '': { title: versionTitle, content: updateLog },
                            'tw': { title: titleTw, content: contentTw },
                            'en': { title: titleEn, content: contentEn },
                            'ja': { title: titleJa, content: contentJa },
                            'ko': { title: titleKo, content: contentKo },
                        };

                        let kvAllOk = true;
                        for (const [lang, item] of Object.entries(noticesByLang)) {
                            // KV key 格式：有语言后缀时用 notices_{lang}_v2，无语言时用 notices_v2（避免双下划线）
                            const cacheKey = lang ? `notices_${lang}_v2` : `notices_v2`;
                            try {
                                await withRetry(() => {
                                    return new Promise((res, rej) => {
                                        try {
                                            let list = [];
                                            try {
                                                const existing = execSync(`npx wrangler kv key get "${cacheKey}" --namespace-id ${kvNsId} --remote --text`, { cwd: serverDir, stdio: 'pipe', timeout: 10000 }).toString().trim();
                                                if (existing && existing !== 'null' && existing !== 'undefined') {
                                                    list = JSON.parse(existing);
                                                }
                                            } catch { }

                                            list.unshift({ title: item.title, content: item.content, type: 'update', created_at: now });
                                            fs.writeFileSync(tempKvPath, JSON.stringify(list), 'utf8');
                                            execSync(`npx wrangler kv key put "${cacheKey}" --namespace-id ${kvNsId} --remote --path="${tempKvPath}"`, { cwd: serverDir, stdio: 'pipe' });
                                            res();
                                        } catch (err) { rej(err); }
                                    });
                                }, 3, 2000, `KV同步[${lang || 'zh'}]`);
                            } catch (kvErr) {
                                // 容错：D1 已有数据，客户端在 1h TTL 兜底期内仍能读到；不因此中断发版
                                kvAllOk = false;
                                console.warn(`   ⚠️ [容错] KV 缓存同步失败 (${lang || 'zh'})，但 D1 已落库，客户端将在 ≤1h 内读到: ${kvErr.message}`);
                            }
                        }
                        if (kvAllOk) {
                            console.log("   ✔ 多语言 KV 边缘缓存全部同步完毕");
                        } else {
                            console.warn("   ⚠️ 部分 KV 边缘缓存同步失败，但发版继续（D1 数据已落库）。");
                        }

                        // ─── 第四步：全部网络交互成功后，修改本地 gradle 并执行 Git 操作 ───
                        console.log("➔ [4/4] 云端数据与缓存已锁定，正在修改版本文件并 Push 代码...");
                        let updatedGradle = originalGradleContent
                            .replace(/versionCode\s*=\s*\d+/, `versionCode = ${newCode}`)
                            .replace(/versionName\s*=\s*"[^"]+"/, `versionName = "${newName}"`);
                        fs.writeFileSync(gradlePath, updatedGradle, 'utf8');
                        isGradleUpdated = true;

                        execSync('git add -A', { cwd: __dirname, stdio: 'inherit' });
                        try {
                            execSync(`git commit -m "chore(release): bump version to v${newName} & update release notes"`, { cwd: __dirname, stdio: 'inherit' });
                        } catch {
                            console.log("提示: 提交树中没有产生可单独提新 commit 的改动...");
                        }
                        execSync('git push origin main', { cwd: __dirname, stdio: 'inherit' });
                        console.log("   ✔ Git 推送成功！远程 CI/CD 编译打包已安全触发");

                        console.log(`\n🎉🎉 发版流水线全部完美结束！`);
                        console.log(`👉 版本号: v${newName} (${newCode})`);
                        console.log(`👉 R2 APK 预定地址: ${apkUrl}`);
                        console.log(`👉 Actions 稍后构建完成后覆盖至 R2，用户端将丝滑无感获取更新！\n`);

                    } catch (err) {
                        console.error(`\n❌ 发版流水线中发生异常中止: ${err.message}`);

                        if (isGradleUpdated) {
                            console.log("🛡️ 正在回滚本地 build.gradle.kts 至修改前状态...");
                            fs.writeFileSync(gradlePath, originalGradleContent, 'utf8');
                            console.log("   ✔ 本地文件已恢复，请检查网络或配置后重新启动 release！");
                        } else {
                            console.log("🛡️ 本地代码未做任何变更，无需回滚。");
                        }
                    } finally {
                        if (fs.existsSync(tempSqlPath)) fs.unlinkSync(tempSqlPath);
                        if (fs.existsSync(tempKvPath)) fs.unlinkSync(tempKvPath);
                        rl.close();
                    }
                });
            });
        });
    });
}

run();