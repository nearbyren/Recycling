package com.recycling.toolsapp

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.utils.TelephonyUtils
import com.recycling.toolsapp.utils.TelephonyUtils.getIccid2
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil

/***
 * 这里考虑优化启动逻辑处理问题
 */
class StartUiActivity : AppCompatActivity() {
    private val cabinetVM: CabinetVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_ui)
        //这里http获取业务ID
        CoroutineScope(Dispatchers.Main).launch {
            val init = SPreUtil[AppUtils.getContext(), "init", false] as Boolean
            if (init) {
                val host = SPreUtil[AppUtils.getContext(), "host", "58.251.251.79"] as String
                val port = SPreUtil[AppUtils.getContext(), "port", 9095] as Int
                Loge.e("调试socket startUI 进入主界面 $host $port")
                initSocket(host, port)
//                startActivity(Intent(this@StartUiActivity, HomeActivity::class.java))
            } else {
                Loge.e("调试socket startUI 进入初始化")
                startActivity(Intent(this@StartUiActivity, InitFactoryActivity::class.java))
            }
        }
        Loge.e("屏幕尺寸大小 ：${getScreenParams()}")
    }

    private fun initSocket(mHost: String? = "58.251.251.79", mPort: Int? = 9095) {
        cabinetVM.ioScope.launch {
            if (mHost != null && mPort != null) {
                SocketManager.initializeSocketClient(host = mHost, port = mPort)
                cabinetVM.vmClient = SocketManager.socketClient
                SocketManager.socketClient.start()
                delay(500)
                val state = cabinetVM.vmClient?.state?.value ?: ConnectionState.DISCONNECTED
                Loge.e("调试socket startUI 当前线程：${Thread.currentThread().name} | state $state")
                BoxToolLogUtils.recordSocket(CmdValue.CONNECTING, "start,${state.name}")
                when (state) {
                    ConnectionState.START -> {

                    }

                    ConnectionState.DISCONNECTED -> {

                    }

                    ConnectionState.CONNECTING -> {

                    }

                    ConnectionState.CONNECTED -> {
                        if (mHost != null) {
                            SPreUtil.put(AppUtils.getContext(), "host", mHost)
                        }
                        if (mPort != null) {
                            SPreUtil.put(AppUtils.getContext(), "port", mPort)
                        }
                        startActivity(Intent(this@StartUiActivity, HomeActivity::class.java))
                    }
                }
            }
        }
    }

    private fun getScreenParams(): String {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        val imei = TelephonyUtils.getImei(AppUtils.getContext())
        val imsi = TelephonyUtils.getImsi(AppUtils.getContext())
        val iccid = TelephonyUtils.getIccid(AppUtils.getContext())
//        val pri = TelephonyUtils.getPrivilegedIds(AppUtils.getContext())
//        val iccid2 = IccidOper.getInstance().GetIccid()

        val rotation = display.rotation
        val surfaceRotationDegrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
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

             imei: $imei
             """.trimIndent()
        str += """

             imsi: $imsi
             """.trimIndent()
        str += """

             iccid: $iccid
             """.trimIndent()
        return str
    }
}