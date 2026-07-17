package com.example.sheeps.data.preference

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.sheeps.data.game.SkinConstants

/**
 * 用户偏好与敏感凭据存储（进程级单例）。
 *
 * - 普通偏好使用 MMKV（线程安全，可任意线程读写）；
 * - 敏感凭据（Token/手机号）懒加载使用 EncryptedSharedPreferences（AndroidX Security，基于 MasterKey 加密存储）。
 * 由于涉及磁盘 I/O，建议避免在 UI 动画等高频路径同步读取大量数据。
 */
@Singleton
class UserPreferences @Inject constructor(
    private val kv: MMKV,
    @ApplicationContext private val context: Context
) {

    // ⚠️ 线程提示：securePrefs 通过 lazy 首次访问时创建，EncryptedSharedPreferences.create 涉及磁盘 I/O，
    //    若首次访问发生在主线程可能掉帧；建议在后台或冷启动阶段预先触发一次访问。其后续读写为内存映射，开销较低。
    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @get:JvmName("fetchUserId")
    val userId: String
        get() {
            var id = kv.decodeString("user_id", null)
            if (id == null) {
                id = "uuid-" + UUID.randomUUID().toString()
                kv.encode("user_id", id)
            }
            return id
        }

    fun getUserId(): String = userId

    fun getUsername(): String {
        return kv.decodeString("username", "游客_${userId.take(8)}") ?: "游客"
    }

    fun setUsername(name: String) {
        kv.encode("username", name)
    }

    fun getUnlockedLevel(): Int {
        return kv.decodeInt("unlocked_level", 1)
    }

    fun setUnlockedLevel(level: Int) {
        kv.encode("unlocked_level", level)
    }

    fun isPrivacyAccepted(): Boolean {
        return kv.decodeBool("privacy_accepted", false)
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        kv.encode("privacy_accepted", accepted)
    }

    // --- Dynamic Locale / Language preferences ---
    
    fun getLanguage(): String {
        return kv.decodeString("user_lang", "") ?: ""
    }

    fun setLanguage(lang: String) {
        kv.encode("user_lang", lang)
    }

    // --- Secure Token Storage using EncryptedSharedPreferences ---

    fun getToken(): String? {
        return securePrefs.getString("jwt_token", null)
    }

    fun setToken(token: String?) {
        securePrefs.edit().putString("jwt_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return securePrefs.getString("refresh_token", null)
    }

    fun setRefreshToken(token: String?) {
        securePrefs.edit().putString("refresh_token", token).apply()
    }

    fun getPhone(): String? {
        return securePrefs.getString("user_phone", null)
    }

    fun setPhone(phone: String?) {
        securePrefs.edit().putString("user_phone", phone).apply()
    }

    fun getPoints(): Int {
        return kv.decodeInt("user_points", 0)
    }

    fun setPoints(points: Int) {
        kv.encode("user_points", points)
    }

    /**
     * 获取用户头像的完整访问 URL
     * 头像现已存储于 Cloudflare R2，通过 Worker 代理返回
     */
    fun getAvatarUrl(): String {
        return kv.decodeString("avatar_url", "") ?: ""
    }

    /**
     * 设置用户头像的完整访问 URL
     */
    fun setAvatarUrl(url: String) {
        kv.encode("avatar_url", url)
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun logout() {
        securePrefs.edit().remove("jwt_token").remove("refresh_token").remove("user_phone").apply()
        kv.removeValueForKey("user_points")
        kv.encode("unlocked_level", 1)
        kv.removeValueForKey("username")
        kv.removeValueForKey("today_signed")
        kv.removeValueForKey("today_signed_date")
        kv.removeValueForKey("sign_streak")
        kv.removeValueForKey("highest_level_cleared")
        kv.removeValueForKey("avatar_url")
    }

    fun getCurrentSkin(): String {
        return kv.decodeString("current_skin", SkinConstants.DEFAULT_SKIN) ?: SkinConstants.DEFAULT_SKIN
    }

    fun setCurrentSkin(skin: String) {
        kv.encode("current_skin", skin)
    }

    fun getTodaySigned(): Boolean {
        val signed = kv.decodeBool("today_signed", false)
        if (!signed) return false
        // 检查是否是今天的签到（防止旧数据永久锁定按钮）
        val signDate = kv.decodeString("today_signed_date", "") ?: ""
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return signDate == today
    }

    fun setTodaySigned(signed: Boolean) {
        kv.encode("today_signed", signed)
        if (signed) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            kv.encode("today_signed_date", today)
        } else {
            kv.removeValueForKey("today_signed_date")
        }
    }

    fun getSignStreak(): Int {
        return kv.decodeInt("sign_streak", 0)
    }

    fun setSignStreak(streak: Int) {
        kv.encode("sign_streak", streak)
    }

    fun getHighestLevelCleared(): Int {
        return kv.decodeInt("highest_level_cleared", 0)
    }

    fun setHighestLevelCleared(level: Int) {
        kv.encode("highest_level_cleared", level)
    }

    // --- Endless (叠塔) survival mode best score ---

    fun getEndlessBest(): Int {
        return kv.decodeInt("endless_best", 0)
    }

    fun setEndlessBest(best: Int) {
        kv.encode("endless_best", best)
    }

    fun getLastShownDailyPopupDate(): String {
        return kv.decodeString("last_shown_daily_popup_date", "") ?: ""
    }

    fun setLastShownDailyPopupDate(date: String) {
        kv.encode("last_shown_daily_popup_date", date)
    }
}