package com.bndg.logfloat

import android.text.TextUtils
import com.bndg.floatlog.HttpLogEvent
import com.bndg.floatlog.LogManager
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpHeaders
import okio.Buffer
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.Locale

class LoggingInterceptor : Interceptor {
    private val UTF8: Charset = Charset.forName("UTF-8")

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val orginUrl = request.url().toString()

        // 忽略文件上传的请求
        if (orginUrl.contains("file")) {
            // 记录文件上传接口日志
            sendEvent(request, "", orginUrl)
            return chain.proceed(request)
        }

        // 处理请求体
        val requestBody = request.body()
        val body = requestBody?.let { getBodyString(it) }
        requestBody?.let {
            if (!canLog(it.contentType())) {
                //  忽略contentType
                return chain.proceed(request)
            }
        }
        sendEvent(request, body, null)

        // 执行请求并获取响应
        val response = chain.proceed(request)

        // 处理响应体
        val responseBody = response.body()
        val rBody = if (HttpHeaders.hasBody(response)) {
            responseBody?.let { getBodyString(it) }
        } else null
        rBody?.let {
            if (!canLog(responseBody?.contentType())) {
                sendEvent(request, body, orginUrl)
            } else {
                sendEvent(request, body, rBody)
            }
        }
        return response
    }

    private fun getBodyString(body: RequestBody): String? {
        val buffer = Buffer()
        body.writeTo(buffer)

        val contentType = body.contentType()
        var charset = UTF8
        contentType?.charset(UTF8)?.let { charset = it }

        return buffer.readString(charset)
    }

    private fun getBodyString(body: ResponseBody): String? {
        val source = body.source()
        source.request(Long.MAX_VALUE) // Buffer the entire body.
        val buffer = source.buffer()

        val contentType = body.contentType()
        var charset = UTF8
        contentType?.charset(UTF8)?.let { charset = it }

        return buffer.clone().readString(charset)
    }

    private fun sendEvent(request: Request, requestBody: String?, responseBody: String?) {
        val results = responseBody ?: "" // 如果 responseBody 为 null，使用空字符串
        val originUrl = request.url().toString()

        // 获取请求头中的 accessToken
        val accessToken = request.header("accessToken")

        // URL 解码
        val url = try {
            URLDecoder.decode(originUrl, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            // 处理异常
            originUrl
        }
        // 如果 requestBody 不为空，构造 JSON 格式的请求体
        val requestBodyJson = requestBody?.let {
            if (!it.startsWith("{")) {
                // 非 JSON 格式的请求体，转换为 JSON 格式
                buildJsonFromParams(it)
            } else {
                // 已经是 JSON 格式的请求体
                it
            }
        } ?: ""

        // 计算哈希值
        val hashValue = (url + requestBodyJson).hashCode()

        // 记录日志
        LogManager.instance.logUpdated(
            HttpLogEvent(
                url,
                requestBodyJson,
                hashValue,
                results,
                accessToken
            )
        )
    }

    // 辅助方法：将参数字符串转换为 JSON 格式
    private fun buildJsonFromParams(params: String): String {
        val jsonBuilder = StringBuilder("{\r\n")
        val pairs = params.split("&")

        pairs.forEach { pair ->
            val keyValue = pair.split("=")
            val key = keyValue[0]
            val value = if (keyValue.size > 1) {
                val s = URLDecoder.decode(keyValue[1], "UTF-8")
                s
            } else "\"\""
            jsonBuilder.append("\"$key\":$value\r\n")
        }

        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    private fun canLog(mediaType: MediaType?): Boolean {
        if (null != mediaType) {
            var mediaTypeString = mediaType.toString()
            if (!TextUtils.isEmpty(mediaTypeString)) {
                mediaTypeString = mediaTypeString.lowercase(Locale.getDefault())
                return (mediaTypeString.contains("text")
                        || mediaTypeString.contains("application/json")
                        || mediaTypeString.contains("x-www-form-urlencoded"))
            }
        }
        return false
    }
}