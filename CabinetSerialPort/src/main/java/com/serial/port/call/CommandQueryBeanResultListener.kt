package com.serial.port.call

import com.serial.port.PortDeviceInfo

/**
 * 单仓查询指令发送响应
 */
fun interface CommandQueryBeanResultListener {

    /***
     * @param lockerInfos 仓位信息
     */
    fun queryResult(lockerInfos: PortDeviceInfo)
}