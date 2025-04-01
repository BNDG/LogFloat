package com.bndg.logfloat

import android.app.Application
import com.bndg.logfloat.http.model.RequestHandler
import com.bndg.logfloat.http.server.ReleaseServer
import com.bndg.logfloat.http.server.TestServer
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import com.hjq.gson.factory.GsonFactory
import com.hjq.gson.factory.ParseExceptionCallback
import com.hjq.http.EasyConfig
import com.hjq.http.config.IRequestInterceptor
import com.hjq.http.config.IRequestServer
import com.hjq.http.model.HttpHeaders
import com.hjq.http.model.HttpParams
import com.hjq.http.request.HttpRequest
import com.tencent.mmkv.BuildConfig
import okhttp3.OkHttpClient


/**
 * @author r
 * @date 2025/1/20
 * @description Brief description of the file content.
 */
class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //初始化
        // 设置 Json 解析容错监听
        GsonFactory.setParseExceptionCallback(object : ParseExceptionCallback {
            override fun onParseObjectException(
                typeToken: TypeToken<*>,
                fieldName: String,
                jsonToken: JsonToken
            ) {
                handlerGsonParseException("解析对象析异常：$typeToken#$fieldName，后台返回的类型为：$jsonToken")
            }

            override fun onParseListItemException(
                typeToken: TypeToken<*>,
                fieldName: String,
                listItemJsonToken: JsonToken
            ) {
                handlerGsonParseException("解析 List 异常：$typeToken#$fieldName，后台返回的条目类型为：$listItemJsonToken")
            }

            override fun onParseMapItemException(
                typeToken: TypeToken<*>,
                fieldName: String,
                mapItemKey: String,
                mapItemJsonToken: JsonToken
            ) {
                handlerGsonParseException("解析 Map 异常：$typeToken#$fieldName，mapItemKey = $mapItemKey，后台返回的条目类型为：$mapItemJsonToken")
            }

            private fun handlerGsonParseException(message: String) {
                require(!BuildConfig.DEBUG) { message }
            }
        })
        // 网络请求框架初始化
        val server: IRequestServer
        if (BuildConfig.DEBUG) {
            server = TestServer()
        } else {
            server = ReleaseServer()
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor())
            .build()

        EasyConfig.with(okHttpClient) // 是否打印日志
            //.setLogEnabled(BuildConfig.DEBUG)
            // 设置服务器配置（必须设置）
            .setServer(server) // 设置请求处理策略（必须设置）
            .setHandler(RequestHandler(this)) // 设置请求参数拦截器
            .setInterceptor(object : IRequestInterceptor {
                override fun interceptArguments(
                    httpRequest: HttpRequest<*>,
                    params: HttpParams,
                    headers: HttpHeaders
                ) {
                    headers.put("token", "hhhhh")
                }
            }) // 设置请求重试次数
            .setRetryCount(1) // 设置请求重试时间
            .setRetryTime(2000) // 添加全局请求参数
            .addHeader("jwt", "112233")
            .into()
    }
}
