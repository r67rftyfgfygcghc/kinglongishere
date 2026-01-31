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
import androidx.compose.ui.unit.dp
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
import com.runshare.app.ui.components.*
import com.runshare.app.utils.LocationUtils
import kotlinx.coroutines.launch

/**
 * 首页/跑步主界面 - 参考导航App设计
 */
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToCheckIn: () -> Unit = {},
    onNavigateToGroup: () -> Unit = {}
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

    // 空闲时的当前位置
    var idleLocation by remember { mutableStateOf<LocationPoint?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isLocating by remember { mutableStateOf(true) }

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

    // 合并位置
    val displayLocation = currentLocation ?: idleLocation

    // 权限请求
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            getIdleLocation(fusedLocationClient) { point ->
                idleLocation = point
                isLocating = false
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
            while (true) {
                getIdleLocation(fusedLocationClient) { point ->
                    idleLocation = point
                    isLocating = false
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
    var showLayersDialog by remember { mutableStateOf(false) }

    // 计算配速
    val pace = remember(distanceMeters, durationMs) {
        LocationUtils.calculatePace(distanceMeters, durationMs)
    }

    // 保存跑步记录
    fun saveRun() {
        scope.launch {
            if (routePoints.isNotEmpty() && distanceMeters > 10) {
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

    // 地图居中回调
    var mapCenterCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图层
        MapViewComposable(
            modifier = Modifier.fillMaxSize(),
            mapProvider = mapProvider,
            currentLocation = displayLocation,
            routePoints = routePoints,
            showCurrentMarker = true,
            onMapReady = { centerCallback ->
                mapCenterCallback = centerCallback
            }
        )

        // 顶部GPS信息栏
        TopInfoBar(
            currentLocation = displayLocation,
            isLocating = isLocating,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 右侧垂直工具栏
        RightToolbar(
            onSearchClick = { /* TODO */ },
            onLayersClick = { showLayersDialog = true },
            onTeamClick = onNavigateToGroup,
            onToolsClick = onNavigateToHistory,
            onLeaderboardClick = onNavigateToLeaderboard,
            onCheckInClick = onNavigateToCheckIn,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        // 左侧浮动按钮
        LeftFloatingButtons(
            onCenterClick = {
                mapCenterCallback?.invoke()
            },
            onOrientationClick = { /* TODO */ },
            onModeClick = { /* TODO */ },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

        // 底部控制面板
        BottomControlPanel(
            runningState = runningState,
            distanceKm = distanceMeters / 1000.0,
            durationFormatted = LocationUtils.formatDuration(durationMs),
            paceFormatted = LocationUtils.formatPace(pace),
            onSettingsClick = onNavigateToSettings,
            onStartClick = {
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
            onPauseClick = { locationService?.pauseRunning() },
            onResumeClick = { locationService?.resumeRunning() },
            onStopClick = {
                locationService?.stopRunning()
                saveRun()
            },
            onShareClick = { showShareDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 自动重置状态
        if (runningState == RunningState.STOPPED) {
            LaunchedEffect(Unit) {
                locationService?.resetState()
            }
        }
    }

    // 分享弹窗
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

    // 图层选择弹窗
    if (showLayersDialog) {
        MapLayersDialog(
            currentProvider = mapProvider,
            onProviderSelected = { provider ->
                scope.launch {
                    prefsRepository.setMapProvider(provider)
                }
                showLayersDialog = false
            },
            onDismiss = { showLayersDialog = false }
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

                OutlinedTextField(
                    value = shareLink,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分享链接") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

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

/**
 * 地图图层选择弹窗
 */
@Composable
fun MapLayersDialog(
    currentProvider: MapProvider,
    onProviderSelected: (MapProvider) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择地图图层") },
        text = {
            Column {
                MapProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (provider == currentProvider)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == currentProvider,
                            onClick = { onProviderSelected(provider) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(provider.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
