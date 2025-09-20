package com.recycling.toolsapp.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.viewModels
import com.blankj.utilcode.util.LogUtils.D
import com.bumptech.glide.Glide
import com.recycling.toolsapp.R
import com.recycling.toolsapp.R.drawable.btn_tab_2_bg
import com.recycling.toolsapp.databinding.FragmentDebugTypeBinding
import com.recycling.toolsapp.databinding.FragmentMobileBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.socket.DoorOpenBean
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.CmdCode
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus


/***
 * 手机号登录
 */
@AndroidEntryPoint class MobileFragment : BaseBindFragment<FragmentMobileBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

    // 创建任务队列
    override fun layoutRes(): Int {
        return R.layout.fragment_mobile
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }

    fun removeLastDigit(numberStr: String): String {
        return if (numberStr.isNotEmpty() && numberStr.all { it.isDigit() }) {
            numberStr.dropLast(1).ifEmpty { "" }
        } else {
            numberStr
        }
    }

    override fun initialize(savedInstanceState: Bundle?) {
        setCountdown(600)
        binding.actvLogin.setOnClickListener {
//            binding.clSelect.isVisible =  !binding.clSelect.isVisible
//            mActivity?.fragmentCoordinator?.navigateBack()
        }
//        binding.acivClose.setOnClickListener {
//            binding.clSelect.isVisible =  !binding.clSelect.isVisible
//        }

        //匹配是否显示两个格口
        if (cabinetVM.doorGeXType == CmdCode.GE1) {
            binding.clCastLeft.isVisible = true
            binding.clCastRight.isVisible = false
        } else if (cabinetVM.doorGeXType == CmdCode.GE2) {
            binding.clCastLeft.isVisible = true
            binding.clCastRight.isVisible = true
        }
        //接收图片
        LiveBus.get(BusType.BUS_MOBILE_CLOS).observeForever { filepath ->
            mActivity?.fragmentCoordinator?.navigateBack()
        }

        binding.acetCastLeft.setOnClickListener {
            val mobile = binding.acetMobile.text.toString()
            cabinetVM.toGoMobile(mobile)
        }
        binding.acetCastRight.setOnClickListener {
            val mobile = binding.acetMobile.text.toString()
            cabinetVM.toGoMobile(mobile)
        }


        binding.actvExit.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateBack()
        }
        //左投口
        binding.clCastLeft.setOnClickListener {

        }
        //右投口
        binding.clCastRight.setOnClickListener {

        }
        val selectableViews =
                listOf(binding.acet1, binding.acet2, binding.acet3, binding.acet4, binding.acet5, binding.acet6, binding.acet7, binding.acet8, binding.acet9, binding.acet0, binding.acetDel, binding.acetMobile, binding.actvLogin)
        selectableViews.forEach { view ->
            view.setOnClickListener {
                val acetMbile = binding.acetMobile
                val value = acetMbile.text.toString()
                val builder = StringBuilder()
                builder.append(value)
                // 执行业务逻辑
                when (view.id) {
                    R.id.acet_1 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_1")
                        builder.append(1)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_2 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_2")
                        builder.append(2)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_3 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_3")
                        builder.append(3)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_4 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_4")
                        builder.append(4)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_5 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_5")
                        builder.append(5)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_6 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_6")
                        builder.append(6)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_7 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_7")
                        builder.append(7)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_8 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_8")
                        builder.append(8)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_9 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_9")
                        builder.append(9)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)
                    }

                    R.id.acet_0 -> {
                        if (value.length == 11) return@setOnClickListener
                        Loge.d("handle acet_0")
                        builder.append(0)
                        acetMbile.setText(builder.toString())
                        acetMbile.setSelection(builder.toString().length)

                    }

                    R.id.acet_del -> {
                        Loge.d("handle acet_del")
                        val result = builder.toString()
                        if (result.isEmpty()) return@setOnClickListener
                        val result2 = removeLastDigit(result)
                        acetMbile.setText(result2)
                    }

                    R.id.actv_login -> {
                        Loge.d("handle actv_login")
                        if (value.length != 11) {
                            cabinetVM.tipMessage("请输入手机号")
                            return@setOnClickListener
                        }
                        binding.clSelect.isVisible = !binding.clSelect.isVisible
                    }
                }
            }
        }
    }
}
