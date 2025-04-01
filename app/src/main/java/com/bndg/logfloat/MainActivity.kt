package com.bndg.logfloat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bndg.floatlog.FloatViewService
import com.bndg.logfloat.http.api.SearchAuthorApi
import com.bndg.logfloat.http.api.SearchBlogsApi
import com.bndg.logfloat.http.api.UpdateImageApi
import com.bndg.logfloat.http.model.HttpData
import com.hjq.http.EasyHttp
import com.hjq.http.config.IRequestApi
import com.hjq.http.listener.HttpCallbackProxy
import com.hjq.http.listener.OnDownloadListener
import com.hjq.http.listener.OnHttpListener
import com.hjq.http.listener.OnUpdateListener
import com.hjq.http.model.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity(), OnHttpListener<Object> {
    private var isDownloading: Boolean = false
    private var isUploading: Boolean = false
    private val btnStart: View? by lazy { findViewById(R.id.btn_start) }
    private val btnStop: View? by lazy { findViewById(R.id.btn_stop) }
    private val btnGet: View? by lazy { findViewById(R.id.btn_get) }
    private val btnPost: View? by lazy { findViewById(R.id.btn_post) }
    private val btnUpload: View? by lazy { findViewById(R.id.btn_upload) }
    private val btnDownload: View? by lazy { findViewById(R.id.btn_download) }

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
                    launchFloatView()
                }
            } else {
                //绘ui代码,这里android6.0以下的系统直接绘出即可
                launchFloatView()
            }
        }
        btnStop?.setOnClickListener {
            destroyFloatView()
        }
        btnUpload?.setOnClickListener {
            if (isUploading) {
                Toast.makeText(
                    this@MainActivity,
                    "正在上传中，请稍后",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            isUploading = true
            val outFile = File(filesDir, "abc.jpg")
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    if (!outFile.exists()) {
                        val assetManager = assets
                        val inputStream = assetManager.open("abc.jpg") // 替换成实际文件名
                        try {
                            FileOutputStream(outFile).use { out ->
                                val buffer = ByteArray(1024)
                                var length: Int
                                while ((inputStream.read(buffer).also { length = it }) > 0) {
                                    out.write(buffer, 0, length)
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                EasyHttp.post(this@MainActivity)
                    .api(UpdateImageApi(outFile))
                    .request(object : OnUpdateListener<Void> {
                        override fun onUpdateStart(api: IRequestApi) {
                        }

                        override fun onUpdateProgressChange(progress: Int) {
                        }

                        override fun onUpdateSuccess(result: Void) {
                            Toast.makeText(
                                this@MainActivity,
                                "上传成功",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onUpdateFail(throwable: Throwable) {
                            Toast.makeText(
                                this@MainActivity,
                                "上传失败",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onUpdateEnd(api: IRequestApi) {
                            isUploading = false
                        }
                    })
            }
        }
        btnDownload?.setOnClickListener {
            if(isDownloading) {
                Toast.makeText(
                    this@MainActivity,
                    "正在下载中，请稍后",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            isDownloading = true
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test.jpg")
            EasyHttp.download(this)
                .method(HttpMethod.GET)
                .file(file) //.url("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk")
                .url("https://down.wss.zone/n8uornv/g/4m/g4mwn8uornv?cdn_sign=1737684057-57-0-ef5a6e98b3d90a1ec3b1cb1edf8d6c98&exp=240&response-content-disposition=attachment%3B%20filename%3D%22%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241205142424.jpg%22%3B%20filename%2A%3Dutf-8%27%27%25E5%25BE%25AE%25E4%25BF%25A1%25E5%259B%25BE%25E7%2589%2587_20241205142424.jpg")
                .resumableTransfer(true)
                .listener(object : OnDownloadListener {
                    override fun onDownloadStart(file: File) {
                    }

                    override fun onDownloadProgressChange(file: File, progress: Int) {
                    }

                    override fun onDownloadSuccess(file: File) {

                        Toast.makeText(
                            this@MainActivity,
                            "下载成功",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                    override fun onDownloadFail(file: File, throwable: Throwable) {
                        Toast.makeText(
                            this@MainActivity,
                            "下载失败",
                            Toast.LENGTH_LONG
                        ).show()
                        file.delete()
                    }

                    override fun onDownloadEnd(file: File) {
                        isDownloading = false
                    }
                })
                .start()
        }

    }

    /**
     * 关闭悬浮窗
     */
    private fun destroyFloatView() {
        val intent = Intent(
            this,
            FloatViewService::class.java
        )
        stopService(intent)

    }

    /**
     * 开启悬浮窗
     */
    private fun launchFloatView() {
        val intent = Intent(
            this,
            FloatViewService::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent); // Android 8.0 及以上版本需使用此方法
        } else {
            startService(intent); // Android 8.0 以下版本
        }
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