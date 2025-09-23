package com.recycling.toolsapp;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.serial.port.utils.AppUtils;
import com.serial.port.utils.FileMdUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView1, textureView2;
    private Spinner cameraSpinner1, cameraSpinner2;
    private Spinner resolutionSpinner1, resolutionSpinner2;
    private Button startPreviewButton1, stopPreviewButton1, recording, stop_recording1, paizhao1;
    private Button startPreviewButton2, stopPreviewButton2;
    private TextView previewPlaceholder1, previewPlaceholder2;

    private CameraManager cameraManager;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private Map<String, CameraInfo> cameraInfoMap = new HashMap<>();
    private List<String> cameraIds = new ArrayList<>();

    private CameraDevice cameraDevice1;
    private CameraDevice cameraDevice2;

    private CameraCaptureSession captureSession1;
    private CameraCaptureSession captureSession2;

    private Size previewSize1;
    private Size previewSize2;

    private List<Size> supportedResolutions1 = new ArrayList<>();
    private List<Size> supportedResolutions2 = new ArrayList<>();

    private String selectedCameraId1;
    private String selectedCameraId2;

    private boolean isPreviewActive1 = false;
    private boolean isPreviewActive2 = false;

    // 方向转换
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // 相机信息类
    private static class CameraInfo {
        String cameraId;
        int lensFacing;
        String displayName;

        CameraInfo(String cameraId, int lensFacing) {
            this.cameraId = cameraId;
            this.lensFacing = lensFacing;

            switch (lensFacing) {
                case CameraCharacteristics.LENS_FACING_FRONT:
                    this.displayName = "前置摄像头:" + cameraId;
                    break;
                case CameraCharacteristics.LENS_FACING_BACK:
                    this.displayName = "后置摄像头:" + cameraId;
                    break;
                case CameraCharacteristics.LENS_FACING_EXTERNAL:
                    this.displayName = "外部摄像头:" + cameraId;
                    break;
                default:
                    this.displayName = "未知摄像头:" + cameraId;
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // 初始化视图
        initViews();

        // 获取相机管理器
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // 检查权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startBackgroundThread();
            setupCameras();
        }
    }

    private void initViews() {
        // 查找视图
        textureView1 = findViewById(R.id.textureView1);
        textureView2 = findViewById(R.id.textureView2);
        cameraSpinner1 = findViewById(R.id.cameraSpinner1);
        cameraSpinner2 = findViewById(R.id.cameraSpinner2);
        resolutionSpinner1 = findViewById(R.id.resolutionSpinner1);
        resolutionSpinner2 = findViewById(R.id.resolutionSpinner2);
        startPreviewButton1 = findViewById(R.id.startPreviewButton1);
        stopPreviewButton1 = findViewById(R.id.stopPreviewButton1);
        recording = findViewById(R.id.recording1);
        stop_recording1 = findViewById(R.id.stop_recording1);
        paizhao1 = findViewById(R.id.paizhao1);
        startPreviewButton2 = findViewById(R.id.startPreviewButton2);
        stopPreviewButton2 = findViewById(R.id.stopPreviewButton2);
        previewPlaceholder1 = findViewById(R.id.previewPlaceholder1);
        previewPlaceholder2 = findViewById(R.id.previewPlaceholder2);

        // 设置纹理视图监听器
        textureView1.setSurfaceTextureListener(textureListener1);
        textureView2.setSurfaceTextureListener(textureListener2);

        // 设置按钮点击事件
        startPreviewButton1.setOnClickListener(v -> startPreview(1));
        stopPreviewButton1.setOnClickListener(v -> stopPreview(1));
        recording.setOnClickListener(v -> startRecording());
        stop_recording1.setOnClickListener(v -> stopRecording());
        paizhao1.setOnClickListener(v -> capturePhoto());
        startPreviewButton2.setOnClickListener(v -> startPreview(2));
        stopPreviewButton2.setOnClickListener(v -> stopPreview(2));

        // 初始状态下禁用停止预览按钮
        stopPreviewButton1.setEnabled(false);
        stopPreviewButton2.setEnabled(false);
    }

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    public void capturePhoto() {

        if (cameraDevice1 == null || captureSession1 == null) return;

        try {
            final CaptureRequest.Builder captureBuilder = cameraDevice1.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader1.getSurface());

            // 自动对焦、自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // 设置 JPEG 方向（根据设备旋转）
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            captureSession1.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "拍照完成", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void startRecording() {
        if (cameraDevice1 == null) {
            Toast.makeText(this, "相机未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1. 配置 MediaRecorder
            setUpMediaRecorder();

            SurfaceTexture texture = textureView1.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize1.getWidth(), previewSize1.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface recorderSurface = mediaRecorder.getSurface();

            // 2. 重新创建包含 preview + recorder 的 Session
            cameraDevice1.createCaptureSession(Arrays.asList(previewSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession1 = session;

                    try {
                        // 3. 创建录制的 CaptureRequest
                        CaptureRequest.Builder recordBuilder = cameraDevice1.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        recordBuilder.addTarget(previewSurface);
                        recordBuilder.addTarget(recorderSurface);

                        // 自动对焦、自动曝光
                        recordBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                        recordBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                        // 4. 开始预览 + 录制
                        session.setRepeatingRequest(recordBuilder.build(), null, backgroundHandler);
                        mediaRecorder.start();
                        isRecording = true;

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "录制配置失败", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setUpMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File file = createVideoFile();
        mediaRecorder.setOutputFile(file.getAbsolutePath());

        mediaRecorder.setVideoEncodingBitRate(10_000_000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(previewSize1.getWidth(), previewSize1.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mediaRecorder.prepare();
    }

    public void stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            // 恢复预览
            startPreview(1);
        }
    }

    private File createVideoFile() {
        return new File(AppUtils.getContext().getFilesDir() + "/audio", "video_" + System.currentTimeMillis() + ".mp4");
    }

    private void setupCameras() {
        try {
            // 获取所有摄像头ID
            String[] cameraIdArray = cameraManager.getCameraIdList();
            cameraIds.clear();
            cameraInfoMap.clear();

            for (String cameraId : cameraIdArray) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (lensFacing != null) {
                    CameraInfo cameraInfo = new CameraInfo(cameraId, lensFacing);
                    cameraInfoMap.put(cameraId, cameraInfo);
                    cameraIds.add(cameraId);
                }
            }

            if (cameraIds.isEmpty()) {
                Toast.makeText(this, "未找到可用摄像头", Toast.LENGTH_SHORT).show();
                return;
            }

            // 设置摄像头选择Spinner
            setupCameraSpinners();

            // 默认选择第一个摄像头
            if (cameraIds.size() > 0) {
                selectedCameraId1 = cameraIds.get(0);
                setupResolutionSpinner(selectedCameraId1, 1);
            }

            if (cameraIds.size() > 1) {
                selectedCameraId2 = cameraIds.get(1);
                setupResolutionSpinner(selectedCameraId2, 2);
            } else {
                selectedCameraId2 = cameraIds.get(0);
                setupResolutionSpinner(selectedCameraId2, 2);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "摄像头访问错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCameraSpinners() {
        // 创建摄像头显示名称列表
        List<String> cameraDisplayNames = new ArrayList<>();
        for (String cameraId : cameraIds) {
            CameraInfo info = cameraInfoMap.get(cameraId);
            cameraDisplayNames.add(info.toString());
        }

        // 设置Spinner适配器
        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cameraDisplayNames);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cameraSpinner1.setAdapter(cameraAdapter);
        cameraSpinner2.setAdapter(cameraAdapter);

        // 设置摄像头选择监听
        cameraSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCameraId1 = cameraIds.get(position);
                setupResolutionSpinner(selectedCameraId1, 1);

                // 如果正在预览，重新启动预览
                if (isPreviewActive1) {
                    stopPreview(1);
                    startPreview(1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        cameraSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCameraId2 = cameraIds.get(position);
                setupResolutionSpinner(selectedCameraId2, 2);

                // 如果正在预览，重新启动预览
                if (isPreviewActive2) {
                    stopPreview(2);
                    startPreview(2);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 默认选择第一个和第二个摄像头（如果有）
        if (cameraIds.size() > 0) {
            cameraSpinner1.setSelection(0);
        }

        if (cameraIds.size() > 1) {
            cameraSpinner2.setSelection(1);
        } else if (cameraIds.size() > 0) {
            cameraSpinner2.setSelection(0);
        }
    }

    private void setupResolutionSpinner(String cameraId, int cameraNum) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                return;
            }

            // 获取支持的预览尺寸
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            List<Size> supportedSizes = Arrays.asList(sizes);

            // 按面积排序
            Collections.sort(supportedSizes, new CompareSizesByArea());

            // 保存支持的尺寸
            if (cameraNum == 1) {
                supportedResolutions1 = supportedSizes;
            } else {
                supportedResolutions2 = supportedSizes;
            }

            // 创建分辨率选项
            List<String> resolutionOptions = new ArrayList<>();
            for (Size size : supportedSizes) {
                resolutionOptions.add(size.getWidth() + "x" + size.getHeight());
            }

            // 设置Spinner适配器
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutionOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            Spinner spinner = cameraNum == 1 ? resolutionSpinner1 : resolutionSpinner2;
            spinner.setAdapter(adapter);

            // 设置默认选择（最高分辨率）
            if (!supportedSizes.isEmpty()) {
                spinner.setSelection(0);

                // 保存默认预览尺寸
                if (cameraNum == 1) {
                    previewSize1 = supportedSizes.get(0);
                } else {
                    previewSize2 = supportedSizes.get(0);
                }
            }

            // 设置分辨率选择监听
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (cameraNum == 1) {
                        previewSize1 = supportedResolutions1.get(position);
                        // 如果正在预览，重新启动预览
                        if (isPreviewActive1) {
                            stopPreview(1);
                            startPreview(1);
                        }
                    } else {
                        previewSize2 = supportedResolutions2.get(position);
                        // 如果正在预览，重新启动预览
                        if (isPreviewActive2) {
                            stopPreview(2);
                            startPreview(2);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener textureListener1 = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // 表面可用，可以开始预览
            if (isPreviewActive1) {
                startPreview(1);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // 调整预览尺寸
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private TextureView.SurfaceTextureListener textureListener2 = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // 表面可用，可以开始预览
            if (isPreviewActive2) {
                startPreview(2);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // 调整预览尺寸
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private void startPreview(int cameraNum) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show();
            return;
        }

        String cameraId = cameraNum == 1 ? selectedCameraId1 : selectedCameraId2;
        Size previewSize = cameraNum == 1 ? previewSize1 : previewSize2;

        if (cameraId == null || previewSize == null) {
            Toast.makeText(this, "请先选择摄像头和分辨率", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 关闭当前相机（如果已打开）
            if (cameraNum == 1 && cameraDevice1 != null) {
                cameraDevice1.close();
                cameraDevice1 = null;
            } else if (cameraNum == 2 && cameraDevice2 != null) {
                cameraDevice2.close();
                cameraDevice2 = null;
            }

            // 打开相机
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    if (cameraNum == 1) {
                        cameraDevice1 = camera;
                        // 初始化 ImageReader（JPEG 格式）
                        imageReader1 = ImageReader.newInstance(previewSize1.getWidth(), previewSize1.getHeight(), ImageFormat.JPEG, 2);
                        imageReader1.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

                        isPreviewActive1 = true;
                        updatePreviewUI(1, true);
//                        createCameraPreviewSession(cameraDevice1, previewSize,
//                                cameraNum == 1 ? textureView1 : textureView2, cameraNum);
                        createCameraPreviewSession1(cameraDevice1, previewSize, cameraNum == 1 ? textureView1 : textureView2, cameraNum);
                    } else {
                        cameraDevice2 = camera;
                        isPreviewActive2 = true;
                        updatePreviewUI(2, true);
                        createCameraPreviewSession(cameraDevice2, previewSize, cameraNum == 1 ? textureView1 : textureView2, cameraNum);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    if (cameraNum == 1) {
                        cameraDevice1 = null;
                        isPreviewActive1 = false;
                        updatePreviewUI(1, false);
                    } else {
                        cameraDevice2 = null;
                        isPreviewActive2 = false;
                        updatePreviewUI(2, false);
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    if (cameraNum == 1) {
                        cameraDevice1 = null;
                        isPreviewActive1 = false;
                        updatePreviewUI(1, false);
                    } else {
                        cameraDevice2 = null;
                        isPreviewActive2 = false;
                        updatePreviewUI(2, false);
                    }
                    Toast.makeText(MainActivity.this, "摄像头打开失败", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "摄像头访问错误", Toast.LENGTH_SHORT).show();
        }
    }

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            backgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
        }
    };

    private class ImageSaver implements Runnable {
        private final Image image;

        ImageSaver(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            File file = createImageFile();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
            }
        }
    }

    private File createImageFile() {
        File dir = new File(AppUtils.getContext().getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        return new File(dir, fileName);
    }

    private void stopPreview(int cameraNum) {
        if (cameraNum == 1 && cameraDevice1 != null) {
            cameraDevice1.close();
            cameraDevice1 = null;
            isPreviewActive1 = false;
            updatePreviewUI(1, false);
        } else if (cameraNum == 2 && cameraDevice2 != null) {
            cameraDevice2.close();
            cameraDevice2 = null;
            isPreviewActive2 = false;
            updatePreviewUI(2, false);
        }
    }

    private void updatePreviewUI(int cameraNum, boolean isActive) {
        runOnUiThread(() -> {
            if (cameraNum == 1) {
                startPreviewButton1.setEnabled(!isActive);
                stopPreviewButton1.setEnabled(isActive);
                previewPlaceholder1.setVisibility(isActive ? View.GONE : View.VISIBLE);
            } else {
                startPreviewButton2.setEnabled(!isActive);
                stopPreviewButton2.setEnabled(isActive);
                previewPlaceholder2.setVisibility(isActive ? View.GONE : View.VISIBLE);
            }
        });
    }

    private ImageReader imageReader1;

    private void createCameraPreviewSession1(CameraDevice cameraDevice, Size previewSize, TextureView textureView, int cameraNum) {
        try {
            SurfaceTexture texture = textureView1.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize1.getWidth(), previewSize1.getHeight());

            Surface previewSurface = new Surface(texture);

            final CaptureRequest.Builder previewRequestBuilder1 = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder1.addTarget(previewSurface);

            // **同时绑定 ImageReader Surface**
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader1.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession1 = session;
                    try {
                        previewRequestBuilder1.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        captureSession1.setRepeatingRequest(previewRequestBuilder1.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "预览配置失败", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession(CameraDevice cameraDevice, Size previewSize, TextureView textureView, int cameraNum) {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }

                    try {
                        // 自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        // 设置预览请求
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        session.setRepeatingRequest(previewRequest, null, backgroundHandler);
                        if (cameraNum == 1) {
                            captureSession1 = session;
                        } else if (cameraNum == 2) {
                            captureSession2 = session;
                        }

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "预览配置失败", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "需要相机权限才能使用此应用", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                startBackgroundThread();
                setupCameras();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        stopPreview(1);
        stopPreview(2);
        stopBackgroundThread();
        super.onPause();
    }

    // 比较尺寸大小的辅助类
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 按面积倒序排列
            return Long.signum((long) rhs.getWidth() * rhs.getHeight() - (long) lhs.getWidth() * lhs.getHeight());
        }
    }
}