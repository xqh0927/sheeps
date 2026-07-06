"use strict";
/**
 * 密码哈希工具模块 (PBKDF2)
 *
 * 使用 HMAC-SHA256 + 100,000 迭代的 PBKDF2 算法进行密码哈希，
 * 配合随机 32 字节盐值，防止彩虹表攻击。
 *
 * 存储格式："salt_hex:hash_hex"
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.hashPassword = hashPassword;
exports.verifyPassword = verifyPassword;
const PBKDF2_ITERATIONS = 100000;
const SALT_LENGTH = 32; // 字节
const HASH_LENGTH = 32; // 字节
/**
 * 将字节数组转换为 HEX 字符串
 */
function bytesToHex(bytes) {
    return Array.from(bytes)
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
}
/**
 * 将 HEX 字符串转换为字节数组
 */
function hexToBytes(hex) {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
        bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
    }
    return bytes;
}
/**
 * 使用 PBKDF2 对密码进行哈希
 *
 * @param password 明文密码
 * @returns 存储格式："salt_hex:hash_hex"
 */
async function hashPassword(password) {
    // 生成随机 32 字节盐值
    const salt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
    // 导入密码密钥材料
    const keyMaterial = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
    // 使用 PBKDF2-HMAC-SHA256 派生密钥
    const derivedBits = await crypto.subtle.deriveBits({
        name: 'PBKDF2',
        salt: salt,
        iterations: PBKDF2_ITERATIONS,
        hash: 'SHA-256',
    }, keyMaterial, HASH_LENGTH * 8 // 位长度
    );
    const hashBytes = new Uint8Array(derivedBits);
    return `${bytesToHex(salt)}:${bytesToHex(hashBytes)}`;
}
/**
 * 验证密码是否匹配存储的哈希值
 *
 * @param password 待验证的明文密码
 * @param stored 存储的哈希字符串，格式为 "salt_hex:hash_hex"
 * @returns 密码是否匹配
 */
async function verifyPassword(password, stored) {
    const parts = stored.split(':');
    if (parts.length !== 2) {
        return false;
    }
    const saltHex = parts[0];
    const storedHashHex = parts[1];
    if (!saltHex || !storedHashHex) {
        return false;
    }
    let salt;
    try {
        salt = hexToBytes(saltHex);
    }
    catch {
        return false;
    }
    // 使用相同的盐值和参数重新派生
    const keyMaterial = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
    const derivedBits = await crypto.subtle.deriveBits({
        name: 'PBKDF2',
        salt: salt,
        iterations: PBKDF2_ITERATIONS,
        hash: 'SHA-256',
    }, keyMaterial, HASH_LENGTH * 8);
    const computedHashHex = bytesToHex(new Uint8Array(derivedBits));
    // 常数时间比较
    return storedHashHex === computedHashHex;
}
