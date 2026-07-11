import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';
import { getI18nBatch, resolveI18n } from '../i18n';

/**
 * 处理玩家每日任务相关的 HTTP 路由请求
 * 包含查询每日任务列表及其当前进度（若无对应日期的条目则自动初始化写入）、以及领取已通关/已完成任务的积分奖励。
 * 
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param lang 客户端语言标识，用于国际化展示任务名称和描述
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleTaskRoutes(request: Request, env: Env, path: string, lang: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * GET /api/task/daily — 查询每日任务列表及当前进度
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * Query 参数（通过请求头 Accept-Language 解析）:
     *   @param {string} [lang] — 语言标识
     *
     * 响应: [{ task_id, name, description, progress, target_count, is_completed, is_rewarded, points_reward }]
     */
    if (path === '/api/task/daily' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        // 业务上下文：以 UTC+8（东八区）计算「今日」日期，作为 user_task 每日进度行的 task_date 维度，保证每日任务按天重置
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];

        // DB：单 batch 查询 task 任务模板表 + user_task 当日进度表；方案 B 先取基列（zh 兜底），再经 i18n 解析多语言任务名/描述
        const [allTasks, userTasks] = await env.DB.batch([
            env.DB.prepare('SELECT id, name, description, target_count, points_reward FROM task'),
            env.DB.prepare('SELECT task_id, progress, is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_date = ?').bind(authUser.userId, chinaToday)
        ]);

        const taskList = allTasks.results as any[];
        const userTaskList = userTasks.results as any[];
        const i18nMap = await getI18nBatch(env, 'task', lang);
        const inserts = [], list = [];

        // 幂等初始化：遍历系统任务模板，若用户今日尚无对应 user_task 进度行则懒加载 INSERT（progress=0, is_completed=0, is_rewarded=0），保证每日任务自动就绪
        for (const task of taskList) {
            let ut = userTaskList.find(u => u.task_id === task.id);
            if (!ut) {
                inserts.push(env.DB.prepare('INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, 0, 0, 0)').bind(authUser.userId, task.id, chinaToday));
                ut = { task_id: task.id, progress: 0, is_completed: 0, is_rewarded: 0 };
            }
            list.push({
                task_id: task.id,
                name: resolveI18n(i18nMap, `task.${task.id}.name`, task.name),
                description: resolveI18n(i18nMap, `task.${task.id}.description`, task.description),
                progress: ut.progress,
                target_count: task.target_count,
                is_completed: ut.is_completed === 1,
                is_rewarded: ut.is_rewarded === 1,
                points_reward: task.points_reward
            });
        }

        // 事务边界：将当日缺失的用户任务进度条目一次性批量写入 user_task 表，保证初始化原子性
        if (inserts.length > 0) await env.DB.batch(inserts);

        // 修复：如果用户今天已签到但 SIGN_IN_ONCE 任务状态未同步（如老用户已签到但任务数据未更新），自动修复
        const signInTask = list.find(t => t.task_id === 'SIGN_IN_ONCE');
        if (signInTask && !signInTask.is_completed) {
            const signRecord = await env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday).first();
            if (signRecord) {
                await env.DB.prepare('INSERT OR REPLACE INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'SIGN_IN_ONCE', chinaToday, 1, 1, 1).run();
                signInTask.progress = 1;
                signInTask.is_completed = true;
                signInTask.is_rewarded = true;
            }
        }

        return new Response(JSON.stringify(list), { headers: corsHeaders });
    }

    /**
     * POST /api/task/claim — 领取任务积分奖励
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 请求体 (JSON):
     *   @param {string} task_id — 任务ID
     *
     * 响应: { success, current_points }
     */
    if (path === '/api/task/claim' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { task_id: string } = await request.json();
        // 参数校验：task_id 必填，缺失返回 400
        if (!body.task_id) return new Response(JSON.stringify({ error: 'Missing task ID' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        // DB：单 batch 读取 user_task 进度（是否完成/已领）、task 奖励积分、users 当前积分，作为领取前置校验
        const [userTaskResult, taskResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('SELECT points_reward FROM task WHERE id = ?').bind(body.task_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userTask = userTaskResult.results[0] as any;
        // 参数校验（幂等防护）：任务必须「已完成且尚未领取」，否则返回 400；重复领取会被 is_rewarded 拦截，避免重复发奖
        if (!userTask || userTask.is_completed === 0 || userTask.is_rewarded === 1) return new Response(JSON.stringify({ error: 'Task not completed or already rewarded' }), { status: 400, headers: corsHeaders });

        const reward = (taskResult.results[0] as any)?.points_reward || 10;
        const remainingPoints = ((userResult.results[0] as any)?.points || 0) + reward;

        // 事务边界：标记 user_task.is_rewarded=1、累加 users.points、写 point_record 流水（source=DAILY_TASK_<id>），三写合并单 batch 原子提交，确保发奖与记账一致
        await env.DB.batch([
            env.DB.prepare('UPDATE user_task SET is_rewarded = 1 WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'IN', reward, `DAILY_TASK_${body.task_id}`, remainingPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, current_points: remainingPoints }), { headers: corsHeaders });
    }

    return null;
}