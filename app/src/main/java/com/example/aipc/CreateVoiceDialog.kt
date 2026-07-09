package com.example.aipc

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class CreateVoiceDialog(private val onVoiceCreated: (name: String) -> Unit) : DialogFragment() {

    private lateinit var etVoiceName: EditText
    private lateinit var btnFind: Button
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button
    private lateinit var tvStatus: TextView
    private lateinit var btnTutorial: Button   // 新增

    private var voiceFound = false
    private var foundName = ""

    // CloneTTS HTTP 客户端
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val CLONE_TTS_API_BASE = "http://127.0.0.1:8080"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_voice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etVoiceName = view.findViewById(R.id.etVoiceName)
        btnFind = view.findViewById(R.id.btnFindVoice)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        tvStatus = view.findViewById(R.id.tvFindStatus)
        btnTutorial = view.findViewById(R.id.btnTutorial)   // 新增

        // 初始状态：确认按钮不可用
        btnConfirm.isEnabled = false
        voiceFound = false

        // 教程按钮点击
        btnTutorial.setOnClickListener { showTutorialDialog() }

        btnFind.setOnClickListener {
            val name = etVoiceName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入音色名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findVoiceInCloneTTS(name)
        }

        btnCancel.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            if (voiceFound) {
                onVoiceCreated(foundName)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "请先查找并确认音色存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== 原有查找逻辑（保持不变） ====================

    private fun findVoiceInCloneTTS(name: String) {
        tvStatus.text = "正在查找..."
        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
        btnFind.isEnabled = false
        voiceFound = false
        btnConfirm.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // 使用短文本测试合成
                    val testText = "测试"
                    val encodedText = java.net.URLEncoder.encode(testText, "UTF-8")
                    val encodedVoice = java.net.URLEncoder.encode(name, "UTF-8")
                    val url = "$CLONE_TTS_API_BASE/api/tts?text=$encodedText&voice=$encodedVoice"
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bodyBytes = response.body?.bytes()
                        if (bodyBytes != null && bodyBytes.size > 100) {
                            Triple(true, "找到音色：$name", null)
                        } else {
                            Triple(false, "音色合成数据异常", null)
                        }
                    } else {
                        when (response.code) {
                            404 -> Triple(false, "未找到音色“$name”", null)
                            else -> Triple(false, "查找失败 (HTTP ${response.code})", null)
                        }
                    }
                } catch (e: IOException) {
                    Triple(false, "无法连接 CloneTTS 服务\n请确保 CloneTTS 已开启「本地 HTTP API 服务」", null)
                } catch (e: Exception) {
                    Triple(false, "查找异常: ${e.message}", null)
                }
            }

            val (success, message, _) = result
            withContext(Dispatchers.Main) {
                tvStatus.text = message
                btnFind.isEnabled = true

                if (success) {
                    voiceFound = true
                    foundName = name
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    btnConfirm.isEnabled = true
                    Toast.makeText(requireContext(), "✓ 音色“$name”已找到", Toast.LENGTH_SHORT).show()
                } else {
                    voiceFound = false
                    btnConfirm.isEnabled = false
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))

                    if (message.contains("无法连接")) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("需要开启 CloneTTS HTTP 服务")
                            .setMessage("请在 CloneTTS 的「高级设置」中开启「本地 HTTP API 服务」，然后重试。")
                            .setPositiveButton("去开启") { _, _ ->
                                openCloneTTSSettings()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    } else if (message.contains("未找到音色")) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("音色不存在")
                            .setMessage("在 CloneTTS 中未找到名为“$name”的音色。\n是否前往 CloneTTS 创建该音色？")
                            .setPositiveButton("去 CloneTTS") { _, _ ->
                                openCloneTTS()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun openCloneTTS() {
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage("com.sipeter.clonetts")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "无法打开 CloneTTS，请手动打开", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "打开失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCloneTTSSettings() {
        openCloneTTS()
        Toast.makeText(requireContext(), "请在 CloneTTS 中开启「本地 HTTP API 服务」", Toast.LENGTH_LONG).show()
    }

    // ==================== 新增：教程弹窗 ====================

    private fun showTutorialDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_voice_tutorial, null)

        // 如果图片资源不存在则隐藏对应的 ImageView
        val imageIds = listOf(
            R.id.iv_ys1 to "ys1",
            R.id.iv_ys2 to "ys2",
            R.id.iv_ys3 to "ys3",
            R.id.iv_ys4 to "ys4",
            R.id.iv_ys5 to "ys5",
            R.id.iv_ys6 to "ys6",
            R.id.iv_ys7 to "ys7"
        )
        for ((viewId, name) in imageIds) {
            val imageView = dialogView.findViewById<ImageView>(viewId)
            val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
            if (resId == 0) {
                imageView.visibility = View.GONE
            } else {
                imageView.setImageResource(resId)
            }
        }

        // 电池优化按钮，调用独立的 Helper
        val btnBattery = dialogView.findViewById<Button>(R.id.btnBatteryOptimization)
        btnBattery.setOnClickListener {
            BatteryOptimizationHelper.openBatteryOptimizationSettings(requireContext())
        }

        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseTutorial)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()
        btnClose.setOnClickListener { dialog.dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}