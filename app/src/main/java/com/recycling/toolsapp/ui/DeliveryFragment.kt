package com.recycling.toolsapp.ui
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentDeliveryBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.PermissionsRequester
import com.recycling.toolsapp.vm.CabinetVM
import com.recycling.toolsapp.vm.CountdownTimer
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.indicator.RoundLinesIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import kotlin.random.Random


/***
 * 称重页
 */
@AndroidEntryPoint class DeliveryFragment : BaseBindFragment<FragmentDeliveryBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
    //隐藏人脸示意图
    private var hideFaceSchematic = false
    private lateinit var permissionsManager: PermissionsRequester
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector? = null
    //0.前置 1.后置 2.外接
    private var LENS_FACING_TYPE = 2
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
        binding.aivShowPhoto
        //按钮
        binding.tvOperation.text
        binding.tvOperation.setOnClickListener {
            mActivity?.fragmentCoordinator?.navigateBack()
            cabinetVM.testTypeEnd(1)

        }
        binding.banner
//        initBanner()
        initBanner2()
        upgradeAi()
        cabinetVM.startTimer(60)
        binding.tvMoney.setOnClickListener {
            val r = Random.nextInt(0, imageUrls.size)
            imageUrls2.add(imageUrls[r])
            binding.banner.setDatas(imageUrls2)
        }

    }

    /****************************************权限管理回调***************************************************/
    var imageUrls2 = mutableListOf<String>()
    var imageUrls =
            listOf("https://img.zcool.cn/community/01b72057a7e0790000018c1bf4fce0.png", "https://img.zcool.cn/community/016a2256fb63006ac7257948f83349.jpg", "https://img.zcool.cn/community/01233056fb62fe32f875a9447400e1.jpg", "https://img.zcool.cn/community/01700557a7f42f0000018c1bd6eb23.jpg")

    private fun initBanner2() {
        binding.banner.apply {
            addBannerLifecycleObserver(requireActivity())
            setBannerRound(20f)
            indicator = RoundLinesIndicator(requireActivity())
            setAdapter(ImageAdapter(imageUrls2))
        }
    }

    private fun initBanner() {
        //使用默认的图片适配器
        binding.banner.apply {
            addBannerLifecycleObserver(requireActivity())
            indicator = CircleIndicator(requireActivity())
            setAdapter(object : BannerImageAdapter<String>(imageUrls) {
                override fun onBindView(holder: BannerImageHolder, data: String, position: Int, size: Int) {
                    Glide.with(requireActivity()).load(data).into(holder.imageView)
                }
            })
        }
    }

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
                            cabinetVM.testTypeEnd(2)
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
