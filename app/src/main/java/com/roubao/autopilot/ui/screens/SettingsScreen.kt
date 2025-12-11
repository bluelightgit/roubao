package com.roubao.autopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.roubao.autopilot.data.ApiProvider
import com.roubao.autopilot.data.AppSettings
import com.roubao.autopilot.ui.theme.BaoziTheme
import com.roubao.autopilot.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateApiKey: (String) -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onUpdateModel: (String) -> Unit,
    onAddCustomModel: (String) -> Unit,
    onRemoveCustomModel: (String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateMaxSteps: (Int) -> Unit,
    allModels: List<String>,
    shizukuAvailable: Boolean
) {
    val colors = BaoziTheme.colors
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMaxStepsDialog by remember { mutableStateOf(false) }
    var showShizukuHelpDialog by remember { mutableStateOf(false) }
    var showOverlayHelpDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 顶部标题
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = "设置",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = "配置 API 和应用选项",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // 连接状态卡片
        item {
            StatusCard(shizukuAvailable = shizukuAvailable)
        }

        // 外观设置分组
        item {
            SettingsSection(title = "外观")
        }

        // 主题模式设置
        item {
            SettingsItem(
                icon = if (colors.isDark) Icons.Default.Star else Icons.Outlined.Star,
                title = "主题模式",
                subtitle = when (settings.themeMode) {
                    ThemeMode.LIGHT -> "浅色模式"
                    ThemeMode.DARK -> "深色模式"
                    ThemeMode.SYSTEM -> "跟随系统"
                },
                onClick = { showThemeDialog = true }
            )
        }

        // 执行设置分组
        item {
            SettingsSection(title = "执行设置")
        }

        // 最大步数设置
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "最大执行步数",
                subtitle = "${settings.maxSteps} 步",
                onClick = { showMaxStepsDialog = true }
            )
        }

        // API 设置分组
        item {
            SettingsSection(title = "API 配置")
        }

        // Base URL 设置
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "API 服务商",
                subtitle = ApiProvider.ALL.find { it.baseUrl == settings.baseUrl }?.name ?: "自定义",
                onClick = { }
            ) {
                ProviderSelector(
                    currentUrl = settings.baseUrl,
                    onSelect = { onUpdateBaseUrl(it.baseUrl) }
                )
            }
        }

        // 模型设置
        item {
            SettingsItem(
                icon = Icons.Default.Build,
                title = "模型",
                subtitle = settings.model,
                onClick = { showModelDialog = true }
            )
        }

        // API Key 设置
        item {
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "API Key",
                subtitle = if (settings.apiKey.isNotEmpty()) "已设置 (${maskApiKey(settings.apiKey)})" else "未设置",
                onClick = { showApiKeyDialog = true }
            )
        }

        // 自定义模型管理
        item {
            SettingsSection(title = "自定义模型")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Add,
                title = "添加自定义模型",
                subtitle = "添加其他支持的模型",
                onClick = { showAddModelDialog = true }
            )
        }

        // 显示已添加的自定义模型
        settings.customModels.forEach { model ->
            item {
                CustomModelItem(
                    model = model,
                    isSelected = model == settings.model,
                    onSelect = { onUpdateModel(model) },
                    onDelete = { onRemoveCustomModel(model) }
                )
            }
        }

        // 帮助分组
        item {
            SettingsSection(title = "帮助")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Shizuku 使用指南",
                subtitle = "了解如何安装和配置 Shizuku",
                onClick = { showShizukuHelpDialog = true }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "悬浮窗权限说明",
                subtitle = "了解为什么需要悬浮窗权限",
                onClick = { showOverlayHelpDialog = true }
            )
        }

        // 关于分组
        item {
            SettingsSection(title = "关于")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "版本",
                subtitle = "1.0.0",
                onClick = { }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Build,
                title = "肉包 Autopilot",
                subtitle = "基于视觉语言模型的 Android 自动化工具",
                onClick = { }
            )
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectDialog(
            currentTheme = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                onUpdateThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    // 最大步数设置对话框
    if (showMaxStepsDialog) {
        MaxStepsDialog(
            currentSteps = settings.maxSteps,
            onDismiss = { showMaxStepsDialog = false },
            onConfirm = {
                onUpdateMaxSteps(it)
                showMaxStepsDialog = false
            }
        )
    }

    // API Key 编辑对话框
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = settings.apiKey,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = {
                onUpdateApiKey(it)
                showApiKeyDialog = false
            }
        )
    }

    // 模型选择对话框
    if (showModelDialog) {
        ModelSelectDialog(
            currentModel = settings.model,
            models = allModels,
            onDismiss = { showModelDialog = false },
            onSelect = {
                onUpdateModel(it)
                showModelDialog = false
            }
        )
    }

    // 添加自定义模型对话框
    if (showAddModelDialog) {
        AddModelDialog(
            onDismiss = { showAddModelDialog = false },
            onConfirm = {
                onAddCustomModel(it)
                showAddModelDialog = false
            }
        )
    }

    // Shizuku 帮助对话框
    if (showShizukuHelpDialog) {
        ShizukuHelpDialog(onDismiss = { showShizukuHelpDialog = false })
    }

    // 悬浮窗权限帮助对话框
    if (showOverlayHelpDialog) {
        OverlayHelpDialog(onDismiss = { showOverlayHelpDialog = false })
    }
}

