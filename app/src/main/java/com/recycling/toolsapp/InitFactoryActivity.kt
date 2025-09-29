package com.recycling.toolsapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.recycling.toolsapp.http.RepoImpl
import com.recycling.toolsapp.http.TaskRestartScheduler
import com.recycling.toolsapp.model.LogEntity
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.utils.TelephonyUtils
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil
import java.io.File
import java.util.regex.Pattern

/***
 * 出厂配置
 * 这里考虑用fragment去构建
 */
class InitFactoryActivity : AppCompatActivity() {
    private val cabinetVM: CabinetVM by viewModels()
    private var acetSn: AppCompatEditText? = null
    private var acetDealerId: AppCompatEditText? = null
    private var acetDeptId: AppCompatEditText? = null
    private var acetQr: AppCompatEditText? = null
    private var acivInit: AppCompatImageView? = null
    private var rgLattice: RadioGroup? = null
    private var acetFlowCard: AppCompatTextView? = null
    private var mRecycleType: Int = -1
    private var dialog: SimpleCalendarDialogFragment? = null
    private var selectedDateStr: String? = null
    private val httpRepo by lazy { RepoImpl() }
    private fun newInit() {
        acivInit = findViewById(R.id.aciv_init)
        val init = SPreUtil[AppUtils.getContext(), "init", false] as Boolean
        if (init) {
            Loge.e("调试socket startUI 进入主界面")
            initSocket()
        } else {
            Loge.e("调试socket startUI 进入初始化")
            acivInit?.isVisible = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init_fractory)
//        newInit()
        initFactory()
    }


