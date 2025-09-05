package com.serial.port.utils

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import com.serial.port.BoxInternal
import com.serial.port.PortDeviceInfo
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/***
 * 记录下位机箱子信息
 * Record lower machine box information
 */
object BoxToolLogUtils {
    @SuppressLint("SimpleDateFormat")
    private val formatdate = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

    /****
     * 工具箱集合信息
     * @param lowerMachines
     */
    fun recordLowerBox(lowerMachines: List<PortDeviceInfo>) {
        val builder = StringBuilder()
        val time = AppUtils.getDateYMDHMS()
        for (box in lowerMachines) {
            val boxCode = String.format(Locale.CHINA, "%02d", box.boxCode)
            builder
                .append(time).append(" | ")
                .append(boxCode).append(" | ")
                .append(box.boxDoorStatus).append(" | ")
                .append(box.boxSn).append(" | ")
                .append(box.boxElectric)
                .append('\n')

        }
        builder.append("---------------------------------------------------")
            .append('\n')
        try {
            val fileName = "box--${AppUtils.getDateYMD()}.txt"
            val path = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/box_info/"
            val dirs = File(path)
            if (!dirs.exists()) {
                dirs.mkdirs()
            }
            val file = File(path, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            // 追加写入模式
            val fos = FileOutputStream(file, true)
            val bos = BufferedOutputStream(fos)
            bos.write(builder.toString().toByteArray())
            bos.flush()
            bos.close()
        } catch (e: SecurityException) {
            Loge.d("BoxToolLogUtils recordLowerBox an error occured while writing file...$e")
        }
    }

    /****
     * 工具箱工具信息
     * @param boxInternals
     *
     */
    fun recordLowerBoxTool(boxInternals: MutableMap<Int, BoxInternal>) {
        val builder = StringBuilder()
        val time = AppUtils.getDateYMDHMS()
        boxInternals.entries.forEach { entry1 ->
            val key = entry1.key
            val data = entry1.value
            val boxCode = String.format(Locale.CHINA, "%02d", data.boxCode)
            val address = String.format(Locale.CHINA, "%02d", key)
            builder
                .append(time).append(" | ")
                .append(boxCode).append(" | ")
                .append(address).append(" | ")
                .append(data.boxSignal).append(" | ")
                .append(data.boxIn).append(" | ")
                .append(data.boxElectric)
                .append('\n')

        }
        builder.append("---------------------------------------------------")
            .append('\n')
        try {
            val fileName = "tool--${AppUtils.getDateYMD()}.txt"
            val path = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/box_info/"
            val dirs = File(path)
            if (!dirs.exists()) {
                dirs.mkdirs()
            }
            val file = File(path, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            // 追加写入模式
            val fos = FileOutputStream(file, true)
            val bos = BufferedOutputStream(fos)
            bos.write(builder.toString().toByteArray())
            bos.flush()
            bos.close()
        } catch (e: SecurityException) {
            Loge.d("BoxToolLogUtils recordLowerBoxTool an error occured while writing file...$e")
        }
    }

    /***
     * 查询工具箱和工具上报日志
     */
    fun listBoxInfoFiles(): List<String>? {
        return try {
            val dir =
                    File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "box_info")
            if (dir.exists() && dir.isDirectory) {
                dir.list()?.filter { File(dir, it).isFile }
            } else {
                null
            }
        } catch (e: SecurityException) {
            Log.e("FileUtils", "Permission denied", e)
            null
        }
    }

    /***
     * @param typePort 232 或者 485
     * 接收下位机数据
     */
    fun receiveOriginalLower(typePort: Int, packet: ByteArray) {
        val builder = StringBuilder()
        val time = AppUtils.getDateYMDHMS()
        builder
            .append(time).append(" | ")
            .append(typePort).append(" | ")
            .append(ByteUtils.toHexString(packet))
            .append('\n')
            .append("----------------------------------------------------------------------------------------------------------------")
            .append('\n')

        try {
            val fileName = "receive-lower-${typePort}--${AppUtils.getDateYMD()}.txt"
            val path = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/box_info/"
            val dirs = File(path)
            if (!dirs.exists()) {
                dirs.mkdirs()
            }
            val file = File(path, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            // 追加写入模式
            val fos = FileOutputStream(file, true)
            val bos = BufferedOutputStream(fos)
            bos.write(builder.toString().toByteArray())
            bos.flush()
            bos.close()
        } catch (e: SecurityException) {
            Loge.d("BoxToolLogUtils originalLower an error occured while writing file...$e")
        }
    }


    /***
     * @param typePort 232 或者 485
     * 发送给下位机数据
     */
    fun sendOriginalLower(typePort: Int, packet: String) {
        val builder = StringBuilder()
        val time = AppUtils.getDateYMDHMS()
        builder
            .append(time).append(" | ")
            .append(typePort).append(" | ")
            .append(packet)
            .append('\n')
            .append("----------------------------------------------------------------------------------------------------------------")
            .append('\n')

        try {
            val fileName = "send-lower-${typePort}--${AppUtils.getDateYMD()}.txt"
            val path = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/box_info/"
            val dirs = File(path)
            if (!dirs.exists()) {
                dirs.mkdirs()
            }
            val file = File(path, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            // 追加写入模式
            val fos = FileOutputStream(file, true)
            val bos = BufferedOutputStream(fos)
            bos.write(builder.toString().toByteArray())
            bos.flush()
            bos.close()
        } catch (e: SecurityException) {
            Loge.d("BoxToolLogUtils originalLower an error occured while writing file...$e")
        }
    }

    /***
     * @param typePort 232 或者 485
     * 发送给下位机数据 定时发送的
     */
    fun sendOriginalLowerStatus(typePort: Int, packet: String) {
        val builder = StringBuilder()
        val time = AppUtils.getDateYMDHMS()
        builder
            .append(time).append(" | ")
            .append(typePort).append(" | ")
            .append(packet)
            .append('\n')
            .append("----------------------------------------------------------------------------------------------------------------")
            .append('\n')

        try {
            val fileName = "send-lower-status-${typePort}--${AppUtils.getDateYMD()}.txt"
            val path = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/box_info/"
            val dirs = File(path)
            if (!dirs.exists()) {
                dirs.mkdirs()
            }
            val file = File(path, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            // 追加写入模式
            val fos = FileOutputStream(file, true)
            val bos = BufferedOutputStream(fos)
            bos.write(builder.toString().toByteArray())
            bos.flush()
            bos.close()
        } catch (e: SecurityException) {
            Loge.d("BoxToolLogUtils originalLower an error occured while writing file...$e")
        }
    }
}