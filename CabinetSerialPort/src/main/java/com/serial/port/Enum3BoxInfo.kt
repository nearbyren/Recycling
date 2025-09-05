package com.serial.port

/***
 * 多功能作业大型箱
 */
enum class Enum3BoxInfo(val code: Int, val address: Int) {
    box_2(1, 2),//照相机
    box_8(2, 8),//尖嘴钳、 7寸
    box_9(3, 9),//老虎钳、 6寸
    box_11(4, 11),//断线钳
    box_12(5, 12),//扳手
    box_13(6, 13),//多功能测试仪
    box_14(7, 14),//电助力液压钳
    box_15(8, 15),//大电钻
    box_246(9, 246),//照明灯
    box_247(10, 247),//角磨机
    box_250(11, 250),//应急电源
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