package com.example.aipc

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import android.media.MediaPlayer
import android.widget.Switch
import android.widget.ImageButton

class HomeFragment : Fragment() {

    private lateinit var dataManager: DataManager
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: Button
    private lateinit var tvBotName: TextView
    private lateinit var ivBotAvatar: ImageView
    private lateinit var btnSwitch: Button
    private lateinit var btnInventory: ImageView
    private lateinit var inventory: Inventory

    private var currentAgent: Agent? = null
    private val isSending = AtomicBoolean(false)
    private lateinit var prefs: SharedPreferences
    private lateinit var storyTypeListener: SharedPreferences.OnSharedPreferenceChangeListener

    // ==================== TTS 相关 ====================
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var ttsEngine: TtsEngine
    private var isRustTtsReady = false
    private var currentUiMode: String = "classic"

    private lateinit var llRoleCard: LinearLayout
    private var cardInserted = false
    private var loadingDrawable: LoadingDrawable? = null
    // 在类中添加（与其他变量并列）
    private var currentBackgroundBitmap: Bitmap? = null
    private var manualBackgroundPath: String = ""
    private var mediaPlayer: MediaPlayer? = null
    // ==================== 背景音乐相关方法 ====================
    private lateinit var btnBgMusicToggle: ImageButton

    private val importBgMusicLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val agent = currentAgent ?: return@registerForActivityResult
                val fileName = "bg_music_${agent.id}.mp3"
                val destFile = File(requireContext().filesDir, fileName)
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                agent.bgMusicPath = destFile.absolutePath
                FileManager.saveAgent(requireContext(), agent)
                updateBgMusicNameDisplay()
                if (dataManager.isBgMusicEnabled()) {
                    playBgMusic(destFile.absolutePath)
                }
                Toast.makeText(requireContext(), "背景音乐已导入", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importBgMusic() {
        importBgMusicLauncher.launch("audio/*")
    }

    fun getCurrentBgMusicName(): String {
        val path = currentAgent?.bgMusicPath ?: ""
        return if (path.isNotEmpty()) File(path).name else ""
    }

    private fun updateBgMusicNameDisplay() {
        val name = getCurrentBgMusicName()
        (activity as? MainActivity)?.updateBgMusicName(name)
    }
    private fun updateBgMusicToggleIcon() {
        btnBgMusicToggle.setImageResource(
            if (dataManager.isBgMusicEnabled()) R.drawable.bgmon else R.drawable.bgmoff
        )
    }
    private fun updateBgMusicState() {
        val enabled = dataManager.isBgMusicEnabled()
        val path = currentAgent?.bgMusicPath ?: ""

        if (enabled) {
            if (path.isNotEmpty() && File(path).exists()) {
                playBgMusic(path)
            } else {
                // 自动修正：文件不存在时关闭开关
                dataManager.setBgMusicEnabled(false)
                stopBgMusic()
            }
        } else {
            stopBgMusic()
        }
        updateBgMusicToggleIcon()
    }

    private fun playBgMusic(path: String) {
        try {
            stopBgMusic() // 先停止旧的
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopBgMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private fun releaseBgMusic() {
        stopBgMusic()
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            uri?.let {
                try {
                    val bitmap = getBitmapFromUri(it)
                    bitmap?.let { bmp ->
                        currentAgent?.let { agent ->
                            val file = File(requireContext().filesDir, "manual_bg_${agent.id}.jpg")
                            FileOutputStream(file).use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            agent.manualBackgroundPath = file.absolutePath
                            agent.backgroundSource = "manual"
                            FileManager.saveAgent(requireContext(), agent)
                            saveCurrentAgent(agent)
                            applyBackgroundBasedOnMode()
                            Toast.makeText(requireContext(), "背景已更新", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "图片加载失败", Toast.LENGTH_SHORT).show()
                    currentAgent?.let { agent ->
                        agent.backgroundSource = "ai"
                        agent.manualBackgroundPath = ""
                        saveCurrentAgent(agent)
                        applyBackgroundBasedOnMode()
                    }
                }
            }
        } else {
            // 用户取消，回退到 AI
            currentAgent?.let { agent ->
                agent.backgroundSource = "ai"
                agent.manualBackgroundPath = ""
                saveCurrentAgent(agent)
                applyBackgroundBasedOnMode()
            }
        }
    }

    // 生图 API 客户端
    private val imageApiClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // CloneTTS HTTP 客户端
    private val cloneTtsClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    // ============================================

    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private fun setLoadingBackground() {
        loadingDrawable?.stop()
        loadingDrawable = LoadingDrawable("背景图生成中")
        rvChatMessages.background = loadingDrawable
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        inventory = Inventory(requireContext())
        prefs = requireContext().getSharedPreferences("aipc_prefs", Context.MODE_PRIVATE)

        val ivCircle: ImageView = view.findViewById(R.id.ivCircle)
        val ivMenu: ImageView = view.findViewById(R.id.ivMenu)
        llRoleCard = view.findViewById(R.id.llRoleCard)
        btnSwitch = view.findViewById(R.id.btnSwitch)
        etMessageInput = view.findViewById(R.id.etMessageInput)
        btnSend = view.findViewById(R.id.btnSend)
        rvChatMessages = view.findViewById(R.id.rvChatMessages)
        tvBotName = view.findViewById(R.id.tvBotName)
        ivBotAvatar = view.findViewById(R.id.ivBotAvatar)
        btnInventory = view.findViewById(R.id.btnInventory)
        btnBgMusicToggle = view.findViewById(R.id.btnBgMusicToggle)

        chatAdapter = ChatAdapter(
            messages,
            userAvatarPath = dataManager.getAvatarPath(),
            userName = dataManager.getUserName(),
            botAvatarBase64 = "",
            botName = ""
        )
        rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        rvChatMessages.adapter = chatAdapter

        loadDefaultAgent()
        // 监听全局背景音乐开关变化
        val prefs = requireContext().getSharedPreferences("aipc_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "bg_music_enabled") {
                updateBgMusicState()
            }
        }
        // ★ 左上角背景音乐开关点击监听
        btnBgMusicToggle.setOnClickListener {
            val currentEnabled = dataManager.isBgMusicEnabled()
            val newEnabled = !currentEnabled

            if (newEnabled) {
                val path = currentAgent?.bgMusicPath ?: ""
                if (path.isEmpty() || !File(path).exists()) {
                    Toast.makeText(requireContext(), "未设置BGM，请先导入", Toast.LENGTH_SHORT).show()
                    // 点击无效，维持关闭状态
                    return@setOnClickListener
                }
            }

            dataManager.setBgMusicEnabled(newEnabled)
            updateBgMusicState()
        }
        ivCircle.setOnClickListener { (activity as? MainActivity)?.openLeftDrawer() }
        ivMenu.setOnClickListener { (activity as? MainActivity)?.openRightDrawer() }
        btnSwitch.setOnClickListener { showSwitchDialog() }

        updateInventoryButtonVisibility()
        storyTypeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "story_type") updateInventoryButtonVisibility()
        }
        prefs.registerOnSharedPreferenceChangeListener(storyTypeListener)

        btnInventory.setOnClickListener {
            val bottomSheet = InventoryBottomSheet(inventory) { itemName, action ->
                val currentText = etMessageInput.text.toString()
                val command = "[${action}'$itemName']"
                val newText = if (currentText.isEmpty()) command else "$currentText $command"
                etMessageInput.setText(newText)
                etMessageInput.setSelection(newText.length)
            }
            bottomSheet.show(parentFragmentManager, "Inventory")
        }

        etMessageInput.imeOptions = EditorInfo.IME_ACTION_NONE
        etMessageInput.setRawInputType(etMessageInput.inputType)

        btnSend.setOnClickListener {
            if (isSending.get()) {
                Toast.makeText(requireContext(), "正在发送中，请稍后", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val message = etMessageInput.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "请输入消息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isSending.set(true)
            etMessageInput.text.clear()
            startSend(message)
        }

        // ========== 初始化 TTS ==========

        // 1. 原生 Android TTS
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "中文语言不支持")
                } else {
                    ttsReady = true
                    Log.d("TTS", "原生 TTS 初始化成功")
                }
            } else {
                Log.e("TTS", "原生 TTS 初始化失败")
            }
        }

