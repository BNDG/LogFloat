package com.bndg.floatlog

/**
 * =============================
 * 作    者：r
 * 描    述：
 * 创建日期：2020/9/2 下午4:58
 * =============================
 */
class HttpLogEvent {
    var header: String? = null
    var url: String
    var params: String
    var hashValue: Int
    var results: String

    constructor(url: String, par: String, hashValue: Int, results: String, header: String?) {
        this.url = url
        this.params = par
        this.hashValue = hashValue
        this.results = results
        this.header = header
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as HttpLogEvent
        return hashValue == that.hashValue
    }

    override fun hashCode(): Int {
        return hashValue
    }
}
