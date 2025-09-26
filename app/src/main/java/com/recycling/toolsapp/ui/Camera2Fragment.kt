package com.recycling.toolsapp.ui

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.recycling.toolsapp.R
import com.recycling.toolsapp.databinding.FragmentCamera2Binding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.FualtType
import com.recycling.toolsapp.utils.PermissionRequest
import com.recycling.toolsapp.utils.PermissionsRequester
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.FileMdUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.Locale


/***
 * 内外相机拍摄
 */
@AndroidEntryPoint class Camera2Fragment : BaseBindFragment<FragmentCamera2Binding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel  // 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
    private lateinit var permissionsManager: PermissionsRequester
    private var cameraDeviceIn: CameraDevice? = null
    private var cameraDeviceOut: CameraDevice? = null

    private var imageReaderIn: ImageReader? = null
    private var imageReaderOut: ImageReader? = null

    private var captureSessionIn: CameraCaptureSession? = null
    private var captureSessionOut: CameraCaptureSession? = null

    private var previewSizeIn: Size? = null
    private var previewSizeOut: Size? = null

    private var supportedResolutionsIn: List<Size> = ArrayList()
    private var supportedResolutionsOut: List<Size> = ArrayList()

    private var selectedCameraIdIn: String? = null
    private var selectedCameraIdOut: String? = null

    private var isPreviewActiveIn = false
    private var isPreviewActiveOut = false

    private var mediaRecorderIn: MediaRecorder? = null
    private var mediaRecorderOut: MediaRecorder? = null
    private var isRecordingIn = false
    private var isRecordingOut = false

    private var cameraManager: CameraManager? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private val cameraInfoMap: MutableMap<String, CameraInfo> = mutableMapOf()
    private val cameraIds: MutableList<String> = mutableListOf()

    // 方向转换
    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }

    // 相机信息类
    class CameraInfo internal constructor(var cameraId: String, var lensFacing: Int) {
        var displayName: String? = null

        init {
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> this.displayName = "前置摄像头:$cameraId"
                CameraCharacteristics.LENS_FACING_BACK -> this.displayName = "后置摄像头:$cameraId"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> this.displayName =
                        "外部摄像头:$cameraId"

                else -> this.displayName = "未知摄像头:$cameraId"
            }
        }

        override fun toString(): String {
            return displayName!!
        }
    }

    override fun layoutRes(): Int {
        return R.layout.fragment_camera2
    }

    override fun isShowActionBar(): Boolean {
        return false
    }

    override fun isShowActionBarBack(): Boolean {
        return false
    }

    override fun doneCountdown() {
    }

    override fun initialize(savedInstanceState: Bundle?) {
        initPermissions()
        binding.atvPictureIn.setOnClickListener {
            capturePhotoIn()
        }
        binding.atvRecordingIn.setOnClickListener {
            startRecordingIn()
        }
        binding.atvStopRecordingIn.setOnClickListener {
            stopRecordingIn()
        }

        binding.atvPictureOut.setOnClickListener {
            capturePhotoOut()
        }
        binding.atvRecordingOut.setOnClickListener {
            startRecordingOut()
        }
        binding.atvStopRecordingOut.setOnClickListener {
            stopRecordingOut()
        }
        initCamera2()
        setupCameras()
        cabinetVM.mainScope.launch {
            delay(1000)
            startPreview(1)
            delay(1000)
            startPreview(2)
        }
        lifecycleScope.launch {
            cabinetVM.getActive.collect { activeType ->
                println("调试socket 调试串口 进行拍照 $activeType")
                cabinetVM.activeType = activeType
                capturePhotoIn()
                capturePhotoOut()
            }
        }
    }

    private fun startBackgroundThread() {

    }

    private fun toGoFaultDesc(desc: String) {
        cabinetVM.toGoCmdUpFault(FualtType.TYPE5, 0, desc)
    }

    private fun setupCameras() {
        try {
            // 获取所有摄像头ID
            val cameraIdArray = cameraManager!!.cameraIdList
            cameraIds.clear()
            cameraInfoMap.clear()

            for (cameraId in cameraIdArray) {
                val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (lensFacing != null) {
                    val cameraInfo = CameraInfo(cameraId, lensFacing)
                    cameraInfoMap[cameraId] = cameraInfo
                    cameraIds.add(cameraId)
                }
            }

            if (cameraIds.isEmpty()) {
                Toast.makeText(AppUtils.getContext(), "未找到可用摄像头", Toast.LENGTH_SHORT).show()
                return
            }

            // 默认选择第一个摄像头
            if (cameraIds.size > 0) {
                selectedCameraIdIn = cameraIds[0]
                selectedCameraIdIn?.let { sc ->
                    setupResolutionSpinner(sc, 1)
                }

            }

            if (cameraIds.size > 1) {
                selectedCameraIdOut = cameraIds[1]
                selectedCameraIdOut?.let { sc ->
                    setupResolutionSpinner(sc, 2)
                }
            } else {
                selectedCameraIdOut = cameraIds[0]
                selectedCameraIdOut?.let { sc ->
                    setupResolutionSpinner(sc, 2)
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(AppUtils.getContext(), "摄像头访问错误", Toast.LENGTH_SHORT).show()
            toGoFaultDesc("摄像头访问异常")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupResolutionSpinner(cameraId: String, cameraNum: Int) {
        try {
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

            // 获取支持的预览尺寸
            val sizes = map.getOutputSizes(SurfaceTexture::class.java)
            val supportedSizes = Arrays.asList(*sizes)

            // 按面积排序
            Collections.sort(supportedSizes, CompareSizesByArea())

            // 保存支持的尺寸
            if (cameraNum == 1) {
                supportedResolutionsIn = supportedSizes
            } else {
                supportedResolutionsOut = supportedSizes
            }

            // 创建分辨率选项
            val resolutionOptions: MutableList<String> = java.util.ArrayList()
            for (size in supportedSizes) {
                resolutionOptions.add(size.width.toString() + "x" + size.height)
            }

            // 设置默认选择（最高分辨率）
            if (!supportedSizes.isEmpty()) {

                // 保存默认预览尺寸
                if (cameraNum == 1) {
                    previewSizeIn = supportedSizes[0]
                } else {
                    previewSizeOut = supportedSizes[0]
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            toGoFaultDesc("摄像获取尺寸异常")
        }
    }

    private val textureListenerIn: TextureView.SurfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    // 表面可用，可以开始预览
                    if (isPreviewActiveIn) {
                        startPreview(1)
                    }
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    // 调整预览尺寸
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                }
            }

    private val textureListenerOut: TextureView.SurfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    // 表面可用，可以开始预览
                    if (isPreviewActiveOut) {
                        startPreview(2)
                    }
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    // 调整预览尺寸
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                }
            }

    fun startPreview(cameraNum: Int) {
        if (ActivityCompat.checkSelfPermission(AppUtils.getContext(), CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(AppUtils.getContext(), "需要相机权限", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = if (cameraNum == 1) selectedCameraIdIn else selectedCameraIdOut
        val previewSize = if (cameraNum == 1) previewSizeIn else previewSizeOut

        if (cameraId == null || previewSize == null) {
            Toast.makeText(AppUtils.getContext(), "请先选择摄像头和分辨率", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 关闭当前相机（如果已打开）
            if (cameraNum == 1 && cameraDeviceIn != null) {
                cameraDeviceIn?.close()
                cameraDeviceIn = null
            } else if (cameraNum == 2 && cameraDeviceOut != null) {
                cameraDeviceOut?.close()
                cameraDeviceOut = null
            }

            // 打开相机
            cameraManager!!.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (cameraNum == 1) {
                        cameraDeviceIn = camera
                        // 初始化 ImageReader（JPEG 格式）
                        imageReaderIn =
                                ImageReader.newInstance(previewSizeIn!!.width, previewSizeIn!!.height, ImageFormat.JPEG, 2)
                        imageReaderIn?.setOnImageAvailableListener(imageAvailableListenerIn, backgroundHandler)
                        isPreviewActiveIn = true
//                        updatePreviewUI(1, true)
                        cameraDeviceIn?.let { createCameraPreviewSessionIn(it, previewSize) }
                    } else {
                        cameraDeviceOut = camera
                        // 初始化 ImageReader（JPEG 格式）
                        imageReaderOut =
                                ImageReader.newInstance(previewSizeOut!!.width, previewSizeOut!!.height, ImageFormat.JPEG, 2)
                        imageReaderOut?.setOnImageAvailableListener(imageAvailableListenerOut, backgroundHandler)

                        isPreviewActiveOut = true
//                        updatePreviewUI(1, true)
                        cameraDeviceOut?.let { createCameraPreviewSessionOut(it, previewSize) }
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (cameraNum == 1) {
                        cameraDeviceIn = null
                        isPreviewActiveIn = false
//                        updatePreviewUI(1, false)
                    } else {
                        cameraDeviceOut = null
                        isPreviewActiveOut = false
//                        updatePreviewUI(2, false)
                    }
                    Toast.makeText(AppUtils.getContext(), "摄像头连接断开", Toast.LENGTH_SHORT).show()

                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (cameraNum == 1) {
                        cameraDeviceIn = null
                        isPreviewActiveIn = false
                        toGoFaultDesc("摄像头打开失败内")

//                        updatePreviewUI(1, false)
                    } else {
                        cameraDeviceOut = null
                        isPreviewActiveOut = false
                        toGoFaultDesc("摄像头打开失败外")

//                        updatePreviewUI(2, false)
                    }
                    Toast.makeText(AppUtils.getContext(), "摄像头打开失败", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(AppUtils.getContext(), "摄像头访问错误", Toast.LENGTH_SHORT).show()
            toGoFaultDesc("摄像头访问错误")
        }
    }

    private fun createCameraPreviewSessionIn(cameraDevice: CameraDevice, previewSize: Size) {
        try {
            val texture: SurfaceTexture? = binding.textureIn.surfaceTexture
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            val previewSurface = Surface(texture)

            val previewRequestBuilder1 =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder1.addTarget(previewSurface)

            // **同时绑定 ImageReader Surface**
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReaderIn!!.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSessionIn = session
                    try {
                        previewRequestBuilder1.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSessionIn?.setRepeatingRequest(previewRequestBuilder1.build(), null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        toGoFaultDesc("摄像头访问错误内")

                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(AppUtils.getContext(), "预览配置失败", Toast.LENGTH_SHORT).show()
                    toGoFaultDesc("预览配置失败内")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            toGoFaultDesc("预览配置失败内")
        }
    }

    private fun createCameraPreviewSessionOut(cameraDevice: CameraDevice?, previewSize: Size) {
        try {
            val texture: SurfaceTexture? = binding.textureOut.getSurfaceTexture()
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            val previewSurface = Surface(texture)

            val previewRequestBuilder1 =
                    cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder1?.addTarget(previewSurface)

            // **同时绑定 ImageReader Surface**
            cameraDevice?.createCaptureSession(Arrays.asList(previewSurface, imageReaderOut!!.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSessionOut = session
                    try {
                        previewRequestBuilder1?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSessionOut?.setRepeatingRequest(previewRequestBuilder1!!.build(), null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                        toGoFaultDesc("预览配置失败外")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(AppUtils.getContext(), "预览配置失败", Toast.LENGTH_SHORT).show()
                    toGoFaultDesc("预览配置失败外")

                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            toGoFaultDesc("预览配置失败外")

        }
    }

    /**
     * 打开内部
     */
    private val imageAvailableListenerIn = ImageReader.OnImageAvailableListener { reader ->
        cabinetVM.ioScope.launch {
            val dir =
                    File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "action")
            val activeType = cabinetVM.activeType
            if (!dir.exists()) dir.mkdirs()
            val fileName =
                    "yuan_in_${activeType}_${cabinetVM.curTransId}__${AppUtils.getDateYMDHMS()}.jpg"
            val destFile = File(dir, fileName)
            val image = reader.acquireNextImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]

            try {
                FileOutputStream(destFile).use { output ->
                    output.write(bytes)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                toGoFaultDesc("在图像上设置可用监听器失败内")
            } finally {
                image.close()
                compression(destFile.absolutePath, dir.absolutePath, activeType, 0)
            }
        }
    }

    /***
     * 打开外部
     */
    private val imageAvailableListenerOut = ImageReader.OnImageAvailableListener { reader ->
        val dir =
                File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "action")
        if (!dir.exists()) dir.mkdirs()
        val activeType = cabinetVM.activeType
        val fileName =
                "yuan_out_${activeType}_${cabinetVM.curTransId}__${AppUtils.getDateYMDHMS()}.jpg"
        val destFile = File(dir, fileName)
        val image = reader.acquireNextImage()
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer[bytes]

        try {
            FileOutputStream(destFile).use { output ->
                output.write(bytes)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            toGoFaultDesc("在图像上设置可用监听器失败外")
        } finally {
            image.close()
            compression(destFile.absolutePath, dir.absolutePath, activeType, 1)
        }

    }

    /**
     * @param inputFilePath  源文件
     * @param outputFilePath 输出文件
     * @param activeType  类型
     * @param inOut  内外 0.内 1外
     */
    fun compression(inputFilePath: String, outputFilePath: String, activeType: String, inOut: Int) {
        Luban.with(requireContext()).load(inputFilePath).ignoreBy(100).setTargetDir(outputFilePath).filter { path ->
            !(TextUtils.isEmpty(path) || path.lowercase(Locale.getDefault()).endsWith(".gif"))
        }.setCompressListener(object : OnCompressListener {
            override fun onStart() {
                println("测试我来了 onStart")

            }

            override fun onSuccess(file: File) {
                println("测试我来了 onSuccess ${file.absolutePath}")
                Log.e("TestFace", "网络导入用户信息 保存成功")
                val downloadsDir =
                        AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val aitionDir = File(downloadsDir, "action")
                val fileName = file.absolutePath.substringAfterLast('/')
                val inOutValue = when (inOut) {
                    0 -> {
                        "in"
                    }

                    1 -> {
                        "out"
                    }

                    else -> {
                        ""
                    }
                }

                val toFileName =
                        "save-${inOutValue}-${activeType}-${cabinetVM.curTransId}--${AppUtils.getDateYMDHMS()}.jpg"
                if (FileMdUtil.renameFileInDir(aitionDir, "$fileName", toFileName)) {
                    val fileValue = FileMdUtil.matchDownloadsName("action", toFileName)
                    when (activeType) {
                        CmdValue.CMD_OPEN_DOOR -> {
                            when (inOut) {
                                0 -> {
                                    cabinetVM.photoOpenIn = fileValue

                                }

                                1 -> {
                                    cabinetVM.photoOpenOut = fileValue
                                }
                            }

                        }

                        CmdValue.CMD_CLOSE_DOOR -> {
                            when (inOut) {
                                0 -> {
                                    cabinetVM.photoCloseIn = fileValue
                                }

                                1 -> {
                                    cabinetVM.photoCloseOut = fileValue
                                }
                            }
                        }
                    }
                    println("调试socket toGoInsertPhoto 网络上传拍照 $toFileName")
                    //上传到服务器
//                    cabinetVM.uploadPhoto(cabinetVM.curSn, cabinetVM.curTransId, 1, toFileName)
                    cabinetVM.taskPicAdd(fileValue)
                    cabinetVM.toGoInsertPhoto(activeType, inOut)
//                    LiveBus.get(BusType.BUS_DELIVERY_PHOTO).post(fileValue)
                }
            }

            override fun onError(e: Throwable) {
                println("测试我来了 onError")
                toGoFaultDesc("拍摄压缩图片异常")
            }
        }).launch()
    }

    fun stopPreview(cameraNum: Int) {

        if (cameraNum == 1 && cameraDeviceIn != null) {
            cameraDeviceIn?.close()
            cameraDeviceIn = null
            isPreviewActiveIn = false
//            updatePreviewUI(1, false)
        } else if (cameraNum == 2 && cameraDeviceOut != null) {
            cameraDeviceOut?.close()
            cameraDeviceOut = null
            isPreviewActiveOut = false
//            updatePreviewUI(2, false)
        }
    }

    private fun initCamera2() {
        // 设置纹理视图监听器
        binding.textureIn.surfaceTextureListener = textureListenerIn
        binding.textureOut.surfaceTextureListener = textureListenerOut
        // 获取相机管理器
        cameraManager = mActivity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }

    fun capturePhotoIn() {
        if (cameraDeviceIn == null || captureSessionIn == null) return

        try {
            val captureBuilder =
                    cameraDeviceIn!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReaderIn!!.surface)

            // 自动对焦、自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            // 设置 JPEG 方向（根据设备旋转）
            val rotation: Int = mActivity?.windowManager?.getDefaultDisplay()?.rotation ?: 90
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS[rotation])

            captureSessionIn!!.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Toast.makeText(AppUtils.getContext(), "拍照完成", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startRecordingIn() {
        if (cameraDeviceIn == null) {
            Toast.makeText(AppUtils.getContext(), "相机未打开", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. 配置 MediaRecorder
            setUpMediaRecorderIn()

            val texture: SurfaceTexture? = binding.textureIn.surfaceTexture
            texture?.setDefaultBufferSize(previewSizeIn!!.width, previewSizeIn!!.height)
            val previewSurface = Surface(texture)
            val recorderSurface: Surface? = mediaRecorderIn?.surface

            // 2. 重新创建包含 preview + recorder 的 Session
            cameraDeviceIn!!.createCaptureSession(Arrays.asList<Surface>(previewSurface, recorderSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSessionIn = session

                    try {
                        // 3. 创建录制的 CaptureRequest
                        val recordBuilder =
                                cameraDeviceIn!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        recordBuilder.addTarget(previewSurface)
                        if (recorderSurface != null) {
                            recordBuilder.addTarget(recorderSurface)
                        }

                        // 自动对焦、自动曝光
                        recordBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        recordBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                        // 4. 开始预览 + 录制
                        session.setRepeatingRequest(recordBuilder.build(), null, backgroundHandler)
                        mediaRecorderIn?.start()
                        isRecordingIn = true
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(AppUtils.getContext(), "录制配置失败", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class) private fun setUpMediaRecorderIn() {
        mediaRecorderIn = MediaRecorder()
        mediaRecorderIn?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorderIn?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorderIn?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val file = createVideoFile()
        mediaRecorderIn?.setOutputFile(file.absolutePath)

        mediaRecorderIn?.setVideoEncodingBitRate(10000000)
        mediaRecorderIn?.setVideoFrameRate(30)
        mediaRecorderIn?.setVideoSize(previewSizeIn!!.width, previewSizeIn!!.height)
        mediaRecorderIn?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorderIn?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        mediaRecorderIn?.prepare()
    }

    fun stopRecordingIn() {
        if (isRecordingIn) {
            try {
                mediaRecorderIn?.stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            mediaRecorderIn?.release()
            mediaRecorderIn = null
            isRecordingIn = false

            // 恢复预览
            startPreview(1)
        }
    }


    fun capturePhotoOut() {
        if (cameraDeviceOut == null || captureSessionOut == null) return

        try {
            val captureBuilder =
                    cameraDeviceOut!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReaderOut!!.surface)

            // 自动对焦、自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            // 设置 JPEG 方向（根据设备旋转）
            val rotation: Int = mActivity?.windowManager?.getDefaultDisplay()?.rotation ?: 90
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS[rotation])

            captureSessionOut!!.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Toast.makeText(AppUtils.getContext(), "拍照完成", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startRecordingOut() {
        if (cameraDeviceOut == null) {
            Toast.makeText(AppUtils.getContext(), "相机未打开", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1. 配置 MediaRecorder
            setUpMediaRecorderOut()

            val texture: SurfaceTexture? = binding.textureOut.surfaceTexture
            texture?.setDefaultBufferSize(previewSizeOut!!.width, previewSizeOut!!.height)
            val previewSurface = Surface(texture)
            val recorderSurface: Surface? = mediaRecorderOut?.surface

            // 2. 重新创建包含 preview + recorder 的 Session
            cameraDeviceOut!!.createCaptureSession(Arrays.asList(previewSurface, recorderSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSessionOut = session

                    try {
                        // 3. 创建录制的 CaptureRequest
                        val recordBuilder =
                                cameraDeviceOut!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        recordBuilder.addTarget(previewSurface)
                        if (recorderSurface != null) {
                            recordBuilder.addTarget(recorderSurface)
                        }

                        // 自动对焦、自动曝光
                        recordBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        recordBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                        // 4. 开始预览 + 录制
                        session.setRepeatingRequest(recordBuilder.build(), null, backgroundHandler)
                        mediaRecorderOut?.start()
                        isRecordingOut = true
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(AppUtils.getContext(), "录制配置失败", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class) private fun setUpMediaRecorderOut() {
        mediaRecorderOut = MediaRecorder()
        mediaRecorderOut?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorderOut?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorderOut?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val file = createVideoFile()
        mediaRecorderOut?.setOutputFile(file.absolutePath)

        mediaRecorderOut?.setVideoEncodingBitRate(10000000)
        mediaRecorderOut?.setVideoFrameRate(30)
        mediaRecorderOut?.setVideoSize(previewSizeIn!!.width, previewSizeIn!!.height)
        mediaRecorderOut?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorderOut?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        mediaRecorderOut?.prepare()
    }

    fun stopRecordingOut() {
        if (isRecordingOut) {
            try {
                mediaRecorderOut?.stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            mediaRecorderOut?.release()
            mediaRecorderOut = null
            isRecordingOut = false

            // 恢复预览
            startPreview(2)
        }
    }

    private fun createVideoFile(): File {
        return File(AppUtils.getContext().filesDir.toString() + "/audio", "video_" + System.currentTimeMillis() + ".mp4")
    }

    /****************************************权限管理回调***************************************************/

    private fun initPermissions() {
        Log.e("TestFace", "权限申请 initPermissions")
        permissionsManager =
                PermissionsRequester(CAMERA, WRITE_EXTERNAL_STORAGE, activity = mActivity!!, onShowRationale = ::dealShowRationale, onPermissionDenied = ::dealPermissionDenied, onNeverAskAgain = ::dealNeverAskAgain, requiresPermission = ::dealRequiresPermission)
        permissionsManager.launch()
    }

    private fun goPermissions() {
        Log.e("TestFace", "权限申请 initPermissions")
        permissionsManager =
                PermissionsRequester(CAMERA, activity = mActivity!!, onShowRationale = ::dealShowRationale, onPermissionDenied = ::dealPermissionDenied, onNeverAskAgain = ::dealNeverAskAgain, requiresPermission = ::dealRequiresPermission2)
        permissionsManager.launch()
    }

    fun dealPermissionDenied() {
        Log.e("TestFace", "权限申请 dealPermissionDenied")
//        Toast.makeText(mActivity?.baseContext!!, "某些权限被拒绝", Toast.LENGTH_LONG).show()
    }

    fun dealRequiresPermission() {
        Log.e("TestFace", "权限申请 dealRequiresPermission")
//        Toast.makeText(mActivity?.baseContext!!, "授予的所有权限", Toast.LENGTH_LONG).show()
    }

    fun dealRequiresPermission2() {
        Log.e("TestFace", "权限申请 dealRequiresPermission2")
    }

    fun dealShowRationale(request: PermissionRequest) {
//        try {
//            Log.e("TestFace", "权限申请 dealShowRationale")
//            val builder = AlertDialog.Builder(mActivity?.baseContext!!)
//            val dialog = builder.create()
//            val dialogView =
//                    View.inflate(mActivity?.baseContext!!, R.layout.dialog_permission, null)
//            dialog.setView(dialogView)
//            dialog.setCanceledOnTouchOutside(false)
//            val btnCancel = dialogView.findViewById<AppCompatTextView>(R.id.cancel)
//            val btnOK = dialogView.findViewById<AppCompatTextView>(R.id.ok)
//            btnCancel.setOnClickListener { v: View? ->
//                dialog.dismiss()
//                request.cancel()
//            }
//            btnOK.setOnClickListener { v: View? ->
//                dialog.dismiss()
//                request.proceed()
//            }
//            dialog.setCanceledOnTouchOutside(false)
//            dialog.show()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

    }

    fun dealNeverAskAgain() {
        Log.e("TestFace", "权限申请 dealNeverAskAgain")
//        Toast.makeText(mActivity?.baseContext!!, "有些许可被拒绝了，再也没有问过。", Toast.LENGTH_LONG)
//            .show()
//        val intent: Intent =
//                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
    }

    /****************************************权限管理回调***************************************************/
}
