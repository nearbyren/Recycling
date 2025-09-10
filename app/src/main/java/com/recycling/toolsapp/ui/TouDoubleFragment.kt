package com.recycling.toolsapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.viewModels
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.google.gson.Gson
import com.recycling.toolsapp.FaceApplication.Companion.enjoySDK
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTouDoubleBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.CurrentActivity.Config.Companion.CURRENT_ROOM_TYPE
import com.recycling.toolsapp.utils.Define
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 双投口
 */
@AndroidEntryPoint class TouDoubleFragment : BaseBindFragment<FragmentTouDoubleBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    override fun layoutRes(): Int {
        return R.layout.fragment_tou_double
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
        binding.clMobile.setOnClickListener {
            mActivity?.navigateTo(fragmentClass = DeliveryFragment::class.java)
        }
        binding.acivCode.setOnClickListener {
            createCode()
        }

    }

    private fun createCode() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_yilaguan)
        val bg = BitmapFactory.decodeResource(resources, R.color.black)
        AwesomeQRCode.Renderer().contents("hahahah").size(100)
            .background(bg)
            .dotScale(0.3f)
            .colorDark(Color.BLACK)
            .colorLight(Color.BLACK)
            .whiteMargin(true)
            .margin(5).logo(bitmap).logoMargin(5).logoRadius(5).logoScale(0.2f).renderAsync(object : AwesomeQRCode.Callback {
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
