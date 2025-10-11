package com.recycling.toolsapp.fitsystembar.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.recycling.toolsapp.ui.TouSingleFragment
import com.recycling.toolsapp.ui.TouDoubleFragment
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus


abstract class BaseLazyLoadFragment : Fragment() {

    /***
     * 获取替换的根布局
     * return
     */
    open fun getLoading(): Int {
        return -1
    }


    private var rootView: View? = null

    private var mIsFirstVisible = true

    private var isViewCreated = false

    /**
     * 20180818
     *
     * @return
     */
    private var isSupportVisible = false

    /**
     * 获取类名
     *
     * @return
     */
    protected val classname: String get() = javaClass.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiveBus.get(BusType.BUS_RESET_COUNTDOWN).observeForever {
            resetCountdown()
        }
    }

    protected var mContext: Context? = null
    var mActivity: BaseActivity? = null


    //2分钟半150 1分钟半90
    val DEFAULT_COUNTDOWN: Int = 20
    private var mSaveCountdown = DEFAULT_COUNTDOWN
    protected var mCancelCountdown: Boolean = false
    private var mShowActionBar = true // 是否显示动作栏
    private val mShowActionBarBack = true // 是否显示动作栏上的返回键


    // 协程倒计时相关
    private var countdownJob: Job? = null
    private var isCountdownPaused = false
    private var remainingCountdownTime = DEFAULT_COUNTDOWN

    // 应用前后台状态监听
    private var appForegroundObserver: LifecycleEventObserver? = null
    private var isAppInForeground = true

    //2分钟半150 1分钟半90
    companion object {
        const val DEFAULT_COUNTDOWN: Int = 6
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        Loge.d("测试 onAttach $mShowActionBar - $mShowActionBarBack")
        mContext = context
        if (mContext is BaseActivity) {
            mActivity = mContext as BaseActivity
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCountdown()
    }

    /**
     * 设置应用前后台状态监听
     */
    private fun setupAppForegroundObserver() {
        appForegroundObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // 应用进入后台
                    isAppInForeground = false
                    pauseCountdown()
                    Loge.d("FragmentCoordinator App moved to background, countdown paused")
                }
                Lifecycle.Event.ON_CREATE->{
                    Loge.d("FragmentCoordinator App moved to background, countdown ON_CREATE")
                }
                Lifecycle.Event.ON_RESUME->{
                    Loge.d("FragmentCoordinator App moved to background, countdown ON_RESUME")
                    // 应用回到前台
                    isAppInForeground = true
                    resumeCountdown()
                    Loge.d("FragmentCoordinator App moved to foreground, countdown resumed")
                }
                Lifecycle.Event.ON_START -> {

                }
                else -> {}
            }
        }

        // 注册到Activity的生命周期
        activity?.lifecycle?.addObserver(appForegroundObserver!!)
    }
    open fun showActionBarBack() {
        mActivity?.showActionBarBack()
    }

    open fun hideActionBarBack() {
        mActivity?.hideActionBarBack()
    }

    open fun showActionBar() {
        mActivity?.showActionBar()
    }

    open fun hideActionBar() {
        mActivity?.hideActionBar()
    }

    protected fun startCountdown() {
        Loge.d("FragmentCoordinator startCountdown")
        updateCountdown(0)
        startCountdown(-1)
    }

    protected fun startCountdown(countdown: Int) {
        Loge.d("FragmentCoordinator startCountdown $countdown mCancelCountdown = $mCancelCountdown")
        if (countdown >= 0) {
            setCountdown(countdown)
        }
        mCancelCountdown = false
        startCountdownCoroutine()
    }

    /**
     * 使用协程启动倒计时
     */
    private fun startCountdownCoroutine() {
        // 取消之前的倒计时
        countdownJob?.cancel()

        countdownJob = lifecycleScope.launch {
            var currentCountdown = remainingCountdownTime

            Loge.d("FragmentCoordinator currentCountdown $currentCountdown isActive $isActive mCancelCountdown $mCancelCountdown")
            while (currentCountdown >= 0 && isActive && !mCancelCountdown) {
                Loge.d("FragmentCoordinator currentCountdown isAppInForeground $isAppInForeground isSupportVisible $isSupportVisible")
                if (isAppInForeground && isSupportVisible) {
                    // 只有在应用在前台且Fragment可见时才更新倒计时
                    updateCountdown(currentCountdown)

                    if (currentCountdown <= 0) {
                        doneCountdown()
                        break
                    }

                    currentCountdown--
                    remainingCountdownTime = currentCountdown
                    Loge.d("FragmentCoordinator currentCountdown $remainingCountdownTime")

                }

                // 等待1秒，但会检查取消状态
                delay(1000)
            }
        }
    }

    /**
     * 暂停倒计时
     */
    private fun pauseCountdown() {
        isCountdownPaused = true
        countdownJob?.cancel()
        Loge.d("FragmentCoordinator Countdown paused, remaining: $remainingCountdownTime")
    }

    /**
     * 恢复倒计时
     */
    private fun resumeCountdown() {
        Loge.d("FragmentCoordinator isCountdownPaused: $isCountdownPaused isSupportVisible: $isSupportVisible mCancelCountdown: $mCancelCountdown")
        if (isCountdownPaused && isSupportVisible && !mCancelCountdown) {
            isCountdownPaused = false
            startCountdownCoroutine()
            Loge.d("FragmentCoordinator Countdown resumed from: $remainingCountdownTime")
        }
    }

    // 取消倒计时
    protected fun cancelCountdown() {
        countdownJob?.cancel()
        mCancelCountdown = true
        remainingCountdownTime = DEFAULT_COUNTDOWN

        if (mActivity != null) {
            mActivity!!.cancelCountdown()
        }
    }

    // 重置倒计时
    fun resetCountdown() {
        setCountdown(mSaveCountdown)
    }

    // 设置倒计时
    fun setCountdown(mCountdown: Int) {
        remainingCountdownTime = mCountdown
        if (isAppInForeground && isSupportVisible) {
            updateCountdown(remainingCountdownTime)
        }
    }

    // 更新倒计时
    protected fun updateCountdown(countdown: Int) {
        if (!isAdded || mCancelCountdown || mActivity == null) {
            Loge.d("FragmentCoordinator updateCountdown: context=$context isAdd=$isAdded activity=$activity")
            return
        }
        mActivity?.updateCountdown(countdown)
    }

    // 完成倒计时
    open protected fun doneCountdown() {
        if (mCancelCountdown || !isAdded) {
            return
        }
        navigateToHome()
    }


    private fun navigateToHome() {
        Loge.d("FragmentCoordinator navigateToHome")
        val fragment = mActivity?.fragmentCoordinator?.getCurrentFragment()
        // 示例：打开首页时使用淡入动画
//        if (fragment is NewHomeFragment) {
//            return
//        }
//        val bool = mActivity?.navigateBackTo(fragmentClass = NewHomeFragment::class.java)

        if (fragment is TouSingleFragment) {
            return
        }
        if (fragment is TouDoubleFragment) {
            return
        }
        val typeGrid = SPreUtil[AppUtils.getContext(), "type_grid", -1]
        when (typeGrid) {
            1 -> {
                val bool = mActivity?.navigateBackTo(fragmentClass = TouSingleFragment::class.java)
                Loge.d("FragmentCoordinator navigateToHome 跳转是否成功：$bool")
            }

            2 -> {
                val bool = mActivity?.navigateBackTo(fragmentClass = TouDoubleFragment::class.java)
                Loge.d("FragmentCoordinator navigateToHome 跳转是否成功：$bool")
            }
        }
    }

    protected fun setShowActionBar(showActionBar: Boolean) {
        mShowActionBar = showActionBar
    }

    protected fun getRootView(): View? {
        return rootView
    }

    @SuppressLint("ClickableViewAccessibility") protected fun setRootView(view: View) {
        this.rootView = view
        if (isShowActionBar()) {
            showActionBar()
        } else {
            hideActionBar()
        }
        if (isShowActionBarBack()) {
            showActionBarBack()
        } else {
            hideActionBarBack()
        }
        rootView?.setOnTouchListener { v, event ->
            if (event?.action == MotionEvent.ACTION_DOWN) {
                Loge.d("FragmentCoordinator resetCountdown ACTION_DOWN")
                resetCountdown()
            }
            false
        }
    }

    /**
     * 加载布局文件
     *
     * @return
     */
    abstract fun layoutRes(): Int

    /**
     *  生命周期create
     */
    protected abstract fun initialize(savedInstanceState: Bundle?)

    abstract fun isShowActionBar(): Boolean
    abstract fun isShowActionBarBack(): Boolean


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        //走这里分发可见状态情况有两种，1. 已缓存的 Fragment 被展示的时候 2. 当前 Fragment 由可见变成不可见的状态时
        // 对于默认 tab 和 间隔 checked tab 需要等到 isViewCreated = true 后才可以通过此通知用户可见，
        // 这种情况下第一次可见不是在这里通知 因为 isViewCreated = false 成立，可见状态在 onActivityCreated 中分发
        // 对于非默认 tab，View 创建完成  isViewCreated =  true 成立，走这里分发可见状态，mIsFirstVisible 此时还为 false  所以第一次可见状态也将通过这里分发
        if (isViewCreated) {
            if (isVisibleToUser && !isSupportVisible) {
                dispatchUserVisibleHint(true)
            } else if (!isVisibleToUser && isSupportVisible) {
                dispatchUserVisibleHint(false)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // 将 View 创建完成标志位设为 true
        isViewCreated = true
        // 默认 Tab getUserVisibleHint() = true !isHidden() = true
        // 对于非默认 tab 或者非默认显示的 Fragment 在该生命周期中只做了 isViewCreated 标志位设置 可见状态将不会在这里分发
        if (!isHidden && userVisibleHint) {
            dispatchUserVisibleHint(true)
        }
    }

    /**
     * 该方法与 setUserVisibleHint 对应，调用时机是 show，hide 控制 Fragment 隐藏的时候，
     * 注意的是，只有当 Fragment 被创建后再次隐藏显示的时候才会调用，第一次 show 的时候是不会回调的。
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            dispatchUserVisibleHint(false)
        } else {
            dispatchUserVisibleHint(true)
        }
    }

    /**
     * 需要再 onResume 中通知用户可见状态的情况是在当前页面再次可见的状态 !mIsFirstVisible 可以保证这一点，
     * 而当前页面 Activity 可见时所有缓存的 Fragment 都会回调 onResume
     * 所以我们需要区分那个Fragment 位于可见状态
     * (!isHidden() && !currentVisibleState && getUserVisibleHint()）可条件可以判定哪个 Fragment 位于可见状态
     */
    override fun onResume() {
        super.onResume()
        if (!mIsFirstVisible) {
            if (!isHidden && !isSupportVisible && userVisibleHint) {
                dispatchUserVisibleHint(true)
            }
        }
    }

    /**
     * 当用户进入其他界面的时候所有的缓存的 Fragment 都会 onPause
     * 但是我们想要知道只是当前可见的的 Fragment 不可见状态，
     * currentVisibleState && getUserVisibleHint() 能够限定是当前可见的 Fragment
     */
    override fun onPause() {
        super.onPause()
        if (isSupportVisible && userVisibleHint) {
            dispatchUserVisibleHint(false)
        }
    }

    /**
     * 统一处理 显示隐藏  做两件事
     * 设置当前 Fragment 可见状态 负责在对应的状态调用第一次可见和可见状态，不可见状态函数
     *
     * @param visible
     */
    private fun dispatchUserVisibleHint(visible: Boolean) {
        //当前 Fragment 是 child 时候 作为缓存 Fragment 的子 fragment getUserVisibleHint = true
        //但当父 fragment 不可见所以 currentVisibleState = false 直接 return 掉
//        LogUtils.e(getClass().getSimpleName() + "  dispatchUserVisibleHint isParentInvisible() " + isParentInvisible());
        // 这里限制则可以限制多层嵌套的时候子 Fragment 的分发
        if (visible && isParentInvisible) return
        //
//        //此处是对子 Fragment 不可见的限制，因为 子 Fragment 先于父 Fragment回调本方法 currentVisibleState 置位 false
//        // 当父 dispatchChildVisibleState 的时候第二次回调本方法 visible = false 所以此处 visible 将直接返回
        if (isSupportVisible == visible) {
            return
        }
        isSupportVisible = visible
        if (visible) {
            if (mIsFirstVisible) {
                mIsFirstVisible = false
                onFragmentFirstVisible()
            }
            onFragmentResume()
            dispatchChildVisibleState(true) //20180818
        } else {
            dispatchChildVisibleState(false) //20180818
            onFragmentPause()
        }
    }

    /**
     * 20180818
     * 用于分发可见时间的时候父获取 fragment 是否隐藏
     *
     * @return true fragment 不可见， false 父 fragment 可见
     */
    private val isParentInvisible: Boolean
        get() {
            return parentFragment?.let {
                if (it is BaseLazyLoadFragment) {
                    val f = parentFragment as BaseLazyLoadFragment
                    f.isSupportVisible.not()
                } else {
                    return false
                }
            } ?: false
        }

    /**
     * 20180818
     * 当前 Fragment 是 child 时候 作为缓存 Fragment 的子 fragment 的唯一或者嵌套 VP 的第一 fragment 时 getUserVisibleHint = true
     * 但是由于父 Fragment 还进入可见状态所以自身也是不可见的， 这个方法可以存在是因为庆幸的是 父 fragment 的生命周期回调总是先于子 Fragment
     * 所以在父 fragment 设置完成当前不可见状态后，需要通知子 Fragment 我不可见，你也不可见，
     *
     *
     * 因为 dispatchUserVisibleHint 中判断了 isParentInvisible 所以当 子 fragment 走到了 onActivityCreated 的时候直接 return 掉了
     *
     *
     * 当真正的外部 Fragment 可见的时候，走 setVisibleHint (VP 中)或者 onActivityCreated (hide show) 的时候
     * 从对应的生命周期入口调用 dispatchChildVisibleState 通知子 Fragment 可见状态
     *
     * @param visible
     */
    private fun dispatchChildVisibleState(visible: Boolean) {
        val childFragmentManager = childFragmentManager
        val fragments = childFragmentManager.fragments
        if (fragments.isNotEmpty()) {
            for (child in fragments) {
                if (child is BaseLazyLoadFragment && !child.isHidden() && child.getUserVisibleHint()) {
                    child.dispatchUserVisibleHint(visible)
                }
            }
        }
    }

    /**
     * 20180818
     *
     * @param fragment
     * @return
     */
    private fun isFragmentVisible(fragment: Fragment): Boolean {
        return !fragment.isHidden && fragment.userVisibleHint
    }

    /**
     * 对用户第一次可见
     */
    open fun onFragmentFirstVisible() {
    }

    /**
     * 对用户可见
     */
    open fun onFragmentResume() {
    }

    /**
     * 对用户不可见
     */
    open fun onFragmentPause() {
    }

    /**
     * 销毁移除布局
     */
    override fun onDestroyView() {
        //当 View 被销毁的时候我们需要重新设置 isViewCreated mIsFirstVisible 的状态
        isViewCreated = false
        mIsFirstVisible = true
        Loge.d("测试 onDestroyView ${this.classname}")
        Loge.d("测试 onDestroyView ${this.classname}")
        // 取消倒计时
        countdownJob?.cancel()
        countdownJob = null

        // 移除观察者
        appForegroundObserver?.let {
            activity?.lifecycle?.removeObserver(it)
        }
        super.onDestroyView()
    }

}

