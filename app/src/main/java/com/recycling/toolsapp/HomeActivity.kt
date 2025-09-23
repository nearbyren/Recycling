package com.recycling.toolsapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.recycling.toolsapp.FaceApplication.Companion.networkMonitor
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.recycling.toolsapp.databinding.ActivityHomeBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindActivity
import com.recycling.toolsapp.socket.DoorOpenBean
import com.recycling.toolsapp.socket.ConfigBean
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.ui.Camera2Fragment
import com.recycling.toolsapp.ui.DeliveryFragment
import com.recycling.toolsapp.ui.TouSingleFragment
import com.recycling.toolsapp.ui.TouDoubleFragment
import com.recycling.toolsapp.utils.CmdType
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.utils.ResultType
import com.recycling.toolsapp.utils.SnackbarUtils
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.CmdCode
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus
import java.io.File

@AndroidEntryPoint class HomeActivity : BaseBindActivity<ActivityHomeBinding>() {
    private val cabinetVM: CabinetVM by viewModels()
    private var isNetworkStatusFirst = true
    override fun layoutRes(): Int {
        return R.layout.activity_home
    }

    @RequiresApi(Build.VERSION_CODES.M) private fun initNetworkState() {
        //全局处理提示信息
        lifecycleScope.launch {
            cabinetVM._isNetworkMsg.collect {
                Loge.d("全局提示信息  $it ${Thread.currentThread().name}")
                SnackbarUtils.show(activity = this@HomeActivity, message = it, duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
            }

        }
        LiveBus.get(BusType.BUS_NET_MSG).observeForever {
            Loge.d("网络导入用户信息 用户信息异常")
            SnackbarUtils.show(activity = this@HomeActivity, message = it.toString(), duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
        }
        networkMonitor.register()
        // 注册监听（传统回调方式）
        networkMonitor.addNetworkStateListener { isConnected ->
            runOnUiThread {
                updateNetworkStatus(isConnected)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M) override fun initialize(savedInstanceState: Bundle?) {
        initNetworkState()
        initDoorStatus()
        initPort()
        cabinetVM.ioScope.launch {
            // 预热相机Provider 快速启动相机能从6秒到2秒
            cabinetVM.cameraProviderFuture = ProcessCameraProvider.getInstance(this@HomeActivity)
            cabinetVM.cameraProviderFuture2 =
                    ProcessCameraProvider.getInstance(this@HomeActivity).get()
        }
        val initSocket = SPreUtil.get(AppUtils.getContext(), "initSocket", false) as Boolean
        if (initSocket) {
            initSocket()
        }

//        countdownUI()
//        lifeUpgradeApk()
//        netUpdateApk()

//        lifeUpgradeChip()
//        netUpdateChip()
    }

    private fun initPort() {
        cabinetVM.startPollingDoor()
        cabinetVM.addDoorQueue(CmdType.CMD5)
//        cabinetVM.requestStatusQuery()
//        cabinetVM.startNewDoor()
    }

    /***
     * 格口 打开 满溢 故障 正常
     */
    private fun initDoorStatus() {
        //门口打开计重页
        lifecycleScope.launch {
            cabinetVM.isOpenDoor.collect {
                Loge.d("调试socket 收到开仓指令 $it")
                if (it) {
                    navigateTo(fragmentClass = DeliveryFragment::class.java)
                }
            }
        }

        //格口一状态提示
        lifecycleScope.launch {
            cabinetVM.isDoor1Value.collect { value ->
                Loge.d("调试socket 格口1状态 $value")
                when (value) {
                    BusType.BUS_OVERFLOW -> {
                        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).post(BusType.BUS_OVERFLOW)
                    }

                    BusType.BUS_FAULT -> {
                        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).post(BusType.BUS_FAULT)
                    }

                    BusType.BUS_NORMAL -> {
                        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).post(BusType.BUS_NORMAL)
                    }
                }
            }
        }
        //格口二状态提示
        lifecycleScope.launch {
            cabinetVM.isDoor2Value.collect { value ->
                Loge.d("调试socket 格口2状态 $value")
                when (value) {
                    BusType.BUS_OVERFLOW -> {
                        LiveBus.get(BusType.BUS_TOU2_DOOR_STATUS).post(BusType.BUS_OVERFLOW)
                    }

                    BusType.BUS_FAULT -> {
                        LiveBus.get(BusType.BUS_TOU2_DOOR_STATUS).post(BusType.BUS_FAULT)
                    }

                    BusType.BUS_NORMAL -> {
                        LiveBus.get(BusType.BUS_TOU2_DOOR_STATUS).post(BusType.BUS_NORMAL)
                    }
                }
            }
        }
//        //查询重量回来
//        lifecycleScope.launch {
//            cabinetVM.getCurWeight.collect { value ->
//                Loge.d("调试socket 重量查询回来 $value")
//                if (value) {
//                    cabinetVM.toGoUpDoorClose()
//                }
//            }
//        }
        //查询重量回来
        lifecycleScope.launch {
            cabinetVM.getCurWeightType.collect { value ->
                Loge.d("调试socket 调试串口 UI 重量查询回来 $value")
                when (value) {
                    //前
                    0 -> {
                        cabinetVM.queryBeforeWeight()
                    }
                    //后
                    1 -> {
                        cabinetVM.toGoUpDoorClose()
                    }
                }

            }
        }

        //门开 门关 状态上报
        lifecycleScope.launch {
            cabinetVM.isDeliveryTypeEnd.collect { endType ->
                //开启定时查询门状态是否关闭完成，后续上传业务
                Loge.d("调试socket 调试串口 接收到称重页结束类型 $endType")
                when (endType) {
                    //点击
                    ResultType.RESULT1 -> {
//                        cabinetVM.testSendCmd(CmdCode.GE_CLOSE)
                        cabinetVM.testSendCmd2(CmdCode.GE_CLOSE)
                    }
                    //倒计时结束
                    ResultType.RESULT2 -> {
//                        cabinetVM.testSendCmd(CmdCode.GE_CLOSE)
                        cabinetVM.testSendCmd2(CmdCode.GE_CLOSE)
                    }
                    //门已经开了
                    ResultType.RESULT310 -> {
//                        cabinetVM.testToGoDownDoorOpen()
                        //门已经开了 通知服务器
                        cabinetVM.sendUpRec(CmdValue.CMD_OPEN_DOOR)
                    }

                    //门已经关闭
                    ResultType.RESULT301 -> {
                        //门已经关闭了 通知服务器
                        cabinetVM.sendUpRec(CmdValue.CMD_CLOSE_DOOR)
                    }
                }
            }
        }
        manager = supportFragmentManager
        //接收启动相机
        lifecycleScope.launch {
            cabinetVM.getStartCamera.collect { cType ->
                when (cType) {
                    0 -> {//移除
                        Loge.d("调试socket 调试串口 添加预览相机 ")
                        removeCamera()
                    }

                    1 -> {//添加
                        Loge.d("调试socket 调试串口 移除预览相机")
                        addCamera()
                    }
                }
            }
        }
    }

    var manager: FragmentManager? = null
    fun addCamera() {
        manager?.let {
            val beginTransaction = it.beginTransaction()
            val f = Camera2Fragment()
            f?.let { fragment ->
                beginTransaction.add(R.id.fl_camera2, fragment, "camera2")
                beginTransaction.commit()
            }
        }
    }

    fun removeCamera() {
        manager?.let { mg ->
            val fragment =   mg.findFragmentByTag("camera2")
            fragment?.let {ft->
                mg.beginTransaction().remove(ft).commit()
            }
        }
    }

    /***
     * socket 连接 和 接收服务器下发
     */
    private fun initSocket() {
        //socket 登录成功加载页面
        lifecycleScope.launch {
            cabinetVM.getLoginCmd.collect {
                if (it) {
                    println("调试socket saveInitNet 加载fragment")
                    binding.acivInit.isVisible = false
                    toGoUi()
                }
            }
        }
        //socket 监听是否连接成功 接收服务器下发
        lifecycleScope.launch {
            cabinetVM.vmClient = SocketManager.socketClient
            val state = cabinetVM.vmClient?.state?.value ?: ConnectionState.DISCONNECTED
            println("调试socket OneActivity 当前线程：${Thread.currentThread().name} | state $state")
            when (state) {
                ConnectionState.START -> {
                    println("调试socket OneActivity 取 开始：${Thread.currentThread().name} | state $state")

                }

                ConnectionState.DISCONNECTED -> {
                    println("调试socket OneActivity 取 已断开连接：${Thread.currentThread().name} | state $state")

                }

                ConnectionState.CONNECTING -> {
                    println("调试socket OneActivity 取 正在连接：${Thread.currentThread().name} | state $state")
                }

                ConnectionState.CONNECTED -> {
                    println("调试socket OneActivity 取 已连接：${Thread.currentThread().name} | state $state")
                    cabinetVM.toGoCmdLogin()
                }
            }
            println("调试socket lifecycleScope ${Thread.currentThread().name} vmClient = ${cabinetVM.vmClient} | state = ${cabinetVM.vmClient?.state}")
            cabinetVM.vmClient?.incoming?.collect { bytes ->
                println("调试socket recv: ${String(bytes)}")
                val json = String(bytes)
                BoxToolLogUtils.recordSocket(CmdValue.RECEIVE, json)
                val cmd = CommandParser.parseCommand(json)

                when (cmd) {
                    CmdValue.CMD_HEART_BEAT -> {

                    }

                    CmdValue.CMD_LOGIN -> {
                        val loginModel = Gson().fromJson(json, ConfigBean::class.java)
                        cabinetVM.saveInitNet(loginModel, false)
                    }

                    CmdValue.CMD_INIT_CONFIG -> {
                        val initConfigModel = Gson().fromJson(json, ConfigBean::class.java)
                        cabinetVM.saveInitNet(initConfigModel, true)
                    }

                    CmdValue.CMD_OPEN_DOOR -> {
                        val doorOpenModel = Gson().fromJson(json, DoorOpenBean::class.java)
                        cabinetVM.toGoSweepCodeCode(doorOpenModel)
                    }

                    CmdValue.CMD_CLOSE_DOOR -> {
                        cabinetVM.refreshWeightStatus()
                    }

                    CmdValue.CMD_PHONE_NUMBER_LOGIN -> {
                        val doorOpenModel = Gson().fromJson(json, DoorOpenBean::class.java)
                        cabinetVM.toGoMobileOpen(cabinetVM.cur1Cabinld, doorOpenModel.userId ?: "")
                    }

                    CmdValue.CMD_RESTART -> {
                        HexConverter.restartApp2(AppUtils.getContext(), 3 * 1000L)
                    }

                    CmdValue.CMD_UPLOAD_LOG -> {
                    }

                    CmdValue.CMD_OTA -> {
                    }
                }
            }
            cabinetVM.vmClient?.state?.collect {
                println("调试socket 连接状态: $it | ${Thread.currentThread().name}")
                when (it) {
                    ConnectionState.START -> {
                        println("调试socket OneActivity 监 开始：${Thread.currentThread().name} | state $state")

                    }

                    ConnectionState.DISCONNECTED -> {
                        println("调试socket OneActivity 监 已断开连接：${Thread.currentThread().name} | state $state")

                    }

                    ConnectionState.CONNECTING -> {
                        println("调试socket OneActivity 监 正在连接：${Thread.currentThread().name} | state $state")
                    }

                    ConnectionState.CONNECTED -> {
                        println("调试socket OneActivity 监 已连接：${Thread.currentThread().name} | state $state")
                        cabinetVM.toGoCmdLogin()
                    }
                }
            }
        }
    }

    fun toGoUi() {
        intent.data?.toString()?.let { deepLink ->
            if (!fragmentCoordinator.handleDeepLink(deepLink)) {
                navigateToHome()
            }
        } ?: navigateToHome()
    }

    private fun getMasterVersion(isUpgrade: Boolean = false) {
        cabinetVM.executeVersion232(isUpgrade, byteArrayOf(0xAA.toByte(), 0xAB.toByte(), 0xAC.toByte()), onUpgrade232 = { version ->
            cabinetVM.mainScope.launch {
//                binding.tvUpgradeMasterCode.text = "主芯片版本：$version"
                cabinetVM.chipMasterVC = version
                if (isUpgrade) {
                }
            }
        })

    }

    private fun netUpdateApk() {
        cabinetVM.downloadApk("", System.currentTimeMillis().toString())
    }

    private fun lifeUpgradeApk() {
        //触发有版本更新
        lifecycleScope.launch {
            cabinetVM.isUpdateCollect.collect {
                if (it) {
                    val versionInfo = cabinetVM.versionInfoCollect.value
                    if (versionInfo.hasUpdate) {
//                        binding.clUpdateVersion.isVisible = true
//                        binding.tvDescription.text = "${versionInfo.description}"
                    }
                } else {
//                    SnackbarUtils.show(fragment = this, message = "没有最新版本更新", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)

                }
            }
        }
        lifecycleScope.launch {
            cabinetVM.downErrorCollect.collect {
                if (it) {
//                    binding.clUpdateVersion.isVisible = false
//                    binding.versionProgress.setProgress(0.toFloat(), true)
                }
            }
        }
        //触发安装
        lifecycleScope.launch {
            cabinetVM.downSuccessCollect.collect { file ->
//                binding.clUpdateVersion.isVisible = false
                installApk(file)
            }
        }
        //触发下载进度
        lifecycleScope.launch {
            cabinetVM.downProgressCollect.collect {
//                binding.versionProgress.setProgress(it, true)
            }
        }
    }

    private fun installApk(file: File) {
        try {
            // 触发安装
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(baseContext, "${baseContext.packageName}.fileProvider", file)
                } else {
                    Uri.fromFile(file)
                }
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            startActivity(intent)
        } catch (e: Throwable) {
//            SnackbarUtils.show(fragment = , message = "安装失败", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
        }
    }

