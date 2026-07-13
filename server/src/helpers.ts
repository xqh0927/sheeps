/**
 * @module helpers
 * @fileoverview 服务端公共工具与鉴权辅助模块。
 *
 * 本模块聚合两类能力：
 * 1. **鉴权与权限守卫**：玩家端 JWT 解析(`getAuthenticatedUser`)、
 *    管理员三级鉴权(`requireAdmin`)以及写操作/超级操作守卫
 *    (`assertCanWrite`/`assertSuper`)，是后台所有写接口的共用前置校验。
 * 2. **通用基础设施**：CORS 头构造、统一错误响应、多语言(i18n)错误映射、
 *    系统配置 KV 缓存读取、游戏模式开关查询。
 *
 * 约定：鉴权类函数返回 `Response` 即表示校验失败，调用方应以
 * `instanceof Response` 判定并直接透传该响应；返回业务对象则表示鉴权通过。
 */

import { Env } from './types';
import { verifyJWT } from './crypto';

/** 管理员角色枚举 */
export type AdminRole = 'super' | 'operator' | 'readonly';

/** 管理员 JWT 解析后的身份信息 */
export interface AdminPayload {
  userId: string;
  phone: string;
  role: AdminRole;
  type: 'access' | 'refresh';
  exp?: number;
}

/** 合法的管理员角色集合 */
const ADMIN_ROLES: AdminRole[] = ['super', 'operator', 'readonly'];

/**
 * 获取跨域资源共享 (CORS) 请求头
 * 优先使用 env.ADMIN_WEB_ORIGIN（管理后台 Pages 站点的精确源），否则回退到 '*'。
 * Allow-Methods 已扩展为 GET,POST,PUT,DELETE,OPTIONS 以支撑后台写操作与预检。
 *
 * @param env 可选，传入后可精确授权管理后台域名
 */
export function getCorsHeaders(env?: Env) {
  const origin = (env && env.ADMIN_WEB_ORIGIN) || '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Content-Type': 'application/json'
  };
}

/**
 * 构造边缘缓存响应头（Cloudflare CDN Cache-Control）。
 * 用于无需鉴权的 public GET 接口，让 Cloudflare 边缘按 max-age 缓存 JSON 响应，降低回源。
 *
 * @param seconds 缓存时长（秒）
 * @param privateCache 为 true 时设为 private（仅客户端缓存，边缘不共享）；默认 false（public 边缘共享）
 * @returns 可展开合并进 Response headers 的对象，如 `{ 'Cache-Control': 'public, max-age=300' }`
 */
export function cacheControl(seconds: number, privateCache = false): Record<string, string> {
  return privateCache
    ? { 'Cache-Control': `private, max-age=${seconds}` }
    : { 'Cache-Control': `public, max-age=${seconds}` };
}

/**
 * 统一构造错误响应（带 CORS 头）
 * @param message 错误描述
 * @param status HTTP 状态码
 * @param env 可选，传入后 CORS 使用精确源
 */
export function jsonError(message: string, status: number, env?: Env): Response {
  return new Response(JSON.stringify({ error: message }), { status, headers: getCorsHeaders(env) });
}

/**
 * 鉴权中间件辅助函数：从请求头解析并验证 Access Token（玩家端）
 * 保留原有玩家鉴权逻辑，不被后台改造破坏。
 * @returns 验证通过返回用户身份信息，否则返回 null
 */
export async function getAuthenticatedUser(request: Request, env: Env): Promise<{ userId: string; phone: string } | null> {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null; // 未携带 Token 或格式错误
  }

  const token = authHeader.substring(7); // 截取 'Bearer ' 之后的部分
  const payload = await verifyJWT(token);

  // 确保使用的是 access token 而不是 refresh token
  if (!payload || payload.type !== 'access') {
    return null;
  }
  return payload;
}

/**
 * 管理员鉴权：校验后台 access token
 * 三级校验：type==='access' && role IN (super,operator,readonly) && is_banned=0。
 * 封禁状态可能在 token 签发之后变化，因此查库二次确认。
 *
 * @returns 成功返回 AdminPayload，失败返回 401 Response（调用方用 instanceof Response 判断）
 */
