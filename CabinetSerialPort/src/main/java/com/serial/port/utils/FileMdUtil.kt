package com.serial.port.utils

import android.content.Context
import android.os.Environment
import java.io.File

object FileMdUtil {

    fun writeToMdFile(context: Context, content: String) {
        // 获取 log 文件路径：data/data/<包名>/log/log.md
        val logDir = File(context.filesDir, "log")
        if (!logDir.exists()) {
            Loge.d("授权成功 首次创建目录...")
            logDir.mkdirs() // 创建 log 目录
        }
        val logFile = File(logDir, "log.md")
        // 确保文件存在，如果不存在则创建
        if (!logFile.exists()) {
            Loge.d("授权成功 首次创建文件...")
            logFile.createNewFile()
        }

        // 写入内容并换行两次
        logFile.appendText("$content\n\n")
    }

    fun matchNewFile(path: String): File {
        return File("${AppUtils.getContext().filesDir}/${path}")
    }

    fun matchNewFileName(path: String,fileName:String): String {
        return File("${AppUtils.getContext().filesDir}/${path}/${fileName}").absolutePath
    }

    /**
     * 检测固件文件是否存在
     */
    fun checkBinFileExists(fileName: String): Boolean {
        val downloadDir = AppUtils.getContext()
            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.let { dir ->
            File(dir, fileName).takeIf { it.exists() && it.isFile } != null
        } ?: false
    }
    /**
     * 重命名指定路径下的文件
     * @param dirPath 目录路径（如：context.filesDir）
     * @param oldFileName 原文件名
     * @param newFileName 新文件名
     * @return 是否重命名成功
     */
    fun renameFileInDir(dirPath: File, oldFileName: String, newFileName: String): Boolean {
        val oldFile = File(dirPath, oldFileName).takeIf { it.exists() } ?: return false

        val newFile = File(dirPath, newFileName)
        if (newFile.exists()) {
            newFile.delete()
        }

        return try {
            oldFile.renameTo(newFile)
        } catch (e: SecurityException) {
            false
        }
    }
}