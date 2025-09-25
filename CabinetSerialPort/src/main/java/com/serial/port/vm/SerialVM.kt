package com.serial.port.vm

import androidx.lifecycle.ViewModel
import com.serial.port.BoxInternal
import com.serial.port.EnumToolsStatus
import com.serial.port.EnumTxStatus
import com.serial.port.PortDeviceInfo
import com.serial.port.call.CommandCalibrationResultListener
import com.serial.port.call.CommandDoorResultListener
import com.serial.port.call.CommandLightsResultListener
import com.serial.port.call.CommandOpenResultListener
import com.serial.port.call.CommandQueryBeanResultListener
import com.serial.port.call.CommandQueryInternalListResultListener
import com.serial.port.call.CommandQueryListResultListener
import com.serial.port.call.CommandQueryListYSDResultListener
import com.serial.port.call.CommandReportResultListener
import com.serial.port.call.CommandSendResultListener
import com.serial.port.call.CommandStatus
import com.serial.port.call.CommandTurnResultListener
import com.serial.port.call.CommandUpgrade232ResultListener
import com.serial.port.call.CommandUpgrade485ResultListener
import com.serial.port.call.CommandWeightResultListener
import com.serial.port.call.DoorStatus
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.HexConverter
import com.serial.port.utils.Loge
import com.serial.port.utils.SendByteData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class SerialVM : ViewModel() {
    /*******************基础协程和变量************************************************/

    //串口232描述文件
    val fd232 = MutableStateFlow<FileDescriptor?>(null)
    private val fileDes232: StateFlow<FileDescriptor?> = fd232

    //接收
    private val fis232 = MutableStateFlow<FileInputStream?>(null)
    private val fis232Read: StateFlow<FileInputStream?> = fis232

    //发送
    private val fos232 = MutableStateFlow<FileOutputStream?>(null)
    private val fosSend232: StateFlow<FileOutputStream?> = fos232

    //串口485描述文件
    val fd485 = MutableStateFlow<FileDescriptor?>(null)
    private val fileDes485: StateFlow<FileDescriptor?> = fd485

    //接收
    private val fis485 = MutableStateFlow<FileInputStream?>(null)
    private val fisRead485: StateFlow<FileInputStream?> = fis485

    //发送
    private val fos485 = MutableStateFlow<FileOutputStream?>(null)
    private val fosSend485: StateFlow<FileOutputStream?> = fos485

    /**
     * 用于处理 I/O 操作的协程作用域
     */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 用于处理 main 操作的协程作用域
     */
    private val mainScope = MainScope()

    /***
     * 用于处理 默认协程作用域
     */
    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 开启发送消息的协程 232
     */
    private var sending232Job: Job? = null

    /**
     * 开启发送消息的协程 232状态查询
     */
    private var sendingStatus232Job: Job? = null

    /**
     * 开启接收消息的协程 232
     */
    private var read232Job: Job? = null

    /**
     * 开启发送消息的协程 485
     */
    private var sending485Job: Job? = null

    /**
     * 开启发送消息的协程 485 状态查询
     */
    private var sendingStatus485Job: Job? = null

    /**
     * 开启接收消息的协程
     */
    private var read485Job: Job? = null

    /**
     * 标记当前执行的指令
     */
    private var commandType485 = -1

    /**
     * 重试次数
     */
    private val maxRetryCount = 3

    /**
     * 重试次数
     */
    private val maxRetryCount2 = 6

    /**
     * 重试次当前重试次数数
     */
    private var retryCount = 0

    /**
     * 重试次当前重试次数数
     */
    private var retryCount2 = 0

    /***
     * 创建一个互斥锁
     */
    private val mutex = Mutex()

    /**
     * 线程安全的状态变量 接收数据
     */
    private val isOpenRecData = AtomicBoolean(false)

    /**
     * 线程安全的状态变量 接收到数据并且是开锁成功
     */
    private var isOpenStatus = AtomicBoolean(false)

    /**
     * 线程安全的状态变量 接收数据
     */
    private val isUpgradeRecData = AtomicBoolean(false)

    /**
     * 线程安全的状态变量 接收到数据并且是开锁成功
     */
    private var isUpgradeStatus = AtomicBoolean(false)

    /*******************基础协程和变量************************************************/

    /******************************************响应回调方法************************************************/
    /***
     * 发送指令是否成功响应结果
     */
    private var commandSendResultListener: CommandSendResultListener? = null
    fun addSendCommandStatusListener(callback: (String) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandSendResultListener = CommandSendResultListener { msg ->
            // 调用传递的回调
            callback(msg)
        }
    }

    /***
     * 所有仓查询指令发送响应结果
     */
    private var commandQueryListResultListener: CommandQueryListResultListener? = null
    fun addCommandQueryListResultListener(callback: (MutableList<PortDeviceInfo>) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandQueryListResultListener = CommandQueryListResultListener { lockerInfos ->
            // 调用传递的回调
            callback(lockerInfos)
        }
    }

    /***
     * 所有内部工具箱信息集合查询指令发送响应
     */
    private var commandQueryInternalListResultListener: CommandQueryInternalListResultListener? =
            null

    fun addCommandQueryInternalListResultListener(callback: (MutableMap<Int, BoxInternal>) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandQueryInternalListResultListener =
                CommandQueryInternalListResultListener { lockerInfos ->
                    // 调用传递的回调
                    callback(lockerInfos)
                }
    }

    /***
     * 单仓查询指令发送响应结果
     */
    private var commandQueryBeanResultListener: CommandQueryBeanResultListener? = null
    fun addCommandQueryBeanResultListener(callback: (PortDeviceInfo) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandQueryBeanResultListener = CommandQueryBeanResultListener { lockerInfos ->
            // 调用传递的回调
            callback(lockerInfos)
        }
    }

    /***
     * 仓查询指令发送响应结果
     */
    private var commandQueryListYSDResultListener: CommandQueryListYSDResultListener? = null
    fun addCommandQueryListYSDResultListener(callback: (commandType: Int, MutableMap<Int, MutableMap<Int, BoxInternal>>) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandQueryListYSDResultListener =
                CommandQueryListYSDResultListener { commandType, lockerInfos ->
                    // 调用传递的回调
                    callback(commandType, lockerInfos)
                }
    }

    /***
     *
     *
     * 固件升级指令发送响应结果
     */
    private var commandUpgrade232ResultListener: CommandUpgrade232ResultListener? = null
    fun addCommandUpgrade232ResultListener(callback: (Int) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandUpgrade232ResultListener = CommandUpgrade232ResultListener { status ->
            // 调用传递的回调
            callback(status)
        }
    }

    /***
     *
     *
     * 固件升级指令发送响应结果
     */
    private var commandUpgrade485ResultListener: CommandUpgrade485ResultListener? = null
    fun addCommandUpgrade485ResultListener(callback: (Int) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandUpgrade485ResultListener = CommandUpgrade485ResultListener { status ->
            // 调用传递的回调
            callback(status)
        }
    }

    /***
     *
     * 开仓指令发送响应结果
     */
    private var commandOpenResultListener: CommandOpenResultListener? = null
    fun addCommandOpenResultListener(callback: (Int, Int) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandOpenResultListener = CommandOpenResultListener { number, status ->
            // 调用传递的回调
            callback(number, status)
        }
    }

    /***
     *
     *发起门操作
     */
    private var commandTurnResultListener: CommandTurnResultListener? = null
    fun addCommandTurnResultListener(callback: (Int, Int) -> Unit) {
        this.commandTurnResultListener = CommandTurnResultListener { number, status ->
            // 调用传递的回调
            callback(number, status)
        }
    }

    /***
     *
     *查询当前重量
     */
    private var commandWeightResultListener: CommandWeightResultListener? = null
    fun addCommandWeightResultListener(callback: (Int) -> Unit) {
        this.commandWeightResultListener = CommandWeightResultListener { weight ->
            // 调用传递的回调
            callback(weight)
        }
    }

    /***
     *
     * 灯光
     */
    private var commandLightsResultListener: CommandLightsResultListener? = null
    fun addCommandLightsResultListener(callback: (Int, Int) -> Unit) {
        this.commandLightsResultListener = CommandLightsResultListener { number, status ->
            // 调用传递的回调
            callback(number, status)
        }
    }

    /***
     *
     * 校准
     */
    private var commandCalibrationResultListener: CommandCalibrationResultListener? = null
    fun addCommandCalibrationResultListener(callback: (Int, Int) -> Unit) {
        this.commandCalibrationResultListener = CommandCalibrationResultListener { number, status ->
            // 调用传递的回调
            callback(number, status)
        }
    }

    /***
     *
     *查询门状态
     */
    private var commandDoorResultListener: CommandDoorResultListener? = null
    fun addCommandDoorResultListener(callback: (Int) -> Unit) {
        this.commandDoorResultListener = CommandDoorResultListener { status ->
            // 调用传递的回调
            callback(status)
        }
    }


    private var commandReportResultListener: CommandReportResultListener? = null
    fun addCommandReportResultListener(callback: (Int) -> Unit) {
        // 使用 lambda 表达式作为回调
        this.commandReportResultListener = CommandReportResultListener { status ->
            // 调用传递的回调
            callback(status)
        }
    }

    /******************************************响应回调方法************************************************/

    /******************************************获取串口描述符和输入输出流************************************************/
    fun initCollect() {
        mainScope.launch {
            fileDes232.collect { file ->
                //获取串口文件描述符成功
                file?.let {
                    Loge.i("串口232", "接232 获取串口文件描述完成开启构建读写流232注册")
                    fis232.value = FileInputStream(file)
                    fos232.value = FileOutputStream(file)
                } ?: run {
                    Loge.i("串口232", "接232 获取串口文件描述完成开启构建读写流232注销")
                }
            }
        }

        mainScope.launch {
            fileDes485.collect { file ->
                //获取串口文件描述符成功
                file?.let {
                    Loge.i("串口485", "接485 获取串口文件描述完成开启构建读写流485注册")
                    fis485.value = FileInputStream(file)
                    fos485.value = FileOutputStream(file)
                } ?: run {
                    Loge.i("串口485", "接485 获取串口文件描述完成开启构建读写流485注销")
                }
            }
        }


        mainScope.launch {
            fis232Read.collect { c ->
                c?.apply {
                    Loge.i("串口232", "接232 构建读取流232注册")
                    startRead232Job()
//                    startRead232JobNew()
//                    startRead232JobNew2()
                } ?: run {
                    stopRead232Job()
                    Loge.i("串口232", "接232 构建读取流流232注销")
                }
            }
        }

        mainScope.launch {
            fosSend232.collect { c ->
                c?.apply {
                    Loge.i("串口232", "接232 构建写入流232注册")
                } ?: run {
                    stopSend232Job()
                    Loge.i("串口232", "接232 构建写入流232注销")
                }
            }
        }


        mainScope.launch {
            fisRead485.collect { c ->
                c?.apply {
                    Loge.i("串口485", "接485 构建读取流485注册")
                    startRead485Job()
                } ?: run {
                    Loge.i("串口485", "接485 构建读取流流485注销")
                    stopSend485Job()
                }
            }
        }

        mainScope.launch {
            fosSend485.collect { c ->
                c?.apply {
                    Loge.i("串口485", "接485 构建写入流485注册")
                } ?: run {
                    stopSend485Job()
                    Loge.i("串口485", "接485 构建写入流485注销")
                }
            }
        }
    }

    fun <T> Flow<T>.onEmpty(
        action: suspend () -> Unit,
    ): Flow<T> = flow {
        var isEmpty = true
        collect { value ->
            isEmpty = false
            emit(value)
        }
        if (isEmpty) action()
    }
    /******************************************获取串口描述符和输入输出流************************************************/

    /******************************************发送指令**************************************************************/
    /***
     * 升级指令前
     * @param sendBytes
     */
    fun upgrade2322(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                startSend232Job(lockerId, -1, sendBytes)
            }
        }
    }

    /***
     * 升级指令
     * @param sendBytes
     */
    fun upgrade232(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                startSend232Job(lockerId, -1, sendBytes)
            }
        }
    }

    /***
     * 开仓指令
     * @param sendBytes
     */
    fun open(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                retryJobS(lockerId, -1, sendBytes)
            }
        }
    }

    /***
     * 故障指令
     * @param sendBytes
     */
    fun fault(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                startSend232Job(lockerId, -1, sendBytes)
            }
        }
    }

    /***
     *查询状态指令
     * @param sendBytes
     */
    fun status(sendBytes: ByteArray) {
        mainScope.launch {
            startSendStatus232Job(sendBytes)
        }
    }

    /***
     * 查询永胜德板子指令
     */
    fun ysd(commandType: Int, sendBytes: ByteArray) {
        mainScope.launch {
            this@SerialVM.commandType485 = commandType
            startSendStatus485Job(sendBytes)
        }
    }

    /***
     * 升级指令
     * @param sendBytes
     */
    fun upgrade485(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                startSend485Job(lockerId, -1, sendBytes)
            }
        }
    }

    /***
     * 升级指令
     * @param sendBytes
     */
    fun upgrade4855(lockerId: Int, sendBytes: ByteArray) {
        mainScope.launch {
            mutex.withLock {
                startSend485Job(lockerId, -1, sendBytes)
            }
        }
    }

    /******************************************发送指令**************************************************************/

    /******************************************发送类型指令**************************************************************/

    /***
     * 重试开仓
     */
    private suspend fun retryJobS2(boxCode: Int, type: Int, sendBytes: ByteArray) {
        Loge.i("串口232", "接232 retryJobS 指令：${ByteUtils.toHexString(sendBytes)} 仓：$boxCode, ${if (type == 1) "发送开仓指令" else "发送关仓指令"}")
        while (retryCount2 < maxRetryCount2) { // 在未达到最大重试次数且 isCan 为 false 时继续重试
            retryCount2++
            val rec = isUpgradeRecData.get()
            val sta = isUpgradeStatus.get()
//            Loge.i("串口232","接232 retryJobS 尝试第 $retryCount 次 $lockerId 仓 指令：${ByteUtils.toHexString(sendBytes)}, ${if (type == 1) "发起开仓" else "发起关仓"} 读取数据：$rec,状态：$sta")
            Loge.i("串口232", "接232 retryJobS 尝试第 $retryCount2 次 $boxCode 仓 指令：${ByteUtils.toHexString(sendBytes)}, 发起开仓 读取数据：$rec,状态：$sta")
            if (rec && sta) {
                Loge.i("串口232", "接232 retryJobS 接收到数据 任务成功，停止重试")
                isUpgradeRecData.set(false)
                isUpgradeStatus.set(false)
                retryCount2 = 0
                commandUpgrade232ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                break
            }
            if (retryCount2 >= maxRetryCount2) {
                Loge.i("串口232", "接232 retryJobS 达到最大重试次数，停止重试")
                isUpgradeRecData.set(false)
                isUpgradeStatus.set(false)
                retryCount2 = 0
                commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAULT)
                break
            }
            startSend232Job(boxCode, type, sendBytes)
            delay(1000)
        }
    }

    /***
     * 重试开仓
     */
    private suspend fun retryJobS(boxCode: Int, type: Int, sendBytes: ByteArray) {
        Loge.i("串口232", "接232 retryJobS 指令：${ByteUtils.toHexString(sendBytes)} 仓：$boxCode, ${if (type == 1) "发送开仓指令" else "发送关仓指令"}")
        while (retryCount < maxRetryCount) { // 在未达到最大重试次数且 isCan 为 false 时继续重试
            retryCount++
            val rec = isOpenRecData.get()
            val sta = isOpenStatus.get()
//            Loge.i("串口232","接232 retryJobS 尝试第 $retryCount 次 $lockerId 仓 指令：${ByteUtils.toHexString(sendBytes)}, ${if (type == 1) "发起开仓" else "发起关仓"} 读取数据：$rec,状态：$sta")
            Loge.i("串口232", "接232 retryJobS 尝试第 $retryCount 次 $boxCode 仓 指令：${ByteUtils.toHexString(sendBytes)}, 发起开仓 读取数据：$rec,状态：$sta")
            if (rec && sta) {
                Loge.i("串口232", "接232 retryJobS 接收到数据 任务成功，停止重试")
                isOpenRecData.set(false)
                isOpenStatus.set(false)
                when (type) {
                    0 -> {
                        //响应关仓
                    }

                    1 -> {
                        //响应开仓
                    }

                    else -> {

                    }
                }
                retryCount = 0
                commandOpenResultListener?.openResult(boxCode, CommandStatus.SUCCEED)
                break
            }
            if (retryCount >= maxRetryCount) {
                Loge.i("串口232", "接232 retryJobS 达到最大重试次数，停止重试")
                isOpenRecData.set(false)
                isOpenStatus.set(false)
                when (type) {
                    0 -> {
                        //响应关仓
                    }

                    1 -> {
                        //响应开仓
                    }

                    else -> {

                    }
                }
                retryCount = 0
                commandOpenResultListener?.openResult(boxCode, CommandStatus.FAULT)
                break
            }
            startSend232Job(boxCode, type, sendBytes)
            delay(1000)
        }
    }

    /***
     * 启动发送消息协程任务
     */
    private fun startSend232Job(lockerId: Int, type: Int, sendBytes: ByteArray) {
        // 启动协程，处理发送消息的任务
        sending232Job = ioScope.launch {
            // 保持协程任务处于活动状态
            if (isActive) {
                fosSend232.value?.apply {
                    write(sendBytes)
                    commandSendResultListener?.sendResult("发送数据成功232：|${ByteUtils.toHexString(sendBytes)}|")
                } ?: run {
                    commandSendResultListener?.sendResult("发送数据失败232：串口未打开 |${ByteUtils.toHexString(sendBytes)}|")
                }
            } else {
                commandSendResultListener?.sendResult("发送数据失败232：协程出现问题... |${ByteUtils.toHexString(sendBytes)}|")
            }
        }
    }

    /***
     * 发送定心查询状态232指令
     * @param sendBytes
     */
    private fun startSendStatus232Job(sendBytes: ByteArray) {
        // 启动协程，处理发送消息的任务
        sendingStatus232Job = ioScope.launch {
            // 保持协程任务处于活动状态
            if (isActive) {
                fosSend232.value?.apply {
                    write(sendBytes)
                    commandSendResultListener?.sendResult("发送数据成功232：|${ByteUtils.toHexString(sendBytes)}|")
                } ?: run {
                    commandSendResultListener?.sendResult("发送数据失败232：串口未打开 |${ByteUtils.toHexString(sendBytes)}|")
                    //模拟串口返回的数据
//                    val result = openAssetsJson()
//                    cabinet2StatusListener?.lockerStatusArray(result, false)
                }
            } else {
                commandSendResultListener?.sendResult("发送数据失败232：协程出现问题... |${ByteUtils.toHexString(sendBytes)}|")
                //模拟串口返回的数据
//                val result = openAssetsJson()
//                cabinet2StatusListener?.lockerStatusArray(result, false)
            }
        }
    }

    /***
     * 启动发送消息协程任务
     */
    private fun startSend485Job(lockerId: Int, type: Int, sendBytes: ByteArray) {
        // 启动协程，处理发送消息的任务
        sending485Job = ioScope.launch {
            // 保持协程任务处于活动状态
            if (isActive) {
                fosSend485.value?.apply {
                    write(sendBytes)
                    commandSendResultListener?.sendResult("发送数据成功485：|${ByteUtils.toHexString(sendBytes)}|")
                } ?: run {
                    commandSendResultListener?.sendResult("发送数据失败485：串口未打开 |${ByteUtils.toHexString(sendBytes)}|")
                }
            } else {
                commandSendResultListener?.sendResult("发送数据失败485：协程出现问题... |${ByteUtils.toHexString(sendBytes)}|")
            }
        }
    }

    /***
     * 发送定心查询状态485指令
     * @param sendBytes
     */
    private fun startSendStatus485Job(sendBytes: ByteArray) {
        // 启动协程，处理发送消息的任务
        sendingStatus485Job = ioScope.launch {
            // 保持协程任务处于活动状态
            if (isActive) {
                fosSend485.value?.apply {
                    write(sendBytes)
                    commandSendResultListener?.sendResult("发送数据成功485：|${ByteUtils.toHexString(sendBytes)}|")
                } ?: run {
                    commandSendResultListener?.sendResult("发送数据失败485：串口未打开 |${ByteUtils.toHexString(sendBytes)}|")
                    commandQueryListYSDResultListener?.queryResult(sendBytes[2].toInt(), mutableMapOf())
                }
            } else {
                commandSendResultListener?.sendResult("发送数据失败485：协程出现问题... |${ByteUtils.toHexString(sendBytes)}|")
                commandQueryListYSDResultListener?.queryResult(sendBytes[2].toInt(), mutableMapOf())
            }
        }
    }

    /******************************************发送类型指令**************************************************************/

    /***********************************************************针对自定义协议V1.0***************************************************************************/
    // 定义帧头和帧尾
    private val frameHeader = SendByteData.RE_FRAME_HEADER
    private val frameTail = SendByteData.RE_FRAME_END

    // 定义帧头和帧尾
    private val frameHeader485 = SendByteData.EX_RE_FRAME_HEADER
    private val frameTail485 = SendByteData.EX_RE_FRAME_END

    // 用于缓存数据的缓存区
    private val buffer232 = mutableListOf<Byte>()
    private val buffer485 = mutableListOf<Byte>()

    // 定义帧头和帧尾
    private val frameEnd = SendByteData.RE_FRAME_END

    // 使用 ByteArray 作为缓冲区，提高处理效率
    private var buffer2322 = ByteArray(0)

    // 缓冲区最大大小，防止内存溢出（根据协议调整） 1MB
    private val MAX_BUFFER_SIZE = 1024 * 1024

    // 配置常量
    private val MAX_BUFFER_SIZE2 = 4096
    val FRAME_HEADER = 0x9B.toByte()
    val FRAME_END = 0x9A.toByte()

    // 缓冲区管理
    private val bufferNew232 = ByteArrayOutputStream(MAX_BUFFER_SIZE2)

    /***
     * 接收下位机发送的数据
     */
    private fun startRead232Job() {
        // 启动协程，处理接收消息的任务
        read232Job = ioScope.launch {
            // 缓存区大小可以根据需要调整
            val bufferRead = ByteArray(1024)
            try {
                // 保持协程任务处于活动状态
                while (isActive) {
//                    Loge.i("串口232", "接232 协程目前状态:isActive $isActive ")
                    fis232Read.value?.let { data ->
                        val bytesRead = data.read(bufferRead)
                        if (bytesRead > 0) {
                            //读取下位机数据
                            val receivedData = bufferRead.copyOf(bytesRead)
                            BoxToolLogUtils.receiveOriginalLower(232, receivedData)
//                            processReceivedData(bufferRead.copyOf(bytesRead))
                            Loge.i("串口232", "接232 大小：${buffer232.size} 积累：${ByteUtils.toHexString(buffer232.toByteArray())}")
                            Loge.i("串口232", "接232 大小：$bytesRead 原始：${ByteUtils.toHexString(receivedData)}")
                            //将下位机数据转换成list存储在缓存区
                            buffer232.addAll(receivedData.toList())
                            val frameStartIndex = buffer232.indexOf(frameHeader)
//                            val frameStartIndex = buffer232.lastIndexOf(frameHeader)
                            if (frameStartIndex == -1) {
                                // 如果没有找到帧头，退出循环
                                Loge.i("串口232", "接232 预备解析没有找到帧头")
                                return@let
                            }
                            // 查找帧尾
                            val frameEndIndex = findFrameEndIndex(frameStartIndex)
                            if (frameEndIndex == -1) {
                                // 如果没有找到帧尾，退出循环
                                Loge.i("串口232", "接232 预备解析没有找到帧尾")
                                return@let
                            }
                            Loge.i("串口232", "接232 帧头: $frameStartIndex || 帧尾: $frameEndIndex || 当前包: ${ByteUtils.toHexString(buffer232.toByteArray())}")
                            // 提取完整的数据包（包括帧头和帧尾）
                            val packet =
                                    buffer232.subList(frameStartIndex, frameEndIndex + 1).toByteArray()
                            Loge.i("串口232", "接232 预备解析完整数据包: ${ByteUtils.toHexString(packet)}")

//                            if (!validateChecksum(packet)) {
//                                Loge.i("串口232", "接 校验码验证失败 ${ByteUtils.toHexString(packet)}")
//                                buffer.subList(0, frameEndIndex + 1).clear()
//                                return@let
//                            }
                            // 处理解析出的数据包
                            handlePacket232(packet)

                            // 从缓冲区中移除已处理的数据
                            buffer232.subList(0, frameEndIndex + 1).clear()
                        }
                    }
                    delay(10)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /***
     * @param frameStartIndex 通过 IndexOf 函数获取帧的位置
     * 查找帧尾的位置，从给定的帧头位置之后开始查找
     */
    private fun findFrameEndIndex(frameStartIndex: Int): Int {
        for (i in frameStartIndex + 1 until buffer232.size) {
            if (buffer232[i] == frameTail) {
                return i  // 找到帧尾的位置
            }
        }
        return -1  // 如果没有找到帧尾，返回 -1
    }

    /***
     * 完整数据处理业务
     * @param packet 下位机原始数据
     */
    private fun handlePacket232(packet: ByteArray) {
        Loge.i("串口232", "接232 handlePacket232 处理数据 size ${packet.size} | ${ByteUtils.toHexString(packet)}")
        if (packet.size < 4) return
        //指令位置
        val seek = 2
        //数据长度位置
        val length = 3
        //提取指令
        val command = packet[seek]
        var dataLength = -1
        when (command) {
            0.toByte(), 1.toByte(), 2.toByte(), 3.toByte(), 4.toByte(), 5.toByte(), 7.toByte(), 8.toByte(), 9.toByte(), 10.toByte(), 11.toByte() -> {
                // 提取数据长度，并将其转换为无符号整数
                dataLength = packet[length].toUByte().toInt()  // 将有符号字节转换为无符号整数
            }

//            2.toByte() -> {
//                val highByte = packet[3]
//                val lowByte = packet[4]
//                Loge.i("串口232","接232 toByte highByte = $highByte lowByte = $lowByte ")
//                dataLength = (highByte.toInt() shl 8) or (lowByte.toInt() and 0xFF)
//
//            }
        }
        val before = packet.size - 1
        Loge.i("串口232", "接232 0.toByte 排除帧尾长度：$before 数据域长度：$dataLength")
        when (command) {
            //启动格口开关
            1.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 1.toByte 数据长度与数据域不匹配")
                    commandTurnResultListener?.openResult(-1, DoorStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 1.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                for (i in data.indices step 2) {
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 1.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    val locker = group[0].toInt()
                    val status = group[1].toInt()
                    if (status == 1) {
                        commandTurnResultListener?.openResult(locker, DoorStatus.SUCCEED)
                    } else {
                        commandTurnResultListener?.openResult(locker, DoorStatus.FAIL)
                    }
                }
            }
            //启动格口状态查询
            2.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 2.toByte 数据长度与数据域不匹配")
                    commandDoorResultListener?.openResult(DoorStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 2.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                for (i in data.indices step 2) {
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 2.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    val locker = group[0].toInt()
                    val status = group[1].toInt()
                    if (status == 1) {
                        commandDoorResultListener?.openResult(DoorStatus.SUCCEED)
                    } else {
                        commandDoorResultListener?.openResult(DoorStatus.FAIL)
                    }
                }
            }
            //清运门
            3.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 3.toByte 数据长度与数据域不匹配")
                    commandOpenResultListener?.openResult(-1, CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 3.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                isOpenRecData.set(true)
                for (i in data.indices step 2) {
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 2.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    val locker = group[0].toInt()
                    val status = group[1].toInt()
                    if (status == 1) {
                        isOpenStatus.set(true)
//                        commandOpenResultListener?.openResult(locker, CommandStatus.SUCCEED)
                    } else {
                        isOpenStatus.set(false)
//                        commandOpenResultListener?.openResult(locker, CommandStatus.FAIL)
                    }
                    Loge.i("串口232", "接232 3.toByte -----------------------------------------------------------")
                }

            }
            //查询重量
            4.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 4.toByte 数据长度与数据域不匹配")
                    commandWeightResultListener?.weightResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 4.toByte size = ${data.size} | ${HexConverter.byteArrayToInt(data)}")
                if (data.isNotEmpty()) {
                    for (i in data.indices step 4) {
                        val end = (i + 4).coerceAtMost(data.size)
                        val group = data.copyOfRange(i, end)
                        val size = group.size
                        Loge.i("串口232", "接232 4.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    }
                    val size = HexConverter.byteArrayToInt(data)
                    commandWeightResultListener?.weightResult(size)
                } else {
                    commandWeightResultListener?.weightResult(CommandStatus.FAIL)
                }
            }
            //查询当前设备状态
            5.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 5.toByte 数据长度与数据域不匹配")
                    commandQueryListResultListener?.queryResult(arrayListOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 5.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                val list = mutableListOf<PortDeviceInfo>()

                val tg1 = data.copyOfRange(0, 12)
                Loge.i("串口232", "接232 测试 1 ${ByteUtils.toHexString(tg1)}")
                val weight1 = tg1.copyOfRange(1,5)
                Loge.i("串口232", "接232 5.toByte 取1重量：${HexConverter.byteArrayToInt(weight1)}")
                val status1 = tg1.copyOfRange(5, 12)
                //烟雾传感器
                var smokeValue1 = 1
                //红外传感器
                var irStateValue1 = -1
                //关门传感器
                var touCGStatusValue1 = 0
                //防夹传感器
                var touJSStatusValue1 = 0
                //投口门状态
                var doorStatusValue1: Int = -1
                //清运门状态
                var lockStatusValue1: Int = -1
                //校准状态
                var xzStatusValue1: Int = -1
                for (i in status1.indices step 7) {
                    val end = (i + 7).coerceAtMost(status1.size)
                    val group = status1.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 5.toByte 取1数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    smokeValue1 = group[0].toUByte().toInt()
                    irStateValue1= group[1].toUByte().toInt()
                    touCGStatusValue1 = group[2].toUByte().toInt()
                    touJSStatusValue1 = group[3].toUByte().toInt()
                    doorStatusValue1 = group[4].toUByte().toInt()
                    lockStatusValue1 = group[5].toUByte().toInt()
                    xzStatusValue1 = group[6].toUByte().toInt()
                }
                val weighValue1 = HexConverter.byteArrayToInt(weight1)
                list.add(PortDeviceInfo().apply {
                    weigh = HexConverter.getWeight(weighValue1)
                    smoke = smokeValue1
                    irState = irStateValue1
                    touGMStatus = touCGStatusValue1
                    touJSStatus = touJSStatusValue1
                    doorStatus = doorStatusValue1
                    lockStatus = lockStatusValue1
                    xzStatus = xzStatusValue1
                })


//
                val tg2 = data.copyOfRange(12, 24)
                Loge.i("串口232", "接232 测试 2 ${ByteUtils.toHexString(tg2)}")
                val weight2 = tg2.copyOfRange(1, 5)
                Loge.i("串口232", "接232 5.toByte 取2重量：${HexConverter.byteArrayToInt(weight2)}")
                val status2 = tg2.copyOfRange(5, 12)
                Loge.i("串口232", "接232 测试 2 ${ByteUtils.toHexString(status2)}")
                //烟雾传感器
                var smokeValue2 = 1
                //红外传感器
                var irStateValue2 = -1
                //关门传感器
                var touCGStatusValue2 = 0
                //防夹传感器
                var touJSStatusValue2 = 0
                //投口门状态
                var doorStatusValue2: Int = -1
                //清运门状态
                var lockStatusValue2: Int = -1
                //校准状态
                var xzStatusValue2: Int = -1
                for (i in status2.indices step 7) {
                    val end = (i + 7).coerceAtMost(status2.size)
                    val group = status2.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 5.toByte 取2数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    smokeValue2 = group[0].toUByte().toInt()
                    irStateValue2= group[1].toUByte().toInt()
                    touCGStatusValue2 = group[2].toUByte().toInt()
                    touJSStatusValue2 = group[3].toUByte().toInt()
                    doorStatusValue2 = group[4].toUByte().toInt()
                    lockStatusValue2 = group[5].toUByte().toInt()
                    xzStatusValue2 = group[6].toUByte().toInt()
                }
                val weighValue2 = HexConverter.byteArrayToInt(weight2)
                list.add(PortDeviceInfo().apply {
                    weigh = HexConverter.getWeight(weighValue2)
                    smoke = smokeValue2
                    irState = irStateValue2
                    touGMStatus = touCGStatusValue2
                    touJSStatus = touJSStatusValue2
                    doorStatus = doorStatusValue2
                    lockStatus = lockStatusValue2
                    xzStatus = xzStatusValue2
                })
                commandQueryListResultListener?.queryResult(list)

            }
            //灯光控制
            6.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 6.toByte 数据长度与数据域不匹配")
                    commandLightsResultListener?.lightsResult(-1, DoorStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 6.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                for (i in data.indices step 2) {
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 6.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    val locker = group[0].toInt()
                    val status = group[1].toInt()
                    if (status == 1) {
                        commandLightsResultListener?.lightsResult(locker, DoorStatus.SUCCEED)
                    } else {
                        commandLightsResultListener?.lightsResult(locker, DoorStatus.FAIL)
                    }
                }
            }

            //进入升级状态 查询状态 升级完成重启
            7.toByte(), 8.toByte(), 10.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 78910.toByte 数据长度与数据域不匹配")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 78910.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                if (data.size == 3) {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
            //查询升级校验结果
            9.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 78910.toByte 数据长度与数据域不匹配")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                var a = ""
                var b = ""
                var c = ""
                for (i in data.indices step 3) {
                    val end = (i + 3).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 9.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    if (size > 0) {
                        a = group[0].toUByte().toString()
                    }
                    if (size > 1) {
                        b = group[1].toUByte().toString()
                    }
                    if (size > 2) {
                        c = group[2].toUByte().toString()
                    }
                }
                Loge.i("串口232", "接232 9.toByte ----------${a}-${b}-${c}-----------------------------------------------")
                if (a == "164" && b == "165" && c == "166") {
                    Loge.i("串口232", "接232 9.toByte 升级完成 ${CommandStatus.SUCCEED}")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else if (a == "180" && b == "181" && c == "182") {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                } else {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
            //版本查询
            11.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 11.toByte 数据长度与数据域不匹配")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口232", "接232 11.toByte size = ${data.size} | ${HexConverter.byteArrayToInt(data)}")
                if (data.isNotEmpty()) {
                    var a = ""
                    var b = ""
                    var c = ""
                    var d = ""
                    for (i in data.indices step 4) {
                        val end = (i + 4).coerceAtMost(data.size)
                        val group = data.copyOfRange(i, end)
                        val size = group.size
                        Loge.i("串口232", "接232 11.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                        if (size > 0) {
                            a = group[0].toUByte().toString()
                        }
                        if (size > 1) {
                            b = group[1].toUByte().toString()
                        }
                        if (size > 2) {
                            c = group[2].toUByte().toString()
                        }
                        if (size > 3) {
                            d = group[3].toUByte().toString()
                        }
                    }
                    Loge.i("串口232", "接232 11.toByte ----------${a}-${b}-${c}-${d}----------------------------------------------")
                    if (a == "255" && b == "255" && c == "255" && d == "255") {
                        commandUpgrade232ResultListener?.upgradeResult(20250101)
                    } else {
                        val size = HexConverter.byteArrayToInt(data)
                        commandUpgrade232ResultListener?.upgradeResult(size)
                    }
                } else {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
            //发送256字节文件回复
            0.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口232", "接232 0.toByte 数据长度与数据域不匹配")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                var a = ""
                var b = ""
                var c = ""
                for (i in data.indices step 3) {
                    val end = (i + 3).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口232", "接232 0.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    if (size > 0) {
                        a = group[0].toUByte().toString()
                    }
                    if (size > 1) {
                        b = group[1].toUByte().toString()
                    }
                    if (size > 2) {
                        c = group[2].toUByte().toString()
                    }
                }
                Loge.i("串口232", "接232 0.toByte ----------${a}-${b}-${c}-----------------------------------------------")
                if (a == "1" && b == "2" && c == "3") {
                    Loge.i("串口232", "接232 0.toByte 升级完成 ${CommandStatus.SUCCEED}")
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else {
                    commandUpgrade232ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }

            //去零清皮
            16.toByte() -> {}
            //校准零点
            17.toByte() -> {}
            //校准2KG
            18.toByte() -> {}
            //校准25KG
            19.toByte() -> {}
            //校准60KG
            20.toByte() -> {}
        }
    }

    /***************************************************数据处理新方式***********************************************/

    private fun startRead232JobNew() {
        // 启动协程，处理接收消息的任务
        read232Job = ioScope.launch {
            // 缓存区大小可以根据需要调整
            val bufferRead = ByteArray(1024)
            try {
                // 保持协程任务处于活动状态
                while (isActive) {
                    Loge.i("串口232", "接232 协程目前状态:isActive $isActive ")
                    fis232Read.value?.let { data ->
                        val bytesRead = data.read(bufferRead)
                        Loge.i("串口232", "接232 下位机原始数据包大小: $bytesRead ")
                        if (bytesRead > 0) {
                            //读取下位机数据
                            val receivedData = bufferRead.copyOf(bytesRead)
                            BoxToolLogUtils.receiveOriginalLower(232, receivedData)
                            processReceivedData(bufferRead.copyOf(bytesRead))
                        }
                    }
                    delay(10)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /***
     * @param newData 接收的数据域
     */
    private fun processReceivedData(newData: ByteArray) {
        BoxToolLogUtils.receiveOriginalLower(232, newData)
        Loge.i("串口232", "接232 大小：${newData.size} 原始：${ByteUtils.toHexString(newData)}")
        // 1. 追加新数据到缓冲区
        bufferNew232.write(newData)
        val currentBuffer = bufferNew232.toByteArray()

        // 2. 处理缓冲区中的数据
        var processedBytes = 0
        var currentIndex = 0
        Loge.i("串口232", "接232 测试新的方式 currentIndex = $currentIndex | ${currentBuffer.size}")
        while (currentIndex < currentBuffer.size) {
            // 3. 查找帧头 (0x9B)
            val headerIndex = findFrameHeader(currentBuffer, currentIndex)
            Loge.i("串口232", "接232 测试新的方式 headerIndex $headerIndex")
            if (headerIndex == -1) break

            // 4. 检查是否有足够的数据获取长度字段 (header + 3)
            if (headerIndex + 3 >= currentBuffer.size) {
                processedBytes = headerIndex
                Loge.i("串口232", "接232 测试新的方式 检查是否有足够的数据获取长度字段")
                break
            }

            // 5. 获取数据长度 (第4个字节)
            val dataLength = currentBuffer[headerIndex + 3].toInt() and 0xFF

            // 6. 计算完整包长度
            val totalLength = 5 + dataLength  // 帧头1 + 地址1 + 命令1 + 长度1 + 数据N + 帧尾1

            // 7. 检查完整数据包
            if (headerIndex + totalLength > currentBuffer.size) {
                processedBytes = headerIndex
                Loge.i("串口232", "接232 测试新的方式 检查完整数据包")
                break
            }

            // 8. 检查帧尾 (0x9A)
            val frameEndIndex = headerIndex + totalLength - 1
            if (currentBuffer[frameEndIndex] != FRAME_END) {
                currentIndex = headerIndex + 1
                Loge.i("串口232", "接232 测试新的方式 检查帧尾 (0x9A)")
                continue
            }

            // 9. 提取完整数据包
            val packet = currentBuffer.copyOfRange(headerIndex, headerIndex + totalLength)
            Loge.i("串口232", "接232 测试新的方式 packet ：${ByteUtils.toHexString(packet)}")
            // 10. 校验和验证 (示例)
//            if (!validateCheckCode(packet)) {
//                currentIndex = frameEndIndex + 1
//                continue
//            }

            // 11. 处理有效数据包
            handlePacket232(packet)

            // 12. 移动处理位置
            currentIndex = frameEndIndex + 1
            processedBytes = currentIndex
        }

        // 13. 保存未处理数据
        bufferNew232.reset()
        if (currentBuffer.size > processedBytes) {
            Loge.i("串口232", "接232 测试新的方式 保存未处理数据 processedBytes = $processedBytes")
            bufferNew232.write(currentBuffer, processedBytes, currentBuffer.size - processedBytes)
        }
    }

    /***
     * @param buffer 完整数据域
     * @param startIndex
     * 查找帧头
     */
    private fun findFrameHeader(buffer: ByteArray, startIndex: Int): Int {
        for (i in startIndex until buffer.size) {
            if (buffer[i] == FRAME_HEADER) return i
        }
        return -1
    }

    /***
     * @param packet 完整数据域
     * 验证校验码 即是末尾前一位
     */
    private fun validateCheckCode(packet: ByteArray): Boolean {
        // 简单示例：校验和是除帧头帧尾外所有字节的和
        if (packet.size < 3) return false

        var checksum = 0
        for (i in 1 until packet.size - 2) { // 跳过帧头和帧尾前的2字节（假设校验和是倒数第二字节）
            checksum += packet[i].toInt() and 0xFF
        }

        val expectedChecksum = packet[packet.size - 2].toInt() and 0xFF
        return (checksum and 0xFF) == expectedChecksum
    }
    /***************************************************数据处理新方式***********************************************/

    /***************************************************核心优化：循环处理缓冲区中的所有完整数据包***********************************************/

    private fun startRead232JobNew2() {
        // 启动协程，处理接收消息的任务
        read232Job = ioScope.launch {
            // 缓存区大小可以根据需要调整
            val bufferRead = ByteArray(1024)
            try {
                // 保持协程任务处于活动状态
                while (isActive) {
                    Loge.i("串口232", "接232 协程目前状态:isActive $isActive ")
                    fis232Read.value?.let { data ->
                        val bytesRead = data.read(bufferRead)
                        Loge.i("串口232", "接232 下位机原始数据包大小: $bytesRead ")
                        if (bytesRead > 0) {
                            //读取下位机数据
                            val receivedData = bufferRead.copyOf(bytesRead)
                            BoxToolLogUtils.receiveOriginalLower(232, receivedData)
                            Loge.i("串口232", "接232 大小：${buffer232.size} 积累：${ByteUtils.toHexString(buffer232.toByteArray())}")
                            Loge.i("串口232", "接232 大小：$bytesRead 原始：${ByteUtils.toHexString(receivedData)}")
                            //将下位机数据转换成list存储在缓存区
                            buffer232.addAll(receivedData.toList())
                            processBuffer()
                        }
                    }
                    delay(10)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun processBuffer() {
        var currentPosition = 0

        while (true) {
            // 1. 查找帧头（从当前位置开始）
            val frameStart = buffer2322.indexOf(frameHeader, currentPosition)
            if (frameStart == -1) break // 没有更多帧头

            // 2. 查找帧尾（必须位于帧头之后）
            val frameEndIndex = buffer2322.indexOf(frameEnd, frameStart + 1)
            if (frameEndIndex == -1) break // 当前帧不完整，等待更多数据

            // 3. 提取数据包（包含头尾）
            val packet = buffer2322.copyOfRange(frameStart, frameEndIndex + 1)
            Loge.i("串口232", "接232 解析到完整包: ${packet}")

            // 4. 校验数据包（可选，根据协议实现）
            // if (!validateChecksum(packet)) {
            //     Loge.i("串口232","校验失败，丢弃包: ${packet.toHexString()}")
            //     currentPosition = frameEndIndex + 1
            //     continue
            // }

            // 5. 处理有效数据包
            handlePacket232(packet)

            // 6. 移动指针到当前帧尾之后，继续查找下一帧
            currentPosition = frameEndIndex + 1
        }

        // 7. 清理已处理的数据（保留未处理部分）
        buffer2322 = if (currentPosition > 0) {
            buffer2322.copyOfRange(currentPosition, buffer2322.size)
        } else {
            buffer2322
        }
    }

    // 自定义带起始位置的 indexOf 方法
    private fun ByteArray.indexOf(byte: Byte, fromIndex: Int = 0): Int {
        for (i in fromIndex.coerceAtLeast(0) until this.size) {
            if (this[i] == byte) return i
        }
        return -1
    }
    /***************************************************核心优化：循环处理缓冲区中的所有完整数据包***********************************************/

    /***
     *  启动接收485消息协程任务
     */
    private fun startRead485Job() {
        // 启动协程，处理接收消息的任务
        read485Job = ioScope.launch {
            // 缓存区大小可以根据需要调整
            val bufferRead = ByteArray(1024)
            try {
                // 保持协程任务处于活动状态
                while (isActive) {
                    Loge.i("串口485", "接485 isActive $isActive")
                    fisRead485.value?.let { data ->
                        val bytesRead = data.read(bufferRead)
                        if (bytesRead > 0) {
                            //读取下位机数据
                            val receivedData = bufferRead.copyOf(bytesRead)
                            BoxToolLogUtils.receiveOriginalLower(485, receivedData)
                            Loge.i("串口485", "接485 大小：${buffer485.size} 积累：${ByteUtils.toHexString(buffer485.toByteArray())}")
                            Loge.i("串口485", "接485 大小：$bytesRead 原始：${ByteUtils.toHexString(receivedData)}")
                            //将下位机数据转换成list存储在缓存区
                            buffer485.addAll(receivedData.toList())
                            val frameStartIndex = buffer485.indexOf(frameHeader485)
//                            val frameStartIndex = buffer485.lastIndexOf(frameHeader485)
                            if (frameStartIndex == -1) {
                                // 如果没有找到帧头，退出循环
                                Loge.i("串口485", "接485 预备解析没有找到帧头")
                                return@let
                            }
                            // 查找帧尾
                            val frameEndIndex = findFrameEndIndex485(frameStartIndex)
                            if (frameEndIndex == -1) {
                                // 如果没有找到帧尾，退出循环
                                Loge.i("串口485", "接485 预备解析没有找到帧尾")
                                return@let
                            }
                            Loge.i("串口485", "接485 帧头: $frameStartIndex || 帧尾: $frameEndIndex || 当前包: ${ByteUtils.toHexString(buffer485.toByteArray())}")
                            // 提取完整的数据包（包括帧头和帧尾）
                            val packet =
                                    buffer485.subList(frameStartIndex, frameEndIndex + 1).toByteArray()
                            Loge.i("串口485", "接485 预备解析完整数据包: ${ByteUtils.toHexString(packet)}")

//                            if (!validateChecksum(packet)) {
//                                Loge.i("串口485", "接485 校验码验证失败 ${ByteUtils.toHexString(packet)}")
//                                buffer485.subList(0, frameEndIndex + 1).clear()
//                                return@let
//                            }
                            // 处理解析出的数据包
                            handlePacket485(packet)
                            // 从缓冲区中移除已处理的数据
                            buffer485.subList(0, frameEndIndex + 1).clear()
                        } else {
                            commandQueryListYSDResultListener?.queryResult(commandType485, mutableMapOf())
                        }
                    } ?: run {
                        commandQueryListYSDResultListener?.queryResult(commandType485, mutableMapOf())
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /***
     * 效验验证码
     * @param byteArray 下位机原始数据
     */
    private fun validateChecksum(byteArray: ByteArray): Boolean {
        // 校验码计算规则：帧头 + 地址 + 指令 + 长度 + 数据域
        // 校验码在倒数第二个字节（6D）
        val data = byteArray.copyOfRange(0, byteArray.size - 2)
        // 计算从帧头到数据域的总和，排除掉最后的帧尾字节（6A）// 不包括帧尾
        val checksumCalculated = data.sumOf { it.toInt() and 0xFF } % 256  // 确保字节按无符号处理
        val checksumExpected = byteArray[byteArray.size - 2].toInt() and 0xFF  // 校验码在倒数第二个字节
        Loge.i("串口", "接 checksumCalculated = $checksumCalculated | checksumExpected = $checksumExpected")
        return checksumCalculated == checksumExpected
    }

    /***
     * @param frameStartIndex 通过 IndexOf 函数获取帧的位置
     * 查找帧尾的位置，从给定的帧头位置之后开始查找
     */
    private fun findFrameEndIndex485(frameStartIndex: Int): Int {
        for (i in frameStartIndex + 1 until buffer485.size) {
            if (buffer485[i] == frameTail485) {
                return i  // 找到帧尾的位置
            }
        }
        return -1  // 如果没有找到帧尾，返回 -1
    }

    /***
     * 完整数据处理业务
     * @param packet 下位机原始数据
     */
    private fun handlePacket485(packet: ByteArray) {
        Loge.i("串口485", "接485 handlePacket485 ${packet.size}")
        if (packet.size < 4) return
        //指令位置
        val seek = 2
        //数据长度位置
        val length = 3
        //提取指令
        val command = packet[seek]
        // 提取数据长度，并将其转换为无符号整数
        val dataLength = packet[length].toUByte().toInt()  // 将有符号字节转换为无符号整数
        //剔除后面两位后续需要匹配数据域+前面四位
        val before = packet.size - 2
        //没有效验的处理
//        val before = packet.size - 1
        Loge.i("串口485", "接485 0.toByte 排除帧尾长度：$before 数据域长度：$dataLength")
        when (command) {
            //当前设备电量 工具电量
            1.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 1.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(1, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 1.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 2) {
                    val tool = mutableMapOf<Int, BoxInternal>()
                    var address = -1
                    var elect = 0
                    // 防止最后一组数据不足4字节
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 1.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //地址
                    if (size > 0) {
                        address = group[0].toUByte().toInt()
                    }
                    //电量
                    if (size > 1) {
                        val e = group.copyOfRange(1, minOf(group.size, 2))
                        elect = e.joinToString(separator = " ") { it.toInt().toString() }.toInt()

                    }
                    tool[address] = BoxInternal().apply {
                        boxAddress = address.toString()
                        boxElectric = elect
//                        boxElectric = if (address == 1) 100 else elect
                    }
                    boxMap[address] = tool
                    Loge.i("串口485", "接485 1.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(1, boxMap)
            }

            //工具是否在仓
            2.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 2.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(2, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 2.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 2) {
                    val tool = mutableMapOf<Int, BoxInternal>()
                    var address = -1
                    var status = -1
                    // 防止最后一组数据不足4字节
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 2.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //地址
                    if (size > 0) {
                        address = group[0].toUByte().toInt()
                    }
                    //是否在仓
                    if (size > 1) {
                        status = group[1].toInt()
                    }
                    tool[address] = BoxInternal().apply {
                        boxAddress = address.toString()
                        boxIn = EnumToolsStatus.getDescByCode(status)
                    }
                    //取出响应回来的地址存储
                    boxMap[address] = tool
                    Loge.i("串口485", "接485 2.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(2, boxMap)
            }

            //当前设备故障 工具故障
            3.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 3.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(3, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 3.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 2) {
                    val tool = mutableMapOf<Int, BoxInternal>()
                    var address = -1
                    var status = -1
                    // 防止最后一组数据不足4字节
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 3.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //地址
                    if (size > 0) {
                        address = group[0].toUByte().toInt()
                    }
                    //状态
                    if (size > 1) {
                        status = group[1].toInt()
                    }
                    tool[address] = BoxInternal().apply {
                        boxAddress = address.toString()
                        boxSignal = EnumTxStatus.getDescByCode(status)
                    }
                    boxMap[address] = tool
                    Loge.i("串口485", "接485 3.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(3, boxMap)
            }

            //当前设备sn
            4.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 4.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(4, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 4.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 8) {
                    val end = (i + 8).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 4.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //sn
                    val snBytes = group.copyOfRange(0, minOf(group.size, 0 + 8))
                    val snBytesValue = snBytes.map { byte ->
                        if (byte == 0x00.toByte()) 0x30.toByte() else byte
                    }.toByteArray()
                    // 将每个字节转换为十进制整数并拼接成字符串
                    val sn = String(snBytesValue, Charsets.US_ASCII)

                    val tool = mutableMapOf<Int, BoxInternal>()
                    tool[1] = BoxInternal().apply {
                        boxSn = "GJX00011"
                    }
                    boxMap[1] = tool
                    Loge.i("串口485", "接485 4.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(4, boxMap)
            }

            //剥线器电量
            5.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 5.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(1, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 5.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 2) {
                    val tool = mutableMapOf<Int, BoxInternal>()
                    var address = -1
                    var elect = 0
                    // 防止最后一组数据不足4字节
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 5.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //地址
                    if (size > 0) {
                        address = group[0].toUByte().toInt()
                    }
                    //电量
                    if (size > 1) {
                        val e = group.copyOfRange(1, minOf(group.size, 2))
                        elect = e.joinToString(separator = " ") { it.toInt().toString() }.toInt()

                    }
                    tool[address] = BoxInternal().apply {
                        boxAddress = address.toString()
                        boxElectric = elect
                    }
                    boxMap[address] = tool
                    Loge.i("串口485", "接485 5.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(5, boxMap)
            }

            //剥线器故障
            6.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 6.toByte 数据长度与数据域不匹配")
                    commandQueryListYSDResultListener?.queryResult(1, mutableMapOf())
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 6.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                //定义箱子
                val boxMap = mutableMapOf<Int, MutableMap<Int, BoxInternal>>()
                for (i in data.indices step 2) {
                    val tool = mutableMapOf<Int, BoxInternal>()
                    var address = -1
                    var status = -1
                    // 防止最后一组数据不足4字节
                    val end = (i + 2).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 6.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    //地址
                    if (size > 0) {
                        address = group[0].toUByte().toInt()
                    }
                    //状态
                    if (size > 1) {
                        status = group[1].toInt()
                    }

                    tool[address] = BoxInternal().apply {
                        boxAddress = address.toString()
                        boxSignal = EnumTxStatus.getDescByCode(status)
                    }
                    boxMap[address] = tool
                    Loge.i("串口485", "接485 6.toByte -----------------------------------------------------------")
                }
                commandQueryListYSDResultListener?.queryResult(6, boxMap)
            }

            //查询升级校验结果
            9.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 78910.toByte 数据长度与数据域不匹配")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                var a = ""
                var b = ""
                var c = ""
                for (i in data.indices step 3) {
                    val end = (i + 3).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 9.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    if (size > 0) {
                        a = group[0].toUByte().toString()
                    }
                    if (size > 1) {
                        b = group[1].toUByte().toString()
                    }
                    if (size > 2) {
                        c = group[2].toUByte().toString()
                    }
                }
                Loge.i("串口485", "接485 9.toByte ----------${a}-${b}-${c}-----------------------------------------------")
                if (a == "164" && b == "165" && c == "166") {
                    Loge.i("串口485", "接485 9.toByte 升级完成 ${CommandStatus.SUCCEED}")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else if (a == "180" && b == "181" && c == "182") {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                } else {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }

            //进入升级状态 查询状态 升级完成重启
            7.toByte(), 8.toByte(), 10.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 78910.toByte 数据长度与数据域不匹配")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 78910.toByte 取数据源：${data.joinToString(" ") { "%02X".format(it) }}")
                isUpgradeRecData.set(true)
                if (data.size == 3) {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
            //版本查询
            11.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 11.toByte 数据长度与数据域不匹配")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                Loge.i("串口485", "接485 11.toByte size = ${data.size} | ${HexConverter.byteArrayToInt(data)}")
                if (data.isNotEmpty()) {
                    var a = ""
                    var b = ""
                    var c = ""
                    var d = ""
                    for (i in data.indices step 4) {
                        val end = (i + 4).coerceAtMost(data.size)
                        val group = data.copyOfRange(i, end)
                        val size = group.size
                        Loge.i("串口485", "接485 11.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                        if (size > 0) {
                            a = group[0].toUByte().toString()
                        }
                        if (size > 1) {
                            b = group[1].toUByte().toString()
                        }
                        if (size > 2) {
                            c = group[2].toUByte().toString()
                        }
                        if (size > 3) {
                            d = group[3].toUByte().toString()
                        }
                    }
                    if (a == "255" && b == "255" && c == "255" && d == "255") {
                        Loge.i("串口485", "接485 11.toByte ----------${a}-${b}-${c}-${d}----------------------------------------------")
                        commandUpgrade485ResultListener?.upgradeResult(20250101)
                    } else {
                        val size = HexConverter.byteArrayToInt(data)
                        commandUpgrade485ResultListener?.upgradeResult(size)
                    }
                } else {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
            //发送256字节文件回复
            0.toByte() -> {
                //取出完整数据
                val toIndex = 4 + dataLength
                if (before != toIndex) {
                    Loge.i("串口485", "接485 0.toByte 数据长度与数据域不匹配")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                    return
                }
                val data = packet.copyOfRange(4, 4 + dataLength)
                var a = ""
                var b = ""
                var c = ""
                for (i in data.indices step 3) {
                    val end = (i + 3).coerceAtMost(data.size)
                    val group = data.copyOfRange(i, end)
                    val size = group.size
                    Loge.i("串口485", "接485 0.toByte 数据拆分：i = $i end $end | size $size | group ${ByteUtils.toHexString(group)}")
                    if (size > 0) {
                        a = group[0].toUByte().toString()
                    }
                    if (size > 1) {
                        b = group[1].toUByte().toString()
                    }
                    if (size > 2) {
                        c = group[2].toUByte().toString()
                    }
                }
                Loge.i("串口485", "接485 0.toByte ----------${a}-${b}-${c}-----------------------------------------------")
                if (a == "1" && b == "2" && c == "3") {
                    Loge.i("串口485", "接485 0.toByte 升级完成 ${CommandStatus.SUCCEED}")
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.SUCCEED)
                } else {
                    commandUpgrade485ResultListener?.upgradeResult(CommandStatus.FAIL)
                }
            }
        }

    }

    /***********************************************************针对自定义协议V1.0***************************************************************************/

    /***********************************************************资源释放***************************************************************************/

    /***
     * 释放资源
     */
    fun closeAllSerialPort() {
        close232SerialPort()
        close485SerialPort()
    }

    /***
     * 释放232资源
     */
    fun close232SerialPort() {

        fd232.value = null
        fis232.value?.close()
        fis232.value = null
        fos232.value?.close()
        fos232.value = null

        stopSend232Job()
        stopRead232Job()

    }

    /***
     * 释放485资源
     */
    fun close485SerialPort() {

        fd485.value = null
        fis485.value?.close()
        fis485.value = null
        fos485.value?.close()
        fos485.value = null

        stopSend485Job()
        stopRead485Job()

    }

    /**
     * 停止发送消息的协程任务
     */
    private fun stopSend232Job() {
        // 取消发送任务
        sending232Job?.cancel()
        sendingStatus232Job?.cancel()
        sending232Job = null
        sendingStatus232Job = null
    }

    /**
     * 停止接收消息的协程任务
     */
    private fun stopRead232Job() {
        // 取消接收任务
        read232Job?.cancel()
        read232Job = null
    }

    /**
     * 停止发送消息的协程任务
     */
    private fun stopSend485Job() {
        sendingStatus485Job?.cancel()
        sending485Job?.cancel()
        sendingStatus485Job = null
        sending485Job = null
    }

    /**
     * 停止接收消息的协程任务
     */
    private fun stopRead485Job() {
        // 取消接收任务
        read485Job?.cancel()
        read485Job = null
    }

    /***********************************************************资源释放***************************************************************************/

}