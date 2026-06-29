export interface Env {
  DB: D1Database;
}

const GITHUB_RELEASES_API = 'https://api.github.com/repos/xqh0927/sheeps-releases/releases/latest';
const GITHUB_RELEASE_CACHE_TTL_MS = 5 * 60 * 1000;

type GitHubReleaseAsset = {
  name?: string;
  browser_download_url?: string;
};

type GitHubRelease = {
  tag_name?: string;
  name?: string;
  body?: string;
  draft?: boolean;
  prerelease?: boolean;
  assets?: GitHubReleaseAsset[];
};

type AppUpdatePayload = {
  has_update: boolean;
  version_name?: string;
  apk_url?: string;
  update_log?: string;
  force_update?: boolean;
};

let githubReleaseCache: { expiresAt: number; release: GitHubRelease | null } | null = null;

export function parseReleaseVersionCode(tagName?: string): number | null {
  if (!tagName) return null;

  const semanticMatch = tagName.match(/v?(\d+)(?:\.(\d+))?(?:\.(\d+))?/i);
  if (!semanticMatch) return null;

  const major = Number.parseInt(semanticMatch[1], 10);
  const minor = semanticMatch[2] ? Number.parseInt(semanticMatch[2], 10) : 0;
  const patch = semanticMatch[3] ? Number.parseInt(semanticMatch[3], 10) : 0;

  if (!Number.isFinite(major) || !Number.isFinite(minor) || !Number.isFinite(patch)) {
    return null;
  }

  if (minor === 0 && patch === 0) {
    return major;
  }

  return major * 10000 + minor * 100 + patch;
}

export function findApkAsset(assets?: GitHubReleaseAsset[]): GitHubReleaseAsset | null {
  return assets?.find(asset =>
    Boolean(asset.browser_download_url) && Boolean(asset.name?.toLowerCase().endsWith('.apk'))
  ) ?? null;
}

export function isForceUpdateRelease(body?: string): boolean {
  if (!body) return false;
  return /\[(force|force_update|强制更新|強制更新)\]|force_update\s*[:=]\s*true/i.test(body);
}

export function mapGitHubReleaseToUpdate(release: GitHubRelease, currentCode: number): AppUpdatePayload | null {
  if (release.draft) return null;

  const versionCode = parseReleaseVersionCode(release.tag_name);
  const apkAsset = findApkAsset(release.assets);
  if (!versionCode || !apkAsset?.browser_download_url || versionCode <= currentCode) {
    return { has_update: false };
  }

  return {
    has_update: true,
    version_name: release.name || release.tag_name,
    apk_url: apkAsset.browser_download_url,
    update_log: release.body || '发现新版本，建议更新以获得更好的游戏体验。',
    force_update: isForceUpdateRelease(release.body)
  };
}

async function fetchLatestGitHubRelease(): Promise<GitHubRelease | null> {
  const now = Date.now();
  if (githubReleaseCache && githubReleaseCache.expiresAt > now) {
    return githubReleaseCache.release;
  }

  const response = await fetch(GITHUB_RELEASES_API, {
    headers: {
      'Accept': 'application/vnd.github+json',
      'User-Agent': 'sheeps-update-checker'
    }
  });

  if (!response.ok) {
    githubReleaseCache = { expiresAt: now + 30 * 1000, release: null };
    return null;
  }

  const release = await response.json<GitHubRelease>();
  githubReleaseCache = { expiresAt: now + GITHUB_RELEASE_CACHE_TTL_MS, release };
  return release;
}

async function getGitHubAppUpdate(currentCode: number): Promise<AppUpdatePayload | null> {
  try {
    const release = await fetchLatestGitHubRelease();
    return release ? mapGitHubReleaseToUpdate(release, currentCode) : null;
  } catch {
    return null;
  }
}

async function getDatabaseAppUpdate(env: Env, currentCode: number): Promise<AppUpdatePayload> {
  const latest = await env.DB.prepare(
    'SELECT version_code, version_name, apk_url, update_log, is_force_update FROM app_version ORDER BY version_code DESC LIMIT 1'
  ).first<{ version_code: number; version_name: string; apk_url: string; update_log: string; is_force_update: number }>();

  if (latest && latest.version_code > currentCode) {
    return {
      has_update: true,
      version_name: latest.version_name,
      apk_url: latest.apk_url,
      update_log: latest.update_log,
      force_update: latest.is_force_update === 1
    };
  }

  return { has_update: false };
}

// 声明机房常驻内存缓存，用于缓存已生成的关卡数据
const CACHE_STAGE_CONFIG = new Map<number, string>();

// HMAC-SHA256 JWT Implementation using Web Crypto API
const JWT_SECRET = 'antigravity_secret_key';

