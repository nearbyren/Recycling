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
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.databinding.FragmentNewHome2Binding
import com.recycling.toolsapp.databinding.FragmentNewHomeBinding
import com.recycling.toolsapp.fitsystembar.base.BaseActivity
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
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


@AndroidEntryPoint class DeliveryFragment : BaseBindFragment<FragmentDeliveryBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    // 创建任务队列
    val taskQueue = PriorityTaskQueue<Int>()
    override fun layoutRes(): Int {
        return R.layout.fragment_delivery
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }


    override fun initialize(savedInstanceState: Bundle?) {

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
