package com.janreins.piso.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class UserProfile(
    val displayName: String = "",
    val hasPin: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT
)

class UserProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("piso_user_profile", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isLocked = MutableStateFlow(hasPin())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private fun loadProfile(): UserProfile {
        val name = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        val theme = try {
            ThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
        return UserProfile(
            displayName = name,
            hasPin = hash.isNotBlank(),
            themeMode = theme
        )
    }

    fun hasProfile(): Boolean {
        return _userProfile.value.displayName.isNotBlank()
    }

    fun hasPin(): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return hash.isNotBlank()
    }

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(KEY_PIN_SALT, null)
        if (salt.isNullOrBlank()) {
            salt = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt
    }

    private fun hashPin(pin: String): String {
        val salt = getOrCreateSalt()
        val input = "piso_salt_${salt}_pin_$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun createProfile(displayName: String, pin: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_DISPLAY_NAME, displayName.trim())
        if (!pin.isNullOrBlank() && pin.length == 4) {
            editor.putString(KEY_PIN_HASH, hashPin(pin))
        } else {
            editor.remove(KEY_PIN_HASH)
        }
        editor.apply()
        _userProfile.value = loadProfile()
        _isLocked.value = false
    }

    fun updateDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
        _userProfile.value = loadProfile()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isBlank()) return true
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    fun unlock(pin: String): Boolean {
        if (verifyPin(pin)) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun lock() {
        if (hasPin()) {
            _isLocked.value = true
        }
    }

    fun setPin(newPin: String) {
        if (newPin.length == 4) {
            prefs.edit().putString(KEY_PIN_HASH, hashPin(newPin)).apply()
            _userProfile.value = loadProfile()
        }
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        setPin(newPin)
        return true
    }

    fun removePin(oldPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        prefs.edit().remove(KEY_PIN_HASH).apply()
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

    companion object {
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
