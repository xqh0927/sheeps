import { Env } from './types';
import { verifyJWT } from './crypto';

/**
 * 获取跨域资源共享 (CORS) 请求头
 * 允许前端 Web 或跨域请求访问 API
 */
export function getCorsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*', // 生产环境建议替换为具体域名
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Content-Type': 'application/json'
  };
}

/**
 * 鉴权中间件辅助函数：从请求头解析并验证 Access Token
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
 * 国际化辅助：根据请求头的 Accept-Language 决定返回的内容语言后缀
 */
export function getLangSuffix(request: Request): string {
  const acceptLang = request.headers.get('Accept-Language') || 'zh';
  if (acceptLang.includes('en')) return 'en';
  if (acceptLang.includes('TW') || acceptLang.includes('rTW') || acceptLang.includes('tw') || acceptLang.includes('Hant')) return 'tw';
  if (acceptLang.includes('ja')) return 'ja';
  if (acceptLang.includes('ko')) return 'ko';
  return ''; // 默认简体中文无后缀
}