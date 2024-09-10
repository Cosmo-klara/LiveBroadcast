package com.example.livebroadcast.settingData

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.livebroadcast.setting.Key
import com.example.livebroadcast.setting.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 信息数据
 *
 * @param portNumber 端口号
 * @param videoBitRate 视频比特率
 * @param audioBitRate 音频比特率
 * @param videoFrameRate 视频帧率
 * @param intervalMs 视频裁剪间隔，毫秒
 * @param isRecordInternalAudio 如果要包含内部音频，则为 true，但它不会查看是否具有权限
 * @param isCustomResolution 如果自定义了视频分辨率，则为 true，如果为 false，则使用屏幕分辨率
 * @param videoHeight 视频高度
 * @param videoWidth 视频宽度
 * @param isMirroringExternalDisplay 外部输出显示器进行镜像时 true
 * @param streamingType 流式处理方法。 默认值为[StreamingType.MpegDash]
 * @param isVP8 在 [StreamingType.MpegDash] 的情况下，使用 VP8 作为视频编解码器时为 true
 */

data class SettingData(
    val portNumber: Int,
    val intervalMs: Long,
    val videoBitRate: Int,
    val videoFrameRate: Int,
    val audioBitRate: Int,
    val isRecordInternalAudio: Boolean,
    val isCustomResolution: Boolean,
    val videoHeight: Int,
    val videoWidth: Int,
    val isMirroringExternalDisplay: Boolean,
    val streamingType: StreamingType,
    val isVP8: Boolean,
) {

    companion object {

        // 默认端口号
        private const val DEFAULT_PORT_NUMBER = 2000

        // 默认文件生成间隔
        private const val DEFAULT_INTERVAL_MS = 1_000L

        // 默认视频比特率，似乎在哪里有问题--当我设为 1024K 时是 0Mbps，而导致视频无法显示
        private const val DEFAULT_VIDEO_BIT_RATE = 1_500_000

        // 默认音频比特率
        private const val DEFAULT_AUDIO_BIT_RATE = 128_000

        // 默认视频帧速率，似乎最高不能超过 60 帧率
        private const val DEFAULT_VIDEO_FRAME_RATE = 30

        // 默认视频宽度
        private const val DEFAULT_VIDEO_WIDTH = 1280

        // 默认视频高度
        private const val DEFAULT_VIDEO_HEIGHT = 720

        /**
         * 从数据存储读取并返回数据类
         *
         * @param context [context]
         */
        fun loadDataStore(context: Context): Flow<SettingData> {
            // Flow 接收数据存储更改，将其转换为数据类，然后返回
            return context.dataStore.data.map { data ->
                SettingData(
                    portNumber = data[Key.PORT_NUMBER] ?: DEFAULT_PORT_NUMBER,
                    intervalMs = data[Key.INTERVAL_MS] ?: DEFAULT_INTERVAL_MS,
                    videoBitRate = data[Key.VIDEO_BIT_RATE] ?: DEFAULT_VIDEO_BIT_RATE,
                    videoFrameRate = data[Key.VIDEO_FRAME_RATE]
                        ?: DEFAULT_VIDEO_FRAME_RATE,
                    audioBitRate = data[Key.AUDIO_BIT_RATE] ?: DEFAULT_AUDIO_BIT_RATE,
                    isRecordInternalAudio = data[Key.IS_RECORD_INTERNAL_AUDIO]
                        ?: false,
                    isCustomResolution = data[Key.IS_CUSTOM_RESOLUTION] ?: false,
                    videoWidth = data[Key.VIDEO_WIDTH] ?: DEFAULT_VIDEO_WIDTH,
                    videoHeight = data[Key.VIDEO_HEIGHT] ?: DEFAULT_VIDEO_HEIGHT,
                    isMirroringExternalDisplay = data[Key.IS_MIRRORING_EXTERNAL_DISPLAY]
                        ?: false,
                    streamingType = data[Key.STREAMING_TYPE]?.let {
                        StreamingType.valueOf(
                            it
                        )
                    } ?: StreamingType.MpegDash,
                    isVP8 = data[Key.MPEG_DASH_CODEC_VP8] ?: false
                )
            }
        }

        /**
         * 存储到[SettingData]
         *
         * @param context [Context]
         * @param settingData
         */
        suspend fun setDataStore(context: Context, settingData: SettingData) {
            context.dataStore.edit {
                it[Key.PORT_NUMBER] = settingData.portNumber
                it[Key.INTERVAL_MS] = settingData.intervalMs
                it[Key.VIDEO_BIT_RATE] = settingData.videoBitRate
                it[Key.VIDEO_FRAME_RATE] = settingData.videoFrameRate
                it[Key.AUDIO_BIT_RATE] = settingData.audioBitRate
                it[Key.IS_RECORD_INTERNAL_AUDIO] =
                    settingData.isRecordInternalAudio
                it[Key.IS_CUSTOM_RESOLUTION] = settingData.isCustomResolution
                it[Key.VIDEO_WIDTH] = settingData.videoWidth
                it[Key.VIDEO_HEIGHT] = settingData.videoHeight
                it[Key.IS_MIRRORING_EXTERNAL_DISPLAY] =
                    settingData.isMirroringExternalDisplay
                it[Key.STREAMING_TYPE] = settingData.streamingType.name
                it[Key.MPEG_DASH_CODEC_VP8] = settingData.isVP8
            }
        }

        /**
         * 重置设置
         * @param context [Context]
         */
        suspend fun resetDataStore(context: Context) {
            context.dataStore.edit {
                it -= Key.PORT_NUMBER
                it -= Key.INTERVAL_MS
                it -= Key.VIDEO_BIT_RATE
                it -= Key.VIDEO_FRAME_RATE
                it -= Key.AUDIO_BIT_RATE
                it -= Key.IS_RECORD_INTERNAL_AUDIO
                it -= Key.IS_CUSTOM_RESOLUTION
                it -= Key.VIDEO_WIDTH
                it -= Key.VIDEO_HEIGHT
                it -= Key.IS_MIRRORING_EXTERNAL_DISPLAY
                it -= Key.STREAMING_TYPE
                it -= Key.MPEG_DASH_CODEC_VP8
            }
        }
    }
}