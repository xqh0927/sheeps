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

// 将代理地址注入当前进程 env，确保 execSync 启动的 wrangler 子进程也能感知并使用代理
// （wrangler 使用自己的 HTTP 客户端 undici，不接受 Node.js Agent，必须通过环境变量传递）
if (PROXY_URL) {
    process.env.HTTPS_PROXY = PROXY_URL;
    process.env.HTTP_PROXY = PROXY_URL;
}

// ─── 管理后台 HTTP API 配置（版本发布与公告均走后台 API） ───
// 可通过环境变量覆盖，或直接替换占位常量后运行
const ADMIN_API_BASE = process.env.ADMIN_API_BASE || 'https://api.xqh.cc.cd';
// 安全提示: 默认凭据仅作本地发版便捷之用; 请将真实账号通过环境变量 ADMIN_PHONE / ADMIN_PASSWORD 注入, 切勿将含真实密码的脚本提交到仓库
const ADMIN_PHONE = process.env.ADMIN_PHONE || '13800000000';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';

// ─── 部署 / 仓库 / KV 相关配置 ───
const R2_PUBLIC_BASE = 'https://file.xqh.cc.cd';           // APK 对外下载域名（R2 自定义域）
const PAGES_PROJECT = 'miadmin-console';                    // Cloudflare Pages 项目名（管理后台）
const KV_NS_ID = '784104ac67eb4f3c83a92e9dcc91b673';        // SHEEPS_CACHE 命名空间 id（见 server/wrangler.jsonc）
const KV_CLEAR_PREFIXES = ['update_check_', 'notices_', 'config_']; // 发布成功后需清空的缓存键前缀
const GITHUB_OWNER = 'xqh0927';                             // 代码仓库 owner（Actions 在此仓库运行）
const GITHUB_REPO = 'sheeps';                               // 代码仓库名
// 用于轮询 GitHub Actions 状态的 token（可用 GH_TOKEN / GITHUB_TOKEN / MY_PAT 任一注入）
const GH_TOKEN = process.env.GH_TOKEN || process.env.GITHUB_TOKEN || process.env.MY_PAT || '';

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
        signal: AbortSignal.timeout(15000),
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
        signal: AbortSignal.timeout(15000),
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

/**
 * 调用管理后台「新增 App 版本」接口，由后端统一处理 status=1 自动发布、
 * release_time 写入与 update_log 的 5 语言 i18n 落库（避免脚本直写 D1 与后台逻辑脱节）。
 * 非 2xx 时抛错（携带响应文本便于排查），成功返回解析后的 JSON。
 */
async function createAppVersionViaApi(token, payload) {
    const url = `${ADMIN_API_BASE}/api/admin/app-versions`;
    const resp = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(15000),
    });
    const text = await resp.text();
    if (!resp.ok) {
        throw new Error(`版本创建接口返回 HTTP ${resp.status}: ${text}`);
    }
    try {
        return JSON.parse(text);
    } catch (e) {
        throw new Error(`版本创建接口响应非 JSON: ${text}`);
    }
}

/**
 * 部署 Cloudflare Worker（后台接口 server）。使用本地当前代码执行 wrangler deploy。
 * 失败直接抛错以中止发版（stdio 继承便于实时查看部署日志）。
 */
function deployWorker() {
    execSync('npx wrangler deploy', { cwd: serverDir, stdio: 'inherit', timeout: 300000 });
}

/**
 * 构建并部署管理后台（admin-console → Cloudflare Pages）。
 * 先 npm run build 产出 dist，再 wrangler pages deploy dist。失败抛错中止发版。
 */
function deployPages() {
    execSync('npm run build', { cwd: adminDir, stdio: 'inherit', timeout: 300000 });
    execSync(`npx wrangler pages deploy dist --project-name ${PAGES_PROJECT}`, { cwd: adminDir, stdio: 'inherit', timeout: 300000 });
}

/**
 * 通过 GitHub REST API 发起 GET 请求（走本地代理），返回解析后的 JSON。
 * 用于轮询 Actions 运行状态。带 20s 超时与可选 Bearer 认证。
 */
