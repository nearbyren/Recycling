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
//import androidx.camera.core.ImageAnalysis
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
//import androidx.lifecycle.lifecycleScope
//import com.recycling.toolsapp.R
//import com.recycling.toolsapp.databinding.FragmentCamerax2Binding
//import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
//import com.recycling.toolsapp.utils.CmdValue
//import com.recycling.toolsapp.utils.PermissionRequest
//import com.recycling.toolsapp.utils.PermissionUtils
//import com.recycling.toolsapp.utils.PermissionsRequester
//import com.recycling.toolsapp.vm.CabinetVM
//import com.serial.port.utils.AppUtils
//import com.serial.port.utils.FileMdUtil
//import com.serial.port.utils.Loge
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//import nearby.lib.signal.livebus.BusType
//import nearby.lib.signal.livebus.LiveBus
//import top.zibin.luban.Luban
//import top.zibin.luban.OnCompressListener
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.concurrent.Executors
//
//
///***
// * 称重页
// *
// */
//@AndroidEntryPoint class CameraX2Fragment : BaseBindFragment<FragmentCamerax2Binding>() {
//    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
//    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
//
//    private lateinit var permissionsManager: PermissionsRequester
//
//    //0.前置 1.后置 2.外接
//    private var LENS_FACING_TYPE0 = 1
//
//    // 创建任务队列
//    override fun layoutRes(): Int {
//        return R.layout.fragment_camerax2
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
//        lifecycleScope.launch {
//            cabinetVM.getDelays.collect { delay ->
//                when (delay) {
//                    3000L/*, 5000L, 7000L */ -> {
//                        Log.e("TestFace", "网络导入用户信息 执行拍照 $delay")
//                        takePicture1()
//                    }
//                }
//            }
//        }
//
//        lifecycleScope.launch {
//            cabinetVM.getActive.collect { activeType ->
//                Log.e("TestFace", "网络导入用户信息 执行拍照 $activeType")
//                takePicture1(activeType)
//            }
//        }
//        binding.atvPicture2.setOnClickListener {
//            takePicture1()
//        }
//        binding.atvRecording2.setOnClickListener {
//            startRecording1()
//        }
//        binding.atvStopRecording2.setOnClickListener {
//            stopRecording1()
//        }
//
//    }
//
//    private fun initCameraX() {
//        startFaceUi()
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
//                        Loge.e("测试我来了 2 filter ${Camera2CameraInfo.from(it).cameraId}")
//                        Camera2CameraInfo.from(it).cameraId == cameraId
//                    }
//                }.build()
//            }
//        }
//        // 如果没有找到，回退到默认后置摄像头（或其他处理方式）
//        return CameraSelector.DEFAULT_BACK_CAMERA
//    }
//// 同理实现 createCustomCameraSelectorForUsb2，但要确保返回另一个摄像头
//    // 在类顶部添加这些变量
//    // 在类顶部添加这些变量
//
//    private var isRecording2 = false
//    private fun startLowLatencyPreview1() {
//        Log.e("TestFace", "网络导入用户信息 startLowLatencyPreview")
//        // 步骤1：立即启动低分辨率预览
//        val resolutionSelector =
//                ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(640, 480), // 目标分辨率
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()
//
//        val preview = Preview.Builder() // 替代 setTargetResolution
//            .build().also {
//                it.surfaceProvider = binding.previewView2.surfaceProvider
//            }
//        // 初始化 ImageCapture (拍照)
//        cabinetVM.imageCapture2 =
//                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
//
//        // 初始化 VideoCapture (录像)
//        val recorder =
//                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
//        cabinetVM.videoCapture2 = VideoCapture.withOutput(recorder)
//
//        cabinetVM.imageAnalysis2 =
//                ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build().also {
//                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
//                        Log.e("TestFace", "网络导入用户信息 setAnalyzer")
////                        cabinetVM.takePictures()
//                    }
//                }
//        // 步骤2：快速绑定摄像头
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraProviderFuture.addListener({
//            try {
//                Log.e("TestFace", "网络导入用户信息 FaceApplication.cameraProviderFuture addListener")
//                cabinetVM.cameraProvider2 = cameraProviderFuture.get()
//                cabinetVM.cameraSelector2 =
//                        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
//                cabinetVM.cameraProvider2?.unbindAll()
////                cabinetVM.cameraSelector2?.let {
//                cabinetVM.cameraProvider2?.bindToLifecycle(this@CameraX2Fragment.viewLifecycleOwner, createCustomCameraSelectorForUsb1(), preview, cabinetVM.imageCapture2, cabinetVM.videoCapture2, cabinetVM.imageAnalysis2)
////                }
//
//            } catch (e: Exception) {
//                Log.e("TestFace", "网络导入用户信息 Fast start failed: ${e.stackTraceToString()}")
//            }
//        }, ContextCompat.getMainExecutor(mActivity?.baseContext!!))
//    }
//
//
//    private fun takePicture1(activeType: String = CmdValue.CMD_OPEN_DOOR) {
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
//                    handleCapturedImage1(uri, activeType)
//                }
//            }
//        })
//    }
//
//    // 处理捕获的图像（可选）
//    private fun handleCapturedImage1(uri: Uri, activeType: String) {
//        // 这里可以添加图像处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "捕获的图像URI: $uri")
//        try {
//            // 获取目标目录
//            val downloadsDir =
//                    AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//            val aitionDir = File(downloadsDir, "action")
//
//            // 确保目录存在
//            if (!aitionDir.exists()) {
//                aitionDir.mkdirs()
//            }
//
//            // 创建时间戳文件名
//
//            val fileName = "${cabinetVM.curTransId}__${AppUtils.getDateYMDHMS()}.jpg"
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
//
//            Log.d("TestFace", "照片已保存到: ${destFile.absolutePath}")
//            Toast.makeText(requireContext(), "照片已保存到Aition目录", Toast.LENGTH_SHORT).show()
//
//            // 可以在这里添加额外的处理逻辑，如上传到服务器等
//            Luban.with(requireContext()).load(destFile.absolutePath).ignoreBy(100).setTargetDir(aitionDir.absolutePath).filter { path ->
//                !(TextUtils.isEmpty(path) || path.lowercase(Locale.getDefault()).endsWith(".gif"))
//            }.setCompressListener(object : OnCompressListener {
//                override fun onStart() {
//                    Loge.e("测试我来了 onStart")
//
//                }
//
//                override fun onSuccess(file: File) {
//                    Loge.e("测试我来了 onSuccess ${file.absolutePath}")
//                    Log.e("TestFace", "网络导入用户信息 保存成功")
//                    val downloadsDir =
//                            AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//                    val aitionDir = File(downloadsDir, "action")
//                    val fileName = file.absolutePath.substringAfterLast('/')
//                    FileMdUtil.renameFileInDir(aitionDir, "$fileName", "${cabinetVM.curTransId}--${AppUtils.getDateYMDHMS()}.jpeg")
//                    cabinetVM.taskPicAdd(file.absolutePath)
//                    when (activeType) {
//                        CmdValue.CMD_OPEN_DOOR -> {
//                            cabinetVM.photoOpenIn = file.absolutePath
//                            cabinetVM.photoOpenOut = file.absolutePath
//                        }
//
//                        CmdValue.CMD_CLOSE_DOOR -> {
//                            cabinetVM.photoCloseIn = file.absolutePath
//                            cabinetVM.photoCloseOut = file.absolutePath
//                        }
//                    }
////                    cabinetVM.toGoInsertPhoto(activeType)
//                    LiveBus.get(BusType.BUS_DELIVERY_PHOTO).post(file.absolutePath)
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
//    @SuppressLint("MissingPermission") private fun startRecording1() {
//        if (isRecording2) {
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
//
//    private fun stopRecording1() {
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
//    // 处理录制的视频（可选）
//    private fun handleRecordedVideo1(uri: Uri) {
//        // 这里可以添加视频处理逻辑
//        // 例如: 显示预览、上传到服务器等
//        Log.d("TestFace", "录制的视频URI: $uri")
//        try {
//            // 获取目标目录
//            val downloadsDir =
//                    AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//            val aitionDir = File(downloadsDir, "action")
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
//            MediaScannerConnection.scanFile(requireContext(), arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
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
//     * 启动打开
//     */
//    private fun startFaceUi() {
//        Log.e("TestFace", "网络导入用户信息 startFaceUi if")
//        if (PermissionUtils.hasSelfPermissions(mActivity?.baseContext!!, CAMERA)) {
//            Log.e("TestFace", "网络导入用户信息 startFaceUi if if")
//            cabinetVM.mainScope.launch {
//                startLowLatencyPreview1()
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
