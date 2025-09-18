package com.recycling.toolsapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTouSingleBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.HexConverter
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.CmdCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus


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
        refresh()
        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).observeForever { msg ->
            when (msg) {
                BusType.BUS_OVERFLOW -> {
                    binding.acivStatus.isVisible = true
                    binding.acivStatus.setBackgroundResource(R.drawable.ic_gzda)
                }

                BusType.BUS_FAULT -> {
                    binding.acivStatus.isVisible = true
                    binding.acivStatus.setBackgroundResource(R.drawable.ic_myda)

                }

                BusType.BUS_NORMAL -> {
                    binding.acivStatus.isVisible = false
                }

                BusType.BUS_REFRESH_DATA -> {
                    refresh()
                }
            }
        }


        binding.tvSinglePrice.setOnClickListener {
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
            cabinetVM.doorGeX = CmdCode.GE1
            cabinetVM.testSendCmd(CmdCode.GE_OPEN)

            //这里查询重量
//            cabinetVM.doorGeX = CmdCode.GE1
//            cabinetVM.testWeightCmd()
        }

//        createCode()
        println("调试socket 进入单格口 ${cabinetVM.mQrCode}")
        cabinetVM.mQrCode?.let { bitmap ->
            binding.acivCode.setImageBitmap(bitmap)
        }

    }


    private fun refresh() {
        //当前价格
        binding.tvSinglePrice.text = cabinetVM.curGePrice
        //当前重量

        val curWeight = cabinetVM.curG1Weight
        val votable = cabinetVM.getVot1Weight()
        println("调试socket 调试串口 刷新Ui $curWeight | $votable")
        binding.tvCurWeight.text = "当前重量(kg)：$curWeight"
        //可再投递重量
        binding.tvVotableValue.text = "可再投递(kg)：$votable"
        val value = binding.tvVotableValue.text.toString()
        setTextColorFromPosition(binding.tvVotableValue, value, 9, Color.YELLOW)
    }

    fun setTextColorFromPosition(textView: AppCompatTextView, text: String, startIndex: Int, color: Int) {
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), startIndex, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
    }

}
