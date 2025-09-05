package com.serial.port

/***
 * 门状态
 */
enum class EnumDoorStatus(val code: Int, val desc: String) {

    CLOSE(0, "关"), OPEN(1, "开"), FAULT(2, "故障"),TIMEOUT(3,"超时"),PROTECTED(4,"受保护");

    companion object {

        fun findByCode(code: Int): EnumDoorStatus? {
            return EnumDoorStatus.values().firstOrNull { it.code == code }
        }

        /***
         * 0.关
         * 1.开
         * 2.故障
         * 3.超时
         * 4.受保护
         */
        fun getDescByCode(code: Int): String {
            return findByCode(code)?.desc ?: "异常"
        }
    }
}

