package com.recycling.toolsapp.ui

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.recycling.toolsapp.FaceApplication.Companion.enjoySDK
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTestSocketBinding
import com.recycling.toolsapp.fitsystembar.base.BaseActivity
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.socket.ConfigBean
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.CurrentActivity.Config.Companion.CURRENT_ROOM_TYPE
import com.recycling.toolsapp.utils.Define
import com.recycling.toolsapp.utils.IccidOper
import com.recycling.toolsapp.utils.TelephonyUtils
import com.recycling.toolsapp.utils.TelephonyUtils.getIccid2
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random


@AndroidEntryPoint class TestSocketFragment : BaseBindFragment<FragmentTestSocketBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    // 创建任务队列
    val taskQueue = PriorityTaskQueue<Int>()
    override fun layoutRes(): Int {
        return R.layout.fragment_test_socket
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }


    override fun initialize(savedInstanceState: Bundle?) {
        binding.tvConnect.setOnClickListener {
            initSocket()
        }
        binding.tvSend.setOnClickListener {
            cmdLogin()
        }
        binding.tvStop.setOnClickListener {
            cabinetVM.ioScope.launch {
                client?.stop()
                job?.cancel()
            }
        }
    }

    // 方法1：键值对转JSON字符串
    // 方法2：任意对象转JSON字符串
    fun convertToJsonString(obj: Any): String {
        return Gson().toJson(obj)
    }

    var job: Job? = null
    var client: SocketClient? = null
    val scope = CoroutineScope(Dispatchers.IO + Job())
    private fun initSocket() {
        cabinetVM.ioScope.launch {
            client =
                    SocketClient(SocketClient.Config(host = "58.251.251.79", port = 9095, heartbeatIntervalMillis = 10_000, heartbeatPayload = "PING".toByteArray()))
            job = scope.launch {
                client?.incoming?.collect { bytes ->
                    println("调试socket recv: ${String(bytes)}")
                    val json = String(bytes)
                    val cmd = CommandParser.parseCommand(json)
                    when (cmd) {

                        CmdValue.CMD_HEART_BEAT -> {
                            println("调试socket recv: 接收心跳成功")
                        }

                        CmdValue.CMD_LOGIN -> {
                            println("调试socket recv: 接收登录成功")
                            val loginModel = Gson().fromJson(json, ConfigBean::class.java)
                            val heartbeatIntervalMillis =
                                    loginModel.config.heartBeatInterval?.toLong() ?: 3
                            client?.config?.heartbeatIntervalMillis1 =
                                    TimeUnit.SECONDS.toMillis(heartbeatIntervalMillis)
                            client?.config?.heartbeatIntervalMillis1 = TimeUnit.SECONDS.toMillis(3)
                            client?.sendHeartbeat()
                        }

                        CmdValue.CMD_INIT_CONFIG -> {
                            val initConfigModel = Gson().fromJson(json, ConfigBean::class.java)
                            println("调试socket recv: 接收 initConfig 成功")
                        }

                        CmdValue.CMD_OPEN_DOOR -> {
                            println("调试socket recv: 接收 openDoor 成功")
                        }

                        CmdValue.CMD_CLOSE_DOOR -> {
                            println("调试socket recv: 接收 closeDoor成功")
                        }

                        CmdValue.CMD_PHONE_NUMBER_LOGIN -> {
                            println("调试socket recv: 接收 phoneNumberLogin 成功")
                        }

                        CmdValue.CMD_PHONE_USER_OPEN_DOOR -> {
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
            }
            client?.start()
            println("调试socket client = $client | state = ${client?.state}")
            client?.state?.collect {
                println("调试socket 连接状态: $it | ${Thread.currentThread().name}")
                when (it) {
                    ConnectionState.START -> {

                    }

                    ConnectionState.DISCONNECTED -> {

                    }

                    ConnectionState.CONNECTING -> {

                    }

                    ConnectionState.CONNECTED -> {
                        cmdLogin()
                    }
                }
            }
        }
    }

    private fun cmdLogin() {
        cabinetVM.ioScope.launch {
            val m =
                    mapOf("cmd" to "login", "sn" to "0136004ST00041", "imei" to "868408061812125", "iccid" to "898604A70821C0049781", "version" to "1.0.0", "timestamp" to System.currentTimeMillis())
            val json = convertToJsonString(m)
            client?.sendText(json)
        }
    }

    private fun getScreenParams(): String {
        val displayManager =
                (mActivity as BaseActivity).getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        val imei = TelephonyUtils.getImei(AppUtils.getContext())
        val imsi = TelephonyUtils.getImsi(AppUtils.getContext())
        val iccid = TelephonyUtils.getIccid(AppUtils.getContext())
        val pri = TelephonyUtils.getPrivilegedIds(AppUtils.getContext())
        val iccid2 = IccidOper.getInstance().GetIccid()

        val rotation = display.rotation
        val surfaceRotationDegrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val dm = DisplayMetrics()
        (mActivity as BaseActivity).windowManager.defaultDisplay.getMetrics(dm)
        val heightPixels = dm.heightPixels
        val widthPixels = dm.widthPixels
        val xdpi = dm.xdpi
        val ydpi = dm.ydpi
        val densityDpi = dm.densityDpi
        val density = dm.density
        val scaledDensity = dm.scaledDensity
        val heightDP = heightPixels / density
        val widthDP = widthPixels / density
        var str = "heightPixels: " + heightPixels + "px"
        str += """

             widthPixels: ${widthPixels}px
             """.trimIndent()
        str += """

             xdpi: ${xdpi}dpi
             """.trimIndent()
        str += """

             ydpi: ${ydpi}dpi
             """.trimIndent()
        str += """

             densityDpi: ${densityDpi}dpi
             """.trimIndent()
        str += "\ndensity: $density"
        str += "\nscaledDensity: $scaledDensity"
        str += """

             heightDP: ${heightDP}dp
             """.trimIndent()
        str += """

             widthDP: ${widthDP}dp
             """.trimIndent()

        str += """

             surfaceRotationDegrees: $surfaceRotationDegrees
             """.trimIndent()

        str += """

             dp: ${resources.getDimension(R.dimen.dp_25)}
             """.trimIndent()
        str += """

             imei: $imei:${pri.first}
             """.trimIndent()
        str += """

             imsi: $imsi:${pri.second}
             """.trimIndent()
        str += """

             iccid: $iccid:${pri.third}:${getIccid2(AppUtils.getContext())}:iccid2:${iccid2}
             """.trimIndent()
        return str
    }


    private fun createQueueTask(): Int {
        val task = Random.nextInt(1000, 10000)
        println("队列 createQueueTask task = $task")
        return task
    }

    private fun consumerQueueTask(task: Int) {
        println("队列 consumerQueueTask task = $task")
//        binding.show.text = "${getScreenParams()}\n task:$task"
        // 获取最后一次位置
        val lastLocation = LocationHelper.getLastKnownLocation(AppUtils.getContext())
        val listener = LocationHelper.requestLocationUpdates(AppUtils.getContext()) { location ->
            println("纬度：${location.latitude}经度：${location.longitude}")
        }
        LocationHelper.removeUpdates(AppUtils.getContext(), listener)
    }

    override fun onResume() {
        super.onResume()
        enjoySDK.setHomePackage("com.recycling.toolsapp")
    }

    override fun onFragmentResume() {
        super.onFragmentResume()
        CURRENT_ROOM_TYPE = Define.ACTIVITY_TYPE_MAIN
        enjoySDK.setHomePackage("com.recycling.toolsapp")
    }

    /*******************************************超时归还提醒***************************************************/

}
