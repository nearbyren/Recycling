package com.serial.port

import android.annotation.SuppressLint
import com.serial.port.utils.Loge
import com.serial.port.utils.ShellUtils
import com.serial.port.vm.SerialVM


class SerialPortManager private constructor() : SerialPort() {
    private var mSdk232: ConfigurationSdk? = null
    private var mSdk485: ConfigurationSdk? = null

    //标记是否初始化
    private var isInit = false
    var serialVM: SerialVM? = null

    /***************************************初始化信息************************************/

    private object SerialPortInstance {
        @SuppressLint("StaticFieldLeak")
        val SERIALPORT = SerialPortManager()
    }

    companion object {
        val instance: SerialPortManager
            get() = SerialPortInstance.SERIALPORT
    }

    fun init(sdk232: ConfigurationSdk?, sdk485: ConfigurationSdk?) {
        if (isInit) return
        this.mSdk232 = sdk232
        this.mSdk485 = sdk485
        serialVM = SerialVM()
        openSerialPort232()
        openSerialPort485()
        serialVM?.initCollect()
        isInit = true
    }
    /***************************************初始化信息************************************/

    /***************************************指令模块************************************/

    /***
     * 下发升级指令前
     * @param sendBytes
     */
    fun upgrade2322(sendBytes: ByteArray) {
        serialVM?.upgrade2322(-1, sendBytes)
    }

    /***
     * 下发升级指令
     * @param sendBytes
     */
    fun upgrade232(sendBytes: ByteArray) {
        serialVM?.upgrade232(-1, sendBytes)
    }

    /***
     * 下发升级指令
     * @param sendBytes
     */
    fun upgrade485(sendBytes: ByteArray) {
        serialVM?.upgrade485(-1, sendBytes)
    }

    /***
     * 下发升级指令
     * @param sendBytes
     */
    fun upgrade4855(sendBytes: ByteArray) {
        serialVM?.upgrade4855(-1, sendBytes)
    }

    /***
     * 下发开仓指令
     * @param sendBytes
     */
    fun issuedOpen(lockerId: Int, sendBytes: ByteArray) {
        serialVM?.open(lockerId, sendBytes)
    }

    /***
     * 下发故障指令
     * @param sendBytes
     */
    fun issuedFault(lockerId: Int, sendBytes: ByteArray) {
        serialVM?.fault(lockerId, sendBytes)
    }

    /**
     * 下发查询状态指令
     * @param sendBytes
     */
    fun issuedStatus(sendBytes: ByteArray) {
        serialVM?.status(sendBytes)
    }

    /***
     * 查询永胜德仓指令
     */
    fun issuedYSD(commandType: Int, sendBytes: ByteArray) {
        serialVM?.ysd(commandType, sendBytes)
    }

    /***************************************指令模块************************************/

    /***************************************串口打开与关闭************************************/

    /**
     * 打开串口232
     *
     */
    private fun openSerialPort232() {
        close232SerialPort()
        mSdk232?.device?.let { device ->
            if (!device.canRead() || !device.canWrite()) {
                val chmod777: Boolean = chmod777(device)
                if (!chmod777) {
                    Loge.d("授权成功 打开串口232失败 没有权限")
                    return@let
                }
            }
            if (!device.canRead() || !device.canWrite()) {
                try {
                    val command: MutableList<String> = ArrayList()
                    command.add("chmod 777 " + device.absolutePath)
                    ShellUtils.execCommand(command, true)
                } catch (e: Exception) {
                    Loge.d("授权成功 打开串口232失败 授权失败")
                    e.printStackTrace()
                    throw SecurityException()
                }
            }
            try {
                val fileDescriptor = open(device.absolutePath, mSdk232!!.baudRate, 0)
                serialVM?.fd232?.value = fileDescriptor
            } catch (e: Exception) {
                Loge.d("授权成功 打开串口232失败 catch异常")
                e.printStackTrace()
            }
        }
    }

    /**
     * 打开串口485
     *
     */
    private fun openSerialPort485() {
        close485SerialPort()
        mSdk485?.device?.let { device ->
            if (!device.canRead() || !device.canWrite()) {
                val chmod777: Boolean = chmod777(device)
                if (!chmod777) {
                    Loge.d("授权成功 打开串口485失败 没有权限")
                    return@let
                }
            }
            if (!device.canRead() || !device.canWrite()) {
                try {
                    val command: MutableList<String> = ArrayList()
                    command.add("chmod 777 " + device.absolutePath)
                    ShellUtils.execCommand(command, true)
                } catch (e: Exception) {
                    Loge.d("授权成功 打开串口485失败 授权失败")
                    e.printStackTrace()
                    throw SecurityException()
                }
            }
            try {
                val fileDescriptor = open(device.absolutePath, mSdk485!!.baudRate, 0)
                serialVM?.fd485?.value = fileDescriptor
            } catch (e: Exception) {
                Loge.d("授权成功 打开串口485失败 catch异常")
                e.printStackTrace()
            }
        }
    }

    /**
     * 关闭所有串口
     *
     */
    fun closeAllSerialPort() {
        isInit = false
        close232SerialPort()
        close485SerialPort()
    }

    /**
     * 关闭232串口
     */
    fun close232SerialPort() {
        isInit = false
        serialVM?.fd232?.let {
            close(1)
        }
        serialVM?.close232SerialPort()
    }

    /**
     * 关闭485串口
     */
    fun close485SerialPort() {
        isInit = false
        serialVM?.fd485?.let {
            close(2)
        }
        serialVM?.close485SerialPort()
    }

    /***************************************串口打开与关闭************************************/
}