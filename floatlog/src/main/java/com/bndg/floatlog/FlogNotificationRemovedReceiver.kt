package com.bndg.floatlog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FlogNotificationRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fvsIntent = Intent(context, FloatViewService::class.java)
        context.stopService(fvsIntent)
    }
}