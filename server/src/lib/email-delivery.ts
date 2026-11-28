import { Env } from '../types';

const DEFAULT_BIRD_API_BASE = 'https://us1.platform.bird.com';
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_REGEX = /^1\d{10}$/;

const EMAIL_HOURLY_LIMIT = 5;
const IP_HOURLY_LIMIT = 15;
const DEFAULT_GLOBAL_DAILY_LIMIT = 50;
const MAX_CODE_VERIFY_ATTEMPTS = 5;
const MAX_PASSWORD_FAIL_ATTEMPTS = 5;
const PASSWORD_LOCK_TTL_SECONDS = 15 * 60;

export class EmailServiceUnavailableError extends Error {
  constructor(message = '邮件服务暂不可用') {
    super(message);
    this.name = 'EmailServiceUnavailableError';
  }
}

/** 归一化邮箱地址（去首尾空白并转小写），格式非法时返回 null */
export function normalizeEmail(input: unknown): string | null {
  if (typeof input !== 'string') return null;
  const normalized = input.trim().toLowerCase();
  if (!normalized || normalized.length > 254 || !EMAIL_REGEX.test(normalized)) {
    return null;
  }
  return normalized;
}

/** 校验中国大陆 11 位手机号格式 */
export function isValidPhone(input: unknown): input is string {
  return typeof input === 'string' && PHONE_REGEX.test(input.trim());
}

/** 基于 Workers KV 的多层限流（单邮箱 5 次/时、单 IP 15 次/时、全站默认 50 封/天），保护免费发信额度 */
export async function checkEmailRateLimit(env: Env, email: string, ip: string): Promise<boolean> {
  if (!env.SHEEPS_CACHE) return true;
  const now = Date.now();
  const hourBucket = Math.floor(now / 3600_000);
  const dayBucket = Math.floor(now / 86_400_000);
  const dailyLimit = Math.max(1, Number(env.EMAIL_DAILY_LIMIT || DEFAULT_GLOBAL_DAILY_LIMIT) || DEFAULT_GLOBAL_DAILY_LIMIT);

  const emailKey = `email_rl:e:${hourBucket}:${email}`;
  const ipKey = ip ? `email_rl:ip:${hourBucket}:${ip}` : null;
  const globalKey = `email_rl:global:${dayBucket}`;

  try {
    const globalCount = Number((await env.SHEEPS_CACHE.get(globalKey)) || '0');
    if (globalCount >= dailyLimit) return false;

    const emailCount = Number((await env.SHEEPS_CACHE.get(emailKey)) || '0');
    if (emailCount >= EMAIL_HOURLY_LIMIT) return false;

    let ipCount = 0;
    if (ipKey) {
      ipCount = Number((await env.SHEEPS_CACHE.get(ipKey)) || '0');
      if (ipCount >= IP_HOURLY_LIMIT) return false;
    }

    await env.SHEEPS_CACHE.put(globalKey, String(globalCount + 1), { expirationTtl: 86_400 });
    await env.SHEEPS_CACHE.put(emailKey, String(emailCount + 1), { expirationTtl: 3600 });
    if (ipKey) {
      await env.SHEEPS_CACHE.put(ipKey, String(ipCount + 1), { expirationTtl: 3600 });
    }
  } catch {
    // KV 异常时不阻断正常发信，仍由 D1 60s 冷却兜底
  }
  return true;
}

function buildEmailContent(code: string, lang: string): { subject: string; text: string; html: string } {
  switch (lang) {
    case 'en':
      return {
        subject: `Verification Code: ${code}`,
        text: `Your verification code is ${code}. It expires in 5 minutes. If you did not request this, please ignore this email.`,
        html: `<p>Your verification code is:</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px;">${code}</p><p>It expires in 5 minutes. If you did not request this, please ignore this email.</p>`,
      };
    case 'tw':
      return {
        subject: `驗證碼：${code}`,
        text: `您的驗證碼是 ${code}，5 分鐘內有效。若非本人操作，請忽略此郵件。`,
        html: `<p>您的驗證碼是：</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px;">${code}</p><p>5 分鐘內有效，若非本人操作，請忽略此郵件。</p>`,
      };
    case 'ja':
      return {
        subject: `認証コード：${code}`,
        text: `認証コードは ${code} です。有効期限は5分間です。心当たりがない場合はこのメールを無視してください。`,
        html: `<p>認証コード：</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px;">${code}</p><p>有効期限は5分間です。心当たりがない場合は無視してください。</p>`,
      };
    case 'ko':
      return {
        subject: `인증 코드: ${code}`,
        text: `인증 코드는 ${code} 입니다. 5분 이내에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시하세요.`,
        html: `<p>인증 코드:</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px;">${code}</p><p>5분 동안 유효합니다. 본인이 요청하지 않았다면 무시하세요.</p>`,
      };
    default:
      return {
        subject: `验证码：${code}`,
        text: `您的验证码是 ${code}，5 分钟内有效。如非本人操作，请忽略此邮件。`,
        html: `<p>您的验证码是：</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px;">${code}</p><p>5 分钟内有效，如非本人操作，请忽略此邮件。</p>`,
      };
  }
}

