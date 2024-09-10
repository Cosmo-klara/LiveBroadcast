package com.example.livebroadcast.handler

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.example.livebroadcast.handler.opengl.InSurface
import com.example.livebroadcast.handler.opengl.TextureRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 视频编码器
 * 输入接口编码为 H264 / VP9。
 */
class VideoEncoder {

    private var mediaCodec: MediaCodec? = null

    // 编码开始时间戳，用于计算每个编码帧的 presentationTimeUs
    private var startUs = 0L

    private var inputOpenGlSurface: InSurface? = null

    var drawSurface: Surface? = null
        private set

    var isDrawAltImage = false

    /**
     * 初始化编码器
     *
     * @param videoWidth
     * @param videoHeight
     * @param bitRate
     * @param frameRate
     * @param iFrameInterval
     * @param codecName [MediaFormat.MIMETYPE_VIDEO_VP9] 和 [MediaFormat.MIMETYPE_VIDEO_AVC] 等
     * @param altImageBitmap 使用 [VideoEncoder.isDrawAltImage] 显示时的图像
     */
    suspend fun prepareEncoder(
        videoWidth: Int,
        videoHeight: Int,
        bitRate: Int,
        frameRate: Int,
        iFrameInterval: Int = 1,
        codecName: String,
        altImageBitmap: Bitmap
    ) =
        withContext(openGlRelatedDispatcher) {
            // 在 OpenGL 中，swapBuffers等只能在执行 makeCurrent 的线程中完成，因此用 withContext 指定线程。
            val videoEncodeFormat =
                MediaFormat.createVideoFormat(codecName, videoWidth, videoHeight).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                }
            mediaCodec = MediaCodec.createEncoderByType(codecName).apply {
                configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            // 输入编码器之前先经过OpenGL
            inputOpenGlSurface = InSurface(mediaCodec!!.createInputSurface(), TextureRenderer())
            inputOpenGlSurface?.makeCurrent()
            inputOpenGlSurface?.createRender(videoWidth, videoHeight)
            inputOpenGlSurface?.setAltImageTexture(altImageBitmap)

            // OpenGL 绘制接口
            drawSurface = inputOpenGlSurface?.drawSurface
        }

    /**
     * 启动编码器并开始使用 OpenGL 进行绘图
     *
     * @param onOutputBufferAvailable 编码数据流
     * @param onOutputFormatAvailable 编码后的MediaFormat可用
     */
    suspend fun startVideoEncode(
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        val bufferInfo = MediaCodec.BufferInfo()
        mediaCodec?.start()
        startUs = System.nanoTime() / 1000L

        // OpenGL使用线程来识别上下文，因此指定OpenGL线程（Dispatcher）
        val openGlRendererJob = launch(openGlRelatedDispatcher) {
            while (isActive) {
                try {
                    if (isDrawAltImage) {
                        inputOpenGlSurface?.drawAltImage()
                        inputOpenGlSurface?.swapBuffers()
                        delay(16)
                    } else {
                        val isNewFrameAvailable = inputOpenGlSurface?.awaitIsNewFrameAvailable()
                        if (isNewFrameAvailable == true) {
                            inputOpenGlSurface?.makeCurrent()
                            inputOpenGlSurface?.updateTexImage()
                            inputOpenGlSurface?.drawImage()
                            inputOpenGlSurface?.swapBuffers()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 编码器循环
        val encoderJob = launch {
            try {
                while (isActive) {
                    val outputBufferId = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    if (outputBufferId >= 0) {
                        val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferId)!!
                        if (bufferInfo.size > 1) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                val fixBufferInfo = MediaCodec.BufferInfo().apply {
                                    set(
                                        bufferInfo.offset,
                                        bufferInfo.size,
                                        bufferInfo.presentationTimeUs - startUs,
                                        bufferInfo.flags
                                    )
                                }
                                if (fixBufferInfo.presentationTimeUs > 0) {
                                    onOutputBufferAvailable(outputBuffer, fixBufferInfo)
                                }
                            }
                        }
                        mediaCodec!!.releaseOutputBuffer(outputBufferId, false)
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        onOutputFormatAvailable(mediaCodec!!.outputFormat)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        openGlRendererJob.join()
        encoderJob.join()
    }

    fun release() {
        try {
            inputOpenGlSurface?.release()
            drawSurface?.release()
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // 协程调度器
        @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
        private val openGlRelatedDispatcher =
            newSingleThreadContext("OpenGlRelatedDispatcher")

        private const val TIMEOUT_US = 10_000L
    }
}