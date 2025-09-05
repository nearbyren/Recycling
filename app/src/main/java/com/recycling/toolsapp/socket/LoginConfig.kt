package com.recycling.toolsapp.socket

/***
 * 登录配置
 */
data class LoginConfig(
    var qrCode: String? = null,
    var debugPasswd: String? = null,
    var heartBeatInterval: String? = null,
    var turnOnLight: String? = null,
    var turnOffLight: String? = null,
    var lightTime: String? = null,
    var status: Int,
    var uploadPhotoURL: String? = null,
    var uploadLogURL: String? = null,
    var logLevel: Int ,
    var irDefaultState: Int,
    var weightSensorMode: Int,
    var list: List<CabinBox>? = null,
)