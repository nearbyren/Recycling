package com.serial.port


import com.serial.port.utils.CmdCode
import com.serial.port.utils.HexConverter
import com.serial.port.utils.SendByteData
import com.serial.port.utils.SendByteData.getOpenCommand


class SerialPortCore {

    /***
     * 启动格口开关
     */
    private val startDoor: MutableMap<Int, ByteArray> = mutableMapOf(
        //格口一
        CmdCode.GE10 to byteArrayOf(0x01, 0x00),//关
        CmdCode.GE11 to byteArrayOf(0x01, 0x01),//开
        //格口二
        CmdCode.GE20 to byteArrayOf(0x02, 0x00),//关
        CmdCode.GE21 to byteArrayOf(0x02, 0x01),//开

    )

    /***
     * 启动格口状态查询
     */
    private val startDoorStatus: MutableMap<Int, ByteArray> = mutableMapOf(
        //格口一
        CmdCode.GE1 to byteArrayOf(0x01, 0x01),
        //格口二
        CmdCode.GE2 to byteArrayOf(0x02, 0x01),

        )

    /***
     * 启动清运门
     */
    private val clearDoor: MutableMap<Int, ByteArray> = mutableMapOf(
        //格口一
        1 to byteArrayOf(0x01, 0x01),
        //格口二
        2 to byteArrayOf(0x02, 0x01),
        //子母格口
        3 to byteArrayOf(0x03, 0x01),
    )

    /***
     * 查询重量
     */
    private val weightDoor: MutableMap<Int, ByteArray> = mutableMapOf(
        //格口一
        CmdCode.GE1 to byteArrayOf(0x01, 0x01),
        //格口二
        CmdCode.GE2 to byteArrayOf(0x02, 0x01),
    )

    /***
     * 发起开启单仓指令
     */
    private val sendSingleCmd: MutableMap<Int, ByteArray> = mutableMapOf(
        1 to SendByteData.CMD_OPEN_1,
        2 to SendByteData.CMD_OPEN_2,
        3 to SendByteData.CMD_OPEN_3,
        4 to SendByteData.CMD_OPEN_4,
        5 to SendByteData.CMD_OPEN_5,
        6 to SendByteData.CMD_OPEN_6,
        7 to SendByteData.CMD_OPEN_7,
        8 to SendByteData.CMD_OPEN_8,
        9 to SendByteData.CMD_OPEN_9,
        10 to SendByteData.CMD_OPEN_10,
        11 to SendByteData.CMD_OPEN_11,
        12 to SendByteData.CMD_OPEN_12,
        13 to SendByteData.CMD_OPEN_13,
    )

