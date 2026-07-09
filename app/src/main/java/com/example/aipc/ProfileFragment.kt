package com.example.aipc

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class ProfileFragment : Fragment() {

    private lateinit var tvEditableName: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var dataManager: DataManager

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = ImagePickerHelper.getPickedImageUri(result.data)
            uri?.let {
                dataManager.setAvatarPath(it.toString())
                Glide.with(this).load(it).circleCrop().into(ivAvatar)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        tvEditableName = view.findViewById(R.id.tvEditableName)
        ivAvatar = view.findViewById(R.id.ivProfileAvatar)

        val itemMyAgent = view.findViewById<TextView>(R.id.itemMyAgent)
        val itemTutorial = view.findViewById<TextView>(R.id.itemTutorial)
        val itemDonate = view.findViewById<TextView>(R.id.itemDonate)
        val itemSource = view.findViewById<TextView>(R.id.itemSource)
        val itemFeedback = view.findViewById<TextView>(R.id.itemFeedback)
        val itemUserAgreement = view.findViewById<TextView>(R.id.itemUserAgreement)

        loadUserData()

        ivAvatar.setOnClickListener {
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

        tvEditableName.setOnClickListener {
            showEditNameDialog()
        }

        // 我的智能体：使用主页相同的底部对话框（带三个按钮）
        itemMyAgent.setOnClickListener {
            showAgentSwitchBottomSheet()
        }

        // 教程：显示自定义弹窗
        itemTutorial.setOnClickListener {
            showTutorialDialog()
        }

        itemDonate.setOnClickListener {
            Toast.makeText(requireContext(), "心意已收到，感谢您的支持！", Toast.LENGTH_LONG).show()
        }
        itemSource.setOnClickListener { Toast.makeText(requireContext(), "源文件预留", Toast.LENGTH_SHORT).show() }
        itemFeedback.setOnClickListener {
            val dialog = FeedbackDialog()
            dialog.show(parentFragmentManager, "Feedback")
        }
        itemUserAgreement.setOnClickListener {
            showUserAgreementDialog()
        }
    }

    private fun loadUserData() {
        val userName = dataManager.getUserName()
        tvEditableName.text = if (userName.isNotEmpty()) userName else "点击编辑昵称"
        val avatarPath = dataManager.getAvatarPath()
        if (avatarPath.isNotEmpty()) {
            Glide.with(this).load(avatarPath).circleCrop().into(ivAvatar)
        }
    }

    private fun showUserAgreementDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("【法律声明】用户协议及免责条款")
            .setMessage(
                "欢迎使用 AIPC（以下简称“本软件”）。\n\n" +
                        "一、服务性质\n" +
                        "本软件官方内置的API服务（智谱AI）严格遵守中国法律法规，不提供任何色情、血腥、暴力或违规内容。\n\n" +
                        "二、【高风险功能】自定义API\n" +
                        "本软件提供“自定义API”功能，允许您连接您自己或第三方的API服务。您必须明确：\n" +
                        "1. 您使用该功能所连接的任何服务，其内容、合法性、安全性均由您自行完全负责；\n" +
                        "2. 您可能通过该功能接触到包括但不限于：色情、性暗示、血腥、暴力、仇恨言论、违反中国法律的内容；\n" +
                        "3. 本软件开发者无法、也不对自定义API中的任何内容进行事先审核或控制。\n\n" +
                        "三、用户承诺与责任\n" +
                        "您承诺：\n" +
                        "1. 您不会利用本软件制作、复制、发布、传播任何违反中华人民共和国法律法规的信息；\n" +
                        "2. 您使用自定义API的行为完全出于合法目的，且您已获得所连接API的合法授权；\n" +
                        "3. 您将对因使用自定义API而产生的所有后果（包括但不限于法律诉讼、行政处罚、民事赔偿）承担全部责任。\n\n" +
                        "四、免责声明\n" +
                        "本软件开发者（以下简称“我”）在此声明：\n" +
                        "1. 我不承担因您使用自定义API而导致的任何直接或间接损失；\n" +
                        "2. 我不承担因您违反法律法规使用本软件而产生的任何责任；\n" +
                        "3. 我不对任何第三方API的可用性、准确性、安全性提供任何担保。\n\n" +
                        "五、法律适用与争议解决\n" +
                        "本协议适用中华人民共和国法律。因本协议产生的任何争议，应提交开发者所在地有管辖权的人民法院诉讼解决。\n\n" +
                        "⚠️ 您已同意本协议。如需再次确认，请仔细阅读以上条款。"
            )
            .setPositiveButton("关闭") { _, _ -> }
            .show()
    }

    private fun showEditNameDialog() {
        val input = EditText(requireContext())
        input.setText(tvEditableName.text)
        AlertDialog.Builder(requireContext())
            .setTitle("编辑昵称")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    tvEditableName.text = newName
                    dataManager.setUserName(newName)
                } else {
                    Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 弹出智能体切换底部对话框（与主页相同）
    private fun showAgentSwitchBottomSheet() {
        val bottomSheet = AgentListBottomSheet(
            onAgentSelected = { selectedAgent ->
                // 切换智能体后，可以通知 HomeFragment 更新，但由于跨页面，简单提示即可
                Toast.makeText(requireContext(), "已切换到：${selectedAgent.name}", Toast.LENGTH_SHORT).show()
            },
            onAgentDeleted = { deletedId ->
                Toast.makeText(requireContext(), "已删除智能体", Toast.LENGTH_SHORT).show()
            },
            onWorldSelected = { worldName ->
                Toast.makeText(requireContext(), "世界组“$worldName”功能开发中", Toast.LENGTH_SHORT).show()
            }
        )
        bottomSheet.show(parentFragmentManager, "AgentList")
    }

    // 教程对话框
    private fun showTutorialDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tutorial, null)
        val tvText = dialogView.findViewById<TextView>(R.id.tvTutorialText)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val tutorialText = """
            本软件为ai角色扮演对话软件，开发者为Woz一人，为免费公益项目，允许进行改编，但禁止二次售卖。
            作者提供的官方api为智谱glm-4-flash正规LLM，尽管软件内有色情、血腥等违规的预设模式，但作者并不开放使用，也不承担法律责任，如想使用上述违规功能需使用自行提供或部署的LLM模型，其法律责任由用户自行承担。
            接下来为软件基本使用规则：
            一、api的连接
            官方提供智谱的api供用户免费使用正常功能，但如同一时间由多个用户同时使用必定会导致ai无法回复。这里建议自己去申请其他LLM模型的api接口，详情见导航栏中API分区的'如何获取api'
            二、基本对话规则语法
            对话中正常输入文字就是在故事中说的话，加入()中的文字为在故事中此处的行动，加入[]中的文字代表在故事中此处使用的物品，但必须是背包中的(为所欲为模式除外)
            三、基本的功能介绍
            左上角⭕为数值系统，这里将显示你与智能体的各项数值，如好感度、爱意值......
            右上角三横杠为菜单，这里可设置自己在此故事下的名称、故事模式、故事类型......
        """.trimIndent()

        tvText.text = tutorialText

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()

        btnClose.setOnClickListener { dialog.dismiss() }
    }
}