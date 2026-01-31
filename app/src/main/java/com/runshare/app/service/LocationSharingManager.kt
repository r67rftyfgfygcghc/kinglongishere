package com.runshare.app.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.runshare.app.model.LocationPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 位置共享管理器
 * 支持自建服务器，离线时本地回退
 */
class LocationSharingManager(private val context: Context) {

    companion object {
        private const val TAG = "LocationSharingManager"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 服务器连接状态
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // 是否正在共享
    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    // 服务器地址
    private var serverUrl: String = ""

    // 用户信息
    private var userId: String = ""
    private var username: String = ""

    // 共享好友的位置
    private val _friendLocations = MutableStateFlow<Map<String, SharedLocation>>(emptyMap())
    val friendLocations: StateFlow<Map<String, SharedLocation>> = _friendLocations.asStateFlow()

    // 订阅的好友ID列表
    private val subscribedFriends = mutableSetOf<String>()

    // WebSocket
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 共享位置数据类
     */
    data class SharedLocation(
        val userId: String,
        val username: String,
        val location: LocationPoint,
        val isRunning: Boolean = false,
        val distance: Double = 0.0,
        val duration: Long = 0L
    )

    /**
     * 配置服务器和用户信息
     */
    fun configure(serverUrl: String, userId: String, username: String) {
        this.serverUrl = serverUrl
        this.userId = userId
        this.username = username
    }

    /**
     * 开始共享位置
     */
    fun startSharing() {
        if (_isSharing.value) return
        _isSharing.value = true
        
        if (serverUrl.isNotEmpty()) {
            connectWebSocket()
        }
    }

    /**
     * 停止共享位置
     */
    fun stopSharing() {
        _isSharing.value = false
        webSocket?.close(1000, "User stopped sharing")
        webSocket = null
        _isConnected.value = false
    }

    /**
     * 更新位置
     */
    fun updateLocation(
        location: LocationPoint,
        isRunning: Boolean = false,
        distance: Double = 0.0,
        duration: Long = 0L
    ) {
        if (!_isSharing.value) return

        val data = SharedLocation(
            userId = userId,
            username = username,
            location = location,
            isRunning = isRunning,
            distance = distance,
            duration = duration
        )

        if (_isConnected.value) {
            sendToServer(data)
        } else {
            // 离线模式：保存本地（可用于后续同步或二维码分享）
            saveLocalLocation(data)
        }
    }

    /**
     * 订阅好友位置
     */
    fun subscribeFriend(friendId: String) {
        subscribedFriends.add(friendId)
        if (_isConnected.value) {
            sendSubscribeMessage(friendId)
        }
    }

    /**
     * 取消订阅好友位置
     */
    fun unsubscribeFriend(friendId: String) {
        subscribedFriends.remove(friendId)
        _friendLocations.value = _friendLocations.value - friendId
    }

    /**
     * 生成分享链接
     */
    fun generateShareLink(): String {
        return if (serverUrl.isNotEmpty()) {
            // 在线模式：生成服务器链接
            "$serverUrl/share/$userId"
        } else {
            // 离线模式：生成本地deep link
            "runshare://location/$userId"
        }
    }

    /**
     * 连接WebSocket
     */
    private fun connectWebSocket() {
        if (serverUrl.isEmpty()) return

        val wsUrl = serverUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _isConnected.value = true
                
                // 发送身份认证
                val authMsg = mapOf(
                    "type" to "auth",
                    "userId" to userId,
                    "username" to username
                )
                webSocket.send(gson.toJson(authMsg))

                // 重新订阅好友
                subscribedFriends.forEach { sendSubscribeMessage(it) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                _isConnected.value = false
                
                // 尝试重连
                if (_isSharing.value) {
                    scope.launch {
                        delay(RECONNECT_DELAY_MS)
                        if (_isSharing.value) connectWebSocket()
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                _isConnected.value = false
            }
        })
    }

    /**
     * 发送位置到服务器
     */
    private fun sendToServer(data: SharedLocation) {
        val message = mapOf(
            "type" to "location",
            "data" to data
        )
        webSocket?.send(gson.toJson(message))
    }

    /**
     * 发送订阅消息
     */
    private fun sendSubscribeMessage(friendId: String) {
        val message = mapOf(
            "type" to "subscribe",
            "friendId" to friendId
        )
        webSocket?.send(gson.toJson(message))
    }

    /**
     * 处理服务器消息
     */
    private fun handleServerMessage(text: String) {
        try {
            val message = gson.fromJson(text, Map::class.java)
            when (message["type"]) {
                "location" -> {
                    val dataMap = message["data"] as? Map<*, *> ?: return
                    val friendId = dataMap["userId"] as? String ?: return
                    val location = gson.fromJson(gson.toJson(dataMap), SharedLocation::class.java)
                    
                    _friendLocations.value = _friendLocations.value + (friendId to location)
                }
                "offline" -> {
                    val friendId = message["userId"] as? String ?: return
                    _friendLocations.value = _friendLocations.value - friendId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
        }
    }

    /**
     * 保存本地位置（离线模式）
     */
    private fun saveLocalLocation(data: SharedLocation) {
        // 保存到SharedPreferences供二维码分享使用
        val prefs = context.getSharedPreferences("location_share", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_location", gson.toJson(data))
            .putLong("last_update", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取本地保存的位置
     */
    fun getLocalLocation(): SharedLocation? {
        val prefs = context.getSharedPreferences("location_share", Context.MODE_PRIVATE)
        val json = prefs.getString("last_location", null) ?: return null
        return try {
            gson.fromJson(json, SharedLocation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 清理资源
     */
    fun destroy() {
        stopSharing()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
