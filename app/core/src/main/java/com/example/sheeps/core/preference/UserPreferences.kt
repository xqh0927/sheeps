package com.example.sheeps.core.preference

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val kv: MMKV,
    @ApplicationContext private val context: Context
) {

    private val securePrefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_user_prefs",
            masterKeyAlias,
            context,
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

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun logout() {
        securePrefs.edit().remove("jwt_token").remove("refresh_token").remove("user_phone").apply()
        kv.removeValueForKey("user_points")
        kv.encode("unlocked_level", 1)
        kv.removeValueForKey("username")
    }
}
