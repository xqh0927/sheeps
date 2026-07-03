const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');
const https = require('https');

/**
 * 使用 Google Translate 免费接口翻译文本
 * 目标语言映射：en=英语, ja=日语, ko=韩语
 */
function translateText(text, targetLang) {
    return new Promise((resolve, reject) => {
        const encoded = encodeURIComponent(text);
        const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${targetLang}&dt=t&q=${encoded}`;
        https.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    // Google Translate 返回 [[["trans1","orig1",...],["trans2",...],...]]
                    const result = JSON.parse(data);
                    const translated = result[0].map(seg => seg[0]).join('');
                    resolve(translated);
                } catch { resolve(text); } // 翻译失败回退原文
            });
        }).on('error', () => resolve(text)); // 网络失败回退原文
    });
}

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const gradlePath = path.join(__dirname, 'app', 'app', 'build.gradle.kts');

function run() {
    if (!fs.existsSync(gradlePath)) {
        console.error(`错误: 找不到 build.gradle.kts 文件，预期路径为: ${gradlePath}`);
        process.exit(1);
    }

    let gradleContent = fs.readFileSync(gradlePath, 'utf8');

    // 正则匹配获取当前的 versionCode 和 versionName
    const codeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);
    const nameMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);

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

    // ─── R2 APK 下载地址 ───
    const R2_PUBLIC_BASE = 'https://apk.xqh.cc.cd';

    const nextCode = currentCode + 1;
    // 自动推荐下一个 versionName
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

                console.log(`\n准备执行以下发布动作:`);
                console.log(`1. 更新 build.gradle.kts ➔ versionCode = ${newCode}, versionName = "${newName}"`);
                console.log(`2. 自动生成云端 D1 数据库 SQL 记录并执行插入`);
                console.log(`3. 自动 Git Commit 并 Push 提交到 GitHub 触发 CI 发布`);
                
                rl.question(`\n确认开始发布？(y/n): `, async (confirm) => {
                    if (confirm.toLowerCase() !== 'y' && confirm.toLowerCase() !== 'yes') {
                        console.log("发布已取消。");
                        rl.close();
                        process.exit(0);
                    }

                    try {
                        // 1. 修改 build.gradle.kts 文件
                        let updatedContent = gradleContent
                            .replace(/versionCode\s*=\s*\d+/, `versionCode = ${newCode}`)
                            .replace(/versionName\s*=\s*"[^"]+"/, `versionName = "${newName}"`);
                        fs.writeFileSync(gradlePath, updatedContent, 'utf8');
                        console.log("➔ build.gradle.kts 更新成功！");

                        // 2. 执行远程 D1 数据库插入
                        const apkUrl = `${R2_PUBLIC_BASE}/sheeps_${newName}.apk`;
                        const now = Date.now();
                        // 避免 SQL 语句因单引号报错
                        const esc = (s) => s.replace(/'/g, "''");
                        const escapedLog = esc(updateLog);
                        const versionTitle = `v${newName} 版本更新`;
                        
                        // ─── 自动翻译标题和更新日志到多语言 ───
                        console.log("➔ 正在翻译公告内容...");
                        const [titleEn, titleJa, titleKo, contentEn, contentJa, contentKo] = await Promise.all([
                            translateText(versionTitle, 'en'),
                            translateText(versionTitle, 'ja'),
                            translateText(versionTitle, 'ko'),
                            translateText(updateLog, 'en'),
                            translateText(updateLog, 'ja'),
                            translateText(updateLog, 'ko')
                        ]);
                        console.log(`   en: ${titleEn}`);
                        console.log(`   ja: ${titleJa}`);
                        console.log(`   ko: ${titleKo}`);
                        
                        // 同时写入 app_version 和 notice 公告表（含多语言列）
                        const sql = [
                            `INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES (${newCode}, '${newName}', '${apkUrl}', '${escapedLog}', 0, ${now});`,
                            `INSERT INTO notice (title, title_en, title_ja, title_ko, content, content_en, content_ja, content_ko, type, created_at) VALUES ('${esc(versionTitle)}', '${esc(titleEn)}', '${esc(titleJa)}', '${esc(titleKo)}', '${escapedLog}', '${esc(contentEn)}', '${esc(contentJa)}', '${esc(contentKo)}', 'update', ${now});`
                        ].join(' ');
                        
                        console.log("➔ 正在执行远程 D1 数据库更新...");
                        const serverDir = path.join(__dirname, 'server');
                        execSync(`npx wrangler d1 execute my-app-db --remote --command="${sql}"`, { cwd: serverDir, stdio: 'inherit' });
                        console.log("➔ D1 数据库记录写入成功！");
                        // 追加各语言 KV 缓存（即时生效）
                        const kvNsId = '784104ac67eb4f3c83a92e9dcc91b673';
                        const noticesByLang = {
                            '':    { title: versionTitle, content: updateLog },
                            'en':  { title: titleEn,      content: contentEn },
                            'ja':  { title: titleJa,      content: contentJa },
                            'ko':  { title: titleKo,      content: contentKo },
                        };
                        for (const [l, t] of Object.entries(noticesByLang)) {
                            const cacheKey = `notices_${l}`;
                            try {
                                const existing = execSync(`npx wrangler kv key get "${cacheKey}" --namespace-id ${kvNsId} --remote --text`, { cwd: serverDir, stdio: 'pipe', timeout: 10000 }).toString().trim();
                                if (existing && existing !== 'null' && existing !== 'undefined') {
                                    const list = JSON.parse(existing);
                                    list.unshift({ title: t.title, content: t.content, type: 'update', created_at: now });
                                    execSync(`npx wrangler kv key put "${cacheKey}" --namespace-id ${kvNsId} --remote --path=/dev/stdin`, {
                                        cwd: serverDir, stdio: 'pipe', input: JSON.stringify(list)
                                    });
                                }
                            } catch {}
                        }

                        // 3. 执行 Git 提交与 Push
                        console.log("➔ 正在提交代码到 Git 远程仓库...");
                        execSync('git add -A', { cwd: __dirname, stdio: 'inherit' });
                        execSync(`git commit -m "chore(release): bump version to v${newName} and update release info"`, { cwd: __dirname, stdio: 'inherit' });
                        execSync('git push origin main', { cwd: __dirname, stdio: 'inherit' });
                        console.log("➔ 代码已成功推送到远程 main 分支，正在触发 GitHub Actions 编译发布...");

                        console.log(`\n🎉 发布流程执行完毕！`);
                        console.log(`Tag 版本: v${newName}`);
                        console.log(`R2 APK 下载: ${apkUrl}`);
                        console.log(`GitHub Actions 将自动构建并上传 APK 到 R2\n`);

                    } catch (err) {
                        console.error("\n❌ 执行发布时发生错误:", err.message);
                    } finally {
                        rl.close();
                    }
                });
            });
        });
    });
}

run();
