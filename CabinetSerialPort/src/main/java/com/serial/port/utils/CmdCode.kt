package com.serial.port.utils

object CmdCode {
    /***
     * 开
     */
    const val GE_OPEN = 1

    /***
     * 关
     */
    const val GE_CLOSE = 0

    /***
     * 格口一
     */
    const val GE1 = 1

    /***
     * 格口二
     */
    const val GE2 = 2

    /***
     * 默认
     */
    const val GE = -1

    /***
     * 启动关格口一
     */
    const val GE10 = 10

    /***
     * 启动开格口一
     */
    const val GE11 = 11

    /***
     * 启动关格口二
     */
    const val GE20 = 20

    /***
     * 启动开格口二
     */
    const val GE21 = 21
}