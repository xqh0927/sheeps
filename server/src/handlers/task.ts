import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';

export async function handleTaskRoutes(request: Request, env: Env, path: string, lang: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/task/daily' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';

        const [allTasks, userTasks] = await env.DB.batch([
            env.DB.prepare(`SELECT id, ${nameCol} as name, ${descCol} as description, target_count, points_reward FROM task`),
            env.DB.prepare('SELECT task_id, progress, is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_date = ?').bind(authUser.userId, chinaToday)
        ]);

        const taskList = allTasks.results as any[];
        const userTaskList = userTasks.results as any[];
        const inserts = [], list = [];

        for (const task of taskList) {
            let ut = userTaskList.find(u => u.task_id === task.id);
            if (!ut) {
                inserts.push(env.DB.prepare('INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, 0, 0, 0)').bind(authUser.userId, task.id, chinaToday));
                ut = { task_id: task.id, progress: 0, is_completed: 0, is_rewarded: 0 };
            }
            list.push({ task_id: task.id, name: task.name, description: task.description, progress: ut.progress, target_count: task.target_count, is_completed: ut.is_completed === 1, is_rewarded: ut.is_rewarded === 1, points_reward: task.points_reward });
        }

        if (inserts.length > 0) await env.DB.batch(inserts);
        return new Response(JSON.stringify(list), { headers: corsHeaders });
    }

    if (path === '/api/task/claim' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { task_id: string } = await request.json();
        if (!body.task_id) return new Response(JSON.stringify({ error: 'Missing task ID' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [userTaskResult, taskResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('SELECT points_reward FROM task WHERE id = ?').bind(body.task_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userTask = userTaskResult.results[0] as any;
        if (!userTask || userTask.is_completed === 0 || userTask.is_rewarded === 1) return new Response(JSON.stringify({ error: 'Task not completed or already rewarded' }), { status: 400, headers: corsHeaders });

        const reward = (taskResult.results[0] as any)?.points_reward || 10;
        const remainingPoints = ((userResult.results[0] as any)?.points || 0) + reward;

        await env.DB.batch([
            env.DB.prepare('UPDATE user_task SET is_rewarded = 1 WHERE user_id = ? AND task_id = ? AND task_date = ?').bind(authUser.userId, body.task_id, chinaToday),
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'IN', reward, `DAILY_TASK_${body.task_id}`, remainingPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, current_points: remainingPoints }), { headers: corsHeaders });
    }

    return null;
}