import { Env } from '../types';

/**
 * R2 图片上传公共助手
 * 抽自 user.ts 的 /api/user/avatar 头像上传逻辑，供管理后台的图片上传/皮肤卡面/道具图标复用。
 *
 * 复用的既有绑定：
 *  - env.AVATAR_BUCKET  (R2 桶 sheeps-apk)
 *  - env.R2_PUBLIC_URL  (公网自定义域名 https://file.xqh.cc.cd)
 *  - env.SHEEPS_CACHE    (KV 缓存，用于 shop 列表缓存刷新)
 */

/** 单图大小上限（字节）：500KB */
const MAX_IMAGE_SIZE = 500 * 1024;

/** 允许的图片 MIME 类型 */
const VALID_IMAGE_TYPES = ['image/png', 'image/webp', 'image/jpeg'];

/**
 * 上传图片到 R2 并返回公网直链 URL。
 *
 * @param env  环境变量（含 AVATAR_BUCKET / R2_PUBLIC_URL）
 * @param key  R2 相对键，如 `images/skins/henan/3.png`
 * @param file 图片文件（来自 multipart FormData 的 File）
 * @returns 公网可访问的完整 URL：`${R2_PUBLIC_URL}/${key}`
 */
export async function putR2Image(env: Env, key: string, file: File): Promise<string> {
  await env.AVATAR_BUCKET.put(key, file.stream(), {
    httpMetadata: { contentType: file.type || 'image/png' },
    customMetadata: { uploadedAt: String(Date.now()) },
  });
  const base = env.R2_PUBLIC_URL || '';
  return `${base}/${key}`;
}

/**
 * 校验上传图片的类型与大小。
 *
 * @param file 待校验的图片文件
 * @returns 校验失败的错误信息（中文）；校验通过返回 null
 */
export function validateImage(file: File): string | null {
  if (file.size > MAX_IMAGE_SIZE) {
    return '图片过大（最大 500KB）';
  }
  if (!VALID_IMAGE_TYPES.includes(file.type)) {
    return '不支持的图片格式（仅允许 png / webp / jpeg）';
  }
  return null;
}

/**
 * 通用 R2 对象上传（不限制文件类型/前缀），供非图片资源（如 APK 安装包）复用。
 *
 * 与 putR2Image 的区别：
 *  - 不校验图片 MIME / 大小；
 *  - key 由调用方全权决定（如 `apks/...apk`），不再受 `images/` 约束。
 *
 * @param env  环境变量（含 AVATAR_BUCKET / R2_PUBLIC_URL）
 * @param key  R2 相对键，如 `apks/sheeps_v2.0.0.apk`
 * @param file 待上传文件（来自 multipart FormData 的 File）
 * @param contentType 可选，缺省回退 file.type 或 application/octet-stream
 * @returns 公网可访问的完整 URL：`${R2_PUBLIC_URL}/${key}`
 */
export async function putR2Object(env: Env, key: string, file: File, contentType?: string): Promise<string> {
  await env.AVATAR_BUCKET.put(key, file.stream(), {
    httpMetadata: { contentType: contentType || file.type || 'application/octet-stream' },
    customMetadata: { uploadedAt: String(Date.now()) },
  });
  const base = env.R2_PUBLIC_URL || '';
  return `${base}/${key}`;
}

/**
 * 清除 shop 列表的全部多语言 KV 缓存。
 * 任何 skin_tiles / item_icons / shop_items.image_url / shop_items."group" 写操作后都必须调用，
 * 保证 Android 下次拉取 shop 列表时拿到最新数据。
 *
 * @param env 环境变量（含 SHEEPS_CACHE）
 */
export async function purgeShopCache(env: Env): Promise<void> {
  for (const lang of ['', 'en', 'tw', 'ja', 'ko']) {
    try {
      await env.SHEEPS_CACHE.delete(`shop_items_${lang}`);
    } catch (_) {
      /* KV 删除失败不阻塞主流程 */
    }
  }
}
