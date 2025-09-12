package com.recycling.toolsapp.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.recycling.toolsapp.StartUiActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.Random
import kotlin.system.exitProcess

object HexConverter {
    /**
     * 将字符串转换为字节数组（ByteArray）
     * @param input 原始字符串
     * @param charset 字符编码（默认UTF-8）
     * @return 字节数组
     */
    fun stringToBytes(input: String, charset: Charset = Charsets.UTF_8): ByteArray {
        return input.toByteArray(charset)
    }

    fun intToByteArray(num: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(num).array()
    }

    fun byteArrayToInt(bytes: ByteArray): Int {
        require(bytes.size == 4) { "Byte array must be 4 bytes long" }
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
    }

    /**
     * 获取重量
     * @param weight 下位机上报的重量
     */
    fun getWeight(weight: Int): String {
        if (weight < 0) return "0.1"
        return "%.2f".format(weight / 100.0)
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串（每两个字符表示一个字节）
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * 一步转换：字符串 → 字节数组 → 十六进制字符串
     * @param input 原始字符串
     * @param charset 字符编码（默认UTF-8）
     */
    fun stringToHexString(input: String, charset: Charset = Charsets.UTF_8): String {
        return bytesToHex(input.toByteArray(charset))
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        // 去除所有空格
        val cleanHex = hexString.replace(" ", "")
        // 检查长度是否为偶数
        require(cleanHex.length % 2 == 0) { "Hex string must have even number of characters" }

        return ByteArray(cleanHex.length / 2) {
            val byteStr = cleanHex.substring(it * 2, it * 2 + 2)
            byteStr.toInt(16).toByte()
        }
    }

    /**
     * @param arrays 合并所有的byteArray
     */
    fun combineByteArrays(vararg arrays: ByteArray): ByteArray {
        var totalLength = 0
        arrays.forEach { totalLength += it.size }

        val result = ByteArray(totalLength)
        var offset = 0
        arrays.forEach {
            System.arraycopy(it, 0, result, offset, it.size)
            offset += it.size
        }
        return result
    }

    fun restartApp2(context: Context, delay: Long, exit: Boolean = true) {
        Log.e("Restarting", "restartApp: 即将重启应用")
        val pendingIntent: PendingIntent
        val intent = Intent(context, StartUiActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntentId = Random().nextInt(1000) + 1
        pendingIntent =
                PendingIntent.getActivity(context, pendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr[AlarmManager.RTC, System.currentTimeMillis() + delay] = pendingIntent
        if (exit) {
            exitProcess(0)
        }
    }

    /**
     * 固件升级完成 重启app
     */
    fun restartApp(context: Context, delay: Long, exit: Boolean = true) {
        Log.i("Restarting", "Restarting app in ${delay}ms") // 改用 INFO 级别日志
        val intent = Intent(context, StartUiActivity::class.java).apply {
            // 清理任务栈
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 添加重启标识
            putExtra("RESTART_SOURCE", "app_recovery")
        }
        try {

            // 使用时间戳作为唯一ID
            val pendingIntentId = System.currentTimeMillis().toInt()
            val pendingIntent =
                    PendingIntent.getActivity(context, pendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val triggerTime = System.currentTimeMillis() + delay
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 版本兼容的闹钟设置
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }

                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }

            if (exit) {
                // 优雅退出
                cleanUpBeforeExit(context)
            }
        } catch (e: SecurityException) {
            Log.i("Restarting", "AlarmManager permission denied, using fallback", e)
            // 备选方案：使用 Handler
            Handler(Looper.getMainLooper()).postDelayed({
                context.startActivity(intent)
                if (exit) cleanUpBeforeExit(context)
            }, delay)
        }
    }

    private fun cleanUpBeforeExit(context: Context) {
        // 关闭所有 Activity
        (context as? Activity)?.finishAffinity()

        // 停止相关服务（示例）
        // 释放其他资源...

        // 最后退出进程
        exitProcess(0)
    }
}