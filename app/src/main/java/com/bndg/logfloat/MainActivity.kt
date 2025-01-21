package com.bndg.logfloat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bndg.floatlog.FloatViewService
import com.bndg.logfloat.http.api.SearchAuthorApi
import com.bndg.logfloat.http.api.SearchBlogsApi
import com.bndg.logfloat.http.model.HttpData
import com.hjq.http.EasyHttp
import com.hjq.http.config.IRequestApi
import com.hjq.http.listener.HttpCallbackProxy
import com.hjq.http.listener.OnHttpListener

class MainActivity : AppCompatActivity(), OnHttpListener<Object> {
    private val btnStart: View? by lazy { findViewById(R.id.btn_start) }
    private val btnStop: View? by lazy { findViewById(R.id.btn_stop) }
    private val btnGet: View? by lazy { findViewById(R.id.btn_get) }
    private val btnPost: View? by lazy { findViewById(R.id.btn_post) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnGet?.setOnClickListener {
            EasyHttp.get(this)
                .api(
                    SearchAuthorApi()
                        .setId(190000)
                )
                .request(object : HttpCallbackProxy<HttpData<List<SearchAuthorApi.Bean?>?>>(this) {
                    override fun onHttpSuccess(result: HttpData<List<SearchAuthorApi.Bean?>?>) {
                        Toast.makeText(
                            this@MainActivity,
                            "Get 请求成功，请看日志",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
        btnPost?.setOnClickListener {
            EasyHttp.post(this)
                .api(
                    SearchBlogsApi()
                        .setKeyword("搬砖不再有")
                )
                .request(object :
                    HttpCallbackProxy<HttpData<SearchBlogsApi.Bean?>>(this@MainActivity) {
                    override fun onHttpSuccess(result: HttpData<SearchBlogsApi.Bean?>) {
                        Toast.makeText(
                            this@MainActivity,
                            "Post 请求成功，请看日志",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
        btnStart?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.setData(Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    //绘ui代码, 这里说明6.0系统已经有权限了
                    startFloatView()
                }
            } else {
                //绘ui代码,这里android6.0以下的系统直接绘出即可
                startFloatView()
            }
        }
        btnStop?.setOnClickListener {
            hideFloatView()
        }

    }

    private fun hideFloatView() {
        val intent = Intent(
            this,
            FloatViewService::class.java
        )
        stopService(intent)

    }

    /**
     * 开启浮动窗口
     */
    private fun startFloatView() {
        val intent = Intent(
            this,
            FloatViewService::class.java
        )
        startService(intent)
    }


    override fun onHttpStart(api: IRequestApi) {
    }

    override fun onHttpSuccess(result: Object) {
    }

    override fun onHttpFail(throwable: Throwable) {
    }

    override fun onHttpEnd(api: IRequestApi) {
    }
}