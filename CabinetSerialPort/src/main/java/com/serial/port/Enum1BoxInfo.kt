package com.serial.port

/***
 * 量测作业小型箱
 */
enum class Enum1BoxInfo(val code: Int, val address: Int) {
    box_2(1, 2),//照相机
    box_3(2, 3),//头灯
    box_4(3, 4),//剥线钳
    box_5(4, 5),//多功能钳形电流表
    box_6(5, 6),//电子尺
    box_8(6, 8),//尖嘴钳、 7寸
    box_9(7, 9),//老虎钳、 6寸
    box_10(8, 10),//电动螺丝刀
    box_251(9, 251);//相机外挂模块

    companion object {
        /***
         * 角标从1开始
         */
        const val quantity = 9
        fun fromCode(code: Int): Int {
            return values().find { it.code == code }?.address ?: -1
        }
    }
}