const { execSync } = require('child_process');
const readline = require('readline');
const fs = require('fs');
const path = require('path');

function askQuestion(query) {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout,
    });
    return new Promise(resolve => rl.question(query, ans => {
        rl.close();
        resolve(ans);
    }));
}

async function main() {
    console.log('==============================================');
    console.log('       秘境消消乐 - 后台接口与数据库部署脚本');
    console.log('==============================================\n');

    try {
        // 1. 检查并发布 D1 数据库架构
        console.log('[1/3] 正在检查并准备发布 SQL 数据库架构...');
        const schemaPath = path.join(__dirname, 'server', 'schema.sql');
        let shouldExecuteSql = true;
        
        if (fs.existsSync(schemaPath)) {
            const sqlContent = fs.readFileSync(schemaPath, 'utf8');
            if (/drop\s+table/i.test(sqlContent)) {
                console.log('\x1b[31m%s\x1b[0m', '\n=============================================================');
                console.log('\x1b[31m%s\x1b[0m', ' ⚠️  警告：检测到 schema.sql 中包含 DROP TABLE 语句！');
                console.log('\x1b[31m%s\x1b[0m', ' 执行该 SQL 脚本将会彻底【清空】数据库中已有的所有表和数据！');
                console.log('\x1b[31m%s\x1b[0m', '=============================================================\n');
                
                const confirm = await askQuestion('请输入 "RESET" 确认重置数据库，输入其他任意键跳过数据库修改: ');
                if (confirm.trim() !== 'RESET') {
                    shouldExecuteSql = false;
                    console.log('\n[提示] 已主动跳过 SQL 数据库架构执行，未修改任何数据库数据。\n');
                } else {
                    console.log('\n[提示] 已确认重置，正在发布 SQL 数据库架构到 Cloudflare D1...');
                }
            }
        }
        
        if (shouldExecuteSql) {
            execSync('npx wrangler d1 execute my-app-db --remote --file=./schema.sql --yes', {
                cwd: path.join(__dirname, 'server'),
                stdio: 'inherit'
            });
            console.log('\n[成功] SQL 数据库架构发布完成。\n');
        }

        // 2. 部署 Worker 后台
        console.log('[2/3] 正在部署后台 API 接口到 Cloudflare Workers...');
        execSync('npx wrangler deploy', {
            cwd: path.join(__dirname, 'server'),
            stdio: 'inherit'
        });
        console.log('\n[成功] 后台 API 接口部署完成。\n');

        // 3. 超级管理员种子数据配置
        console.log('[3/3] 是否需要初始化/更新超级管理员（super）账号？');
        const choose = await askQuestion('请输入 (y 表示是, 其他键表示跳过): ');
        if (choose.trim().toLowerCase() === 'y') {
            const phone = await askQuestion('请输入管理员手机号: ');
            const password = await askQuestion('请输入管理员密码: ');

            console.log('正在执行超级管理员配置...');
            
            // 执行 scripts/seed-admin.mjs 获取 SQL
            const tempSqlFile = path.join(__dirname, 'server', 'temp_seed.sql');
            try {
                execSync('node scripts/seed-admin.mjs > temp_seed.sql', {
                    cwd: path.join(__dirname, 'server'),
                    env: {
                        ...process.env,
                        ADMIN_PHONE: phone,
                        ADMIN_PASSWORD: password,
                        SQL_ONLY: 'true'
                    }
                });
                
                // 执行生成的 SQL 文件
                execSync('npx wrangler d1 execute my-app-db --remote --file=temp_seed.sql --yes', {
                    cwd: path.join(__dirname, 'server'),
                    stdio: 'inherit'
                });
                console.log('\n[成功] 超级管理员账号配置完成！');
            } finally {
                if (fs.existsSync(tempSqlFile)) {
                    fs.unlinkSync(tempSqlFile);
                }
            }
        }

        console.log('\n==============================================');
        console.log('         部署完成！所有后台接口和数据库已就绪。');
        console.log('==============================================');

    } catch (err) {
        console.error('\n[错误] 部署过程中发生异常:', err.message);
        process.exit(1);
    }
}

main();
