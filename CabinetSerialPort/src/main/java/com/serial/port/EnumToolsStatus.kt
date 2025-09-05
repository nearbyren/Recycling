package com.serial.port

/***
 * 工具是否在仓
 */
enum class EnumToolsStatus(val code: Int, val desc: String) {
    In(1, "在"), NOT(0, "否");

    companion object {

        fun findByCode(code: Int): EnumToolsStatus? {
            return EnumToolsStatus.values().firstOrNull { it.code == code }
        }

        /***
         * 1.在仓
         * 2.不在仓
         */
        fun getDescByCode(code: Int): String {
            return findByCode(code)?.desc ?:"异常"
        }
    }
}