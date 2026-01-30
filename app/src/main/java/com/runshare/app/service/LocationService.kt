package com.runshare.app.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.runshare.app.MainActivity
import com.runshare.app.R
import com.runshare.app.RunShareApp
import com.runshare.app.model.LocationPoint
import com.runshare.app.model.RunningState
import com.runshare.app.utils.LocationUtils
import com.runshare.app.utils.LocationUtils.toLocationPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 位置追踪前台服务
 */
class LocationService : Service() {

    private val binder = LocationBinder()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 跑步状态
    private val _runningState = MutableStateFlow(RunningState.IDLE)
    val runningState: StateFlow<RunningState> = _runningState.asStateFlow()

    // 轨迹点列表
    private val _routePoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val routePoints: StateFlow<List<LocationPoint>> = _routePoints.asStateFlow()

    // 当前位置
    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()

    // 距离（米）
    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    // 持续时间（毫秒）
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // 开始时间
    private var startTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L

    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRunning()
            ACTION_PAUSE -> pauseRunning()
            ACTION_RESUME -> resumeRunning()
            ACTION_STOP -> stopRunning()
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val point = location.toLocationPoint()
                    _currentLocation.value = point

                    if (_runningState.value == RunningState.RUNNING) {
                        val currentPoints = _routePoints.value.toMutableList()

                        // 计算距离增量
                        if (currentPoints.isNotEmpty()) {
                            val lastPoint = currentPoints.last()
                            val distance = LocationUtils.calculateDistance(lastPoint, point)

                            // 过滤掉异常跳跃（>100m/s不太可能）
                            val timeDiff = (point.timestamp - lastPoint.timestamp) / 1000.0
                            if (timeDiff > 0 && distance / timeDiff < 100) {
                                _distanceMeters.value += distance
                            }
                        }

                        currentPoints.add(point)
                        _routePoints.value = currentPoints

                        // 更新持续时间
                        _durationMs.value = System.currentTimeMillis() - startTime - pausedDuration

                        // 更新通知
                        updateNotification()
                    }
                }
            }
        }
    }

    /**
     * 开始跑步
     */
    fun startRunning() {
        if (_runningState.value != RunningState.IDLE) return

        // 启动前台服务
        startForegroundWithNotification()

        // 开始位置更新
        startLocationUpdates()

        startTime = System.currentTimeMillis()
        pausedDuration = 0L
        _routePoints.value = emptyList()
        _distanceMeters.value = 0.0
        _durationMs.value = 0L
        _runningState.value = RunningState.RUNNING
    }

    /**
     * 暂停跑步
     */
    fun pauseRunning() {
        if (_runningState.value != RunningState.RUNNING) return

        pauseStartTime = System.currentTimeMillis()
        _runningState.value = RunningState.PAUSED
        updateNotification()
    }

    /**
     * 继续跑步
     */
    fun resumeRunning() {
        if (_runningState.value != RunningState.PAUSED) return

        pausedDuration += System.currentTimeMillis() - pauseStartTime
        _runningState.value = RunningState.RUNNING
        updateNotification()
    }

    /**
     * 停止跑步
     */
    fun stopRunning() {
        stopLocationUpdates()
        _runningState.value = RunningState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _runningState.value = RunningState.IDLE
        _routePoints.value = emptyList()
        _distanceMeters.value = 0.0
        _durationMs.value = 0L
        _currentLocation.value = null
        startTime = 0L
        pausedDuration = 0L
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceKm = _distanceMeters.value / 1000.0
        val durationFormatted = LocationUtils.formatDuration(_durationMs.value)

        return NotificationCompat.Builder(this, RunShareApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.running_notification_title))
            .setContentText(getString(R.string.running_notification_text, String.format("%.2f", distanceKm), durationFormatted))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 2000L // 2秒
        private const val LOCATION_FASTEST_INTERVAL = 1000L // 最快1秒
        private const val MIN_DISTANCE_METERS = 5f // 最小移动5米

        fun startService(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
