package com.bndg.floatlog

// LogObserver 接口，外部实现该接口以接收日志更新
interface LogObserver {
    fun onLogUpdated(logEvent: HttpLogEvent)
}