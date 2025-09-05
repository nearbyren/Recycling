package com.serial.port.call

import com.serial.port.BoxInternal

/**
 * 所有内部工具箱信息集合查询指令发送响应
 */
fun interface CommandQueryInternalListResultListener {

    /***
     * @param lockerInfos 内部工具箱信息集合
     */
    fun queryResult(lockerInfos: MutableMap<Int,BoxInternal>)
}