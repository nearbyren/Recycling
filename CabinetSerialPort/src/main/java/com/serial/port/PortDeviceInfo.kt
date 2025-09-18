package com.serial.port

data class PortDeviceInfo(
    /***
     * 烟雾警报
     */
    var smoke: Int? = null,
    /***
     *
     * capacity 是当前箱体的超重状态值
     * 0正常
     * 1是红外遮挡
     * 2是重量达到（initConfig-箱体配置：overflow）满溢
     * 3是红外遮挡并且重量达到（initConfig：irOverflow）
     */
    var capacity: Int? = null,
    /***
     * 红外状态
     */
    var irState: Int? = null,
    /***
     * 当前重量
     */
    var weigh: String? = null,
    /***
     * 投传感器状态
     */
    var touCGStatus: Int? = null,
    /***
     * 投递门
     */
    var doorStatus: Int? = null,
    /***
     * 清运门
     */
    var lockStatus: Int? = null,

    /***
     * 校准
     */
    var xzStatus: Int? = null,
    /***
     * 格口ID
     */
    var cabinId: String? = null,

    )
