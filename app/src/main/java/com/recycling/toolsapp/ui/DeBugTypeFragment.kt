package com.recycling.toolsapp.ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.viewModels
import com.recycling.toolsapp.R
import com.recycling.toolsapp.R.drawable.btn_tab_2_bg
import com.recycling.toolsapp.databinding.FragmentDebugTypeBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.CmdCode
import dagger.hilt.android.AndroidEntryPoint


/***
 * 称重页
 */
@AndroidEntryPoint class DeBugTypeFragment : BaseBindFragment<FragmentDebugTypeBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    //当前格口
    var currentGe = 1

    // 创建任务队列
    override fun layoutRes(): Int {
        return R.layout.fragment_debug_type
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }


    override fun initialize(savedInstanceState: Bundle?) {
        setCountdown(300)
        binding.actvLeft.isSelected = true
        binding.actvRight.isSelected = false
        binding.actvLeft.setOnClickListener {
            currentGe = CmdCode.GE1
            binding.actvLeft.isSelected = true
            binding.actvRight.isSelected = false
        }

        binding.actvRight.setOnClickListener {
            currentGe = CmdCode.GE2
            binding.actvLeft.isSelected = false
            binding.actvRight.isSelected = true
        }

        //返回
        binding.actvReturn.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateBack()
        }

        //称重
        binding.actvWeighing.setOnClickListener { }
        //去零清皮
        binding.actvClearPeel.setOnClickListener { }

        //内摄像头
        binding.actvInCamera.setOnClickListener { }

        //外摄像头
        binding.actvOutCamera.setOnClickListener { }

        //推杆
        binding.rgPushRod.setOnCheckedChangeListener { _, checkedId ->
        }
        //内灯
        binding.rgIn.setOnCheckedChangeListener { _, checkedId ->
        }
        //外灯
        binding.rgOut.setOnCheckedChangeListener { _, checkedId ->
        }
        //电磁锁
        binding.rgLock.setOnCheckedChangeListener { _, checkedId ->
        }
        //校准
        binding.rgKg.setOnCheckedChangeListener { _, checkedId ->
        }
    }
}