/** 通过 Bird Platform Email HTTP API 发送 6 位邮箱验证码 */
export async function sendVerificationEmail(env: Env, email: string, code: string, lang = ''): Promise<void> {
  const apiKey = env.BIRD_API_KEY?.trim();
  const senderEmail = env.EMAIL_SENDER?.trim();
  if (!apiKey || !senderEmail) {
    throw new EmailServiceUnavailableError();
  }

  const apiBase = (env.BIRD_API_BASE?.trim() || DEFAULT_BIRD_API_BASE).replace(/\/+$/, '');
  const senderName = env.EMAIL_SENDER_NAME?.trim() || '秘境消消乐';
  const { subject, text, html } = buildEmailContent(code, lang);

  const response = await fetch(`${apiBase}/v1/email/messages`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    redirect: 'manual',
    signal: AbortSignal.timeout(8000),
    body: JSON.stringify({
      from: { email: senderEmail, name: senderName },
      to: [email],
      subject,
      html,
      text,
    }),
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => '');
    console.error('[email-delivery] Bird failed:', response.status, errText);
    throw new Error(`Bird email delivery failed with status ${response.status}`);
  }
}

/** 原子核销指定签发时刻且未过期的邮箱验证码（防并发重放与重发竞态） */
export async function consumeEmailCode(env: Env, email: string, code: string, issuedAt: number): Promise<boolean> {
  const result = await env.DB.prepare(
    'DELETE FROM email_verification_codes WHERE email = ? AND code = ? AND created_at = ? AND created_at >= ?'
  ).bind(email, code, issuedAt, Date.now() - 5 * 60 * 1000).run();
  return result.meta.changes === 1;
}

/**
 * 校验邮箱验证码（含 5 分钟过期与连续输错 5 次自动作废）。
 * 仅做校验，通过后由调用方在业务前置检查完毕时调用 consumeEmailCode 完成核销。
 */
export async function verifyEmailCodeRecord(
  env: Env,
  email: string,
  inputCode: string,
  record: { code: string; created_at: number } | null | undefined
): Promise<{ ok: true; issuedAt: number } | { ok: false; error: string }> {
  if (!record) {
    return { ok: false, error: '验证码错误' };
  }
  if (Date.now() - record.created_at > 5 * 60 * 1000) {
    await env.DB.prepare('DELETE FROM email_verification_codes WHERE email = ? AND created_at = ?')
      .bind(email, record.created_at).run();
    return { ok: false, error: '验证码已过期' };
  }
  if (record.code !== inputCode) {
    if (env.SHEEPS_CACHE) {
      const failKey = `email_code_fail:${email}:${record.created_at}`;
      try {
        const nextAttempts = Number((await env.SHEEPS_CACHE.get(failKey)) || '0') + 1;
        if (nextAttempts >= MAX_CODE_VERIFY_ATTEMPTS) {
          await env.DB.prepare('DELETE FROM email_verification_codes WHERE email = ? AND created_at = ?')
            .bind(email, record.created_at).run();
          await env.SHEEPS_CACHE.delete(failKey);
          return { ok: false, error: '验证码错误次数过多，请重新获取' };
        }
        await env.SHEEPS_CACHE.put(failKey, String(nextAttempts), { expirationTtl: 300 });
      } catch {
        // KV 异常时仍返回普通错误
      }
    }
    return { ok: false, error: '验证码错误' };
  }
  return { ok: true, issuedAt: record.created_at };
}

/** 检查密码登录是否因连续失败处于锁定状态（15 分钟） */
export async function isPasswordLoginLocked(env: Env, accountKey: string): Promise<boolean> {
  if (!env.SHEEPS_CACHE) return false;
  try {
    const count = Number((await env.SHEEPS_CACHE.get(`pw_fail:${accountKey}`)) || '0');
    return count >= MAX_PASSWORD_FAIL_ATTEMPTS;
  } catch {
    return false;
  }
}

/** 记录一次密码登录失败（连续 5 次触发 15 分钟锁定） */
export async function recordPasswordLoginFailure(env: Env, accountKey: string): Promise<void> {
  if (!env.SHEEPS_CACHE) return;
  const key = `pw_fail:${accountKey}`;
  try {
    const next = Number((await env.SHEEPS_CACHE.get(key)) || '0') + 1;
    await env.SHEEPS_CACHE.put(key, String(next), { expirationTtl: PASSWORD_LOCK_TTL_SECONDS });
  } catch {
    // 忽略 KV 写入异常
  }
}

/** 密码登录成功后清零失败计数 */
export async function clearPasswordLoginFailures(env: Env, accountKey: string): Promise<void> {
  if (!env.SHEEPS_CACHE) return;
  try {
    await env.SHEEPS_CACHE.delete(`pw_fail:${accountKey}`);
  } catch {
    // 忽略 KV 异常
  }
}

