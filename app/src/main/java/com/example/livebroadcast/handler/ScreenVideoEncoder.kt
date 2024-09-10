package com.example.livebroadcast.handler

import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.projection.MediaProjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 录制屏幕并编码
 *
 * 参考 ：[https://www.jianshu.com/p/d7eb518195fd]
 *
 * @param displayDpi DPI
 * @param mediaProjection [MediaProjection]
 */
class ScreenVideoEncoder(
    private val displayDpi: Int,
    private val mediaProjection: MediaProjection
) {

    // 封装视频编码器
    private val videoEncoder = VideoEncoder()

    // 屏幕录制
    private var virtualDisplay: VirtualDisplay? = null

    /**
     * 初始化编码器
     *
     * @param videoWidth
     * @param videoHeight
     * @param bitRate
     * @param frameRate
     * @param iFrameInterval
     * @param isMirroringExternalDisplay 镜像外部显示输出
     * @param codecName [MediaFormat.MIMETYPE_VIDEO_VP9]和[MediaFormat.MIMETYPE_VIDEO_AVC]等
     * @param altImageBitmap 切换应用程序时等参考：[MediaProjection.Callback.onCapturedContentVisibilityChanged]
     */
    suspend fun prepareEncoder(
        videoWidth: Int,
        videoHeight: Int,
        bitRate: Int,
        frameRate: Int,
        iFrameInterval: Int = 1,
        isMirroringExternalDisplay: Boolean,
        codecName: String,
        altImageBitmap: Bitmap
    ) {
        videoEncoder.prepareEncoder(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            bitRate = bitRate,
            frameRate = frameRate,
            iFrameInterval = iFrameInterval,
            codecName = codecName,
            altImageBitmap = altImageBitmap
        )
        withContext(Dispatchers.Main) {
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                    super.onCapturedContentVisibilityChanged(isVisible)
                    videoEncoder.isDrawAltImage = !isVisible
                }

            }, null)
        }
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "LiveBroadcast",
            videoWidth,
            videoHeight,
            displayDpi,
            if (isMirroringExternalDisplay) {
                // 镜像外接显示输出
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
            } else {
                // 设备端镜像
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
            },
            videoEncoder.drawSurface,
            null,
            null
        )
    }

    /**
     * 启动编码器
     *
     * @param onOutputBufferAvailable
     * @param onOutputFormatAvailable
     */
    suspend fun start(
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) {
        videoEncoder.startVideoEncode(
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                try {
                    onOutputBufferAvailable(byteBuffer, bufferInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onOutputFormatAvailable = onOutputFormatAvailable
        )
    }

    fun release() {
        videoEncoder.release()
        virtualDisplay?.release()
    }

}