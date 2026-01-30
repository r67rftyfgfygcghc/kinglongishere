package com.runshare.app.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 位置点数据类
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = 0f
) {
    companion object {
        private val gson = Gson()

        /**
         * 将位置点列表序列化为JSON
         */
        fun listToJson(points: List<LocationPoint>): String {
            return gson.toJson(points)
        }

        /**
         * 从JSON反序列化为位置点列表
         */
        fun listFromJson(json: String): List<LocationPoint> {
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<List<LocationPoint>>() {}.type
            return gson.fromJson(json, type)
        }
    }
}

/**
 * 跑步状态枚举
 */
enum class RunningState {
    IDLE,       // 空闲
    RUNNING,    // 正在跑步
    PAUSED,     // 暂停
    STOPPED     // 已停止
}

/**
 * 地图提供商枚举
 */
enum class MapProvider(val displayName: String) {
    OSM("OpenStreetMap"),
    AMAP("高德地图"),
    BAIDU("百度地图"),
    TENCENT("腾讯地图");

    companion object {
        fun fromName(name: String): MapProvider {
            return entries.find { it.name == name } ?: OSM
        }
    }
}
