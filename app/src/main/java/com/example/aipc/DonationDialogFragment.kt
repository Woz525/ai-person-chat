package com.example.aipc

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class DonationDialog : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_donation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnWatchAd).setOnClickListener {
            // 预留：激励视频广告
            Toast.makeText(requireContext(), "观看广告功能预留", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnWechatPay).setOnClickListener {
            // 预留：微信支付
            Toast.makeText(requireContext(), "微信支付功能预留", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}