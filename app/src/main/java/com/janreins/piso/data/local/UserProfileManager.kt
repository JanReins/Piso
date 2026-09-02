package com.janreins.piso.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class UserProfile(
    val displayName: String = "",
    val hasPassword: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT
)

class UserProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("piso_user_profile", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isLocked = MutableStateFlow(hasPassword())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private fun loadProfile(): UserProfile {
        val name = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val hash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        val theme = try {
            ThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
        return UserProfile(
            displayName = name,
            hasPassword = hash.isNotBlank(),
            themeMode = theme
        )
    }

    fun hasProfile(): Boolean {
        return _userProfile.value.displayName.isNotBlank()
    }

    fun hasPassword(): Boolean {
        val hash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        return hash.isNotBlank()
    }

    fun createProfile(displayName: String, password: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_DISPLAY_NAME, displayName.trim())
        if (!password.isNullOrBlank()) {
            editor.putString(KEY_PASSWORD_HASH, hashPassword(password))
        } else {
            editor.remove(KEY_PASSWORD_HASH)
        }
        editor.apply()
        _userProfile.value = loadProfile()
        _isLocked.value = false
    }

    fun updateDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
        _userProfile.value = loadProfile()
    }

    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        if (storedHash.isBlank()) return true
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }

    fun unlock(password: String): Boolean {
        if (verifyPassword(password)) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun lock() {
        if (hasPassword()) {
            _isLocked.value = true
        }
    }

    fun setPassword(newPassword: String) {
        prefs.edit().putString(KEY_PASSWORD_HASH, hashPassword(newPassword)).apply()
        _userProfile.value = loadProfile()
    }

    fun changePassword(oldPass: String, newPass: String): Boolean {
        if (!verifyPassword(oldPass)) return false
        setPassword(newPass)
        return true
    }

    fun removePassword(oldPass: String): Boolean {
        if (!verifyPassword(oldPass)) return false
        prefs.edit().remove(KEY_PASSWORD_HASH).apply()
        _userProfile.value = loadProfile()
        _isLocked.value = false
        return true
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _userProfile.value = loadProfile()
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
        _userProfile.value = loadProfile()
        _isLocked.value = false
    }

    private fun hashPassword(password: String): String {
        val input = "piso_salt_offline_$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
