import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';

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

    // 1. 查询每日任务列表及进度接口
    if (path === '/api/task/daily' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';

        // 批量查询系统全局任务模板和该用户当天的任务进度
        const [allTasks, userTasks] = await env.DB.batch([
            env.DB.prepare(`SELECT id, ${nameCol} as name, ${descCol} as description, target_count, points_reward FROM task`),
            env.DB.prepare('SELECT task_id, progress, is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_date = ?').bind(authUser.userId, chinaToday)
        ]);

        const taskList = allTasks.results as any[];
        const userTaskList = userTasks.results as any[];
        const inserts = [], list = [];

        // 任务初始化对齐逻辑：遍历系统所有任务，如果用户今天未生成该任务进度条目，则进行懒加载插入
        for (const task of taskList) {
            let ut = userTaskList.find(u => u.task_id === task.id);
            if (!ut) {
                inserts.push(env.DB.prepare('INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, 0, 0, 0)').bind(authUser.userId, task.id, chinaToday));
                ut = { task_id: task.id, progress: 0, is_completed: 0, is_rewarded: 0 };
            }
            list.push({ task_id: task.id, name: task.name, description: task.description, progress: ut.progress, target_count: task.target_count, is_completed: ut.is_completed === 1, is_rewarded: ut.is_rewarded === 1, points_reward: task.points_reward });
        }

        // 批量将缺失的进度条目插入 D1 数据库
        if (inserts.length > 0) await env.DB.batch(inserts);
        return new Response(JSON.stringify(list), { headers: corsHeaders });
    }

    // 2. 领取任务积分奖励接口
    if (path === '/api/task/claim' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { task_id: string } = await request.json();
        if (!body.task_id) return new Response(JSON.stringify({ error: 'Missing task ID' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        // 批量查询当前任务进度、奖励积分数、以及用户当前的总积分数
        const [userTaskResult, taskResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('SELECT points_reward FROM task WHERE id = ?').bind(body.task_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userTask = userTaskResult.results[0] as any;
        // 校验：必须满足“已完成 (is_completed = 1)”且“未领取过 (is_rewarded = 0)”方可领取
        if (!userTask || userTask.is_completed === 0 || userTask.is_rewarded === 1) return new Response(JSON.stringify({ error: 'Task not completed or already rewarded' }), { status: 400, headers: corsHeaders });

        const reward = (taskResult.results[0] as any)?.points_reward || 10;
        const remainingPoints = ((userResult.results[0] as any)?.points || 0) + reward;

        // 执行原子事务：标记已领取、增加用户账户积分、写入积分明细流水
        await env.DB.batch([
            env.DB.prepare('UPDATE user_task SET is_rewarded = 1 WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'IN', reward, `DAILY_TASK_${body.task_id}`, remainingPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, current_points: remainingPoints }), { headers: corsHeaders });
    }

    return null;
}