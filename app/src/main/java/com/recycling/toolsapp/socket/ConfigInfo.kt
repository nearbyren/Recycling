package com.recycling.toolsapp.socket

/***
 * 配置信息
 */
data class ConfigInfo(
    //二维码地址
    var qrCode: String? = null,
    //调试密码
    var debugPasswd: String? = null,
    //心跳秒
    var heartBeatInterval: String? = null,
    //打开灯
    var turnOnLight: String? = null,
    //关闭灯
    var turnOffLight: String? = null,
    //光照时间
    var lightTime: String? = null,
    //箱子状态
    var status: Int,
    //图片上传路径
    var uploadPhotoURL: String? = null,
    //日志上传路径
    var uploadLogURL: String? = null,
    var logLevel: Int ,
    //红外默认状态
    var irDefaultState: Int,
    //重量传感器模式
    var weightSensorMode: Int,
    //格口信息
    var list: List<ConfigLattice>? = null,
    //图片音频资源
    var resourceList: List<ConfigRes>? = null,
)