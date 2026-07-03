const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');
const https = require('https'); // ⚠️ 修复：删除了重复的 require

// 引入代理库（如果没安装则降级为直连）
let HttpsProxyAgent;
try { HttpsProxyAgent = require('https-proxy-agent').HttpsProxyAgent; } catch { }

// 填写你本地的 HTTP/SOCKS 代理地址（已为你默认填好 10808）
const PROXY_URL = process.env.HTTPS_PROXY || process.env.HTTP_PROXY || 'http://127.0.0.1:10808';

/**
 * 带有指数退避的通用异步重试函数
 */
async function withRetry(fn, retries = 3, delay = 2000, actionName = "任务") {
    for (let i = 1; i <= retries; i++) {
        try {
            return await fn();
        } catch (err) {
            console.warn(`⚠️ [${actionName}] 第 ${i}/${retries} 次失败: ${err.message || err}`);
            if (i === retries) {
                throw new Error(`[${actionName}] 失败次数已达上限，终止执行！`);
            }
            await new Promise(res => setTimeout(res, delay * i));
        }
    }
}

/**
 * 使用 Google Translate 免费接口翻译文本 (修复了代理参数传入问题)
 */
function translateText(text, targetLang) {
    return new Promise((resolve, reject) => {
        const encoded = encodeURIComponent(text);
        const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${targetLang}&dt=t&q=${encoded}`;

        const options = {};
        if (HttpsProxyAgent && PROXY_URL) {
            options.agent = new HttpsProxyAgent(PROXY_URL);
        }

        // ⚠️ 修复：必须把 options 传进 https.get 作为第二个参数！
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

    const R2_PUBLIC_BASE = 'https://apk.xqh.cc.cd';
    const nextCode = currentCode + 1;
    const parts = currentName.split('.');
    if (parts.length >= 3) {
        parts[parts.length - 1] = parseInt(parts[parts.length - 1], 10) + 1;
    }
    const nextName = parts.join('.');

    rl.question(`请输入新的 versionCode [回车默认: ${nextCode}]: `, (codeAns) => {
        const newCode = codeAns.trim() ? parseInt(codeAns.trim(), 10) : nextCode;

        rl.question(`请输入新的 versionName [回车默认: "${nextName}"]: `, (nameAns) => {
            const newName = nameAns.trim() ? nameAns.trim() : nextName;

            rl.question(`请输入本次版本更新的说明日志 (update_log): `, (logAns) => {
                const updateLog = logAns.trim() ? logAns.trim() : "常规性能优化与问题修复";

                console.log(`\n准备执行以下【安全发布流水线】:`);
                console.log(`1. 自动翻译升级日志（已开启代理配置 + 异常自动降级）`);
                console.log(`2. 预写版本与公告数据至 Cloudflare D1 & KV（带重试与防脚本注入）`);
                console.log(`3. 确认数据库全部成功后，更新 gradle 文件并将全部本地代码 Push 触发编译`);

                rl.question(`\n确认开始发布？(y/n): `, async (confirm) => {
                    if (confirm.toLowerCase() !== 'y' && confirm.toLowerCase() !== 'yes') {
                        console.log("发布已取消。");
                        rl.close();
                        process.exit(0);
                    }

                    let isGradleUpdated = false;
                    const tempSqlPath = path.join(__dirname, 'temp_release_query.sql');
                    const tempKvPath = path.join(__dirname, 'temp_kv_payload.json');

                    try {
                        // ─── 第一步：网络翻译 (加入重试 + 降级兜底) ───
                        console.log("\n➔ [1/4] 正在连接 Google Translate 自动翻译版本日志...");
                        const versionTitle = `v${newName} 版本更新 / Version Update`;

                        // 安全翻译包装：如果3次通过代理还超时的，直接返回原文（中文），绝对不卡死发版！
                        const safeTranslate = async (text, targetLang, actionName) => {
                            try {
                                return await withRetry(() => translateText(text, targetLang), 3, 1000, actionName);
                            } catch (err) {
                                console.warn(`   ⚠️ [降级提示] ${actionName} 无法连接，将自动回退使用原始中文！`);
                                return text;
                            }
                        };

                        const [titleEn, titleJa, titleKo, contentEn, contentJa, contentKo] = await Promise.all([
                            safeTranslate(versionTitle, 'en', "翻译标题-英文"),
                            safeTranslate(versionTitle, 'ja', "翻译标题-日文"),
                            safeTranslate(versionTitle, 'ko', "翻译标题-韩文"),
                            safeTranslate(updateLog, 'en', "翻译日志-英文"),
                            safeTranslate(updateLog, 'ja', "翻译日志-日文"),
                            safeTranslate(updateLog, 'ko', "翻译日志-韩文")
                        ]);
                        console.log("   ✔ 多语言文本处理完毕");

                        // ─── 第二步：更新 D1 数据库 ───
                        console.log("➔ [2/4] 正在更新云端 D1 数据库...");
                        const apkUrl = `${R2_PUBLIC_BASE}/sheeps_${newName}.apk`;
                        const now = Date.now();
                        const escSql = (s) => s.replace(/'/g, "''");

                        const sqlContent = [
                            `INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES (${newCode}, '${newName}', '${apkUrl}', '${escSql(updateLog)}', 0, ${now});`,
                            `INSERT INTO notice (title, title_en, title_ja, title_ko, content, content_en, content_ja, content_ko, type, created_at) VALUES ('${escSql(versionTitle)}', '${escSql(titleEn)}', '${escSql(titleJa)}', '${escSql(titleKo)}', '${escSql(updateLog)}', '${escSql(contentEn)}', '${escSql(contentJa)}', '${escSql(contentKo)}', 'update', ${now});`
                        ].join('\n');

                        fs.writeFileSync(tempSqlPath, sqlContent, 'utf8');

                        await withRetry(() => {
                            return new Promise((res, rej) => {
                                try {
                                    execSync(`npx wrangler d1 execute my-app-db --remote --file="${tempSqlPath}"`, { cwd: serverDir, stdio: 'pipe' });
                                    res();
                                } catch (e) { rej(e); }
                            });
                        }, 3, 2000, "D1 数据库远程更新");

                        console.log("   ✔ D1 数据库更新成功！");

                        // ─── 第三步：更新 KV 边缘缓存 ───
                        console.log("➔ [3/4] 正在同步多语言 KV 缓存板...");
                        const kvNsId = '784104ac67eb4f3c83a92e9dcc91b673';
                        const noticesByLang = {
                            '': { title: versionTitle, content: updateLog },
                            'en': { title: titleEn, content: contentEn },
                            'ja': { title: titleJa, content: contentJa },
                            'ko': { title: titleKo, content: contentKo },
                        };

                        for (const [lang, item] of Object.entries(noticesByLang)) {
                            const cacheKey = `notices_${lang}`;
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
                        }
                        console.log("   ✔ 多语言 KV 边缘缓存全部同步完毕");

                        // ─── 第四步：全部网络交互成功后，修改本地 gradle 并执行 Git 操作 ───
                        console.log("➔ [4/4] 数据库与缓存已锁定，正在修改版本文件并 Push 代码...");
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