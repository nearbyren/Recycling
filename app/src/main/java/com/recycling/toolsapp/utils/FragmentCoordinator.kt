package com.recycling.toolsapp.utils

import android.os.Bundle
import androidx.annotation.AnimRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.recycling.toolsapp.FaceApplication
import com.recycling.toolsapp.R
import com.recycling.toolsapp.ui.TouSingleFragment
import com.serial.port.utils.Loge
import java.util.Stack

class FragmentCoordinator private constructor(
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
) {
    val fragmentStack = Stack<FragmentInfo>()
    private val lifecycleCallbacks = mutableMapOf<String, FragmentLifecycleCallback>()
    private val resultCallbacks = mutableMapOf<Class<out Fragment>, ResultCallbackWrapper>()
    val animation =
            AnimConfigBuilder().setEnterAnim(R.anim.fade_in).setExitAnim(R.anim.fade_out).build()

    // 修改为使用 LinkedHashMap 保持顺序，并存储更多信息
    private data class ResultCallbackWrapper(
        val callback: FragmentResultCallback,
        var pendingResult: Bundle? = null,
        var isDestroyed: Boolean = false,
    )

    companion object {
        @Volatile
        private var instance: FragmentCoordinator? = null

        fun getInstance(fragmentManager: FragmentManager, containerId: Int): FragmentCoordinator {
            return instance ?: synchronized(this) {
                Loge.d("FragmentCoordinator getInstance")
                instance ?: FragmentCoordinator(fragmentManager, containerId).also { instance = it }
            }
        }
    }

    data class AnimConfig(
        @AnimRes val enter: Int = R.anim.slide_in_right,
        @AnimRes val exit: Int = R.anim.slide_out_left,
        @AnimRes val popEnter: Int = R.anim.slide_in_left,
        @AnimRes val popExit: Int = R.anim.slide_out_right,
    )

    interface FragmentLifecycleCallback {
        fun onFragmentCreated(fragment: Fragment) {}
        fun onFragmentViewCreated(fragment: Fragment) {}
        fun onFragmentResumed(fragment: Fragment) {}
        fun onFragmentPaused(fragment: Fragment) {}
        fun onFragmentDestroyed(fragment: Fragment) {}
    }

    interface FragmentResultCallback {
        fun onResult(result: Bundle)
    }

    data class FragmentInfo(
        val fragmentClass: Class<out Fragment>,
        val tag: String,
        val args: Bundle? = null,
        val animConfig: AnimConfig? = null,
        val deepLink: String? = null,
    )

    init {
        fragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, s: Bundle?) {
                Loge.d("FragmentCoordinator onFragmentCreated")
                lifecycleCallbacks[f.tag]?.onFragmentCreated(f)
            }

            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: android.view.View, s: Bundle?) {
                lifecycleCallbacks[f.tag]?.onFragmentViewCreated(f)
                Loge.d("FragmentCoordinator onFragmentViewCreated")

            }

            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                lifecycleCallbacks[f.tag]?.onFragmentResumed(f)
                Loge.d("FragmentCoordinator onFragmentResumed")
                if (f is TouSingleFragment) {

                }
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                lifecycleCallbacks[f.tag]?.onFragmentPaused(f)
                Loge.d("FragmentCoordinator onFragmentPaused")

            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                Loge.d("FragmentCoordinator onFragmentDestroyed")
                lifecycleCallbacks[f.tag]?.onFragmentDestroyed(f)
                lifecycleCallbacks.remove(f.tag)
                // 只标记为已销毁，但不立即移除回调
                resultCallbacks[f.javaClass]?.let { wrapper ->
                    wrapper.isDestroyed = true
                    // 如果没有待处理的结果，才移除回调
                    if (wrapper.pendingResult == null) {
                        resultCallbacks.remove(f.javaClass)
                    }
                }
            }
        }, true)
    }

    // 清理方法也需要相应修改
    fun clearResultCallback(fragmentClass: Class<out Fragment>) {
        resultCallbacks[fragmentClass]?.let { wrapper ->
            if (wrapper.pendingResult == null) {
                resultCallbacks.remove(fragmentClass)
            }
        }
    }

    fun navigateTo(
        fragmentClass: Class<out Fragment>,
        args: Bundle? = null,
        animConfig: AnimConfig? = null,
        deepLink: String? = null,
        addToBackStack: Boolean = true,
        lifecycleCallback: FragmentLifecycleCallback? = null,
        resultCallback: FragmentResultCallback? = null,
    ) {
        Loge.d("FragmentCoordinator navigateTo ${fragmentClass.simpleName}")
        val tag = fragmentClass.simpleName
        // 保存回调，使用包装类
        lifecycleCallback?.let { lifecycleCallbacks[tag] = it }
        resultCallback?.let {
            resultCallbacks[fragmentClass] =
                    ResultCallbackWrapper(callback = it, pendingResult = null, isDestroyed = false)
        }

        // 保存回调，使用 Fragment Class 作为 key
        lifecycleCallback?.let { lifecycleCallbacks[tag] = it }

        val fragment = fragmentClass.newInstance().apply {
            arguments = args
        }

        val transaction = fragmentManager.beginTransaction()

        animConfig?.let {
            transaction.setCustomAnimations(it.enter, it.exit, it.popEnter, it.popExit)
        } ?: kotlin.run {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)

        }
