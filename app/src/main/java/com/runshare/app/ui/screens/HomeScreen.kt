package com.runshare.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.RunEntity
import com.runshare.app.model.LocationPoint
import com.runshare.app.model.MapProvider
import com.runshare.app.model.RunningState
import com.runshare.app.service.LocationService
import com.runshare.app.service.LocationSharingManager
import com.runshare.app.ui.components.MapViewComposable
import com.runshare.app.ui.components.RunStatsCard
import com.runshare.app.utils.LocationUtils
import com.runshare.app.utils.ShareUtils
import kotlinx.coroutines.launch

/**
 * 首页/跑步主界面
 */
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 数据库和偏好设置
    val database = remember { RunDatabase.getInstance(context) }
    val prefsRepository = remember { PreferencesRepository(context) }
    val mapProvider by prefsRepository.mapProvider.collectAsState(initial = MapProvider.OSM)
    val username by prefsRepository.username.collectAsState(initial = "跑步者")
    val serverUrl by prefsRepository.serverUrl.collectAsState(initial = "")
    val isSharingEnabled by prefsRepository.sharingEnabled.collectAsState(initial = false)

    // 位置共享管理器
    val sharingManager = remember { LocationSharingManager(context) }
    val isConnected by sharingManager.isConnected.collectAsState()
    val isSharing by sharingManager.isSharing.collectAsState()
    val friendLocations by sharingManager.friendLocations.collectAsState()

    // 空闲时的当前位置（非跑步时）
    var idleLocation by remember { mutableStateOf<LocationPoint?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 服务绑定
    var locationService by remember { mutableStateOf<LocationService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as LocationService.LocationBinder
                locationService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                locationService = null
                isBound = false
            }
        }
    }

    // 绑定服务
    DisposableEffect(Unit) {
        val intent = Intent(context, LocationService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            if (isBound) {
                context.unbindService(serviceConnection)
            }
            sharingManager.destroy()
        }
    }

    // 从服务获取状态
    val runningState by locationService?.runningState?.collectAsState() ?: remember { mutableStateOf(RunningState.IDLE) }
    val currentLocation by locationService?.currentLocation?.collectAsState() ?: remember { mutableStateOf<LocationPoint?>(null) }
    val routePoints by locationService?.routePoints?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val distanceMeters by locationService?.distanceMeters?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val durationMs by locationService?.durationMs?.collectAsState() ?: remember { mutableStateOf(0L) }

    // 合并位置：跑步时用服务位置，空闲时用单次获取的位置
    val displayLocation = currentLocation ?: idleLocation

    // 权限请求
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            // 权限获取后立即获取当前位置
            getIdleLocation(fusedLocationClient) { point ->
                idleLocation = point
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 空闲时定期获取位置
    LaunchedEffect(hasLocationPermission, runningState) {
        if (hasLocationPermission && runningState == RunningState.IDLE) {
            // 每10秒更新一次空闲位置
            while (true) {
                getIdleLocation(fusedLocationClient) { point ->
                    idleLocation = point
                    // 如果正在共享，上传位置
                    if (isSharing && point != null) {
                        sharingManager.updateLocation(point, isRunning = false)
                    }
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    // 配置共享管理器
    LaunchedEffect(serverUrl, username) {
        val userId = prefsRepository.getOrCreateUserId()
        sharingManager.configure(serverUrl, userId, username)
        
        if (isSharingEnabled && serverUrl.isNotEmpty()) {
            sharingManager.startSharing()
        }
    }

    // 跑步时更新共享位置
    LaunchedEffect(currentLocation, runningState) {
        if (isSharing && currentLocation != null && runningState == RunningState.RUNNING) {
            sharingManager.updateLocation(
                currentLocation!!,
                isRunning = true,
                distance = distanceMeters,
                duration = durationMs
            )
        }
    }

    // 分享弹窗状态
    var showShareDialog by remember { mutableStateOf(false) }

    // 计算配速
    val pace = remember(distanceMeters, durationMs) {
        LocationUtils.calculatePace(distanceMeters, durationMs)
    }

    // 保存跑步记录
    fun saveRun() {
        scope.launch {
            if (routePoints.isNotEmpty() && distanceMeters > 10) { // 至少10米才保存
                val run = RunEntity(
                    startTime = routePoints.first().timestamp,
                    endTime = routePoints.last().timestamp,
                    distanceMeters = distanceMeters,
                    durationMs = durationMs,
                    avgPaceMinPerKm = pace,
                    routePointsJson = LocationPoint.listToJson(routePoints)
                )
                database.runDao().insert(run)
            }
            locationService?.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图层
        MapViewComposable(
            modifier = Modifier.fillMaxSize(),
            mapProvider = mapProvider,
            currentLocation = displayLocation,
            routePoints = routePoints,
            showCurrentMarker = true
        )

        // 顶部栏 - 添加状态栏间距
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding() + 8.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 历史记录按钮
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = "历史记录",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 共享状态指示器
            if (isSharing) {
                Row(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) "共享中" else "离线",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // 设置按钮
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 分享位置按钮（右下角）
        FloatingActionButton(
            onClick = { showShareDialog = true },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(Icons.Filled.Share, contentDescription = "分享位置")
        }

        // 底部控制区
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding.calculateBottomPadding() + 16.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 统计卡片（跑步中显示）
            if (runningState != RunningState.IDLE) {
                RunStatsCard(
                    distance = String.format("%.2f", distanceMeters / 1000.0),
                    duration = LocationUtils.formatDuration(durationMs),
                    pace = LocationUtils.formatPace(pace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (runningState) {
                    RunningState.IDLE -> {
                        // 开始跑步按钮
                        Button(
                            onClick = {
                                if (hasLocationPermission) {
                                    LocationService.startService(context)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(8.dp, CircleShape),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "开始",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    RunningState.RUNNING -> {
                        // 暂停按钮
                        Button(
                            onClick = { locationService?.pauseRunning() },
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(6.dp, CircleShape),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50) // 绿色
                            )
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "暂停")
                        }

                        // 停止按钮
                        Button(
                            onClick = {
                                locationService?.stopRunning()
                                saveRun()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(6.dp, CircleShape),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336) // 红色
                            )
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "停止")
                        }
                    }

                    RunningState.PAUSED -> {
                        // 继续按钮
                        Button(
                            onClick = { locationService?.resumeRunning() },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "继续")
                        }

                        // 停止按钮
                        Button(
                            onClick = {
                                locationService?.stopRunning()
                                saveRun()
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "停止")
                        }
                    }

                    RunningState.STOPPED -> {
                        // 自动重置
                        LaunchedEffect(Unit) {
                            locationService?.resetState()
                        }
                    }
                }
            }
        }
    }

    // 分享位置弹窗
    if (showShareDialog) {
        ShareLocationDialog(
            onDismiss = { showShareDialog = false },
            shareLink = sharingManager.generateShareLink(),
            username = username,
            isSharing = isSharing,
            onToggleSharing = { enabled ->
                scope.launch {
                    prefsRepository.setSharingEnabled(enabled)
                    if (enabled) {
                        sharingManager.startSharing()
                    } else {
                        sharingManager.stopSharing()
                    }
                }
            }
        )
    }
}

/**
 * 获取空闲时的当前位置
 */
@SuppressLint("MissingPermission")
private fun getIdleLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onResult: (LocationPoint?) -> Unit
) {
    val cancellationToken = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(
                    LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        speed = location.speed,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                onResult(null)
            }
        }
        .addOnFailureListener {
            onResult(null)
        }
}

/**
 * 分享位置弹窗
 */
@Composable
fun ShareLocationDialog(
    onDismiss: () -> Unit,
    shareLink: String,
    username: String,
    isSharing: Boolean,
    onToggleSharing: (Boolean) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享我的位置") },
        text = {
            Column {
                // 共享开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("开启位置共享")
                    Switch(
                        checked = isSharing,
                        onCheckedChange = onToggleSharing
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "用户名: $username",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 分享链接
                OutlinedTextField(
                    value = shareLink,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分享链接") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 分享按钮
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "来看看我的实时位置吧！\n$shareLink")
                        }
                        context.startActivity(Intent.createChooser(intent, "分享位置"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享链接")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
