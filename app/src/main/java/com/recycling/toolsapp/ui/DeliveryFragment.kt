package com.recycling.toolsapp.ui

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.HorizontalScrollView
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.size
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.internal.ViewUtils.dpToPx
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.PermissionRequest
import com.recycling.toolsapp.utils.PermissionUtils
import com.recycling.toolsapp.utils.PermissionsRequester
import com.recycling.toolsapp.utils.ResultType
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.serial.port.utils.Loge
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.indicator.RoundLinesIndicator
import com.youth.banner.util.BannerUtils.setBannerRound
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
        setCountdown(60)
        //倒计时
        binding.cpvView.setMaxProgress(60)
        //标题
        binding.tvTitle
        //当前称重
        binding.tvWeightValue.text
        //当前金额
        binding.tvMoneyValue.text
        //图片
//        binding.aivShowPhoto
        //按钮
        binding.tvOperation.text
        binding.tvOperation.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateBack()
            cabinetVM.testTypeEnd(ResultType.RESULT1)

        }

//        initBanner()
//        initBanner2()
        upgradeAi()
        cabinetVM.startTimer(60)
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
        initCameraX()
//        lifecycleScope.launch {
//            cabinetVM.getTakePic.collect { url ->
//                Log.e("TestFace", "网络导入用户信息 刷新照片")
//                imageUrls2.add(url)
//                binding.banner.setDatas(imageUrls2)
//            }
//        }
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
                            binding.cpvView.setMaxProgress(10)

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
