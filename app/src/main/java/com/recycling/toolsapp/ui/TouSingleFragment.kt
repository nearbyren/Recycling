package com.recycling.toolsapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import androidx.fragment.app.viewModels
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.google.gson.Gson
import com.recycling.toolsapp.FaceApplication.Companion.enjoySDK
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTouSingleBinding
import com.recycling.toolsapp.fitsystembar.base.BaseActivity
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.CurrentActivity.Config.Companion.CURRENT_ROOM_TYPE
import com.recycling.toolsapp.utils.Define
import com.recycling.toolsapp.utils.HexConverter
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
import kotlin.random.Random


/**
 * 单投口
 */
@AndroidEntryPoint class TouSingleFragment : BaseBindFragment<FragmentTouSingleBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    override fun layoutRes(): Int {
        return R.layout.fragment_tou_single
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }

    override fun initialize(savedInstanceState: Bundle?) {
        binding.tvTitle.setOnClickListener {
            HexConverter.restartApp2(AppUtils.getContext(), 2 * 500L)
        }
        binding.tvValue2.setOnClickListener {
            mActivity?.navigateTo(fragmentClass = TestSocketFragment::class.java)

        }
        binding.clMobile.setOnClickListener {
            mActivity?.navigateTo(fragmentClass = DeliveryFragment::class.java)
        }
        binding.acivCode.setOnClickListener {
            createCode()
        }
    }
    private fun createCode() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_yilaguan)
        AwesomeQRCode.Renderer().contents("hahahah").size(R.dimen.dp_130).logo(bitmap).margin(R.dimen.dp_5).renderAsync(object : AwesomeQRCode.Callback {
            override fun onError(renderer: AwesomeQRCode.Renderer, e: Exception) {
                e.printStackTrace()
            }

            override fun onRendered(renderer: AwesomeQRCode.Renderer, bitmap: Bitmap) {
                cabinetVM.mainScope.launch {
                    binding.acivCode.setImageBitmap(bitmap)
                }
            }
        })
    }

}