function githubApiGet(apiPath) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'api.github.com',
            path: apiPath,
            method: 'GET',
            headers: {
                'User-Agent': 'sheeps-release-script',
                'Accept': 'application/vnd.github+json',
                'X-GitHub-Api-Version': '2022-11-28',
            },
        };
        if (GH_TOKEN) options.headers['Authorization'] = `Bearer ${GH_TOKEN}`;
        if (HttpsProxyAgent && PROXY_URL) options.agent = new HttpsProxyAgent(PROXY_URL);

        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => {
                if (res.statusCode < 200 || res.statusCode >= 300) {
                    return reject(new Error(`GitHub API HTTP ${res.statusCode}: ${String(data).slice(0, 200)}`));
                }
                try { resolve(JSON.parse(data)); }
                catch (e) { reject(new Error(`GitHub API 响应非 JSON: ${e.message}`)); }
            });
        });
        req.on('error', reject);
        req.setTimeout(20000, () => req.destroy(new Error('GitHub API 请求超时')));
        req.end();
    });
}

/**
 * 轮询等待本次提交（headSha）触发的 GitHub Actions 打包流水线完成。
 * 成功返回 true；失败或超时抛错。最长等待 20 分钟，每 15s 轮询一次。
 */
async function waitForCiRun(headSha) {
    const maxWaitMs = 20 * 60 * 1000;
    const intervalMs = 15000;
    const start = Date.now();
    let seen = false;

    while (Date.now() - start < maxWaitMs) {
        let runs = [];
        try {
            const json = await githubApiGet(`/repos/${GITHUB_OWNER}/${GITHUB_REPO}/actions/runs?head_sha=${headSha}&per_page=10`);
            runs = (json && json.workflow_runs) || [];
        } catch (e) {
            console.warn(`   ⚠️ 查询 CI 状态失败（将重试）: ${e.message}`);
        }

        if (runs.length > 0) {
            seen = true;
            const run = runs[0]; // 最新一次
            const mins = Math.round((Date.now() - start) / 6000) / 10;
            if (run.status === 'completed') {
                if (run.conclusion === 'success') {
                    console.log(`   ✔ CI 打包成功（run #${run.run_number}）`);
                    return true;
                }
                throw new Error(`CI 打包未成功，结论=${run.conclusion}，详见 ${run.html_url}`);
            }
            console.log(`   … CI 进行中（${run.status}），已等待 ${mins} 分钟...`);
        } else {
            console.log(`   … 尚未检测到本次提交触发的 CI，继续等待...`);
        }
        await new Promise(r => setTimeout(r, intervalMs));
    }
    throw new Error(`等待 CI 超时（${maxWaitMs / 60000} 分钟）${seen ? '，CI 仍未完成' : '，未检测到 CI run'}`);
}

/**
 * 清空 KV 边缘缓存：列出所有键，过滤 KV_CLEAR_PREFIXES 指定前缀，批量删除。
 * 让客户端下次请求回源读取最新 D1 数据（版本/公告/配置）。
 */
function clearKvCache() {
    let names = [];
    const listOut = execSync(
        `npx wrangler kv key list --namespace-id ${KV_NS_ID} --remote`,
        { cwd: serverDir, stdio: ['ignore', 'pipe', 'pipe'], timeout: 30000 }
    ).toString();
    // wrangler 会把 banner 输出到 stderr，stdout 为纯 JSON 数组；容错截取 [ ... ]
    const s = listOut.indexOf('[');
    const e = listOut.lastIndexOf(']');
    if (s !== -1 && e !== -1 && e > s) {
        const arr = JSON.parse(listOut.slice(s, e + 1));
        names = arr.map(k => k.name).filter(n => KV_CLEAR_PREFIXES.some(p => n.startsWith(p)));
    }

    if (names.length === 0) {
        console.log('   ✔ 无匹配前缀的 KV 缓存键，无需清理');
        return;
    }

    const bulkPath = path.join(__dirname, 'temp_kv_delete.json');
    fs.writeFileSync(bulkPath, JSON.stringify(names), 'utf8');
    try {
        execSync(
            `npx wrangler kv bulk delete "${bulkPath}" --namespace-id ${KV_NS_ID} --remote --force`,
            { cwd: serverDir, stdio: 'pipe', timeout: 60000 }
        );
        console.log(`   ✔ 已清空 ${names.length} 个 KV 缓存键（前缀: ${KV_CLEAR_PREFIXES.join(', ')}）`);
    } finally {
        if (fs.existsSync(bulkPath)) fs.unlinkSync(bulkPath);
    }
}

