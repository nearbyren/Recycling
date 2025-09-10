package com.recycling.toolsapp.socket


/***
 * 关闭舱门
 */
data class DoorCloseDto(

    //指令
    var cmd: String? = null,
    //事务id
    var transId: String? = null,

    //服务下发
    //状态 0.成功 1.失败
    var retCode: Int,
    //舱门信息
    var info: DoorCloseInfo? = null,

    //终端上发
    var timestamp: String? = null,
    //重量减重后
    var afterDownWeight: Float? = null,
    //重量减重前
    var beforeDownWeight: Float? = null,
    //重量修改后
    var afterUpWeight: Float? = null,
    //重量修改前
    var beforeUpWeight: Float? = null,
    //刷新重量
    var refWeight: Float? = null,
    //改变重量
    var changeWeight: Float? = null,
    //当前重量
    var curWeight: Float? = null,
    //手机号
    var phoneNumber: Float? = null,
    //舱门编号
    var cabinId: String? = null,
) {
    constructor() : this(null, null, 0, null, null, null,
        null, null, null, null, null, null, null, null)
}
