package com.runshare.app.utils

import android.location.Location
import com.runshare.app.model.LocationPoint
import kotlin.math.*

/**
 * 位置工具类
 */
object LocationUtils {

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 地球半径（米）

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * 计算两个LocationPoint之间的距离
     */
    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        return calculateDistance(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude
        )
    }

    /**
     * 计算轨迹总距离
     */
    fun calculateTotalDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(points[i - 1], points[i])
        }
        return totalDistance
    }

    /**
     * 计算配速（分钟/公里）
     */
    fun calculatePace(distanceMeters: Double, durationMs: Long): Double {
        if (distanceMeters <= 0) return 0.0

        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationMs / 60000.0

        return durationMinutes / distanceKm
    }

    /**
     * 将Location转换为LocationPoint
     */
    fun Location.toLocationPoint(): LocationPoint {
        return LocationPoint(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            timestamp = time,
            accuracy = accuracy
        )
    }

    /**
     * 格式化距离显示
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()}m"
        } else {
            String.format("%.2fkm", meters / 1000.0)
        }
    }

    /**
     * 格式化时长显示
     */
    fun formatDuration(durationMs: Long): String {
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
     * 格式化配速显示
     */
    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm.isInfinite() || paceMinPerKm.isNaN()) {
            return "--'--\""
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format("%d'%02d\"", minutes, seconds)
    }

    /**
     * 过滤掉明显错误的GPS点
     */
    fun filterInvalidPoints(points: List<LocationPoint>): List<LocationPoint> {
        if (points.isEmpty()) return emptyList()

        return points.filter { point ->
            // 过滤掉精度过低的点
            point.accuracy < 50 &&
                    // 过滤掉明显不合理的坐标
                    point.latitude in -90.0..90.0 &&
                    point.longitude in -180.0..180.0
        }
    }
}
