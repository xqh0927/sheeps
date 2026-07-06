import { decryptAES, encryptAES } from './crypto';

/**
 * 加密中间件：解密请求体、加密响应体
 *
 * 拦截所有 /api/ 的 POST/PUT 请求：
 * 1. 若请求体包含 `encrypted` 字段 → 解密还原原始 JSON，传给下游路由
 * 2. 响应时 → 将响应体 JSON 加密后包装为 `{"encrypted":"..."}`
 * 3. 若请求体不包含 `encrypted` 字段（老客户端兼容）→ 直接透传
 *
 * 注意：WebSocket 升级请求不经过此中间件（在 index.ts 中提前处理）
 */

/**
 * 解密请求体（如需要）
 *
 * @param request 原始 HTTP 请求
 * @returns 包含解密后请求体和是否已解密的标志
 */
export async function decryptRequest(
  request: Request
): Promise<{ request: Request; wasEncrypted: boolean }> {
  // 仅处理 POST/PUT/PATCH 的 /api/ 请求
  const method = request.method.toUpperCase();
  const url = new URL(request.url);
  if (
    !['POST', 'PUT', 'PATCH'].includes(method) ||
    !url.pathname.startsWith('/api/')
  ) {
    return { request, wasEncrypted: false };
  }

  // 跳过 multipart/form-data 请求（头像上传等二进制流无法走 JSON 加解密）
  const contentType = request.headers.get('Content-Type') || '';
  if (contentType.includes('multipart/form-data')) {
    return { request, wasEncrypted: false };
  }

  // 尝试读取请求体
  let bodyText: string;
  try {
    bodyText = await request.text();
  } catch {
    return { request, wasEncrypted: false };
  }

  if (!bodyText || bodyText.trim().length === 0) {
    // 空 body，用原始内容重建 Request
    const newRequest = new Request(request.url, {
      method: request.method,
      headers: request.headers,
      body: bodyText || undefined,
    });
    return { request: newRequest, wasEncrypted: false };
  }

  let bodyJson: any;
  try {
    bodyJson = JSON.parse(bodyText);
  } catch {
    // 非 JSON 请求体，用原始 body 重建 Request
    const newRequest = new Request(request.url, {
      method: request.method,
      headers: request.headers,
      body: bodyText,
    });
    return { request: newRequest, wasEncrypted: false };
  }

  // 检查是否包含 encrypted 字段
  if (!bodyJson.encrypted || typeof bodyJson.encrypted !== 'string') {
    // 未加密的请求，用原始 body 重建 Request（因为上面的 text() 已消费 body）
    const newRequest = new Request(request.url, {
      method: request.method,
      headers: request.headers,
      body: bodyText,
    });
    return { request: newRequest, wasEncrypted: false };
  }

  // 解密
  try {
    const decrypted = await decryptAES(bodyJson.encrypted);
    const newRequest = new Request(request.url, {
      method: request.method,
      headers: request.headers,
      body: decrypted,
    });
    return { request: newRequest, wasEncrypted: true };
  } catch (e: any) {
    // 解密失败，用原始密文重建 Request 并透传（下游会因格式错误而报错）
    console.error('Request decryption failed:', e.message);
    const newRequest = new Request(request.url, {
      method: request.method,
      headers: request.headers,
      body: bodyText,
    });
    return { request: newRequest, wasEncrypted: false };
  }
}

/**
 * 加密响应体（如原始请求是加密的）
 *
 * @param response 路由处理器返回的原始响应
 * @returns 加密包装后的响应（或原始响应，若无需加密）
 */
export async function encryptResponse(response: Response): Promise<Response> {
  const contentType = response.headers.get('Content-Type') || '';
  if (!contentType.includes('application/json')) {
    return response;
  }

  let bodyText: string;
  try {
    bodyText = await response.text();
  } catch {
    return response;
  }

  if (!bodyText || bodyText.trim().length === 0) {
    return response;
  }

  try {
    const encrypted = await encryptAES(bodyText);
    const wrappedBody = JSON.stringify({ encrypted });
    const newHeaders = new Headers(response.headers);
    newHeaders.set('Content-Length', String(new TextEncoder().encode(wrappedBody).length));
    return new Response(wrappedBody, {
      status: response.status,
      statusText: response.statusText,
      headers: newHeaders,
    });
  } catch (e: any) {
    console.error('Response encryption failed:', e.message);
    return response;
  }
}