async function generateJWT(payload: any): Promise<string> {
  const header = { alg: 'HS256', typ: 'JWT' };
  const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  const encodedPayload = btoa(unescape(encodeURIComponent(JSON.stringify(payload)))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(JWT_SECRET),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const signatureBuffer = await crypto.subtle.sign(
    'HMAC',
    key,
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`)
  );
  const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
    .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

async function verifyJWT(token: string): Promise<any | null> {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [encodedHeader, encodedPayload, signature] = parts;
  
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(JWT_SECRET),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const expectedSigBuffer = await crypto.subtle.sign(
    'HMAC',
    key,
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`)
  );
  const expectedSig = btoa(String.fromCharCode(...new Uint8Array(expectedSigBuffer)))
    .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    
  if (signature !== expectedSig) return null;
  
  try {
    const decodedPayload = decodeURIComponent(escape(atob(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'))));
    const payload = JSON.parse(decodedPayload);
    if (payload.exp && Date.now() > payload.exp) {
      return null;
    }
    return payload;
  } catch {
    return null;
  }
}

// SHA-256 helper for simple anti-cheat verification
async function sha256(message: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// CORS Headers Configuration
function getCorsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Content-Type': 'application/json'
  };
}

// Auth Helper - verifies ACCESS tokens only
async function getAuthenticatedUser(request: Request, env: Env): Promise<{ userId: string; phone: string } | null> {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  const token = authHeader.substring(7);
  const payload = await verifyJWT(token);
  if (!payload || payload.type !== 'access') {
    return null;
  }
  return payload;
}

// Resolve language suffix based on Accept-Language
function getLangSuffix(request: Request): string {
  const acceptLang = request.headers.get('Accept-Language') || 'zh';
  if (acceptLang.includes('en')) return 'en';
  if (acceptLang.includes('TW') || acceptLang.includes('rTW') || acceptLang.includes('tw') || acceptLang.includes('Hant')) return 'tw';
  if (acceptLang.includes('ja')) return 'ja';
  if (acceptLang.includes('ko')) return 'ko';
  return ''; // Default Simplified Chinese
}

// Level structure generator helper
interface Point3D {
  x: number;
  y: number;
  z: number;
}

interface TileData {
  id: string;
  x: number;
  y: number;
  z: number;
  type: number;
  isBlind: boolean;
  sealedCount: number;
}

function getDifficultyForLevel(levelId: number): number {
  if (levelId === 1) return 1;
  if (levelId === 2) return 2;
  return 3;
}

function lcg(seed: number) {
  let s = seed;
  return function() {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

function generateSolvableLevel(levelId: number): TileData[] {
  let coordinates: Point3D[] = [];
  const seed = levelId * 1000;

  if (levelId === 1) {
    coordinates = [
      { x: 1.0, y: 1.0, z: 0 }, { x: 2.0, y: 1.0, z: 0 },
      { x: 1.0, y: 2.0, z: 0 }, { x: 2.0, y: 2.0, z: 0 },
      { x: 1.5, y: 1.5, z: 1 }, { x: 2.5, y: 1.5, z: 1 },
      { x: 1.5, y: 2.5, z: 1 }, { x: 2.5, y: 2.5, z: 1 },
      { x: 2.0, y: 2.0, z: 2 }
    ];
  } else {
    // x = levelId. Total cards N linearly increases: N = 36 + (x - 2) * 12
    const maxCards = levelId === 2 ? 36 : 36 + (levelId - 2) * 12;
    const possibleCoords: Point3D[] = [];
    
    // Layers count L converges with square root: L = 12 - 8 / sqrt(x - 1), capped at 12
    const layersCount = levelId === 2 ? 4 : Math.min(12, Math.floor(12 - 8 / Math.sqrt(levelId - 1)));
    
    const baseSize = 6 + Math.floor(levelId / 20);
    for (let z = 0; z < layersCount; z++) {
      const size = Math.max(3, baseSize - Math.floor(z / 3));
      const offset = (z % 2 === 0) ? 0 : 0.5;
      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
          possibleCoords.push({
            x: c + offset + 1.0,
            y: r + offset + 1.0,
            z: z
          });
        }
      }
    }

    let rand = lcg(seed);
    for (let i = possibleCoords.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      const temp = possibleCoords[i];
      possibleCoords[i] = possibleCoords[j];
      possibleCoords[j] = temp;
    }

    const count = Math.min(possibleCoords.length, maxCards) - (Math.min(possibleCoords.length, maxCards) % 3);
    coordinates = possibleCoords.slice(0, count);
  }

  coordinates.sort((a, b) => a.z - b.z);

  const W = 1.0;
  const H = 1.0;
  
  // Card types T increases logarithmically: T = 3 + 3 * ln(x), capped at 16
  const numTypes = levelId === 1 ? 3 : Math.min(16, Math.floor(3 + 3 * Math.log(levelId)));

  interface Node {
    index: number;
    coord: Point3D;
    assignedType: number;
  }

  const nodes: Node[] = coordinates.map((c, idx) => ({
    index: idx,
    coord: c,
    assignedType: -1
  }));

  const blocks = (a: Point3D, b: Point3D) => {
    return a.z > b.z && Math.abs(a.x - b.x) < W && Math.abs(a.y - b.y) < H;
  };

  const unassigned = new Set<Node>(nodes);
  let randAssign = lcg(seed + 100);

  while (unassigned.size > 0) {
    const exposedNodes: Node[] = [];
    for (const node of unassigned) {
      let isCovered = false;
      for (const other of unassigned) {
        if (other !== node && blocks(other.coord, node.coord)) {
          isCovered = true;
          break;
        }
      }
      if (!isCovered) {
        exposedNodes.push(node);
      }
    }

    if (exposedNodes.length < 3) {
      const rem = Array.from(unassigned);
      while (rem.length >= 3) {
        const type = Math.floor(randAssign() * numTypes) + 1;
        for (let k = 0; k < 3; k++) {
          const n = rem.pop()!;
          n.assignedType = type;
          unassigned.delete(n);
        }
      }
      for (const n of rem) {
        n.assignedType = 1;
        unassigned.delete(n);
      }
      break;
    }

    const type = Math.floor(randAssign() * numTypes) + 1;
    for (let k = 0; k < 3; k++) {
      const idx = Math.floor(randAssign() * exposedNodes.length);
      const chosen = exposedNodes.splice(idx, 1)[0];
      chosen.assignedType = type;
      unassigned.delete(chosen);
    }
  }

  let randProps = lcg(seed + 200);
  return nodes.map((node) => {
    let isBlind = false;
    let sealedCount = 0;

    if (levelId >= 2) {
      const r = randProps();
      if (levelId % 10 === 0 && r < 0.15) {
        isBlind = true;
      } else if (r < 0.30) {
        sealedCount = 1;
      }
    }

    return {
      id: `tile_${node.index}`,
      x: node.coord.x,
      y: node.coord.y,
      z: node.coord.z,
      type: node.assignedType,
      isBlind,
      sealedCount
    };
  });
}

// Router Implementation
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const corsHeaders = getCorsHeaders();

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const lang = getLangSuffix(request);

    try {
      // 1. Send SMS Code simulation
      if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { phone: string } = await request.json();
        if (!body.phone) {
          return new Response(JSON.stringify({ error: 'Missing phone number' }), { status: 400, headers: corsHeaders });
        }
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        await env.DB.prepare(
          'INSERT OR REPLACE INTO login_token (phone, code, created_at) VALUES (?, ?, ?)'
        ).bind(body.phone, code, Date.now()).run();

        return new Response(JSON.stringify({ success: true, code }), { headers: corsHeaders });
      }

      // 2. Login Verification with Guest Merge
      if (path === '/api/auth/login' && request.method === 'POST') {
        const body: { phone: string; code: string; device_uuid?: string } = await request.json();
        if (!body.phone || !body.code) {
          return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });
        }

        const codeRecord = await env.DB.prepare(
          'SELECT code, created_at FROM login_token WHERE phone = ?'
        ).bind(body.phone).first<{ code: string; created_at: number }>();

        if (!codeRecord || codeRecord.code !== body.code) {
          return new Response(JSON.stringify({ error: 'Verification code incorrect' }), { status: 400, headers: corsHeaders });
        }

        if (Date.now() - codeRecord.created_at > 5 * 60 * 1000) {
          return new Response(JSON.stringify({ error: 'Verification code expired' }), { status: 400, headers: corsHeaders });
        }

        await env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone).run();

        let user = await env.DB.prepare(
          'SELECT id, phone, username, avatar, points FROM users WHERE phone = ?'
        ).bind(body.phone).first<{ id: string; phone: string; username: string; avatar: string | null; points: number }>();

        let isNewUser = false;
        if (!user) {
          isNewUser = true;
          const uuid = crypto.randomUUID();
          const defaultUsername = `国风玩家_${body.phone.slice(-4)}`;
          await env.DB.prepare(
            'INSERT INTO users (id, phone, username, avatar, points, created_at) VALUES (?, ?, ?, ?, ?, ?)'
          ).bind(uuid, body.phone, defaultUsername, null, 0, Date.now()).run();

          // Unlock Level 1 by default
          await env.DB.prepare(
            'INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)'
          ).bind(uuid, Date.now()).run();

          // Grant initial inventory items
          const initialItems = ['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE'];
          for (const item of initialItems) {
            await env.DB.prepare(
              'INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)'
            ).bind(uuid, item, 1).run();
          }

          user = { id: uuid, phone: body.phone, username: defaultUsername, avatar: null, points: 0 };
        }

        // Guest Merge: inherit points, inventory and unlocked levels
        if (body.device_uuid && body.device_uuid !== user.id) {
          const guestUser = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(body.device_uuid).first<{ points: number }>();
          if (guestUser) {
            // 1. Merge points
            const mergedPoints = user.points + guestUser.points;
            await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(mergedPoints, user.id).run();
            user.points = mergedPoints;

            // 2. Merge levels
            const guestLevels = await env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(body.device_uuid).all<{ level_id: number }>();
            for (const row of guestLevels.results) {
              await env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)')
                .bind(user.id, row.level_id, Date.now()).run();
            }

            // 3. Merge items
            const guestItems = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(body.device_uuid).all<{ item_type: string; count: number }>();
            for (const row of guestItems.results) {
              await env.DB.prepare(
                'INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ' +
                'ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?'
              ).bind(user.id, row.item_type, row.count, row.count).run();
            }

            // 4. Delete guest progress
            await env.DB.prepare('DELETE FROM level_unlock WHERE user_id = ?').bind(body.device_uuid).run();
            await env.DB.prepare('DELETE FROM user_items WHERE user_id = ?').bind(body.device_uuid).run();
            await env.DB.prepare('DELETE FROM users WHERE id = ?').bind(body.device_uuid).run();
          }
        }

        // Generate Double Tokens
        const token = await generateJWT({ userId: user.id, phone: user.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await generateJWT({ userId: user.id, phone: user.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        // Query unlocked levels
        const unlockedRows = await env.DB.prepare(
          'SELECT level_id FROM level_unlock WHERE user_id = ?'
        ).bind(user.id).all<{ level_id: number }>();
        const unlockedLevels = unlockedRows.results.map(r => r.level_id);

        // Query inventory items
        const itemRows = await env.DB.prepare(
          'SELECT item_type, count FROM user_items WHERE user_id = ?'
        ).bind(user.id).all<{ item_type: string; count: number }>();
        const items = itemRows.results;

        // Query sign in status
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const signToday = await env.DB.prepare(
          'SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?'
        ).bind(user.id, chinaToday).first();

        const lastSign = await env.DB.prepare(
          'SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1'
        ).bind(user.id).first<{ streak: number }>();

        return new Response(JSON.stringify({
          success: true,
          token,
          refreshToken,
          user,
          unlocked_levels: unlockedLevels,
          items,
          today_signed: signToday !== null,
          sign_streak: lastSign ? lastSign.streak : 0
        }), { headers: corsHeaders });
      }

      // Token silent refresh
      if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body: { refreshToken: string } = await request.json();
        if (!body.refreshToken) {
          return new Response(JSON.stringify({ error: 'Missing refresh token' }), { status: 400, headers: corsHeaders });
        }
        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') {
          return new Response(JSON.stringify({ error: 'Invalid or expired refresh token' }), { status: 401, headers: corsHeaders });
        }

        const newAccessToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({
          success: true,
          token: newAccessToken,
          refreshToken: newRefreshToken
        }), { headers: corsHeaders });
      }

      // 3. User Data Sync with backup log
      if (path === '/api/user/sync' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const body: {
          points?: number;
          unlocked_levels?: number[];
          items?: { item_type: string; count: number }[];
        } = await request.json();

        // 1. Back up existing cloud data before overwrite
        const oldUser = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
        if (oldUser) {
          const oldLevels = await env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId).all();
          const oldItems = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId).all();
          const backupPayload = JSON.stringify({
            points: oldUser.points,
            unlocked_levels: oldLevels.results.map((r: any) => r.level_id),
            items: oldItems.results
          });
          // Write to backup logs
          await env.DB.prepare(
            'INSERT INTO backup_save_log (user_id, save_data, created_at) VALUES (?, ?, ?)'
          ).bind(authUser.userId, backupPayload, Date.now()).run();

          // Delete logs older than 7 days
          await env.DB.prepare(
            'DELETE FROM backup_save_log WHERE created_at < ?'
          ).bind(Date.now() - 7 * 86400000).run();
        }

        // Apply sync updates
        if (body.points !== undefined && body.points > 0) {
          await env.DB.prepare(
            'UPDATE users SET points = MAX(points, ?) WHERE id = ?'
          ).bind(body.points, authUser.userId).run();
        }

        if (body.unlocked_levels) {
          for (const lvl of body.unlocked_levels) {
            await env.DB.prepare(
              'INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)'
            ).bind(authUser.userId, lvl, Date.now()).run();
          }
        }

        if (body.items) {
          for (const item of body.items) {
            await env.DB.prepare(
              'INSERT OR REPLACE INTO user_items (user_id, item_type, count) VALUES (?, ?, ' +
              'COALESCE((SELECT MAX(count, ?) FROM user_items WHERE user_id = ? AND item_type = ?), ?))'
            ).bind(authUser.userId, item.item_type, item.count, authUser.userId, item.item_type, item.count).run();
          }
        }

        // Return updated values
        const updatedUser = await env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE id = ?').bind(authUser.userId).first();
        const updatedLevels = await env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId).all();
        const updatedItems = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId).all();

        return new Response(JSON.stringify({
          success: true,
          user: updatedUser,
          unlocked_levels: updatedLevels.results.map((r: any) => r.level_id),
          items: updatedItems.results
        }), { headers: corsHeaders });
      }

      // 4. Unlock Level
      if (path === '/api/level/unlock' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const body: { level_id: number } = await request.json();
        if (body.level_id === undefined) {
          return new Response(JSON.stringify({ error: 'Missing level ID' }), { status: 400, headers: corsHeaders });
        }

        // Check if already unlocked
        const checkUnlock = await env.DB.prepare(
          'SELECT 1 FROM level_unlock WHERE user_id = ? AND level_id = ?'
        ).bind(authUser.userId, body.level_id).first();
        if (checkUnlock) {
          const userPoints = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
          return new Response(JSON.stringify({ success: true, current_points: userPoints?.points || 0 }), { headers: corsHeaders });
        }

        // Fetch unlock points cost from DB config
        const configRow = await env.DB.prepare('SELECT value FROM config WHERE key = ?')
          .bind(`level_${body.level_id}_unlock_points`).first<{ value: string }>();
        const cost = configRow ? parseInt(configRow.value, 10) : (body.level_id === 2 ? 50 : (body.level_id === 3 ? 100 : 200));

        const userRow = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
        if (!userRow || userRow.points < cost) {
          return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });
        }

        // Transaction simulation
        const newPoints = userRow.points - cost;
        await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId).run();
        await env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)')
          .bind(authUser.userId, body.level_id, Date.now()).run();
        await env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
          .bind(authUser.userId, 'OUT', cost, `UNLOCK_LEVEL_${body.level_id}`, newPoints, Date.now()).run();

        return new Response(JSON.stringify({ success: true, current_points: newPoints }), { headers: corsHeaders });
      }

      // 5. Submit Score (Auto Unlock, Anti-Cheat check and First Clear Points reward)
      if (path === '/api/score/submit' && request.method === 'POST') {
        const body: {
          user_id: string;
          level_id: number;
          score: number;
          clear_time_ms: number;
          sign: string;
        } = await request.json();

        if (!body.user_id || body.level_id === undefined || body.score === undefined || body.clear_time_ms === undefined || !body.sign) {
          return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });
        }

        // Logical validation for Anti-Cheat
        if (body.score > 20000 || body.clear_time_ms < 3000) {
          return new Response(JSON.stringify({ error: 'Cheating detected. Score submitted is invalid.' }), { status: 400, headers: corsHeaders });
        }

        const expectedSignInput = `${body.user_id}_${body.level_id}_${body.clear_time_ms}_folklore`;
        const expectedSign = await sha256(expectedSignInput);

        if (body.sign !== expectedSign) {
          return new Response(JSON.stringify({ error: 'Invalid signature' }), { status: 403, headers: corsHeaders });
        }

        // Detect if JWT auth user
        const authUser = await getAuthenticatedUser(request, env);
        const resolvedUserId = authUser ? authUser.userId : body.user_id;

        // Check if user exists, if not, create a guest/new user and unlock first 3 levels automatically
        let userExists = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(resolvedUserId).first<{ points: number }>();
        if (!userExists) {
          const defaultUsername = resolvedUserId.startsWith('uuid-') ? `游客_${resolvedUserId.slice(-4)}` : `玩家_${resolvedUserId.slice(-4)}`;
          await env.DB.prepare(
            'INSERT INTO users (id, phone, username, avatar, points, created_at) VALUES (?, ?, ?, ?, ?, ?)'
          ).bind(resolvedUserId, null, defaultUsername, null, 0, Date.now()).run();

          // Automatically unlock the first 3 levels for new guest/offline users so they can play them directly
          for (const lvl of [1, 2, 3]) {
            await env.DB.prepare(
              'INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)'
            ).bind(resolvedUserId, lvl, Date.now()).run();
          }

          userExists = { points: 0 };
        }

        // Check if first clear of this level
        const clears = await env.DB.prepare(
          'SELECT COUNT(*) as count FROM leaderboard WHERE user_id = ? AND level_id = ?'
        ).bind(resolvedUserId, body.level_id).first<{ count: number }>();
        
        let firstClear = false;
        let finalPoints = userExists.points;
        if (!clears || clears.count === 0) {
          firstClear = true;
          finalPoints += 50; // reward 50 points for first clear
          await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(finalPoints, resolvedUserId).run();
          await env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
            .bind(resolvedUserId, 'IN', 50, 'FIRST_CLEAR', finalPoints, Date.now()).run();
        }

        // Auto unlock next level
        await env.DB.prepare(
          'INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)'
        ).bind(resolvedUserId, body.level_id + 1, Date.now()).run();

        // Increment daily task progress
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        for (const tid of ['PLAY_3_GAMES', 'PLAY_5_GAMES']) {
          const taskProgress = await env.DB.prepare(
            'SELECT ut.progress, t.target_count, ut.is_completed FROM user_task ut JOIN task t ON ut.task_id = t.id WHERE ut.user_id = ? AND ut.task_id = ? AND ut.task_date = ?'
          ).bind(resolvedUserId, tid, chinaToday).first<{ progress: number; target_count: number; is_completed: number }>();

          if (!taskProgress) {
            const target = tid === 'PLAY_3_GAMES' ? 3 : 5;
            await env.DB.prepare(
              'INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, 1, 0, 0)'
            ).bind(resolvedUserId, tid, chinaToday).run();
          } else if (taskProgress.is_completed === 0) {
            const newProg = Math.min(taskProgress.target_count, taskProgress.progress + 1);
            const completed = newProg >= taskProgress.target_count ? 1 : 0;
            await env.DB.prepare(
              'UPDATE user_task SET progress = ?, is_completed = ? WHERE user_id = ? AND task_id = ? AND task_date = ?'
            ).bind(newProg, completed, resolvedUserId, tid, chinaToday).run();
          }
        }

        // Insert into leaderboard
        await env.DB.prepare(
          'INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, achieved_at) VALUES (?, ?, ?, ?, ?)'
        ).bind(resolvedUserId, body.level_id, body.score, body.clear_time_ms, Date.now()).run();

        return new Response(JSON.stringify({ success: true, first_clear: firstClear, points_reward: firstClear ? 50 : 0 }), { headers: corsHeaders });
      }

      // 6. Sign In
      if (path === '/api/sign/today' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const checkSign = await env.DB.prepare(
          'SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?'
        ).bind(authUser.userId, chinaToday).first();

        if (checkSign) {
          return new Response(JSON.stringify({ error: 'Already signed in today' }), { status: 400, headers: corsHeaders });
        }

        const chinaYesterday = new Date(Date.now() + 8 * 3600000 - 24 * 3600000).toISOString().split('T')[0];
        const lastSign = await env.DB.prepare(
          'SELECT streak FROM sign_record WHERE user_id = ? AND sign_date = ?'
        ).bind(authUser.userId, chinaYesterday).first<{ streak: number }>();

        const newStreak = lastSign ? lastSign.streak + 1 : 1;

        // Fetch reward config
        const rewardsConfigRow = await env.DB.prepare('SELECT value FROM config WHERE key = ?').bind('sign_rewards').first<{ value: string }>();
        const rewards = rewardsConfigRow ? rewardsConfigRow.value.split(',').map(Number) : [20, 20, 30, 30, 40, 50, 100];
        const rewardPoints = rewards[Math.min(newStreak, 7) - 1] || 20;

        const userRow = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
        const newPoints = (userRow?.points || 0) + rewardPoints;

        await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId).run();
        await env.DB.prepare('INSERT INTO sign_record (user_id, sign_date, streak, points_rewarded, created_at) VALUES (?, ?, ?, ?, ?)')
          .bind(authUser.userId, chinaToday, newStreak, rewardPoints, Date.now()).run();
        await env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
          .bind(authUser.userId, 'IN', rewardPoints, 'SIGN_IN', newPoints, Date.now()).run();

        // Increment SIGN_IN_ONCE task
        await env.DB.prepare(
          'INSERT OR REPLACE INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, ?, ?, ?)'
        ).bind(authUser.userId, 'SIGN_IN_ONCE', chinaToday, 1, 1, 0).run();

        return new Response(JSON.stringify({
          success: true,
          streak: newStreak,
          reward_points: rewardPoints,
          current_points: newPoints
        }), { headers: corsHeaders });
      }

      // 7. Shop Items List with multi-language
      if (path === '/api/shop/items' && request.method === 'GET') {
        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';
        const queryStr = `SELECT id, ${nameCol} as name, ${descCol} as description, image_url, item_type, points_price, stock FROM shop_items`;
        const items = await env.DB.prepare(queryStr).all();
        return new Response(JSON.stringify(items.results), { headers: corsHeaders });
      }

      // 8. Shop Exchange
      if (path === '/api/shop/exchange' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const body: { shop_item_id: number; count: number } = await request.json();
        if (body.shop_item_id === undefined || !body.count || body.count < 1) {
          return new Response(JSON.stringify({ error: 'Invalid parameters' }), { status: 400, headers: corsHeaders });
        }

        const shopItem = await env.DB.prepare(
          'SELECT name, item_type, points_price, stock FROM shop_items WHERE id = ?'
        ).bind(body.shop_item_id).first<{ name: string; item_type: string; points_price: number; stock: number }>();

        if (!shopItem || shopItem.stock < body.count) {
          return new Response(JSON.stringify({ error: 'Item out of stock or not found' }), { status: 400, headers: corsHeaders });
        }

        const totalCost = shopItem.points_price * body.count;
        const userRow = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
        
        if (!userRow || userRow.points < totalCost) {
          return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });
        }

        const remainingPoints = userRow.points - totalCost;

        // Perform transactional operations
        await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId).run();
        await env.DB.prepare('UPDATE shop_items SET stock = stock - ? WHERE id = ?').bind(body.count, body.shop_item_id).run();
        
        await env.DB.prepare(
          'INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ' +
          'ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?'
        ).bind(authUser.userId, shopItem.item_type, body.count, body.count).run();

        await env.DB.prepare(
          'INSERT INTO exchange_record (user_id, shop_item_id, item_type, count, points_cost, created_at) VALUES (?, ?, ?, ?, ?, ?)'
        ).bind(authUser.userId, body.shop_item_id, shopItem.item_type, body.count, totalCost, Date.now()).run();

        await env.DB.prepare(
          'INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)'
        ).bind(authUser.userId, 'OUT', totalCost, `SHOP_REDEEM_${shopItem.item_type}`, remainingPoints, Date.now()).run();

        const backpackItem = await env.DB.prepare('SELECT count FROM user_items WHERE user_id = ? AND item_type = ?')
          .bind(authUser.userId, shopItem.item_type).first<{ count: number }>();

        return new Response(JSON.stringify({
          success: true,
          item_type: shopItem.item_type,
          new_count: backpackItem?.count || 0,
          remaining_points: remainingPoints
        }), { headers: corsHeaders });
      }

      // 9. Daily Tasks with multi-language
      if (path === '/api/task/daily' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        
        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';
        const queryStr = `SELECT id, ${nameCol} as name, ${descCol} as description, target_count, points_reward FROM task`;
        
        const allTasks = await env.DB.prepare(queryStr).all<{
          id: string; name: string; description: string; target_count: number; points_reward: number;
        }>();

        const list = [];
        for (const task of allTasks.results) {
          let userTask = await env.DB.prepare(
            'SELECT progress, is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_id = ? AND task_date = ?'
          ).bind(authUser.userId, task.id, chinaToday).first<{ progress: number; is_completed: number; is_rewarded: number }>();

          if (!userTask) {
            await env.DB.prepare(
              'INSERT INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, 0, 0, 0)'
            ).bind(authUser.userId, task.id, chinaToday).run();
            userTask = { progress: 0, is_completed: 0, is_rewarded: 0 };
          }

          list.push({
            task_id: task.id,
            name: task.name,
            description: task.description,
            progress: userTask.progress,
            target_count: task.target_count,
            is_completed: userTask.is_completed === 1,
            is_rewarded: userTask.is_rewarded === 1,
            points_reward: task.points_reward
          });
        }

        return new Response(JSON.stringify(list), { headers: corsHeaders });
      }

      // 10. Claim Task Reward
      if (path === '/api/task/claim' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const body: { task_id: string } = await request.json();
        if (!body.task_id) {
          return new Response(JSON.stringify({ error: 'Missing task ID' }), { status: 400, headers: corsHeaders });
        }

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const userTask = await env.DB.prepare(
          'SELECT is_completed, is_rewarded FROM user_task WHERE user_id = ? AND task_id = ? AND task_date = ?'
        ).bind(authUser.userId, body.task_id, chinaToday).first<{ is_completed: number; is_rewarded: number }>();

        if (!userTask || userTask.is_completed === 0 || userTask.is_rewarded === 1) {
          return new Response(JSON.stringify({ error: 'Task not completed or already rewarded' }), { status: 400, headers: corsHeaders });
        }

        const taskRow = await env.DB.prepare('SELECT points_reward FROM task WHERE id = ?').bind(body.task_id).first<{ points_reward: number }>();
        const reward = taskRow?.points_reward || 10;

        const userRow = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId).first<{ points: number }>();
        const remainingPoints = (userRow?.points || 0) + reward;

        await env.DB.prepare('UPDATE user_task SET is_rewarded = 1 WHERE user_id = ? AND task_id = ? AND task_date = ?')
          .bind(authUser.userId, body.task_id, chinaToday).run();
        await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId).run();
        await env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
          .bind(authUser.userId, 'IN', reward, `DAILY_TASK_${body.task_id}`, remainingPoints, Date.now()).run();

        return new Response(JSON.stringify({ success: true, current_points: remainingPoints }), { headers: corsHeaders });
      }

      // 11. Notices List with multi-language
      if (path === '/api/notice/list' && request.method === 'GET') {
        const titleCol = lang ? `COALESCE(title_${lang}, title)` : 'title';
        const contentCol = lang ? `COALESCE(content_${lang}, content)` : 'content';
        const queryStr = `SELECT id, ${titleCol} as title, ${contentCol} as content, type, created_at FROM notice ORDER BY created_at DESC`;
        const notices = await env.DB.prepare(queryStr).all();
        return new Response(JSON.stringify(notices.results), { headers: corsHeaders });
      }

      // 12. Leaderboard Classified & Paginated
      if (path === '/api/leaderboard' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('level_id');
        if (!levelIdStr) {
          return new Response(JSON.stringify({ error: 'Missing level_id' }), { status: 400, headers: corsHeaders });
        }
        const levelId = parseInt(levelIdStr, 10);
        const type = url.searchParams.get('type') || 'history'; // daily, weekly, history
        const page = parseInt(url.searchParams.get('page') || '1', 10);
        const limit = parseInt(url.searchParams.get('limit') || '20', 10);
        const offset = (page - 1) * limit;

        let timeFilter = 0;
        const now = Date.now();
        const chinaTodayStr = new Date(now + 8 * 3600000).toISOString().split('T')[0];
        const todayStart = new Date(chinaTodayStr + 'T00:00:00+08:00').getTime();

        if (type === 'daily') {
          timeFilter = todayStart;
        } else if (type === 'weekly') {
          const dayOfWeek = (new Date(now + 8 * 3600000).getDay() + 6) % 7; // Monday = 0
          timeFilter = todayStart - dayOfWeek * 24 * 3600000;
        }

        const results = await env.DB.prepare(
          `SELECT u.username, u.avatar, l.clear_time_ms, l.score, l.achieved_at 
           FROM leaderboard l 
           JOIN users u ON l.user_id = u.id 
           WHERE l.level_id = ? AND l.achieved_at >= ?
           ORDER BY l.clear_time_ms ASC 
           LIMIT ? OFFSET ?`
        ).bind(levelId, timeFilter, limit, offset).all();

        return new Response(JSON.stringify({ success: true, rankings: results.results }), { headers: corsHeaders });
      }

      // 13. User profile details (all-in-one endpoint)
      if (path === '/api/user/profile' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }

        const user = await env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE id = ?').bind(authUser.userId).first();
        const levels = await env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId).all();
        const items = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId).all();
        
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const signedToday = await env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday).first();
        const lastSign = await env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1').bind(authUser.userId).first<{ streak: number }>();
        const highestCleared = await env.DB.prepare('SELECT MAX(level_id) as highest FROM leaderboard WHERE user_id = ?').bind(authUser.userId).first<{ highest: number | null }>();

        return new Response(JSON.stringify({
          success: true,
          user,
          unlocked_levels: levels.results.map((r: any) => r.level_id),
          items: items.results,
          today_signed: signedToday !== null,
          sign_streak: lastSign ? lastSign.streak : 0,
          highest_level_cleared: highestCleared?.highest || 0
        }), { headers: corsHeaders });
      }

      // 14. Points Flow History
      if (path === '/api/user/points-history' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }
        const records = await env.DB.prepare(
          'SELECT type, amount, source, remaining_points, created_at FROM point_record WHERE user_id = ? ORDER BY created_at DESC LIMIT 50'
        ).bind(authUser.userId).all();
        return new Response(JSON.stringify(records.results), { headers: corsHeaders });
      }

      // 15. Exchange Records History
      if (path === '/api/user/exchange-history' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) {
          return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        }
        const records = await env.DB.prepare(
          'SELECT shop_item_id, item_type, count, points_cost, created_at FROM exchange_record WHERE user_id = ? ORDER BY created_at DESC LIMIT 50'
        ).bind(authUser.userId).all();
        return new Response(JSON.stringify(records.results), { headers: corsHeaders });
      }

      // 16. Get/Set Configs (Admin)
      if (path === '/api/admin/config' && request.method === 'GET') {
        const list = await env.DB.prepare('SELECT key, value FROM config').all();
        return new Response(JSON.stringify(list.results), { headers: corsHeaders });
      }

      if (path === '/api/admin/config' && request.method === 'POST') {
        const body: { key: string; value: string } = await request.json();
        if (!body.key || !body.value) {
          return new Response(JSON.stringify({ error: 'Missing config key or value' }), { status: 400, headers: corsHeaders });
        }
        await env.DB.prepare('INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)')
          .bind(body.key, body.value).run();
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      // Default fallback to old Level API (with in-memory cache)
      if (path === '/api/level' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('id');
        if (!levelIdStr) {
          return new Response(JSON.stringify({ error: 'Missing level ID' }), { status: 400, headers: corsHeaders });
        }
        const levelId = parseInt(levelIdStr, 10);
        if (isNaN(levelId)) {
          return new Response(JSON.stringify({ error: 'Invalid level ID' }), { status: 400, headers: corsHeaders });
        }

        // 1. 内存中存在？直接秒回
        if (CACHE_STAGE_CONFIG.has(levelId)) {
          return new Response(CACHE_STAGE_CONFIG.get(levelId)!, { headers: corsHeaders });
        }

        // 2. 内存没有？从 D1 数据库查询
        const levelRow = await env.DB.prepare(
          'SELECT layout_data FROM levels WHERE level_id = ?'
        ).bind(levelId).first<{ layout_data: string }>();

        if (levelRow) {
          // 写入内存，以便下次直接返回
          CACHE_STAGE_CONFIG.set(levelId, levelRow.layout_data);
          return new Response(levelRow.layout_data, { headers: corsHeaders });
        }

        const newLayout = generateSolvableLevel(levelId);
        const layoutJson = JSON.stringify(newLayout);

        // Store level layouts in database to prevent local generation discrepancy
        await env.DB.prepare(
          'INSERT OR IGNORE INTO levels (level_id, difficulty, layout_data, created_at) VALUES (?, ?, ?, ?)'
        ).bind(levelId, getDifficultyForLevel(levelId), layoutJson, Date.now()).run();

        // 写入内存
        CACHE_STAGE_CONFIG.set(levelId, layoutJson);

        return new Response(layoutJson, { headers: corsHeaders });
      }

      // App Update Check endpoint
      if (path === '/api/app/check-update' && request.method === 'GET') {
        const currentCodeStr = url.searchParams.get('version_code');
        const currentCode = currentCodeStr ? parseInt(currentCodeStr, 10) : 1;

        const githubUpdate = await getGitHubAppUpdate(currentCode);
        if (githubUpdate?.has_update) {
          return new Response(JSON.stringify(githubUpdate), { headers: corsHeaders });
        }

        const databaseUpdate = await getDatabaseAppUpdate(env, currentCode);
        return new Response(JSON.stringify(databaseUpdate), { headers: corsHeaders });
      }

      // Original User Rename endpoint
      if (path === '/api/user/rename' && request.method === 'POST') {
        const body: { id: string; new_username: string } = await request.json();
        if (!body.id || !body.new_username) {
          return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });
        }
        await env.DB.prepare(
          'UPDATE users SET username = ? WHERE id = ?'
        ).bind(body.new_username, body.id).run();

        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      return new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });
    } catch (e: any) {
      return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
    }
  }
};
