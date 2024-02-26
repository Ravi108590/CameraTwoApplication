package com.example.cameratwoapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    //    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 201;
    private static final String TAG = "Camera2VideoImage";
    private TextureView textureView;
    private Camera2VideoHelper camera2VideoHelper; // Object of the class
    private boolean isRecording = false;
    private String recordedVideoFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
//        Button recordButton = findViewById(R.id.recordButton);
//        recordButton.setOnClickListener(v -> {
//            if (isRecording) {
//                stopRecording();
//            } else {
//                startRecording();
//            }
//        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        } else {
            //startCamera();
        }
    }

    private void startCamera() {
        camera2VideoHelper = new Camera2VideoHelper(this, textureView);
        camera2VideoHelper.startBackgroundThread();
        if (textureView.isAvailable()) {
            camera2VideoHelper.openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(camera2VideoHelper.surfaceTextureListener);
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (camera2VideoHelper != null) {
//            //startCamera();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        if (camera2VideoHelper != null) {
//            camera2VideoHelper.closeCamera();
//            camera2VideoHelper.stopBackgroundThread();
//        }
//        super.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (camera2VideoHelper != null) {
//            camera2VideoHelper.closeCamera();
//            camera2VideoHelper.stopBackgroundThread();
//        }
//        super.onDestroy();
//    }
//
//    private void startRecording() {
//        if (camera2VideoHelper != null) {
//            camera2VideoHelper.startRecordingVideo();
//            isRecording = true;
//            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void stopRecording() {
//        if (camera2VideoHelper != null) {
//            camera2VideoHelper.stopRecordingVideo();
//            isRecording = false;
//            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(MainActivity.this, VideoPlaybackActivity.class);
//            intent.putExtra("VIDEO_FILE_PATH", recordedVideoFilePath);
//            startActivity(intent);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera, audio, and storage permissions are required", Toast.LENGTH_SHORT).show();
        }
    }
}
