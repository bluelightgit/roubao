package com.roubao.autopilot.data

import android.content.Context
import android.content.SharedPreferences
import com.roubao.autopilot.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * API 提供商配置
 */
data class ApiProvider(
    val name: String,
    val baseUrl: String,
    val defaultModel: String
) {
    companion object {
        val ALIYUN = ApiProvider(
            name = "阿里云 (Qwen-VL)",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen3-vl-plus"
        )
        val MODELSCOPE = ApiProvider(
            name = "ModelScope",
            baseUrl = "https://api-inference.modelscope.cn/v1",
            defaultModel = "iic/GUI-Owl-7B"
        )

        val ALL = listOf(ALIYUN, MODELSCOPE)
    }
}

/**
 * 应用设置
 */
data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = ApiProvider.ALIYUN.baseUrl,
    val model: String = ApiProvider.ALIYUN.defaultModel,
    val customModels: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hasSeenOnboarding: Boolean = false,
    val maxSteps: Int = 25
)

/**
 * 设置管理器
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("baozi_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
        return AppSettings(
            apiKey = prefs.getString("api_key", AppSettings().apiKey) ?: AppSettings().apiKey,
            baseUrl = prefs.getString("base_url", AppSettings().baseUrl) ?: AppSettings().baseUrl,
            model = prefs.getString("model", AppSettings().model) ?: AppSettings().model,
            customModels = prefs.getStringSet("custom_models", emptySet())?.toList() ?: emptyList(),
            themeMode = themeMode,
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false),
            maxSteps = prefs.getInt("max_steps", 25)
        )
    }

    fun updateApiKey(apiKey: String) {
        prefs.edit().putString("api_key", apiKey).apply()
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

    fun addCustomModel(model: String) {
        val newModels = (_settings.value.customModels + model).distinct()
        prefs.edit().putStringSet("custom_models", newModels.toSet()).apply()
        _settings.value = _settings.value.copy(customModels = newModels)
    }

    fun removeCustomModel(model: String) {
        val newModels = _settings.value.customModels - model
        prefs.edit().putStringSet("custom_models", newModels.toSet()).apply()
        _settings.value = _settings.value.copy(customModels = newModels)
    }

    fun selectProvider(provider: ApiProvider) {
        updateBaseUrl(provider.baseUrl)
        updateModel(provider.defaultModel)
    }

    fun getCurrentProvider(): ApiProvider? {
        return ApiProvider.ALL.find { it.baseUrl == _settings.value.baseUrl }
    }

    fun getAllModels(): List<String> {
        val builtIn = listOf(
            "qwen3-vl-plus",
            "qwen-vl-max",
            "qwen-vl-plus",
            "iic/GUI-Owl-7B"
        )
        return (builtIn + _settings.value.customModels).distinct()
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
}