@Composable
fun StatusCard(shizukuAvailable: Boolean) {
    val colors = BaoziTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (shizukuAvailable) colors.success.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (shizukuAvailable) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (shizukuAvailable) colors.success else colors.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (shizukuAvailable) "Shizuku 已连接" else "Shizuku 未连接",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (shizukuAvailable) colors.success else colors.error
                )
                Text(
                    text = if (shizukuAvailable) "设备控制功能可用" else "请启动 Shizuku 并授权",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    val colors = BaoziTheme.colors
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = colors.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = BaoziTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textHint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProviderSelector(
    currentUrl: String,
    onSelect: (ApiProvider) -> Unit
) {
    val colors = BaoziTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ApiProvider.ALL.forEach { provider ->
            val isSelected = provider.baseUrl == currentUrl
            Surface(
                modifier = Modifier.clickable { onSelect(provider) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.primary else colors.backgroundInput
            ) {
                Text(
                    text = provider.name.split(" ").first(),
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CustomModelItem(
    model: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = BaoziTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.backgroundCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(2.dp, colors.textHint, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = model,
                fontSize = 14.sp,
                color = if (isSelected) colors.primary else colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = colors.textHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ThemeSelectDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    val colors = BaoziTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("选择主题", color = colors.textPrimary)
        },
        text = {
            Column {
                listOf(
                    ThemeMode.LIGHT to "浅色模式",
                    ThemeMode.DARK to "深色模式",
                    ThemeMode.SYSTEM to "跟随系统"
                ).forEach { (mode, label) ->
                    val isSelected = mode == currentTheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(mode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, colors.textHint, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun ApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = BaoziTheme.colors
    var key by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("API Key", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "请输入您的 API Key",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.backgroundInput)
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = key,
                            onValueChange = { key = it },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp)
                        )
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(
                                text = if (showKey) "隐藏" else "显示",
                                fontSize = 12.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key) }) {
                Text("确定", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun ModelSelectDialog(
    currentModel: String,
    models: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = BaoziTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("选择模型", color = colors.textPrimary)
        },
        text = {
            Column {
                models.forEach { model ->
                    val isSelected = model == currentModel
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(model) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, colors.textHint, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = model,
                                fontSize = 14.sp,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun AddModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = BaoziTheme.colors
    var modelName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("添加自定义模型", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "输入模型名称或 ID",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.backgroundInput)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (modelName.isEmpty()) {
                        Text(
                            text = "例如: gpt-4-vision",
                            color = colors.textHint,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (modelName.isNotBlank()) onConfirm(modelName.trim()) },
                enabled = modelName.isNotBlank()
            ) {
                Text("添加", color = if (modelName.isNotBlank()) colors.primary else colors.textHint)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}

private fun maskApiKey(key: String): String {
    return if (key.length > 8) {
        "${key.take(4)}****${key.takeLast(4)}"
    } else {
        "****"
    }
}

@Composable
fun ShizukuHelpDialog(onDismiss: () -> Unit) {
    val colors = BaoziTheme.colors
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("Shizuku 使用指南", color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                HelpStep(
                    number = "1",
                    title = "下载 Shizuku",
                    description = "从 Google Play 或 GitHub 下载 Shizuku 应用"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 下载按钮
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往下载 Shizuku", color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "2",
                    title = "启动 Shizuku",
                    description = "打开 Shizuku 应用，根据您的设备选择启动方式：\n\n• 无线调试（推荐）：需要 Android 11+，在开发者选项中开启无线调试\n• 连接电脑：通过 ADB 命令启动"
                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "3",
                    title = "授权肉包",
                    description = "在 Shizuku 的「应用管理」中找到「肉包」，点击授权按钮"
                )
                Spacer(modifier = Modifier.height(16.dp))
                HelpStep(
                    number = "4",
                    title = "开始使用",
                    description = "授权完成后，返回肉包应用，即可开始使用"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = colors.primary)
            }
        }
    )
}

@Composable
fun OverlayHelpDialog(onDismiss: () -> Unit) {
    val colors = BaoziTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("悬浮窗权限说明", color = colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "为什么需要悬浮窗权限？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "肉包在执行任务时需要显示悬浮窗来：",
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint("显示当前执行进度")
                BulletPoint("提供停止按钮，随时中断任务")
                BulletPoint("在其他应用上方显示状态信息")

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "如何开启？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 点击执行任务时会自动提示\n2. 或前往：设置 > 应用 > 肉包 > 悬浮窗权限\n3. 开启「允许显示在其他应用上层」",
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "隐私安全",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "悬浮窗仅在任务执行期间显示，不会收集任何个人信息。任务完成后悬浮窗会自动消失。",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = colors.primary)
            }
        }
    )
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    description: String
) {
    val colors = BaoziTheme.colors
    Row {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    val colors = BaoziTheme.colors
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = colors.textPrimary
        )
    }
}

@Composable
fun MaxStepsDialog(
    currentSteps: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = BaoziTheme.colors
    var steps by remember { mutableStateOf(currentSteps.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundCard,
        title = {
            Text("最大执行步数", color = colors.textPrimary)
        },
        text = {
            Column {
                Text(
                    text = "设置 Agent 单次任务的最大执行步数。步数越多，能完成的任务越复杂，但消耗的 token 也越多。",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 当前值显示
                Text(
                    text = "${steps.toInt()} 步",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 滑块
                Slider(
                    value = steps,
                    onValueChange = { steps = it },
                    valueRange = 5f..100f,
                    steps = 18, // (100-5)/5 - 1 = 18 个刻度点，每 5 步一个
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.backgroundInput
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 范围提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "5",
                        fontSize = 12.sp,
                        color = colors.textHint
                    )
                    Text(
                        text = "100",
                        fontSize = 12.sp,
                        color = colors.textHint
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 快捷选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 50).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { steps = preset.toFloat() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (steps.toInt() == preset) colors.primary else colors.backgroundInput
                        ) {
                            Text(
                                text = "$preset",
                                fontSize = 14.sp,
                                color = if (steps.toInt() == preset) Color.White else colors.textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(steps.toInt()) }) {
                Text("确定", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.textSecondary)
            }
        }
    )
}
