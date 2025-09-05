package com.recycling.toolsapp.utils

object Define {

    /***
     * 0.主界面
     */
    val ACTIVITY_TYPE_MAIN = 0

    /***
     * 1.领取 归还
     */
    val ACTIVITY_TYPE_OPEN = 1

    /***
     * 领取
     */
    val ACTIVITY_TYPE_OPEN_L = 11

    /***
     *  归还
     */
    val ACTIVITY_TYPE_OPEN_H = 10

    /***
     * 2.查看
     */
    val ACTIVITY_TYPE_LOOK = 2

    /***
     * 3.紧急开锁
     */
    val ACTIVITY_TYPE_OPEN_MANAGER = 3

    /***
     * 4.系统管理
     */
    val ACTIVITY_TYPE_SYSTEM = 4

    /***
     *当前主芯片处理
     */
    val SYSTEM_MASTER_CYCLE_TYPE = 41

    /***
     * 当前从芯片处理
     */
    val SYSTEM_FROM_CYCLE_TYPE = 42

    /***
     * 主从芯片升级
     */
    val SYSTEM_UPGRADE_CYCLE_TYPE = 412

    /***
     * 5.工单管理
     */
    val ACTIVITY_TYPE_ORDER = 5

    /***
     * 6.人员管理
     */
    val ACTIVITY_TYPE_PERSION = 6

    /***
     * 7.箱子操作记录
     */
    val ACTIVITY_TYPE_RECORD = 7

    /***
     * 8.故障管理
     */
    val ACTIVITY_TYPE_FAULT = 8

    /***
     * 操作类型 11.领取操作完成 10.归还操作完成
     * 操作类型 1.领用 0.归还 仓流程【当仓门开领取流程进行中则101 当仓门开归还流程进行中则111 当仓门关闭后 是空闲则 10  仓门关闭后 是正在充电则 11】
     *
     *
     */
    /***
     * 归还操作完成
     */
    val OPT_STATUS_11 = 11

    /***
     * 领取操作完成
     */
    val OPT_STATUS_10 = 10

    /***
     * 领取
     */
    val OPT_STATUS_1 = 1

    /***
     * 归还
     */
    val OPT_STATUS_0 = 0

    /***
     * 归还中
     */
    val OPT_STATUS_111 = 111

    /***
     * 领取中
     */
    val OPT_STATUS_101 = 101

    /***
     * 权限类型
     */
    val PERMISSION_TYPE = "permissionType"

    /***
     * 用户名称
     */
    val USER_NAME = "userName"

    /***
     * 用户id
     */
    val USER_ID = "userId"

    /***
     * 账号
     */
    val ACCOUNT = "account"

    /***
     * 密码
     */
    val USER_PASSWORD = "userPassword"

    /***
     * 工号
     */
    val USER_NO = "userNo"

    /***
     * 头像
     */
    val USER_AVATAR = "userAvatar"

    /***
     * 点仓时间
     */
    val SETUP_TIME = "setupTime"

    /***
     * 人脸识别头像
     */
    val VERIFY_AVATAR = "verifyAvatar"

    /***
     * 超时间时间
     */
    val TIME_OUT = "timeOut"

    /***
     * 默认柜体编号
     */
    var DEFAULT_CABINET_NO = "GJG00004"

    /**
     * 请取出工具箱
     */
    var VOICE_QU_CHU = "box_qc"

    /**
     * 请放入工具箱
     */
    var VOICE_FANG_RU = "box_fr"

    /**
     * 请关闭箱门
     */
    var VOICE_GIA_NBI_BOX = "box_frgh_close"

    /**
     * 请领取箱子
     */
    var VOICE_LING_QU = "zk_lingqu"

    /**
     * 请将舱门关闭。
     */
    var VOICE_GUAN_BI = "qing_guanbi"

}