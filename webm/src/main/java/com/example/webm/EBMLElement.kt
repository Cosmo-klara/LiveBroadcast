package com.example.webm

/**
 * EBML 元素数据类
 *
 * @param tagId [MatroskaTags]
 * @param value 数据
 * @param dataSize 数据大小
 */
data class EBMLElement(
    val tagId: MatroskaTags,
    val value: ByteArray,
    val dataSize: ByteArray = createDataSize(value),
) {
    // 返回连接 ID DataSize Value 的字节数组
    fun toElementBytes() = (tagId.id + dataSize + value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EBMLElement

        if (tagId != other.tagId) return false
        if (!value.contentEquals(other.value)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tagId.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}