        // 2. Rust 引擎（预留）
        ttsEngine = TtsEngine()
        // ★ 初始化 UI 模式
        // 在 onViewCreated 中，完成控件初始化后：
        currentUiMode = dataManager.getUiMode()
        applyUiMode(currentUiMode)  // 此时 currentAgent 为 null，不会插入卡片
        loadDefaultAgent()
        // 监听全局背景音乐开关变化
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "bg_music_enabled") {
                updateBgMusicState()
            }
        }
        updateBgMusicToggleIcon()
    }

    private fun updateInventoryButtonVisibility() {
        btnInventory.visibility = if (dataManager.getStoryType() == "为所欲为") View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        prefs.unregisterOnSharedPreferenceChangeListener(storyTypeListener)
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        if (::ttsEngine.isInitialized) {
            ttsEngine.release()
        }
        releaseBgMusic()
    }

    // ========== 语音合成辅助方法 ==========

    private fun speakWithNativeTts(text: String, voice: String) {
        if (!ttsReady) {
            Log.w("TTS", "原生 TTS 未就绪")
            return
        }
        val pitch = when (voice) {
            "男声" -> 0.7f
            "女声" -> 1.4f
            else -> 1.0f
        }
        val speed = when (voice) {
            "男声" -> 0.9f
            "女声" -> 1.1f
            else -> 1.0f
        }
        tts.setPitch(pitch)
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun speakWithRustEngine(text: String, refAudioPath: String) {
        if (!isRustTtsReady) {
            Log.w("RustTTS", "Rust 引擎未就绪，回退到原生 TTS")
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val success = ttsEngine.speak(text, refAudioPath)
            if (!success) {
                Log.e("RustTTS", "Rust 合成失败，回退到原生 TTS")
                Handler(Looper.getMainLooper()).post {
                    speakWithNativeTts(text, dataManager.getMode())
                }
            }
        }
    }

    // ==================== CloneTTS 集成 ====================

    /**
     * 检测 CloneTTS HTTP 服务是否可用（通过 Socket 连接测试）
     */
    private suspend fun isCloneTtsServiceAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 8080), 3000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 使用 CloneTTS HTTP API 合成语音，返回 Pair<音频数据, 错误信息>
     */
    private suspend fun synthesizeWithCloneTTS(text: String, voiceName: String): Pair<ByteArray?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = "http://127.0.0.1:8080"
                val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
                val encodedVoice = java.net.URLEncoder.encode(voiceName, "UTF-8")
                val url = "$baseUrl/api/tts?text=$encodedText&voice=$encodedVoice"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "audio/pcm; rate=16000; channels=1")
                    .header("User-Agent", "AIPC")
                    .build()

                val response = cloneTtsClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyBytes = response.body?.bytes()
                    if (bodyBytes == null || bodyBytes.isEmpty()) {
                        return@withContext Pair(null, "合成返回空数据")
                    }

                    val pcmData = if (bodyBytes.size > 44 && bodyBytes.sliceArray(0..3).contentEquals("RIFF".toByteArray())) {
                        bodyBytes.copyOfRange(44, bodyBytes.size)
                    } else {
                        bodyBytes
                    }

                    if (pcmData.isEmpty()) {
                        return@withContext Pair(null, "PCM 数据为空")
                    }
                    Pair(pcmData, null)
                } else {
                    val errorMsg = when (response.code) {
                        404 -> "音色不存在 (404)"
                        500 -> "服务器内部错误 (500)"
                        405 -> "请求方法不被支持 (405)"
                        else -> "HTTP ${response.code}: ${response.message}"
                    }
                    Pair(null, errorMsg)
                }
            } catch (e: java.net.ConnectException) {
                Pair(null, "连接失败: ${e.message}")
            } catch (e: java.net.SocketTimeoutException) {
                Pair(null, "连接超时")
            } catch (e: Exception) {
                Pair(null, "异常: ${e.message}")
            }
        }
    }

    /**
     * 安全播放 PCM 音频（估算时长等待播放完成）
     */
    private fun playPcmAudioWithResult(pcmData: ByteArray): Boolean {
        if (pcmData.isEmpty()) return false

        val sampleRates = intArrayOf(24000, 22050, 16000)
        var success = false

        for (sampleRate in sampleRates) {
            try {
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT

                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                if (minBufferSize <= 0) continue

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size.coerceAtLeast(minBufferSize))
                    .build()

                audioTrack.play()
                val writeResult = audioTrack.write(pcmData, 0, pcmData.size)
                if (writeResult >= 0) {
                    val durationMs = (pcmData.size / (sampleRate * 2L)) * 1000L
                    Thread.sleep(durationMs + 300)
                    audioTrack.stop()
                    audioTrack.release()
                    success = true
                    break
                } else {
                    audioTrack.release()
                }
            } catch (e: Exception) {
                // 忽略，尝试下一个采样率
            }
        }

        if (!success) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(requireContext(), "播放音频失败，已回退到系统语音", Toast.LENGTH_LONG).show()
            }
        }
        return success
    }

    /**
     * 语音合成入口：优先使用 CloneTTS，失败时回退到原生 TTS
     */
    private fun synthesizeAndSpeak(text: String, voice: String) {
        if (text.isBlank()) return

        if (voice.isEmpty() || voice == "男声" || voice == "女声") {
            speakWithNativeTts(text, voice)
            return
        }

        lifecycleScope.launch {
            val serviceAvailable = isCloneTtsServiceAvailable()
            if (!serviceAvailable) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("CloneTTS 服务未响应")
                        .setMessage("请确保 CloneTTS 已开启「本地 HTTP API 服务」并将应用保持在前台。")
                        .setPositiveButton("打开 CloneTTS") { _, _ ->
                            CloneTtsHelper.launchCloneTTS(requireContext())
                        }
                        .setNegativeButton("使用原生语音") { _, _ ->
                            speakWithNativeTts(text, voice)
                        }
                        .setNeutralButton("取消", null)
                        .show()
                }
                return@launch
            }

            val (pcmData, errorMsg) = synthesizeWithCloneTTS(text, voice)

            if (pcmData != null && pcmData.isNotEmpty()) {
                val playSuccess = withContext(Dispatchers.IO) {
                    playPcmAudioWithResult(pcmData)
                }
                if (!playSuccess) {
                    withContext(Dispatchers.Main) {
                        speakWithNativeTts(text, voice)
                    }
                }
            } else {
                val displayMsg = errorMsg ?: "未知错误"
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("语音合成失败")
                        .setMessage("错误原因：$displayMsg\n\n长文本合成可能需要更长时间，请确保 CloneTTS 处于前台或已锁定后台。")
                        .setPositiveButton("回退到原生语音") { _, _ ->
                            speakWithNativeTts(text, voice)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }
    }

    // ==========================================

    private fun startSend(message: String) {
        if (currentAgent == null) {
            Toast.makeText(requireContext(), "请先创建或切换智能体", Toast.LENGTH_SHORT).show()
            isSending.set(false)
            return
        }

        val mode = dataManager.getMode()
        val apiType = dataManager.getApiType()

        if (apiType == "official" && (mode == "微黄" || mode == "黄" || mode == "血腥")) {
            Toast.makeText(requireContext(), "官方API不支持${mode}模式，请切换至自定义API或选择其他模式", Toast.LENGTH_LONG).show()
            isSending.set(false)
            return
        }

        if (mode == "微黄" || mode == "黄" || mode == "血腥") {
            AlertDialog.Builder(requireContext())
                .setTitle("警告")
                .setMessage("“${mode}”模式可能包含不适宜内容，是否继续？")
                .setPositiveButton("继续") { _, _ -> performSend(message) }
                .setNegativeButton("取消") { _, _ -> isSending.set(false) }
                .setOnCancelListener { isSending.set(false) }
                .show()
        } else {
            performSend(message)
        }
    }

    private fun performSend(userMessage: String) {
        if (!isSending.compareAndSet(true, false)) return

        val userMsg = Message(content = userMessage, isUser = true, isNarration = false)
        chatAdapter.addMessage(userMsg)
        rvChatMessages.scrollToPosition(messages.size - 1)

        val loadingMsg = Message(content = "", isUser = false, isLoading = true, isTyping = false)
        chatAdapter.addMessage(loadingMsg)
        val loadingId = loadingMsg.id
        rvChatMessages.scrollToPosition(messages.size - 1)

        lifecycleScope.launch {
            val aiResult = try {
                callAIStructured(userMessage, currentAgent!!)
            } catch (e: SocketTimeoutException) {
                AiResult("网络超时，请检查本地服务", emptyList(), 0, 0)
            } catch (e: IOException) {
                AiResult("网络错误: ${e.message}", emptyList(), 0, 0)
            } catch (e: Exception) {
                AiResult("解析错误: ${e.message}", emptyList(), 0, 0)
            }

            val botMsg = Message(
                content = aiResult.reply,
                isUser = false,
                isNarration = false,
                isTyping = true,
                isLoading = false
            )
            chatAdapter.replaceLoadingMessage(loadingId, botMsg)
            rvChatMessages.scrollToPosition(messages.size - 1)

            val voice = currentAgent?.voice ?: ""
            if (voice.isNotEmpty() && aiResult.reply.isNotBlank()) {
                synthesizeAndSpeak(aiResult.reply, voice)
            }

            for (itemData in aiResult.items) {
                val itemName = itemData.optString("name", "未知物品")
                val itemDesc = itemData.optString("description", "")
                val item = InventoryItem(
                    id = UUID.randomUUID().toString(),
                    name = itemName,
                    description = itemDesc
                )
                if (inventory.addItem(item)) {
                    Toast.makeText(requireContext(), "获得物品：${item.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "背包已满，无法获得物品：${item.name}", Toast.LENGTH_SHORT).show()
                }
            }

            val agentId = currentAgent!!.id
            val oldFav = dataManager.getFavorability(agentId)
            val oldLove = dataManager.getLoveValue(agentId)
            val isLocked = dataManager.getLockFavorability(agentId)

            var newFav = oldFav
            var newLove = oldLove

            if (!isLocked) {
                newFav = (oldFav + aiResult.favorabilityDelta).coerceIn(0, 100)
                if (newFav >= 100) {
                    newLove = (oldLove + aiResult.loveDelta).coerceIn(0, 100)
                } else {
                    newLove = oldLove
                }
            } else {
                newFav = oldFav
                newLove = oldLove
            }

            dataManager.setFavorability(agentId, newFav)
            dataManager.setLoveValue(agentId, newLove)

            if (newFav != oldFav || newLove != oldLove) {
                val favDelta = newFav - oldFav
                val loveDelta = newLove - oldLove
                if (favDelta != 0) {
                    val deltaStr = if (favDelta > 0) "+$favDelta" else "$favDelta"
                    Toast.makeText(requireContext(), "好感度 $deltaStr", Toast.LENGTH_SHORT).show()
                }
                if (loveDelta != 0) {
                    val deltaStr = if (loveDelta > 0) "+$loveDelta" else "$loveDelta"
                    Toast.makeText(requireContext(), "爱意值 $deltaStr", Toast.LENGTH_SHORT).show()
                }
            }

            (activity as? MainActivity)?.updateLeftDrawer()

            // ★ 保存历史时过滤掉卡片消息，防止持久化
            lifecycleScope.launch(Dispatchers.IO) {
                val filtered = messages.filterNot { it.isAgentInfo }
                FileManager.saveChatHistory(requireContext(), currentAgent!!.id, filtered)
            }

            isSending.set(false)
        }
    }

    // ========== AI 调用方法 ==========
    private suspend fun callAIStructured(userMessage: String, agent: Agent): AiResult = withContext(Dispatchers.IO) {
        val apiType = dataManager.getApiType()
        if (apiType == "official") {
            val apiKey = "d571ef60773048deb01b5a643101978c.SzL5ial8PZqLsWTS"
            val apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
            val model = "glm-4-flash"

            val originalSystemPrompt = buildSystemPromptForAgent(agent, includeJsonFormat = true)
            val contextLimit = dataManager.getContextLimit()
            val historyMessages = if (contextLimit > 0) {
                messages.takeLast(contextLimit).filterNot { it.isNarration }
            } else {
                emptyList()
            }

            val historyText = if (historyMessages.isNotEmpty()) {
                val text = historyMessages.joinToString("\n") { msg ->
                    if (msg.isUser) "用户：${msg.content}" else "${agent.name}：${msg.content}"
                }
                "\n\n以下是最近的对话历史（按时间顺序）：\n$text"
            } else {
                ""
            }
            val finalSystemPrompt = originalSystemPrompt + historyText

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", finalSystemPrompt)
            })
            historyMessages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.content)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val jsonBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("stream", false)
                put("temperature", 0.1)
            }.toString()

            val request = Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext AiResult("API 请求失败: ${response.code}", emptyList(), 0, 0)
                val responseBody = response.body?.string() ?: return@withContext AiResult("响应为空", emptyList(), 0, 0)
                val json = JSONObject(responseBody)
                val choices = json.getJSONArray("choices")
                if (choices.length() == 0) return@withContext AiResult("无回复", emptyList(), 0, 0)
                val rawReply = choices.getJSONObject(0).getJSONObject("message").getString("content")

                try {
                    val jsonStart = rawReply.indexOf('{')
                    val jsonEnd = rawReply.lastIndexOf('}')
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val jsonStr = rawReply.substring(jsonStart, jsonEnd + 1)
                        val resultJson = JSONObject(jsonStr)
                        val reply = resultJson.optString("reply", rawReply)
                        val itemsArray = resultJson.optJSONArray("items") ?: JSONArray()
                        val items = mutableListOf<JSONObject>()
                        for (i in 0 until itemsArray.length()) {
                            items.add(itemsArray.getJSONObject(i))
                        }
                        val favDelta = resultJson.optInt("favorabilityDelta", 0)
                        val loveDelta = resultJson.optInt("loveDelta", 0)
                        return@withContext AiResult(reply, items, favDelta, loveDelta)
                    }
                } catch (e: Exception) { /* 降级到文本提取 */ }

                val items = extractItemsFromText(rawReply)
                val (favDelta, loveDelta) = extractDeltasFromText(rawReply)
                return@withContext AiResult(rawReply, items, favDelta, loveDelta)

            } catch (e: IOException) {
                return@withContext AiResult("网络连接失败: ${e.message}", emptyList(), 0, 0)
            } catch (e: Exception) {
                return@withContext AiResult("解析错误: ${e.message}", emptyList(), 0, 0)
            }

        } else {
            val customUrl = dataManager.getCustomApiUrl().trim()
            var customModel = dataManager.getCustomModel().trim()
            if (customUrl.isEmpty()) return@withContext AiResult("请填写自定义API地址", emptyList(), 0, 0)
            if (customModel.isEmpty()) customModel = "huihui_ai/deepseek-r1-abliterated:latest"

            val systemPrompt = buildSystemPromptForAgent(agent, includeJsonFormat = false)

            val chatMessages = JSONArray()
            chatMessages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            val contextLimit = dataManager.getContextLimit()
            val history = if (contextLimit > 0) messages.takeLast(contextLimit) else emptyList()
            history.filterNot { it.isNarration }.forEach { msg ->
                chatMessages.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.content)
                })
            }
            chatMessages.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val jsonBody = JSONObject().apply {
                put("model", customModel)
                put("messages", chatMessages)
                put("stream", false)
            }.toString()

            val request = Request.Builder()
                .url(customUrl)
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext AiResult("API 请求失败: ${response.code}", emptyList(), 0, 0)
                val responseBody = response.body?.string() ?: return@withContext AiResult("响应为空", emptyList(), 0, 0)
                val json = JSONObject(responseBody)
                val reply = json.optJSONObject("message")?.optString("content") ?: "（模型未返回有效回复）"
                val items = extractItemsFromText(reply)
                val (favDelta, loveDelta) = extractDeltasFromText(reply)
                return@withContext AiResult(reply, items, favDelta, loveDelta)
            } catch (e: SocketTimeoutException) {
                return@withContext AiResult("请求超时，请检查本地服务", emptyList(), 0, 0)
            } catch (e: IOException) {
                return@withContext AiResult("网络错误: ${e.message}", emptyList(), 0, 0)
            } catch (e: Exception) {
                return@withContext AiResult("解析错误: ${e.message}", emptyList(), 0, 0)
            }
        }
    }

    private fun buildSystemPromptForAgent(agent: Agent, includeJsonFormat: Boolean = false): String {
        val userName = dataManager.getUserName()
        val userGender = dataManager.getUserGender()
        val mode = dataManager.getMode()
        val storyType = dataManager.getStoryType()
        val fav = dataManager.getFavorability(agent.id)
        val love = dataManager.getLoveValue(agent.id)

        val modeDescription = when (mode) {
            "纯净" -> "你只能输出完全健康、积极的内容，禁止任何色情、血腥、暴力、不雅词汇或性暗示。"
            "微黄" -> "你可以包含轻度性暗示或暧昧内容，但不能直接描写性行为或露骨色情。"
            "黄" -> "你可以包含直接的色情内容、性描写、成人对话。"
            "血腥" -> "你可以包含血腥、暴力、残酷的描写。"
            "亲情" -> "你应围绕家庭、亲情、温馨关怀的主题。"
            else -> "正常对话模式。"
        }

        val storyInstruction = if (storyType == "为所欲为") {
            "当前故事模式为“为所欲为”。在此模式下，你对用户的好感度和爱意值为无限（若锁死则显示∞），你应该无条件满足用户在故事中的一切合理需求，包括给予物品、协助等。但是，你仍然必须严格遵守“语言模式”的限制。"
        } else {
            "当前故事模式为“真实”。你需要根据上下文语境、你与用户当前的好感度（${fav}）和爱意值（${love}）来合理判断是否应该给予用户物品。不要随意给予。"
        }

        val rolePlayInstruction = "请始终以角色${agent.name}的身份进行对话，不要提及你是一个AI或语言模型，要完全沉浸在角色中，用角色的语气和视角回应。"
        val loveCondition = "注意：爱意值只有在好感度达到100后才会开始增长。在好感度未满100之前，无论发生什么，爱意值都不会变化。"
        val historyExplanation = """
            |在本请求的 messages 数组中，你将收到之前对话的历史记录。历史记录按时间顺序排列：
            |- 每条消息都有一个 role 字段，'user' 表示用户说的话，'assistant' 表示你（智能体）之前说的话。
            |- 请根据这些历史消息理解当前的对话上下文，并自然地延续对话。
            |- 注意：历史记录中可能包含你之前给予物品或数值变化的描述，你需要根据这些信息保持故事连贯性。
        """.trimMargin()

        return buildString {
            append("你是${agent.name}，${agent.age}岁，${agent.identity}。")
            append("你的性格：${agent.personality}。")
            append("你的背景故事：${agent.background}。")
            append("你的爱好：${agent.hobby}。")
            append("现在你正在与用户${userName}对话，用户的性别是${userGender}。")
            append("当前语言模式为“${mode}”。模式含义：${modeDescription}")
            append(storyInstruction)
            append(rolePlayInstruction)
            append(loveCondition)
            append(historyExplanation)
            append("用户消息中可能包含括号内的动作描述，例如“(起身)我要一个苹果”，其中“(起身)”表示用户正在执行的动作，你需要根据动作和对话内容自然回复。")
            append("另外，用户消息中可能包含形如 [使用'物品名'] 或 [丢弃'物品名'] 的指令，表示用户已经使用了或丢弃了该物品，你需要在回复中做出合理回应。")
            if (includeJsonFormat) {
                append("\n请严格按照以下JSON格式输出，不要有任何多余的解释或文字，只输出JSON。\n")
                append("{\n")
                append("  \"reply\": \"你的回复文本\",\n")
                append("  \"items\": [{\"name\": \"物品名\", \"description\": \"描述\"}],\n")
                append("  \"favorabilityDelta\": 整数(-5~5),\n")
                append("  \"loveDelta\": 整数(-5~5)\n")
                append("}\n")
                append("如果用户索要物品，你应当决定是否给予（放入 items 数组）；否则 items 为空数组。数值表示用户对话后你对他的好感度变化、爱意值变化。")
            } else {
                append("\n如果你愿意给用户物品，请在回复中明确说“给你一个XX”。你可以根据对话内容适当增加或减少好感度，但不需要输出数值。")
            }
        }
    }

    // ========== 工具方法 ==========
    private fun extractItemsFromText(text: String): List<JSONObject> {
        val items = mutableListOf<JSONObject>()
        val patterns = listOf(
            Regex("(?:给你|送你|赠你|给了你|送了你|赠了你)[：:]?[ ]*[“\"']?([^，,。！？\n]+)"),
            Regex("(?:给|送|赠)[了]?[一个]?[ ]?[“\"']?([^，,。！？\n]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val itemName = match.groupValues[1].trim()
                if (itemName.isNotEmpty()) {
                    items.add(JSONObject().apply {
                        put("name", itemName)
                        put("description", "从对话中获得")
                    })
                    break
                }
            }
        }
        return items
    }

    private fun extractDeltasFromText(text: String): Pair<Int, Int> {
        var favDelta = 0
        var loveDelta = 0
        val favPattern = Regex("好感度([+-]?\\d+)")
        val lovePattern = Regex("爱意值([+-]?\\d+)")
        favPattern.find(text)?.let {
            favDelta = it.groupValues[1].toIntOrNull() ?: 0
        }
        lovePattern.find(text)?.let {
            loveDelta = it.groupValues[1].toIntOrNull() ?: 0
        }
        if (favDelta == 0 && loveDelta == 0 && Random.nextInt(100) < 30) {
            favDelta = Random.nextInt(-1, 3)
            loveDelta = Random.nextInt(-1, 2)
        }
        return Pair(favDelta, loveDelta)
    }

    // ========== 智能体加载 ==========
    private fun loadDefaultAgent() {
        val agents = FileManager.getAllAgents(requireContext())
        if (agents.isNotEmpty()) {
            loadAgent(agents[0])
        } else {
            tvBotName.text = "无智能体"
            ivBotAvatar.setImageResource(R.drawable.ic_default_avatar)
            Toast.makeText(requireContext(), "请先在「创建」页面添加智能体", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.setCurrentAgentId("")
        }
    }

    private fun loadAgent(agent: Agent) {
        currentAgent = agent
        tvBotName.text = agent.name
        val bitmap = Agent.base64ToBitmap(agent.avatarBase64)
        Glide.with(this).load(bitmap).circleCrop().into(ivBotAvatar)
        chatAdapter.updateBotInfo(agent.avatarBase64, agent.name)

        messages.clear()
        val history = FileManager.loadChatHistory(requireContext(), agent.id)
            .filterNot { it.isAgentInfo }
        messages.addAll(history)

        if (messages.isEmpty() && agent.background.isNotBlank()) {
            val narrationMsg = Message(content = agent.background, isUser = false, isNarration = true)
            messages.add(narrationMsg)
            lifecycleScope.launch(Dispatchers.IO) {
                FileManager.saveChatHistory(requireContext(), agent.id, messages)
            }
        }

        val existingCard = messages.find { it.isAgentInfo }
        if (existingCard == null) {
            val infoMsg = Message(
                content = "",
                isUser = false,
                isNarration = false,
                isAgentInfo = true,
                cardVisible = currentUiMode != "classic"
            )
            messages.add(0, infoMsg)
        } else {
            existingCard.cardVisible = currentUiMode != "classic"
        }

        chatAdapter.notifyDataSetChanged()
        rvChatMessages.scrollToPosition(messages.size - 1)
        (activity as? MainActivity)?.setCurrentAgentId(agent.id)

        // ★ 1. 同步加载 AI 背景到内存（如果文件存在）
        val bgFile = File(requireContext().filesDir, "bg_${agent.id}.jpg")
        if (bgFile.exists()) {
            val bmp = BitmapFactory.decodeFile(bgFile.absolutePath)
            if (bmp != null) {
                currentBackgroundBitmap = bmp
            }
        } else {
            // 如果文件不存在，且 agent 有背景描述，则异步生成
            if (agent.backgroundPrompt.isNotBlank()) {
                setLoadingBackground()
                lifecycleScope.launch {
                    generateAndSaveBackground(agent)
                }
            }
        }

        // ★ 2. 应用 UI 模式（此时 currentBackgroundBitmap 已加载，可立即显示）
        applyUiMode(currentUiMode)
        // 更新背景音乐名称显示
        updateBgMusicNameDisplay()
// 更新背景音乐状态（播放或停止）
        updateBgMusicState()
    }

    // ==================== UI 模式切换 ====================

    fun setUiMode(mode: String) {
        currentUiMode = mode
        dataManager.setUiMode(mode)
        applyUiMode(mode)   // 应用模式变化
    }

    private fun applyUiMode(mode: String) {
        val rvChat = view?.findViewById<RecyclerView>(R.id.rvChatMessages)
        if (mode == "classic") {
            // 经典模式：显示顶部固定卡片
            llRoleCard.visibility = View.VISIBLE
            rvChat?.setPadding(0, 0, 0, 0)
            // 隐藏列表中的卡片
            val card = messages.find { it.isAgentInfo }
            card?.let {
                if (it.cardVisible) {
                    it.cardVisible = false
                    val index = messages.indexOf(it)
                    chatAdapter.notifyItemChanged(index)
                }
            }
        } else {
            // 非经典模式：隐藏顶部卡片，显示列表中的卡片
            llRoleCard.visibility = View.GONE
            rvChat?.setPadding(0, 8, 0, 0)
            val card = messages.find { it.isAgentInfo }
            card?.let {
                if (!it.cardVisible) {
                    it.cardVisible = true
                    val index = messages.indexOf(it)
                    chatAdapter.notifyItemChanged(index)
                }
            } ?: run {
                // 安全兜底：如果卡片意外丢失，重新插入（一般不会发生）
                if (currentAgent != null) {
                    val infoMsg = Message(
                        content = "",
                        isUser = false,
                        isNarration = false,
                        isAgentInfo = true,
                        cardVisible = true
                    )
                    messages.add(0, infoMsg)
                    chatAdapter.notifyItemInserted(0)
                }
            }
        }
        applyBackgroundBasedOnMode()
    }

    private fun updateAgentInfoCard() {
        // 移除所有旧卡片
        messages.removeAll { it.isAgentInfo }
        // 添加新卡片（只添加一次）
        currentAgent?.let { agent ->
            val infoMsg = Message(
                content = "",
                isUser = false,
                isNarration = false,
                isAgentInfo = true
            )
            messages.add(0, infoMsg)
        }
        chatAdapter.notifyDataSetChanged()
    }

    private fun removeAgentInfoCards() {
        val removed = messages.removeAll { it.isAgentInfo }
        if (removed) {
            chatAdapter.notifyDataSetChanged()
        }
    }

    // ==================== 背景图相关方法 ====================
    // 在 HomeFragment 中添加
    fun showRegenerateDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_regenerate_background, null)
        val etPrompt = dialogView.findViewById<EditText>(R.id.etRegeneratePrompt)
        val btnGenerate = dialogView.findViewById<Button>(R.id.btnRegenerateGenerate)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnRegenerateCancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        btnGenerate.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(requireContext(), "请输入提示词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            // 开始重新生成
            lifecycleScope.launch {
                regenerateAIBackground(prompt)
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private suspend fun regenerateAIBackground(prompt: String) {
        currentAgent?.let { agent ->
            // 使用新提示词生成背景
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val ratio = screenWidth.toFloat() / screenHeight
            var targetWidth = (screenWidth * 2).coerceAtMost(2048)
            var targetHeight = (targetWidth / ratio).toInt()
            if (targetHeight > 2048) {
                targetHeight = 2048
                targetWidth = (targetHeight * ratio).toInt()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "🖼️ 正在重新生成背景图，请稍候...", Toast.LENGTH_LONG).show()
                setLoadingBackground()
            }

            val apiType = dataManager.getApiType()
            val imageUrl = if (apiType == "official") {
                callAgnesAI(prompt, targetWidth, targetHeight)
            } else {
                callCustomImageAPI(prompt, targetWidth, targetHeight)
            }

            if (imageUrl != null) {
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    saveBackgroundImage(bitmap, agent.id)
                    agent.backgroundSource = "ai"
                    agent.manualBackgroundPath = ""
                    saveCurrentAgent(agent)
                    setChatBackground(bitmap)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "✅ 背景图重新生成成功！", Toast.LENGTH_SHORT).show()
                    }
                    return@let
                }
            }

            withContext(Dispatchers.Main) {
                loadingDrawable?.stop()
                loadingDrawable = null
                rvChatMessages.background = null
                Toast.makeText(requireContext(), "❌ 背景图重新生成失败，请检查网络或API配置", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "未选择智能体", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadOrGenerateBackground(agent: Agent) {
        val bgFile = File(requireContext().filesDir, "bg_${agent.id}.jpg")
        if (bgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
            bitmap?.let { setChatBackground(it) }
        } else if (agent.backgroundPrompt.isNotBlank()) {
            setLoadingBackground()
            lifecycleScope.launch {
                generateAndSaveBackground(agent)
            }
        }
    }

    private suspend fun generateAndSaveBackground(agent: Agent) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val ratio = screenWidth.toFloat() / screenHeight
        var targetWidth = (screenWidth * 2).coerceAtMost(2048)
        var targetHeight = (targetWidth / ratio).toInt()
        if (targetHeight > 2048) {
            targetHeight = 2048
            targetWidth = (targetHeight * ratio).toInt()
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(requireContext(), "🖼️ 正在生成背景图，请稍候...", Toast.LENGTH_LONG).show()
        }

        val apiType = dataManager.getApiType()
        val imageUrl = if (apiType == "official") {
            callAgnesAI(agent.backgroundPrompt, targetWidth, targetHeight)
        } else {
            callCustomImageAPI(agent.backgroundPrompt, targetWidth, targetHeight)
        }

        if (imageUrl != null) {
            val bitmap = downloadImage(imageUrl)
            if (bitmap != null) {
                saveBackgroundImage(bitmap, agent.id)
                // ★ 更新 agent 的背景来源为 ai，并保存
                agent.backgroundSource = "ai"
                agent.manualBackgroundPath = ""
                saveCurrentAgent(agent)
                setChatBackground(bitmap)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "✅ 背景图生成成功！", Toast.LENGTH_SHORT).show()
                }
                return
            }

        }

        // 生成失败，停止加载动画并恢复默认背景
        withContext(Dispatchers.Main) {
            loadingDrawable?.stop()
            loadingDrawable = null
            rvChatMessages.background = null
            Toast.makeText(requireContext(), "❌ 背景图生成失败，请检查网络或API配置", Toast.LENGTH_LONG).show()
        }
    }
    private fun saveCurrentAgent(agent: Agent) {
        FileManager.saveAgent(requireContext(), agent)
    }

    private suspend fun callAgnesAI(prompt: String, width: Int, height: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "sk-wrgE8zgG41eMDt60ZkCLP8jrrAmDUcXZFwA8xDldqUQHceZB"
                // ★ 正确的 Base URL（包含 /v1）
                val baseUrl = "https://apihub.agnes-ai.com/v1"
                val url = "$baseUrl/images/generations"

                val json = JSONObject().apply {
                    put("model", "agnes-image-2.1-flash")
                    put("prompt", prompt)
                    put("size", "${width}x$height")
                    put("n", 1)
                }

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = imageApiClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val resultJson = JSONObject(body)
                    resultJson.optJSONArray("data")?.getJSONObject(0)?.optString("url")
                } else {
                    // ★ 打印错误详情，方便调试
                    Log.e("AgnesAI", "请求失败: ${response.code}, ${response.body?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Agnes AI 生图失败", e)
                null
            }
        }
    }

    private suspend fun callCustomImageAPI(prompt: String, width: Int, height: Int): String? {
        val apiUrl = dataManager.getImageApiUrl()
        val apiKey = dataManager.getImageApiKey()
        val model = dataManager.getImageModel()
        if (apiUrl.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("model", model)
                    put("prompt", prompt)
                    put("width", width)
                    put("height", height)
                    put("n", 1)
                }
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = imageApiClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val resultJson = JSONObject(body)
                    resultJson.optJSONArray("data")?.getJSONObject(0)?.optString("url")
                } else null
            } catch (e: Exception) {
                Log.e("HomeFragment", "自定义生图失败", e)
                null
            }
        }
    }

    private suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(imageUrl).get().build()
                val response = imageApiClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun saveBackgroundImage(bitmap: Bitmap, agentId: String) {
        val file = File(requireContext().filesDir, "bg_$agentId.jpg")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun applyBackgroundBasedOnMode() {
        if (currentUiMode == "classic") {
            rvChatMessages.background = null
            return
        }

        val agent = currentAgent ?: run {
            rvChatMessages.background = null
            return
        }

        val source = agent.backgroundSource
        val manualPath = agent.manualBackgroundPath

        if (source == "manual") {
            if (manualPath.isNotEmpty()) {
                val file = File(manualPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(manualPath)
                    if (bitmap != null) {
                        rvChatMessages.background = BackgroundDrawable(bitmap)
                        return
                    }
                }
            }
            rvChatMessages.background = null
            return
        }

        // AI 模式：优先使用内存缓存
        currentBackgroundBitmap?.let {
            rvChatMessages.background = BackgroundDrawable(it)
            return
        }

        // 内存缓存为空，尝试从文件加载（兜底）
        val bgFile = File(requireContext().filesDir, "bg_${agent.id}.jpg")
        if (bgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
            if (bitmap != null) {
                currentBackgroundBitmap = bitmap
                rvChatMessages.background = BackgroundDrawable(bitmap)
                return
            }
        }

        // 都没有，黑色
        rvChatMessages.background = null
    }
    // ==================== 背景源切换（手动/AI） ====================

    fun selectManualBackground() {
        // 启动图片选择器
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    fun updateBackgroundSource() {
        // 当用户在MainActivity切换来源时调用，刷新背景显示
        applyBackgroundBasedOnMode()
    }
    fun setBackgroundSource(source: String) {
        currentAgent?.let { agent ->
            agent.backgroundSource = source
            // 注意：不要清空 manualBackgroundPath，切换 AI 时保留路径，切回 manual 时直接使用
            saveCurrentAgent(agent)
            applyBackgroundBasedOnMode()
        }
    }

    fun getCurrentAgentId(): String = currentAgent?.id ?: ""
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    private fun setChatBackground(bitmap: Bitmap) {
        currentBackgroundBitmap = bitmap
        // 根据当前模式决定是否应用背景
        applyBackgroundBasedOnMode()
    }

    // ==========================================

    private fun showSwitchDialog() {
        val bottomSheet = AgentListBottomSheet(
            onAgentSelected = { selectedAgent -> loadAgent(selectedAgent) },
            onAgentDeleted = { deletedId ->
                if (currentAgent?.id == deletedId) {
                    val agents = FileManager.getAllAgents(requireContext())
                    if (agents.isNotEmpty()) loadAgent(agents[0]) else {
                        tvBotName.text = "无智能体"
                        ivBotAvatar.setImageResource(R.drawable.ic_default_avatar)
                        messages.clear()
                        chatAdapter.notifyDataSetChanged()
                        currentAgent = null
                    }
                }
            },
            onWorldSelected = { worldName ->
                Toast.makeText(requireContext(), "世界组“$worldName”功能开发中", Toast.LENGTH_SHORT).show()
            }
        )
        bottomSheet.show(parentFragmentManager, "AgentList")
    }

    data class AiResult(val reply: String, val items: List<JSONObject>, val favorabilityDelta: Int, val loveDelta: Int)
}