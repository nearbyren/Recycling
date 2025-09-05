package com.recycling.toolsapp.socket


/***
 * 登录 初始化配置
 */
data class OpenDoorDto(
    var cmd: String? = null,
    var retCode: Int? = null,
    var sn: String? = null,
    var config: LoginConfig,
)
