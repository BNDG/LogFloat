package com.bndg.floatlog

// LogManager 用于管理观察者并通知更新
class LogManager private constructor() {
    private val observers = mutableListOf<LogObserver>()

    companion object {
        val instance: LogManager by lazy { LogManager() }
    }

    // 注册观察者
    fun addObserver(observer: LogObserver) {
        observers.add(observer)
    }

    // 移除观察者
    fun removeObserver(observer: LogObserver) {
        observers.remove(observer)
    }

    // 通知所有观察者
    fun notifyObservers(logEvent: HttpLogEvent) {
        observers.forEach { it.onLogUpdated(logEvent) }
    }

    // 模拟日志更新
    fun logUpdated(logEvent: HttpLogEvent) {
        notifyObservers(logEvent)
    }
}