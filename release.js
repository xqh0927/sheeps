const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');

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
                
                rl.question(`\n确认开始发布？(y/n): `, (confirm) => {
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
                        const apkUrl = `https://github.com/xqh0927/sheeps-releases/releases/download/v${newName}/sheeps_${newName}.apk`;
                        const now = Date.now();
                        // 避免 SQL 语句因单引号报错
                        const escapedLog = updateLog.replace(/'/g, "''");
                        const sql = `INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES (${newCode}, '${newName}', '${apkUrl}', '${escapedLog}', 0, ${now});`;
                        
                        console.log("➔ 正在执行远程 D1 数据库更新...");
                        // 运行 wrangler
                        const serverDir = path.join(__dirname, 'server');
                        execSync(`npx wrangler d1 execute my-app-db --remote --command="${sql}"`, { cwd: serverDir, stdio: 'inherit' });
                        console.log("➔ D1 数据库记录写入成功！");

                        // 3. 提示 Git 提交操作
                        console.log("\n➔ 自动代码提交已禁用。请在本地核对并测试无误后，手动执行以下命令推送触发 GitHub 自动构建：");
                        console.log(`   git add .`);
                        console.log(`   git commit -m "chore(release): bump version to v${newName}"`);
                        console.log(`   git push origin main`);

                        console.log(`\n🎉 发布流程执行完毕！`);
                        console.log(`Tag 版本: v${newName}`);
                        console.log(`新 APK 下载地址将在自动打包后生效: ${apkUrl}\n`);

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
