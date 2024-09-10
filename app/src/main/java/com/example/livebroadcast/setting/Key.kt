package com.example.livebroadcast.setting

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Key {

    // 端口号
    val PORT_NUMBER = intPreferencesKey("port_number")

    // 刷新间隔
    val INTERVAL_MS = longPreferencesKey("interval_ms")

    // 视频码率，单位为 bit
    val VIDEO_BIT_RATE = intPreferencesKey("video_bit_rate")

    // 视频帧率，fps
    val VIDEO_FRAME_RATE = intPreferencesKey("video_frame_rate")

    // 音频比特率，单位为 bit
    val AUDIO_BIT_RATE = intPreferencesKey("audio_bit_rate")

    // 是否录制内部音频
    val IS_RECORD_INTERNAL_AUDIO = booleanPreferencesKey("is_record_internal_audio")

    // 是否关闭 Nice to meet you屏幕的引导
    val IS_HIDE_HELLO_CARD = booleanPreferencesKey("is_hide_hello_card")

    // 是否指定分辨率
    val IS_CUSTOM_RESOLUTION = booleanPreferencesKey("is_custom_resolution")

    // 视频高度
    val VIDEO_HEIGHT = intPreferencesKey("video_height")

    // 视频宽度
    val VIDEO_WIDTH = intPreferencesKey("video_width")

    // 镜像外部输出显示器（例如 HDMI）
    val IS_MIRRORING_EXTERNAL_DISPLAY = booleanPreferencesKey("is_mirroring_external_display")

    // 流式传输方式
    val STREAMING_TYPE = stringPreferencesKey("streaming_type")

    // 使用 VP8 时为 true， VP9 无法播放时
    val MPEG_DASH_CODEC_VP8 = booleanPreferencesKey("streaming_type_mpeg_dash_vp8")
}