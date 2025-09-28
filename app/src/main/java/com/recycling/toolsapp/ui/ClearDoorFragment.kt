package com.recycling.toolsapp.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentClearDoorBinding
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.ResultType
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.ClearTimer
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.CmdCode
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus
import kotlin.random.Random


/***
 * 清运门
 */
@AndroidEntryPoint class ClearDoorFragment : BaseBindFragment<FragmentClearDoorBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    // 创建任务队列
    override fun layoutRes(): Int {
        return R.layout.fragment_clear_door
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }


    override fun initialize(savedInstanceState: Bundle?) {
        refresh()
        LiveBus.get(BusType.BUS_CLEAR_STATUS).observeForever { msg ->
            when (msg) {
                BusType.BUS_REFRESH_DATA -> {
                    refresh()
                }
            }
        }
        initClick()
        initCountdown()
        upgradeAi()
    }

    private fun initClick() {
        binding.tvClearBeforeText.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateBack()
        }
    }

    override fun doneCountdown() {
    }

    private fun initCountdown() {
        setCountdown(500)
        //倒计时
        binding.cpvView.setMaxProgress(500)
        cabinetVM.clearStartTimer(500)
    }

    private fun refresh() {
        //清运前重量
        val beforeValue = cabinetVM.weightClearBefore
        val afterValue = cabinetVM.weightClearAfter
        binding.tvClearBeforeValue.text = "${beforeValue}KG"
        //清运后重量
        binding.tvClearAfterValue.text = "${afterValue}KG"
        //换算重量
        val clearValue = cabinetVM.subtractFloats(beforeValue ?: "0.00", afterValue ?: "0.00")
        //清运重量
        binding.tvClearValue.text = "${clearValue}KG"
    }

    private fun upgradeAi() {
        // 在 Activity/Fragment 中收集状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cabinetVM.clearState.collect { state ->
                    when (state) {
                        is ClearTimer.CountdownState.Starting -> {
                            binding.cpvView.setMaxProgress(500)
                        }

                        is ClearTimer.CountdownState.Running -> {
                            // 更新 UI
                            binding.cpvView.setProgress(state.secondsRemaining)
                        }

                        ClearTimer.CountdownState.Finished -> {
                            mActivity?.fragmentCoordinator?.navigateBack()

                        }

                        is ClearTimer.CountdownState.Error -> {
                            cabinetVM.tipMessage(state.message)
                        }
                    }
                }
            }
        }
    }
}
