package com.bndg.floatlog

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

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


    class FloatViewServiceBinder : Binder() {
        fun getService(): FloatViewService {
            return FloatViewService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogManager.instance.removeObserver(this)
        destroyFloat()
    }

    fun destroyFloat() {
        if (mFloatView != null) {
            mFloatView?.destroy()
        }
        mFloatView = null
    }

    override fun onLogUpdated(logEvent: HttpLogEvent) {
        logEvent?.let {
            mFloatView?.setContent(it)
        }
    }
}