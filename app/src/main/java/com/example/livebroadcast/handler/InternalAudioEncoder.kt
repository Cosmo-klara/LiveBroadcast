package com.example.livebroadcast.handler

import android.annotation.SuppressLint
import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 使用 PCM 提取内部音频并使用 AAC Opus 进行编码
 *
 * @param mediaProjection [MediaProjection]，用于录制内部音频
 */
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioEncoder(mediaProjection: MediaProjection) {

    // 封装音频编码器
    private val audioEncoder = AudioEncoder()

    private var audioRecord: AudioRecord? = null

    init {
        // 用于捕获内部音频
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(48_000)
            setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        }.build()
        audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
        }.build()
        audioRecord!!.startRecording()
    }

    /**
     * 初始化编码器
     *
     * @param sampleRate 采样率
     * @param channelCount 通道数
     * @param bitRate 比特率
     * @param isOpus 如果 Opus 用作编解码器，则为 true
     */
    fun prepareEncoder(
        sampleRate: Int = 48_000,
        channelCount: Int = 2,
        bitRate: Int = 192_000,
        isOpus: Boolean = false,
    ) = audioEncoder.prepareEncoder(sampleRate, channelCount, bitRate, isOpus)

    /**
     * 启动编码器。提取内部音频并编码
     *
     * @param onOutputBufferAvailable
     * @param onOutputFormatAvailable
     */
    suspend fun start(
        onOutputBufferAvailable: suspend (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        onOutputFormatAvailable: suspend (MediaFormat) -> Unit,
    ) = withContext(Dispatchers.Default) {
        audioEncoder.startAudioEncode(
            onRecordInput = { bytes ->
                // 提取和编码 PCM 音频
                return@startAudioEncode audioRecord!!.read(bytes, 0, bytes.size)
            },
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
        audioEncoder.release()
        audioRecord?.stop()
        audioRecord?.release()
    }

}