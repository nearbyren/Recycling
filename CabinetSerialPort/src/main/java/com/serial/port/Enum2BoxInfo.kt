package com.serial.port

/***
 * 基础作业中型箱
 */
enum class Enum2BoxInfo(val code: Int, val address: Int) {
    box_2(1, 2),//照相机
    box_3(2, 3),//头灯
    box_4(3, 4),//剥线钳
    box_7(4, 7),//多功能钳形电流表
    box_8(5, 8),//尖嘴钳、 7寸
    box_9(6, 9),//老虎钳、 6寸
    box_10(7, 10),//电动螺丝刀
    box_241(8, 241),//AI仪器仪表（主机）
    box_242(9, 242),//AI仪器仪表（A相电流钳）
    box_243(10, 243),//AI仪器仪表（B相电流钳）
    box_244(11, 244),//AI仪器仪表（C相电流钳）
    box_251(12, 251);//相机外挂模块

    companion object {
        /***
         * 角标从1开始
         */
        const val quantity = 12
        fun fromCode(code: Int): Int {
            return values().find { it.code == code }?.address ?: -1
        }
    }
}