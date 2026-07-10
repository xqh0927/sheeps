/**
 * rollback.js —— 一键回滚发布（数据层安全逆向）
 *
 * 与 release.js 的"写版本 / 发公告 / 清 KV"相对应的回滚：
 *   1. 将最新已发布（status=1）的 app_version 改回 status=0，使其退出 /api/app/check-update 判定；
 *      客户端下次检测将回落到上一个 status=1 的版本（若无可回落则不再提示更新）。
 *   2. 撤回本次发布对应的版本更新公告（按版本号标题匹配）。
 *   3. 清空 KV 边缘缓存（update_check_ / notices_ / config_），让客户端立即回源读到回滚后的状态。
 *
 * 默认只做「数据层」回滚（安全、可逆、即时生效），不动 git、不动已上传的 R2 APK。
 * 如需把后台接口 / 管理后台的代码也回滚到上一部署，显式加 --deploy（会逐项确认，谨慎操作）。
 *
 * 运行：
 *   node rollback.js              # 交互确认后回滚
 *   node rollback.js --yes        # 跳过确认，直接回滚（CI / 脚本调用）
 *   node rollback.js --deploy     # 数据层回滚 + 代码部署回滚（每项单独确认）
 *   GH_TOKEN=xxx node rollback.js # 若 --deploy 需要 GitHub/Pages 操作（通常不需要）
 *
 * 凭据通过环境变量注入（ADMIN_PHONE / ADMIN_PASSWORD），请勿将真实密码写入本文件后提交。
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');

// ─── 代理（与 release.js 一致，保障 wrangler 子进程走代理） ───
const PROXY_URL = process.env.HTTPS_PROXY || process.env.HTTP_PROXY || 'http://127.0.0.1:10808';
if (PROXY_URL) {
    process.env.HTTPS_PROXY = PROXY_URL;
    process.env.HTTP_PROXY = PROXY_URL;
}

// ─── 管理后台 HTTP API 配置 ───
const ADMIN_API_BASE = process.env.ADMIN_API_BASE || 'https://api.xqh.cc.cd';
const ADMIN_PHONE = process.env.ADMIN_PHONE || '13800000000';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';

// ─── KV 配置（与 release.js 完全一致） ───
const KV_NS_ID = '784104ac67eb4f3c83a92e9dcc91b673'; // SHEEPS_CACHE 命名空间 id
const KV_CLEAR_PREFIXES = ['update_check_', 'notices_', 'config_'];

const serverDir = path.join(__dirname, 'server');
const adminConsoleDir = path.join(__dirname, 'admin-console');

// ─── 命令行参数 ───
const ARGS = process.argv.slice(2);
const SKIP_CONFIRM = ARGS.includes('--yes') || ARGS.includes('-y');
const DO_DEPLOY_ROLLBACK = ARGS.includes('--deploy');

// ─── 工具函数 ───
async function withRetry(fn, retries = 3, delay = 2000, actionName = '任务') {
    for (let i = 1; i <= retries; i++) {
        try {
            return await fn();
        } catch (err) {
            console.warn(`⚠️ [${actionName}] 第 ${i}/${retries} 次失败: ${err.message || err}`);
            if (i === retries) throw new Error(`[${actionName}] 失败次数已达上限，终止执行！`);
            await new Promise(r => setTimeout(r, delay * i));
        }
    }
}

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
function ask(question) {
    return new Promise(resolve => rl.question(question, ans => resolve((ans || '').trim().toLowerCase())));
}

// ─── 后台鉴权与 API 封装 ───
async function adminLogin() {
    const resp = await fetch(`${ADMIN_API_BASE}/api/admin/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: ADMIN_PHONE, password: ADMIN_PASSWORD }),
        signal: AbortSignal.timeout(15000),
    });
    const text = await resp.text();
    if (!resp.ok) throw new Error(`登录接口返回 HTTP ${resp.status}: ${text}`);
    let json;
    try { json = JSON.parse(text); } catch (e) { throw new Error(`登录响应非 JSON: ${text}`); }
    const token = json.token || (json.data && json.data.token);
    if (!token) throw new Error(`登录响应未包含 token: ${text}`);
    return token;
}

async function apiReq(method, apiPath, token, body) {
    const resp = await fetch(`${ADMIN_API_BASE}${apiPath}`, {
        method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: AbortSignal.timeout(15000),
    });
    const text = await resp.text();
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch (_) { /* 允许非 JSON 响应 */ }
    if (!resp.ok) {
        throw new Error(`HTTP ${resp.status}: ${text}`);
    }
    return json;
}

