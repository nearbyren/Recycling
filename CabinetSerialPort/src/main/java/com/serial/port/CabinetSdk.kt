package com.serial.port

import java.io.File

class CabinetSdk private constructor() {

    companion object {

        private var isInit = false
        private var serialPortCore: SerialPortCore? = null

        /***
         * genymotion  ttyS1
         * 平板 232 ttyS2
         * 平板 485 ttyS3
         */
        fun init() {
            if (isInit) return
            isInit = true
            //串口232 genymotion模拟器配置 ttyS1 ttyS2  真实环境 ttys2 ttys3 说明：匹配oracle VM virtualBox 串口编号（N）COM2  路径/地址COM3映射的PC串口COM3
            val sdk: ConfigurationSdk =
                    ConfigurationSdk.ConfigurationBuilder(File("/dev/ttyS0"), 115200).log("TAG", true, false).build()
            //485
            val sdk485: ConfigurationSdk =
                    ConfigurationSdk.ConfigurationBuilder(File("/dev/ttyS4"), 115200).log("TAG", true, false).build()
            serialPortCore = SerialPortCore()
            SerialPortManager.instance.init(sdk, sdk485)
        }

        /***
         * 释放资源
         */
        fun release() {
            serialPortCore = null
            isInit = false
            SerialPortManager.instance.closeAllSerialPort()
        }

        /***
         * 启动投口
         * @param code
         * @param turnDoorCallback
         * @param sendCallback 发送是否成功
         */
        @Synchronized
        fun turnDoor(code: Int, turnDoorCallback: (Int, Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.turnDoor(code, turnDoorCallback, sendCallback)
        }

        /***
         * 启动投口状态查询
         * @param code
         * @param onDoorStatus
         * @param sendCallback 发送是否成功
         */
        @Synchronized
        fun turnDoorStatus(code: Int, onDoorStatus: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.turnDoorStatus(code, onDoorStatus, sendCallback)
        }

        /***
         * 清运门开启
         * @param code 类型
         * @param onOpenStatus
         * @param sendCallback
         */
        @Synchronized
        fun openClear(code: Int, onOpenStatus: (boxCode: Int, status: Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.openClear(code, onOpenStatus, sendCallback)
        }

        /***
         * 启动查询投口重量
         * @param weightCallback
         * @param sendCallback 发送是否成功
         */
        @Synchronized
        fun queryWeight(boxCode: Int, weightCallback: (Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.queryWeight(boxCode, weightCallback, sendCallback)
        }

        /***
         * 启动查询当前设备状态
         * @param onBoxStatus
         * @param sendCallback 发送是否成功
         */
        @Synchronized
        fun queryStatus(onBoxStatus: (lowerMachines: MutableList<PortDeviceInfo>) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.queryStatus(onBoxStatus, sendCallback)
        }

        /***
         *  启动灯光控制
         * @param lightsCallback
         * @param sendCallback 发送是否成功
         */
        @Synchronized
        fun startLights(boxCode: Int, lightsCallback: (Int, Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.startLights(boxCode, lightsCallback, sendCallback)
        }

        /***
         * 固件升级前动作
         * @param byte
         * @param onUpgrade 返回开仓是否成功
         * @param sendCallback 发送是否成功
         */
        fun firmwareUpgrade2322(commandType: Int, byte: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.firmwareUpgrade2322(commandType, byte, onUpgrade, sendCallback)
        }

        /***
         * 固件升级
         * @param byte
         * @param onUpgrade 返回开仓是否成功
         * @param sendCallback 发送是否成功
         */
        fun firmwareUpgrade232(byte: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.firmwareUpgrade232(byte, onUpgrade, sendCallback)
        }

        /***
         * 芯片版本查询
         * @param byte
         * @param onUpgrade 返回开仓是否成功
         * @param sendCallback 发送是否成功
         */
        fun queryVersion232(commandType: Int, byte: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
            serialPortCore?.firmwareUpgrade2322(commandType, byte, onUpgrade, sendCallback)
        }

    }
}
