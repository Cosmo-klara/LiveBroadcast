package com.example.livebroadcast.handler

import android.media.projection.MediaProjection
import com.example.livebroadcast.settingData.SettingData
import java.io.File

interface StreamingInterface {

    val parentFolder: File

    // 镜像设置
    val settingData: SettingData

    suspend fun startServer()

    /**
     * 初始化编码器并开始编码
     *
     * 对视频和内部音频进行编码，并生成来连续文件
     *
     * 当镜像结束时，由于协程被取消，使用 try-finally 来释放资源。
     *
     * @param mediaProjection [MediaProjection]
     * @param videoHeight
     * @param videoWidth
     */
    suspend fun prepareAndStartEncode(
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int
    )

    fun destroy()
}