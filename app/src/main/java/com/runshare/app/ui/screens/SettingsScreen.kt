package com.runshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.model.MapProvider
import kotlinx.coroutines.launch

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = remember { PreferencesRepository(context) }

    val mapProvider by prefsRepository.mapProvider.collectAsState(initial = MapProvider.OSM)
    val keepScreenOn by prefsRepository.keepScreenOn.collectAsState(initial = true)
    val voicePrompt by prefsRepository.voicePromptEnabled.collectAsState(initial = false)
    val shareDuration by prefsRepository.shareDurationMinutes.collectAsState(initial = 30)
    
    // 新增：用户和服务器设置
    val username by prefsRepository.username.collectAsState(initial = "跑步者")
    val serverUrl by prefsRepository.serverUrl.collectAsState(initial = "")

    var showMapProviderDialog by remember { mutableStateOf(false) }
    var showShareDurationDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 地图设置
            SettingsSection(title = "地图设置") {
                SettingsItem(
                    icon = Icons.Filled.Map,
                    title = "地图提供商",
                    subtitle = mapProvider.displayName,
                    onClick = { showMapProviderDialog = true }
                )
            }

            // 跑步设置
            SettingsSection(title = "跑步设置") {
                SettingsSwitchItem(
                    icon = Icons.Filled.ScreenLockPortrait,
                    title = "保持屏幕常亮",
                    subtitle = "跑步时保持屏幕不关闭",
                    checked = keepScreenOn,
                    onCheckedChange = {
                        scope.launch { prefsRepository.setKeepScreenOn(it) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = Icons.Filled.VolumeUp,
                    title = "语音播报",
                    subtitle = "每公里语音播报跑步数据",
                    checked = voicePrompt,
                    onCheckedChange = {
                        scope.launch { prefsRepository.setVoicePrompt(it) }
                    }
                )
            }

            // 分享设置
            SettingsSection(title = "分享设置") {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    title = "用户名",
                    subtitle = username,
                    onClick = { showUsernameDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Filled.Cloud,
                    title = "服务器地址",
                    subtitle = if (serverUrl.isEmpty()) "未设置（离线模式）" else serverUrl,
                    onClick = { showServerDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Filled.Timer,
                    title = "分享时长",
                    subtitle = "${shareDuration}分钟",
                    onClick = { showShareDurationDialog = true }
                )
            }

            // 关于
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "版本",
                    subtitle = "1.0.0",
                    onClick = {}
                )
            }
        }

        // 地图提供商选择对话框
        if (showMapProviderDialog) {
            AlertDialog(
                onDismissRequest = { showMapProviderDialog = false },
                title = { Text("选择地图") },
                text = {
                    Column {
                        MapProvider.entries.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            prefsRepository.setMapProvider(provider)
                                            showMapProviderDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = provider == mapProvider,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(provider.displayName)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMapProviderDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 分享时长选择对话框
        if (showShareDurationDialog) {
            val durations = listOf(15, 30, 60, 120, -1) // -1代表持续分享
            val durationLabels = listOf("15分钟", "30分钟", "1小时", "2小时", "持续分享")

            AlertDialog(
                onDismissRequest = { showShareDurationDialog = false },
                title = { Text("分享时长") },
                text = {
                    Column {
                        durations.forEachIndexed { index, duration ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            prefsRepository.setShareDuration(duration)
                                            showShareDurationDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = duration == shareDuration,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(durationLabels[index])
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShareDurationDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 用户名编辑对话框
        if (showUsernameDialog) {
            var inputValue by remember { mutableStateOf(username) }
            AlertDialog(
                onDismissRequest = { showUsernameDialog = false },
                title = { Text("设置用户名") },
                text = {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                prefsRepository.setUsername(inputValue)
                                showUsernameDialog = false
                            }
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUsernameDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 服务器地址编辑对话框
        if (showServerDialog) {
            var inputValue by remember { mutableStateOf(serverUrl) }
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = { Text("设置服务器地址") },
                text = {
                    Column {
                        Text(
                            text = "输入你的服务器WebSocket地址，留空则使用离线模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            label = { Text("服务器地址") },
                            placeholder = { Text("例如: http://192.168.1.100:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                prefsRepository.setServerUrl(inputValue)
                                showServerDialog = false
                            }
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}
