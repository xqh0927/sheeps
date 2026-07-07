"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.configureSecrets = configureSecrets;
exports.generateJWT = generateJWT;
exports.verifyJWT = verifyJWT;
exports.sha256 = sha256;
exports.decryptAES = decryptAES;
exports.encryptAES = encryptAES;
/**
 * 服务器间共享的 JWT 密钥
 * 开发环境使用内置 fallback；生产环境由 Worker Secrets 经 configureSecrets 注入。
 */
let JWT_SECRET = 'antigravity_secret_key';
/**
 * 共享的 AES-256 密钥（HEX 编码）
 * 开发环境使用内置 fallback；生产环境由 Worker Secrets 经 configureSecrets 注入。
 */
let AES_KEY_HEX = 'a1b2c3d4e5f6a7b8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2';
/** Worker 实例级缓存的 HMAC CryptoKey，避免每次 JWT 操作都重新 importKey（省 30-40% CPU） */
let cachedKey = null;
/** Worker 实例级缓存的 AES CryptoKey，避免每次加解密都 importKey */
let cachedAesKey = null;
/**
 * 注入生产环境密钥（Worker Secrets）。
 * 在 index.ts 的 fetch 入口调用一次；保留 dev fallback 常量以便本地 `wrangler dev`。
 *
 * @param env Worker 环境变量（含可选的 JWT_SECRET / AES_KEY_HEX）
 */
function configureSecrets(env) {
    if (env && env.JWT_SECRET) {
        JWT_SECRET = env.JWT_SECRET;
        cachedKey = null; // 密钥变更，强制下次重新 importKey
    }
    if (env && env.AES_KEY_HEX) {
        AES_KEY_HEX = env.AES_KEY_HEX;
        cachedAesKey = null;
    }
}
async function getKey() {
    if (!cachedKey) {
        cachedKey = await crypto.subtle.importKey('raw', new TextEncoder().encode(JWT_SECRET), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
    }
    return cachedKey;
}
/**
 * 签发 JWT (JSON Web Token)
 * 透传任意 payload 字段（含 userId/phone/role/type/exp），仅做 HMAC-SHA256 签名。
 *
 * @param payload 需要签名的数据负载
 * @returns 签名后的 Base64 字符串 (Header.Payload.Signature)
 */
async function generateJWT(payload) {
    const header = { alg: 'HS256', typ: 'JWT' };
    // Base64Url 编码 Header 和 Payload
    const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    const encodedPayload = btoa(unescape(encodeURIComponent(JSON.stringify(payload)))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    // 使用缓存的 CryptoKey 生成 HMAC-SHA256 签名（避免重复 importKey）
    const key = await getKey();
    const signatureBuffer = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`));
    const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
        .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    return `${encodedHeader}.${encodedPayload}.${signature}`;
}
/**
 * 验证并解析 JWT
 * @param token 客户端传来的 JWT 字符串
 * @returns 验证成功返回 Payload 对象（含任意透传字段），失败或过期返回 null
 */
async function verifyJWT(token) {
    const parts = token.split('.');
    if (parts.length !== 3)
        return null; // 格式错误
    const [encodedHeader, encodedPayload, signature] = parts;
    // 使用缓存的 CryptoKey 重新计算签名进行比对防伪造
    const key = await getKey();
    const expectedSigBuffer = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`));
    const expectedSig = btoa(String.fromCharCode(...new Uint8Array(expectedSigBuffer)))
        .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    if (signature !== expectedSig)
        return null; // 签名不匹配，Token 被篡改
    try {
        // 解码 Payload 并检查是否过期
        const decodedPayload = decodeURIComponent(escape(atob(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'))));
        const payload = JSON.parse(decodedPayload);
        if (payload.exp && Date.now() > payload.exp) {
            return null; // Token 已过期
        }
        return payload;
    }
    catch {
        return null;
    }
}
/**
 * 简单的 SHA-256 哈希计算，用于关卡 layout_data 摘要等
 */
async function sha256(message) {
    const msgBuffer = new TextEncoder().encode(message);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    // 转换为 16 进制字符串
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}
// ====================== AES-256-GCM 加解密模块 ======================
/**
 * 将 HEX 字符串转换为 Uint8Array
 * @param hex HEX 编码的字符串
 * @returns 对应的字节数组
 */
function hexToBytes(hex) {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
        bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
    }
    return bytes;
}
/**
 * 获取或缓存 AES-GCM CryptoKey
 * @returns 导入的 CryptoKey 实例
 */
async function getAesKey() {
    if (!cachedAesKey) {
        const rawKey = hexToBytes(AES_KEY_HEX);
        cachedAesKey = await crypto.subtle.importKey('raw', rawKey, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
    }
    return cachedAesKey;
}
/**
 * 使用 AES-256-GCM 解密 Base64 编码的密文
 *
 * 密文格式：Base64(IV(12字节) || ciphertext || authTag(16字节))
 *
 * @param encryptedBase64 Base64 编码的密文
 * @returns 解密后的明文字符串（通常为 JSON）
 */
async function decryptAES(encryptedBase64) {
    const key = await getAesKey();
    const encryptedBytes = Uint8Array.from(atob(encryptedBase64), c => c.charCodeAt(0));
    // 提取 IV（前 12 字节）
    const iv = encryptedBytes.slice(0, 12);
    // 提取密文 + authTag（authTag 在末尾 16 字节）
    const ciphertextWithTag = encryptedBytes.slice(12);
    const decryptedBuffer = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, ciphertextWithTag);
    return new TextDecoder().decode(decryptedBuffer);
}
/**
 * 使用 AES-256-GCM 加密明文
 *
 * 输出格式：Base64(IV(12字节) || ciphertext || authTag(16字节))
 *
 * @param plaintext 需要加密的明文字符串
 * @returns Base64 编码的密文
 */
async function encryptAES(plaintext) {
    const key = await getAesKey();
    // 生成 12 字节随机 IV
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encodedPlaintext = new TextEncoder().encode(plaintext);
    const encryptedBuffer = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encodedPlaintext);
    // 拼接 IV + 密文(含 authTag)
    const result = new Uint8Array(iv.length + encryptedBuffer.byteLength);
    result.set(iv, 0);
    result.set(new Uint8Array(encryptedBuffer), iv.length);
    return btoa(String.fromCharCode(...result));
}