    /***
     * 固件升级前动作
     * @param commandType 指令类型
     * @param onUpgrade 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun firmwareUpgrade2322(commandType: Int, data: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandUpgrade232ResultListener { status ->
            onUpgrade(status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        when (commandType) {
            7 -> {
                val command = 0x07.toByte()
                val sendByte = SendByteData.createSendNotCheckSumByte(command, data)
                SerialPortManager.instance.upgrade232(sendByte)
            }

            8 -> {
                val command = 0x08.toByte()
                val data2 = byteArrayOf(0xa1.toByte(), 0xa2.toByte(), 0xa3.toByte())
                val sendByte = HexConverter.combineByteArrays(data2, data)
                val byte = SendByteData.createSendNotCheckSumByte(command, sendByte)
                SerialPortManager.instance.upgrade232(byte)
            }

            9 -> {
                val command = 0x09.toByte()
                val byte = SendByteData.createSendNotCheckSumByte(command, data)
                SerialPortManager.instance.upgrade232(byte)
            }

            10 -> {
                val command = 0x0a.toByte()
                val sendByte = SendByteData.createSendNotCheckSumByte(command, data)
                SerialPortManager.instance.upgrade232(sendByte)
            }

            11 -> {
                val command = 0x0b.toByte()
                val sendByte = SendByteData.createSendNotCheckSumByte(command, data)
                SerialPortManager.instance.upgrade232(sendByte)
            }
        }

    }

    /***
     * 查询主芯片版本号
     * @param callback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun queryVersion232(data: ByteArray, callback: (commandType: Int, MutableMap<Int, MutableMap<Int, BoxInternal>>) -> Unit, sendCallback: (String) -> Unit) {
        // 封装回调传递
        SerialPortManager.instance.serialVM?.addCommandQueryListYSDResultListener { commandType, result ->
            callback(commandType, result) // 直接触发传入的回调
        }
        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        val command = 0x0b.toByte()
        val sendByte = SendByteData.createSendNotCheckSumByte(command, data)
        SerialPortManager.instance.upgrade232(sendByte)
    }

    /***
     * 固件升级
     * @param onUpgrade 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun firmwareUpgrade232(byte: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandUpgrade232ResultListener { status ->
            onUpgrade(status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        SerialPortManager.instance.upgrade232(byte)
    }

    /***
     * 固件升级前动作
     * @param commandType 指令类型
     * @param onUpgrade 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun firmwareUpgrade4855(commandType: Int, data: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandUpgrade485ResultListener { status ->
            onUpgrade(status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        when (commandType) {
            7 -> {
                val command = 0x07.toByte()
                val sendByte = SendByteData.createSendCheckSumByte(command, 0x00.toByte(), data)
                SerialPortManager.instance.upgrade485(sendByte)
            }

            8 -> {
                val command = 0x08.toByte()
                val data2 = byteArrayOf(0xa1.toByte(), 0xa2.toByte(), 0xa3.toByte())
                val sendByte = HexConverter.combineByteArrays(data2, data)
                val byte = SendByteData.createSendCheckSumByte(command, 0x00.toByte(), sendByte)
                SerialPortManager.instance.upgrade485(byte)
            }

            9 -> {
                val command = 0x09.toByte()
                val byte = SendByteData.createSendCheckSumByte(command, 0x00.toByte(), data)
                SerialPortManager.instance.upgrade485(byte)
            }

            10 -> {
                val command = 0x0a.toByte()
                val sendByte = SendByteData.createSendCheckSumByte(command, 0x00.toByte(), data)
                SerialPortManager.instance.upgrade485(sendByte)
            }

            11 -> {
                val command = 0x0b.toByte()
                val sendByte = SendByteData.createSendCheckSumByte(command, 0x00.toByte(), data)
                SerialPortManager.instance.upgrade485(sendByte)
            }
        }
    }

    /***
     * 固件升级
     * @param onUpgrade 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun firmwareUpgrade485(byte: ByteArray, onUpgrade: (status: Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandUpgrade485ResultListener { status ->
            onUpgrade(status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        SerialPortManager.instance.upgrade485(byte)
    }

    /***
     * 查询从芯片版本号
     * @param callback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun queryVersion485(boxCode: Int, commandType: Int, data: ByteArray, callback: (commandType: Int, MutableMap<Int, MutableMap<Int, BoxInternal>>) -> Unit, sendCallback: (String) -> Unit) {
        // 封装回调传递
        SerialPortManager.instance.serialVM?.addCommandQueryListYSDResultListener { commandType, result ->
            callback(commandType, result) // 直接触发传入的回调
        }
        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        val byte = SendByteData.createSendCheckSumByte(0x0B.toByte(), 0x00.toByte(), data)
        SerialPortManager.instance.issuedYSD(commandType, byte)
    }

    /***
     * 下报大箱剥线电量
     * @param data 下报的 电量 在位 故障
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun reportedPower(data: ByteArray, reportedCallback: (Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandReportResultListener { status ->
            reportedCallback(status)
        }
        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        val byte = SendByteData.createSendNotCheckSumByte(0x04.toByte(), data)
        SerialPortManager.instance.issuedStatus(byte)
    }

    /***
     * @param code 仓位
     * @param openCallback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun openClear(code: Int, openCallback: (Int, Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandOpenResultListener { number, status ->
            openCallback(number, status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        clearDoor[code]?.let {
            val command = 0x02.toByte()
            val data = it
            val byte = SendByteData.createSendNotCheckSumByte(command, data)
            SerialPortManager.instance.issuedOpen(code, byte)
        }
    }

    /***
     * 查询所有仓 门状态 电量 sn 【注：1-11仓 有电量和sn】
     * @param onBoxStatusCallback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun queryStatus(onBoxStatusCallback: (MutableList<PortDeviceInfo>) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandQueryListResultListener { result ->
            onBoxStatusCallback(result)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        val command = SendByteData.CMD_ALL_STATUS_COMMAND
        val data = SendByteData.CMD_ALL_STATUS_DATA
        val byte = SendByteData.createSendNotCheckSumByte(command, data)
        SerialPortManager.instance.issuedStatus(byte)

    }

    /***
     * 启动格口开关
     */
    @Synchronized
    fun turnDoor(code: Int, turnDoorCallback: (Int, Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandTurnResultListener { number, status ->
            turnDoorCallback(number, status)
        }
        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        startDoor[code]?.let {
            val command = 0x01.toByte()
            val data = it
            val byte = SendByteData.createSendNotCheckSumByte(command, data)
            SerialPortManager.instance.issuedStatus(byte)

        }
    }

    /***
     * 启动格口状态查询
     */
    @Synchronized
    fun turnDoorStatus(boxCode: Int, openCallback: (Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandDoorResultListener { status ->
            openCallback(status)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        startDoorStatus[boxCode]?.let {
            val command = 0x03.toByte()
            val data = it
            val byte = SendByteData.createSendNotCheckSumByte(command, data)
            SerialPortManager.instance.issuedStatus(byte)
        }
    }

    /***
     * 启动格口重量
     */
    @Synchronized
    fun queryWeight(code: Int, weightCallback: (Int) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandWeightResultListener { weight ->
            weightCallback(weight)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }

        weightDoor[code]?.let {
            val command = 0x04.toByte()
            val data = it
            val byte = SendByteData.createSendNotCheckSumByte(command, data)
            SerialPortManager.instance.issuedStatus(byte)

        }
    }

    /***
     * 根据工具箱箱号查询内部工具箱信息集合查询指令发送响应
     * @param commandType 0x01-0x0b
     * @param onBoxToolStatusCallback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun queryWarehouseInternalCommand(commandType: Int, onBoxToolStatusCallback: (MutableMap<Int, BoxInternal>) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandQueryInternalListResultListener { result ->
            onBoxToolStatusCallback(result)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        val command = SendByteData.CMD_BOX_STATUS_COMMAND
        val data = getOpenCommand(commandType)
        val byte = SendByteData.createSendNotCheckSumByte(command, byteArrayOf(data))
        SerialPortManager.instance.issuedStatus(byte)

    }

    /***
     * 查询单个仓的 门状态 电量 sn
     * @param boxCode 仓位
     * @param onBoxStatusCallback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized @Deprecated("弃用")
    fun queryWarehouseCommand(boxCode: Int, onBoxStatusCallback: (PortDeviceInfo) -> Unit, sendCallback: (String) -> Unit) {
        SerialPortManager.instance.serialVM?.addCommandQueryBeanResultListener { result ->
            onBoxStatusCallback(result)
        }

        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
        sendSingleCmd[boxCode]?.let {
            val command = getOpenCommand(boxCode)
            val data = it
            val byte = SendByteData.createSendNotCheckSumByte(command, data)
            SerialPortManager.instance.issuedStatus(byte)
        }
    }

    /***
     * 万盛德指令
     * @param commandType 指令类型 commandType
     * @param callback 下发指令接收反馈信息
     * @param sendCallback 下发指令是否成功回调
     */
    @Synchronized
    fun queryYSD(boxCode: Int, commandType: Int, data: ByteArray, callback: (commandType: Int, MutableMap<Int, MutableMap<Int, BoxInternal>>) -> Unit, sendCallback: (String) -> Unit) {
        // 封装回调传递
        SerialPortManager.instance.serialVM?.addCommandQueryListYSDResultListener { commandType, result ->
            callback(commandType, result) // 直接触发传入的回调
        }
        SerialPortManager.instance.serialVM?.addSendCommandStatusListener { msg ->
            sendCallback(msg)
        }
//        val hexString = "0x" + boxCode.toString(16).padStart(2, '0').uppercase(Locale.ROOT)
//        // 解析回 Byte 类型
//        val address: Byte = parseByte(hexString.substring(2), 16)

        val add = when (boxCode) {
            11 -> {
                0xFF.toByte()
            }

            12 -> {
                0xF5.toByte()
            }

            else -> {
                0x00.toByte()
            }
        }
        val command = when (commandType) {
            1 -> {
                0x01.toByte()
            }

            2 -> {
                0x02.toByte()
            }

            3 -> {
                0x03.toByte()
            }

            4 -> {
                0x04.toByte()
            }

            5 -> {
                0x05.toByte()
            }

            6 -> {
                0x06.toByte()
            }

            else -> {
                0x00.toByte()
            }
        }

        val byte = SendByteData.createSendCheckSumByte(command, add, data)
        SerialPortManager.instance.issuedYSD(commandType, byte)
    }


}