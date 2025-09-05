package com.serial.port

/***
 * 通信状态
 */
enum class EnumTxStatus(val code: Int, val desc: String) {
    NORMAL(1, "正常"), FAULT(2, "故障"), EX(0, "异常");

    companion object {

        fun findByCode(code: Int): EnumTxStatus? {
            return values().firstOrNull { it.code == code }
        }

        /***
         * 1.正常
         * 2.故障
         */
        fun getDescByCode(code: Int): String {
            return findByCode(code)?.desc ?: "异常"
        }
    }
}