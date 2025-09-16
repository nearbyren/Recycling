package com.recycling.toolsapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.viewModels
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTouDoubleBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.CmdCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
//            HexConverter.restartApp2(AppUtils.getContext(), 2 * 500L)
            mActivity?.navigateTo(fragmentClass = DeliveryFragment::class.java)
        }
        binding.clMobile.setOnClickListener {
//            mActivity?.navigateTo(fragmentClass = DeliveryFragment::class.java)
            mActivity?.navigateTo(fragmentClass = MobileFragment::class.java)
        }

        binding.clLeft.setOnClickListener {
            //假设这里接收到服务指令 先启动检测门状态再发起开门指令
            cabinetVM.doorGeX = CmdCode.GE1
            cabinetVM.testSendCmd(CmdCode.GE_OPEN)
            //这里查询重量
//            cabinetVM.doorGeX = CmdCode.GE1
//            cabinetVM.testWeightCmd()
        }

        binding.clRight.setOnClickListener {
            //假设这里接收到服务指令 先启动检测门状态再发起开门指令
            cabinetVM.doorGeX = CmdCode.GE2
            cabinetVM.testSendCmd(CmdCode.GE_OPEN)
            //这里查询重量
//            cabinetVM.doorGeX = CmdCode.GE2
//            cabinetVM.testWeightCmd()
        }

        binding.acivLogo.setOnClickListener {
            cabinetVM.testClearCmd()
        }

        val valueLeft = binding.tvLeftKetouValue.text.toString()
        setTextColorFromPosition(binding.tvLeftKetouValue, valueLeft, 9, Color.YELLOW)
        val valueRight = binding.tvRightKetouValue.text.toString()
        setTextColorFromPosition(binding.tvRightKetouValue, valueRight, 9, Color.YELLOW)
        println("调试socket 进入双格口 ${ cabinetVM.mQrCode}")
        cabinetVM.mQrCode?.let { bitmap->
            binding.acivCode.setImageBitmap(bitmap)
        }

//        createCode()
    }

    fun setTextColorFromPosition(textView: AppCompatTextView, text: String, startIndex: Int, color: Int) {
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), startIndex, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
    }
    private fun createCode() {
        val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_gdw)
        val bg = BitmapFactory.decodeResource(resources, R.color.black)
        AwesomeQRCode.Renderer().contents("https://nearby.ren/")
            .background(bg)
            .size(800) // 增加尺寸以提高可扫描性
            .roundedDots(true)
            .dotScale(0.6f) // 增加点的大小
            .colorDark(Color.BLACK) // 深色部分为黑色
            .colorLight(Color.WHITE) // 浅色部分为白色 - 这是关键修复
            .whiteMargin(true)
            .margin(20) // 增加边距
            .logo(logoBitmap)
            .logoMargin(10)
            .logoRadius(10)
            .logoScale(0.15f) // 减小logo尺寸，避免遮挡关键信息
            .renderAsync(object : AwesomeQRCode.Callback {
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
