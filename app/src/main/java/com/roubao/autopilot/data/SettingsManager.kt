package com.roubao.autopilot.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.roubao.autopilot.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * API 提供商配置
 */
data class ApiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String
) {
    companion object {
        val ALIYUN = ApiProvider(
            id = "aliyun",
            name = "阿里云 (Qwen-VL)",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen3-vl-plus"
        )
        val OPENAI = ApiProvider(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o"
        )
        val OPENROUTER = ApiProvider(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "anthropic/claude-3.5-sonnet"
        )
        val CUSTOM = ApiProvider(
            id = "custom",
            name = "自定义",
            baseUrl = "",
            defaultModel = ""
        )

        val ALL = listOf(ALIYUN, OPENAI, OPENROUTER, CUSTOM)
    }
}

/**
 * 应用设置
 */
/**
 * 默认推荐模型
 */
const val DEFAULT_MODEL = "qwen3-vl-plus"

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = ApiProvider.ALIYUN.baseUrl,
    val model: String = DEFAULT_MODEL,
    val cachedModels: List<String> = emptyList(), // 从 API 获取的模型列表缓存
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hasSeenOnboarding: Boolean = false,
    val maxSteps: Int = 25,
    val cloudCrashReportEnabled: Boolean = true, // 云端崩溃上报，默认开启
    val rootModeEnabled: Boolean = false, // Shizuku Root 模式
    val suCommandEnabled: Boolean = false, // 允许 su -c 命令（需要 Root 模式开启）
    val screenshotMode: ScreenshotMode = ScreenshotMode.SHIZUKU_SCREENCAP // 截图方式
)

enum class ScreenshotMode {
    SHIZUKU_SCREENCAP,
    MEDIA_PROJECTION
}

/**
 * 设置管理器
 */
class SettingsManager(context: Context) {

    // 普通设置存储
    private val prefs: SharedPreferences =
        context.getSharedPreferences("baozi_settings", Context.MODE_PRIVATE)

    // 加密存储（用于敏感数据如 API Key）
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "baozi_secure_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 加密失败时回退到普通存储（不应该发生）
            android.util.Log.e("SettingsManager", "Failed to create encrypted prefs", e)
            prefs
        }
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    init {
        // 迁移旧的明文 API Key 到加密存储
        migrateApiKeyToSecureStorage()
    }

    /**
     * 迁移旧的明文 API Key 到加密存储
     */
    private fun migrateApiKeyToSecureStorage() {
        val oldApiKey = prefs.getString("api_key", null)
        if (!oldApiKey.isNullOrEmpty()) {
            // 保存到加密存储
            securePrefs.edit().putString("api_key", oldApiKey).apply()
            // 删除旧的明文存储
            prefs.edit().remove("api_key").apply()
            android.util.Log.d("SettingsManager", "API Key migrated to secure storage")
        }
    }

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
        val screenshotModeStr =
            prefs.getString("screenshot_mode", ScreenshotMode.SHIZUKU_SCREENCAP.name)
                ?: ScreenshotMode.SHIZUKU_SCREENCAP.name
        val screenshotMode = try {
            ScreenshotMode.valueOf(screenshotModeStr)
        } catch (e: Exception) {
            ScreenshotMode.SHIZUKU_SCREENCAP
        }
        return AppSettings(
            apiKey = securePrefs.getString("api_key", "") ?: "",  // 从加密存储读取
            baseUrl = prefs.getString("base_url", ApiProvider.ALIYUN.baseUrl) ?: ApiProvider.ALIYUN.baseUrl,
            model = prefs.getString("model", ApiProvider.ALIYUN.defaultModel) ?: ApiProvider.ALIYUN.defaultModel,
            cachedModels = prefs.getStringSet("cached_models", emptySet())?.toList() ?: emptyList(),
            themeMode = themeMode,
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false),
            maxSteps = prefs.getInt("max_steps", 25),
            cloudCrashReportEnabled = prefs.getBoolean("cloud_crash_report_enabled", true),
            rootModeEnabled = prefs.getBoolean("root_mode_enabled", false),
            suCommandEnabled = prefs.getBoolean("su_command_enabled", false),
            screenshotMode = screenshotMode
        )
    }

    fun updateApiKey(apiKey: String) {
        securePrefs.edit().putString("api_key", apiKey).apply()  // 存储到加密存储
        _settings.value = _settings.value.copy(apiKey = apiKey)
    }

    fun updateBaseUrl(baseUrl: String) {
        prefs.edit().putString("base_url", baseUrl).apply()
        _settings.value = _settings.value.copy(baseUrl = baseUrl)
    }

    fun updateModel(model: String) {
        prefs.edit().putString("model", model).apply()
        _settings.value = _settings.value.copy(model = model)
    }

    /**
     * 更新缓存的模型列表（从 API 获取后调用）
     */
    fun updateCachedModels(models: List<String>) {
        val distinctModels = models.distinct()
        prefs.edit().putStringSet("cached_models", distinctModels.toSet()).apply()
        _settings.value = _settings.value.copy(cachedModels = distinctModels)
    }

    /**
     * 清空缓存的模型列表
     */
    fun clearCachedModels() {
        prefs.edit().remove("cached_models").apply()
        _settings.value = _settings.value.copy(cachedModels = emptyList())
    }

    /**
     * 选择预设服务商
     */
    fun selectProvider(provider: ApiProvider) {
        if (provider.id == "custom") {
            // 自定义不改变 URL，只标记
            return
        }
        updateBaseUrl(provider.baseUrl)
        updateModel(provider.defaultModel)
        clearCachedModels() // 切换服务商时清空模型缓存
    }

    /**
     * 获取当前服务商
     */
    fun getCurrentProvider(): ApiProvider? {
        val currentUrl = _settings.value.baseUrl
        return ApiProvider.ALL.find { it.baseUrl == currentUrl && it.id != "custom" }
    }

    /**
     * 判断是否使用自定义 URL
     */
    fun isCustomUrl(): Boolean {
        return getCurrentProvider() == null
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.name).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun setOnboardingSeen() {
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
        _settings.value = _settings.value.copy(hasSeenOnboarding = true)
    }

    fun updateMaxSteps(maxSteps: Int) {
        val validSteps = maxSteps.coerceIn(5, 100) // 限制范围 5-100
        prefs.edit().putInt("max_steps", validSteps).apply()
        _settings.value = _settings.value.copy(maxSteps = validSteps)
    }

    fun updateCloudCrashReportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_crash_report_enabled", enabled).apply()
        _settings.value = _settings.value.copy(cloudCrashReportEnabled = enabled)
    }

    fun updateRootModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("root_mode_enabled", enabled).apply()
        _settings.value = _settings.value.copy(rootModeEnabled = enabled)
        // 关闭 Root 模式时，同时关闭 su -c
        if (!enabled) {
            updateSuCommandEnabled(false)
        }
    }

    fun updateSuCommandEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("su_command_enabled", enabled).apply()
        _settings.value = _settings.value.copy(suCommandEnabled = enabled)
    }

    fun updateScreenshotMode(mode: ScreenshotMode) {
        prefs.edit().putString("screenshot_mode", mode.name).apply()
        _settings.value = _settings.value.copy(screenshotMode = mode)
    }
}
