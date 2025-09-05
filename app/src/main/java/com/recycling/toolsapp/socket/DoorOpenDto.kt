package com.recycling.toolsapp.socket


/***
 * 打开舱门
 */
data class DoorOpenDto(

    //指令
    var cmd: String? = null,
    //事务id
    var transId: String? = null,
    //舱门编码
    var cabinId: String? = null,

    //服务下发
    var openType: Int,

    //终端上发
    //状态 0.成功 1.失败
    var retCode: Int,
    //手机号
    var phoneNumber: String? = null,
    //当前重量
    var curWeight: Float? = null,
    //时间戳
    var timestamp: String? = null,
) {
    constructor() : this(null, null, null, 0, 0, null, 0.0f, null)
}
