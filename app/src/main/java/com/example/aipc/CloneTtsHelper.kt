package com.example.aipc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object CloneTtsHelper {
    private const val TAG = "CloneTtsHelper"
    private const val PACKAGE_NAME = "com.sipeter.clonetts"

    // CloneTTS 的下载信息（保留）
    const val CLONE_APK_URL = "https://github.com/sipeter/CloneTTS/releases/download/v0.6.5/CloneTTS-V0.6.5_c26061714.apk"
    private const val CLONE_APK_FILENAME = "CloneTTS.apk"

    // 通用 OkHttp 客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "no-cache")
                .build()
            chain.proceed(request)
        }
        .build()

    private var downloadCall: okhttp3.Call? = null
    private var isCancelled = false

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val code: Int? = null, val message: String) : DownloadResult()
        object Cancelled : DownloadResult()
    }

    /**
     * 通用文件下载函数（带进度、可取消）
     * @param url 下载地址
     * @param fileName 保存的文件名
     * @param context Context
     * @param onProgress 进度回调 (0~100)
     */
    suspend fun downloadFile(
        url: String,
        fileName: String,
        context: Context,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            isCancelled = false
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            downloadCall = call
            val response = call.execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "无响应体"
                return@withContext DownloadResult.Error(
                    code = response.code,
                    message = "HTTP ${response.code}: $errorBody"
                )
            }

            val contentLength = response.body?.contentLength() ?: -1
            val inputStream = response.body?.byteStream() ?: return@withContext DownloadResult.Error(
                message = "响应体为空"
            )

            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            apkFile.parentFile?.mkdirs()
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    outputStream.close()
                    inputStream.close()
                    response.close()
                    apkFile.delete()
                    return@withContext DownloadResult.Cancelled
                }
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    withContext(Dispatchers.Main) { onProgress(progress) }
                }
            }
            outputStream.close()
            inputStream.close()
            response.close()
            DownloadResult.Success(apkFile)
        } catch (e: IOException) {
            DownloadResult.Error(
                code = null,
                message = "IO异常: ${e.message ?: "未知"}\n可能原因：网络中断、服务器拒绝连接、或链接已失效。"
            )
        } catch (e: Exception) {
            DownloadResult.Error(
                code = null,
                message = "异常: ${e.message ?: "未知"}"
            )
        }
    }

    /**
     * 下载 CloneTTS（保持原有方法签名，内部调用通用函数）
     */
    suspend fun downloadApk(context: Context, onProgress: (Int) -> Unit): DownloadResult {
        return downloadFile(CLONE_APK_URL, CLONE_APK_FILENAME, context, onProgress)
    }

    fun cancelDownload() {
        isCancelled = true
        downloadCall?.cancel()
    }

    /**
     * 安装 APK（通用）
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK 文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "无法打开安装界面，请手动安装", Toast.LENGTH_LONG).show()
        }
    }

    // ============ 检测和启动 CloneTTS（保持不变） ============
    fun isCloneTtsInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getCloneTtsPackageName(context: Context): String? {
        return if (isCloneTtsInstalled(context)) PACKAGE_NAME else null
    }

    fun launchCloneTTS(context: Context): Boolean {
        val pkg = getCloneTtsPackageName(context) ?: return false
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}