// ─── 业务：版本 / 公告 / KV ───
async function listAppVersions(token) {
    const json = await apiReq('GET', '/api/admin/app-versions?pageSize=100', token);
    const list = (json && (json.list || json.data)) || [];
    return list.map(v => ({
        version_code: Number(v.version_code),
        version_name: v.version_name,
        status: Number(v.status),
        release_time: v.release_time,
        apk_url: v.apk_url,
    }));
}

async function setVersionStatus(token, versionCode, status) {
    return apiReq('PUT', `/api/admin/app-versions/${versionCode}`, token, { status });
}

async function listNotices(token) {
    const json = await apiReq('GET', '/api/admin/notices?pageSize=50', token);
    const list = (json && (json.list || json.data)) || [];
    return list.map(n => ({ id: n.id, title: n.title, type: n.type, created_at: n.created_at }));
}

async function deleteNotice(token, id) {
    return apiReq('DELETE', `/api/admin/notices/${id}`, token);
}

function clearKvCache() {
    let names = [];
    const listOut = execSync(
        `npx wrangler kv key list --namespace-id ${KV_NS_ID} --remote`,
        { cwd: serverDir, stdio: ['ignore', 'pipe', 'pipe'], timeout: 30000 }
    ).toString();
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

// ─── 可选：代码部署回滚 ───
async function deployRollback() {
    console.log('\n➔ [部署回滚] 将把后台接口与管理后台代码回滚到上一部署（需逐项确认）');

    // 1) 后台接口（Cloudflare Workers）
    if (DO_DEPLOY_ROLLBACK) {
        const ans = SKIP_CONFIRM ? 'y' : await ask('   回滚后台接口(Workers)到上一部署？[y/N] ');
        if (ans === 'y' || ans === 'yes') {
            try {
                console.log('   ⏳ 正在执行 wrangler rollback（server）...');
                execSync('npx wrangler rollback --remote', { cwd: serverDir, stdio: 'inherit', timeout: 120000 });
                console.log('   ✔ 后台接口已回滚到上一部署');
            } catch (e) {
                console.warn(`   ⚠️ 后台接口回滚失败或需手动确认：${e.message}`);
                console.warn('      手动命令: cd server && npx wrangler rollback --remote');
            }
        } else {
            console.log('   ⊘ 跳过后台接口回滚');
        }

        // 2) 管理后台（Cloudflare Pages）—— 需指定上一部署 id，尽力解析
        const ans2 = SKIP_CONFIRM ? 'y' : await ask('   回滚管理后台(Pages)到上一部署？[y/N] ');
        if (ans2 === 'y' || ans2 === 'yes') {
            try {
                console.log('   ⏳ 列出管理后台部署记录...');
                const out = execSync(`npx wrangler pages deployment list --project-name miadmin-console`, {
                    cwd: __dirname, stdio: ['ignore', 'pipe', 'pipe'], timeout: 60000,
                }).toString();
                // 提取部署 id（wrangler 输出中的 32 位十六进制串）
                const ids = [...out.matchAll(/\b([0-9a-f]{32})\b/g)].map(m => m[1]);
                if (ids.length >= 2) {
                    const prevId = ids[1]; // ids[0] 为当前，ids[1] 为上一部署
                    console.log(`   ⏳ 回滚到上一部署: ${prevId}`);
                    execSync(`npx wrangler pages deployment rollback ${prevId} --project-name miadmin-console`, {
                        cwd: __dirname, stdio: 'inherit', timeout: 120000,
                    });
                    console.log('   ✔ 管理后台已回滚到上一部署');
                } else {
                    console.warn('   ⚠️ 无法从部署列表解析出上一部署 id，请手动执行：');
                    console.warn(`       npx wrangler pages deployment list --project-name miadmin-console`);
                    console.warn(`       npx wrangler pages deployment rollback <上一部署id> --project-name miadmin-console`);
                }
            } catch (e) {
                console.warn(`   ⚠️ 管理后台回滚失败：${e.message}`);
            }
        } else {
            console.log('   ⊘ 跳过管理后台回滚');
        }
    }
}

// ─── 主流程 ───
async function run() {
    console.log('═══════════════════════════════════════════════');
    console.log('  sheeps 发布回滚工具');
    console.log('═══════════════════════════════════════════════\n');

    const token = await withRetry(() => adminLogin(), 3, 2000, '管理后台登录');
    console.log('✔ 管理后台登录成功\n');

    // 1) 找到最新已发布版本（status=1，version_code 最大）
    const versions = await listAppVersions(token);
    const published = versions.filter(v => v.status === 1).sort((a, b) => b.version_code - a.version_code);
    if (published.length === 0) {
        console.log('ℹ️ 当前没有 status=1 的已发布版本，无需回滚。');
        return;
    }
    const target = published[0];
    const fallback = published[1]; // 回滚后客户端将回落到的版本（若有）
    console.log(`🎯 待回滚版本: v${target.version_name} (version_code=${target.version_code}, status=1)`);
    console.log(`   回滚后客户端将回落到: ${fallback ? `v${fallback.version_name} (code=${fallback.version_code})` : '（无更早的已发布版本，将不再提示更新）'}`);

    if (!SKIP_CONFIRM) {
        const ans = await ask('确认将上述版本撤回（status→0）并清理公告与 KV 缓存？[y/N] ');
        if (ans !== 'y' && ans !== 'yes') {
            console.log('⊘ 已取消回滚。');
            return;
        }
    }

    // 2) 撤回版本（status→0）
    console.log('\n➔ [1/3] 撤回已发布版本（status → 0）...');
    await withRetry(() => setVersionStatus(token, target.version_code, 0), 3, 2000, '撤回版本状态');
    console.log(`   ✔ v${target.version_name} 已设为 status=0，退出更新检测`);

    // 3) 撤回版本更新公告（按标题匹配版本号）
    console.log('\n➔ [2/3] 撤回本次发布的版本更新公告...');
    const notices = await listNotices(token);
    const cand = notices
        .filter(n => n.title && n.title.includes(target.version_name))
        .sort((a, b) => Number(b.id) - Number(a.id));
    if (cand.length > 0) {
        const notice = cand[0];
        console.log(`   匹配到公告: #${notice.id} 「${notice.title}」`);
        const delAns = SKIP_CONFIRM ? 'y' : await ask('   删除该公告？[y/N] ');
        if (delAns === 'y' || delAns === 'yes') {
            await withRetry(() => deleteNotice(token, notice.id), 3, 2000, '删除公告');
            console.log(`   ✔ 已删除公告 #${notice.id}`);
        } else {
            console.log('   ⊘ 跳过公告删除');
        }
    } else {
        console.log('   ℹ️ 未找到标题含该版本号的公告，跳过（如需手动删除，请到管理后台操作）');
        if (!SKIP_CONFIRM && notices.length > 0) {
            console.log('   近期公告:');
            notices.slice(0, 5).forEach(n => console.log(`     #${n.id} 「${n.title}」`));
        }
    }

    // 4) 清空 KV 缓存，让客户端立即回源
    console.log('\n➔ [3/3] 清空 KV 边缘缓存...');
    clearKvCache();

    // 5) 可选：代码部署回滚
    if (DO_DEPLOY_ROLLBACK) {
        await deployRollback();
    }

    console.log('\n═══════════════════════════════════════════════');
    console.log('✔ 回滚完成。客户端将在缓存清空后（最长 5 分钟内）回源读到回滚结果。');
    console.log('  说明：已升级到该版本的用户不会被强制降级；回滚仅停止"继续推送此版本更新"。');
    console.log('═══════════════════════════════════════════════');
}

run().catch((err) => {
    console.error(`\n❌ 回滚流程异常中止: ${err.message || err}`);
    process.exit(1);
}).finally(() => {
    rl.close();
});
