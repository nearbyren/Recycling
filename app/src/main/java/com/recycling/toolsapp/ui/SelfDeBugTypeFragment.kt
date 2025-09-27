package com.recycling.toolsapp.ui

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentDebugTypeBinding
import com.recycling.toolsapp.databinding.FragmentSelfDebugTypeBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.CmdCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/***
 * 自验称重
 */
@AndroidEntryPoint class SelfDeBugTypeFragment : BaseBindFragment<FragmentSelfDebugTypeBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    //当前格口
    var currentGe = CmdCode.GE1

    var weightKg = -1

    // 创建任务队列
    override fun layoutRes(): Int {
        return R.layout.fragment_self_debug_type
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }

    override fun doneCountdown() {
        super.doneCountdown()
    }

    override fun initialize(savedInstanceState: Bundle?) {
        cabinetVM.flowCmd05.value = false
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

        //内摄像头
        binding.actvInCamera.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateTo(fragmentClass = CameraInFragment::class.java)
        }

        //外摄像头
        binding.actvOutCamera.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateTo(fragmentClass = CameraOutFragment::class.java)
        }

        //推杆
        binding.rgPushRod.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.mrb_push_open -> "开"
                R.id.mrb_push_close -> "关"
                else -> null
            }
            when (selected) {
                "开" -> {
                    cabinetVM.testTurnDoor(currentGe, CmdCode.GE_OPEN)
                }

                "关" -> {
                    cabinetVM.testTurnDoor(currentGe, CmdCode.GE_CLOSE)
                }
            }
        }
        //内灯
        binding.rgIn.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.mrb_in_open -> "开"
                R.id.mrb_in_close -> "关"
                else -> null
            }
            when (selected) {
                "开" -> {
                    cabinetVM.testLightsCmd(CmdCode.IN_LIGHTS_OPEN)
                }

                "关" -> {
                    cabinetVM.testLightsCmd(CmdCode.IN_LIGHTS_CLOSE)
                }
            }
        }
        //外灯
        binding.rgOut.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.mrb_out_open -> "开"
                R.id.mrb_out_close -> "关"
                else -> null
            }
            when (selected) {
                "开" -> {
                    cabinetVM.testLightsCmd(CmdCode.OUT_LIGHTS_OPEN)
                }

                "关" -> {
                    cabinetVM.testLightsCmd(CmdCode.OUT_LIGHTS_CLOSE)
                }
            }
        }
        //电磁锁
        binding.rgLock.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.mrb_lock_open -> "开"
                R.id.mrb_lock_close -> "关"
                else -> null
            }
            when (selected) {
                "开" -> {
                    cabinetVM.testClearDoor(currentGe)
                }

                "关" -> {

                }
            }
        }

        //去零清皮
        binding.actvClearPeel.setOnClickListener {
            cabinetVM.testWeightCali2(currentGe, CmdCode.CALIBRATION_0)
        }
        //称重
        binding.actvWeighing.setOnClickListener {
            //校准前发送的指令
//            setRbEnabled(false)
            setRbEnabled2(true, false)
            binding.actvWeighing.isEnabled = false
            binding.clWeight.isVisible = true
            binding.actvPrompt.text = "正在进行称重校准前处理中，请勿操作其他"
            cabinetVM.testWeightCali(currentGe, CmdCode.CALIBRATION_1)

        }
        setRbEnabled2(true, false)
        //校准
        binding.rgKg.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.mrb_kg1 -> getString(R.string.kg_1)
                R.id.mrb_kg2 -> getString(R.string.kg_2)
                R.id.mrb_kg3 -> getString(R.string.kg_3)
                R.id.mrb_kg4 -> getString(R.string.kg_4)
                else -> null
            }
            if (weightKg == -1) {
                cabinetVM.tipMessage("请先点击称重校准")
//                setRbEnabled(false)
                setRbEnabled2(false, false)
                return@setOnCheckedChangeListener
            }
            //校准kg禁止点击称重校准
            binding.actvWeighing.isEnabled = false
            binding.clWeight.isVisible = true
            when (selected) {
                getString(R.string.kg_1) -> {
                    if (weightKg == 2 || weightKg == 3 || weightKg == 4) {
                        cabinetVM.tipMessage("正在进行${getString(R.string.kg_1)}，请勿操作其他")
                        return@setOnCheckedChangeListener
                    }
                    binding.actvPrompt.text = "正在进行${getString(R.string.kg_1)}，请勿操作其他"
                    weightKg = 1
                    cabinetVM.testWeightCali(currentGe, CmdCode.CALIBRATION_2)
                }

                getString(R.string.kg_2) -> {
                    if (weightKg == 1 || weightKg == 3 || weightKg == 4) {
                        cabinetVM.tipMessage("正在进行${getString(R.string.kg_2)}，请勿操作其他")
                        return@setOnCheckedChangeListener
                    }
                    binding.actvPrompt.text = "正在进行${getString(R.string.kg_2)}，请勿操作其他"
                    weightKg = 2
                    cabinetVM.testWeightCali(currentGe, CmdCode.CALIBRATION_3)
                }

                getString(R.string.kg_3) -> {
                    if (weightKg == 1 || weightKg == 2 || weightKg == 4) {
                        cabinetVM.tipMessage("正在进行${getString(R.string.kg_3)}，请勿操作其他")
                        return@setOnCheckedChangeListener
                    }
                    binding.actvPrompt.text = "正在进行${getString(R.string.kg_3)}，请勿操作其他"
                    weightKg = 3
                    cabinetVM.testWeightCali(currentGe, CmdCode.CALIBRATION_4)
                }

                getString(R.string.kg_4) -> {
                    if (weightKg == 1 || weightKg == 2 || weightKg == 3) {
                        cabinetVM.tipMessage("正在进行${getString(R.string.kg_4)}，请勿操作其他")
                        return@setOnCheckedChangeListener
                    }
                    binding.actvPrompt.text = "正在进行${getString(R.string.kg_4)}，请勿操作其他"
                    weightKg = 4
                    cabinetVM.testWeightCali(currentGe, CmdCode.CALIBRATION_5)
                }

            }
        }

        //清运锁状态
        lifecycleScope.launch {
            cabinetVM.getTestClearDoor.collect { result ->
                if (result) {
                    binding.mrbLockOpen.isChecked = true
                    binding.mrbLockClose.isChecked = false
                    cabinetVM.tipMessage("清运开门成功")
                } else {
                    binding.mrbLockOpen.isChecked = false
                    binding.mrbLockClose.isChecked = true
                    cabinetVM.tipMessage("清运开门失败")
                }
            }
        }

        //称重前校准操作
        lifecycleScope.launch {
            cabinetVM.getCaliBefore2.collect { result ->
                binding.clWeight.isVisible = false
                //校准完成复原点击按钮
                binding.actvWeighing.isEnabled = true
                if (result) {
                    cabinetVM.tipMessage("校准前处理已完成，请选择校准类型")
//                    setRbEnabled(true)
                    setRbEnabled2(false, false)
                    weightKg = 0
                } else {
                    cabinetVM.tipMessage("校准前处理未完成，请重新点击称重校准")
//                    setRbEnabled(false)
                    setRbEnabled2(false, false)
                    weightKg = -1
                }
            }
        }

        //称重效验结果
        lifecycleScope.launch {
            cabinetVM.getCaliResult.collect { result ->
                if (result) {
                    cabinetVM.tipMessage("校准完成")
                    setRbEnabled2(false, true)
                } else {
                    cabinetVM.tipMessage("校准失败")
                    setRbEnabled2(false, false)
                }
                //校准完成复原点击按钮
                binding.actvWeighing.isEnabled = true
                binding.clWeight.isVisible = false
                weightKg = -1
            }
        }
    }

    fun setRbEnabled2(isAll: Boolean, isEnabled: Boolean) {
        if (isAll) {
            binding.mrbKg1.isChecked = false
            binding.mrbKg2.isChecked = false
            binding.mrbKg3.isChecked = false
            binding.mrbKg4.isChecked = false

            binding.mrbKg1.isEnabled = false
            binding.mrbKg2.isEnabled = false
            binding.mrbKg3.isEnabled = false
            binding.mrbKg4.isEnabled = false
        } else {
            binding.mrbKg1.isEnabled = true
            binding.mrbKg2.isEnabled = true
            binding.mrbKg3.isEnabled = true
            binding.mrbKg4.isEnabled = true
            when (weightKg) {
                1 -> {
                    binding.mrbKg1.isChecked = isEnabled
                }

                2 -> {
                    binding.mrbKg2.isChecked = isEnabled
                }

                3 -> {
                    binding.mrbKg3.isChecked = isEnabled
                }

                4 -> {
                    binding.mrbKg4.isChecked = isEnabled
                }
            }
        }
    }

    fun setRbEnabled(isEnabled: Boolean, weightKg: Int = -1) {
        for (i in 0 until binding.rgKg.childCount) {
            val child = binding.rgKg.getChildAt(i)
            child.isEnabled = isEnabled
            if (!isEnabled) {
                if (child is RadioButton) {
                    child.isChecked = false
                }
            } else {
                if ((weightKg - 1) == i) {
                    if (child is RadioButton) {
                        child.isChecked = true
                    }
                }
            }
        }
    }

    // 禁止选择
    fun setRadioGroupEnabled(rg: RadioGroup, enabled: Boolean) {
        for (i in 0 until rg.childCount) {
            val child = rg.getChildAt(i)
            child.isEnabled = enabled
            if (!enabled) {
                // 清除选中态
                if (child is RadioButton) {
                    child.isChecked = false
                }
                // 拦截点击
                child.setOnTouchListener { _, _ -> true }
            } else {
                // 恢复可点击
                child.setOnTouchListener(null)
            }
        }
    }

    override fun onDestroy() {
        cabinetVM.flowCmd05.value = true
        super.onDestroy()
    }
}
