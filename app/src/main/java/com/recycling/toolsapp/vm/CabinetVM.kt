package com.recycling.toolsapp.vm

import android.graphics.Bitmap
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.recycling.toolsapp.db.DatabaseManager
import com.recycling.toolsapp.http.HttpUrl
import com.recycling.toolsapp.http.RepoImpl
import com.recycling.toolsapp.http.VersionDto
import com.recycling.toolsapp.model.CabinEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.TransEntity
import com.recycling.toolsapp.socket.CabinBox
import com.recycling.toolsapp.socket.DoorCloseDto
import com.recycling.toolsapp.socket.DoorOpenDto
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginConfig
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.HexConverter
import com.serial.port.CabinetSdk
import com.serial.port.PortDeviceInfo
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.CRC32MPEG2Util
import com.serial.port.utils.Loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nearby.lib.netwrok.download.SingleDownloader
import nearby.lib.netwrok.response.CorHttp
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel class CabinetVM @Inject constructor() : ViewModel() {

    private val httpRepo by lazy { RepoImpl() }

    /**
     * dbIoJob
     */
    private var dbIoJob: Job? = null

    /**
     * 用于处理 I/O 操作的协程作用域
     */
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 用于处理 main 操作的协程作用域
     */
    val mainScope = MainScope()

    /***
     * 创建一个Channel，类型为Int，表示命令类型 232串口
     */
    private val commandQueue = Channel<Int>()

    /***
     * 创建一个Channel，类型为Int，表示命令类型 485串口
     */
    private val commandQueue485 = Channel<Int>()

    private val sendFileByte232 = Channel<ByteArray>()

    private val sendFileByte485 = Channel<ByteArray>()

    private val faceQueue = Channel<Bitmap>()

    private val markQueue = Channel<Boolean>()

    /***
     * 创建一个Channel，类型为Int，表示命令类型
     */
    private val masterChipQueue = Channel<Int>()

    /***
     * 创建一个Channel，类型为Int，表示命令类型
     */
    private val fromChipQueue = Channel<Int>()

    /***
     * 创建一个Channel，类型为Int，表示命令类型
     */
    private val queue485 = Channel<Int>()

    /***
     * 用于处理 默认协程作用域
     */
    val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    //处理网络提示语
    private val flowIsNetworkMessage = MutableSharedFlow<String>(replay = 1)
    val _isNetworkMessage = flowIsNetworkMessage.asSharedFlow()

    /***
     * 提示语
     */
    fun tipMessage(msg: String) {
        ioScope.launch {
            flowIsNetworkMessage.emit("${msg}")
        }
    }

    /******************************************* socket通信 *************************************************/
    var vmClient: SocketClient? = null

    fun initSocket() {
        println("调试socket cabinetVM initSocket")
        ioScope.launch {
            delay(5000)
            println("调试socket cabinetVM initSocket start")
            vmClient =
                    SocketClient(SocketClient.Config(host = "58.251.251.79", port = 9095, heartbeatIntervalMillis = 10_000, heartbeatPayload = "PING".toByteArray()))
            println("调试socket cabinetVM initSocket client $vmClient")
            vmClient?.incoming?.collect { bytes ->
                println("调试socket recv: ${String(bytes)}")
                val json = String(bytes)
                val cmd = CommandParser.parseCommand(json)
                when (cmd) {
                    "heartBeat" -> {
                        println("调试socket recv: 接收心跳成功")
                        //记录日志
                    }

                    "login" -> {
                        println("调试socket recv: 接收登录成功")
                        val loginModel = Gson().fromJson(json, LoginDto::class.java)

                        val heartbeatIntervalMillis =
                                loginModel.config.heartBeatInterval?.toLong() ?: 3
                        vmClient?.config?.heartbeatIntervalMillis1 =
                                TimeUnit.SECONDS.toMillis(heartbeatIntervalMillis)
                        //保存配置
                        loginModel.sn?.let { sn ->
                            toGetSaveConfigEntity(sn, loginModel.config)
                        }
                        //保存箱体
                        toGetSaveCabins(loginModel.config.list)
                        vmClient?.sendHeartbeat()
                    }

                    "initConfig" -> {
                        val initConfigModel = Gson().fromJson(json, InitConfigDto::class.java)
                        println("调试socket recv: 接收 initConfig 成功")
                    }

                    "openDoor" -> {
                        println("调试socket recv: 接收 openDoor 成功")
                        val doorOpenModel = Gson().fromJson(json, DoorOpenDto::class.java)
                        toGoDownDoorOpen(doorOpenModel)
                    }

                    "closeDoor" -> {
                        println("调试socket recv: 接收 closeDoor成功")
                    }

                    "phoneNumberLogin" -> {
                        println("调试socket recv: 接收 phoneNumberLogin 成功")
                    }

                    "phoneUserOpenDoor" -> {
                        println("调试socket recv: 接收 phoneUserOpenDoor 成功")
                    }

                    "restart" -> {
                        println("调试socket recv: 接收 restart 成功")
                    }

                    "uploadLog" -> {
                        println("调试socket recv: 接收 uploadLog 成功")
                    }

                    "ota" -> {
                        println("调试socket recv: 接收 OTA 成功")
                    }
                }
            }
            //启动socket连接
            vmClient?.start()
            println("调试socket client = $vmClient | state = ${vmClient?.state}")
            vmClient?.state?.collect {
                println("调试socket 连接成功1: $it ${Thread.currentThread().name}")
                when (it) {
                    ConnectionState.START -> {
                        println("调试socket START 开始")
                    }

                    ConnectionState.DISCONNECTED -> {
                        println("调试socket DISCONNECTED 已断开连接")
                    }

                    ConnectionState.CONNECTING -> {
                        println("调试socket CONNECTING 正在连接")
                    }

                    ConnectionState.CONNECTED -> {
                        println("调试socket CONNECTED 已连接")
                        toGoCmdLogin()
                    }
                }
            }
        }
    }

    fun convertToJsonString(obj: Any): String {
        return Gson().toJson(obj)
    }

    fun toGoCmdLogin() {
        ioScope.launch {
            val m =
                    mapOf("cmd" to "login", "sn" to "0136004ST00041", "imei" to "868408061812125", "iccid" to "898604A70821C0049781", "version" to "1.0.0", "timestamp" to System.currentTimeMillis())
            val json = convertToJsonString(m)
            vmClient?.sendText(json)
        }
    }

    /***
     * 打开舱门
     */
    fun toGoDownDoorOpen(model: DoorOpenDto) {
        val trensEntity = TransEntity().apply {
            transId = model.transId
            openType = model.openType
            cabinId = model.cabinId
            upStatus = 0
            time = AppUtils.getDateYMDHMS()
        }
        DatabaseManager.insertTrans(AppUtils.getContext(), trensEntity)
        //构建上发数据
        val doorOpen = DoorOpenDto().apply {
            cmd = "openDoor"
            transId = "closeDoor"
            cabinId = "closeDoor"
            phoneNumber = "closeDoor"
            curWeight = 0.0f
            retCode = 0
            timestamp = AppUtils.getDateYMDHMS()
        }

    }

    /***
     * 关闭舱门
     */
    fun toGoUpDoorClose() {
        //构建上发数据
        val doorClose = DoorCloseDto().apply {
            cmd = "closeDoor"
            transId = "closeDoor"
            cabinId = "closeDoor"
            phoneNumber = "closeDoor"
            curWeight = "closeDoor"
            changeWeight = "closeDoor"
            refWeight = "closeDoor"
            beforeUpWeight = "closeDoor"
            afterUpWeight = "closeDoor"
            beforeDownWeight = "closeDoor"
            afterDownWeight = "closeDoor"
            timestamp = AppUtils.getDateYMDHMS()
        }


    }

    /***
     * @param config 配置信息
     * 保存配置信息
     */
    fun toGetSaveConfigEntity(snCode: String, config: LoginConfig): Long {
        println("回收柜 开始添加配置")
        var row = -1L
        val saveConfig = ConfigEntity().apply {
            sn = snCode
            heartBeatInterval = config.heartBeatInterval
            turnOnLight = config.turnOnLight
            turnOffLight = config.turnOffLight
            lightTime = config.lightTime
            uploadPhotoURL = config.uploadPhotoURL
            uploadLogURL = config.uploadLogURL
            qrCode = config.qrCode
            logLevel = config.logLevel
            status = config.status
            debugPasswd = config.debugPasswd
            irDefaultState = config.irDefaultState
            weightSensorMode = config.weightSensorMode
        }
        val queryConfig = DatabaseManager.queryInitConfig(AppUtils.getContext(), snCode)
        if (queryConfig == null) {
            row = DatabaseManager.insertInitConfig(AppUtils.getContext(), saveConfig)
            println("回收柜 添加配置")
        } else {
            queryConfig.heartBeatInterval = config.heartBeatInterval
            queryConfig.heartBeatInterval = config.heartBeatInterval
            queryConfig.turnOnLight = config.turnOnLight
            queryConfig.turnOffLight = config.turnOffLight
            queryConfig.lightTime = config.lightTime
            queryConfig.uploadPhotoURL = config.uploadPhotoURL
            queryConfig.uploadLogURL = config.uploadLogURL
            queryConfig.qrCode = config.qrCode
            queryConfig.logLevel = config.logLevel
            queryConfig.status = config.status
            queryConfig.debugPasswd = config.debugPasswd
            queryConfig.irDefaultState = config.irDefaultState
            queryConfig.weightSensorMode = config.weightSensorMode
            println("回收柜 更新配置")
        }
        return row
    }

    /***
     * @param cabinBox 箱体信息
     * 保存箱体信息
     */
    fun toGetSaveCabins(cabinBoxs: List<CabinBox>?) {
        println("回收柜 开始保存箱体信息")
        cabinBoxs?.let {
            for (cabinBox in cabinBoxs) {
                val saveConfig = CabinEntity().apply {
                    cabinId = cabinBox.cabinId
                    capacity = cabinBox.capacity
                    createTime = cabinBox.createTime
                    delFlag = cabinBox.delFlag
                    doorStatus = cabinBox.doorStatus
                    filledTime = cabinBox.filledTime
                    netId = cabinBox.id
                    ir = cabinBox.ir
                    overweight = cabinBox.overweight
                    price = cabinBox.price
                    rodHinderValue = cabinBox.rodHinderValue
                    sn = cabinBox.sn
                    smoke = cabinBox.smoke
                    sort = cabinBox.sort
                    sync = cabinBox.sync
                    volume = cabinBox.volume
                    weight = cabinBox.weight
                }
                val queryCabin =
                        cabinBox.cabinId?.let { cabinId -> DatabaseManager.queryCabinEntity(AppUtils.getContext(), cabinId) }
                if (queryCabin == null) {
                    DatabaseManager.insertCabin(AppUtils.getContext(), saveConfig)
                    println("回收柜 添加箱体信息")
                } else {
                    queryCabin.cabinId = cabinBox.cabinId
                    queryCabin.capacity = cabinBox.capacity
                    queryCabin.createTime = cabinBox.createTime
                    queryCabin.delFlag = cabinBox.delFlag
                    queryCabin.doorStatus = cabinBox.doorStatus
                    queryCabin.filledTime = cabinBox.filledTime
                    queryCabin.netId = cabinBox.id
                    queryCabin.ir = cabinBox.ir
                    queryCabin.overweight = cabinBox.overweight
                    queryCabin.price = cabinBox.price
                    queryCabin.rodHinderValue = cabinBox.rodHinderValue
                    queryCabin.sn = cabinBox.sn
                    queryCabin.smoke = cabinBox.smoke
                    queryCabin.sort = cabinBox.sort
                    queryCabin.sync = cabinBox.sync
                    queryCabin.volume = cabinBox.volume
                    queryCabin.weight = cabinBox.weight
                    println("回收柜 更新箱体信息")
                }
            }
        }
    }

    /******************************************* socket通信 *************************************************/

    /*******************************************下位机通信部分*************************************************/
    //下发开仓
    fun issuedCmd(option: Int, onResponseResult: (type: String, openStatus: Int, success: Boolean) -> Unit) {
        CabinetSdk.openCommand(1, onOpenStatus = { lockerNo, status ->

        }, sendCallback)

    }

    //定时查询状态
    fun timingStatus() {
        CabinetSdk.queryStatus(lockerListStatusCallback, sendCallback)
    }

    private val lockerListStatusCallback: (MutableList<PortDeviceInfo>) -> Unit = { lowerMachines ->

    }
    /*******************************************下位机通信部分*************************************************/

    /*******************************************固件升级开始*************************************************/
    //232方式
    private val flowSteps7 = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps7: SharedFlow<Boolean> = flowSteps7.asSharedFlow()

    private val flowSteps8 = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps8: SharedFlow<Boolean> = flowSteps8.asSharedFlow()

    private val flowSteps8f = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps8f: SharedFlow<Boolean> = flowSteps8f.asSharedFlow()

    private val flowSteps9 = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps9: SharedFlow<Boolean> = flowSteps9.asSharedFlow()

    private val flowSteps10 = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps10: SharedFlow<Boolean> = flowSteps10.asSharedFlow()

    private val flowSteps232Succes = MutableSharedFlow<Boolean>(replay = 1)
    val isFlowSteps232Succes: SharedFlow<Boolean> = flowSteps232Succes.asSharedFlow()

    //芯片升级完成
    private val flowIsChip = MutableStateFlow(false)
    val isChip: MutableStateFlow<Boolean> = flowIsChip

    //芯片文件下载完成可进行升级
    private val flowIsChipSuccess = MutableSharedFlow<Boolean>(replay = 1)
    val IsChipSuccess: SharedFlow<Boolean> = flowIsChipSuccess.asSharedFlow()

    //232 次数超限
    private val flowFaildCount232 = MutableSharedFlow<Boolean>(replay = 1)
    val isFaildCount232: SharedFlow<Boolean> = flowFaildCount232.asSharedFlow()

    //1.固件升级失败  2.固件升级完成
    var isMasterFromChip = true

    //是否进行下位机固件升级
    var isLoweUpgrade232 = false

    //发送的文件数据
    val sendByteList232 = mutableListOf<ByteArray>()

    /***
     * 下载主芯片版本名称
     */
    var chipMasterName = ""

    /***
     * 下载主芯片版本大小
     */
    var chipMasterV = 0

    /***
     * 当前主芯片版本大小
     */
    var chipMasterVC = 0

    //统计能发送次数
    var send8fCount232 = 0

    fun findFileInSDCard(fileName: String): File? {
        // /sdcard/Android/data//com.cabinet.toolsapp/files/Download/20250701.bin
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // 获取应用私有存储目录下的download文件夹
            val downloadDir =
                    AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.walk()?.firstOrNull {
                it.name.equals(fileName, ignoreCase = true)
            }
        } else null
    }

    /**
     * 定时检测主从芯片是否升级完成 重启app
     */
    fun pollingUpgradeChip() {
        Loge.d("芯片升级 是否芯片升级完成 $isMasterFromChip | ${isChip.value} ")
        if (isMasterFromChip) {
            ioScope.launch {
                while (isActive) {
                    isMasterFromChip = false
                    Loge.d("芯片升级 是否芯片升级完成 ${isChip.value}")
                    if (isChip.value) {
                        Loge.d("芯片升级 启动计时重启app")
                        isChip.value = false
                        startTimer(5)
                        delay(500)
                    }
                }
            }
        }
    }

    //固件升级倒计时重启
    private val timer = CountdownTimer(viewModelScope)
    val countdownState = timer.countdownState

    fun startTimer(seconds: Int) {
        timer.startCountdown(seconds)
    }

    fun pauseTimer() {
        timer.pauseCountdown()
    }

    fun resetTimer() {
        timer.resetCountdown()
    }


    fun upgradeChip() {
//        delDownloadsFile()
        pollingUpgradeChip()
        ioScope.launch {
            if (!isChip.value) {
                flowSteps7.emit(true)
            } else {
                flowSteps7.emit(false)
                tipMessage("主芯片已经升级完成")
            }
        }

    }

    /***
     * 查询主芯片版本
     */
    fun executeVersion232(isUpgrade: Boolean = false, data: ByteArray, onUpgrade232: (status: Int) -> Unit) {
        Loge.i("串口232", "发232 执行指令: executePower")
        CabinetSdk.queryVersion232(11, data, onUpgrade = { status ->
            Loge.d("芯片升级 主芯片升级 接收指令回调 queryVersion232 = $status")
            //usb文件版本大于查询版本
            onUpgrade232(status)
        }, sendCallback)

    }

    /***
     * 提供给查询箱子内部工具状态
     * @param bytes 文件字节256个字节
     * 添加发送协议队列
     */
    private fun addFileByteQueue232(bytes: ByteArray) {
        ioScope.launch {
            // 将指令依次加入队列
            sendFileByte232.send(bytes)
        }
    }

    fun chipSet7() {
        isLoweUpgrade232 = true
        val head = byteArrayOf(0xaa.toByte(), 0xbb.toByte(), 0xcc.toByte())
        CabinetSdk.firmwareUpgrade2322(7, head, onUpgrade = { status ->
            ioScope.launch {
                Loge.d("芯片升级 主芯片升级 接收指令回调 status7 = $status")
                if (status == 1) {
                    flowSteps8.emit(true)
                } else {
                    flowSteps8.emit(false)
                }
                Loge.d("芯片升级 主芯片升级 发送升级指令结束, 数据：${ByteUtils.toHexString(head)}")
            }
        }, sendCallback)
    }

    fun chipSet8() {
        val file2 = findFileInSDCard(chipMasterName)
        val fileType = byteArrayOf(0xf1.toByte())
        val filSize = file2?.length()?.toInt() ?: 0
        filSize.let { size ->
            if (size < 0) return@let
            //软件大小
            val sizeByte = HexConverter.intToByteArray(size)
            Loge.d("芯片升级 主芯片升级 filSize = $size | sizeByte = ${ByteUtils.toHexString(sizeByte)}")
            //软件版本
            val vByte = HexConverter.intToByteArray(chipMasterV)
            Loge.d("芯片升级 主芯片升级 chipMasterV = $chipMasterV vByte = ${ByteUtils.toHexString(vByte)}")
            //CRC效验值
            val crc = CRC32MPEG2Util.computeFile(file2!!.absolutePath)
            val crcByte = HexConverter.intToByteArray(crc.toInt())
            Loge.d("芯片升级 主芯片升级 crcByte = ${ByteUtils.toHexString(crcByte)}")
            val sendByte =
                    com.serial.port.utils.HexConverter.combineByteArrays(fileType, sizeByte, vByte, crcByte)
            CabinetSdk.firmwareUpgrade2322(8, sendByte, onUpgrade = { status ->
                ioScope.launch {
                    Loge.d("芯片升级 主芯片升级 接收指令回调 status8 = $status")
                    if (status == 1) {
                        flowSteps8f.emit(true)
                    } else {
                        flowSteps8f.emit(false)
                    }
                    Loge.d("芯片升级 主芯片升级 发送状态指令结束 文件大小字节：${ByteUtils.toHexString(sendByte)}")
                }
            }, sendCallback)
        }
    }

    fun chipSet8f() {
        ioScope.launch {
            val masterFile = findFileInSDCard(chipMasterName)
            masterFile?.let { file ->
                sendByteList232.clear()
                try {
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(256)
                        var bytesRead: Int
                        var blockIndex = 0
                        // 循环读取直到文件结束
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
//                            // 处理实际读取的字节
//                            val blockToSend = if (bytesRead == 256) {
//                                buffer
//                            } else {
//                                // 最后一块可能不满256字节
//                                buffer.copyOf(bytesRead)
//                            }
                            // 无论是否满256字节，都创建新数组
                            val blockToSend =
                                    buffer.copyOfRange(0, bytesRead) // 或 buffer.copyOf(bytesRead)
                            Loge.d("芯片升级 主芯片升级 封装好数据 共发送 $blockIndex 个数据块，发了数据：${ByteUtils.toHexString(blockToSend)}")
                            sendByteList232.add(blockToSend)
                            blockIndex++

                        }
                        Loge.d("芯片升级 主芯片升级 共发送 $blockIndex 个数据块")
                    }
                } catch (e: Exception) {
                    Loge.d("芯片升级 主芯片升级 处理文件时出错: ${e.message}")
                } finally {
                    if (sendByteList232.isNotEmpty()) {
                        Loge.d("芯片升级 主芯片升级 封装好数据 开始发送文件数据")
                        sendByteList232.forEachIndexed { index, bytes ->
                            Loge.d("芯片升级 主芯片升级 封装好数据 发送第$index 个数据块，验证数据：${ByteUtils.toHexString(bytes)}")
                        }
                        chipSet8fs()
                    } else {
                        Loge.d("芯片升级 主芯片升级 封装好数据 没有文件数据")
                    }
                }
            }
        }
    }

    fun chipSet8fs() {
        ioScope.launch {
            if (sendByteList232.isNotEmpty()) {
                addFileByteQueue232(sendByteList232[send8fCount232])
                Loge.d("芯片升级 主芯片升级 chipSet8fs ${sendByteList232.size}")
                while (isActive) {  // 保证协程一直运行
                    val sendByte = sendFileByte232.receive()  // 从Channel中接收指令
                    Loge.i("串口232", "发232 主芯片升级 发送第$send8fCount232 个数据块，数据：${ByteUtils.toHexString(sendByte)}")
                    if (sendByte.isNotEmpty()) {
                        CabinetSdk.firmwareUpgrade232(sendByte, onUpgrade = { status ->
                            ioScope.launch {
                                if (status == 1) {
                                    send8fCount232++
                                    if (send8fCount232 <= sendByteList232.size - 1) {
                                        Loge.d("芯片升级 主芯片升级 sendByteList = ${sendByteList232.size} | send8fCount = $send8fCount232 | status = $status ")
                                        addFileByteQueue232(sendByteList232[send8fCount232])
                                    } else {
                                        flowSteps9.emit(true)
                                    }
                                } else {
                                    flowSteps9.emit(false)
                                }
                            }
                        }, sendCallback)
                        Loge.d("芯片升级 主芯片升级 sendByteList = ${sendByteList232.size} | send8fCount = $send8fCount232 ")
                        // 每次指令之间间隔1秒
                        delay(50)
//                        delay(3000)
                    }
                }
            }
        }
    }

    fun chipSet9() {
        val end = byteArrayOf(0xa4.toByte(), 0xa5.toByte(), 0xa6.toByte())
        CabinetSdk.firmwareUpgrade2322(9, end, onUpgrade = { status ->
            Loge.d("芯片升级 主芯片升级 接收指令回调 status9 = $status")
            ioScope.launch {
                if (status == 1) {
                    flowSteps10.emit(true)
                } else {
                    flowSteps10.emit(false)
                }
            }
        }, sendCallback)
    }

    fun chipSet10() {
        val cmd7 = byteArrayOf(0xa7.toByte(), 0xa8.toByte(), 0xa9.toByte())
        CabinetSdk.firmwareUpgrade2322(10, cmd7, onUpgrade = { status ->
            ioScope.launch {
                Loge.d("芯片升级 主芯片升级 接收指令回调 status10 = $status")
                if (status == 1) {
                    flowSteps232Succes.emit(true)
                    isChip.value = true
                    tipMessage("主芯片升级完成")
                }
                Loge.d("芯片升级 主芯片升级 接收重启指令完成 文件大小字节：${ByteUtils.toHexString(cmd7)}")
            }
        }, sendCallback)
    }

    /*******************************************固件升级结束*************************************************/
    /***
     * @return message 消息
     */
    var isPrintSend = true
    private var sendCount = 0
    private val sendCallback: (String) -> Unit = { message ->
        if (isPrintSend) {
            try {
                val startIndex = message.indexOf("|") + 1
                val text = message.substring(0, startIndex)
                val lastIndex = message.lastIndexOf("|")
                val byteData = message.substring(startIndex, lastIndex)
                val data = byteData.split(" ")
                val protocolType = data[0]
                val command = data[2]
                when (protocolType) {
                    "9A" -> {
                        if (command == "00" || command == "01" || command == "03" || command == "04" || command == "05" || command == "06" || command == "07" || command == "08" || command == "09" || command.contains("0a", ignoreCase = true) || command.contains("0b", ignoreCase = true)) {
                            Loge.i("串口232", "发送成功 发送给下位机数据状态 ${Thread.currentThread().name}: $text $command | $byteData")
                            BoxToolLogUtils.sendOriginalLower(232, byteData)
                        } else if (command == "02"/* && sendCount >= 10*/) {
                            sendCount = 0
                            Loge.i("串口232", "发送成功 发送给下位机数据状态 ${Thread.currentThread().name}: $text $command | $byteData")
                            BoxToolLogUtils.sendOriginalLowerStatus(232, byteData)
                        }
                        sendCount++
                    }

                    else -> {
                        BoxToolLogUtils.sendOriginalLower(232485, byteData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /*******************************************http模块*************************************************/
    //获取APP版本信息
    private val flowVersionInfo = MutableStateFlow<VersionDto>(VersionDto())
    val versionInfoCollect: StateFlow<VersionDto> = flowVersionInfo
    private fun refreshVersionInfo(list: VersionDto) {
        flowVersionInfo.value = list
    }

    //获取app版本
    private val flowIsUpdate = MutableSharedFlow<Boolean>(replay = 1)
    val isUpdateCollect = flowIsUpdate.asSharedFlow()

    //下载进度异常
    private val flowDownError = MutableSharedFlow<Boolean>(replay = 1)
    val downErrorCollect = flowDownError.asSharedFlow()

    //下载进度中
    private val flowDownProgress = MutableStateFlow<Float>(0f)
    val downProgressCollect = flowDownProgress

    //下载进度完成
    private val flowDownSuccess = MutableSharedFlow<File>(replay = 1)
    val downSuccessCollect = flowDownSuccess.asSharedFlow()

    /***
     * 获取在线版本
     */
    fun checkVersion(version: String) {
        ioScope.launch {
            val post = mutableMapOf<String, Any>()
            post[HttpUrl.VERSION] = version
            httpRepo.version(post).onCompletion {
                Loge.d("网络请求 获取在线版本 onCompletion $post")

            }.onSuccess {
                Loge.d("网络请求 获取在线版本 onSuccess ${it.toString()}")
                refreshVersionInfo(it!!)
                flowIsUpdate.emit(true)

            }.onFailure { code, message ->
                Loge.d("网络请求 获取在线版本 onFailure $code $message")
                flowIsUpdate.emit(false)

                flowIsNetworkMessage.emit("$message")
            }.onCatch { e ->
                Loge.d("网络请求 获取在线版本 onCatch ${e.errorMsg}")
                flowIsUpdate.emit(false)
                flowIsNetworkMessage.emit("${e.errorMsg}")
            }
        }
    }

    /***
     * 下载版本
     */
    fun downloadApk(downloadUrl: String, apkName: String) {
        singleDownloader = SingleDownloader(CorHttp.getInstance().getClient())
        singleDownloader?.onStart {
            Loge.d("网络请求 downloadApk onStart $downloadUrl")

        }?.onProgress { current, total, progress ->
            Loge.d("网络请求 downloadApk onProgress $current $total $progress")
            mainScope.launch {
                flowDownProgress.emit(progress.toFloat())
            }
        }?.onSuccess { url, file ->
            Loge.d("网络请求 downloadApk onSuccess $url ${file.path} $")
            ioScope.launch {
                flowDownSuccess.emit(file)
            }

        }?.onError { url, cause ->
            Loge.d("网络请求 downloadApk onError $url ${cause.message} ")
            ioScope.launch {
                flowDownError.emit(true)
                flowIsNetworkMessage.emit("${cause.message}")
            }

        }?.onCompletion { url, filePath ->
            Loge.d("网络请求 downloadApk onCompletion $url $filePath ")
            ioScope.launch {

            }
        }?.excute(downloadUrl, File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${apkName}.apk").toString())

//              ?.excute("https://cdn.chenair.com/down/apk/chuangyachuangzuodashi.apk", File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${apkName}.apk").toString())
    }

    //下载
    var singleDownloader: SingleDownloader? = null

    /***
     * 取消下载
     */
    fun downloadCancel() {
        singleDownloader?.cancel()
    }

    /***
     * 下载主芯片文件
     */
    fun downloadChip(downloadUrl: String, chip: String) {
        singleDownloader = SingleDownloader(CorHttp.getInstance().getClient())
        singleDownloader?.onStart {
            Loge.d("网络请求 downloadMasterChip onStart $downloadUrl")
        }?.onProgress { current, total, progress ->
            Loge.d("网络请求 downloadMasterChip onProgress $current $total $progress")

        }?.onSuccess { url, file ->
            Loge.d("网络请求 downloadMasterChip onSuccess $url ${file.path} $")
            ioScope.launch {
                //标记芯片文件完成
                flowIsChipSuccess.emit(true)
            }
        }?.onError { url, cause ->
            Loge.d("网络请求 downloadMasterChip onError $url ${cause.message} ")
            ioScope.launch {
                flowIsNetworkMessage.emit("${cause.message}")
            }

        }?.onCompletion { url, filePath ->
            Loge.d("网络请求 downloadMasterChip onCompletion $url $filePath ")

        }?.excute(downloadUrl, File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${chip}.bin").toString())
    }

    /*******************************************http模块*************************************************/

}
