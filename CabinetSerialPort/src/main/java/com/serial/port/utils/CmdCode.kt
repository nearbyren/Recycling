package com.serial.port.utils

object CmdCode {

    /***
     * 查询重量前
     */
    const val GE_WEIGHT_FRONT = 0

    /***
     * 查询重量后
     */
    const val GE_WEIGHT_BACK = 1

    /***
     * 开
     */
    const val GE_OPEN = 1

    /***
     * 关
     */
    const val GE_CLOSE = 0

    /***
     * 开关中
     */
    const val GE_OPEN_CLOSE_ING = 2

    /***
     * 开关门故障
     */
    const val GE_OPEN_CLOSE_FAULT = 3

    /***
     * 设备状态
     */
    const val GE_DEVICE_STATUS = 1

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

    /***
     *
     */
    const val IN_LIGHTS_OPEN = 1
    const val IN_LIGHTS_CLOSE = 2

    const val OUT_LIGHTS_OPEN = 1
    const val OUT_LIGHTS_CLOSE = 2
}