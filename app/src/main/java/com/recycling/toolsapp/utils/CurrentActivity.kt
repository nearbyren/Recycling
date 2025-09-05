package com.recycling.toolsapp.utils

interface CurrentActivity {

    interface Config {

        companion object {

            /***
             * 当前需要查看的activity
             * */
            var CURRENT_ROOM_TYPE = -1

            /***
             * 标记当前操作的类型
             * 领取操作
             * 归还操作
             *
             */
            var CURRENT_ROOM_OPTION_TYPE = -1

            /****
             * 标记当前循环的类型
             */
            var CURRENT_CYCLE_TYPE = -1

            /***主界面操作*/
            val isMain
                get() = CURRENT_ROOM_TYPE == Define.ACTIVITY_TYPE_MAIN

            /***领取归还操作*/
            val isOpen
                get() = CURRENT_ROOM_TYPE == Define.ACTIVITY_TYPE_OPEN

            /***领取操作*/
            val isOpen_L
                get() = CURRENT_ROOM_OPTION_TYPE == Define.ACTIVITY_TYPE_OPEN_L

            /***归还操作*/
            val isOpen_H
                get() = CURRENT_ROOM_OPTION_TYPE == Define.ACTIVITY_TYPE_OPEN_H

            /***查看操作*/
            val isLook
                get() = CURRENT_ROOM_TYPE == Define.ACTIVITY_TYPE_LOOK

            /***紧急开仓操作*/
            val isOpenManager
                get() = CURRENT_ROOM_TYPE == Define.ACTIVITY_TYPE_OPEN_MANAGER

            /***系统设置页*/
            val isSystemSettingsPage
                get() = CURRENT_ROOM_TYPE == Define.ACTIVITY_TYPE_SYSTEM

            /***固件升级*/
            val isUpgradePage
                get() = CURRENT_ROOM_TYPE == Define.SYSTEM_UPGRADE_CYCLE_TYPE

            /***系统设置页 处理主芯片*/
            val isMasterChip
                get() = CURRENT_CYCLE_TYPE ==  Define.SYSTEM_MASTER_CYCLE_TYPE

            /***系统设置页 处理从芯片*/
            val isFromChip
                get() = CURRENT_CYCLE_TYPE ==  Define.SYSTEM_FROM_CYCLE_TYPE
        }
    }
}