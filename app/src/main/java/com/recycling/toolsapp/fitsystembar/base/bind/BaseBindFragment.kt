package com.recycling.toolsapp.fitsystembar.base.bind

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.recycling.toolsapp.fitsystembar.base.BaseLazyLoadFragment

/**
 * @author: lr
 * @created on: 2022/7/10 3:34 下午
 * @description:基于DataBinding的Fragment的基类
 */
abstract class BaseBindFragment<B : ViewDataBinding> : BaseLazyLoadFragment() {

    protected lateinit var binding: B
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (getRootView() != null) {
            return getRootView()
        }
        injectDataBinding(inflater, container)
        initialize(savedInstanceState)
        return getRootView()
    }

    protected open fun injectDataBinding(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DataBindingUtil.inflate(inflater, layoutRes(), container, false)
        binding.lifecycleOwner = this
        setRootView(binding.root)
    }

    override fun onDestroyView() {
        binding.unbind()
        super.onDestroyView()
    }
}