export async function requireAdmin(request: Request, env: Env): Promise<AdminPayload | Response> {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return jsonError('未提供管理员凭证', 401, env);
  }

  const token = authHeader.substring(7).trim();
  const payload = await verifyJWT(token);

  if (!payload || payload.type !== 'access' || !ADMIN_ROLES.includes(payload.role)) {
    return jsonError('无效的管理员凭证', 401, env);
  }

  // 封禁状态可能晚于 token 签发发生变化，查库二次确认
  const row = await env.DB.prepare('SELECT is_banned FROM users WHERE id = ?').bind(payload.userId).first<{ is_banned: number }>();
  if (!row || row.is_banned === 1) {
    return jsonError('管理员账号已被封禁', 401, env);
  }

  return payload as AdminPayload;
}

/**
 * 写操作守卫：readonly 角色调用写接口 → 403
 * @returns 无权时返回 403 Response，否则返回 null
 */
export function assertCanWrite(payload: AdminPayload): Response | null {
  if (payload.role === 'readonly') {
    return jsonError('无写权限（当前为只读角色）', 403);
  }
  return null;
}

/**
 * 超级守卫：非 super 角色调用 super 专属接口 → 403
 * @returns 无权时返回 403 Response，否则返回 null
 */
export function assertSuper(payload: AdminPayload): Response | null {
  if (payload.role !== 'super') {
    return jsonError('仅超级管理员可执行此操作', 403);
  }
  return null;
}

/**
 * 国际化辅助：根据请求头的 `Accept-Language` 决定返回的内容语言后缀。
 *
 * 用于 `translateErrorMessage` 选择错误消息的目标语言。匹配优先级为：
 * en(英语) > TW/rTW/tw/Hant(繁体中文) > ja(日语) > ko(韩语) > 其它(简体中文)。
 *
 * @param request 客户端 HTTP 请求，从中读取 Accept-Language 头
 * @returns 语言后缀：'en' | 'tw' | 'ja' | 'ko'；默认简体中文返回空串 ''
 */
export function getLangSuffix(request: Request): string {
  const acceptLang = request.headers.get('Accept-Language') || 'zh';
  if (acceptLang.includes('en')) return 'en';
  if (acceptLang.includes('TW') || acceptLang.includes('rTW') || acceptLang.includes('tw') || acceptLang.includes('Hant')) return 'tw';
  if (acceptLang.includes('ja')) return 'ja';
  if (acceptLang.includes('ko')) return 'ko';
  return ''; // 默认简体中文无后缀
}

/**
 * 错误信息国际化映射翻译表（i18n 数据源）。
 *
 * 以简体中文业务文案为 key，映射到 en/tw/ja/ko 四种语言译本；
 * 由 `translateErrorMessage` 在响应返回前按需查表翻译。
 */
