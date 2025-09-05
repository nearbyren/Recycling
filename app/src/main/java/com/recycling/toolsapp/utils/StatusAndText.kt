package com.recycling.toolsapp.utils

import com.serial.port.EnumBoxType

object StatusAndText {

    /***
     * 先判断故障 在处理 在柜 空闲
     */
    fun findStatusText(status: String, serial: String): String {
        return if (status != EnumBoxType.getDescByCode(101)) {
            if (isSnNull(serial)) {
                EnumBoxType.getDescByCode(10)
            } else {
                EnumBoxType.getDescByCode(11)
            }
        } else {
            EnumBoxType.getDescByCode(101)
        }
    }

    /***
     * 查询箱子状态
     * @param status 0.门关闭 1.门开启 2.门通信异常
     * @param serial 序列号
     */
    fun getStatusSerial(status: String, serial: String): MutableList<String> {
        val result = mutableListOf<String>()
        var statusValue = ""
        statusValue = if (isSnNull(serial)) {
            EnumBoxType.getDescByCode(10)
        } else if (status == EnumBoxType.getDescByCode(101)) {
            EnumBoxType.getDescByCode(101)
        } else {
            EnumBoxType.getDescByCode(11)
        }
        result.add(statusValue)
        result.add(status)
        result.add(serial)
        return result
    }

    fun getBoxCodeElect(index: Int, electric: Int): String {
        val sb = StringBuffer()
        sb.append("$index 仓 ")
        if (index < 12) {
            sb.append("| 电量:$electric")
        }
        return sb.toString()
    }

    /***
     * 处理sn是否为null
     */
    fun isSnNull(boxSn: String?): Boolean {
        if (boxSn.isNullOrBlank()) return true
        if (boxSn == "00000000"||boxSn == "BXQ00000") return true
        return false
    }
}