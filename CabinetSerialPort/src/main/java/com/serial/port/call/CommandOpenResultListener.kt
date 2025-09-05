package com.serial.port.call

/**
 * 开仓指令发送响应
 */
fun interface CommandOpenResultListener {
    /***
     * @param number 仓位
     * @param status 状态
     */
    fun openResult(number: Int, status: Int)

}