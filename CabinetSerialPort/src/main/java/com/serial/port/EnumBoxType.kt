package com.serial.port

/***
 * 箱子类型
 */
enum class EnumBoxType(val code: Int, val desc: String) {

    SMALL(1, "小"),
    MIDDLE(2, "中"),
    BIG(3, "大"),
    STRIPPER(4, "剥线器"),
    GROCERY(5, "多功能存储仓"),
    IDLE(10, "空闲"),
    IN(11, "在柜"),
    FAULT(101, "故障");

    companion object {

        fun findByCode(code: Int): EnumBoxType? {
            return EnumBoxType.values().firstOrNull { it.code == code }
        }

        /***
         * 1.小
         * 2.中
         * 3.大
         * 4.剥线器
         * 5.多功能存储仓
         * 10.空闲
         * 11.在柜
         * 101.故障
         */
        fun getDescByCode(code: Int): String {
            return findByCode(code)?.desc ?: "无"
        }

        /***
         * @param boxCode 仓号 根据仓号获取所属名称
         */
        fun getDescByName(boxCode: Int): String {
            return when (boxCode) {
                1, 2, 3, 4, 5, 6, 7, 8 -> {
                    getDescByCode(1)
                }

                9, 10 -> {
                    getDescByCode(2)
                }

                11 -> {
                    getDescByCode(3)
                }

                12 -> {
                    getDescByCode(4)
                }

                else -> {
                    getDescByCode(5)
                }
            }
        }
    }
}

