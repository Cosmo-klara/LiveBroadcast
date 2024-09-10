package com.example.livebroadcast.dashWebM

import android.content.Context
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import com.example.livebroadcast.settingData.SettingData
import com.example.livebroadcast.handler.InternalAudioEncoder
import com.example.livebroadcast.handler.ScreenVideoEncoder
import com.example.livebroadcast.handler.StreamingInterface
import com.example.server.Server
import com.example.webm.WebM
import com.example.livebroadcast.utility.PartialMirroringPauseImageTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * 使用 MPEG-DASH 进行流式镜像时要使用的类
 * 定期剪下 WebM 并以 MPEG-DASH 格式分发。
 *
 * [prepareEncoder]函数的大小在 VP9的情况下会被忽略
 *
 * @param context [Context]
 * @param settingData
 */
class DashStreaming(
    private val context: Context,
    override val parentFolder: File,
    override val settingData: SettingData,
) : StreamingInterface {

    // 管理生成的文件
    private var contentManager: ContentManager? = null

    // 写入容器的类
    private var webMWriter: WebMWriter? = null

    // 视频编码器
    private var screenVideoEncoder: ScreenVideoEncoder? = null

    // 内部音频编码器。如果不使用, null
    private var internalAudioEncoder: InternalAudioEncoder? = null

    // 服务器
    private var server: Server? = null

    // 是否初始化内部音频编码器
    private val isInitializedInternalAudioEncoder: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && internalAudioEncoder != null

    override suspend fun startServer() = withContext(Dispatchers.Default) {
        // 服务器启动。在此阶段，浏览器端的查看页面可用
        server = Server(
            portNumber = settingData.portNumber,
            hostingFolder = ContentManager.getOutputFolder(parentFolder),
            indexHtml = INDEX_HTML
        ).apply { startServer() }
    }

    override suspend fun prepareAndStartEncode(
        mediaProjection: MediaProjection,
        videoHeight: Int,
        videoWidth: Int
    ) = withContext(Dispatchers.Default) {
        // 初始化编码器
        // 内容管理,清除以前的数据
        contentManager = ContentManager(
            parentFolder = parentFolder,
            audioPrefixName = AUDIO_FILE_PREFIX_NAME,
            videoPrefixName = VIDEO_FILE_PREFIX_NAME
        ).apply { deleteGenerateFile() }
        val isVP8 = settingData.isVP8
        // 准备编码器
        screenVideoEncoder =
            ScreenVideoEncoder(context.resources.configuration.densityDpi, mediaProjection).apply {
                prepareEncoder(
                    videoWidth = settingData.videoWidth,
                    videoHeight = settingData.videoHeight,
                    bitRate = settingData.videoBitRate,
                    frameRate = settingData.videoFrameRate,
                    isMirroringExternalDisplay = settingData.isMirroringExternalDisplay,
                    codecName = if (isVP8) MediaFormat.MIMETYPE_VIDEO_VP8 else MediaFormat.MIMETYPE_VIDEO_VP9,
                    altImageBitmap = PartialMirroringPauseImageTool.generatePartialMirroringPauseImage(
                        context,
                        settingData.videoWidth,
                        settingData.videoHeight
                    )
                )
            }
        // 如果需要编码内部音频
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && settingData.isRecordInternalAudio) {
            internalAudioEncoder = InternalAudioEncoder(mediaProjection).apply {
                prepareEncoder(
                    bitRate = settingData.audioBitRate,
                    isOpus = true,
                )
            }
        }
        // 创建 MPEG-DASH 初始化段
        webMWriter = WebMWriter()
        val webMWriter = webMWriter!!
        val dashContentManager = contentManager!!
        // 映像和音频会分别在不同的 WebM文件中传送，需要分别制作。
        if (isInitializedInternalAudioEncoder) {
            dashContentManager.createFile(AUDIO_INIT_SEGMENT_FILENAME).also { init ->
                webMWriter.createAudioInitSegment(
                    filePath = init.path,
                    channelCount = 2,
                    samplingRate = 48_000
                )
            }
        }
        dashContentManager.createFile(VIDEO_INIT_SEGMENT_FILENAME).also { init ->
            webMWriter.createVideoInitSegment(
                filePath = init.path,
                codecName = if (isVP8) WebM.VP8_CODEC_NAME else WebM.VP9_CODEC_NAME,
                videoWidth = settingData.videoWidth,
                videoHeight = settingData.videoHeight
            )
        }
        dashContentManager.createFile(MANIFEST_FILENAME).apply {
            writeText(
                DashManifestTool.createManifest(
                    fileIntervalSec = (settingData.intervalMs / 1_000).toInt(),
                    whetherAudio = settingData.isRecordInternalAudio,
                    isVP8 = isVP8
                )
            )
        }

        // 开始编码
        val videoEncoderJob = launch {
            try {
                screenVideoEncoder!!.start(
                    onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                        webMWriter.appendVideoEncodeData(byteBuffer, bufferInfo)
                    },
                    onOutputFormatAvailable = {
                        //
                    }
                )
            } finally {
                screenVideoEncoder!!.release()
            }
        }
        // 内置音频编码器
        val audioEncoderJob = if (isInitializedInternalAudioEncoder) {
            launch {
                try {
                    internalAudioEncoder?.start(
                        onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                            webMWriter.appendAudioEncodeData(byteBuffer, bufferInfo)
                        },
                        onOutputFormatAvailable = {
                            //
                        }
                    )
                } finally {
                    internalAudioEncoder?.release()
                }
            }
        } else null
        val segmentFileCreateJob = launch {
            delay(settingData.intervalMs)
            // 定期重新创建文件，以创建按顺序编号的段文件，以便与 MPEG-DASH 一起使用
            while (isActive) {
                // 测量保存文件所需的时间
                val time = measureTimeMillis {
                    // 创建一个文件大约需要 50 毫秒，并行一下
                    listOf(
                        launch {
                            dashContentManager.createIncrementAudioFile { segment ->
                                webMWriter.createAudioMediaSegment(segment.path)
                            }
                        },
                        launch {
                            dashContentManager.createIncrementVideoFile { segment ->
                                webMWriter.createVideoMediaSegment(segment.path)
                            }
                        }
                    ).joinAll()
                }
                // 如果不减去文件保存所花费的时间，保存所需的时间会积累起来，导致分段文件无法及时生成。
                // MPEG-DASH 需要按照时间顺序生成段文件，因此必须按时生成段文件。
                delay(settingData.intervalMs - time)
            }
        }
        // 等待加入，直到任务结束
        videoEncoderJob.join()
        audioEncoderJob?.join()
        segmentFileCreateJob.join()
    }

    override fun destroy() {
        server?.stopServer()
    }

    companion object {
        // 音频媒体段名称
        private const val AUDIO_FILE_PREFIX_NAME = "audio"

        // 视频媒体段名称
        private const val VIDEO_FILE_PREFIX_NAME = "video"

        // 音频初始化段名称
        private const val AUDIO_INIT_SEGMENT_FILENAME = "audio_init.webm"

        // 视频初始化段名称
        private const val VIDEO_INIT_SEGMENT_FILENAME = "video_init.webm"

        // 清单文件名称
        private const val MANIFEST_FILENAME = "manifest.mpd"

        private const val INDEX_HTML =
            """
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MPEG-DASH</title>
    <style>
        *{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body{
            min-height: 100vh;
            display:flex;
            justify-content: center;
            align-items: center;
            background-color: #8696a7;
        }
        .container{
            position: relative;
            justify-content: center;
            align-items: center;
            text-align: center;
            flex-wrap: wrap;
        }
        .container .card{
            z-index: 1;
            position: relative;
            width: 1000px;
            height: 600px;
            background-color: rgba(255,255,255,0.1);
            margin: 100px;
            border-radius: 15px;
            box-shadow: 20px 20px 50px rgba(0,0,0,0.5);
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
            border-top: 1px solid rgba(255,255,255,0.5);
            border-left: 1px solid rgba(255,255,255,0.5);
            backdrop-filter: blur(5px);
        }
        video {
            width: 750px;
            height: 450px;
        }
    </style>
</head>

<body>
<div class="container">
    <p>如果没有出现视频，请尝试重新加载</p>
    <div class="card">
        <video id="videoPlayer" controls muted autoplay></video>
    </div>
</div>
    <script src="https://cdn.dashjs.org/latest/dash.all.debug.js"></script>
    <script>
        (function () {
            var url = "manifest.mpd";
            var player = dashjs.MediaPlayer().create();
            player.initialize(document.querySelector("#videoPlayer"), url, true);
        })();
    </script>
</body>

</html>
            """

    }

}