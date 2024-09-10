package com.example.webm

import java.io.File

// 测试用，WebM 的结构非常容易理解，MKVToolNix 真是神一般的存在
fun main() {
    val file = File("init.webm")
    val webM = WebM()
    val ebmlHeader = webM.createEBMLHeader()
    val segment = webM.createSegment(
        muxingAppName = WebM.MUXING_APP,
        writingAppName = WebM.WRITING_APP,
        videoTrackId = WebM.VIDEO_TRACK_ID,
        videoCodecName = WebM.VIDEO_CODEC,
        videoWidth = WebM.VIDEO_WIDTH,
        videoHeight = WebM.VIDEO_HEIGHT,
        audioTrackId = WebM.AUDIO_TRACK_ID,
        audioCodecName = WebM.AUDIO_CODEC,
        channelCount = WebM.CHANNELS,
        samplingRate = WebM.SAMPLING_FREQUENCY
    )
    println(ebmlHeader.toElementBytes().toHexString())
    println(segment.toElementBytes().toHexString())
    file.appendBytes(ebmlHeader.toElementBytes())
    file.appendBytes(segment.toElementBytes())
}

class WebM {

    companion object {

        const val VIDEO_TRACK_ID = 1
        const val VIDEO_CODEC = "V_VP9"
        const val VIDEO_WIDTH = 1280
        const val VIDEO_HEIGHT = 720

        const val AUDIO_TRACK_ID = 2
        const val AUDIO_CODEC = "A_OPUS"
        const val SAMPLING_FREQUENCY = 48_000.0F
        const val CHANNELS = 2

        const val MUXING_APP = "LBL_webm"
        const val WRITING_APP = "LBL"

        const val SIMPLE_BLOCK_FLAGS_KEYFRAME = 0x80
        const val SIMPLE_BLOCK_FLAGS = 0x00

        const val EBML_VERSION = 0x01
        const val EBML_READ_VERSION = 0x01
        const val EBML_MAX_ID_LENGTH = 0x04
        const val EBML_MAXSIZE_LENGTH = 0x08
        const val EBML_DOCTYPE_WEBM = "webm"
        const val EBML_DOCTYOE_VERSION = 0x02
        const val EBML_DOCTYPE_READ_VERSION = 0x02

        const val INFO_TIMESCALE_MS = 1000000

        const val VP8_CODEC_NAME = "V_VP8"
        const val VP9_CODEC_NAME = "V_VP9"
        const val OPUS_CODEC_NAME = "A_OPUS"

        const val OPUS_HEAD = "OpusHead"
        const val OPUS_VERSION = 0x01

        // TrackType

        // 音轨
        const val TRACK_TYPE_AUDIO = 2

        // 视频轨道
        const val TRACK_TYPE_VIDEO = 1

        val UNKNOWN_SIZE = byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }

    // 自上次调用 [createSimpleBlock] 以来的时间刻度
    private var prevCreateClusterTimescale = 0

