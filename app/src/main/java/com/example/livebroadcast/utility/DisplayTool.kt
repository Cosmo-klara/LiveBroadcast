package com.example.livebroadcast.utility

import android.app.Activity
import androidx.window.layout.WindowMetricsCalculator

// 获取手机屏幕尺寸
object DisplayTool {

    /**
     * 屏幕尺寸
     *
     * @param activity [Activity]
     * @return （高，宽）
     */
    fun getDisplaySize(activity: Activity): Pair<Int, Int> {
        // 可以兼容 Android R (11) 以下
        val metrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(activity)
        val height = metrics.bounds.height()
        val width = metrics.bounds.width()
        return (height to width)
    }
}