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
import com.recycling.toolsapp.databinding.FragmentTouSingleBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.utils.MediaPlayerHelper
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.CmdCode
import com.serial.port.utils.FileMdUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


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
//            mActivity?.navigateTo(fragmentClass = TestSocketFragment::class.java)
            mActivity?.navigateTo(fragmentClass = DeBugTypeFragment::class.java)

        }
        binding.clMobile.setOnClickListener {
//            mActivity?.navigateTo(fragmentClass = DeliveryFragment::class.java)
            mActivity?.navigateTo(fragmentClass = MobileFragment::class.java)
        }
        binding.acivLogo.setOnClickListener {
            cabinetVM.testClearCmd()
//            val data = HexConverter.intToByteArray(6450)
//            val data2 = HexConverter.intToByteArray(1)
//            val weight = HexConverter.byteArrayToInt(data)
//            val result = "%.2f".format(weight / 100.0)
//            println("测试我来了 $result| ${ByteUtils.toHexString(data)}|${ByteUtils.toHexString(data2)}")
//            cabinetVM.downloadRes("http://112.91.141.155:8999/apk/db2432e2c5f34c5cb31db57db29a8c5d", FileMdUtil.matchNewFileName("audio", "opendoor.wav")) { success ->
//                println("网络 下载音频 $success ")
//                if(success){
//                    MediaPlayerHelper.playAudioFromAppFiles(AppUtils.getContext(), "audio","opendoor.wav")
//                }
//            }
        }
        binding.clContent.setOnClickListener {
            //假设这里接收到服务指令 先启动检测门状态再发起开门指令
//            cabinetVM.doorGeX = CmdCode.GE1
//            cabinetVM.testSendCmd(CmdCode.GE_OPEN)

            //这里查询重量
            cabinetVM.doorGeX = CmdCode.GE1
            cabinetVM.testWeightCmd()
        }
        val value = binding.tvKetouValue.text.toString()
        setTextColorFromPosition(binding.tvKetouValue, value, 9, Color.YELLOW)
        println("")
//        createCode()
        println("调试socket 进入单格口 ${ cabinetVM.mQrCode}")
        cabinetVM.mQrCode?.let { bitmap->
            binding.acivCode.setImageBitmap(bitmap)
        }

    }

    fun setTextColorFromPosition(textView: AppCompatTextView, text: String, startIndex: Int, color: Int) {
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), startIndex, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
    }

    private fun createCode() {
        val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_gdw)
        val bg = BitmapFactory.decodeResource(resources, R.color.black)
        AwesomeQRCode.Renderer().contents("https://nearby.ren/").background(bg).size(800) // 增加尺寸以提高可扫描性
            .roundedDots(true).dotScale(0.6f) // 增加点的大小
            .colorDark(Color.BLACK) // 深色部分为黑色
            .colorLight(Color.WHITE) // 浅色部分为白色 - 这是关键修复
            .whiteMargin(true).margin(20) // 增加边距
            .logo(logoBitmap).logoMargin(10).logoRadius(10).logoScale(0.15f) // 减小logo尺寸，避免遮挡关键信息
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
