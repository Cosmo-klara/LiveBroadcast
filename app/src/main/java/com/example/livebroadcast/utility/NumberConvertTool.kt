package com.example.livebroadcast.utility

/**
 * Byte - > MB等 单位转换
 *
 */
object NumberConvertTool {

    /**
     *
     * 仅兼容 bps / Kbps / Mbps
     *
     * @param bit 位
     * @return 附上单位返回
     */
    fun convert(bit: Int): String {
        return when (bit) {
            in 1L..1_000_000L -> "${bit shr 10} Kbps"
            in 1_000_000L..1_000_000_000L -> "${bit shr 20} Mbps"
            else -> "$bit Bps"
        }
    }
}