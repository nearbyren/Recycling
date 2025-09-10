package com.recycling.toolsapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.recycling.toolsapp.FaceApplication.Companion.networkMonitor
import com.recycling.toolsapp.databinding.ActivityOneBinding
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindActivity
import com.recycling.toolsapp.socket.DoorOpenDto
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.ui.TouSingleFragment
import com.recycling.toolsapp.ui.TouDoubleFragment
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.FragmentCoordinator
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.utils.SnackbarUtils
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil
import nearby.lib.signal.livebus.LiveBus
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint class OneActivity : BaseBindActivity<ActivityOneBinding>() {

    private val cabinetVM: CabinetVM by viewModels()
    private var downTime = 0L
    private var isNetworkStatusFirst = true
    override fun layoutRes(): Int {
        return R.layout.activity_one
    }

    @RequiresApi(Build.VERSION_CODES.M) override fun initialize(savedInstanceState: Bundle?) {
        // 处理深度链接
        intent.data?.toString()?.let { deepLink ->
            if (!fragmentCoordinator.handleDeepLink(deepLink)) {
                // 如果没有匹配的深度链接，打开首页
                navigateToHome()
            }
        } ?: navigateToHome()

        initNetworkState()
//        countdownUI()
//        lifeUpgradeApk()
//        netUpdateApk()

//        lifeUpgradeChip()
//        netUpdateChip()

        val initSocket = SPreUtil.get(AppUtils.getContext(), "initSocket", false) as Boolean
        if (initSocket) {
            initSocket()
        }
    }


    @RequiresApi(Build.VERSION_CODES.M) private fun initNetworkState() {
        //全局处理提示信息
        lifecycleScope.launch {
            cabinetVM._isNetworkMessage.collect {
                Loge.d("全局提示信息  $it ${Thread.currentThread().name}")
                SnackbarUtils.show(activity = this@OneActivity, message = it, duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
            }
        }
        LiveBus.get("netMessage").observeForever {
            Loge.d("网络导入用户信息 用户信息异常")
            SnackbarUtils.show(activity = this@OneActivity, message = it.toString(), duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
        }
        networkMonitor.register()
        // 注册监听（传统回调方式）
        networkMonitor.addNetworkStateListener { isConnected ->
            runOnUiThread {
                updateNetworkStatus(isConnected)
            }
        }
    }

    private fun initSocket() {
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
                val cmd = CommandParser.parseCommand(json)
                when (cmd) {

                    "heartBeat" -> {
                        println("调试socket recv: 接收心跳成功")
                    }

                    "login" -> {
                        println("调试socket recv: 接收登录成功")
                        val loginModel = Gson().fromJson(json, LoginDto::class.java)
                        val heartbeatIntervalMillis =
                                loginModel.config.heartBeatInterval?.toLong() ?: 10
                        cabinetVM.vmClient?.config?.heartbeatIntervalMillis1 =
                                TimeUnit.SECONDS.toMillis(heartbeatIntervalMillis)
                        println("调试socket recv: 心跳秒：$heartbeatIntervalMillis")
                        cabinetVM.vmClient?.config?.heartbeatIntervalMillis1 =
                                TimeUnit.SECONDS.toMillis(10)
                        //保存所有配置
                        loginModel.sn?.let { sn ->
                            cabinetVM.toGetSaveConfigEntity(sn, loginModel.config)
                        }
                        //保存箱体
                        cabinetVM.toGetSaveCabins(loginModel.config.list)
                        delay(500)
                        //发送心跳
                        cabinetVM.vmClient?.sendHeartbeat()
                    }

                    "initConfig" -> {
                        val initConfigModel = Gson().fromJson(json, InitConfigDto::class.java)
                        println("调试socket recv: 接收 initConfig 成功")
                    }

                    "openDoor" -> {
                        println("调试socket recv: 接收 openDoor 成功")
                        val doorOpenModel = Gson().fromJson(json, DoorOpenDto::class.java)
                        cabinetVM.toGoDownDoorOpen(doorOpenModel)
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


    private fun open() {
        cabinetVM.issuedCmd(1, onResponseResult = { type, openStatus, success ->

        })
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
            if (!isNetworkStatusFirst) {
                isNetworkStatusFirst = false
                // 网络已连接
                SnackbarUtils.show(activity = this@OneActivity, message = "网络状态已打开", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
            } else {
                binding.tvNetwork.text = "网络已经连接"
            }
        } else {
            // 网络断开
            if (!isNetworkStatusFirst) {
                isNetworkStatusFirst = false
                SnackbarUtils.show(activity = this@OneActivity, message = "网络状态已断开", duration = Snackbar.LENGTH_LONG, textColor = Color.WHITE, textAlignment = View.TEXT_ALIGNMENT_CENTER, horizontalCenter = true, position = SnackbarUtils.Position.CENTER)
            }
        }
    }

    private fun navigateToHome() {
        val typeGrid = SPreUtil[AppUtils.getContext(), "type_grid", -1]
        println("调试socket navigateToHome $typeGrid")
        val sn = SPreUtil[AppUtils.getContext(), "sn", ""]
        val typeText = when (typeGrid) {
            1 -> {
                "单口"
            }

            2 -> {
                "双口"
            }

            3 -> {
                "子母口"
            }

            else -> {
                "-"
            }
        }
        binding.tvSn.text = "$typeText sn：$sn"
        binding.tvVersion.text = "版本号：v${AppUtils.getVersionName()}"
        when (typeGrid) {
            1, 3 -> {
                navigateTo(fragmentClass = TouSingleFragment::class.java, addToBackStack = true, lifecycleCallback = object : FragmentCoordinator.FragmentLifecycleCallback {
                    override fun onFragmentResumed(fragment: Fragment) {
                        super.onFragmentResumed(fragment)
                        hide()
                    }
                })
            }

            2 -> {
                navigateTo(fragmentClass = TouDoubleFragment::class.java, addToBackStack = true, lifecycleCallback = object : FragmentCoordinator.FragmentLifecycleCallback {
                    override fun onFragmentResumed(fragment: Fragment) {
                        super.onFragmentResumed(fragment)
                        hide()
                    }
                })
            }
        }
    }

    fun hide() {
        hideActionBar()
        hideActionBarBack()
    }

}