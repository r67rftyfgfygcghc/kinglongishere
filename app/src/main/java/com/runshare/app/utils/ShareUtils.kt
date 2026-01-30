package com.runshare.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.runshare.app.data.RunEntity
import com.runshare.app.model.LocationPoint
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 分享工具类
 */
object ShareUtils {

    private val gson = Gson()

    /**
     * 分享数据类
     */
    data class ShareData(
        val type: String, // "live" or "history"
        val sessionId: String,
        val runId: Long? = null,
        val points: List<LocationPoint>? = null,
        val distance: Double? = null,
        val duration: Long? = null
    )

    /**
     * 生成分享链接
     */
    fun generateShareLink(data: ShareData): String {
        val json = gson.toJson(data)
        val encoded = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        return "runshare://share?data=$encoded"
    }

    /**
     * 解析分享链接
     */
    fun parseShareLink(link: String): ShareData? {
        return try {
            val dataParam = link.substringAfter("data=")
            val json = String(android.util.Base64.decode(dataParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
            gson.fromJson(json, ShareData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成二维码
     */
    fun generateQRCode(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    /**
     * 分享跑步记录文本
     */
    fun shareRunAsText(context: Context, run: RunEntity) {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
        val text = """
            🏃 跑步记录
            📅 ${dateFormat.format(Date(run.startTime))}
            📏 距离: ${String.format("%.2f", run.getDistanceKm())} 公里
            ⏱️ 时长: ${run.getFormattedDuration()}
            🚀 配速: ${run.getFormattedPace()}
            
            来自「跑步分享」App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享跑步记录"))
    }

    /**
     * 导出为GPX格式
     */
    fun exportToGpx(context: Context, run: RunEntity): File? {
        val points = run.getRoutePoints()
        if (points.isEmpty()) return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val gpxContent = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="RunShare">""")
            appendLine("""  <trk>""")
            appendLine("""    <name>跑步记录 ${run.id}</name>""")
            appendLine("""    <trkseg>""")

            for (point in points) {
                appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}">""")
                appendLine("""        <ele>${point.altitude}</ele>""")
                appendLine("""        <time>${dateFormat.format(Date(point.timestamp))}</time>""")
                appendLine("""      </trkpt>""")
            }

            appendLine("""    </trkseg>""")
            appendLine("""  </trk>""")
            appendLine("""</gpx>""")
        }

        val fileName = "run_${run.id}_${run.startTime}.gpx"
        val file = File(context.getExternalFilesDir(null), fileName)

        return try {
            FileWriter(file).use { it.write(gpxContent) }
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成唯一会话ID
     */
    fun generateSessionId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(12)
    }
}
