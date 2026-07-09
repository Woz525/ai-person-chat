package com.example.aipc

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

object BatteryOptimizationHelper {

    /**
     * 跳转到系统电池优化设置页面
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开电池优化设置，请手动在系统设置中关闭", Toast.LENGTH_LONG).show()
        }
    }
}