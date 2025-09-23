//package com.recycling.toolsapp
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.widget.Button
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.video.FallbackStrategy
//import androidx.camera.video.Quality
//import androidx.camera.video.Recorder
//import androidx.camera.video.Recording
//import androidx.camera.video.VideoCapture
//import androidx.camera.video.VideoCapture.withOutput
//import androidx.camera.video.VideoRecordEvent
//import androidx.camera.view.PreviewView
//import androidx.core.content.ContextCompat
//import androidx.work.await
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.Locale
//import android.Manifest.permission.CAMERA
//import android.Manifest.permission.RECORD_AUDIO
//import androidx.camera.core.UseCaseGroup
//import androidx.camera.video.QualitySelector
//import java.util.Date
//
//
//class CaneraxxActivity : AppCompatActivity() {
//    private lateinit var previewView1: PreviewView
//    private lateinit var previewView2: PreviewView
//    private lateinit var btnPhoto: Button
//    private lateinit var btnVideo: Button
//
//    private var videoCapture1: VideoCapture<Recorder>? = null
//    private var videoCapture2: VideoCapture<Recorder>? = null
//    private var recording1: Recording? = null
//    private var recording2: Recording? = null
//
//    private val scope = CoroutineScope(Dispatchers.Main)
//
//    private val permissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { granted ->
//        if (granted.values.all { it }) {
//            startConcurrentCamera()
//        }
//    }
//
//    @SuppressLint("MissingInflatedId") override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        previewView1 = findViewById(R.id.previewView1)
//        previewView2 = findViewById(R.id.previewView2)
//        btnPhoto = findViewById(R.id.btnPhoto)
//        btnVideo = findViewById(R.id.btnVideo)
//
//        permissionLauncher.launch(
//            arrayOf(
//               CAMERA,
//               RECORD_AUDIO
//            )
//        )
//    }
//
//    @SuppressLint("MissingPermission", "RestrictedApi") private fun startConcurrentCamera() {
//        scope.launch {
//            val cameraProvider = ProcessCameraProvider.getInstance(this@CaneraxxActivity).await()
//
//            // 获取支持的并发组合
//            val combos = cameraProvider.availableConcurrentCameraInfos
//            if (combos.isEmpty()) {
//                // 不支持并发
//                return@launch
//            }
//
//            val combo = combos.first()
//            val info1 = combo.first
//            val info2 = combo.second
//
//            val preview1 = Preview.Builder().build().apply {
//                setSurfaceProvider(previewView1.surfaceProvider)
//            }
//            val preview2 = Preview.Builder().build().apply {
//                setSurfaceProvider(previewView2.surfaceProvider)
//            }
//
//            // 拍照用例
//            val imageCapture1 = androidx.camera.core.ImageCapture.Builder().build()
//            val imageCapture2 = androidx.camera.core.ImageCapture.Builder().build()
//
//            // 视频用例
//            videoCapture1 = withOutput(
//                Recorder.Builder()
//                    .setQualitySelector(Quality.HD.let { QualitySelector.from(it, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)) })
//                    .build()
//            )
//            videoCapture2 = withOutput(
//                Recorder.Builder()
//                    .setQualitySelector(Quality.HD.let { QualitySelector.from(it, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)) })
//                    .build()
//            )
//
//            val group1 = UseCaseGroup.Builder()
//                .addUseCase(preview1)
//                .addUseCase(imageCapture1)
//                .addUseCase(videoCapture1!!)
//                .build()
//
//            val group2 = UseCaseGroup.Builder()
//                .addUseCase(preview2)
//                .addUseCase(imageCapture2)
//                .addUseCase(videoCapture2!!)
//                .build()
//            cameraProvider.bindToLifecycleInConcurrentMode(
//                this@CaneraxxActivity,
//                combo,
//                listOf(group1, group2)
//            )
//
//            // 拍照
//            btnPhoto.setOnClickListener {
//                val file1 = createFile("IMG1_", ".jpg")
//                val file2 = createFile("IMG2_", ".jpg")
//                imageCapture1.takePicture(
//                    androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file1).build(),
//                    ContextCompat.getMainExecutor(this@CaneraxxActivity),
//                    object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
//                        override fun onError(exc: androidx.camera.core.ImageCaptureException) {}
//                        override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {}
//                    }
//                )
//                imageCapture2.takePicture(
//                    androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file2).build(),
//                    ContextCompat.getMainExecutor(this@CaneraxxActivity),
//                    object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
//                        override fun onError(exc: androidx.camera.core.ImageCaptureException) {}
//                        override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {}
//                    }
//                )
//            }
//
//            // 录制
//            btnVideo.setOnClickListener {
//                if (recording1 == null && recording2 == null) {
//                    val file1 = createFile("VID1_", ".mp4")
//                    val file2 = createFile("VID2_", ".mp4")
//                    recording1 = videoCapture1?.output
//                        ?.prepareRecording(this@CaneraxxActivity, androidx.camera.video.FileOutputOptions.Builder(file1).build())
//                        ?.apply { withAudioEnabled() }
//                        ?.start(ContextCompat.getMainExecutor(this@CaneraxxActivity)) { event ->
//                            if (event is VideoRecordEvent.Finalize) {
//                                recording1 = null
//                            }
//                        }
//                    recording2 = videoCapture2?.output
//                        ?.prepareRecording(this@CaneraxxActivity, androidx.camera.video.FileOutputOptions.Builder(file2).build())
//                        ?.apply { withAudioEnabled() }
//                        ?.start(ContextCompat.getMainExecutor(this@CaneraxxActivity)) { event ->
//                            if (event is VideoRecordEvent.Finalize) {
//                                recording2 = null
//                            }
//                        }
//                    btnVideo.text = "停止录制"
//                } else {
//                    recording1?.stop()
//                    recording2?.stop()
//                    recording1 = null
//                    recording2 = null
//                    btnVideo.text = "开始录制"
//                }
//            }
//        }
//    }
//
//    private fun createFile(prefix: String, suffix: String): File {
//        val dir = getExternalFilesDir(null) ?: filesDir
//        if (!dir.exists()) dir.mkdirs()
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//        return File(dir, "$prefix$timeStamp$suffix")
//    }
//}