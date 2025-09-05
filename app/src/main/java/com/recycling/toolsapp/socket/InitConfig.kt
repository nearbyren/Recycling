package com.recycling.toolsapp.socket

/***
 * Init 基础信息
 */
data class InitConfig(
    var heartBeatInterval: String? = null,
    var turnOnLight: String? = null,
    var turnOffLight: String? = null,
    var lightTime: String? = null,
    var uploadPhotoURL: String? = null,
    var uploadLogURL: String? = null,
    var qrCode: String? = null,
    var logLevel: Int? = null,
    var list: List<CabinBox>? = null,
    var resourceList: List<InitResource>? = null,
    var status: Int? = null,
    var debugPasswd: String? = null,
    var irDefaultState: Int? = null,
    var weightSensorMode: Int? = null,

)