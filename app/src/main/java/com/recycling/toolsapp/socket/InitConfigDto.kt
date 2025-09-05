package com.recycling.toolsapp.socket

/***
 * Init 初始化配置
 */
data class InitConfigDto(
    var cmd: String? = null,
    var sn: String? = null,
    var config: InitConfig,
)