    private fun netUpdateChip() {
        cabinetVM.downloadChip("https://cdn.chenair.com/d/down/nvdata_bin.zip", "masterChip")
    }

    //固件下载和升级监听
    private fun lifeUpgradeChip() {
        //下载完成
        lifecycleScope.launch {
            cabinetVM.IsChipSuccess.collect {
                if (it) {
                    Loge.d("芯片升级 主从芯片下载完成")
//                    binding.clFirmwareUpgrade.isVisible = true
//                    binding.tvFirmwareTitle.text = "固件升级中"
//                    binding.tvFirmwareDes.text = "正在升级固件中，耐心等待，请勿进行其他操作。"
                    if (cabinetVM.chipMasterV > cabinetVM.chipMasterVC) {
                        cabinetVM.upgradeChip()
                    } else {
                        cabinetVM.tipMessage("当前已经是最新版本")
                    }
                } else {
                    Loge.d("芯片升级 主从芯片未下载完成")
                }
            }
        }
        //232方式执行步骤升级
        lifecycleScope.launch {
            cabinetVM.isFlowSteps7.collect {
                if (it) {
//                    binding.clFirmwareUpgrade.isVisible = true
//                    binding.tvFirmwareTitle.text = "固件升级中"
//                    binding.tvFirmwareDes.text = "正在升级主芯片固件中，耐心等待，请勿进行其他操作。"
                    Loge.d("芯片升级 主芯片升级 接收指令7 来了回调")
                    cabinetVM.chipSet7()
                } else {
//                    binding.clFirmwareUpgrade.isVisible = false
                    Loge.d("芯片升级 主芯片升级 接收指令7 没来回调")
                }
            }
        }

        lifecycleScope.launch {
            cabinetVM.isFlowSteps8.collect {
                if (it) {
                    Loge.d("芯片升级 主芯片升级 接收指令8 来了回调")
                    cabinetVM.chipSet8()
                } else {
                    Loge.d("芯片升级 主芯片升级 接收指令8 没来回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                }
            }
        }

        lifecycleScope.launch {
            cabinetVM.isFlowSteps8f.collect {
                if (it) {
                    Loge.d("芯片升级 主芯片升级 接收指令8f 来了回调")
                    cabinetVM.chipSet8f()
                } else {
                    Loge.d("芯片升级 主芯片升级 接收指令8f 没来回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                }
            }
        }

        lifecycleScope.launch {
            cabinetVM.isFlowSteps9.collect {
                if (it) {
                    Loge.d("芯片升级 主芯片升级 接收指令9 来了回调")
                    delay(5000)
                    cabinetVM.chipSet9()
                } else {
                    Loge.d("芯片升级 主芯片升级 接收指令9 没来回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                }
            }
        }

        lifecycleScope.launch {
            cabinetVM.isFlowSteps10.collect {
                if (it) {
                    Loge.d("芯片升级 主芯片升级 接收指令10 来了回调")
                    cabinetVM.chipSet10()
                } else {
                    Loge.d("芯片升级 主芯片升级 接收指令10 没来回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                }
            }
        }

        lifecycleScope.launch {
            cabinetVM.isFlowSteps232Succes.collect {
                if (it) {
                    Loge.d("芯片升级 主芯片升级 接收指令success 来了回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                } else {
                    Loge.d("芯片升级 主芯片升级 接收指令success 没来回调")
//                    binding.clFirmwareUpgrade.isVisible = false
                }
            }
        }
    }

    private fun countdownUI() {
        // 在 Activity/Fragment 中收集状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cabinetVM.countdownState.collect { state ->
                    when (state) {
                        is CountdownTimer.CountdownState.Starting -> {
//                            binding.clFirmwareUpgrade.isVisible = true
//                            binding.tvFirmwareTitle.text =
//                                    if (cabinetVM.currentUpgrade == 1) "固件升级失败" else "固件升级完成"
                        }

                        is CountdownTimer.CountdownState.Running -> {
                            // 更新 UI
//                            binding.tvFirmwareDes.text =
//                                    if (cabinetVM.currentUpgrade == 1) "本次升级固件失败，请尝试重新升级" else "升级已经完成，计时${state.secondsRemaining}秒，即将重APP。"
                        }

                        CountdownTimer.CountdownState.Finished -> {
//                            if (cabinetVM.currentUpgrade == 1) {
//                                binding.clFirmwareUpgrade.isVisible = false
//                            } else {
                            HexConverter.restartApp2(AppUtils.getContext(), 2 * 1000L)
//                            }
                        }

                        is CountdownTimer.CountdownState.Error -> {
                            cabinetVM.tipMessage(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun updateNetworkStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.acivSignal.setBackgroundResource(R.drawable.ic_xinhao1)
            if (!isNetworkStatusFirst) {
                isNetworkStatusFirst = false
                // 网络已连接
                SnackbarUtils.show(activity = this@HomeActivity, message = "网络状态已打开", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
                binding.tvNetwork.text = "网络已经连接"
            } else {
                binding.tvNetwork.text = "网络已经连接"
            }
        } else {
            binding.acivSignal.setBackgroundResource(R.drawable.ic_xinhao0)
            // 网络断开
            if (!isNetworkStatusFirst) {
                isNetworkStatusFirst = false
                SnackbarUtils.show(activity = this@HomeActivity, message = "网络状态已断开", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
                binding.tvNetwork.text = "网络已经断开"
            } else {
                binding.tvNetwork.text = "网络已经断开"
            }
        }
    }

    private fun navigateToHome() {
        val typeGrid = SPreUtil[AppUtils.getContext(), "type_grid", -1]
        println("调试socket navigateToHome $typeGrid")
        val initSn = SPreUtil[AppUtils.getContext(), "init_sn", "init_sn"]
        val loginSn = SPreUtil[AppUtils.getContext(), "login_sn", "login_sn"]
        if (initSn == loginSn) {
            binding.tvSn.text = "sn：$initSn"
        } else {
            binding.tvSn.text = "sn：$initSn|$loginSn"
        }
        binding.tvVersion.text = "版本号：v${AppUtils.getVersionName()}"
        when (typeGrid) {
            1, 3 -> {
                navigateTo(fragmentClass = TouSingleFragment::class.java, addToBackStack = true)
            }

            2 -> {
                navigateTo(fragmentClass = TouDoubleFragment::class.java, addToBackStack = true)
            }
        }
    }

    fun hide() {
        hideActionBar()
        hideActionBarBack()
    }

    override fun onDetachedFromWindow() {
        println("调试socket home onDestroy")
        cabinetVM.closeSock()
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        println("调试socket home onDestroy")
        cabinetVM.closeSock()
        super.onDestroy()
    }
}