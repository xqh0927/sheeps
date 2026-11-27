import { Env } from '../types';

export function avatarPrefix(userId: string): string {
  return `avatars/${userId}_`;
}

export function ownsAvatar(userId: string, object: R2Object): boolean {
  const prefix = avatarPrefix(userId);
  if (!object.key.startsWith(prefix)) return false;
  const owner = object.customMetadata?.userId;
  return (!owner || owner === userId) && /^\d+\.png$/.test(object.key.slice(prefix.length));
}

// A cursor can include similarly named users; the exact timestamp suffix and owner metadata
// restrict deletion to files made by the avatar uploader. Never derive keys from avatar_url.
export async function removeOwnedAvatars(env: Env, userId: string, through: number): Promise<void> {
  const prefix = avatarPrefix(userId);
  let cursor: string | undefined;
  do {
    const options = { prefix, cursor, include: ['customMetadata'] };
    const page = await env.AVATAR_BUCKET.list(options);
    const keys = page.objects.filter(object => ownsAvatar(userId, object)
      && Number(object.key.slice(prefix.length, -4)) <= through).map(object => object.key);
    if (keys.length) await env.AVATAR_BUCKET.delete(keys);
    cursor = page.truncated ? page.cursor : undefined;
  } while (cursor);
}

export function enqueueAvatarCleanup(env: Env, eligibleUsersSql: string, params: (string | number)[], cutoff: number) {
  return env.DB.prepare(`INSERT INTO account_avatar_cleanup (user_id, deleted_at, revision)
    SELECT id, ?, ? FROM (${eligibleUsersSql}) WHERE true
    ON CONFLICT(user_id) DO UPDATE SET deleted_at=MAX(deleted_at, excluded.deleted_at), revision=excluded.revision`)
    .bind(cutoff, crypto.randomUUID(), ...params);
}

export async function completeAvatarCleanup(env: Env, userId: string): Promise<boolean> {
  try {
    const job = await env.DB.prepare('SELECT deleted_at, revision FROM account_avatar_cleanup WHERE user_id=?')
      .bind(userId).first<{ deleted_at: number; revision: string }>();
    if (!job) return true;
    await removeOwnedAvatars(env, userId, job.deleted_at);
    // A late upload can enqueue another pass while a previous pass is in R2.
    const result = await env.DB.prepare('DELETE FROM account_avatar_cleanup WHERE user_id=? AND revision=?')
      .bind(userId, job.revision).run();
    return result.meta.changes === 1;
  } catch {
    // Leave the row for the next scheduled pass even when R2 or D1 is temporarily unavailable.
    await env.DB.prepare('UPDATE account_avatar_cleanup SET last_attempt_at=? WHERE user_id=?')
      .bind(Date.now(), userId).run().catch(() => {});
    return false;
  }
}

export async function retryAvatarCleanup(env: Env): Promise<void> {
  const page = await env.DB.prepare('SELECT user_id FROM account_avatar_cleanup ORDER BY last_attempt_at, deleted_at LIMIT 20')
    .all<{ user_id: string }>();
  for (const job of page.results) await completeAvatarCleanup(env, job.user_id);
}
