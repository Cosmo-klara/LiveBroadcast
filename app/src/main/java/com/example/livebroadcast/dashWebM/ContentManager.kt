package com.example.livebroadcast.dashWebM

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MPEG-DASH 的文件管理类
 *
 * - [parentFolder]
 *  - public
 *      - 这是生成的视频放置的文件夹，视频返回给客户端
 *  - temp
 *      - 需要临时保存的文件
 *
 * @param parentFolder
 * @param audioPrefixName 要添加到音频文件前面的字符串
 * @param videoPrefixName 要添加到视频文件开头的字符串
 */
class ContentManager(
    private val parentFolder: File,
    private val audioPrefixName: String,
    private val videoPrefixName: String,
) {
    private var audioCount = 0
    private var videoCount = 0

    private val audioFileList = arrayListOf<File>()
    private val videoFileList = arrayListOf<File>()

    private val outputFolder = getOutputFolder(parentFolder).apply { mkdir() }

    /**
     * 生成按顺序编号的连续音频文件
     * 写入文件时，.temp 自动附加
     * Ktor 抛出了异常，可能是因为文件太大（我不确定是否是在写入过程中），所以我在写入时将文件名更改为临时文件名。
     *
     * @param fileIO 完成后，文件名将更改为临时文件名
     * @return
     */
    suspend fun createIncrementAudioFile(fileIO: suspend (File) -> Unit) =
        withContext(Dispatchers.IO) {
            val index = audioCount++
            val resultFile = File(outputFolder, "$audioPrefixName${index}.$WEBM_EXTENSION")
            val tempFile =
                File(outputFolder, "$audioPrefixName${index}.$WEBM_EXTENSION.$FILE_WRITING_SUFFIX")
            fileIO(tempFile)
            tempFile.renameTo(resultFile)
            // 删除旧数据
            audioFileList.add(resultFile)
            removeUnUseFile(audioFileList)
            return@withContext resultFile
        }

    /**
     * 生成按顺序编号的视频文件
     * 这是 [createIncrementAudioFile] 的视频文件版本
     *
     * @param fileIO
     * @return
     */
    suspend fun createIncrementVideoFile(fileIO: suspend (File) -> Unit) =
        withContext(Dispatchers.IO) {
            val index = videoCount++
            val resultFile = File(outputFolder, "$videoPrefixName${index}.$WEBM_EXTENSION")
            val tempFile =
                File(outputFolder, "$videoPrefixName${index}.$WEBM_EXTENSION.$FILE_WRITING_SUFFIX")
            fileIO(tempFile)
            tempFile.renameTo(resultFile)
            videoFileList.add(resultFile)
            removeUnUseFile(videoFileList)
            return@withContext resultFile
        }

    /**
     * 生成文件的函数
     * 与 [createIncrementAudioFile] 和 [createIncrementVideoFile] 相同，只是文件名可以更改。
     *
     * @param fileName
     * @return [File]
     */
    suspend fun createFile(fileName: String) = withContext(Dispatchers.IO) {
        File(outputFolder, fileName).apply {
            createNewFile()
        }
    }

    suspend fun deleteGenerateFile() = withContext(Dispatchers.IO) {
        outputFolder.listFiles()?.forEach { it.delete() }
    }

    /**
     * 删除不再使用的媒体片段，保留 [FILE_HOLD_COUNT] 个新媒体片段并删除其余的
     *
     * @param fileList
     */
    private suspend fun removeUnUseFile(fileList: ArrayList<File>) = withContext(Dispatchers.IO) {
        val deleteItemSize = fileList.size - FILE_HOLD_COUNT
        if (deleteItemSize >= 0) {
            fileList.take(deleteItemSize).forEach { it.delete() }
        }
    }

    companion object {
        private const val WEBM_EXTENSION = "webm"

        /** 包含成品视频的文件夹名称 */
        private const val OUTPUT_VIDEO_FOLDER_NAME = "dist"

        /** 添加到正在写入的文件末尾（temp 后缀） */
        private const val FILE_WRITING_SUFFIX = "temp"

        /** 如果保存的分段文件数量太少，MPEG-DASH 无法从中间播放 */
        private const val FILE_HOLD_COUNT = 10

        /**
         * 返回生成的视频音频的存储位置
         *
         * @param parentFolder [File]
         * @return 保存位置 [File]
         */
        fun getOutputFolder(parentFolder: File): File =
            parentFolder.resolve(OUTPUT_VIDEO_FOLDER_NAME)
    }

}