package com.recycling.toolsapp.ui

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.recycling.toolsapp.FaceApplication.Companion.enjoySDK
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.CurrentActivity.Config.Companion.CURRENT_ROOM_TYPE
import com.recycling.toolsapp.utils.Define
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


/***
 * 称重页
 */
@AndroidEntryPoint class DeliveryFragment : BaseBindFragment<FragmentDeliveryBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    // 创建任务队列
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
        setCountdown(10)
        //倒计时
        binding.cpvView.setMaxProgress(10)
        //标题
        binding.tvTitle
        //当前称重
        binding.tvWeightValue.text
        //当前金额
        binding.tvMoneyValue.text
        //图片
        binding.aivShowPhoto
        //按钮
        binding.tvOperation.text
        binding.tvOperation.setOnClickListener {
            cabinetVM.testTypeEnd(1)
        }
        upgradeAi()
        cabinetVM.startTimer(10)
    }

    private fun upgradeAi() {
        // 在 Activity/Fragment 中收集状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cabinetVM.countdownState.collect { state ->
                    when (state) {
                        is CountdownTimer.CountdownState.Starting -> {
                            binding.cpvView.setMaxProgress(10)

                        }

                        is CountdownTimer.CountdownState.Running -> {
                            // 更新 UI
                            binding.cpvView.setProgress(state.secondsRemaining)
                        }

                        CountdownTimer.CountdownState.Finished -> {
                            cabinetVM.testTypeEnd(2)
                        }

                        is CountdownTimer.CountdownState.Error -> {
                            cabinetVM.tipMessage(state.message)
                        }
                    }
                }
            }
        }
    }
}