    /**
     * 轨道创建音频和视频片段
     * 有关参数参阅 [createAudioSegment] [createVideoSegment]
     */
    fun createSegment(
        muxingAppName: String,
        writingAppName: String,
        videoTrackId: Int,
        videoUid: Int = videoTrackId,
        videoCodecName: String,
        videoWidth: Int,
        videoHeight: Int,
        audioTrackId: Int,
        audioUid: Int = audioTrackId,
        audioCodecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val videoTrack = createVideoTrackEntryElement(videoTrackId, videoUid, videoCodecName, videoWidth, videoHeight)
        val audioTrack = createAudioTrackEntryElement(audioTrackId, audioUid, audioCodecName, channelCount, samplingRate)
        val tracks = EBMLElement(MatroskaTags.Tracks, videoTrack.toElementBytes() + audioTrack.toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * 创建一个轨道仅为视频的片段
     *
     * @param muxingAppName 输入将写入 WebM 的库的名称
     * @param writingAppName 输入使用库的应用程序的名称
     * @param trackId 轨道编号
     * @param uid 可以考虑加入[trackId]
     * @param codecName 编解码器名称
     * @param videoWidth
     * @param videoHeight
     */
    fun createVideoSegment(
        muxingAppName: String,
        writingAppName: String,
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        videoWidth: Int,
        videoHeight: Int,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val tracks = EBMLElement(MatroskaTags.Tracks, createVideoTrackEntryElement(trackId, uid, codecName, videoWidth, videoHeight).toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * 创建一个轨道仅为音频的片段
     *
     * @param muxingAppName
     * @param writingAppName
     * @param trackId
     * @param uid
     * @param channelCount 通道数，2个立体声
     * @param codecName
     * @param samplingRate 采样率
     */
    fun createAudioSegment(
        muxingAppName: String,
        writingAppName: String,
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        val info = createInfo(INFO_TIMESCALE_MS, muxingAppName, writingAppName)
        val tracks = EBMLElement(MatroskaTags.Tracks, createAudioTrackEntryElement(trackId, uid, codecName, channelCount, samplingRate).toElementBytes())
        val cluster = createStreamingCluster()

        val segmentValue = info.toElementBytes() + tracks.toElementBytes() + cluster.toElementBytes()
        return EBMLElement(MatroskaTags.Segment, segmentValue, UNKNOWN_SIZE)
    }

    /**
     * 添加数据
     * 返回一个字节数组
     *
     * @param trackNumber 曲目编号，判断是视频还是音频
     * @param timescaleMs 编码数据时间
     * @param byteArray 编码数据
     * @param isKeyFrame 如果它是一个关键帧，则为 true
     */
    fun appendSimpleBlock(
        trackNumber: Int,
        timescaleMs: Int,
        byteArray: ByteArray,
        isKeyFrame: Boolean = false,
    ): ByteArray {
        // CreateCluster 的相对时间
        val simpleBlockTimescale = timescaleMs - prevCreateClusterTimescale
        return if (isKeyFrame || simpleBlockTimescale > Short.MAX_VALUE) {
            // 如果时间超过 16位，则重新添加 Cluster，然后添加 SimpleBlock
            // 由于添加了 Cluster之后，SimpleBlock必须是关键帧，所以我在收到关键帧时重新创建 Cluster。
            // 使用 Android 的 ExoPlayer，除非在关键帧的开头，否则无法播放。
            prevCreateClusterTimescale = timescaleMs
            (createStreamingCluster(timescaleMs).toElementBytes() + createSimpleBlock(trackNumber, 0, byteArray, isKeyFrame).toElementBytes())
        } else {
            createSimpleBlock(trackNumber, simpleBlockTimescale, byteArray, isKeyFrame).toElementBytes()
        }
    }

    /**
     * 创建一个 SimpleBlock 放入 Cluster 中
     *
     * @param trackNumber
     * @param simpleBlockTimescale
     * @param byteArray
     * @param isKeyFrame
     */
    private fun createSimpleBlock(
        trackNumber: Int,
        simpleBlockTimescale: Int,
        byteArray: ByteArray,
        isKeyFrame: Boolean,
    ): EBMLElement {
        val vIntTrackNumberBytes = trackNumber.toVInt()
        val simpleBlockBytes = simpleBlockTimescale.toClusterTimescale()
        val flagsBytes = byteArrayOf((if (isKeyFrame) SIMPLE_BLOCK_FLAGS_KEYFRAME else SIMPLE_BLOCK_FLAGS).toByte())
        val simpleBlockValue = vIntTrackNumberBytes + simpleBlockBytes + flagsBytes + byteArray

        return EBMLElement(MatroskaTags.SimpleBlock, simpleBlockValue)
    }

    /**
     * 创建可流式传输的 Cluster
     * 数据大小未定义
     *
     * @param timescaleMs 开始时间(毫秒)
     */
    private fun createStreamingCluster(timescaleMs: Int = 0): EBMLElement {
        // 将时间转换为十六进制，并使其成为2字节的字节数组
        val timescaleBytes = timescaleMs.toByteArrayFat()
        val timescale = EBMLElement(MatroskaTags.Timestamp, timescaleBytes)
        val clusterValue = timescale.toElementBytes()

        return EBMLElement(MatroskaTags.Cluster, clusterValue, UNKNOWN_SIZE)
    }

    /**
     * 创建音轨
     *
     * @param trackId
     * @param uid
     * @param channelCount
     * @param codecName
     * @param samplingRate
     */
    private fun createAudioTrackEntryElement(
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        channelCount: Int,
        samplingRate: Float,
    ): EBMLElement {
        // 音轨信息
        val audioTrackNumber = EBMLElement(MatroskaTags.TrackNumber, trackId.toByteArray())
        val audioTrackUid = EBMLElement(MatroskaTags.TrackUID, uid.toByteArray())
        val audioCodecId = EBMLElement(MatroskaTags.CodecID, codecName.toAscii())
        val audioTrackType = EBMLElement(MatroskaTags.TrackType, TRACK_TYPE_AUDIO.toByteArray())
        // 在 Segment > Tracks > Audio 的 CodecPrivate 中放入内容
        // 创建 Opus 标题
        // https://www.rfc-editor.org/rfc/rfc7845
        // Version = 0x01
        // Channel Count = 0x02
        // Pre-Skip = 0x00 0x00
        // Input Sample Rate ( little endian ) 0x80 0xBB 0x00 0x00 --- Kotlin 是 Big endian，所以它是倒置的
        // Output Gain 0x00 0x00
        // Mapping Family 0x00
        // ??? 0x00 0x00
        val opusHeader = OPUS_HEAD.toAscii() + byteArrayOf(OPUS_VERSION.toByte()) + byteArrayOf(channelCount.toByte()) + byteArrayOf(0x00.toByte(), 0x00.toByte()) + samplingRate.toInt().toClusterTimescale().reversed() + byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val codecPrivate = EBMLElement(MatroskaTags.CodecPrivate, opusHeader)
        // 采样率是浮点数，所以需要一些额外的步骤。
        val sampleFrequency = EBMLElement(MatroskaTags.SamplingFrequency, samplingRate.toBits().toByteArray())
        val channels = EBMLElement(MatroskaTags.Channels, channelCount.toByteArray())
        val audioTrack = EBMLElement(MatroskaTags.AudioTrack, sampleFrequency.toElementBytes() + channels.toElementBytes())
        val audioTrackEntryValue = audioTrackNumber.toElementBytes() + audioTrackUid.toElementBytes() + audioCodecId.toElementBytes() + audioTrackType.toElementBytes() + codecPrivate.toElementBytes() + audioTrack.toElementBytes()

        return EBMLElement(MatroskaTags.TrackEntry, audioTrackEntryValue)
    }

    /**
     * 创建视频轨道
     *
     * @param trackId
     * @param uid
     * @param codecName
     * @param videoWidth
     * @param videoHeight
     */
    private fun createVideoTrackEntryElement(
        trackId: Int,
        uid: Int = trackId,
        codecName: String,
        videoWidth: Int,
        videoHeight: Int,
    ): EBMLElement {
        val videoTrackNumber = EBMLElement(MatroskaTags.TrackNumber, trackId.toByteArray())
        val videoTrackUid = EBMLElement(MatroskaTags.TrackUID, uid.toByteArray())
        val videoCodecId = EBMLElement(MatroskaTags.CodecID, codecName.toAscii())
        val videoTrackType = EBMLElement(MatroskaTags.TrackType, TRACK_TYPE_VIDEO.toByteArray())
        val pixelWidth = EBMLElement(MatroskaTags.PixelWidth, videoWidth.toByteArray())
        val pixelHeight = EBMLElement(MatroskaTags.PixelHeight, videoHeight.toByteArray())
        val videoTrack = EBMLElement(MatroskaTags.VideoTrack, pixelWidth.toElementBytes() + pixelHeight.toElementBytes())
        val videoTrackEntryValue = videoTrackNumber.toElementBytes() + videoTrackUid.toElementBytes() + videoCodecId.toElementBytes() + videoTrackType.toElementBytes() + videoTrack.toElementBytes()

        return EBMLElement(MatroskaTags.TrackEntry, videoTrackEntryValue)
    }

    /**
     * 创建信息元素
     *
     * @param timescaleMs [INFO_TIMESCALE_MS]
     * @param muxingAppName
     * @param writingAppName
     */
    private fun createInfo(
        timescaleMs: Int,
        muxingAppName: String,
        writingAppName: String,
    ): EBMLElement {
        val timestampScale = EBMLElement(MatroskaTags.TimestampScale, timescaleMs.toByteArray())
        val muxingApp = EBMLElement(MatroskaTags.MuxingApp, muxingAppName.toAscii())
        val writingApp = EBMLElement(MatroskaTags.WritingApp, writingAppName.toAscii())

        val infoValue = timestampScale.toElementBytes() + muxingApp.toElementBytes() + writingApp.toElementBytes()
        return EBMLElement(MatroskaTags.Info, infoValue)
    }

    /**
     * 创建 EBML 标头（ WebM！）
     *
     * @param version EBML版本
     * @param readVersion 解析器最低版本
     * @param maxIdLength 最大 ID长度
     * @param maxSizeLength 数据大小的最大长度
     * @param docType WebM
     * @param docTypeVersion DocType解释器版本
     * @param docTypeReadVersion DocType解析器最低版本
     */
    fun createEBMLHeader(
        version: Int = EBML_VERSION,
        readVersion: Int = EBML_READ_VERSION,
        maxIdLength: Int = EBML_MAX_ID_LENGTH,
        maxSizeLength: Int = EBML_MAXSIZE_LENGTH,
        docType: String = EBML_DOCTYPE_WEBM,
        docTypeVersion: Int = EBML_DOCTYOE_VERSION,
        docTypeReadVersion: Int = EBML_DOCTYPE_READ_VERSION,
    ): EBMLElement {
        val ebmlVersion = EBMLElement(MatroskaTags.EBMLVersion, version.toByteArray())
        val ebmlEBMLReadVersion = EBMLElement(MatroskaTags.EBMLReadVersion, readVersion.toByteArray())
        val ebmlEBMLMaxIDLength = EBMLElement(MatroskaTags.EBMLMaxIDLength, maxIdLength.toByteArray())
        val ebmlEBMLMaxSizeLength = EBMLElement(MatroskaTags.EBMLMaxSizeLength, maxSizeLength.toByteArray())
        val ebmlDocType = EBMLElement(MatroskaTags.DocType, docType.toAscii())
        val ebmlDocTypeVersion = EBMLElement(MatroskaTags.DocTypeVersion, docTypeVersion.toByteArray())
        val ebmlDocTypeReadVersion = EBMLElement(MatroskaTags.DocTypeReadVersion, docTypeReadVersion.toByteArray())
        val ebmlValue = ebmlVersion.toElementBytes() + ebmlEBMLReadVersion.toElementBytes() + ebmlEBMLMaxIDLength.toElementBytes() + ebmlEBMLMaxSizeLength.toElementBytes() + ebmlDocType.toElementBytes() + ebmlDocTypeVersion.toElementBytes() + ebmlDocTypeReadVersion.toElementBytes()

        return EBMLElement(MatroskaTags.EBML, ebmlValue)
    }
}

/**
 * ID 的大小也是可变的，计算大小
 * ID 的长度可以从 ID的第一个字节获得
 */
internal fun Byte.calcIdSize() = this.toInt().let { int ->
    when {
        (int and 0x80) != 0 -> 1
        (int and 0x40) != 0 -> 2
        (int and 0x20) != 0 -> 3
        (int and 0x10) != 0 -> 4
        (int and 0x08) != 0 -> 5
        (int and 0x04) != 0 -> 6
        (int and 0x02) != 0 -> 7
        (int and 0x01) != 0 -> 8
        else -> -1// 多分無い
    }
}

/** 将数字编码为 V_INT */
internal fun Int.toVInt(): ByteArray {
    val valueByteArray = this.toByteArray()
    val valueSize = when (valueByteArray.size) {
        1 -> 0b1000_0000
        2 -> 0b0100_0000
        3 -> 0b0010_0000
        4 -> 0b0001_0000
        5 -> 0b0000_1000
        6 -> 0b0000_0100
        7 -> 0b0000_0010
        else -> 0b0000_0001
    }
    return valueByteArray.apply {
        this[0] = (valueSize or this[0].toInt()).toByte()
    }
}

/**
 * 从 Value 的 [ByteArray] 创建并返回所需的数据大小，即使表示值的数据大小是可变长度
 */
internal fun createDataSize(valueBytes: ByteArray): ByteArray {
    val size = valueBytes.size
    // 将 Int 转换为十六进制再转换为 ByteArray
    val dataSizeBytes = size.toByteArrayFat()
    val firstDataSizeByte = dataSizeBytes.first()
    // 数据大小本身是可变长度的，所以描述数据大小是多少字节
    // 它叫做 V_INT，通过查看从头开始的 1的数量就可以知道还剩下多少字节。
    // 1000 0000 -> 7 位 ( 1xxx xxxx )
    // 0100 0000 -> 14 位 ( 01xx xxxx xxxx xxxx )
    val dataSizeBytesSize = when (dataSizeBytes.size) {
        1 -> 0b1000_0000
        2 -> 0b0100_0000
        3 -> 0b0010_0000
        4 -> 0b0001_0000
        5 -> 0b0000_1000
        6 -> 0b0000_0100
        7 -> 0b0000_0010
        else -> 0b0000_0001
    }
    // 在字节大小的数据开头执行V_INT的OR操作。
    val dataSize = dataSizeBytes.apply {
        this[0] = (dataSizeBytesSize or firstDataSizeByte.toInt()).toByte()
    }
    return dataSize
}

// 将 Int 转换为十六进制，并使其成为 2字节的字节数组
internal fun Int.toClusterTimescale() = byteArrayOf((this shr 8).toByte(), this.toByte())

// 将 Int 转换为十六进制并使其成为 [ByteArray]。返回 4个字节
internal fun Int.toByteArrayFat() = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    this.toByte(),
)

/**
 * 将 Int 转换为十六进制并在 [ByteArray] 中返回。最多 4 个字节
 *
 */
internal fun Int.toByteArray(): ByteArray {
    return when {
        this < 0xFF -> byteArrayOf(this.toByte())
        this shr 8 == 0 -> byteArrayOf(
            (this shr 8).toByte(),
            this.toByte(),
        )
        this shr 16 == 0 -> byteArrayOf(
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
        else -> byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
    }
}

// 将字符串转换为 ASCII 字节数组
internal fun String.toAscii() = this.toByteArray(charset = Charsets.US_ASCII)

// 转换为十六进制
internal fun ByteArray.toHexString(separator: String = " ") = this.joinToString(separator = separator) { "%02x".format(it) }

// 转换为十六进制
internal fun Byte.toHexString() = "%02x".format(this)

// 将 Byte 转换为 Int 类型。字节必须是无符号值
internal fun Byte.toUnsignedInt() = this.toInt() and 0xFF

internal fun Int.toUnsignedInt() = this and 0xFF