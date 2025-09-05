package com.serial.port.vm

object CommandType {

    /***
     *   管理柜获取箱子电池电量（包含箱子本身以及箱内各设备）
     */
    const val ysd_command_01 = 1

    /***
     * 管理柜获取箱内设备在仓信息
     */
    const val ysd_command_02 = 2

    /***
     * 管理柜获取箱子故障信息（包含箱子本身以及箱内各设备）
     */
    const val ysd_command_03 = 3

    /***
     * 管理柜获取箱子SN号
     */
    const val ysd_command_04 = 4

    /***
     *管理柜获取剥线器电量
     */
    const val ysd_command_05 = 5

    /***
     *管理柜获取剥线器故障
     */
    const val ysd_command_06 = 6


    /***
     *升级 升级前指令
     */
    const val ysd_command_07 = 7

    /***
     *升级 查询状态指令
     */
    const val ysd_command_08 = 8

    /***
     *升级 发送文件结束指令
     */
    const val ysd_command_09 = 9

    const val gdw_command_01 = 1
    const val gdw_command_02 = 2
    const val gdw_command_03 = 3
}