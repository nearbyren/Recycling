package com.recycling.toolsapp.utils

import android.content.Context
import android.media.MediaPlayer
import com.serial.port.utils.Loge

object MediaPlayerHelper {

    private var mediaPlayer: MediaPlayer? = null

    // 锁对象，用于同步操作
    private val lock = Any()

    /**
     * 播放指定音频文件
     * @param context 上下文，用于访问 Assets
     * @param fileName 文件名，相对于 `assets/media/` 路径
     */
    fun playAudio(context: Context, fileName: String) {
        synchronized(lock) {
            if(isPlaying()){
//               Loge.d("有语音信息来了 语音播报我来了...播报进行中")
                return@synchronized
            }
//            Loge.d("有语音信息来了 语音播报我来了...播报开始")

            releaseMediaPlayer() // 确保释放之前的资源

            try {
                val assetFileDescriptor = context.assets.openFd("media/$fileName.mp3")
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                    setOnCompletionListener {
//                        Loge.d("有语音信息来了 语音播报我来了...播报完毕")
                        releaseMediaPlayer() // 确保释放之前的资源
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                releaseMediaPlayer() // 确保释放之前的资源
                Loge.d("有语音信息来了 播放音频出错：${e.message}")
            }
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    fun releaseMediaPlayer() {
        synchronized(lock) {
            mediaPlayer?.let {
                it.release()
                mediaPlayer = null
            }
        }
    }

    /**
     * 停止播放音频
     */
    fun stopAudio() {
        synchronized(lock) {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
            releaseMediaPlayer()
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        synchronized(lock) {
            return mediaPlayer?.isPlaying == true
        }
    }
}

