package com.runshare.app.ui.screens

import android.Manifest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runshare.app.data.PreferencesRepository
import com.runshare.app.data.RunDatabase
import com.runshare.app.data.RunEntity
import com.runshare.app.model.LocationPoint
import com.runshare.app.model.MapProvider
import com.runshare.app.model.RunningState
import com.runshare.app.service.LocationService
import com.runshare.app.ui.components.MapViewComposable
import com.runshare.app.ui.components.RunStatsCard
import com.runshare.app.utils.LocationUtils
import kotlinx.coroutines.launch

/**
 * 首页/跑步主界面
 */
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
        }
    }

    // 从服务获取状态
    val runningState by locationService?.runningState?.collectAsState() ?: remember { mutableStateOf(RunningState.IDLE) }
    val currentLocation by locationService?.currentLocation?.collectAsState() ?: remember { mutableStateOf<LocationPoint?>(null) }
    val routePoints by locationService?.routePoints?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val distanceMeters by locationService?.distanceMeters?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val durationMs by locationService?.durationMs?.collectAsState() ?: remember { mutableStateOf(0L) }

    // 权限请求
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

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
            currentLocation = currentLocation,
            routePoints = routePoints,
            showCurrentMarker = true
        )

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
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

        // 底部控制区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
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
                        FilledTonalButton(
                            onClick = { locationService?.pauseRunning() },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "暂停")
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
}
