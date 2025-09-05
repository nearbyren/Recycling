package com.serial.port

/***
 * 匹配数据上传到平台
 */
enum class EnumReportBoxType(val desc: String, val code: Int) {

    IN("在仓", 1),
    IDLE("空闲", 0),
    FAULT("故障", -1);

    companion object {

        fun findByDesc(desc: String): EnumReportBoxType? {
            return EnumReportBoxType.values().firstOrNull { it.desc == desc }
        }

        /***
         * 1.在仓
         * 0.空闲
         * -1.故障
         */
        fun getDescByCode(desc: String): Int {
            return findByDesc(desc)?.code ?: 1
        }

    }
}