const ERROR_TRANSLATIONS: Record<string, Record<string, string>> = {
  '请填写手机号': {
    'en': 'Please enter phone number',
    'tw': '請填寫手機號碼',
    'ja': '電話番号を入力してください',
    'ko': '전화번호를 입력하십시오'
  },
  '请填写手机号和验证码': {
    'en': 'Please enter phone number and verification code',
    'tw': '請填寫手機號和驗證碼',
    'ja': '電話番号と認証コードを入力してください',
    'ko': '전화번호와 인증번호를 입력하십시오'
  },
  '验证码错误': {
    'en': 'Invalid verification code',
    'tw': '驗證碼錯誤',
    'ja': '認証コードが正しくありません',
    'ko': '인증번호가 잘못되었습니다'
  },
  '验证码已过期': {
    'en': 'Verification code expired',
    'tw': '驗證碼已過期',
    'ja': '認証コードの有効期限が切れています',
    'ko': '인증번호가 만료되었습니다'
  },
  '缺少刷新令牌': {
    'en': 'Missing refresh token',
    'tw': '缺少刷新令牌',
    'ja': 'リフレッシュトークンがありません',
    'ko': '리프레시 토큰이 누락되었습니다'
  },
  '登录凭证已过期，请重新登录': {
    'en': 'Login expired, please log in again',
    'tw': '登入憑證已過期，請重新登入',
    'ja': 'ログイン期限が切れました。再度ログインしてください',
    'ko': '로그인 증명이 만료되었습니다. 다시 로그인하십시오'
  },
  '请填写完整注册信息': {
    'en': 'Please fill in registration info',
    'tw': '請填寫完整註冊信息',
    'ja': '登録情報を入力してください',
    'ko': '등록 정보를 모두 입력하십시오'
  },
  '该手机号已注册': {
    'en': 'Phone number already registered',
    'tw': '該手機號已註冊',
    'ja': 'この電話番号は既に登録されています',
    'ko': '이미 등록된 전화번호입니다'
  },
  '请填写手机号和密码': {
    'en': 'Please enter phone number and password',
    'tw': '請填寫手機號和密碼',
    'ja': '電話番号とパスワードを入力してください',
    'ko': '전화번호와 비밀번호를 입력하십시오'
  },
  '用户不存在': {
    'en': 'User does not exist',
    'tw': '用戶不存在',
    'ja': 'ユーザーが存在しません',
    'ko': '사용자가 존재하지 않습니다'
  },
  '尚未设置密码，请使用验证码登录': {
    'en': 'Password not set, please log in with code',
    'tw': '尚未設置密碼，請使用驗證碼登入',
    'ja': 'パスワードが設定されていません。コードでログインしてください',
    'ko': '비밀번호가 설정되지 않았습니다. 인증번호로 로그인하십시오'
  },
  '密码错误': {
    'en': 'Incorrect password',
    'tw': '密碼錯誤',
    'ja': 'パスワードが正しくありません',
    'ko': '비밀번호가 틀렸습니다'
  },
  '请填写完整信息': {
    'en': 'Please fill in all details',
    'tw': '請填寫完整信息',
    'ja': 'すべての情報を入力してください',
    'ko': '모든 정보를 입력하십시오'
  },
  'Unauthorized': {
    'en': 'Unauthorized, please log in again',
    'tw': '未授權，請重新登入',
    'ja': '未認可。再度ログインしてください',
    'ko': '인증되지 않음. 다시 로그인하십시오'
  },
  'Missing new_username': {
    'en': 'Missing new username',
    'tw': '缺少新用戶名',
    'ja': '新しいユーザー名がありません',
    'ko': '새 사용자 이름이 누락되었습니다'
  },
  'Expected multipart/form-data': {
    'en': 'Expected multipart form data',
    'tw': '需要 multipart/form-data',
    'ja': 'multipart/form-data形式が必要です',
    'ko': 'multipart/form-data 형식이 필요합니다'
  },
  'Missing avatar file': {
    'en': 'Missing avatar file',
    'tw': '缺少頭像文件',
    'ja': 'アバターファイルがありません',
    'ko': '아바타 파일이 누락되었습니다'
  },
  'Avatar image too large (max 512KB)': {
    'en': 'Avatar image too large (max 512KB)',
    'tw': '頭像圖片太大 (最大 512KB)',
    'ja': 'アバター画像が大きすぎます (最大 512KB)',
    'ko': '아바타 이미지가 너무 큽니다 (최대 512KB)'
  },
  'Invalid image type (allowed: png, jpeg, webp)': {
    'en': 'Invalid image type (allowed: png, jpeg, webp)',
    'tw': '不支援的圖片格式 (僅允許 png, jpeg, webp)',
    'ja': '無効な画像形式です (許可: png, jpeg, webp)',
    'ko': '잘못된 이미지 형식입니다 (허용: png, jpeg, webp)'
  },
  'Missing userId': {
    'en': 'Missing user ID',
    'tw': '缺少用戶ID',
    'ja': 'ユーザーIDがありません',
    'ko': '사용자 ID가 누락되었습니다'
  },
  'Avatar not found': {
    'en': 'Avatar not found',
    'tw': '找不到頭像',
    'ja': 'アバターが見つかりません',
    'ko': '아바타를 찾을 수 없습니다'
  },
  'Avatar file not found': {
    'en': 'Avatar file not found',
    'tw': '找不到頭像文件',
    'ja': 'アバターファイルが見つかりません',
    'ko': '아바타 파일을 찾을 수 없습니다'
  },
  'Missing task ID': {
    'en': 'Missing task ID',
    'tw': '缺少任務ID',
    'ja': 'タスクIDがありません',
    'ko': '작업 ID가 누락되었습니다'
  },
  'Task not completed or already rewarded': {
    'en': 'Task not completed or already rewarded',
    'tw': '任務未完成或已領取獎勵',
    'ja': 'タスクが未完了、または既に報酬を受け取っています',
    'ko': '작업이 완료되지 않았거나 이미 보상을 받았습니다'
  },
  'Missing config key or value': {
    'en': 'Missing config key or value',
    'tw': '缺少配置鍵或值',
    'ja': '設定キーまたは値がありません',
    'ko': '설정 키 또는 값이 누락되었습니다'
  },
  'Invalid parameters': {
    'en': 'Invalid parameters',
    'tw': '無效參數',
    'ja': '無効なパラメータ',
    'ko': '유효하지 않은 매개변수'
  },
  'Item out of stock': {
    'en': 'Item out of stock',
    'tw': '商品庫存不足',
    'ja': '在庫がありません',
    'ko': '상품 재고가 부족합니다'
  },
  'Insufficient points': {
    'en': 'Insufficient points',
    'tw': '積分不足',
    'ja': 'ポイントが不足しています',
    'ko': '포인트가 부족합니다'
  },
  'Missing playerId': {
    'en': 'Missing player ID',
    'tw': '缺少玩家ID',
    'ja': 'プレイヤーIDがありません',
    'ko': '플레이어 ID가 누락되었습니다'
  },
  'Missing level ID': {
    'en': 'Missing level ID',
    'tw': '缺少關卡ID',
    'ja': 'ステージIDがありません',
    'ko': '스테이지 ID가 누락되었습니다'
  },
  'Missing level_id': {
    'en': 'Missing level ID',
    'tw': '缺少關卡ID',
    'ja': 'ステージIDがありません',
    'ko': '스테이지 ID가 누락되었습니다'
  },
  'Invalid level ID': {
    'en': 'Invalid level ID',
    'tw': '無效關卡ID',
    'ja': '無効なステージID',
    'ko': '유효하지 않은 스테이지 ID'
  },
  'Invalid signature': {
    'en': 'Invalid signature',
    'tw': '無效簽名',
    'ja': '無効な署名',
    'ko': '유효하지 않은 서명'
  },
  'Already signed in today': {
    'en': 'Already signed in today',
    'tw': '今天已簽到',
    'ja': '本日は既にログインチェック済みです',
    'ko': '오늘 이미 로그인 체크를 완료했습니다'
  },
  'Not Found': {
    'en': 'Not Found',
    'tw': '未找到',
    'ja': '見つかりません',
    'ko': '찾을 수 없음'
  },
  'Missing parameters': {
    'en': 'Missing parameters',
    'tw': '缺少參數',
    'ja': 'パラメータが不足しています',
    'ko': '매개변수가 누락되었습니다'
  }
};

