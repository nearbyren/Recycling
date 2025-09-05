package com.serial.port.call

import com.serial.port.PortDeviceInfo

/**
 * 所有仓查询指令发送响应
 */
fun interface CommandQueryListResultListener {

    /***
     * @param lockerInfos 仓位集合信息
     */
    fun queryResult(lockerInfos: MutableList<PortDeviceInfo>)
}