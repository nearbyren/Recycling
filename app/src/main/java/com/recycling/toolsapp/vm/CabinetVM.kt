package com.recycling.toolsapp.vm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.recycling.toolsapp.R
import com.recycling.toolsapp.db.DatabaseManager
import com.recycling.toolsapp.http.HttpUrl
import com.recycling.toolsapp.http.RepoImpl
import com.recycling.toolsapp.http.VersionDto
import com.recycling.toolsapp.model.LatticeEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.ResEntity
import com.recycling.toolsapp.model.StateEntity
import com.recycling.toolsapp.model.TransEntity
import com.recycling.toolsapp.model.WeightEntity
import com.recycling.toolsapp.socket.ConfigBean
import com.recycling.toolsapp.socket.ConfigLattice
import com.recycling.toolsapp.socket.DoorCloseBean
import com.recycling.toolsapp.socket.DoorOpenBean
import com.recycling.toolsapp.socket.ConfigInfo
import com.recycling.toolsapp.socket.ConfigRes
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.utils.CmdType
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.utils.MediaPlayerHelper
import com.recycling.toolsapp.utils.ResultType
import com.serial.port.CabinetSdk
import com.serial.port.PortDeviceInfo
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.CRC32MPEG2Util
import com.serial.port.utils.CmdCode
import com.serial.port.utils.FileMdUtil
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
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

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

    private val doorQueue = Channel<Int>()

    //
    var mQrCode: Bitmap? = null

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

    //相机提供者
    var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    //处理摄像头提供程序
    var cameraProvider1: ProcessCameraProvider? = null
    var cameraSelector1: CameraSelector? = null
    var imageCapture1: ImageCapture? = null
    var imageAnalysis1: ImageAnalysis? = null
    var videoCapture1: VideoCapture<Recorder>? = null
    var recording1: Recording? = null

    var cameraProvider2: ProcessCameraProvider? = null
    var cameraSelector2: CameraSelector? = null
    var imageCapture2: ImageCapture? = null
    var videoCapture2: VideoCapture<Recorder>? = null
    var recording2: Recording? = null
    val delays = listOf(3, 6, 9, 12) // 延迟时间列表（秒）

    private val flowDelays = MutableSharedFlow<Long>(replay = 1)
    val getDelays = flowDelays.asSharedFlow()

    //保存的图片
    private val flowTakePic = MutableSharedFlow<String>(replay = 1)
    val getTakePic = flowTakePic.asSharedFlow()

    fun taskPicAdd(pic: String) {
        ioScope.launch {
            Log.e("TestFace", "网络导入用户信息 发送照片")
            flowTakePic.emit(pic)
        }
    }

    fun takePictures() {
        Log.e("TestFace", "网络导入用户信息 takePictures")
        delays.forEach {
            ioScope.launch {
                val del = it * 1000L
                delay(del)
                Log.e("TestFace", "网络导入用户信息 launch $del")
                flowDelays.emit(del)
            }
        }
    }

    private val flowLoginCmd = MutableSharedFlow<Boolean>(replay = 1)
    val getLoginCmd = flowLoginCmd.asSharedFlow()

    //处理网络提示语
    private val flowIsNetworkMessage = MutableSharedFlow<String>(replay = 1)
    val _isNetworkMessage = flowIsNetworkMessage.asSharedFlow()

    //openDoor
    private val flowIsOpenDoor = MutableSharedFlow<Boolean>(replay = 1)
    val isOpenDoor = flowIsOpenDoor.asSharedFlow()

    /***
     * 倒计时结束  countdownEnd
     *  1.点击
     *  2.倒计时结束
     *  310.门打开
     *  301.门关闭
     */
    private val flowDeliveryTypeEnd = MutableSharedFlow<Int>(replay = 1)
    val isDeliveryTypeEnd = flowDeliveryTypeEnd.asSharedFlow()

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

    /*
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
                         CmdValue.CMD_HEART_BEAT -> {
                            println("调试socket recv: 接收心跳成功")
                            //记录日志
                        }

                         CmdValue.CMD_LOGIN -> {
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
                            delay(500)
                            vmClient?.sendHeartbeat()
                        }

                        CmdValue.CMD_INIT_CONFIG -> {
                            val initConfigModel = Gson().fromJson(json, InitConfigDto::class.java)
                            println("调试socket recv: 接收 initConfig 成功")
                        }

                      CmdValue.CMD_OPEN_DOOR  -> {
                            println("调试socket recv: 接收 openDoor 成功")
                            val doorOpenModel = Gson().fromJson(json, DoorOpenDto::class.java)
                            toGoDownDoorOpen(doorOpenModel)
                        }

                       CmdValue.CMD_CLOSE_DOOR  -> {
                            println("调试socket recv: 接收 closeDoor成功")
                        }

                         CmdValue.CMD_PHONE_NUMBER_LOGIN -> {
                            println("调试socket recv: 接收 phoneNumberLogin 成功")
                        }

                          CmdValue.CMD_PHONE_USER_OPEN_DOOR  -> {
                            println("调试socket recv: 接收 phoneUserOpenDoor 成功")
                        }

                         CmdValue.CMD_RESTART -> {
                            println("调试socket recv: 接收 restart 成功")
                        }

                        CmdValue.CMD_UPLOAD_LOG -> {
                            println("调试socket recv: 接收 uploadLog 成功")
                        }

                         CmdValue.CMD_OTA -> {
                            println("调试socket recv: 接收 OTA 成功")
                        }
                    }
                }
                //启动socket连接
                vmClient?.start()
                println("调试socket client = $vmClient | state = ${vmClient?.state}")
                vmClient?.state?.collect {
                    println("调试socket 连接状态: $it | ${Thread.currentThread().name}")
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
    */

    fun convertToJsonString(obj: Any): String {
        return Gson().toJson(obj)
    }

    fun toGoCmdLogin() {
        ioScope.launch {
            val m =
                    mapOf("cmd" to "login", "sn" to "0136004ST00041", "imsi" to "460086096808642", "imei" to "868408061812125", "iccid" to "898604A70821C0049781", "version" to "1.0.0", "timestamp" to System.currentTimeMillis())
            val json = convertToJsonString(m)
            vmClient?.sendText(json)
        }
    }

    var currentDoor: Int = -1

    /*****************************************监听仓门是否关闭******************************************/
    var doorJob: Job? = null

    /***
     * 操作格口类型状态
     * 0.关门 1.开门
     */
    var geTypeStatu = CmdCode.GE

    /***
     * 标记当前格口
     */
    var doorGeX = CmdCode.GE
    fun addDoorQueue(commandType: Int) {
        ioScope.launch {
            println("调试串口 进来 addDoorQueue $commandType")
            // 将指令依次加入队列
            doorQueue.send(commandType)
        }
    }

    //标记接收 门开 门关回调
    private val flowIsDoorOpenClose = MutableStateFlow(false)
    val getDoorOpenClose: MutableStateFlow<Boolean> = flowIsDoorOpenClose

    //标记接收 重量 回调
    private val flowCurWeight = MutableStateFlow(false)
    val getCurWeight: MutableStateFlow<Boolean> = flowCurWeight

    private val flowIsDoorOpen = MutableStateFlow(false)
    val isDoorOpen: MutableStateFlow<Boolean> = flowIsDoorOpen

    private val flowIsDoorClose = MutableStateFlow(false)
    val isDoorClose: MutableStateFlow<Boolean> = flowIsDoorClose

    /***************************************** 发送格口开启 格口状态查询 格口重量查询 ******************************************/

    /**
     * 发送格口开关门
     * 实时格口门状态
     * 查询格口重量
     */
    fun pollingDoor() {
        println("调试串口 启动检测门状态轮询")
        doorJob = ioScope.launch {
            while (isActive) {
                val cmdType = doorQueue.receive()  // 从Channel中接收指令
                println("调试串口 pollingDoor  指令：$cmdType | actionStatus：$geTypeStatu")
                if (cmdType == CmdType.CMD1) {//启动格口开门
                    val code = when (doorGeX) {
                        CmdCode.GE1 -> {
                            if (geTypeStatu == CmdCode.GE_OPEN) {
                                CmdCode.GE11
                            } else {
                                CmdCode.GE10
                            }
                        }

                        CmdCode.GE2 -> {
                            if (geTypeStatu == CmdCode.GE_OPEN) {
                                CmdCode.GE21
                            } else {
                                CmdCode.GE20
                            }
                        }

                        else -> {
                            -1
                        }
                    }
                    if (code == -1) return@launch
                    CabinetSdk.turnDoor(code, turnDoorCallback = { lockerNo, status ->
                        println("调试串口 发送扭动门状态 接收回调 $status")
                    }, sendCallback)
                    delay(1000)
                    addDoorQueue(CmdType.CMD3)
                } else if (cmdType == CmdType.CMD3) {//查询投口门状态
                    CabinetSdk.turnDoorStatus(doorGeX, onDoorStatus = { status ->
                        println("调试串口 发送查询门状态 接收回调 $status")
                        if (geTypeStatu == CmdCode.GE_OPEN && status == CmdCode.GE_OPEN) {
                            flowIsDoorOpenClose.value = true
                            testPortResult(status)
                        } else if (geTypeStatu == CmdCode.GE_CLOSE && status == CmdCode.GE_CLOSE) {
                            flowIsDoorOpenClose.value = true
                            testPortResult(status)
                        }
                    }, sendCallback)
                    //当没有接收到回调至继续查询
                    if (!getDoorOpenClose.value) {
                        println("调试串口 pollingDoor 继续")
                        delay(1000)
                        addDoorQueue(CmdType.CMD3)
                    } else if (getDoorOpenClose.value) {
                        println("调试串口 pollingDoor 停止查询门状态")
                        flowIsDoorOpenClose.value = false
                        cancelDoorJob()
                    }
                } else if (cmdType == CmdType.CMD4) {//查询格口重量
                    CabinetSdk.queryWeight(doorGeX, weightCallback = { weight ->
                        println("调试串口 发送查询重量 接收回调 $weight")
                        flowCurWeight.value = true
                    }, sendCallback)
                    //当没有接收到回调至继续查询
                    if (!getCurWeight.value) {
                        println("调试串口 pollingDoor 继续")
                        delay(1000)
                        addDoorQueue(CmdType.CMD4)
                    } else if (getCurWeight.value) {
                        println("调试串口 pollingDoor 停止查询重量")
                        cancelDoorJob()
                        flowCurWeight.value = false

                    }
                }
            }
        }
    }

    fun cancelDoorJob() {
        doorJob?.cancel()
        doorJob = null
    }

    /***************************************** 发送格口开启 格口状态查询 格口重量查询 ******************************************/

    /***
     *
     */
    fun testClearCmd() {
        val code = Random.nextInt(1, 3)
        println("调试串口 testClearCmd code $code")
        CabinetSdk.openClear(code, onOpenStatus = { lockerNo, status ->
            println("调试串口 testClearCmd 接收到回调")
        }, sendCallback)
    }

    /***
     *
     */
    fun testWeightCmd() {
        flowCurWeight.value = false
        pollingDoor()
        addDoorQueue(4)
    }

    /***
     * @param status
     * 0.门关动作
     * 1.门开动作
     *
     * 3.发送开门
     * 4.发送关门
     * 5.查询门状态
     */
    fun testSendCmd(status: Int) {
        this.geTypeStatu = status
        pollingDoor()
        addDoorQueue(1)
    }

    /***
     * @param typeEnd
     * 1.点击
     * 2.计时结束
     */
    fun testTypeEnd(typeEnd: Int) {
        ioScope.launch {
            flowDeliveryTypeEnd.emit(typeEnd)
        }
    }

    var currentTransId = ""
    fun testToGoDownDoorOpen() {
        ioScope.launch {
            currentTransId = AppUtils.getUUID()
            val model = DoorOpenBean().apply {
                //指令
                cmd = "openDoor"
                //事务id
                transId = currentTransId
                //舱门编码
                cabinId = "20250102125515989511"
                //服务下发
                openType = 1
                //终端上发
                //状态 0.成功 1.失败
                retCode = 0
                //手机号
                phoneNumber = "18938844110"
                //当前重量
                curWeight = "2.00"
                //时间戳
                timestamp = AppUtils.getDateYMDHMS()
            }
            val trensEntity = TransEntity().apply {
                cmd = "openDoor"
                transId = model.transId
                openType = model.openType
                cabinId = model.cabinId
                openStatus = -1
                time = AppUtils.getDateYMDHMS()
            }
            //这里需要下发指令查询获取当前重量

            val row = DatabaseManager.insertTrans(AppUtils.getContext(), trensEntity)

            println("调试socket 接收开仓 记录事务 $row")

            //查询当前格口获取重量
            val lattice =
                    DatabaseManager.queryLatticeEntity(AppUtils.getContext(), model.cabinId ?: "")

            //构建上发数据
            //获取交易订单号
            val tid = model.transId
            //获取格接口编码
            val cid = model.cabinId
            //获取格口类型
            val gType = model.openType
//            val weight = "%.2f".format(lattice.weight?:0.00f).toFloat()
//            println("调试socket 发送开仓成功 weight $weight |db:${lattice.weight}")
            val doorOpen = DoorOpenBean().apply {
                cmd = "openDoor"
                transId = tid
                cabinId = cid
                phoneNumber = ""
                curWeight = lattice.weight
                retCode = 0
                timestamp = AppUtils.getDateYMDHMS()
            }
            val json = convertToJsonString(doorOpen)
            println("调试socket 发送开仓成功 $json")

            val weightEntity = WeightEntity().apply {
                cmd = "openDoor"
                transId = tid
                curWeight = lattice.weight
                time = AppUtils.getDateYMDHMS()
            }
            val traRow = DatabaseManager.insertWeight(AppUtils.getContext(), weightEntity)

            println("调试socket 接收开仓 记录一条前后重量数据 $traRow")


            flowIsOpenDoor.emit(true)
        }
    }

    /***
     * 接收服务器 格口开启
     */
    fun toGoDownDoorOpen(model: DoorOpenBean) {
        ioScope.launch {
            val trensEntity = TransEntity().apply {
                transId = model.transId
                openType = model.openType
                cabinId = model.cabinId
                openStatus = -1
                time = AppUtils.getDateYMDHMS()
            }
            //记录事务数据
            val row = DatabaseManager.insertTrans(AppUtils.getContext(), trensEntity)
            println("调试socket 添加开仓记录 $row")
//-----
//            //查询当前格口获取重量
//            val lattice =
//                    DatabaseManager.queryLatticeEntity(AppUtils.getContext(), model.cabinId ?: "")
//            //构建上发数据
//            //获取交易订单号
//            val tid = model.transId
//            //获取格接口编码
//            val cid = model.cabinId
//            //获取格口类型
//            val gType = model.openType
////            val weight = "%.2f".format(lattice.weight?:0.00f).toFloat()
////            println("调试socket 发送开仓成功 weight $weight")
//            val doorOpen = DoorOpenBean().apply {
//                cmd = "openDoor"
//                transId = tid
//                cabinId = cid
//                phoneNumber = ""
//                curWeight = lattice.weight
//                retCode = 0
//                timestamp = AppUtils.getDateYMDHMS()
//            }
//            val json = convertToJsonString(doorOpen)
//            println("调试socket 发送开仓成功 $json")
//            vmClient?.sendText(json)

//            flowIsOpenDoor.emit(true)
        }
    }

    fun sendUpRec(type: String) {
        println("调试串口 上发类型 $type")
        when (type) {
            CmdValue.CMD_OPEN_DOOR -> {
                //当打开成功则更新本地记录
                ioScope.launch {
                    val trans = DatabaseManager.queryTransMax(AppUtils.getContext())
                    //获取交易订单号
                    val tid = trans.transId
                    //获取格接口编码
                    val cid = trans.cabinId
                    tid?.let { tidid ->
                        DatabaseManager.upTransOpenStatus(AppUtils.getContext(), 1, tidid)
                        println("调试串口 更新本地打开状态 ")
                    }

                    //查询当前格口获取重量
                    val lattice =
                            DatabaseManager.queryLatticeEntity(AppUtils.getContext(), cid ?: "")

                    //构建上发数据
//            val weight = "%.2f".format(lattice.weight?:0.00f).toFloat()
//            println("调试socket 发送开仓成功 weight $weight")
                    val doorOpen = DoorOpenBean().apply {
                        cmd = "openDoor"
                        transId = tid
                        cabinId = cid
                        phoneNumber = ""
                        curWeight = lattice.weight
                        retCode = 0
                        timestamp = AppUtils.getDateYMDHMS()
                    }
                    val json = convertToJsonString(doorOpen)
                    println("调试socket 发送开仓成功 $json")
//                    vmClient?.sendText(json)

                }
            }

            CmdValue.CMD_CLOSE_DOOR -> {
                //当上发个服务器接收关闭后，则更新本地记录信息
                ioScope.launch {
                    println("调试串口 更新本地关闭状态 ")
                    val trans = DatabaseManager.queryTransMax(AppUtils.getContext())
                    //获取交易订单号
                    val tid = trans.transId
                    //获取格接口编码
                    val cid = trans.cabinId
                    tid?.let { tidid ->
                        DatabaseManager.upTransCloseStatus(AppUtils.getContext(), 1, tidid)
                        println("调试串口 更新本地交易状态未关闭 ")
                    }
                    //查询当前格口获取重量
                    val lattice =
                            DatabaseManager.queryLatticeEntity(AppUtils.getContext(), cid ?: "")

                    //构建上发数据
//            val weight = "%.2f".format(lattice.weight?:0.00f).toFloat()
//            println("调试socket 发送开仓成功 weight $weight")
                    //这里应该拿到的是最新称重体重
                    val doorClose = DoorCloseBean().apply {
                        cmd = "closeDoor"
                        transId = tid
                        cabinId = cid
                        phoneNumber = ""
                        curWeight = lattice.weight
                        changeWeight = "10.00"
                        refWeight = "0.00"
                        beforeUpWeight = "0.00"
                        afterUpWeight = "0.00"
                        beforeDownWeight = "0.00"
                        afterDownWeight = "0.00"
                        timestamp = AppUtils.getDateYMDHMS()
                    }
                    val json = convertToJsonString(doorClose)
                    println("调试socket 发送关门成功 $json")
//                    vmClient?.sendText(json)
                }
            }
        }
    }

    /***
     * 关闭舱门
     */
    fun toGoUpDoorClose() {
        //构建上发数据
        val doorClose = DoorCloseBean().apply {
            cmd = "closeDoor"
            transId = ""
            cabinId = ""
            phoneNumber = ""
            curWeight = "0.00"
            changeWeight = "0.00"
            refWeight = "0.00"
            beforeUpWeight = "0.00"
            afterUpWeight = "0.00"
            beforeDownWeight = "0.00"
            afterDownWeight = "0.00"
            timestamp = AppUtils.getDateYMDHMS()
        }
    }

    /**
     * 保存网络数据
     */
    fun saveInitNet(loginModel: ConfigBean) {
        println("调试socket saveInitNet ${Thread.currentThread().name}")
        ioScope.launch {
            val heartbeatIntervalMillis = loginModel.config.heartBeatInterval?.toLong() ?: 10
            vmClient?.config?.heartbeatIntervalMillis1 =
                    TimeUnit.SECONDS.toMillis(heartbeatIntervalMillis)
            println("调试socket recv: 心跳秒：$heartbeatIntervalMillis")
            vmClient?.config?.heartbeatIntervalMillis1 = TimeUnit.SECONDS.toMillis(10)
            val config = loginModel.config
            //保存基础配置信息
            loginModel.sn?.let { snCode ->
                println("调试socket 开始添加配置 ${Thread.currentThread().name}")
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
                    val row = DatabaseManager.insertConfig(AppUtils.getContext(), saveConfig)
                    println("调试socket 添加配置 $row")
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
                    val row = DatabaseManager.upConfigEntity(AppUtils.getContext(), saveConfig)
                    println("调试socket 更新配置 $row")
                }
            }

            //创建二维码
            val logoBitmap =
                    BitmapFactory.decodeResource(AppUtils.getContext().resources, R.mipmap.ic_launcher_by)
            val bg = BitmapFactory.decodeResource(AppUtils.getContext().resources, R.color.black)
            AwesomeQRCode.Renderer().contents(config.qrCode).background(bg).size(800) // 增加尺寸以提高可扫描性
                .roundedDots(true).dotScale(0.6f) // 增加点的大小
                .colorDark(Color.BLACK) // 深色部分为黑色
                .colorLight(Color.WHITE) // 浅色部分为白色 - 这是关键修复
                .whiteMargin(true).margin(20) // 增加边距
                .logo(logoBitmap).logoMargin(10).logoRadius(10).logoScale(0.15f) // 减小logo尺寸，避免遮挡关键信息
                .renderAsync(object : AwesomeQRCode.Callback {
                    override fun onError(renderer: AwesomeQRCode.Renderer, e: Exception) {
                        println("调试socket 创建二维码失败")
                        e.printStackTrace()
                    }

                    override fun onRendered(renderer: AwesomeQRCode.Renderer, bitmap: Bitmap) {
                        mQrCode = bitmap
                        println("调试socket 创建二维码成功 保存开始")
                        saveBitmapToInternalStorage(bitmap, "qrCode.png")

                    }
                })
            //保存箱体信息
            println("调试socket 开始保存箱体信息 ${Thread.currentThread().name}")
            val stateBox = mutableListOf<StateEntity>()
            var volume = 3
            loginModel.config.list?.let { lattices ->
                for (lattice in lattices) {
                    val saveConfig = LatticeEntity().apply {
                        cabinId = lattice.cabinId
                        capacity = lattice.capacity
                        createTime = lattice.createTime
                        delFlag = lattice.delFlag
                        doorStatus = lattice.doorStatus
                        filledTime = lattice.filledTime
                        netId = lattice.id
                        ir = lattice.ir
                        overweight = lattice.overweight
                        price = lattice.price
                        rodHinderValue = lattice.rodHinderValue
                        sn = lattice.sn
                        smoke = lattice.smoke
                        sort = lattice.sort
                        sync = lattice.sync
                        volume = lattice.volume
                        weight = lattice.weight
                    }
                    volume = lattice.volume
                    val queryLattice =
                            lattice.cabinId?.let { cabinId -> DatabaseManager.queryLatticeEntity(AppUtils.getContext(), cabinId) }
                    if (queryLattice == null) {
                        val rowCabin =
                                DatabaseManager.insertLattice(AppUtils.getContext(), saveConfig)
                        println("调试socket 添加箱体信息 $rowCabin")
                        val setCapacity = lattice.capacity?.toInt() ?: 0
                        val setIrState = lattice.ir
                        val setWeigh = lattice.weight?.toFloat() ?: 0.00f
                        val setCabinId = lattice.cabinId ?: ""
                        val state = StateEntity().apply {
                            smoke = 0
                            capacity = setCapacity
                            irState = setIrState
                            weigh = setWeigh
                            doorStatus = 0
                            lockStatus = 0
                            cabinId = setCabinId
                            time = AppUtils.getDateYMDHMS()
                        }
                        stateBox.add(state)

                    } else {
                        queryLattice.cabinId = lattice.cabinId
                        queryLattice.capacity = lattice.capacity
                        queryLattice.createTime = lattice.createTime
                        queryLattice.delFlag = lattice.delFlag
                        queryLattice.doorStatus = lattice.doorStatus
                        queryLattice.filledTime = lattice.filledTime
                        queryLattice.netId = lattice.id
                        queryLattice.ir = lattice.ir
                        queryLattice.overweight = lattice.overweight
                        queryLattice.price = lattice.price
                        queryLattice.rodHinderValue = lattice.rodHinderValue
                        queryLattice.sn = lattice.sn
                        queryLattice.smoke = lattice.smoke
                        queryLattice.sort = lattice.sort
                        queryLattice.sync = lattice.sync
                        queryLattice.volume = lattice.volume
                        queryLattice.weight = lattice.weight
                        val rowCabin =
                                DatabaseManager.upLatticeEntity(AppUtils.getContext(), queryLattice)
                        println("调试socket 更新箱体信息 $rowCabin")
                    }
                }
                //配置音量
                MediaPlayerHelper.setVolume(AppUtils.getContext(), 2)
                for (state in stateBox) {
                    val rowState = DatabaseManager.insertState(AppUtils.getContext(), state)
                    println("调试socket 添加心跳信息 $rowState")
                }
            }
            //保存资源配置
            println("调试socket 开始加载资源 ${Thread.currentThread().name}")
            loginModel.config.resourceList?.let { resources ->
                for (resource in resources) {
                    val saveResource = ResEntity().apply {
                        filename = resource.filename
                        url = resource.url
                        md5 = resource.md5
                        time = AppUtils.getDateYMDHMS()
                    }
                    val queryResource =
                            DatabaseManager.queryRes(AppUtils.getContext(), resource.filename ?: "")
                    if (queryResource == null) {
                        val row = DatabaseManager.insertRes(AppUtils.getContext(), saveResource)
                        println("调试socket 添加资源 $row")
                    } else {
                        queryResource.filename = resource.filename
                        queryResource.url = resource.filename
                        queryResource.md5 = resource.md5
                        //资源不一致下载到本地
                        if (queryResource.md5 != resource.md5) {
                            val fileName = resource.filename ?: ""
                            var dir = FileMdUtil.matchNewFileName("audio", fileName)
                            if (shouldAudio(fileName)) {
                                dir = FileMdUtil.matchNewFileName("audio", fileName)
                            } else if (shouldPGJ(fileName)) {
                                dir = FileMdUtil.matchNewFileName("res", fileName)
                            }
                            queryResource.url?.let { url ->
                                downloadRes(url, dir) { success ->
                                    if (success) {
                                        queryResource.status = 0
                                        val row =
                                                DatabaseManager.upResEntity(AppUtils.getContext(), queryResource)
                                        println("调试socket 下载资源成功 ${queryResource.filename}")
                                        println("调试socket 下载资源成功 更新数据 $row")
                                    } else {
                                        queryResource.status = 1
                                        val row =
                                                DatabaseManager.upResEntity(AppUtils.getContext(), queryResource)
                                        println("调试socket 下载资源失败 $row")
                                        println("调试socket 下载资源失败 更新数据 $row")
                                    }
                                }
                            }
                        }

                    }
                }
            }
            //发送心跳
            vmClient?.sendHeartbeat()
            println("调试socket saveInitNet 启动页面")
            delay(2000)
            flowLoginCmd.emit(true)
        }
    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, fileName: String): String? {
        // 指定存储目录：/data/data/包名/files/userAvatar/
//        val dir = File(AppUtils.getContext().filesDir, "faceVerify")
        val dir = FileMdUtil.matchNewFile("res")
        if (!dir.exists()) {
            dir.mkdirs() // 创建目录
        }

        // 创建保存文件
        val file = File(dir, fileName)
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)
            // 将 Bitmap 压缩为 PNG 格式保存
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            return file.absolutePath // 返回保存路径
        } catch (e: IOException) {
            e.printStackTrace()

        } finally {
            fos?.close() // 关闭输出流
        }
        return null
    }

    /***
     * @param config 配置信息
     * 保存配置信息
     */
    fun toGetSaveConfigEntity(snCode: String, config: ConfigInfo) {
        ioScope.launch {
            println("调试socket 开始添加配置 ${Thread.currentThread().name}")

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
                val row = DatabaseManager.insertConfig(AppUtils.getContext(), saveConfig)
                println("调试socket 添加配置 $row")
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
                val row = DatabaseManager.upConfigEntity(AppUtils.getContext(), saveConfig)
                println("调试socket 更新配置 $row")
            }
//            return row
        }
    }

    /***
     * @param lattice 箱体信息
     * 保存箱体信息
     */
    fun toGetSaveLattice(lattices: List<ConfigLattice>?) {
        ioScope.launch {
            println("调试socket 开始保存箱体信息 ${Thread.currentThread().name}")
            val stateBox = mutableListOf<StateEntity>()
            var volume = 3
            lattices?.let {
                for (lattice in lattices) {
                    val saveConfig = LatticeEntity().apply {
                        cabinId = lattice.cabinId
                        capacity = lattice.capacity
                        createTime = lattice.createTime
                        delFlag = lattice.delFlag
                        doorStatus = lattice.doorStatus
                        filledTime = lattice.filledTime
                        netId = lattice.id
                        ir = lattice.ir
                        overweight = lattice.overweight
                        price = lattice.price
                        rodHinderValue = lattice.rodHinderValue
                        sn = lattice.sn
                        smoke = lattice.smoke
                        sort = lattice.sort
                        sync = lattice.sync
                        volume = lattice.volume
                        weight = lattice.weight
                    }
                    volume = lattice.volume
                    val queryLattice =
                            lattice.cabinId?.let { cabinId -> DatabaseManager.queryLatticeEntity(AppUtils.getContext(), cabinId) }
                    if (queryLattice == null) {
                        val rowCabin =
                                DatabaseManager.insertLattice(AppUtils.getContext(), saveConfig)
                        println("调试socket 添加箱体信息 $rowCabin")
                        val setCapacity = lattice.capacity?.toInt() ?: 0
                        val setIrState = lattice.ir
                        val setWeigh = lattice.weight?.toFloat() ?: 0.00f
                        val setCabinId = lattice.cabinId ?: ""
                        val state = StateEntity().apply {
                            smoke = 0
                            capacity = setCapacity
                            irState = setIrState
                            weigh = setWeigh
                            doorStatus = 0
                            lockStatus = 0
                            cabinId = setCabinId
                            time = AppUtils.getDateYMDHMS()
                        }
                        stateBox.add(state)

                    } else {
                        queryLattice.cabinId = lattice.cabinId
                        queryLattice.capacity = lattice.capacity
                        queryLattice.createTime = lattice.createTime
                        queryLattice.delFlag = lattice.delFlag
                        queryLattice.doorStatus = lattice.doorStatus
                        queryLattice.filledTime = lattice.filledTime
                        queryLattice.netId = lattice.id
                        queryLattice.ir = lattice.ir
                        queryLattice.overweight = lattice.overweight
                        queryLattice.price = lattice.price
                        queryLattice.rodHinderValue = lattice.rodHinderValue
                        queryLattice.sn = lattice.sn
                        queryLattice.smoke = lattice.smoke
                        queryLattice.sort = lattice.sort
                        queryLattice.sync = lattice.sync
                        queryLattice.volume = lattice.volume
                        queryLattice.weight = lattice.weight
                        val rowCabin =
                                DatabaseManager.upLatticeEntity(AppUtils.getContext(), queryLattice)
                        println("调试socket 更新箱体信息 $rowCabin")
                    }
                }
                //配置音量
                MediaPlayerHelper.setVolume(AppUtils.getContext(), 2)
                for (state in stateBox) {
                    val rowState = DatabaseManager.insertState(AppUtils.getContext(), state)
                    println("调试socket 添加心跳信息 $rowState")
                }
            }
        }

    }

    /***
     * @param resources 资源
     *
     */
    fun toGetResource(resources: List<ConfigRes>?) {
        ioScope.launch {
            println("调试socket 开始加载资源 ${Thread.currentThread().name}")
            resources?.let {
                for (resource in resources) {
                    val saveResource = ResEntity().apply {
                        filename = resource.filename
                        url = resource.url
                        md5 = resource.md5
                        time = AppUtils.getDateYMDHMS()
                    }
                    val queryResource =
                            DatabaseManager.queryRes(AppUtils.getContext(), resource.filename ?: "")
                    if (queryResource == null) {
                        val row = DatabaseManager.insertRes(AppUtils.getContext(), saveResource)
                        println("调试socket 添加资源 $row")
                    } else {
                        queryResource.filename = resource.filename
                        queryResource.url = resource.filename
                        queryResource.md5 = resource.md5
                        //资源不一致下载到本地
                        if (queryResource.md5 != resource.md5) {
                            val fileName = resource.filename ?: ""
                            var dir = FileMdUtil.matchNewFileName("audio", fileName)
                            if (shouldAudio(fileName)) {
                                dir = FileMdUtil.matchNewFileName("audio", fileName)
                            } else if (shouldPGJ(fileName)) {
                                dir = FileMdUtil.matchNewFileName("res", fileName)
                            }
                            queryResource.url?.let { url ->
                                downloadRes(url, dir) { success ->
                                    if (success) {
                                        queryResource.status = 0
                                        val row =
                                                DatabaseManager.upResEntity(AppUtils.getContext(), queryResource)
                                        println("调试socket 下载资源成功 ${queryResource.filename}")
                                        println("调试socket 下载资源成功 更新数据 $row")
                                    } else {
                                        queryResource.status = 1
                                        val row =
                                                DatabaseManager.upResEntity(AppUtils.getContext(), queryResource)
                                        println("调试socket 下载资源失败 $row")
                                        println("调试socket 下载资源失败 更新数据 $row")
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }


    private fun shouldAudio(fileName: String): Boolean {
        return fileName.contains("wav", ignoreCase = true) || fileName.contains("mp3", ignoreCase = true) || fileName.contains("mp4", ignoreCase = true)
    }

    private fun shouldPGJ(fileName: String): Boolean {
        return fileName.contains("png", ignoreCase = true) || fileName.contains("gif", ignoreCase = true) || fileName.contains("jpg", ignoreCase = true)
    }

    /***
     * 下载版本
     *
     */
    fun downloadRes(downloadUrl: String, filePath: String, callback: (Boolean) -> Unit) {
        singleDownloader = SingleDownloader(CorHttp.getInstance().getClient())
        singleDownloader?.onStart {
            Loge.d("网络请求 downloadRes onStart $downloadUrl")

        }?.onProgress { current, total, progress ->
            Loge.d("网络请求 downloadRes onProgress $current $total $progress")

        }?.onSuccess { url, file ->
            Loge.d("网络请求 downloadRes onSuccess $url ${file.path} $")
            callback(true)
        }?.onError { url, cause ->
            Loge.d("网络请求 downloadRes onError $url ${cause.message} ")
            callback(false)
        }?.onCompletion { url, filePath ->
            Loge.d("网络请求 downloadRes onCompletion $url $filePath ")

        }?.excute(downloadUrl, filePath)
    }

    /******************************************* socket通信 *************************************************/

    /*******************************************下位机通信部分*************************************************/

    //定时查询状态
    fun timingStatus() {
        println("调试串口 进来 timingStatus")
        ioScope.launch {
            while (isActive) {
                val commandType = commandQueue.receive()  // 从Channel中接收指令
                println("调试串口 进来 while $commandType")
                if (commandType == 0) {
                    CabinetSdk.queryStatus(lockerListStatusCallback, sendCallback)
                    //箱体状态查询
                    addQueueCommand(0)
                    delay(500)
                }

            }
        }
    }

    fun testPortResult(status: Int) {
        ioScope.launch {
            println("调试串口 通知锁状态 $status")
            if (status == 0) {
                flowDeliveryTypeEnd.emit(ResultType.RESULT301)
            } else if (status == 1) {
                flowDeliveryTypeEnd.emit(ResultType.RESULT310)
            }
        }
    }

    fun addQueueCommand(commandType: Int) {
        ioScope.launch {
            println("调试串口 进来 addQueueCommand $commandType")
            // 将指令依次加入队列
            commandQueue.send(commandType)
        }
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
