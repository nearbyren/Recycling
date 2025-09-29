//package com.recycling.toolsapp.ui.test
//
//import android.Manifest.permission.CAMERA
//import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//import android.annotation.SuppressLint
//import android.content.ContentValues
//import android.content.Context
//import android.hardware.camera2.CameraCharacteristics
//import android.hardware.camera2.CameraManager
//import android.media.MediaScannerConnection
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.text.TextUtils
//import android.util.Log
//import android.util.Size
//import android.widget.Toast
//import androidx.camera.camera2.interop.Camera2CameraInfo
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ExperimentalLensFacing
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.ImageCaptureException
//import androidx.camera.core.Preview
//import androidx.camera.core.resolutionselector.ResolutionSelector
//import androidx.camera.core.resolutionselector.ResolutionStrategy
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.video.MediaStoreOutputOptions
//import androidx.camera.video.Quality
//import androidx.camera.video.QualitySelector
//import androidx.camera.video.Recorder
//import androidx.camera.video.VideoCapture
//import androidx.camera.video.VideoRecordEvent
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.viewModels
//import com.recycling.toolsapp.R
//import com.recycling.toolsapp.databinding.FragmentCameraxBinding
//import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
//import com.recycling.toolsapp.utils.PermissionRequest
//import com.recycling.toolsapp.utils.PermissionUtils
//import com.recycling.toolsapp.utils.PermissionsRequester
//import com.recycling.toolsapp.vm.CabinetVM
//import com.serial.port.utils.AppUtils
//import com.serial.port.utils.Loge
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//import top.zibin.luban.Luban
//import top.zibin.luban.OnCompressListener
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//
///***
// * 称重页
// */
//@AndroidEntryPoint class CameraXFragment : BaseBindFragment<FragmentCameraxBinding>() {
//    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
//    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
//
//    private lateinit var permissionsManager: PermissionsRequester
//
//    //0.前置 1.后置 2.外接
//    private var LENS_FACING_TYPE0 = 0
//    private var LENS_FACING_TYPE1 = 1
//
//    // 创建任务队列
//    override fun layoutRes(): Int {
//        return R.layout.fragment_camerax
//    }
//
//    override fun isShowActionBar(): Boolean {
//        return false
//    }
//
//    override fun isShowActionBarBack(): Boolean {
//        return false
//    }
//
//
//    override fun initialize(savedInstanceState: Bundle?) {
//        setCountdown(900)
//        initPermissions()
//        initCameraX()
//        binding.atvPicture1.setOnClickListener {
//            takePicture1()
//        }
//        binding.atvRecording1.setOnClickListener {
//            startRecording1()
//        }
//        binding.atvStopRecording1.setOnClickListener {
//            stopRecording1()
//        }
//        binding.atvPicture2.setOnClickListener {
//            takePicture2()
//        }
//        binding.atvRecording2.setOnClickListener {
//            startRecording2()
//        }
//        binding.atvStopRecording2.setOnClickListener {
//            stopRecording2()
//        }
//    }
//
//    private fun initCameraX() {
//        startFaceUi()
//    }
//
//    // 在类顶部添加这些变量
//
//    private var isRecording1 = false
//    private var isRecording2 = false
//    private fun startLowLatencyPreview1() {
//        Log.e("TestFace", "网络导入用户信息 startLowLatencyPreview")
//        // 步骤1：立即启动低分辨率预览
//        val resolutionSelector =
//                ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(640, 480), // 目标分辨率
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()
//
//        val preview =
//                Preview.Builder().setResolutionSelector(resolutionSelector) // 替代 setTargetResolution
//                    .build().also {
//                        it.surfaceProvider = binding.previewView1.surfaceProvider
//                    }
//        val preview2 =
//                Preview.Builder().setResolutionSelector(resolutionSelector) // 替代 setTargetResolution
//                    .build().also {
//                        it.surfaceProvider = binding.previewView2.surfaceProvider
//                    }
//        // 初始化 ImageCapture (拍照)
//        cabinetVM.imageCapture1 =
//                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetResolution(Size(640, 480)).build()
//        cabinetVM.imageCapture2 =
//                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetResolution(Size(640, 480)).build()
//        // 初始化 VideoCapture (录像)
//        val recorder =
//                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
//        cabinetVM.videoCapture1 = VideoCapture.withOutput(recorder)
//        cabinetVM.videoCapture2 = VideoCapture.withOutput(recorder)
//
//        // 步骤2：快速绑定摄像头
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        val cameraProviderFuture2 = ProcessCameraProvider.getInstance(requireContext())
//        cabinetVM.cameraProvider1 = cameraProviderFuture.get()
//        cabinetVM.cameraProvider2 = cameraProviderFuture2.get()
//        cabinetVM.cameraProvider1?.unbindAll()
//        cabinetVM.cameraProvider2?.unbindAll()
//
//
//        cameraProviderFuture.addListener({
//            try {
//                Log.e("TestFace", "网络导入用户信息 FaceApplication.cameraProviderFuture addListener")
//
//                    cabinetVM.cameraProvider1?.bindToLifecycle(viewLifecycleOwner, createCustomCameraSelectorForUsb1(), preview, cabinetVM.imageCapture1, cabinetVM.videoCapture1)
//            } catch (e: Exception) {
//                Log.e("TestFace", "网络导入用户信息 Fast start failed: ${e.stackTraceToString()}")
//            }
//        }, ContextCompat.getMainExecutor(mActivity?.baseContext!!))
//        cameraProviderFuture.addListener({
//            try {
//                Log.e("TestFace", "网络导入用户信息 FaceApplication.cameraProviderFuture addListener")
//
//                cabinetVM.cameraProvider2?.bindToLifecycle(viewLifecycleOwner, createCustomCameraSelectorForUsb2(), preview2, cabinetVM.imageCapture2, cabinetVM.videoCapture2)
//            } catch (e: Exception) {
//                Log.e("TestFace", "网络导入用户信息 Fast start failed: ${e.stackTraceToString()}")
//            }
//        }, ContextCompat.getMainExecutor(mActivity?.baseContext!!))
//
//
//    }
//
//    private fun createCustomCameraSelectorForUsb1(): CameraSelector {
//        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraIdList = cameraManager.cameraIdList
//        // 遍历所有摄像头ID，查找USB摄像头
//        for (cameraId in cameraIdList) {
//            Loge.e("测试我来了 2 $cameraId")
//            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
//            // 这里需要根据USB摄像头的特性来筛选，例如通过镜头朝向、传感器信息等
//            // 注意：CameraCharacteristics 中可能没有直接标识是否为USB摄像头的字段
//            // 一种常见方式是通过镜头朝向（LENS_FACING_EXTERNAL）或尝试打开摄像头并读取其支持的配置来判断
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
//                Loge.e("测试我来了 2 if ")
//                // 假设你找到了第一个外部摄像头
//                return CameraSelector.Builder().addCameraFilter { cameraInfos ->
//                    cameraInfos.filter {
//                        Loge.e("测试我来了 2 filter ${ Camera2CameraInfo.from(it).cameraId}")
//                        Camera2CameraInfo.from(it).cameraId == cameraId
//                    }
//                }.build()
//            }
//        }
//        // 如果没有找到，回退到默认后置摄像头（或其他处理方式）
//        return CameraSelector.DEFAULT_FRONT_CAMERA
//    }
//    private fun createCustomCameraSelectorForUsb2(): CameraSelector {
//        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraIdList = cameraManager.cameraIdList
//        // 遍历所有摄像头ID，查找USB摄像头
//        for (cameraId in cameraIdList) {
//            Loge.e("测试我来了 2 $cameraId")
//            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
//            // 这里需要根据USB摄像头的特性来筛选，例如通过镜头朝向、传感器信息等
//            // 注意：CameraCharacteristics 中可能没有直接标识是否为USB摄像头的字段
//            // 一种常见方式是通过镜头朝向（LENS_FACING_EXTERNAL）或尝试打开摄像头并读取其支持的配置来判断
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
//                Loge.e("测试我来了 2 if ")
//                // 假设你找到了第一个外部摄像头
//                return CameraSelector.Builder().addCameraFilter { cameraInfos ->
//                    cameraInfos.filter {
//                        Loge.e("测试我来了 2 filter ${ Camera2CameraInfo.from(it).cameraId}")
//                        Camera2CameraInfo.from(it).cameraId == cameraId
//                    }
//                }.build()
//            }
//        }
//        // 如果没有找到，回退到默认后置摄像头（或其他处理方式）
//        return CameraSelector.DEFAULT_BACK_CAMERA
//    }
//
//
//    private fun takePicture1() {
//        // 创建时间戳名称和存储路径
//        val name =
//                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        // 创建输出选项
//        val outputOptions =
//                ImageCapture.OutputFileOptions.Builder(requireContext().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
//
//        // 执行拍照
//        cabinetVM.imageCapture1?.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
//            override fun onError(exc: ImageCaptureException) {
//                Log.e("TestFace", "拍照失败: ${exc.message}", exc)
//                // 显示错误提示
//                Toast.makeText(requireContext(), "拍照失败: ${exc.message}", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                val msg = "照片已保存: ${output.savedUri}"
//                Log.d("TestFace", msg)
//                // 显示成功提示
//                Toast.makeText(requireContext(), "照片已保存", Toast.LENGTH_SHORT).show()
//
//                // 可以在这里处理保存的照片
//                output.savedUri?.let { uri ->
//                    // 例如: 显示预览或上传到服务器
//                    handleCapturedImage1(uri)
//                }
//            }
//        })
//    }
//
//    // 处理捕获的图像（可选）
//    private fun handleCapturedImage1(uri: Uri) {
//        // 这里可以添加图像处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "捕获的图像URI: $uri")
//        try {
//            // 获取目标目录
//            val downloadsDir = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//            val aitionDir = File(downloadsDir, "aition")
//
//            // 确保目录存在
//            if (!aitionDir.exists()) {
//                aitionDir.mkdirs()
//            }
//
//            // 创建时间戳文件名
//            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val fileName = "1IMG_${timestamp}.jpg"
//            val destFile = File(aitionDir, fileName)
//
//            // 从原始URI读取内容并写入目标文件
//            val inputStream = requireContext().contentResolver.openInputStream(uri)
//            val outputStream = FileOutputStream(destFile)
//
//            inputStream?.use { input ->
//                outputStream.use { output ->
//                    input.copyTo(output)
//                }
//            }
//
//            // 更新媒体库（可选，使文件在相册中可见）
//            MediaScannerConnection.scanFile(
//                requireContext(),
//                arrayOf(destFile.absolutePath),
//                arrayOf("image/jpeg"),
//                null
//            )
//
//            Log.d("TestFace", "照片已保存到: ${destFile.absolutePath}")
//            Toast.makeText(requireContext(), "照片已保存到Aition目录", Toast.LENGTH_SHORT).show()
//
//            // 可以在这里添加额外的处理逻辑，如上传到服务器等
//            Luban.with(requireContext()).load(destFile.absolutePath)
//                .ignoreBy(100).setTargetDir(aitionDir.absolutePath).filter {
//                    path -> !(TextUtils.isEmpty(path) || path.lowercase(Locale.getDefault()).endsWith(".gif")) }
//                .setCompressListener(object : OnCompressListener {
//                override fun onStart() {
//                    Loge.e("测试我来了 onStart")
//
//                }
//
//                override fun onSuccess(file: File) {
//                    Loge.e("测试我来了 onSuccess ${file.absolutePath}")
//
//                }
//
//                override fun onError(e: Throwable) {
//                    Loge.e("测试我来了 onError")
//
//                }
//            }).launch()
//
//        } catch (e: Exception) {
//            Log.e("TestFace", "保存照片失败: ${e.message}", e)
//            Toast.makeText(requireContext(), "保存照片失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//    private fun startRecording1() {
//        if (isRecording1) {
//            Log.d("TestFace", "已经在录制中")
//            return
//        }
//
//        // 创建时间戳名称和存储路径
//        val name =
//                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//
//        // 创建媒体存储输出选项
//        val mediaStoreOutput =
//                MediaStoreOutputOptions.Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build()
//
//        // 开始录制
//        cabinetVM.recording1 =
//                cabinetVM.videoCapture1?.output?.prepareRecording(requireContext(), mediaStoreOutput)?.withAudioEnabled()  // 启用音频录制（需要麦克风权限）
//                    ?.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
//                        when (recordEvent) {
//                            is VideoRecordEvent.Start -> {
//                                isRecording1 = true
//                                Log.d("TestFace", "开始录制")
//                                Toast.makeText(requireContext(), "开始录制", Toast.LENGTH_SHORT).show()
//                                // 更新UI，例如显示录制中状态
//                            }
//
//                            is VideoRecordEvent.Finalize -> {
//                                isRecording1 = false
//                                if (!recordEvent.hasError()) {
//                                    val msg = "视频已保存: ${recordEvent.outputResults.outputUri}"
//                                    Log.d("TestFace", msg)
//                                    Toast.makeText(requireContext(), "视频已保存", Toast.LENGTH_SHORT).show()
//
//                                    // 处理保存的视频
//                                    recordEvent.outputResults.outputUri?.let { uri ->
//                                        handleRecordedVideo1(uri)
//                                    }
//                                } else {
//                                    Log.e("TestFace", "录制错误: ${recordEvent.error}")
//                                    cabinetVM.recording1?.close()
//                                    cabinetVM.recording1 = null
//                                    Toast.makeText(requireContext(), "录制失败", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        }
//                    }
//    }
//
//    private fun stopRecording1() {
//        if (!isRecording1) {
//            Log.d("TestFace", "当前没有在录制")
//            return
//        }
//
//        cabinetVM.recording1?.stop()
//        cabinetVM.recording1 = null
//        isRecording1 = false
//        Log.d("TestFace", "停止录制")
//    }
//
//    // 处理录制的视频（可选）
//    private fun handleRecordedVideo1(uri: Uri) {
//        // 这里可以添加视频处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "录制的视频URI: $uri")
//        try {
//            // 获取目标目录
//            val downloadsDir = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//            val aitionDir = File(downloadsDir, "aition")
//
//            // 确保目录存在
//            if (!aitionDir.exists()) {
//                aitionDir.mkdirs()
//            }
//
//            // 创建时间戳文件名
//            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val fileName = "VID_${timestamp}.mp4"
//            val destFile = File(aitionDir, fileName)
//
//            // 从原始URI读取内容并写入目标文件
//            val inputStream = requireContext().contentResolver.openInputStream(uri)
//            val outputStream = FileOutputStream(destFile)
//
//            inputStream?.use { input ->
//                outputStream.use { output ->
//                    input.copyTo(output)
//                }
//            }
//
//            // 更新媒体库（可选，使文件在相册中可见）
//            MediaScannerConnection.scanFile(
//                requireContext(),
//                arrayOf(destFile.absolutePath),
//                arrayOf("video/mp4"),
//                null
//            )
//
//            Log.d("TestFace", "视频已保存到: ${destFile.absolutePath}")
//            Toast.makeText(requireContext(), "视频已保存到Aition目录", Toast.LENGTH_SHORT).show()
//
//            // 可以在这里添加额外的处理逻辑，如上传到服务器等
//
//        } catch (e: Exception) {
//            Log.e("TestFace", "保存视频失败: ${e.message}", e)
//            Toast.makeText(requireContext(), "保存视频失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//    private fun startLowLatencyPreview2() {
//        Log.e("TestFace", "网络导入用户信息 startLowLatencyPreview")
//        // 步骤1：立即启动低分辨率预览
//        val resolutionSelector =
//                ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(640, 480), // 目标分辨率
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()
//
//        val preview =
//                Preview.Builder().setResolutionSelector(resolutionSelector) // 替代 setTargetResolution
//                    .build().also {
//                        it.surfaceProvider = binding.previewView2.surfaceProvider
//                    }
//        // 初始化 ImageCapture (拍照)
//        cabinetVM.imageCapture2 =
//                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetResolution(Size(640, 480)).build()
//
//        // 初始化 VideoCapture (录像)
//        val recorder =
//                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
//        cabinetVM.videoCapture2 = VideoCapture.withOutput(recorder)
//
//        // 步骤2：快速绑定摄像头
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraProviderFuture.addListener({
//            try {
//                Log.e("TestFace", "网络导入用户信息 FaceApplication.cameraProviderFuture addListener")
//                cabinetVM.cameraProvider2 = cameraProviderFuture.get()
//                cabinetVM.cameraSelector2 = findExternalCamera2(LENS_FACING_TYPE1)
//                cabinetVM.cameraProvider2?.unbindAll()
//                cabinetVM.cameraSelector2?.let {
//                    cabinetVM.cameraProvider2?.bindToLifecycle(viewLifecycleOwner, it, preview, cabinetVM.imageCapture2, cabinetVM.videoCapture2)
//                }
//
//            } catch (e: Exception) {
//                Log.e("TestFace", "网络导入用户信息 Fast start failed: ${e.stackTraceToString()}")
//            }
//        }, ContextCompat.getMainExecutor(mActivity?.baseContext!!))
//    }
//    private fun takePicture2() {
//        // 创建时间戳名称和存储路径
//        val name =
//                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        // 创建输出选项
//        val outputOptions =
//                ImageCapture.OutputFileOptions.Builder(requireContext().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
//
//        // 执行拍照
//        cabinetVM.imageCapture2?.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
//            override fun onError(exc: ImageCaptureException) {
//                Log.e("TestFace", "拍照失败: ${exc.message}", exc)
//                // 显示错误提示
//                Toast.makeText(requireContext(), "拍照失败: ${exc.message}", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                val msg = "照片已保存: ${output.savedUri}"
//                Log.d("TestFace", msg)
//                // 显示成功提示
//                Toast.makeText(requireContext(), "照片已保存", Toast.LENGTH_SHORT).show()
//
//                // 可以在这里处理保存的照片
//                output.savedUri?.let { uri ->
//                    // 例如: 显示预览或上传到服务器
//                    handleCapturedImage2(uri)
//                }
//            }
//        })
//    }
//    private fun startRecording2() {
//        if (isRecording1) {
//            Log.d("TestFace", "已经在录制中")
//            return
//        }
//
//        // 创建时间戳名称和存储路径
//        val name =
//                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//
//        // 创建媒体存储输出选项
//        val mediaStoreOutput =
//                MediaStoreOutputOptions.Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build()
//
//        // 开始录制
//        cabinetVM.recording2 =
//                cabinetVM.videoCapture2?.output?.prepareRecording(requireContext(), mediaStoreOutput)?.withAudioEnabled()  // 启用音频录制（需要麦克风权限）
//                    ?.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
//                        when (recordEvent) {
//                            is VideoRecordEvent.Start -> {
//                                isRecording2 = true
//                                Log.d("TestFace", "开始录制")
//                                Toast.makeText(requireContext(), "开始录制", Toast.LENGTH_SHORT).show()
//                                // 更新UI，例如显示录制中状态
//                            }
//
//                            is VideoRecordEvent.Finalize -> {
//                                isRecording2 = false
//                                if (!recordEvent.hasError()) {
//                                    val msg = "视频已保存: ${recordEvent.outputResults.outputUri}"
//                                    Log.d("TestFace", msg)
//                                    Toast.makeText(requireContext(), "视频已保存", Toast.LENGTH_SHORT).show()
//
//                                    // 处理保存的视频
//                                    recordEvent.outputResults.outputUri?.let { uri ->
//                                        handleRecordedVideo1(uri)
//                                    }
//                                } else {
//                                    Log.e("TestFace", "录制错误: ${recordEvent.error}")
//                                    cabinetVM.recording2?.close()
//                                    cabinetVM.recording2 = null
//                                    Toast.makeText(requireContext(), "录制失败", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        }
//                    }
//    }
//    private fun stopRecording2() {
//        if (!isRecording2) {
//            Log.d("TestFace", "当前没有在录制")
//            return
//        }
//
//        cabinetVM.recording2?.stop()
//        cabinetVM.recording2 = null
//        isRecording2 = false
//        Log.d("TestFace", "停止录制")
//    }
//
//    // 处理捕获的图像（可选）
//    private fun handleCapturedImage2(uri: Uri) {
//        // 这里可以添加图像处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "捕获的图像URI: $uri")
//    }
//    // 处理录制的视频（可选）
//    private fun handleRecordedVideo2(uri: Uri) {
//        // 这里可以添加视频处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "录制的视频URI: $uri")
//        try {
//            // 获取目标目录
//            val downloadsDir = AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//            val aitionDir = File(downloadsDir, "aition")
//
//            // 确保目录存在
//            if (!aitionDir.exists()) {
//                aitionDir.mkdirs()
//            }
//
//            // 创建时间戳文件名
//            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val fileName = "VID_${timestamp}.mp4"
//            val destFile = File(aitionDir, fileName)
//
//            // 从原始URI读取内容并写入目标文件
//            val inputStream = requireContext().contentResolver.openInputStream(uri)
//            val outputStream = FileOutputStream(destFile)
//
//            inputStream?.use { input ->
//                outputStream.use { output ->
//                    input.copyTo(output)
//                }
//            }
//
//            // 更新媒体库（可选，使文件在相册中可见）
//            MediaScannerConnection.scanFile(
//                requireContext(),
//                arrayOf(destFile.absolutePath),
//                arrayOf("video/mp4"),
//                null
//            )
//
//            Log.d("TestFace", "视频已保存到: ${destFile.absolutePath}")
//            Toast.makeText(requireContext(), "视频已保存到Aition目录", Toast.LENGTH_SHORT).show()
//
//            // 可以在这里添加额外的处理逻辑，如上传到服务器等
//
//        } catch (e: Exception) {
//            Log.e("TestFace", "保存视频失败: ${e.message}", e)
//            Toast.makeText(requireContext(), "保存视频失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    /***
//     *  查找外接摄像头
//     * @param selector
//     *  0 前置摄像头头
//     *  1 后置摄像头头
//     *  2 外接摄像头头
//     *  -1 未知摄像头
//     */
//    @androidx.annotation.OptIn(ExperimentalLensFacing::class) @OptIn(ExperimentalLensFacing::class)
//    @SuppressLint("RestrictedApi")
//    private fun findExternalCamera1(selector: Int = CameraSelector.LENS_FACING_EXTERNAL): CameraSelector? {
//        Log.e("TestFace", "网络导入用户信息 findExternalCamera $selector")
//        return cabinetVM.cameraProvider1?.availableCameraInfos?.firstOrNull { info ->
//            val cameraSelector = when (selector) {
//                CameraSelector.LENS_FACING_FRONT -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT
//                }
//
//                CameraSelector.LENS_FACING_BACK -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK
//                }
//
//                CameraSelector.LENS_FACING_EXTERNAL -> {   // 通过特性判断外接摄像头
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_EXTERNAL
//                }
//
//                else -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_UNKNOWN
//                }
//            }
//            cameraSelector
//        }?.cameraSelector
//    }
//    /***
//     *  查找外接摄像头
//     * @param selector
//     *  0 前置摄像头头
//     *  1 后置摄像头头
//     *  2 外接摄像头头
//     *  -1 未知摄像头
//     */
//    @androidx.annotation.OptIn(ExperimentalLensFacing::class) @OptIn(ExperimentalLensFacing::class)
//    @SuppressLint("RestrictedApi")
//    private fun findExternalCamera2(selector: Int = CameraSelector.LENS_FACING_EXTERNAL): CameraSelector? {
//        Log.e("TestFace", "网络导入用户信息 findExternalCamera $selector")
//        return cabinetVM.cameraProvider2?.availableCameraInfos?.firstOrNull { info ->
//            val cameraSelector = when (selector) {
//                CameraSelector.LENS_FACING_FRONT -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT
//                }
//
//                CameraSelector.LENS_FACING_BACK -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK
//                }
//
//                CameraSelector.LENS_FACING_EXTERNAL -> {   // 通过特性判断外接摄像头
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_EXTERNAL
//                }
//
//                else -> {
//                    info.cameraSelector.lensFacing == CameraSelector.LENS_FACING_UNKNOWN
//                }
//            }
//            cameraSelector
//        }?.cameraSelector
//    }
//    /** 暂停相机 */
//    private fun pauseCamera() {
//        Log.e("TestFace", "网络导入用户信息 pauseCamera ")
//        cabinetVM.cameraProvider1?.unbindAll()
////        camerax141 = null
//    }
//
//    /***
//     * 启动打开
//     */
//    private fun startFaceUi() {
//        Log.e("TestFace", "网络导入用户信息 startFaceUi if")
//        if (PermissionUtils.hasSelfPermissions(mActivity?.baseContext!!, CAMERA)) {
//            Log.e("TestFace", "网络导入用户信息 startFaceUi if if")
//            cabinetVM.mainScope.launch {
//                startLowLatencyPreview1()
////                delay(3000)
////                startLowLatencyPreview2()
//            }
//
//
//        } else {
//            Log.e("TestFace", "网络导入用户信息 startFaceUi if else")
//            goPermissions()
//        }
//    }
//
//    /****************************************权限管理回调***************************************************/
//
//    private fun initPermissions() {
//        Log.e("TestFace", "权限申请 initPermissions")
//        permissionsManager =
//                PermissionsRequester(CAMERA, WRITE_EXTERNAL_STORAGE, activity = mActivity!!, onShowRationale = ::dealShowRationale, onPermissionDenied = ::dealPermissionDenied, onNeverAskAgain = ::dealNeverAskAgain, requiresPermission = ::dealRequiresPermission)
//        permissionsManager.launch()
//    }
//
//    private fun goPermissions() {
//        Log.e("TestFace", "权限申请 initPermissions")
//        permissionsManager =
//                PermissionsRequester(CAMERA, activity = mActivity!!, onShowRationale = ::dealShowRationale, onPermissionDenied = ::dealPermissionDenied, onNeverAskAgain = ::dealNeverAskAgain, requiresPermission = ::dealRequiresPermission2)
//        permissionsManager.launch()
//    }
//
//    fun dealPermissionDenied() {
//        Log.e("TestFace", "权限申请 dealPermissionDenied")
////        Toast.makeText(mActivity?.baseContext!!, "某些权限被拒绝", Toast.LENGTH_LONG).show()
//    }
//
//    fun dealRequiresPermission() {
//        Log.e("TestFace", "权限申请 dealRequiresPermission")
////        Toast.makeText(mActivity?.baseContext!!, "授予的所有权限", Toast.LENGTH_LONG).show()
//    }
//
//    fun dealRequiresPermission2() {
//        Log.e("TestFace", "权限申请 dealRequiresPermission2")
//    }
//
//    fun dealShowRationale(request: PermissionRequest) {
////        try {
////            Log.e("TestFace", "权限申请 dealShowRationale")
////            val builder = AlertDialog.Builder(mActivity?.baseContext!!)
////            val dialog = builder.create()
////            val dialogView =
////                    View.inflate(mActivity?.baseContext!!, R.layout.dialog_permission, null)
////            dialog.setView(dialogView)
////            dialog.setCanceledOnTouchOutside(false)
////            val btnCancel = dialogView.findViewById<AppCompatTextView>(R.id.cancel)
////            val btnOK = dialogView.findViewById<AppCompatTextView>(R.id.ok)
////            btnCancel.setOnClickListener { v: View? ->
////                dialog.dismiss()
////                request.cancel()
////            }
////            btnOK.setOnClickListener { v: View? ->
////                dialog.dismiss()
////                request.proceed()
////            }
////            dialog.setCanceledOnTouchOutside(false)
////            dialog.show()
////        } catch (e: Exception) {
////            e.printStackTrace()
////        }
//
//    }
//
//    fun dealNeverAskAgain() {
//        Log.e("TestFace", "权限申请 dealNeverAskAgain")
////        Toast.makeText(mActivity?.baseContext!!, "有些许可被拒绝了，再也没有问过。", Toast.LENGTH_LONG)
////            .show()
////        val intent: Intent =
////                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
////        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
////        startActivity(intent)
//    }
//
//    /****************************************权限管理回调***************************************************/
//}
