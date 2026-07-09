package com.example.aipc

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File

object ModelManager {
    private const val TAG = "ModelManager"
    private val REQUIRED_MODELS = listOf(
        "sovits_onnx.onnx",
        "bert_onnx.onnx",
        "ssl_encoder.onnx"
    )
    // 请替换为您实际的下载链接（建议使用国内镜像加速）
    private const val MODEL_BASE_URL = "https://huggingface.co/Stardust-mini/GPT-SoVITS-Rust-models/resolve/main/"

    private var downloadIds = mutableListOf<Long>()
    private var downloadCompleteCallback: (() -> Unit)? = null
    private var progressCallback: ((Int, String?) -> Unit)? = null  // 进度、速度文本
    private var isCancelled = false
    private var lastBytes = 0L
    private var lastTime = 0L

    fun getModelDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun areModelsInstalled(context: Context): Boolean {
        val dir = getModelDir(context)
        return REQUIRED_MODELS.all { File(dir, it).exists() }
    }

    fun downloadModels(
        context: Context,
        onProgress: ((Int, String?) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadIds.clear()
        isCancelled = false
        progressCallback = onProgress
        downloadCompleteCallback = onComplete
        lastBytes = 0
        lastTime = System.currentTimeMillis()

        for (modelName in REQUIRED_MODELS) {
            val uri = Uri.parse(MODEL_BASE_URL + modelName)
            val request = DownloadManager.Request(uri).apply {
                setTitle("下载 AI 语音模型")
                setDescription("正在下载 $modelName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, "models", modelName)
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                setAllowedOverRoaming(false)
            }
            val id = downloadManager.enqueue(request)
            downloadIds.add(id)
        }

        val handler = Handler(Looper.getMainLooper())
        val progressRunnable = object : Runnable {
            override fun run() {
                if (isCancelled || downloadIds.isEmpty()) {
                    handler.removeCallbacks(this)
                    if (isCancelled) {
                        // 取消所有任务
                        downloadIds.forEach { downloadManager.remove(it) }
                        downloadIds.clear()
                        onComplete?.invoke() // 通知取消
                    }
                    return
                }
                val result = getTotalProgressAndSpeed(context)
                val progress = result.first
                val speedText = result.second
                progressCallback?.invoke(progress, speedText)

                if (progress >= 100) {
                    handler.removeCallbacks(this)
                    downloadCompleteCallback?.invoke()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(progressRunnable)
    }

    private fun getTotalProgressAndSpeed(context: Context): Pair<Int, String?> {
        if (downloadIds.isEmpty()) return Pair(0, null)
        var total = 0
        var totalBytesDownloaded = 0L
        var totalBytes = 0L
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        for (id in downloadIds) {
            val query = DownloadManager.Query().setFilterById(id)
            val cursor: Cursor? = downloadManager.query(query)
            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    if (statusIndex != -1 && bytesIndex != -1 && totalIndex != -1) {
                        val status = it.getInt(statusIndex)
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> total += 100
                            DownloadManager.STATUS_RUNNING -> {
                                val bytes = it.getLong(bytesIndex)
                                val totalBytesForModel = it.getLong(totalIndex)
                                if (totalBytesForModel > 0) {
                                    total += (bytes.toFloat() / totalBytesForModel * 100).toInt()
                                    totalBytesDownloaded += bytes
                                    totalBytes += totalBytesForModel
                                }
                            }
                            else -> { /* 忽略 */ }
                        }
                    }
                }
            }
        }
        val avgProgress = (total / downloadIds.size).coerceIn(0, 100)

        // 计算速度
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastTime) / 1000.0
        var speedText: String? = null
        if (elapsedSeconds > 0 && totalBytesDownloaded > 0) {
            val deltaBytes = totalBytesDownloaded - lastBytes
            val speedKbps = (deltaBytes / elapsedSeconds) / 1024.0
            if (speedKbps > 1024) {
                speedText = String.format("%.2f MB/s", speedKbps / 1024)
            } else {
                speedText = String.format("%.1f KB/s", speedKbps)
            }
        }
        lastBytes = totalBytesDownloaded
        lastTime = currentTime
        return Pair(avgProgress, speedText)
    }

    fun cancelDownload() {
        isCancelled = true
    }
}