/** 将 rl.question 包装为 Promise，用于无 token 时的交互式确认兜底 */
function ask(question) {
    return new Promise(resolve => rl.question(question, ans => resolve(ans)));
}

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const gradlePath = path.join(__dirname, 'app', 'app', 'build.gradle.kts');
const serverDir = path.join(__dirname, 'server');
const adminDir = path.join(__dirname, 'admin-console');

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

    const nextCode = currentCode + 1;
    // 正则定位末尾数字自增，兼容 1.0.6 和 1.0.6-beta1
    const nextName = currentName.replace(/(\d+)(?!.*\d)/, (m) => String(parseInt(m, 10) + 1));

    rl.question(`请输入新的 versionCode [回车默认: ${nextCode}]: `, (codeAns) => {
        let newCode = codeAns.trim() ? parseInt(codeAns.trim(), 10) : nextCode;

        rl.question(`请输入新的 versionName [回车默认: "${nextName}"]: `, (nameAns) => {
            const newName = nameAns.trim() ? nameAns.trim() : nextName;

            rl.question(`请输入本次版本更新的说明日志 (update_log): `, (logAns) => {
                const updateLog = logAns.trim() ? logAns.trim() : "常规性能优化与问题修复";

                console.log(`\n准备执行以下【一键全流程发布流水线】:`);
                console.log(`1. 自动翻译升级日志（繁体 / 英文 / 日文 / 韩文）`);
                console.log(`2. 部署 server（Workers 后台接口）最新代码`);
                console.log(`3. 构建并部署 admin-console（管理后台）最新代码`);
                console.log(`4. 调用管理后台 API 写入新版本（自动发布 + 多语言）`);
                console.log(`5. 调用管理后台 API 发布多语言公告`);
                console.log(`6. 全量提交并 Push 代码到 GitHub（触发 CI 打包 APK 上传 R2）`);
                console.log(`7. 轮询等待 CI 打包完成后，清空 KV 边缘缓存`);

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
                    let isPushed = false;

                    // ─── Git 前置安全校验（仅校验分支，pull --rebase 延后到 commit 之后以避免工作区脏冲突） ───
                    console.log("➔ 正在校验 Git 工作区...");
                    try {
                        const currentBranch = execSync('git rev-parse --abbrev-ref HEAD', { stdio: 'pipe' }).toString().trim();
                        if (currentBranch !== 'main') {
                            console.error(`\n❌ 当前在 [${currentBranch}] 分支，发版必须在 [main] 分支！`);
                            rl.close(); process.exit(1);
                        }
                        console.log("   ✓ 分支检查通过（当前 main）");
                    } catch (gitErr) {
                        console.error(`\n❌ Git 前置检查失败: ${gitErr.message}`);
                        console.error("   请先切换回 main 分支后再试。");
                        rl.close(); process.exit(1);
                    }

                    try {
                        // ─── 第一步：网络翻译 (重试 3 次，仍失败则中止整个发布流程) ───
                        // 注意：翻译失败不再降级，而是直接抛出，交由外层 catch 中止流水线，
                        // 避免带着未翻译的日志发布到线上。
                        console.log("\n➔ [1/7] 正在连接 Google Translate 自动翻译版本日志...");
                        const versionTitle = `v${newName} 版本更新 / Version Update`;

                        const safeTranslate = async (text, targetLang, actionName) => {
                            try {
                                return await withRetry(() => translateText(text, targetLang), 3, 1000, actionName);
                            } catch (err) {
                                throw new Error(`❌ ${actionName} 翻译失败（已重试 3 次），发布流程中止：${err.message}`);
                            }
                        };

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

                        // ─── 第二步：部署 server（Workers 后台接口）最新代码 ───
                        // 必须在写版本/公告 API 之前部署，确保后端逻辑（createAppVersion 等）为最新。
                        console.log("\n➔ [2/7] 正在部署 server（Workers 后台接口）...");
                        await withRetry(() => { deployWorker(); return true; }, 2, 3000, "部署 Worker");
                        console.log("   ✔ server 部署成功");

                        // ─── 第三步：构建并部署 admin-console（管理后台）最新代码 ───
                        console.log("\n➔ [3/7] 正在构建并部署 admin-console（管理后台）...");
                        await withRetry(() => { deployPages(); return true; }, 2, 3000, "部署 Pages");
                        console.log("   ✔ admin-console 部署成功");

                        // ─── 管理后台登录（版本发布与公告两步共用） ───
                        const token = await withRetry(() => adminLogin(), 3, 2000, "管理后台登录");
                        console.log("   ✔ 管理后台登录成功，已获取访问令牌");

                        // ─── 第四步：调用管理后台 API 写入新版本 ───
                        // 由后台 createAppVersion 统一处理 status=1 自动发布、release_time、update_log 5 语言 i18n。
                        console.log("\n➔ [4/7] 正在调用管理后台 API 写入新版本（自动发布 + 多语言）...");
                        const apkUrl = `${R2_PUBLIC_BASE}/sheeps_${newName}.apk`;
                        const now = Date.now();

                        // ─── 发版前先查询 D1 当前最大 version_code（权威基准，避免盲插主键冲突） ───
                        let dbMaxCode = 0;
                        try {
                            const maxOut = execSync(
                                `npx wrangler d1 execute my-app-db --remote --command="SELECT MAX(version_code) AS m FROM app_version"`,
                                { cwd: serverDir, stdio: 'pipe', timeout: 30000 }
                            ).toString();
                            const m = maxOut.match(/"m"\s*:\s*(\d+)/);
                            if (m) {
                                dbMaxCode = parseInt(m[1], 10);
                            } else {
                                dbMaxCode = 0; // 表为空或值为 null
                            }
                        } catch (dbErr) {
                            dbMaxCode = 0;
                            console.warn(`   ⚠️ [容错] 查询 D1 当前最大 version_code 失败，将以 gradle 推断值继续: ${dbErr.message}`);
                        }

                        // 以 D1 真实最大值 +1 与 gradle 推断值取较大者，作为本次实际写入的 version_code
                        const effectiveNewCode = Math.max(dbMaxCode + 1, newCode);
                        if (effectiveNewCode !== newCode) {
                            console.warn(`⚠️ D1 当前最大 version_code=${dbMaxCode}，为避免主键冲突，本次 version_code 将使用 ${effectiveNewCode}（gradle 推断值为 ${newCode}）`);
                        }
                        newCode = effectiveNewCode;

                        // 版本请求体：透传列 + 多语言后缀键（后端 createAppVersion 自动处理 status/release_time/i18n）
                        const versionBody = {
                            version_code: newCode,
                            version_name: newName,
                            apk_url: apkUrl,
                            update_log: updateLog,         // 中文（基列）
                            update_log_en: contentEn,
                            update_log_tw: contentTw,
                            update_log_ja: contentJa,
                            update_log_ko: contentKo,
                            status: 1,                     // 已发布：后端自动写 release_time = now
                            is_force_update: 0,
                            created_at: now,
                        };
                        await withRetry(() => createAppVersionViaApi(token, versionBody), 3, 2000, "版本 API 写入 D1");
                        console.log("   ✔ 新版本已通过管理后台 API 写入 D1（status=1 已发布，多语言已落库）");

                        // ─── 第五步：调用管理后台 API 发布多语言公告 ───
                        // 公告仅写入 D1（真理来源）；不再预热 KV，改由第七步统一清空 KV 让客户端回源。
                        console.log("\n➔ [5/7] 正在调用管理后台 API 发布多语言公告...");
                        const noticeBody = {
                            title: versionTitle,
                            content: updateLog,
                            type: 'update',
                            created_at: now,
                            title_tw: titleTw, title_en: titleEn, title_ja: titleJa, title_ko: titleKo,
                            content_tw: contentTw, content_en: contentEn, content_ja: contentJa, content_ko: contentKo,
                        };
                        await withRetry(() => createNoticeViaApi(token, noticeBody), 3, 2000, "公告 API 写入 D1");
                        console.log("   ✔ 公告已通过管理后台 API 写入 D1");

                        // ─── 第六步：全量提交并 Push 代码，触发 CI 打包 APK ───
                        console.log("\n➔ [6/7] 正在修改版本文件并全量提交、推送代码（触发 CI 打包）...");
                        const updatedGradle = originalGradleContent
                            .replace(/versionCode\s*=\s*\d+/, `versionCode = ${newCode}`)
                            .replace(/versionName\s*=\s*"[^"]+"/, `versionName = "${newName}"`);
                        fs.writeFileSync(gradlePath, updatedGradle, 'utf8');
                        isGradleUpdated = true;

                        // 全量提交（.gitignore 已排除 .workbuddy / dist / build 等，git add -A 安全）
                        execSync('git add -A', { cwd: __dirname, stdio: 'inherit' });
                        try {
                            execSync(`git commit -m "chore(release): bump version to v${newName} & deploy latest code"`, { cwd: __dirname, stdio: 'inherit' });
                        } catch {
                            console.log("提示: 没有可提交的改动（工作区可能已干净）...");
                        }
                        // commit 之后工作区已干净，再同步远程避免 non-fast-forward
                        execSync('git pull origin main --rebase', { cwd: __dirname, stdio: 'inherit', timeout: 60000 });
                        execSync('git push origin main', { cwd: __dirname, stdio: 'inherit' });
                        isPushed = true;
                        const headSha = execSync('git rev-parse HEAD', { cwd: __dirname, stdio: 'pipe' }).toString().trim();
                        console.log(`   ✔ 代码已全量推送（commit ${headSha.slice(0, 8)}），CI 打包已触发`);

                        // ─── 第七步：等待 CI 打包完成，然后清空 KV 缓存 ───
                        console.log("\n➔ [7/7] 正在等待 GitHub Actions 打包 APK 并上传 R2...");
                        const actionsUrl = `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/actions`;
                        let ciOk = false;
                        if (GH_TOKEN) {
                            try {
                                await waitForCiRun(headSha);
                                ciOk = true;
                            } catch (ciErr) {
                                console.warn(`   ⚠️ 自动轮询 CI 失败: ${ciErr.message}`);
                                console.log(`   请手动查看: ${actionsUrl}`);
                                const ans = await ask(`   若 CI 已成功打包并上传 R2，输入 y 继续清空 KV（其他键跳过清理）: `);
                                ciOk = (ans.trim().toLowerCase() === 'y' || ans.trim().toLowerCase() === 'yes');
                            }
                        } else {
                            console.log(`   ℹ️ 未检测到 GitHub token（GH_TOKEN / GITHUB_TOKEN / MY_PAT），无法自动轮询。`);
                            console.log(`   请手动查看打包进度: ${actionsUrl}`);
                            const ans = await ask(`   待 CI 成功打包并上传 R2 后，输入 y 继续清空 KV: `);
                            ciOk = (ans.trim().toLowerCase() === 'y' || ans.trim().toLowerCase() === 'yes');
                        }

                        if (ciOk) {
                            console.log("➔ 正在清空 KV 边缘缓存...");
                            try {
                                clearKvCache();
                            } catch (kvErr) {
                                console.warn(`   ⚠️ 清空 KV 失败（不影响已发布数据，可稍后手动清理）: ${kvErr.message}`);
                            }
                        } else {
                            console.log("   ⏭️ 已跳过清空 KV（缓存将在 TTL 到期后自动失效）。");
                        }

                        console.log(`\n🎉🎉 一键全流程发布结束！`);
                        console.log(`👉 版本号: v${newName} (${newCode})`);
                        console.log(`👉 后台接口/管理后台: 已部署最新代码`);
                        console.log(`👉 R2 APK 地址: ${apkUrl}`);
                        console.log(`👉 KV 缓存: ${ciOk ? '已清空，客户端将读取最新数据' : '未清空（TTL 兜底）'}\n`);

                    } catch (err) {
                        console.error(`\n❌ 发版流水线中发生异常中止: ${err.message}`);

                        if (isPushed) {
                            // 代码已部署且已推送：不回滚版本文件（版本已发布）。仅提示后续手动收尾。
                            console.log("ℹ️ 代码已部署并推送，版本已发布；仅 CI 等待/清 KV 阶段未完成。");
                            console.log("   可稍后到 GitHub Actions 确认打包结果，并在需要时手动清空 KV 缓存。");
                        } else if (isGradleUpdated) {
                            console.log("🛡️ 正在回滚本地 build.gradle.kts 至修改前状态...");
                            fs.writeFileSync(gradlePath, originalGradleContent, 'utf8');
                            console.log("   ✔ 本地文件已恢复，请检查后重新启动 release！");
                        } else {
                            console.log("🛡️ 本地代码未做任何变更，无需回滚。");
                        }
                    } finally {
                        rl.close();
                    }
                });
            });
        });
    });
}

run();
