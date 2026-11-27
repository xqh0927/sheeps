import { Env } from '../types';
import { generateJWT } from '../crypto';
import { getCorsHeaders } from '../helpers';

export function newPlayer(db: D1Database, id: string, phone: string, passwordHash: string | null) {
    const now = Date.now();
    return [
        db.prepare('INSERT INTO users (id, phone, username, role, points, password_hash, created_at) VALUES (?, ?, ?, ?, 0, ?, ?)')
            .bind(id, phone, '国风玩家_' + phone.slice(-4), 'user', passwordHash, now),
        db.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)').bind(id, now),
        ...['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE'].map(item =>
            db.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, 1)').bind(id, item))
    ];
}

// The run ID is stable across login. Keep both the leaderboard row and its receipt so
// that retrying an already acknowledged guest run cannot add another ranking or task win.
function transferGuest(db: D1Database, guest: string, target: string) {
    const move = (table: string) => db.prepare(`UPDATE OR IGNORE ${table} SET user_id=? WHERE user_id=?`).bind(target, guest);
    const remove = (table: string) => db.prepare(`DELETE FROM ${table} WHERE user_id=?`).bind(guest);
    return [
        db.prepare('UPDATE users SET points=points+COALESCE((SELECT points FROM users WHERE id=?),0) WHERE id=?').bind(guest, target),
        db.prepare(`INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) SELECT ?,level_id,unlocked_at FROM level_unlock WHERE user_id=?`).bind(target, guest),
        db.prepare(`INSERT INTO user_items (user_id, item_type, count) SELECT ?,item_type,count FROM user_items WHERE user_id=?
            ON CONFLICT(user_id,item_type) DO UPDATE SET count=CASE WHEN excluded.item_type GLOB 'SKIN_*'
                THEN MAX(user_items.count,excluded.count) ELSE user_items.count+excluded.count END`).bind(target, guest),
        // Receipts have a unique internal attempt ID. The public idempotency key is (user_id, run_id).
        db.prepare(`INSERT INTO stage_score_receipts(user_id,run_id,attempt_id)
            SELECT ?,run_id,lower(hex(randomblob(16))) FROM stage_score_receipts WHERE user_id=?
            ON CONFLICT(user_id,run_id) DO NOTHING`).bind(target, guest),
        db.prepare(`DELETE FROM leaderboard WHERE id IN (SELECT g.score_id FROM stage_run_scores g
            JOIN stage_run_scores a ON a.user_id=? AND a.run_id=g.run_id WHERE g.user_id=?)`).bind(target, guest),
        move('stage_run_scores'), move('stage_attempts'), move('leaderboard'),
        move('point_record'), move('exchange_record'), move('backup_save_log'),
        move('item_consumptions'), move('shop_exchange_receipts'),
        move('sign_record'),
        db.prepare(`INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded)
            SELECT ?,task_id,task_date,progress,is_completed,is_rewarded FROM user_task WHERE user_id=?
            ON CONFLICT(user_id,task_id,task_date) DO UPDATE SET
            progress=MAX(user_task.progress,excluded.progress), is_completed=MAX(user_task.is_completed,excluded.is_completed),
            is_rewarded=MAX(user_task.is_rewarded,excluded.is_rewarded)`).bind(target, guest),
        remove('sign_record'), remove('user_task'), remove('level_unlock'), remove('user_items'),
        db.prepare('DELETE FROM users WHERE id=? AND phone IS NULL').bind(guest)
    ];
}

export async function finishPlayerLogin(env: Env, id: string, guest: string | undefined, initial: D1PreparedStatement[] = []) {
    const db = env.DB;
    // A device identifier can only denote an unregistered player. All transfers use current
    // rows in the same transaction, including when two login requests consume the same guest.
    if (guest && /^uuid-[a-f0-9-]{36}$/i.test(guest) && guest !== id &&
        await db.prepare('SELECT 1 FROM users WHERE id=? AND phone IS NULL').bind(guest).first()) {
        initial.push(...transferGuest(db, guest, id));
    }
    const today = new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 10);
    const results = await db.batch<Record<string, string | number | null>>([...initial,
        db.prepare('SELECT id,phone,username,avatar_url AS avatar,points,(password_hash IS NOT NULL) AS has_password FROM users WHERE id=?').bind(id),
        db.prepare('SELECT level_id FROM level_unlock WHERE user_id=? ORDER BY level_id').bind(id),
        db.prepare('SELECT item_type,count FROM user_items WHERE user_id=?').bind(id),
        db.prepare(`SELECT sign_date,streak FROM sign_record WHERE user_id=? AND sign_date BETWEEN date(?,'-1 day') AND ? ORDER BY sign_date DESC LIMIT 1`).bind(id, today, today)
    ]);
    const [account, levels, items, signs] = results.slice(initial.length);
    const {has_password, ...user} = account.results[0];
    const claims = {userId: id, phone: user.phone};
    const [token, refreshToken] = await Promise.all([
        generateJWT({...claims, type: 'access', exp: Date.now() + 7200000}),
        generateJWT({...claims, type: 'refresh', exp: Date.now() + 30 * 86400000})
    ]);
    return Response.json({
        success: true, token, refreshToken, user, hasPassword: !!has_password,
        unlocked_levels: levels.results.map(row => row.level_id),
        items: items.results,
        today_signed: signs.results[0]?.sign_date === today,
        sign_streak: signs.results[0]?.streak ?? 0
    }, {headers: getCorsHeaders()});
}
