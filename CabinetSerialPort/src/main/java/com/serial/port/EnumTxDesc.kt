package com.serial.port

/***
 * 通信状态
 */
enum class EnumTxDesc(val desc: String, val code: Int) {
    NORMAL("正常", 1), FAULT("故障", 2);

    companion object {

        fun findByDesc(desc: String): EnumTxDesc? {
            return values().firstOrNull { it.desc == desc }
        }

        /***
         * 1.正常
         * 2.故障
         */
        fun getDescByCode(desc: String): Int {
            return findByDesc(desc)?.code ?: 2
        }
    }
}