package com.bndg.floatlog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


/**
 * @author r
 * @date 2025/1/20
 * @description Brief description of the file content.
 */
class FloatViewService : Service(), LogObserver {

    private var mFloatView: FloatView? = null

    override fun onBind(intent: Intent?): IBinder {
        return FloatViewServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        mFloatView = FloatView(this)
        mFloatView?.show()
        LogManager.instance.addObserver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        startForegroundService()
        return START_STICKY // 如果服务被杀死，系统会尝试重新创建它
    }

    private fun startForegroundService() {
        // 创建通知渠道（针对 Android 8.0 及以上版本）
        var channelId = "flog_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "flog_service_channel",
                "float log Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            var channelNotification = manager?.createNotificationChannel(channel)
            channelNotification?.let {
                channelId = channel.id
            }
        }
        // 创建一个 PendingIntent，用于监听通知的移除事件
        val deleteIntent = Intent(
            this,
            FlogNotificationRemovedReceiver::class.java
        )
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, deleteIntent,
            flags
        )

        // 构建通知
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("日志查看器-移除以关闭")
            .setSmallIcon(R.drawable.flog_baseline_info_24)
            .setDeleteIntent(pendingIntent)
            .build()

        // 启动前台服务
        startForeground(1, notification) // 第一个参数是通知 ID，必须唯一
    }

    class FloatViewServiceBinder : Binder() {
        fun getService(): FloatViewService {
            return FloatViewService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止前台服务并移除通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stopForeground(STOP_FOREGROUND_REMOVE) // 新方法
        } else {
            stopForeground(true) // 兼容旧版本
        }
        LogManager.instance.removeAllObserver()
        destroyFloat()
    }

    private fun destroyFloat() {
        if (mFloatView != null) {
            mFloatView?.destroy()
        }
        mFloatView = null
    }

    override fun onLogUpdated(logEvent: HttpLogEvent) {
        logEvent.let {
            mFloatView?.setContent(it)
        }
    }
}