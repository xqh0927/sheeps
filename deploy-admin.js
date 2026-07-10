const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

async function main() {
    console.log('==============================================');
    console.log('         秘境消消乐 - 管理后台发布脚本');
    console.log('==============================================\n');

    const adminDir = path.join(__dirname, 'admin-console');

    try {
        // 1. 检查并安装依赖
        console.log('[1/3] 正在检查并安装管理后台依赖项 (npm install)...');
        execSync('npm install --legacy-peer-deps', {
            cwd: adminDir,
            stdio: 'inherit'
        });
        console.log('\n[成功] 依赖项安装完成。\n');

        // 2. 检测配置文件警告
        const hasEnv = fs.existsSync(path.join(adminDir, '.env'));
        const hasEnvProd = fs.existsSync(path.join(adminDir, '.env.production'));
        if (!hasEnv && !hasEnvProd) {
            console.log('[警告] 目录下未检测到 .env 或 .env.production 配置文件！');
            console.log('请确保网页端或本地已配置环境变量 VITE_API_BASE 指向正确的 Worker 后台域名。\n');
        }

        // 3. 构建管理后台静态产物
        console.log('[2/3] 正在打包/构建管理后台产物 (npm run build)...');
        execSync('npm run build', {
            cwd: adminDir,
            stdio: 'inherit'
        });
        console.log('\n[成功] 打包构建完成。\n');

        // 4. 发布到 Cloudflare Pages
        console.log('[3/3] 正在部署管理后台静态产物到 Cloudflare Pages...');
        execSync('npx wrangler pages deploy dist --project-name=miadmin-console', {
            cwd: adminDir,
            stdio: 'inherit'
        });
        console.log('\n==============================================');
        console.log('         部署完成！管理后台已成功发布。');
        console.log('==============================================');

    } catch (err) {
        console.error('\n[错误] 部署过程中发生异常:', err.message);
        process.exit(1);
    }
}

main();
