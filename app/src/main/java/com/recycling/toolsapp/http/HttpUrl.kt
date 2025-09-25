package com.recycling.toolsapp.http

import android.content.Context
import android.net.wifi.WifiManager
import com.recycling.toolsapp.utils.Define
import com.serial.port.utils.AppUtils
import nearby.lib.netwrok.response.SPreUtil

object HttpUrl {

    /***
     * 工具柜编号
     */
    const val TOOLG_ID = "toolg_id"

    //系统设置页

    /***
     * 设置
     */
    const val SETTING_IP = "setting_ip"

    /***
     * 状态时间
     */
    const val POINT_TIME = "point_time"

    //上传工具柜状态

    /***
     * 状态
     */
    const val TOOLG_STATUS = "toolg_status"

    /***
     * 地址
     */
    const val TOOLG_SITE = "toolg_site"

    /***
     * 版本号吗
     */
    const val VERSION = "version"

    //获取用户工单
    /***
     * 用户ID
     */
    const val PERSION = "person_id"

    /***
     * 工单ID
     */
    const val WORK_ID = "work_id"

    /***
     * 状态
     */
    const val STSTUS = "status"

    //上传领取归还动作
    /***
     * 工具箱编码
     */
    const val TOOLX_ID = "toolx_id"

    /***
     * 工具箱编号01~11分别代表11个工具箱的编号
     */
    const val TOOLX_NUM = "toolx_num"

    /***
     * 工具箱仓位状态: ly领用触发，gh归还触发（在仓）
     */
    const val TOOLX_CW = "toolx_cw"

    /***
     * 工具箱电量
     */
    const val DIANLIANGH = "dianliang"

    /***
     * 工具箱网络状态
     */
    const val TOOLX_STATUS = "toolx_status"

    /***
     * 文件头像路径
     */
    const val N_FILES = "nfiles"

    /***
     * 版本更新
     */
    const val verupdateg = "verupdateg/index"

    /***
     * 用户信息
     */
    const val facedload = "faces/facedload"

    /***
     * 获取工单信息
     */
    const val gdlist3 = "lists/gdlist3"

    /***
     * 完成工单
     */
    const val gdfinish = "lists/gdfinish"

    /***
     * 上报工具箱状态信息
     */
    const val boxstatus = "boxstatus/boxgui"

    /***
     * 上报工具箱领取归还操作
     */
    const val boxiang = "boxstatus/boxiang"

    /***
     * 更新用户头像
     */
    const val faceUpdate = "faces/faceUpdate"

    /***
     * 添加网络用户
     */
    const val addFaceUser = "faces/faceUpdate"

    /***
     * 上传照片
     */
    const val uploadPhoto = "device/upload/photo"

    /***
     * 上传日志
     */
    const val uploadLog = "device/upload/log"

    /***
     * 获取mac地址
     */
    fun getMaxAddress(): String {
        val wifiManager =
                AppUtils.getContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val macAddress = wifiInfo.macAddress
        return macAddress
    }

    /***
     * 操作 领取 或者 归还 处理
     * @param boxCode 仓号
     * @param boxSn 箱子编码
     * @param boxCw 2.领用触发  3.归还触发
     */
    fun postBoxStatus(boxCode: Int, boxSn: String, electric: Int, boxCw: Int): MutableMap<String, Any> {
        return mutableMapOf<String, Any>().apply {
            val toolgID = SPreUtil[AppUtils.getContext(),TOOLG_ID, Define.DEFAULT_CABINET_NO] as String
            put(TOOLG_ID, toolgID)
            put(TOOLX_ID, boxSn)
            put(TOOLX_NUM, boxCode)
            put(TOOLX_CW, if (boxCw == 2) "ly" else "gh")
            put(DIANLIANGH, "${electric}%")
            put(TOOLX_STATUS, 1)
        }
    }
}