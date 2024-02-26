package com.example.cameratwoapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Camera2VideoHelper {
    private static final String TAG = "Camera2VideoImage";
    private final Context context;
    private final TextureView textureView;
    private String cameraId;
    private CameraDevice cameraDevice;
    private Size videoSize;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private MediaRecorder mediaRecorder;

    public Camera2VideoHelper(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join(); //Waits for this thread to die.
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //StateCallback is then used to receive updates about the state of the CameraCaptureSession.
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            Camera2VideoHelper.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            Camera2VideoHelper.this.cameraDevice = null;
        }
    };

//    configure and set up a CameraCaptureSession that will handle the preview of the camera feed.
//    A preview session is essential for displaying live camera output to the user before capturing an
//    image or recording a video.
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();

//            statement is a way to test assumptions during development, and it is typically used
//            for debugging purposes.
            assert texture != null;
            texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(
                    java.util.Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }
                            Camera2VideoHelper.this.cameraCaptureSession = cameraCaptureSession;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed: Failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                //responsible for obtaining the characteristics of a specific camera device
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                this.cameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

//    public void startRecordingVideo() {
//        if (null == cameraDevice || !textureView.isAvailable() || null == videoSize) {
//            return;
//        }
//        try {
//            closePreviewSession();
////            setUpMediaRecorder();
//            SurfaceTexture texture = textureView.getSurfaceTexture();
//            assert texture != null;
//            texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//            java.util.List<Surface> surfaces = new java.util.ArrayList<>();
//
//            Surface previewSurface = new Surface(texture);
//            surfaces.add(previewSurface);
//            captureRequestBuilder.addTarget(previewSurface);
//
//            Surface recorderSurface = mediaRecorder.getSurface();
//            surfaces.add(recorderSurface);
//            captureRequestBuilder.addTarget(recorderSurface);
//
//            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    Camera2VideoHelper.this.cameraCaptureSession = cameraCaptureSession;
//                    updatePreview();
//                    mediaRecorder.start();
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    Log.e(TAG, "Failed to start recording");
//                }
//            }, backgroundHandler);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public void stopRecordingVideo() {
//        mediaRecorder.stop();
//        mediaRecorder.reset();
//    }

    private void closePreviewSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

//    public void setUpMediaRecorder() throws IOException {
//        final Activity activity = (Activity) context;
//        if (null == activity) {
//            return;
//        }
//        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setOutputFile(getOutputMediaFile().getAbsolutePath());
//        mediaRecorder.setVideoEncodingBitRate(10000000);
//        mediaRecorder.setVideoFrameRate(30);
//        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mediaRecorder.setOrientationHint(90);
//        mediaRecorder.prepare();
//    }
//
//    private File getOutputMediaFile() {
//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//                Log.e(TAG, "Failed to create directory");
//                return null;
//            }
//        }
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
//        return mediaFile;
//    }
//
    public TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
}
