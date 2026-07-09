package com.example.aipc

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class FeedbackDialog : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBilibili).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://space.bilibili.com/1544299698"))
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnEmail).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:woz_525@qq.com")
                putExtra(Intent.EXTRA_SUBJECT, "AIPC 用户反馈")
                putExtra(Intent.EXTRA_TEXT, "请在此输入您的反馈内容...")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "未检测到邮件应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}