//        if (fragment.isAdded) {
//            transaction.show(fragment)
//        } else {
//            transaction.add(containerId, fragment, tag)
//        }

        transaction.setReorderingAllowed(true)
        transaction.replace(containerId, fragment, tag)

        if (addToBackStack) {
            transaction.addToBackStack(tag)
            fragmentStack.push(FragmentInfo(fragmentClass, tag, args, animConfig, deepLink))
        }
//        transaction.commit()
        transaction.commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }


    fun navigateBack(): Boolean {
        fragmentStack.forEach { i ->
            Loge.d("FragmentCoordinator navigateBack for = ${i?.tag}")
        }
        if (fragmentStack.size > 1 && !fragmentManager.isStateSaved) {
            // 使用同步方法弹出回退栈
            val popped = fragmentManager.popBackStackImmediate()
            if (popped) {
                // 成功弹出后移除栈顶记录
                val removedFragment = fragmentStack.pop()
                resultCallbacks.remove(removedFragment.fragmentClass)
                return true
            }
            return false
        }
        // 只剩HomeFragment，不可回退
        return false
    }

    /***
     * @param fragmentClass
     * 导航回到指定fragment
     */
    fun navigateBackTo(fragmentClass: Class<out Fragment>): Boolean {
        val isAppForeground = FaceApplication.getInstance().isAppForeground.value ?: true
        Loge.d("FragmentCoordinator navigateBackTo 是否已保存状态：${fragmentManager.isStateSaved} 前台：$isAppForeground")
        if (!isAppForeground) {
            return false
        }
        val index = fragmentStack.indexOfFirst { it.fragmentClass == fragmentClass }
        Loge.d("FragmentCoordinator navigateBackTo $index = simpleName = ${fragmentClass.simpleName}")
        if (index != -1) {
            val targetFragment = fragmentStack[index]
            fragmentManager.popBackStack(targetFragment.tag, 0)
            while (fragmentStack.size > index + 1) {
                fragmentStack.pop()
            }
            return true
        }
        return false
    }

    fun handleDeepLink(deepLink: String): Boolean {
        val fragmentInfo = fragmentStack.find { it.deepLink == deepLink }
        Loge.d("FragmentCoordinator handleDeepLink $deepLink fragmentInfo = $fragmentInfo")
        return if (fragmentInfo != null) {
            navigateBackTo(fragmentInfo.fragmentClass)
            true
        } else {
            false
        }
    }

    fun sendResult(result: Bundle) {
        fragmentStack.forEach { i ->
            Loge.d("FragmentCoordinator sendResult for = ${i?.tag}")
        }
        if (fragmentStack.size >= 2) {
            val previousFragment = fragmentStack[fragmentStack.size - 1]
            val tag = previousFragment.fragmentClass
            val wrapper = resultCallbacks[previousFragment.fragmentClass]
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (wrapper != null) {
                    if (!wrapper.isDestroyed) {
                        // Fragment 还未销毁，直接调用回调
                        wrapper.callback.onResult(result)
                        resultCallbacks.remove(tag)
                    } else {
                        // Fragment 已销毁，但还有待处理的结果
                        wrapper.pendingResult = result
                        wrapper.callback.onResult(result)
                        // 结果已处理，可以安全移除
                        resultCallbacks.remove(tag)
                    }
                    Loge.d("移除tag = $tag")
                }
            }
        }
    }

    fun clearStack() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentStack.clear()
    }

    fun getCurrentFragment(): Fragment? {
        fragmentStack.forEach { i ->
            Loge.d("FragmentCoordinator getCurrentFragment 所有堆栈 = ${i?.tag}")
        }
        return if (fragmentStack.isNotEmpty()) {
            val current = fragmentStack.peek()
            fragmentManager.findFragmentByTag(current.tag)
        } else {
            null
        }
    }


    class AnimConfigBuilder {
        private var enter: Int = R.anim.slide_in_right
        private var exit: Int = R.anim.slide_out_left
        private var popEnter: Int = R.anim.slide_in_left
        private var popExit: Int = R.anim.slide_out_right

        fun setEnterAnim(@AnimRes anim: Int) = apply { enter = anim }
        fun setExitAnim(@AnimRes anim: Int) = apply { exit = anim }
        fun setPopEnterAnim(@AnimRes anim: Int) = apply { popEnter = anim }
        fun setPopExitAnim(@AnimRes anim: Int) = apply { popExit = anim }

        fun build() = AnimConfig(enter, exit, popEnter, popExit)
    }
}