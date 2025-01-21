package com.bndg.floatlog

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * @author r
 * @date 2025/1/20
 * @description Brief description of the file content.
 */
class FloatViewService: Service() {

    private var mFloatView: FloatView? = null

    override fun onBind(intent: Intent?): IBinder {
        return FloatViewServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        mFloatView = FloatView(this)
        mFloatView?.show()
        LogManager.instance.addObserver(object : LogObserver {
            override fun onLogUpdated(logEvent: HttpLogEvent) {
                onEvent(logEvent)
            }
        })
    }

    fun onEvent(event: HttpLogEvent?) {
        event?.let {
            mFloatView?.setContent(it)
        }
    }

    class FloatViewServiceBinder : Binder() {
        fun getService(): FloatViewService {
            return FloatViewService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyFloat()
    }

    fun destroyFloat() {
        if (mFloatView != null) {
            mFloatView?.destroy()
        }
        mFloatView = null
    }
}