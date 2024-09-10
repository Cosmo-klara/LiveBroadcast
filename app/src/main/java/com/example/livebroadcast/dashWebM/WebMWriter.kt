package com.example.livebroadcast.dashWebM

import android.media.MediaCodec
import com.example.webm.WebM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class WebMWriter {

    // 处理 WebM 容器格式的类，用于音频
    private val audioWebM = WebM()

    // 处理视频的 WebM 容器格式的类
    private val videoWebM = WebM()

    // 写入文件时视频数据将被清除
    private val videoAppendBytes = arrayListOf<ByteArray>()

    // 写入文件时音频数据将被清除
    private val audioAppendBytes = arrayListOf<ByteArray>()

    // 当映像写入和重新创建段文件的处理同时运行时（ 从不同的 Job），会导致同时写入并且使映像变得混乱。
    // 互斥以确保即使它们是从不同的 Job 写入的，也只会串行写入。

    // 避免同时写入视频数据
    private val videoFileWriteMutex = Mutex()

    // 避免同时写入音频数据
    private val audioFileWriteMutex = Mutex()

    /**
     * 创建音频初始化段
     *
     * @param filePath 文件路径
     * @param channelCount 通道数
     * @param samplingRate 采样率
     */
    suspend fun createAudioInitSegment(
        filePath: String,
        channelCount: Int,
        samplingRate: Int,
    ) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = audioWebM.createEBMLHeader()
            val audioTrackSegment = audioWebM.createAudioSegment(
                muxingAppName = WebM.MUXING_APP,
                writingAppName = WebM.WRITING_APP,
                trackId = WebM.AUDIO_TRACK_ID,
                codecName = WebM.OPUS_CODEC_NAME,
                channelCount = channelCount,
                samplingRate = samplingRate.toFloat()
            )

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(audioTrackSegment.toElementBytes())
        }
    }

    /**
     * 创建视频初始化段
     *
     * @param filePath
     * @param videoHeight
     * @param videoWidth
     */
    suspend fun createVideoInitSegment(
        filePath: String,
        codecName: String = WebM.VP9_CODEC_NAME,
        videoWidth: Int,
        videoHeight: Int,
    ) = withContext(Dispatchers.IO) {
        File(filePath).also { initFile ->
            val ebmlHeader = videoWebM.createEBMLHeader()
            val segment = videoWebM.createVideoSegment(
                muxingAppName = WebM.MUXING_APP,
                writingAppName = WebM.WRITING_APP,
                trackId = WebM.VIDEO_TRACK_ID,
                codecName = codecName,
                videoWidth = videoWidth,
                videoHeight = videoHeight
            )

            initFile.appendBytes(ebmlHeader.toElementBytes())
            initFile.appendBytes(segment.toElementBytes())
        }
    }

    /**
     * 创建音频媒体片段
     *
     * @param filePath
     */
    suspend fun createAudioMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        audioFileWriteMutex.withLock {
            File(filePath).also { segmentFile ->
                // 使用 for 来应对 ConcurrentModificationException
                for (i in 0 until audioAppendBytes.size)
                    segmentFile.appendBytes(audioAppendBytes[i])
                audioAppendBytes.clear()
            }
        }
    }

    /**
     * 为视频创建媒体片段
     *
     * @param filePath
     */
    suspend fun createVideoMediaSegment(filePath: String) = withContext(Dispatchers.IO) {
        videoFileWriteMutex.withLock {
            File(filePath).also { segmentFile ->
                for (i in 0 until videoAppendBytes.size)
                    segmentFile.appendBytes(videoAppendBytes[i])
                videoAppendBytes.clear()
            }
        }
    }

    /**
     * 保存 MediaCodec 编码的音频数据
     *
     * @param bufferInfo
     * @param byteBuffer 编码结果
     */
    suspend fun appendAudioEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        audioFileWriteMutex.withLock {
            val byteArray = byteBuffer.asByteArray()
            // 对于 Opus 来说，它可能始终是关键帧
            val isKeyFrame = true
            audioAppendBytes += audioWebM.appendSimpleBlock(
                WebM.AUDIO_TRACK_ID,
                (bufferInfo.presentationTimeUs / 1000).toInt(),
                byteArray,
                isKeyFrame
            )
        }
    }

    /**
     * 保存 MediaCodec 编码的视频数据
     *
     * @param bufferInfo
     * @param byteBuffer
     */
    suspend fun appendVideoEncodeData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        videoFileWriteMutex.withLock {
            val byteArray = byteBuffer.asByteArray()
            val isKeyFrame = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
            videoAppendBytes += videoWebM.appendSimpleBlock(
                WebM.VIDEO_TRACK_ID,
                (bufferInfo.presentationTimeUs / 1000).toInt(),
                byteArray,
                isKeyFrame
            )
        }
    }

    // 将 [ByteBuffer] 转换为 [ByteArray]
    private fun ByteBuffer.asByteArray() = ByteArray(this.remaining()).also { byteArray ->
        this.get(byteArray)
    }

}