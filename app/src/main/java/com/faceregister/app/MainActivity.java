package com.faceregister.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaActionSound;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String MEDIA_DIRECTORY_NAME = "NowFloats";

    private Context context;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;

    private int CAPTURE_DELAY = 3;
    private Timer timeTicker;

    private ImageView ivAutoFocus;

    // CAMERA VERSION TWO DECLARATIONS
    private Camera2Source mCamera2Source = null;

    // COMMON TO BOTH CAMERAS
    private CameraSourcePreview mPreview;
    private FaceDetector previewFaceDetector = null;
    private GraphicOverlay mGraphicOverlay;
    private FaceGraphicOld mFaceGraphic;
    private boolean wasActivityResumed = false;
    private CameraSource cameraSource = null;
    private ImageView ibSwitch;
    private TextView tvTimer;

    // DEFAULT CAMERA BEING OPENED
    private boolean usingFrontCamera = true;

    private OverlayView mOverlayView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        findViewById();

        if (checkGooglePlayAvailability()) {
            requestPermissionThenOpenCamera();

            switchButtonCallback();

        }

        createCameraSource();
        startCameraSource();

    }


    private void findViewById() {
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);
        ivAutoFocus = findViewById(R.id.ivAutoFocus);

        mOverlayView = findViewById(R.id.overlay_view);
        tvTimer = findViewById(R.id.tvTimer);

        ibSwitch = findViewById(R.id.ibSwitch);
    }


    private void switchButtonCallback() {
        ibSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (usingFrontCamera) {
                    cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                    usingFrontCamera = false;
                } else {
                    cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
                    usingFrontCamera = true;
                }
                mPreview.stop();
                startCameraSource();
            }
        });
    }

    public String createImageFromBitmap(Bitmap bitmap) {
        String fileName = "myImage";//no .png or .jpg needed
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            // remember close file output
            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }

    private void saveImage(Bitmap picture) {
        stopCameraSource();
        Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);
        intent.putExtra("BitmapImage", createImageFromBitmap(picture));
        intent.putExtra("MOVED_FROM", "SmileScan");
        setResult(1, intent);
        startActivity(intent);
        Log.d(TAG, "Taken picture is here!");
        return;
//        runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                ibSwitch.setEnabled(true);
//            }
//        });
//
//        FileOutputStream out = null;
//
//        try {
//            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), MEDIA_DIRECTORY_NAME);
//
//            // Create the storage directory if it does not exist
//            if (!mediaStorageDir.exists()) {
//                if (!mediaStorageDir.mkdirs()) {
//                    return;
//                }
//            }
//
//            //create a new file, specifying the path, and the filename
//            //which we want to save the file as.
//            File file = new File(mediaStorageDir, "NF_" + System.currentTimeMillis() + "_PIC.jpg");
//
//            out = new FileOutputStream(file);
//            picture.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (out != null) {
//                    out.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }


    private boolean checkGooglePlayAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApiAvailability.isUserResolvableError(resultCode)) {
            googleApiAvailability.getErrorDialog(MainActivity.this, resultCode, 2404).show();
        }

        return false;
    }


    private void requestPermissionThenOpenCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }


    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, mGraphicOverlay);
        }

        try {
            cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor(getResources(), new FaceDetectionProcessor.ISmileListener() {
                @Override
                public void getSmiledPicture(Bitmap picture) {
                    smiled(picture);
                }
            }));

        } catch (Exception e) {
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }


    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (mPreview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (mGraphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                mPreview.start(cameraSource, mGraphicOverlay);

            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }


    private void stopCameraSource() {
        mPreview.stop();
    }


    private void takePicture(Bitmap picture) {
        //Play default capture sound
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
        saveImage(picture);
//        if (mCamera2Source != null)
//            mCamera2Source.takePicture(camera2SourceShutterCallback, camera2SourcePictureCallback);
    }

    private void capture(Bitmap picture) {
        Log.i(TAG, "Timer - " + CAPTURE_DELAY);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                tvTimer.setText(String.valueOf(CAPTURE_DELAY));
                CAPTURE_DELAY--;
            }
        });

        if (CAPTURE_DELAY == -1) {
            if (timeTicker != null) {
                timeTicker.cancel();
            }

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    CAPTURE_DELAY = 3;
                    tvTimer.setVisibility(View.GONE);
                }
            });

            takePicture(picture);
        }
    }


    public void smiled(final Bitmap picture) {
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.FOCUS_COMPLETE);

        tvTimer.setVisibility(View.VISIBLE);

        timeTicker = new Timer();

        timeTicker.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                capture(picture);
            }
        }, 0, 1000);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "Camera Permission Required", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "Storage Permission Required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (wasActivityResumed) {
            //If the CAMERA2 is paused then resumed, it won't start again unless creating the whole camera again.
//            if (usingFrontCamera) {
//                createCameraSourceFront();
//            } else {
//                createCameraSourceBack();
//            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        wasActivityResumed = true;
        stopCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopCameraSource();

        if (previewFaceDetector != null) {
            previewFaceDetector.release();
        }
    }


    Camera2Source.IOverlayView overlayViewCallback = new Camera2Source.IOverlayView() {

        @Override
        public void setRect(final Rect rect) {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mOverlayView.setRect(rect);
                    mOverlayView.requestLayout();
                }
            });
        }
    };
}