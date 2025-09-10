package com.recycling.toolsapp.ui

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentDebugTypeBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.vm.CabinetVM
import dagger.hilt.android.AndroidEntryPoint


/***
 * 称重页
 */
@AndroidEntryPoint class DeBugTypeFragment : BaseBindFragment<FragmentDebugTypeBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })

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

    }
}
