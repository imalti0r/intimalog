package com.intima.log.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.intima.log.data.db.Gender
import com.intima.log.data.db.StatsBasis
import com.intima.log.data.db.UserProfile
import com.intima.log.data.security.SecurityManager
import com.intima.log.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile")

/**
 * 用户偏好仓库。PIN 哈希不入 DataStore（明文偏好文件），改存加密 SharedPreferences。
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val context: Context,
) {

    /**
     * 切换桌面图标别名：伪装开→显示「日常」alias、禁用真名 alias；关→反之。
     * PackageManager.setComponentEnabledMode 会刷新启动器（桌面图标名即时生效）。
     */
    suspend fun applyLauncherAlias(disguiseEnabled: Boolean) {
        val pm = context.packageManager
        val real = android.content.ComponentName(context, "com.intima.log.ui.MainActivityReal")
        val disguise = android.content.ComponentName(context, "com.intima.log.ui.MainActivityDisguise")
        pm.setComponentEnabledSetting(
            disguise,
            if (disguiseEnabled) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            real,
            if (disguiseEnabled) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            android.content.pm.PackageManager.DONT_KILL_APP,
        )
    }

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val gender = stringPreferencesKey("gender")
        val statsBasis = stringPreferencesKey("stats_basis")
        val enableMultiPartner = booleanPreferencesKey("enable_multi_partner")
        val lockEnabled = booleanPreferencesKey("lock_enabled")
        val disguiseEnabled = booleanPreferencesKey("disguise_enabled")
        val disguiseIcon = stringPreferencesKey("disguise_icon")
        val strongDisguise = booleanPreferencesKey("strong_disguise")
        val screenshotAllowed = booleanPreferencesKey("screenshot_allowed")
        val cycleModuleEnabled = booleanPreferencesKey("cycle_module_enabled")
        val weeklyReminder = booleanPreferencesKey("weekly_reminder")
        val themeMode = stringPreferencesKey("theme_mode")          // SYSTEM / LIGHT / DARK
        val splitCounting = booleanPreferencesKey("split_counting") // 统计口径：拆分段计数
        val webdavUrl = stringPreferencesKey("webdav_url")
        val webdavUser = stringPreferencesKey("webdav_user")
        val webdavPassEnc = stringPreferencesKey("webdav_pass_enc") // Keystore AES 加密后 base64
        val webdavDir = stringPreferencesKey("webdav_dir")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { p ->
        UserProfile(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            gender = p[Keys.gender]?.let { runCatching { Gender.valueOf(it) }.getOrNull() } ?: Gender.UNDISCLOSED,
            statsBasis = p[Keys.statsBasis]?.let { runCatching { StatsBasis.valueOf(it) }.getOrNull() } ?: StatsBasis.SESSION,
            enableMultiPartner = p[Keys.enableMultiPartner] ?: true,
            lockEnabled = p[Keys.lockEnabled] ?: false,
            disguiseEnabled = p[Keys.disguiseEnabled] ?: false,
            disguiseIcon = p[Keys.disguiseIcon] ?: "calculator",
            strongDisguise = p[Keys.strongDisguise] ?: false,
            screenshotAllowed = p[Keys.screenshotAllowed] ?: false,
            cycleModuleEnabled = p[Keys.cycleModuleEnabled] ?: false,
            weeklyReminder = p[Keys.weeklyReminder] ?: false,
            pinHash = null,
            pinSalt = null,
        )
    }

    suspend fun setOnboardingDone(done: Boolean) = context.profileDataStore.edit { it[Keys.onboardingDone] = done }
    suspend fun setGender(gender: Gender) = context.profileDataStore.edit { it[Keys.gender] = gender.name }
    suspend fun setStatsBasis(basis: StatsBasis) = context.profileDataStore.edit { it[Keys.statsBasis] = basis.name }
    suspend fun setMultiPartnerEnabled(enabled: Boolean) = context.profileDataStore.edit { it[Keys.enableMultiPartner] = enabled }
    suspend fun setLockEnabled(enabled: Boolean) = context.profileDataStore.edit { it[Keys.lockEnabled] = enabled }
    suspend fun setDisguise(enabled: Boolean, icon: String? = null) = context.profileDataStore.edit {
        it[Keys.disguiseEnabled] = enabled
        if (icon != null) it[Keys.disguiseIcon] = icon
    }
    suspend fun setStrongDisguise(enabled: Boolean) = context.profileDataStore.edit { it[Keys.strongDisguise] = enabled }
    suspend fun setScreenshotAllowed(allowed: Boolean) = context.profileDataStore.edit { it[Keys.screenshotAllowed] = allowed }
    suspend fun setCycleModuleEnabled(enabled: Boolean) = context.profileDataStore.edit { it[Keys.cycleModuleEnabled] = enabled }
    suspend fun setWeeklyReminder(enabled: Boolean) = context.profileDataStore.edit { it[Keys.weeklyReminder] = enabled }

    // ---- 夜间模式 ----

    fun themeModeFlow(): Flow<ThemeMode> = context.profileDataStore.data.map { p ->
        p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.profileDataStore.edit { it[Keys.themeMode] = mode.name }

    // ---- 拆分计数口径 ----

    fun splitCountingFlow(): Flow<Boolean> = context.profileDataStore.data.map { p ->
        p[Keys.splitCounting] ?: true
    }

    suspend fun setSplitCounting(count: Boolean) = context.profileDataStore.edit { it[Keys.splitCounting] = count }

    // ---- WebDAV 同步配置 ----

    data class WebDavConfig(val url: String, val user: String, val password: String, val dir: String)

    fun webDavConfigFlow(): Flow<WebDavConfig?> = context.profileDataStore.data.map { p ->
        val url = p[Keys.webdavUrl] ?: return@map null
        val passEnc = p[Keys.webdavPassEnc] ?: return@map null
        val pass = runCatching { SecurityManager.decryptWithKeystore(passEnc) }.getOrNull() ?: return@map null
        WebDavConfig(url, p[Keys.webdavUser] ?: "", pass, p[Keys.webdavDir] ?: "intimalog")
    }

    suspend fun setWebDavConfig(url: String, user: String, password: String, dir: String) =
        context.profileDataStore.edit {
            it[Keys.webdavUrl] = url.trim().trimEnd('/')
            it[Keys.webdavUser] = user
            it[Keys.webdavPassEnc] = SecurityManager.encryptWithKeystore(password)
            it[Keys.webdavDir] = dir.ifBlank { "intimalog" }
        }

    suspend fun clearWebDavConfig() = context.profileDataStore.edit {
        it.remove(Keys.webdavUrl); it.remove(Keys.webdavUser); it.remove(Keys.webdavPassEnc)
    }

    // ---- PIN（存加密 SharedPreferences，不进 DataStore） ----

    fun hasPin(): Boolean {
        val prefs = context.getSharedPreferences("intima_pin", Context.MODE_PRIVATE)
        return prefs.contains("pin_hash")
    }

    suspend fun setPin(pin: String) {
        val ph = SecurityManager.hashPin(pin)
        context.getSharedPreferences("intima_pin", Context.MODE_PRIVATE).edit()
            .putString("pin_hash", ph.hash)
            .putString("pin_salt", ph.salt)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val prefs = context.getSharedPreferences("intima_pin", Context.MODE_PRIVATE)
        val hash = prefs.getString("pin_hash", null) ?: return false
        val salt = prefs.getString("pin_salt", null) ?: return false
        return SecurityManager.verifyPin(pin, hash, salt)
    }
}
