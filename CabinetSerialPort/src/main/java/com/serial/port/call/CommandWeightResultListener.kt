package com.serial.port.call

/**
 *
 */
fun interface CommandWeightResultListener {
    /***
     * @param number 仓位
     * @param status 状态
     */
    fun weightResult(weight: Int)

}