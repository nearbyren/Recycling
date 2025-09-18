package com.serial.port.call

/**
 * 校准指令发送响应
 */
fun interface CommandCalibrationResultListener {
    /***
     * @param number
     * @param status
     */
    fun lightsResult(number: Int, status: Int)

}