package com.example.aipc

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class ApiFragment : Fragment() {

    private lateinit var dataManager: DataManager
    private lateinit var rgApiType: RadioGroup
    private lateinit var llCustomKey: View

    // 对话配置
    private lateinit var etApiKey: EditText
    private lateinit var etApiUrl: EditText
    private lateinit var etModel: EditText

    // 生图配置
    private lateinit var etImageApiKey: EditText
    private lateinit var etImageApiUrl: EditText
    private lateinit var etImageModel: EditText

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_api, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())
        prefs = requireContext().getSharedPreferences("aipc_prefs", Context.MODE_PRIVATE)

        rgApiType = view.findViewById(R.id.rgApiType)
        llCustomKey = view.findViewById(R.id.llCustomKey)

        etApiKey = view.findViewById(R.id.etApiKey)
        etApiUrl = view.findViewById(R.id.etApiUrl)
        etModel = view.findViewById(R.id.etModel)

        etImageApiKey = view.findViewById(R.id.etImageApiKey)
        etImageApiUrl = view.findViewById(R.id.etImageApiUrl)
        etImageModel = view.findViewById(R.id.etImageModel)

        val ivHelp = view.findViewById<ImageView>(R.id.ivHelp)

        // 强制清除一次生图密钥旧数据（仅首次）
        //prefs.edit().putString("image_api_key", "").commit()

        // 加载已保存的设置
        if (dataManager.getApiType() == "custom") {
            rgApiType.check(R.id.rbCustom)
            llCustomKey.visibility = View.VISIBLE

            // 对话配置
            etApiKey.setText(dataManager.getCustomApiKey())
            etApiUrl.setText(dataManager.getCustomApiUrl())
            etModel.setText(dataManager.getCustomModel())

            // 生图配置
            etImageApiKey.setText(dataManager.getImageApiKey())
            etImageApiUrl.setText(dataManager.getImageApiUrl())
            etImageModel.setText(dataManager.getImageModel())

            // 如果生图模型是旧默认值，清除
            if (dataManager.getImageModel() == "Agnes-Image-2.1-Flash") {
                prefs.edit().putString("image_model", "").commit()
                etImageModel.setText("")
            }
        } else {
            rgApiType.check(R.id.rbOfficial)
            llCustomKey.visibility = View.GONE
        }

        // ★★★ 对话密钥的 TextWatcher（正常工作） ★★★
        etApiKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("custom_api_key", s.toString()).commit()
            }
        })

        // 对话地址
        etApiUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("custom_api_url", s.toString()).commit()
            }
        })

        // 对话模型
        etModel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("custom_model", s.toString()).commit()
            }
        })

        // ★★★ 生图密钥的 TextWatcher（与对话密钥完全一样，只改 key） ★★★
        etImageApiKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("image_api_key", s.toString()).commit()
            }
        })

        // 生图地址
        etImageApiUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("image_api_url", s.toString()).commit()
            }
        })

        // 生图模型
        etImageModel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("image_model", s.toString()).commit()
            }
        })

        rgApiType.setOnCheckedChangeListener { _, checkedId ->
            val isCustom = checkedId == R.id.rbCustom
            if (isCustom) {
                llCustomKey.visibility = View.VISIBLE
                dataManager.setApiType("custom")
                Toast.makeText(requireContext(), "已切换到自定义API，请填写对话和生图配置", Toast.LENGTH_SHORT).show()
            } else {
                llCustomKey.visibility = View.GONE
                dataManager.setApiType("official")
                Toast.makeText(requireContext(), "已切换到官方API [限每分钟次数]", Toast.LENGTH_SHORT).show()
            }
            (activity as? MainActivity)?.refreshModeState()
        }

        ivHelp.setOnClickListener {
            showApiHelpDialog()
        }
    }

    // ★ 二次保障：在 onStop 中同步保存所有值
    override fun onStop() {
        super.onStop()
        prefs.edit().apply {
            putString("custom_api_key", etApiKey.text.toString())
            putString("custom_api_url", etApiUrl.text.toString())
            putString("custom_model", etModel.text.toString())
            putString("image_api_key", etImageApiKey.text.toString())
            putString("image_api_url", etImageApiUrl.text.toString())
            putString("image_model", etImageModel.text.toString())
        }.commit()
    }

    private fun showApiHelpDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_api_help, null)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()

        btnClose.setOnClickListener { dialog.dismiss() }
    }
}