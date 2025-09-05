package com.recycling.toolsapp.socket


/***
 * 状态
 */
data class StateInfo(
    //烟雾警报
    var smoke: Int? = null,
    //容量
    var capacity: Int? = null,
    //红外状态
    var irState: Int? = null,
    //当前重量
    var weigh: Int? = null,
    //门状态
    var doorStatus: Int? = null,
    //舱门编码
    var cabinId: String? = null,
    //gps
    var gps: GpsInfo? = null,
)
