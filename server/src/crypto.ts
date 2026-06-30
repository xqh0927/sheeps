/** * 服务器间共享的 JWT 密钥 
 * 注意：生产环境中建议将其移入环境机密变量 (Worker Secrets) 中
 */
const JWT_SECRET = 'antigravity_secret_key';

/**
 * 签发 JWT (JSON Web Token)
 * @param payload 需要签名的数据负载 (如 userId, phone)
 * @returns 签名后的 Base64 字符串 (Header.Payload.Signature)
 */
export async function generateJWT(payload: any): Promise<string> {
  const header = { alg: 'HS256', typ: 'JWT' };

  // Base64Url 编码 Header 和 Payload
  const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  const encodedPayload = btoa(unescape(encodeURIComponent(JSON.stringify(payload)))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

  // 使用 Web Crypto API 生成 HMAC-SHA256 签名
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

/**
 * 验证并解析 JWT
 * @param token 客户端传来的 JWT 字符串
 * @returns 验证成功返回 Payload 对象，失败或过期返回 null
 */
export async function verifyJWT(token: string): Promise<any | null> {
  const parts = token.split('.');
  if (parts.length !== 3) return null; // 格式错误

  const [encodedHeader, encodedPayload, signature] = parts;

  // 重新计算签名进行比对防伪造
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

  if (signature !== expectedSig) return null; // 签名不匹配，Token 被篡改

  try {
    // 解码 Payload 并检查是否过期
    const decodedPayload = decodeURIComponent(escape(atob(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'))));
    const payload = JSON.parse(decodedPayload);
    if (payload.exp && Date.now() > payload.exp) {
      return null; // Token 已过期
    }
    return payload;
  } catch {
    return null;
  }
}

/**
 * 简单的 SHA-256 哈希计算，用于防作弊签名校验
 */
export async function sha256(message: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  // 转换为 16 进制字符串
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}