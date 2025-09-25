package com.serial.port.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    fun matchNewFileName(path: String, fileName: String): String {
        return File("${AppUtils.getContext().filesDir}/${path}/${fileName}").absolutePath
    }

    fun matchDownloadsName(path: String, fileName: String): String {
        return File("${AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/${path}/${fileName}").absolutePath
    }

    /**
     * 检测固件文件是否存在
     */
    fun checkBinFileExists(fileName: String): Boolean {
        val downloadDir = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return downloadDir?.let { dir ->
            File(dir, fileName).takeIf { it.exists() && it.isFile } != null
        } ?: false
    }

    /**
     * 检测音频文件是否存在
     */
    fun checkAudioFileExists(fileName: String): Boolean {
        val dataFiles = FileMdUtil.matchNewFile("audio")
        return dataFiles?.let { dir ->
            File(dir, fileName).takeIf { it.exists() && it.isFile } != null
        } ?: false
    }

    /**
     * 检测资源文件是否存在
     */
    fun checkResFileExists(fileName: String): Boolean {
        val dataFiles = FileMdUtil.matchNewFile("res")
        return dataFiles?.let { dir ->
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

    /**
     * 保存 音频 图片资源
     * @param bitmap
     * @param fileName
     */
    fun saveBitmapToInternalStorage(bitmap: Bitmap, fileName: String): String? {
        // 指定存储目录：/data/data/包名/files/userAvatar/
//        val dir = File(AppUtils.getContext().filesDir, "faceVerify")
        val dir = FileMdUtil.matchNewFile("res")
        if (!dir.exists()) {
            dir.mkdirs() // 创建目录
        }

        // 创建保存文件
        val file = File(dir, fileName)
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)
            // 将 Bitmap 压缩为 PNG 格式保存
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            return file.absolutePath // 返回保存路径
        } catch (e: IOException) {
            e.printStackTrace()

        } finally {
            fos?.close() // 关闭输出流
        }
        return null
    }

    /***
     * 检测音频文件
     */
    fun shouldAudio(fileName: String): Boolean {
        return fileName.contains("wav", ignoreCase = true) || fileName.contains("mp3", ignoreCase = true) || fileName.contains("mp4", ignoreCase = true)
    }

    /***
     * 检测png文件
     */
    fun shouldPGJ(fileName: String): Boolean {
        return fileName.contains("png", ignoreCase = true) || fileName.contains("gif", ignoreCase = true) || fileName.contains("jpg", ignoreCase = true)
    }
}