/**
 * 将业务错误消息翻译为指定语言（i18n）。
 *
 * 查找 {@link ERROR_TRANSLATIONS} 映射表；若 `lang` 为空（简体中文）或
 * 未命中目标语言，则原样返回原始错误文案，保证客户端始终能拿到可读信息。
 *
 * @param error 原始错误消息（简体中文 key）
 * @param lang 目标语言后缀，由 `getLangSuffix` 产出（'en'/'tw'/'ja'/'ko' 或 ''）
 * @returns 翻译后的错误消息；未命中时退回原始 `error`
 */
export function translateErrorMessage(error: string, lang: string): string {
  if (!lang) return error; // 默认简体中文，直接返回原信息
  const translations = ERROR_TRANSLATIONS[error];
  if (translations && translations[lang]) {
    return translations[lang];
  }
  return error;
}

/**
 * 读取系统配置值（优先 KV 缓存，未命中查 D1 并回填缓存）
 * 用于签到奖励、解锁积分门槛等低频变更的配置项
 */
export async function getCachedConfig(env: Env, key: string, defaultValue: string): Promise<string> {
  const cacheKey = `config_${key}`;
  const cached = await env.SHEEPS_CACHE.get(cacheKey);
  if (cached !== null && cached !== undefined) return cached;

  const row = await env.DB.prepare('SELECT value FROM config WHERE key = ?').bind(key).first<{ value: string }>();
  const value = row?.value ?? defaultValue;
  // 异步写 KV，不阻塞主流程
  env.SHEEPS_CACHE.put(cacheKey, value, { expirationTtl: 600 }).catch(() => {});
  return value;
}

/**
 * 读取游戏模式开关状态。
 * 闯关模式（gamemode_stage）默认开，无尽生存模式（gamemode_endless）默认关。
 * 复用 getCachedConfig（KV 缓存，低频变更）。
 *
 * @returns { stage: boolean; endless: boolean } 各模式是否开启
 */
export async function getGameModeStatus(env: Env): Promise<{ stage: boolean; endless: boolean; battle: boolean }> {
  const stage = await getCachedConfig(env, 'gamemode_stage', 'on');
  const endless = await getCachedConfig(env, 'gamemode_endless', 'off');
  const battle = await getCachedConfig(env, 'gamemode_battle', 'off');
  return { stage: stage === 'on', endless: endless === 'on', battle: battle === 'on' };
}
