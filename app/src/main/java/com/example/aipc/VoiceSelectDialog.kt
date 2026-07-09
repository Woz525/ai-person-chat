package com.example.aipc

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VoiceSelectDialog(
    private val onVoiceSelected: (String) -> Unit
) : DialogFragment() {

    private lateinit var llVoiceOptions: LinearLayout
    private lateinit var optionNew: LinearLayout
    private lateinit var dataManager: DataManager

    // Watt Toolkit 包名列表
    private val WATT_PACKAGES = listOf(
        "net.steampp.app",
        "com.steampp.net",
        "cn.steampp.net"
    )

    // Watt Toolkit 下载链接
    private val WATT_URL = "https://c1039.lanosso.com/03e05fdc3f5f6c3f011533cb08d253c7/6a4323a2/2022/08/15/18c22ab8d13ee3e918dee1c025385e66.apk?fn=Steam%20%20_android_v2.8.3.apk"
    private val WATT_FILENAME = "WattToolkit.apk"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_voice_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        llVoiceOptions = view.findViewById(R.id.llVoiceOptions)
        val optionMale = view.findViewById<LinearLayout>(R.id.optionMale)
        val optionFemale = view.findViewById<LinearLayout>(R.id.optionFemale)
        optionNew = view.findViewById(R.id.optionNew)

        optionMale.setOnClickListener { selectVoice("男声") }
        optionFemale.setOnClickListener { selectVoice("女声") }

        loadCustomVoices()

        optionNew.setOnClickListener {
            if (CloneTtsHelper.isCloneTtsInstalled(requireContext())) {
                val createDialog = CreateVoiceDialog { name ->
                    dataManager.saveCustomVoice(name, "")
                    loadCustomVoices()
                    selectVoice(name)
                }
                createDialog.show(parentFragmentManager, "CreateVoice")
            } else {
                showFirstStepDialog()
            }
        }
    }

    // ================== 引导流程 ==================

    private fun showFirstStepDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("下载 CloneTTS")
            .setMessage("您是否有一个能打开 GitHub 的梯子（VPN/代理）？")
            .setPositiveButton("有") { _, _ -> showVpnHintDialog() }
            .setNegativeButton("没有") { _, _ -> handleNoVpn() }
            .setCancelable(false)
            .show()
    }

    private fun showVpnHintDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("请开启 VPN")
            .setMessage("请先开启您的 VPN 或代理工具，确保能访问 GitHub。\n开启后点击下方按钮继续。")
            .setPositiveButton("是的，我已开启") { _, _ ->
                lifecycleScope.launch { downloadAndInstallCloneTts() }
            }
            .setNegativeButton("取消") { _, _ ->
                Toast.makeText(requireContext(), "已取消下载", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun handleNoVpn() {
        if (!isAdded) return
        if (isWattToolkitInstalled()) {
            showWattEnableDialog()
        } else {
            showWattDownloadConfirmDialog()
        }
    }

    private fun isWattToolkitInstalled(): Boolean {
        val pm = requireContext().packageManager
        for (pkg in WATT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) { }
        }
        return false
    }

    private fun showWattDownloadConfirmDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("需要 Watt Toolkit")
            .setMessage("未检测到 Watt Toolkit，它可帮助您访问 GitHub。\n是否在应用内下载并自动安装？\n如果您已安装但未被检测到，请选择“我已有此加速器”。")
            .setPositiveButton("继续下载") { _, _ ->
                lifecycleScope.launch { downloadWattToolkitWithProgress() }
            }
            .setNegativeButton("我已有此加速器") { _, _ ->
                showWattEnableDialog()
            }
            .setNeutralButton("取消") { _, _ ->
                Toast.makeText(requireContext(), "已取消", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun downloadWattToolkitWithProgress() {
        if (!isAdded) return

        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("正在下载 Watt Toolkit")
            setMessage("请稍候...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setMax(100)
            setCancelable(false)
            setButton(ProgressDialog.BUTTON_NEGATIVE, "取消") { _, _ ->
                CloneTtsHelper.cancelDownload()
                dismiss()
                Toast.makeText(requireContext(), "已取消下载", Toast.LENGTH_SHORT).show()
            }
            show()
        }

        val result = CloneTtsHelper.downloadFile(
            url = WATT_URL,
            fileName = WATT_FILENAME,
            context = requireContext(),
            onProgress = { progress ->
                progressDialog.progress = progress
                progressDialog.setMessage("下载进度: $progress%")
            }
        )
        progressDialog.dismiss()

        when (result) {
            is CloneTtsHelper.DownloadResult.Success -> {
                try {
                    CloneTtsHelper.installApk(requireContext(), result.file)
                    Toast.makeText(requireContext(), "请完成 Watt Toolkit 的安装", Toast.LENGTH_LONG).show()
                    showWattEnableDialog()
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("安装失败")
                        .setMessage("无法启动安装界面，请手动到以下路径安装：\n${result.file.absolutePath}\n安装完成后请点击“我已安装完成”；或前往官网下载。")
                        .setPositiveButton("我已安装完成") { _, _ ->
                            showWattEnableDialog()
                        }
                        .setNegativeButton("打开官网") { _, _ ->
                            openWattOfficialWebsite()
                        }
                        .show()
                }
            }
            is CloneTtsHelper.DownloadResult.Error -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("下载 Watt Toolkit 失败")
                    .setMessage("错误代码：${result.code ?: "未知"}\n\n${result.message}\n\n如果您已手动安装，请点击“我已安装完成”；或前往官网下载。")
                    .setPositiveButton("我已安装完成") { _, _ ->
                        showWattEnableDialog()
                    }
                    .setNegativeButton("打开官网") { _, _ ->
                        openWattOfficialWebsite()
                    }
                    .show()
            }
            CloneTtsHelper.DownloadResult.Cancelled -> {
                Toast.makeText(requireContext(), "下载已取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWattEnableDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Watt Toolkit 准备就绪")
            .setMessage("请确保已开启 Watt Toolkit 的「GitHub 加速」功能。\n准备好后点击「是」继续下载 CloneTTS。")
            .setPositiveButton("是，已开启") { _, _ ->
                lifecycleScope.launch { downloadAndInstallCloneTts() }
            }
            .setNegativeButton("打开 Watt Toolkit") { _, _ ->
                openWattToolkit()
                AlertDialog.Builder(requireContext())
                    .setTitle("请开启 GitHub 加速")
                    .setMessage("请手动打开 Watt Toolkit，开启「GitHub 加速」功能。\n完成后点击「是」继续下载。")
                    .setPositiveButton("是，已开启") { _, _ ->
                        lifecycleScope.launch { downloadAndInstallCloneTts() }
                    }
                    .setNegativeButton("取消") { _, _ ->
                        Toast.makeText(requireContext(), "已取消下载", Toast.LENGTH_SHORT).show()
                    }
                    .setCancelable(false)
                    .show()
            }
            .setCancelable(false)
            .show()
    }

    private fun openWattToolkit() {
        val pm = requireContext().packageManager
        for (pkg in WATT_PACKAGES) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) { }
        }
        Toast.makeText(requireContext(), "请手动打开 Watt Toolkit", Toast.LENGTH_SHORT).show()
    }

    private fun openWattOfficialWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://steampp.net/"))
        startActivity(intent)
    }

    // ================== 下载 CloneTTS ==================

    private fun isNetworkAvailable(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private suspend fun downloadAndInstallCloneTts() {
        if (!isAdded) return
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "无网络连接", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("正在下载 CloneTTS")
            setMessage("请稍候...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setMax(100)
            setCancelable(false)
            setButton(ProgressDialog.BUTTON_NEGATIVE, "取消") { _, _ ->
                CloneTtsHelper.cancelDownload()
                dismiss()
                Toast.makeText(requireContext(), "已取消下载", Toast.LENGTH_SHORT).show()
            }
            show()
        }

        val result = CloneTtsHelper.downloadApk(
            context = requireContext(),
            onProgress = { progress ->
                progressDialog.progress = progress
                progressDialog.setMessage("下载进度: $progress%")
            }
        )
        progressDialog.dismiss()

        when (result) {
            is CloneTtsHelper.DownloadResult.Success -> {
                try {
                    CloneTtsHelper.installApk(requireContext(), result.file)
                    Toast.makeText(requireContext(), "安装包已下载，请手动完成安装", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("安装失败")
                        .setMessage("无法启动安装界面，请手动到以下路径安装：\n${result.file.absolutePath}")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
            is CloneTtsHelper.DownloadResult.Error -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("下载 CloneTTS 失败")
                    .setMessage("错误代码：${result.code ?: "未知"}\n\n${result.message}\n\n是否跳转浏览器下载？")
                    .setPositiveButton("跳转浏览器") { _, _ ->
                        openBrowserForCloneTts()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            CloneTtsHelper.DownloadResult.Cancelled -> {
                Toast.makeText(requireContext(), "下载已取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openBrowserForCloneTts() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CloneTtsHelper.CLONE_APK_URL))
        startActivity(intent)
        Toast.makeText(requireContext(), "请在浏览器中下载并安装 CloneTTS", Toast.LENGTH_LONG).show()
    }

    // ================== 自定义音色等 ==================

    private fun loadCustomVoices() {
        val children = llVoiceOptions.childCount
        for (i in children - 1 downTo 0) {
            val child = llVoiceOptions.getChildAt(i)
            if (child != optionNew && child.id != R.id.optionMale && child.id != R.id.optionFemale) {
                llVoiceOptions.removeViewAt(i)
            }
        }
        val voices = dataManager.getCustomVoices()
        val insertIndex = llVoiceOptions.indexOfChild(optionNew)
        voices.forEach { (name, _) ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_custom_voice, llVoiceOptions, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvCustomVoiceName)
            val btnDelete = itemView.findViewById<View>(R.id.btnDeleteCustomVoice)
            tvName.text = name
            itemView.setOnClickListener { selectVoice(name) }
            btnDelete.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("删除音色")
                    .setMessage("确定要删除自定义音色“$name”吗？此操作不可恢复。")
                    .setPositiveButton("删除") { _, _ ->
                        dataManager.deleteCustomVoice(name)
                        Toast.makeText(requireContext(), "已删除“$name”", Toast.LENGTH_SHORT).show()
                        loadCustomVoices()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            llVoiceOptions.addView(itemView, insertIndex)
        }
    }

    private fun selectVoice(name: String) {
        onVoiceSelected(name)
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}