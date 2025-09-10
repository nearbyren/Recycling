package com.recycling.toolsapp

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.recycling.toolsapp.fitsystembar.base.BaseActivity
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.IccidOper
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.utils.TelephonyUtils
import com.recycling.toolsapp.utils.TelephonyUtils.getIccid2
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil
import java.util.regex.Pattern

/***
 * 出厂配置
 */
class InitFactoryActivity : AppCompatActivity() {
    private val cabinetVM: CabinetVM by viewModels()
    private var acetSn: AppCompatEditText? = null
    private var group: RadioGroup? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init_fractory)
        acetSn = findViewById(R.id.acet_sn)
        acetSn?.setAlphanumericLimit()
        group = findViewById(R.id.rgBuckleType)
        val selectedId = group?.checkedRadioButtonId
        selectedId?.let { sid ->
            selectedText(sid)
        }
        val clInit = findViewById<ConstraintLayout>(R.id.cl_init)
        clInit.setOnClickListener {
            println("调试socket 点击初始化 ")
            test()   //调试1
//            initSocket()//调试2
        }
        group?.setOnCheckedChangeListener { _, checkedId ->
            selectedText(checkedId)
        }
        println("屏幕尺寸大小 ：${getScreenParams()}")
    }

    private fun test() {
        //SPreUtil.put(AppUtils.getContext(), "init", true)
        SPreUtil.put(AppUtils.getContext(), "initSocket", false)
        val snText = acetSn?.text.toString()
        SPreUtil.put(AppUtils.getContext(), "sn", snText)
        startActivity(Intent(this@InitFactoryActivity, OneActivity::class.java))
        finish()
    }

    fun selectedText(checkedId: Int) {
        val selected = when (checkedId) {
            R.id.rbSingle -> "单口"
            R.id.rbDouble -> "双口"
            R.id.rbParentChild -> "子母口"
            else -> null
        }
        when (selected) {
            "单口" -> {
                SPreUtil.put(AppUtils.getContext(), "type_grid", 1)
            }

            "双口" -> {
                SPreUtil.put(AppUtils.getContext(), "type_grid", 2)
            }

            "子母口" -> {
                SPreUtil.put(AppUtils.getContext(), "type_grid", 3)
            }
        }
    }

    fun AppCompatEditText.setAlphanumericLimit() {
        // 输入过滤器
        filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val pattern = Pattern.compile("[^a-zA-Z0-9]")
            if (pattern.matcher(source).find()) "" else null
        }, InputFilter.LengthFilter(31) // 最大31个字符（小于32）
        )

        // 实时验证
        addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().matches(Regex("^[a-zA-Z0-9]*$"))) {
                    error = "只允许字母和数字"
                }
            }
        })
    }

    abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }


    private fun initSocket() {
        cabinetVM.ioScope.launch {
            SPreUtil.put(AppUtils.getContext(), "initSocket", true)
            SocketManager.initializeSocketClient(host = "58.251.251.79", port = 9095)
            cabinetVM.vmClient = SocketManager.socketClient
            SocketManager.socketClient.start()
            delay(500)
            val state = cabinetVM.vmClient?.state?.value ?: ConnectionState.DISCONNECTED
            println("调试socket startUI 当前线程：${Thread.currentThread().name} | state $state")
            when (state) {
                ConnectionState.START -> {

                }

                ConnectionState.DISCONNECTED -> {

                }

                ConnectionState.CONNECTING -> {

                }

                ConnectionState.CONNECTED -> {
                    startActivity(Intent(this@InitFactoryActivity, OneActivity::class.java))
                    finish()
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
//        str += """
//
//             imei: $imei:${pri.first}
//             """.trimIndent()
//        str += """
//
//             imsi: $imsi:${pri.second}
//             """.trimIndent()
//        str += """
//
//             iccid: $iccid:${pri.third}:${getIccid2(AppUtils.getContext())}:iccid2:${iccid2}
//             """.trimIndent()
        return str
    }
}