package com.example.aipc

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream
import java.util.*
import java.io.File
import java.io.FileOutputStream

class CreateFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var cvAvatar: CardView
    private lateinit var btnMale: Button
    private lateinit var btnFemale: Button
    private lateinit var btnCreate: Button
    private lateinit var etName: EditText
    private lateinit var etPersonality: EditText
    private lateinit var etBackstory: EditText
    private lateinit var etIdentity: EditText
    private lateinit var etAge: EditText
    private lateinit var etHobby: EditText

    // 音色相关
    private lateinit var btnVoice: ImageButton
    private lateinit var tvVoiceStatus: TextView
    private lateinit var llVoiceSelector: LinearLayout
    private var selectedVoice: String = ""

    // ★ 新增：背景图描述
    private lateinit var etBackgroundPrompt: EditText
    private lateinit var btnImportBgMusic: Button
    private lateinit var tvBgMusicName: TextView
    private var selectedBgMusicUri: Uri? = null
    private var importedBgMusicPath: String = ""

    private var selectedAvatarBitmap: Bitmap? = null
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedBgMusicUri = uri
            tvBgMusicName.text = "已选择：${uri.lastPathSegment}"
        }
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            uri?.let {
                try {
                    val bitmap = getBitmapFromUriCompat(it)
                    val scaledBitmap = bitmap?.let { bmp ->
                        val maxSize = 512
                        val width = bmp.width
                        val height = bmp.height
                        if (width > maxSize || height > maxSize) {
                            val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
                            val newWidth = (width * ratio).toInt()
                            val newHeight = (height * ratio).toInt()
                            Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true)
                        } else bmp
                    }
                    selectedAvatarBitmap = scaledBitmap
                    Glide.with(this).load(scaledBitmap).circleCrop().into(ivAvatar)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            ImagePickerHelper.pickImage(pickImageLauncher)
        } else {
            Toast.makeText(requireContext(), "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivAvatar = view.findViewById(R.id.ivAvatar)
        cvAvatar = view.findViewById(R.id.cvAvatar)
        btnMale = view.findViewById(R.id.btnMale)
        btnFemale = view.findViewById(R.id.btnFemale)
        btnCreate = view.findViewById(R.id.btnCreate)
        etName = view.findViewById(R.id.etName)
        etPersonality = view.findViewById(R.id.etPersonality)
        etBackstory = view.findViewById(R.id.etBackstory)
        etIdentity = view.findViewById(R.id.etIdentity)
        etAge = view.findViewById(R.id.etAge)
        etHobby = view.findViewById(R.id.etHobby)
        btnImportBgMusic = view.findViewById(R.id.btnImportBgMusic)
        tvBgMusicName = view.findViewById(R.id.tvBgMusicName)


        btnImportBgMusic.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        // 音色控件
        btnVoice = view.findViewById(R.id.btnVoice)
        tvVoiceStatus = view.findViewById(R.id.tvVoiceStatus)
        llVoiceSelector = view.findViewById(R.id.llVoiceSelector)

        // ★ 新增：背景图描述
        etBackgroundPrompt = view.findViewById(R.id.etBackgroundPrompt)

        btnImportBgMusic = view.findViewById(R.id.btnImportBgMusic)
        tvBgMusicName = view.findViewById(R.id.tvBgMusicName)

        // 导入按钮（原有）
        val btnImport = view.findViewById<Button>(R.id.btnImport)
        btnImport.setOnClickListener {
            Toast.makeText(requireContext(), "暂未开发直接导入智能体文件的功能", Toast.LENGTH_LONG).show()
        }
        btnImportBgMusic.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }
        cvAvatar.setOnClickListener {
            (activity as? MainActivity)?.checkAndRequestStoragePermission()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ImagePickerHelper.pickImage(pickImageLauncher)
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ImagePickerHelper.pickImage(pickImageLauncher)
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        btnMale.setOnClickListener { setGenderSelected(true) }
        btnFemale.setOnClickListener { setGenderSelected(false) }
        setGenderSelected(true)

        val voiceClickAction = View.OnClickListener {
            showVoiceSelectDialog()
        }
        llVoiceSelector.setOnClickListener(voiceClickAction)
        btnVoice.setOnClickListener(voiceClickAction)

        btnCreate.setOnClickListener {
            createAgent()
        }
    }

    private fun setGenderSelected(isMale: Boolean) {
        if (isMale) {
            btnMale.backgroundTintList = requireContext().getColorStateList(R.color.sex_selected)
            btnFemale.backgroundTintList = requireContext().getColorStateList(R.color.sex_unselected)
        } else {
            btnMale.backgroundTintList = requireContext().getColorStateList(R.color.sex_unselected)
            btnFemale.backgroundTintList = requireContext().getColorStateList(R.color.sex_selected)
        }
    }

    private fun createAgent() {
        val name = etName.text.toString().trim()
        val personality = etPersonality.text.toString().trim()
        val backstory = etBackstory.text.toString().trim()
        val identity = etIdentity.text.toString().trim()
        val age = etAge.text.toString().trim()
        val hobby = etHobby.text.toString().trim()
        val gender = if (btnMale.backgroundTintList == requireContext().getColorStateList(R.color.sex_selected)) "男" else "女"
        val backgroundPrompt = etBackgroundPrompt.text.toString().trim()  // ★ 新增
        var bgMusicPath = ""
        if (selectedBgMusicUri != null) {
            try {
                val fileName = "bg_music_${UUID.randomUUID()}.mp3"
                val destFile = File(requireContext().filesDir, fileName)
                requireContext().contentResolver.openInputStream(selectedBgMusicUri!!)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                bgMusicPath = destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "音频导入失败", Toast.LENGTH_SHORT).show()
            }
        }


        if (name.isEmpty()) { Toast.makeText(requireContext(), "请填写名字", Toast.LENGTH_SHORT).show(); return }
        if (personality.isEmpty()) { Toast.makeText(requireContext(), "请填写性格", Toast.LENGTH_SHORT).show(); return }
        if (backstory.isEmpty()) { Toast.makeText(requireContext(), "请填写背景故事", Toast.LENGTH_SHORT).show(); return }
        if (identity.isEmpty()) { Toast.makeText(requireContext(), "请填写身份", Toast.LENGTH_SHORT).show(); return }
        if (age.isEmpty()) { Toast.makeText(requireContext(), "请填写年龄", Toast.LENGTH_SHORT).show(); return }
        if (selectedAvatarBitmap == null) { Toast.makeText(requireContext(), "请选择头像", Toast.LENGTH_SHORT).show(); return }

        val avatarBase64 = Agent.bitmapToBase64(selectedAvatarBitmap!!)
        val id = UUID.randomUUID().toString()
        val firstMessage = "你好，我是$name，$personality，很高兴认识你"

        val agent = Agent(
            id = id,
            name = name,
            avatarBase64 = avatarBase64,
            personality = personality,
            background = backstory,
            identity = identity,
            age = age,
            gender = gender,
            firstMessage = firstMessage,
            hobby = hobby,
            voice = selectedVoice,
            backgroundPrompt = backgroundPrompt,   // ★ 新增
            bgMusicPath = bgMusicPath
        )

        FileManager.saveAgent(requireContext(), agent)

        // 清空表单
        etName.text.clear()
        etPersonality.text.clear()
        etBackstory.text.clear()
        etIdentity.text.clear()
        etAge.text.clear()
        etHobby.text.clear()
        selectedAvatarBitmap = null
        ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        setGenderSelected(true)
        selectedVoice = ""
        tvVoiceStatus.text = "(暂未选择)"
        etBackgroundPrompt.text.clear()  // ★ 新增

        Toast.makeText(requireContext(), "创建成功，智能体已保存", Toast.LENGTH_LONG).show()
        selectedBgMusicUri = null
        tvBgMusicName.text = "(未导入)"
    }

    private fun getBitmapFromUriCompat(uri: Uri): Bitmap? {
        return try {
            val contentResolver: ContentResolver = requireContext().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fis = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(fis)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showVoiceSelectDialog() {
        val dialog = VoiceSelectDialog { voiceName ->
            selectedVoice = voiceName
            tvVoiceStatus.text = "($voiceName)"
        }
        dialog.show(parentFragmentManager, "VoiceSelect")
    }

}