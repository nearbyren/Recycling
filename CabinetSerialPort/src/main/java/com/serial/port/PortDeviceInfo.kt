package com.serial.port

data class PortDeviceInfo(
    /***
     * 箱子类型：xx小箱子 zx中箱子 dx大箱子 bx剥线器 zwx杂物箱
     */
    var boxType: String? = null,
    /***
     * 根据仓号去知道 小中大箱子
     */
    var boxCode: Int = 0,
    /***
     * whElectric 电量
     */
    var boxElectric: Int = 0,
    /***
     *     0 -> boxStatus = "门关闭"
     *     1 -> boxStatus = "门开启"
     *     2 -> boxStatus = "门故障"
     */
    var boxDoorStatus: String? = null,
    /***
     * 设备序列号
     * sn 非0  20个字节 在柜 否则空闲
     */
    var boxSn: String? = null)
