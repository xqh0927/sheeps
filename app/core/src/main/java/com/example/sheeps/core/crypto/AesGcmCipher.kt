package com.example.sheeps.core.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 加解密单例工具
 *
 * 提供请求体和响应体的对称加密/解密，密钥与服务端一致。
 * 密文格式：Base64(IV(12字节) || ciphertext || authTag(16字节))
 */
object AesGcmCipher {

    /** AES-256 共享密钥（HEX 编码，与服务端 crypto.ts 一致） */
    private const val AES_KEY_HEX =
        "a1b2c3d4e5f6a7b8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2"

    /** GCM 认证标签长度（位） */
    private const val GCM_TAG_LENGTH = 128

    /** GCM IV 长度（字节） */
    private const val GCM_IV_LENGTH = 12

    /** AES 算法标识 */
    private const val ALGORITHM = "AES/GCM/NoPadding"

    /** 安全随机数生成器 */
    private val secureRandom = SecureRandom()

    /** 缓存的 SecretKeySpec，避免重复创建 */
    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = hexStringToByteArray(AES_KEY_HEX)
        SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 将 HEX 字符串转换为字节数组
     *
     * @param hex HEX 编码的字符串
     * @return 对应的字节数组
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * 使用 AES-256-GCM 加密明文
     *
     * @param plaintext 待加密的明文字符串
     * @return Base64 编码的密文（IV + ciphertext + authTag）
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)

        // 生成 12 字节随机 IV
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 拼接 IV + 密文（密文末尾已包含 authTag）
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * 使用 AES-256-GCM 解密 Base64 编码的密文
     *
     * @param encryptedBase64 Base64 编码的密文（IV + ciphertext + authTag）
     * @return 解密后的明文字符串
     */
    fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // 提取 IV（前 12 字节）
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)

        // 提取密文（含 authTag）
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}
