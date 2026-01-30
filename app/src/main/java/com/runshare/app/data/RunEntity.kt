package com.runshare.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.runshare.app.model.LocationPoint

/**
 * 跑步记录实体类
 */
@Entity(tableName = "run_records")
data class RunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 开始时间戳
    val startTime: Long,

    // 结束时间戳
    val endTime: Long,

    // 总距离（米）
    val distanceMeters: Double,

    // 持续时间（毫秒）
    val durationMs: Long,

    // 平均配速（分钟/公里）
    val avgPaceMinPerKm: Double,

    // 轨迹点JSON
    val routePointsJson: String,

    // 标题/备注
    val title: String = "",

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取轨迹点列表
     */
    fun getRoutePoints(): List<LocationPoint> {
        return LocationPoint.listFromJson(routePointsJson)
    }

    /**
     * 获取格式化的距离（公里）
     */
    fun getDistanceKm(): Double {
        return distanceMeters / 1000.0
    }

    /**
     * 获取格式化的时长字符串
     */
    fun getFormattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 获取格式化的配速字符串
     */
    fun getFormattedPace(): String {
        if (avgPaceMinPerKm <= 0 || avgPaceMinPerKm.isInfinite()) {
            return "--'--\""
        }
        val minutes = avgPaceMinPerKm.toInt()
        val seconds = ((avgPaceMinPerKm - minutes) * 60).toInt()
        return String.format("%d'%02d\"", minutes, seconds)
    }
}
