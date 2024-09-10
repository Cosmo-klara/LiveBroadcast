package com.example.livebroadcast.settingData

import com.example.livebroadcast.R

/**
 * 模式列表
 * 提供了高拓展的可选择编码方式
 *
 * @param title 模式 ID
 * @param message 描述 ID
 * @param icon 图标
 *
 */
enum class StreamingType(val title: Int, val message: Int, val icon: Int) {
    /** 使用 MPEG-DASH 交付 */
    MpegDash(
        title = R.string.streaming_setting_type_mpeg_dash_title,
        message = R.string.streaming_setting_type_mpeg_dash_description,
        icon = R.drawable.streaming_type_dash
    ),
    // 在此处可以拓展编码方式，需要在[value.xml]和 [ScreenMirroringService.kt]下补充对应内容


}