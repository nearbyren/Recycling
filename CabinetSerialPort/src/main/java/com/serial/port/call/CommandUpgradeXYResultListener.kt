package com.serial.port.call

/**
 * 效验发送文件是否准确
 */
fun interface CommandUpgradeXYResultListener {
    /***
     * @param status 状态
     */
    fun upgradeResult(bytes: ByteArray)

}