package com.example.garbagedataclassificationcollection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraFeedActivity extends AppCompatActivity {
    public static final String CAMERA_FEED= "Camera Feed";
    public static final String CAMERA_ACCESS= "Camera Access";
    public static final int REQUEST_CAM_PERMISSION = 3;
    private TextureView camFeed;
    private TextureView.SurfaceTextureListener camFeedSurfaceTexture = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Toast.makeText(CameraFeedActivity.this, "I'm available, mi-lord", Toast.LENGTH_SHORT).show();
            setupCamera(i, i1);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice cam;
    private CameraDevice.StateCallback camStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            cam = cameraDevice;
            startPreview();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            cam = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cam=null;
        }
    };

    private String camId;
    private Size camPreviewSize;

    private Handler bgHandler;
    private HandlerThread bgThread;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size size, Size t1) {
            return Long.signum((long)size.getHeight()*size.getWidth() / (long)t1.getHeight()*t1.getWidth()); // shouldn't I add () around t1 width*height?
        }
    }

    private CaptureRequest.Builder capReqBuilder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_feed);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        camFeed = findViewById(R.id.cam_feed);

        findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_FULLSCREEN
                |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBgThread();
        if(camFeed.isAvailable()){
            setupCamera(camFeed.getWidth(), camFeed.getHeight());
            connectCamera();
        }else{
            camFeed.setSurfaceTextureListener(camFeedSurfaceTexture);
        }
    }

    // in case we leave our app, we better free cam resource
    @Override
    protected void onPause(){
        closeCamera();
        stopBgThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAM_PERMISSION){
            if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Can't collect data without camera permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // setting up camera (getting its id)
    private void setupCamera(int width, int height){
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            for(String camId: camManager.getCameraIdList()){
                CameraCharacteristics camChar = camManager.getCameraCharacteristics(camId);
                int lensFacingDir = camChar.get(CameraCharacteristics.LENS_FACING);
                if(lensFacingDir == CameraCharacteristics.LENS_FACING_BACK){
                    // adjusting orientation for calculating preview size
                    StreamConfigurationMap map = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    int deviceOrien = getWindowManager().getDefaultDisplay().getRotation();
                    int totalRotation = sensorToDeviceOrien(camChar, deviceOrien);
                    boolean isPortrait = totalRotation==90 || totalRotation==270;
                    int rotatedWidth=width;
                    int rotatedHeight = height;
                    if(isPortrait){
                        rotatedHeight=width;
                        rotatedWidth=height;
                    }
                    camPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),rotatedWidth, rotatedHeight);
                    this.camId = camId;
                    return;
                }
            }
        }catch (CameraAccessException e){
            Log.d(CAMERA_ACCESS, "Error Accessing CameraId", e);
        }
    }

    private void connectCamera(){
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
                Toast.makeText(this,"Camera permission required for the app", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAM_PERMISSION);
        }else{
            try{
                camManager.openCamera(camId,camStateCallback,bgHandler);
            }catch (CameraAccessException e){
                Log.d(CAMERA_ACCESS, "Error cam permission", e);
            }
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = camFeed.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(camPreviewSize.getWidth(), camPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture); // should be freed after use
        try{
           capReqBuilder = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
           capReqBuilder.addTarget(previewSurface);
           cam.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
               @Override
               public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                   try {
                       cameraCaptureSession.setRepeatingRequest(capReqBuilder.build(), null, bgHandler);
                   } catch (CameraAccessException e) {
                       Log.d(CAMERA_ACCESS, "Error in setting up cam preview"+e);
                       throw new RuntimeException(e);
                   }
               }

               @Override
               public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                   Toast.makeText(CameraFeedActivity.this, "Error in previewing camera", Toast.LENGTH_SHORT).show();
               }
           }, null);
        }catch(CameraAccessException e){
            Log.d(CAMERA_ACCESS, "error in creating capture req: "+e);
        }

    }
    private void closeCamera(){
        if(cam!=null){
            cam.close();
            cam=null;
        }
    }
    private void startBgThread(){
        if(bgThread==null || !bgThread.isAlive()){
            bgThread = new HandlerThread("CameraFeed");
            bgThread.start();
            bgHandler = new Handler(bgThread.getLooper());
        }
    }

    private void stopBgThread(){
        if(bgThread!=null){
            bgThread.quitSafely();
            try{
                bgThread.join();
                bgThread = null;
                bgHandler = null;
            }catch (InterruptedException e){
                Log.d(CAMERA_FEED,"Error stopping Bg Thread", e);
            }
        }
    }

    private static int sensorToDeviceOrien(@NonNull CameraCharacteristics camChar, int deviceOrien){
        int sensorOrien = camChar.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrien = ORIENTATIONS.get(deviceOrien);
        return (sensorOrien+deviceOrien+360)%360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<>();
        for(Size option:choices){
            if(option.getHeight()== option.getWidth()*height/width && option.getWidth()>=width&&
                option.getHeight()>=height){
                bigEnough.add(option);
            }
        }
        if(!bigEnough.isEmpty()){
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        else{
            return choices[8];
        }
    }
}