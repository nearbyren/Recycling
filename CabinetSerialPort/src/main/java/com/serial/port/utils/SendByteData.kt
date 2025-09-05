package com.serial.port.utils

object SendByteData {
    /***
     *  0x8B
     */
    const val EX_RE_FRAME_HEADER = 0x8B.toByte()

    /***
     *  0x8A
     */
    const val EX_RE_FRAME_END = 0x8A.toByte()


    /***
     * 接收 0x9B
     */
    const val RE_FRAME_HEADER = 0x9B.toByte()

    /***
     * 接收 0x9A
     */
    const val RE_FRAME_END = 0x9A.toByte()

    /***
     * 发送 0x9A
     */
    const val SE_FRAME_HEADER = 0x9A.toByte()

    /***
     * 发送 0x9B
     */
    const val SE_FRAME_END = 0x9B.toByte()

    /***
     * 开仓 1-13 指令
     */
    /***
     * 查询仓状态
     * @param 0x9A 帧头
     * @param 0x00 地址
     * @param 0x01 指令
     *
     * @param 0x02 数据长度
     * @param 0x01 data1
     * @param 0x01 data2
     *
     * @param 0x1F 校验字节 //内部不使用效验码
     * @param 0x9B 帧尾
     *
     * 内部指令
     * 发送示例指令 0x9A, 0x00, 0x01, 0x02, 0x01, 0x01, 0x9B
     * 返回示例指令 0x9B, 0x00, 0x01, 0x02, 0x01, 0x01, 0x9A
     *
     * 外部指令 暂无
     *
     */

    //开仓指令
    const val CMD_OPEN_STATUS_COMMAND: Byte = 0x01.toByte()
    val CMD_OPEN_1: ByteArray = byteArrayOf(0x01, 0x01)
    val CMD_OPEN_2: ByteArray = byteArrayOf(0x02, 0x01)
    val CMD_OPEN_3: ByteArray = byteArrayOf(0x03, 0x01)
    val CMD_OPEN_4: ByteArray = byteArrayOf(0x04, 0x01)
    val CMD_OPEN_5: ByteArray = byteArrayOf(0x05, 0x01)
    val CMD_OPEN_6: ByteArray = byteArrayOf(0x06, 0x01)
    val CMD_OPEN_7: ByteArray = byteArrayOf(0x07, 0x01)
    val CMD_OPEN_8: ByteArray = byteArrayOf(0x08, 0x01)
    val CMD_OPEN_9: ByteArray = byteArrayOf(0x09, 0x01)
    val CMD_OPEN_10: ByteArray = byteArrayOf(0x0A, 0x01)
    val CMD_OPEN_11: ByteArray = byteArrayOf(0x0B, 0x01)
    val CMD_OPEN_12: ByteArray = byteArrayOf(0x0C, 0x01)
    val CMD_OPEN_13: ByteArray = byteArrayOf(0x0D, 0x01)

    const val CMD_OPEN_FAULT_COMMAND: Byte = 0x04.toByte()


    val  CMD_04_COMMAND  :ByteArray = byteArrayOf(0xaa.toByte(),0xbb.toByte(),0xcc.toByte())

    fun getOpenCommand(lockerNo: Int): Byte {
        return when (lockerNo) {
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

            7 -> {
                0x07.toByte()
            }

            8 -> {
                0x08.toByte()
            }

            9 -> {
                0x09.toByte()
            }

            10 -> {
                0x0A.toByte()
            }

            11 -> {
                0x0B.toByte()
            }

            12 -> {
                0x0C.toByte()
            }

            13 -> {
                0x0D.toByte()
            }

            else -> {
                0x00.toByte()
            }
        }
    }

    /***
     * 查询所有仓状态
     * @param 0x9A, 0x00, 0x02, 0x00, 0x00, 0x9B
     * @return 返回的
     * 1b - 帧头
     * 00 - 地址
     * 02 - 指令
     * f4 - 数据长度
     * 01 - 门状态
     * 63 - 电量
     * .. - SN码
     *
     * 1b 00 02
     * f4
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 01
     * 01 00
     * 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 03
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 04
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 05
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 06
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 07
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 08
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 09
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 0a
     * 01 63
     * 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 0b
     * 00
     * 02
     * 1a
     *
     * 00 - 12门状态
     * 02 - 13门状态
     * 1a - 帧尾
     */
    const val CMD_ALL_STATUS_COMMAND: Byte = 0x02.toByte()
    val CMD_ALL_STATUS_DATA: ByteArray = byteArrayOf()

    /***
     * 查询内部工具箱信息集合
     * @param 0x9A, 0x00, 0x03,0x00, 0x9B
     * @return 返回的
     * 1b - 帧头
     * 00 - 地址
     * 03 - 指令
     * 10 - 数据长度
     *
     * 01 - 箱号
     * 02 - 设备地址
     * 63 - 电量
     * 01 - 是否在箱
     * 01 - 工具状态
     *
     * 02 - 箱号
     * 03 - 设备地址
     * 44 - 电量
     * 01 - 是否在箱
     * 01 - 工具状态
     *
     * 03 - 箱号
     * 04 - 设备地址
     * 37 - 电量
     * 01 - 是否在箱
     * 01 - 工具状态
     *
     * 1a
     */

    const val CMD_BOX_STATUS_COMMAND: Byte = 0x03.toByte()

    /**
     * 构造协议帧的字节数组
     *
     * 方便开发调试去掉效验码
     * @param command 指令
     * @param data 数据域
     * @return 构造好的字节数组 不包含效验码
     */
    fun createSendNotCheckSumByte(command: Byte, data: ByteArray): ByteArray {
        val frameHeader: Byte = SE_FRAME_HEADER
        val address: Byte = 0x00.toByte()
        val frameTail: Byte = SE_FRAME_END
        val dataLength: Byte = data.size.toByte()
        // 构造帧（帧头 + 地址 + 指令 + 长度 + 数据域）
        val frame = mutableListOf<Byte>().apply {
            add(frameHeader)
            add(address)
            add(command)
            add(dataLength)
            addAll(data.toList())
        }
        frame.add(frameTail)
        // 转为字节数组返回
        return frame.toByteArray()
    }

    /**
     * 构造协议帧的字节数组
     *
     * @param command 指令
     * @param data 数据域
     * @return 构造好的字节数组 包含效验码
     */
    fun createSendCheckSumByte(command: Byte, add: Byte, data: ByteArray): ByteArray {
        val frameHeader: Byte = 0x8A.toByte()
        val address: Byte = add
        val frameTail: Byte = 0x8B.toByte()
        val dataLength: Byte = data.size.toByte()

        // 构造帧（帧头 + 地址 + 指令 + 长度 + 数据域）
        val frameWithoutChecksum = mutableListOf<Byte>().apply {
            add(frameHeader)
            add(address)
            add(command)
            add(dataLength)
            addAll(data.toList())
        }

        // 计算校验字节
        val checksum: Byte = (frameWithoutChecksum.sumOf { it.toUByte().toInt() } % 256).toByte()

        // 添加校验字节和帧尾
        frameWithoutChecksum.add(checksum)
        frameWithoutChecksum.add(frameTail)

        // 转为字节数组返回
        return frameWithoutChecksum.toByteArray()
    }

}