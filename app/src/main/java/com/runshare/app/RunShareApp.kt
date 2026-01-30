package com.runshare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.osmdroid.config.Configuration

class RunShareApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "running_channel"
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化OSMDroid配置
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@RunShareApp, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }

        // 创建通知渠道
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
