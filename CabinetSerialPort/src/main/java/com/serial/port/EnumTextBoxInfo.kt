package com.serial.port

/***
 * 所有 小 中 大 工具类型 名称
 * @code 地址码
 * @toolName 设备名称
 * @electric 电量
 * @gz 通信是否正常
 * @description 描述是否 存在 电量 在仓 通信
 * @isDZG 包含 电量 在仓 通信
 * @status 工具是否被移除显示
 */
enum class EnumTextBoxInfo(val code: Int, val toolName: String, val electric: Int, val gz: Int, val description: String, val isDZG: Boolean, val status: Int) {
    box_2(2, "照相机", 0, 0, "电量|在仓|故障", true, 1),//照相机
    box_3(3, "头灯", 0, 0, "电量|在仓|故障", true, 1),//头灯
    box_4(4, "剥线钳", 0, 0, "在仓", false, 1),//剥线钳
    box_5(5, "多功能钳形电流表", 0, 0, "电量|在仓|故障", true, 1),//多功能钳型电流表
    box_6(6, "电子尺", 0, 0, "电量|在仓|故障", true, 1),//电子尺
    box_7(7, "手电筒", 0, 0, "电量|在仓|故障", true, 1),//手电筒
    box_8(8, "尖嘴钳、 7寸", 0, 0, "在仓", false, 1),//尖嘴钳、 7寸
    box_9(9, "老虎钳、 6寸", 0, 0, "在仓", false, 1),//老虎钳、 6寸
    box_10(10, "电动螺丝刀", 0, 0, "电量|在仓|故障", true, 1),//电动螺丝刀

    box_11(11, "断线钳", 0, 0, "在仓", false, 1),//断线钳
    box_12(12, "扳手", 0, 0, "在仓", false, 1),//扳手
    box_13(13, "多功能测试仪", 0, 0, "电量|在仓|故障", true, 1),//多功能测试仪
    box_14(14, "电助力液压钳", 0, 0, "电量|在仓|故障", true, 1),//电助力液压钳
    box_15(15, "大电钻", 0, 0, "电量|在仓|故障", true, 1),//大电钻

    box_241(241, "AI仪器仪表（主机）", 0, 0, "电量|在仓|故障", true, 1),//AI仪器仪表（主机）
    box_242(242, "AI仪器仪表（A相电流钳）", 0, 0, "电量|在仓|故障", true, 1),//AI仪器仪表（A相电流钳）
    box_243(243, "AI仪器仪表（B相电流钳）", 0, 0, "电量|在仓|故障", true, 1),//AI仪器仪表（B相电流钳）
    box_244(244, "AI仪器仪表（C相电流钳）", 0, 0, "电量|在仓|故障", true, 1),//AI仪器仪表（C相电流钳）

    box_246(246, "照明灯", 0, 0, "电量|在仓|故障", true, 1),//照明灯
    box_245(245, "剥线器", 0, 0, "电量|在仓|故障", true, 1),//剥线器
    box_247(247, "角磨机", 0, 0, "电量|在仓|故障", true, 1),//角磨机
    box_250(250, "应急电源",0, 0, "电量|在仓|故障", true, 1),//应急电源
    box_251(251, "相机外挂模块",0, 0, "在仓", false, 1);//相机外挂模块

    companion object {

        fun fromName(code: Int): String {
            return values().find { it.code == code }?.toolName ?: ".."
        }

        fun fromElectric(code: Int): Int {
            return values().find { it.code == code }?.electric ?: 0
        }

        fun fromGz(code: Int): Int {
            return values().find { it.code == code }?.gz ?: 0
        }

        fun fromDescription(code: Int): String {
            return values().find { it.code == code }?.description ?: ".."
        }

        fun fromDZG(code: Int): Boolean {
            return values().find { it.code == code }?.isDZG ?: false
        }

        fun fromStatus(code: Int): Int {
            return values().find { it.code == code }?.status ?: 0
        }


    }
}