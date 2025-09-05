package com.serial.port.camera

import com.serial.port.utils.ByteUtils
import com.serial.port.utils.CRC16
import com.serial.port.utils.CmdData
import com.serial.port.utils.Loge


class CameraPortCore {

    private val cmdMap: MutableMap<Int, ByteArray> = mutableMapOf(1 to CmdData.cmd_open_1, 2 to CmdData.cmd_open_2, 3 to CmdData.cmd_open_3, 4 to CmdData.cmd_open_4, 5 to CmdData.cmd_open_5, 6 to CmdData.cmd_open_6, 7 to CmdData.cmd_open_7, 8 to CmdData.cmd_open_8, 9 to CmdData.cmd_open_9, 10 to CmdData.cmd_open_10, 11 to CmdData.cmd_open_11, 12 to CmdData.cmd_open_12, 13 to CmdData.cmd_open_13, 14 to CmdData.cmd_open_14, 15 to CmdData.cmd_open_15, 16 to CmdData.cmd_open_16, 17 to CmdData.cmd_open_17, 18 to CmdData.cmd_open_18, 19 to CmdData.cmd_open_19)
    private val cmdCloseMap: MutableMap<Int, ByteArray> = mutableMapOf(1 to CmdData.cmd_close_1, 2 to CmdData.cmd_close_2, 3 to CmdData.cmd_close_3, 4 to CmdData.cmd_close_4, 5 to CmdData.cmd_close_5, 6 to CmdData.cmd_close_6, 7 to CmdData.cmd_close_7, 8 to CmdData.cmd_close_8, 9 to CmdData.cmd_close_9, 10 to CmdData.cmd_close_10, 11 to CmdData.cmd_close_11, 12 to CmdData.cmd_close_12)


    val openBytelist24 = MutableList<Byte>(24) { 0 }//24个字节
    var opTimeList = MutableList<Int>(12) { 0 }

    val closeBytelist24 = MutableList<Byte>(24) { 0 }//24个字节
    var closeTimeList = MutableList<Int>(12) { 0 }

    val othBytelist8 = MutableList<Byte>(10) { 0 }//十个字节
    var othTimeList = MutableList<Int>(6) { 0 }


    /***
     * 查询仓状态
     */
    fun test() {
//        CameraPortManager.instance.camerasVM
        Loge.d("发 CameraPortCore")
        val cmd_status_arr = byteArrayOf(0xEF.toByte(), 0xAA.toByte(), 0x21, 0x00, 0x00, 0x21)
        CameraPortManager.instance.test(cmd_status_arr)
    }

    private fun computeCRC16(bytes: ByteArray): ByteArray {
        val crc16 = CRC16.calcCrc16(bytes)
        val crcByte = ByteUtils.fromInt16(crc16)
        if (crcByte.size < 2) {
            return bytes
        }
        val index = bytes.lastIndex
        val tempArray = bytes.copyOf(bytes.size + 2)
        tempArray[index + 1] = crcByte[0]
        tempArray[index + 2] = crcByte[1]
        return tempArray
    }

}