    private fun onSimpleCalendarDialogClick() {
        supportFragmentManager.let {
            dialog = SimpleCalendarDialogFragment()
            dialog?.show(it, "show-date-calendar")
            //            dialog?.setButtonSize()
            dialog?.setOnDateSelectedListener(object : SimpleCalendarDialogFragment.OnDateSelectedListener {
                @RequiresApi(Build.VERSION_CODES.O) override fun onDateSelected(calendarDay: CalendarDay) {
                    val selectedDateStr = "${calendarDay.year}-${
                        calendarDay.month.toString().padStart(2, '0')
                    }-${
                        calendarDay.day.toString().padStart(2, '0')
                    }"
                    Loge.e("selectedDateStr $selectedDateStr")
                    acetFlowCard?.text = selectedDateStr
                    val dealerId = acetDealerId?.text.toString()
                    val deptId = acetDeptId?.text.toString()
                    TaskRestartScheduler.scheduleSpecificDate(AppUtils.getContext(), selectedDateStr, "${dealerId}:${deptId}", "christmas_special")
                    TaskRestartScheduler.triggerImmediately(AppUtils.getContext(), "urgent_cleanup")
                    //                    val tools = binding.toolsSearch.text.toString()
                    //                    binding.dateSearch.text = selectedDateStr
                    //                    filterData(tools, selectedDateStr)
                }
            })

        }
    }

    class SimpleCalendarDialogFragment : AppCompatDialogFragment(), OnDateSelectedListener {
        private var listener: OnDateSelectedListener? = null
        private var selectedDate: CalendarDay? = null
        private var textView: AppCompatTextView? = null
        var alertDialog: AlertDialog? = null

        interface OnDateSelectedListener {
            fun onDateSelected(calendarDay: CalendarDay)
        }

        fun setOnDateSelectedListener(listener: OnDateSelectedListener) {
            this.listener = listener
        }

        fun dismissDialog() {
            alertDialog?.dismiss()
        }

        fun setButtonSize() {
            val positiveButton = alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.textSize = 32f
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val inflater: LayoutInflater? = activity?.layoutInflater
            //inflate custom layout and get views
            //pass null as parent view because will be in dialog layout
            val view: View = inflater!!.inflate(R.layout.dialog_basic, null)
            textView = view.findViewById(R.id.tv_date)
            val widget = view.findViewById<MaterialCalendarView>(R.id.calendarView)
            val today = CalendarDay.today()
            widget.state().edit().setMinimumDate(CalendarDay.from(2025, 1, 1)) // 设置最小可选日期
                .setMaximumDate(today) // 设置最大可选日期为今天
                .commit()
            widget.setOnDateChangedListener(this)
            alertDialog =
                    AlertDialog.Builder(activity).setTitle(R.string.title_date_dialogs).setView(view).setPositiveButton(android.R.string.ok) { dialog, _ ->
                        selectedDate?.let { data ->
                            listener?.onDateSelected(data)
                        }
                        dialog.dismiss()
                    }.create()
            val positiveButton = alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.textSize = 64f
            return alertDialog!!
        }

        override fun onDateSelected(widget: MaterialCalendarView, calendarDay: CalendarDay, selected: Boolean) {
            selectedDate = calendarDay
            val selectedDateStr = "${calendarDay.year}年/${
                calendarDay.month.toString().padStart(2, '0')
            }月/${
                calendarDay.day.toString().padStart(2, '0')
            }日"
            textView?.text = selectedDateStr
        }
    }

    private fun initFactory() {
        acetDealerId = findViewById(R.id.acet_dealer_id)
        acetDeptId = findViewById(R.id.acet_dept_id)
        acetQr = findViewById(R.id.acet_qr)
        acetSn = findViewById(R.id.acet_sn)
        acetSn?.setAlphanumericLimit()
        acetQr?.setAlphanumericLimit2()
        rgLattice = findViewById(R.id.rg_lattice)
        acetFlowCard = findViewById(R.id.acet_flow_card)
        val selectedId = rgLattice?.checkedRadioButtonId
        selectedId?.let { sid ->
            selectedText(sid)
        }
        val actvInit = findViewById<AppCompatTextView>(R.id.actv_init)
        acetFlowCard?.setOnClickListener {
            acetQr?.let { qr->
                hideKeyboard(qr)
            }
            onSimpleCalendarDialogClick()
        }
        actvInit.setOnClickListener {
            Loge.e("调试socket 点击初始化 ")
//            test()   //调试1
            initSocket()//调试2
//            submit()

        }
        rgLattice?.setOnCheckedChangeListener { _, checkedId ->
            selectedText(checkedId)
        }
        Loge.e("屏幕尺寸大小 ：${getScreenParams()}")
    }

    private fun submit() {
        //外部提供的
        val dealerId = acetDealerId?.text.toString()
        val deptId = acetDeptId?.text.toString()
        val qr = acetQr?.text.toString()
        if (TextUtils.isEmpty(dealerId)) {
            Toast.makeText(AppUtils.getContext(),"请输入经销商id",Toast.LENGTH_LONG).show()
            return
        }
        if (TextUtils.isEmpty(deptId)) {
            Toast.makeText(AppUtils.getContext(),"请输入部门id",Toast.LENGTH_LONG).show()
            return
        }
        if (TextUtils.isEmpty(qr)) {
            Toast.makeText(AppUtils.getContext(),"请输入二维码内容信息",Toast.LENGTH_LONG).show()
            return
        }

        //自己内部的
        val flowCard = acetFlowCard?.text.toString()
        val sn = acetSn?.text.toString()
        if (TextUtils.isEmpty(flowCard)) {
            Toast.makeText(AppUtils.getContext(),"请选择流量卡到期时间",Toast.LENGTH_LONG).show()
            return
        }
        if (TextUtils.isEmpty(sn)) {
            Toast.makeText(AppUtils.getContext(),"请输入出厂配置sn码",Toast.LENGTH_LONG).show()
            return
        }
        if (mRecycleType == -1) {
            Toast.makeText(AppUtils.getContext(),"请输入出厂回收箱格口类型",Toast.LENGTH_LONG).show()
            return
        }
        val from = mutableMapOf<String, Any>()
        //客户提供
        from["dealerId"] = dealerId
        from["deptId"] = deptId
        from["urlQrSuffix"] = qr
        from["debugPasswd"] = 123456 ////设备调试密码
        from["expirationDate"] = acetFlowCard?.text.toString()    //流量卡到期时间
        from["imei"] = TelephonyUtils.getImei(AppUtils.getContext()) ?: ""
        from["recycleType"] = mRecycleType  ////回收箱类型 1单个投口  2 双投口  3子母口
        from["sn"] = acetSn?.text.toString()
        from["weightSensorMode"] = 1//	//2颗称重芯片工作模式(0独立, 1协同)
        val headers = mutableMapOf<String, String>()
        headers["token"] = "c2f5de4f-93de-4195-9c74-aa6ad9b2edd8"
        Loge.e("from $from | headers $headers")
//        cabinetVM.ioScope.launch {
//            httpRepo.issueDevice(headers, from).onCompletion {
//                Loge.d("网络请求 出厂化配置 onCompletion $headers | $from")
//
//            }.onSuccess { user ->
//                Loge.d("网络请求 出厂化配置 onSuccess ${Thread.currentThread().name} ${user.toString()}")
//
//            }.onFailure { code, message ->
//                Loge.d("网络请求 出厂化配置 onFailure $code $message")
//
//            }.onCatch { e ->
//                Loge.d("网络请求 出厂化配置 onCatch ${e.errorMsg}")
//            }
//        }
    }

    private fun test() {
        //SPreUtil.put(AppUtils.getContext(), "init", true)
        SPreUtil.put(AppUtils.getContext(), "initSocket", false)
        val snText = acetSn?.text.toString()
        SPreUtil.put(AppUtils.getContext(), "init_sn", snText)
        startActivity(Intent(this@InitFactoryActivity, HomeActivity::class.java))
        finish()
    }

    private fun selectedText(checkedId: Int) {
        val selected = when (checkedId) {
            R.id.mrb_lattice1 -> "单格口"
            R.id.mrb_lattice2 -> "双格口"
            R.id.mrb_lattice3 -> "子母格口"
            else -> null
        }
        when (selected) {
            "单格口" -> {
                mRecycleType = 1
                SPreUtil.put(AppUtils.getContext(), "type_grid", 1)
            }

            "双格口" -> {
                mRecycleType = 2
                SPreUtil.put(AppUtils.getContext(), "type_grid", 2)
            }

            "子母格口" -> {
                mRecycleType = 3
                SPreUtil.put(AppUtils.getContext(), "type_grid", 3)
            }
        }
    }

    private fun AppCompatEditText.setAlphanumericLimit() {
        // 输入过滤器
        filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val pattern = Pattern.compile("[^a-zA-Z0-9]")
            if (pattern.matcher(source).find()) "" else null
        }, InputFilter.LengthFilter(31)) // 最大31个字符（小于32）

        // 实时验证
        addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().matches(Regex("^[a-zA-Z0-9]*$"))) {
                    error = "只允许字母和数字"
                }
            }
        })
    }

    private fun AppCompatEditText.setAlphanumericLimit2() {
        // 输入过滤器
        filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val pattern = Pattern.compile("^//\\.([A-Za-z0-9]+)/$")
            if (pattern.matcher(source).find()) "" else null
        }, InputFilter.LengthFilter(31)) // 最大31个字符（小于32）

        // 实时验证
        addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (!s.toString().matches(Regex("^//\\.([A-Za-z0-9]+)/$"))) {
                    error = "只允许//./字母和数字（大小写不敏感）"
                }
            }
        })
    }

    abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun hideKeyboard(view: View) {
        val imm =
                baseContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun initSocket() {
        cabinetVM.ioScope.launch {
            SPreUtil.put(AppUtils.getContext(), "initSocket", true)
            SocketManager.initializeSocketClient(host = "58.251.251.79", port = 9095)
            cabinetVM.vmClient = SocketManager.socketClient
            SocketManager.socketClient.start()
            delay(500)
            val state = cabinetVM.vmClient?.state?.value ?: ConnectionState.DISCONNECTED
            Loge.e("调试socket startUI 当前线程：${Thread.currentThread().name} | state $state")
            when (state) {
                ConnectionState.START -> {

                }

                ConnectionState.DISCONNECTED -> {

                }

                ConnectionState.CONNECTING -> {

                }

                ConnectionState.CONNECTED -> {
                    SPreUtil.put(AppUtils.getContext(), "init", true)
                    startActivity(Intent(this@InitFactoryActivity, HomeActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun getScreenParams(): String {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        val imei = TelephonyUtils.getImei(AppUtils.getContext())
//        val imsi = TelephonyUtils.getImsi(AppUtils.getContext())
//        val iccid = TelephonyUtils.getIccid(AppUtils.getContext())
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

//             imei: $imei
//             """.trimIndent()
//        str += """
////
////             imsi: $imsi:${pri.second}
////             """.trimIndent()
//        str += """
//
//             iccid: $iccid:${pri.third}:${getIccid2(AppUtils.getContext())}:iccid2:${iccid2}
//             """.trimIndent()
        return str
    }
}