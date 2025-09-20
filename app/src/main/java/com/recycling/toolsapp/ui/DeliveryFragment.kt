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
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.ResultType
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.CmdCode
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nearby.lib.signal.livebus.BusType
import nearby.lib.signal.livebus.LiveBus
import kotlin.random.Random


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
        refresh()
        LiveBus.get(BusType.BUS_TOU1_DOOR_STATUS).observeForever { msg ->
            when (msg) {
                BusType.BUS_REFRESH_DATA -> {
                    refresh()
                }
            }
        }
        LiveBus.get(BusType.BUS_DELIVERY_STATUS).observeForever { msg ->
            Loge.d("网络导入用户信息 用户信息异常")
            binding.tvOperation.isEnabled = true
            binding.tvOperation.text = "仓门已经打开"
            when (msg) {
                BusType.BUS_DELIVERY_CLOSE -> {
                    mActivity?.fragmentCoordinator?.navigateBack()
                }

                BusType.BUS_DELIVERY_ABNORMAL -> {
                    binding.tvOperation.text = "格口关闭异常，再点击关闭"
                }
            }
        }
        initClick()
        initCountdown()
//        initBanner()
//        initBanner2()
        upgradeAi()
        initCameraX()

    }

    private fun initClick() {
        //图片
//        binding.aivShowPhoto
        //按钮
        binding.tvOperation.text
        binding.tvOperation.setOnClickListener {
            println("调试socket 调试串口 ui 点击关闭")
            cabinetVM.testTypeEnd(ResultType.RESULT1)
            binding.tvOperation.isEnabled = false
            binding.tvOperation.text = "正在关闭仓门"
        }
        binding.tvMoney.setOnClickListener {
            val r = Random.nextInt(0, imageUrls.size)
//            imageUrls2.add(imageUrls[r])
//            binding.banner.setDatas(imageUrls2)
            val iv = AppCompatImageView(requireActivity()).apply {
                layoutParams =
                        LinearLayoutCompat.LayoutParams(0, LinearLayoutCompat.LayoutParams.MATCH_PARENT).apply {
                            weight = 1f
                            setMargins(20, 20, 20, 20)
                        }
                scaleType = ImageView.ScaleType.MATRIX
            }
            Glide.with(requireActivity()).load(imageUrls[r]).into(iv)
            binding.llPhoto.addView(iv)
            scrollToPosition(binding.llPhoto.size)
        }
        //接收图片
        LiveBus.get(BusType.BUS_DELIVERY_PHOTO).observeForever { filepath ->
            val iv = AppCompatImageView(requireActivity()).apply {
                layoutParams =
                        LinearLayoutCompat.LayoutParams(0, LinearLayoutCompat.LayoutParams.MATCH_PARENT).apply {
                            weight = 1f
                            setMargins(20, 20, 20, 20)
                        }
                scaleType = ImageView.ScaleType.MATRIX
            }
            Glide.with(requireActivity()).load(filepath).into(iv)
            binding.llPhoto.addView(iv)
            scrollToPosition(binding.llPhoto.size)

        }
    }

    private fun initCountdown() {
        setCountdown(300)
        //倒计时
        binding.cpvView.setMaxProgress(300)
        cabinetVM.startTimer(300)
    }

    fun scrollToPosition(position: Int) {
        val container = binding.llPhoto
        if (position < 0 || position >= container.childCount) return

        val targetView = container.getChildAt(position)
        val scrollView = binding.hsv

        // 计算目标视图的居中位置
        val viewLeft = targetView.left
        val viewWidth = targetView.width
        val scrollWidth = scrollView.width
        val targetScroll = viewLeft - (scrollWidth - viewWidth) / 2

        scrollView.smoothScrollTo(targetScroll, 0)
    }

    private fun refresh() {
        //标题
        binding.tvTitle
        val curWeightValue = when (cabinetVM.doorGeX) {
            CmdCode.GE1 -> {
                cabinetVM.weight1AfterIng ?: "0.00"
            }

            CmdCode.GE2 -> {
                cabinetVM.weight2AfterIng ?: "0.00"
            }

            else -> {
                "0.00"
            }
        }
        //当前称重
        binding.tvWeightValue.text = "$curWeightValue 公斤"

        val price = cabinetVM.curGePrice ?: "0.6"
        println("调试socket 调试串口 ui 重量：${curWeightValue} | $price")
        val floatValue = cabinetVM.multiplyFloats(price, curWeightValue)
        //当前金额
        binding.tvMoneyValue.text = "$floatValue 元"
    }

    fun initCameraX() {
        val manager = mActivity?.supportFragmentManager
        manager?.let {
            val beginTransaction = it.beginTransaction()
            val f = CameraX1Fragment()
            f?.let { fragment ->
                beginTransaction.add(R.id.frame1, fragment, "station_ad")
                beginTransaction.commit()
            }
        }
    }
    /****************************************权限管理回调***************************************************/
    /****************************************权限管理回调***************************************************/
    var imageUrls2 = mutableListOf<String>()
    var imageUrls =
            listOf("https://img.zcool.cn/community/01b72057a7e0790000018c1bf4fce0.png", "https://img.zcool.cn/community/016a2256fb63006ac7257948f83349.jpg", "https://img.zcool.cn/community/01233056fb62fe32f875a9447400e1.jpg", "https://img.zcool.cn/community/01700557a7f42f0000018c1bd6eb23.jpg")
//
//    private fun initBanner2() {
//        binding.banner.apply {
//            addBannerLifecycleObserver(requireActivity())
//            setBannerRound(20f)
//            indicator = RoundLinesIndicator(requireActivity())
//            setAdapter(ImageAdapter(imageUrls2))
//        }
//    }
//
//    private fun initBanner() {
//        //使用默认的图片适配器
//        binding.banner.apply {
//            addBannerLifecycleObserver(requireActivity())
//            indicator = CircleIndicator(requireActivity())
//            setAdapter(object : BannerImageAdapter<String>(imageUrls) {
//                override fun onBindView(holder: BannerImageHolder, data: String, position: Int, size: Int) {
//                    Glide.with(requireActivity()).load(data).into(holder.imageView)
//                }
//            })
//        }
//    }

    private fun upgradeAi() {
        // 在 Activity/Fragment 中收集状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cabinetVM.countdownState.collect { state ->
                    when (state) {
                        is CountdownTimer.CountdownState.Starting -> {
                            binding.cpvView.setMaxProgress(300)

                        }

                        is CountdownTimer.CountdownState.Running -> {
                            // 更新 UI
                            binding.cpvView.setProgress(state.secondsRemaining)
                        }

                        CountdownTimer.CountdownState.Finished -> {
                            mActivity?.fragmentCoordinator?.navigateBack()
                            cabinetVM.testTypeEnd(ResultType.RESULT2)
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
