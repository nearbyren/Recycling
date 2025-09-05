package com.serial.port.call

import com.serial.port.BoxInternal

/**
 * 永胜德仓查询指令发送响应
 */
fun interface CommandQueryListYSDResultListener {

    /***
     * @param commandType  指令
     * @param result 返回 箱子电量  工具电量 箱子在仓状态 箱子sn码
     */
    fun queryResult(commandType: Int, result: MutableMap<Int, MutableMap<Int, BoxInternal>>)
}