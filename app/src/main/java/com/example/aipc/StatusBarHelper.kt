package com.example.aipc

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

object StatusBarHelper {
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        if (result == 0) {
            result = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics).toInt()
        }
        return result
    }
}