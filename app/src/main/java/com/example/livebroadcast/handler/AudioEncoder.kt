package com.example.livebroadcast.handler

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class AudioEncoder {

    // MediaCodec编码器
    private var mediaCodec: MediaCodec? = null

    // 视频切换时， presentationTimeUs 开始计数
    private var prevPresentationTimeUs = 0L

    /**
     * 编码器初始化
     *
     * @param sampleRate
     * @param channelCount
     * @param bitRate
     * @param isOpus
     */
    fun prepareEncoder(
        sampleRate: Int = 48_000,
        channelCount: Int = 2,
        bitRate: Int = 192_000,
        isOpus: Boolean = false,
    ) {
        val codec = if (isOpus) MediaFormat.MIMETYPE_AUDIO_OPUS else MediaFormat.MIMETYPE_AUDIO_AAC
        val audioEncodeFormat =
            MediaFormat.createAudioFormat(codec, sampleRate, channelCount).apply {
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            }
        mediaCodec = MediaCodec.createEncoderByType(codec).apply {
            configure(audioEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    /**
     * 启动编码器，使用协程
     *
     * @param onRecordInput 传递一个ByteArray，将音频数据放入其中并返回大小。
     * @param onOutputBufferAvailable
     * @param onOutputFormatAvailable
     */
    suspend fun startAudioEncode(
        onRecordInput: suspend (ByteArray) -> Int,
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        val bufferInfo = MediaCodec.BufferInfo()
        mediaCodec!!.start()

        try {
            while (isActive) {
                val inputBufferId = mediaCodec!!.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    // 将 AudioRecode 数据放入其中
                    val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferId)!!
                    val capacity = inputBuffer.capacity()
                    val byteArray = ByteArray(capacity)
                    // 将数据放入byteArray中
                    val readByteSize = onRecordInput(byteArray)
                    if (readByteSize > 0) {
                        // 写入的数据可以在[onOutputBufferAvailable]处接收
                        inputBuffer.put(byteArray, 0, readByteSize)
                        mediaCodec!!.queueInputBuffer(
                            inputBufferId,
                            0,
                            readByteSize,
                            System.nanoTime() / 1000,
                            0
                        )
                    }
                }
                val outputBufferId = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferId)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // 重新创建缓冲区信息
                            // 自从 MediaCodec 启动以来，BufferInfo 中的 presentationTimeUs 的值可能一直在不断增加
                            // 让它从0开始，重新创建
                            val fixBufferInfo = MediaCodec.BufferInfo().apply {
                                // 切换视频时输入0
                                if (prevPresentationTimeUs == 0L) {
                                    prevPresentationTimeUs = bufferInfo.presentationTimeUs
                                    set(bufferInfo.offset, bufferInfo.size, 0, bufferInfo.flags)
                                } else {
                                    set(
                                        bufferInfo.offset,
                                        bufferInfo.size,
                                        bufferInfo.presentationTimeUs - prevPresentationTimeUs,
                                        bufferInfo.flags
                                    )
                                }
                            }
                            onOutputBufferAvailable(outputBuffer, fixBufferInfo)
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

    fun release() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // 媒体编解码器超时
        private const val TIMEOUT_US = 10_000L

    }
}