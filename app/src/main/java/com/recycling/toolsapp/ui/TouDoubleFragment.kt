package com.recycling.toolsapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.github.sumimakito.awesomeqr.AwesomeQRCode
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentTouDoubleBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.CmdCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus

/**
 * 双投口
 */
@AndroidEntryPoint class TouDoubleFragment : BaseBindFragment<FragmentTouDoubleBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
    private var downTime = 0L
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
        binding.clRoot.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - downTime >= 2000) {
                        // 执行2秒长按回调
                        mActivity?.navigateTo(fragmentClass = ReportingBoxFragment::class.java, args = Bundle().apply {

                        })
                        true // 消耗事件
                    } else false
                }

                else -> false
            }
        }
        cabinetVM.doorGeXType = CmdCode.GE2
        refresh()
        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).observeForever { msg ->
            when (msg) {
                BusType.BUS_OVERFLOW -> {
                    binding.acivStatusLeft.isVisible = true
                    binding.acivStatusLeft.setBackgroundResource(R.drawable.ic_my1)
                }

                BusType.BUS_FAULT -> {
                    binding.acivStatusLeft.isVisible = true
                    binding.acivStatusLeft.setBackgroundResource(R.drawable.ic_gz1)

                }

                BusType.BUS_NORMAL -> {
                    binding.acivStatusLeft.isVisible = false
                }

                BusType.BUS_REFRESH_DATA -> {
                    refresh()
                }
            }
        }

        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).observeForever { msg ->
            when (msg) {
                BusType.BUS_OVERFLOW -> {
                    binding.acivStatusRight.isVisible = true
                    binding.acivStatusRight.setBackgroundResource(R.drawable.ic_my2)
                }

                BusType.BUS_FAULT -> {
                    binding.acivStatusRight.isVisible = true
                    binding.acivStatusRight.setBackgroundResource(R.drawable.ic_gz2)

                }

                BusType.BUS_NORMAL -> {
                    binding.acivStatusRight.isVisible = false
                }
            }
        }
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
        println("调试socket 进入双格口 ${cabinetVM.mQrCode}")
        cabinetVM.mQrCode?.let { bitmap ->
            binding.acivCode.setImageBitmap(bitmap)
        }

    }

    private fun refresh() {
        //当前价格
        binding.tvDoublePrice.text = cabinetVM.curGePrice
        //投口一
        //当前重量
        binding.tvLeftCurWeight.text = "当前重量(kg)：${cabinetVM.curG1Weight}"
        //可再投递重量
        binding.tvLeftVotableValue.text = "可再投递(kg)：${cabinetVM.getVot1Weight()}"
        val leftValue = binding.tvLeftVotableValue.text.toString()
        setTextColorFromPosition(binding.tvLeftVotableValue, leftValue, 9, Color.YELLOW)

        //投口二
        //当前重量
        binding.tvRightCurWeight.text = "当前重量(kg)：${cabinetVM.curG2Weight}"
        //可再投递重量
        binding.tvRightVotableValue.text = "可再投递(kg)：${cabinetVM.getVot2Weight()}"
        val rightValue = binding.tvRightVotableValue.text.toString()
        setTextColorFromPosition(binding.tvRightVotableValue, rightValue, 9, Color.YELLOW)

    }

    fun setTextColorFromPosition(textView: AppCompatTextView, text: String, startIndex: Int, color: Int) {
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), startIndex, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
    }
}
