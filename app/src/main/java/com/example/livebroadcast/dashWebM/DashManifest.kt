package com.example.livebroadcast.dashWebM

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 创建 MPEG-DASH 清单文件
 *
 * [MPD验证工具](https://conformance.dashif.org/)
 */
object DashManifestTool {

    /**
     * 指定视频数据的可用时间 ISO 8601
     */
    private val isoDateFormat =
        // ZZZZZ 是 Android 实现
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.CHINA)

    /**
     * 创建清单
     *
     * @param fileIntervalSec 视频文件生成间隔
     * @param whetherAudio 如果视频中包含音频则为 true
     * @param isVP8 如果使用 VP8 而不是 VP9，则为 true
     * @return XML
     */
    fun createManifest(
        fileIntervalSec: Int = 1,
        whetherAudio: Boolean = false,
        isVP8: Boolean = false,
    ): String {
        val publishTime = isoDateFormat.format(System.currentTimeMillis())
        // 内容可用的时间 (ISO-8601)
        val availabilityStartTime = isoDateFormat.format(System.currentTimeMillis())
        val videoCodec = if (isVP8) "vp8" else "vp9"
        // 如果指定如 minimumUpdatePeriod =“P60S”之类的内容，则清单文件将按照指定的时间间隔进行更新。
        return if (whetherAudio) {
            """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" publishTime="$publishTime" availabilityStartTime="$availabilityStartTime" maxSegmentDuration="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/">
              <BaseURL>/</BaseURL>
              <Period start="PT0S" id="live">
              
                <AdaptationSet mimeType="video/webm" contentType="video">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/video_init.webm" media="/video${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="video_track" codecs="$videoCodec"/>
                </AdaptationSet>
                
                <AdaptationSet mimeType="audio/webm" contentType="audio">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/audio_init.webm" media="/audio${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="audio_track" codecs="opus"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        } else {
            """
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" publishTime="$publishTime" availabilityStartTime="$availabilityStartTime" maxSegmentDuration="PT${fileIntervalSec}S" minBufferTime="PT${fileIntervalSec}S" type="dynamic" profiles="urn:mpeg:dash:profile:isoff-live:2011,http://dashif.org/guidelines/">
              <BaseURL>/</BaseURL>
              <Period start="PT0S" id="live">              
                <AdaptationSet mimeType="video/webm" contentType="video">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <SegmentTemplate duration="$fileIntervalSec" initialization="/video_init.webm" media="/video${"$"}Number${'$'}.webm" startNumber="0"/>
                  <Representation id="video_track" codecs="$videoCodec"/>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()
        }
    }

}