package com.faceregister.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {
    public static final String MEDIA_DIRECTORY_NAME = "NowFloats";
    private static final String TAG = "FaceDetectionProcessor";
    private boolean oneTime = true;
    private final FirebaseVisionFaceDetector detector;
    private final ISmileListener smileListener;

    private final Bitmap overlayBitmap;

    public FaceDetectionProcessor(Resources resources, ISmileListener smileListener) {
        this.smileListener = smileListener;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build();

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);

        overlayBitmap = BitmapFactory.decodeResource(resources, R.drawable.common_full_open_on_phone);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable final Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull final FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }
        for (int i = 0; i < faces.size(); ++i) {
            final FirebaseVisionFace face = faces.get(i);

            int cameraFacing =
                    frameMetadata != null ? frameMetadata.getCameraFacing() :
                            Camera.CameraInfo.CAMERA_FACING_BACK;
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, cameraFacing, overlayBitmap);

            graphicOverlay.add(faceGraphic);
            graphicOverlay.add(new GraphicOverlay.Graphic(graphicOverlay) {
                @Override
                public void draw(Canvas canvas) {
                    FirebaseVisionFace thisFace = face;
                    int height = canvas.getHeight();
                    int width = canvas.getWidth();
                    Point centerOfCanvas = new Point(width / 2, height / 2);
                    int rectW = 700;
                    int rectH = 900;
                    int left = centerOfCanvas.x - (rectW / 2);
                    int top = centerOfCanvas.y - (rectH / 2);
                    int right = centerOfCanvas.x + (rectW / 2);
                    int bottom = centerOfCanvas.y + (rectH / 2);
                    RectF rect = new RectF(left, top, right, bottom);
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
//                    paint.setStrokeWidth(20);
                    paint.setTextSize(40);
                    float x = translateX(face.getBoundingBox().centerX());
                    float y = translateY(face.getBoundingBox().centerY());
                    float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
                    float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
                    float boundleft = x - xOffset;
                    float boundlefttop = y - yOffset;
                    float boundleftright = x + xOffset;
                    float boundleftbottom = y + yOffset;
                    float allowCrossLimit = 100;
                    if (Math.round(rect.left - boundleft) < allowCrossLimit && Math.round(rect.right - boundleftright) < allowCrossLimit
                            && Math.abs(rect.top - boundlefttop) < allowCrossLimit * 2 && Math.abs(rect.bottom - boundleftbottom) < allowCrossLimit * 2) {
                        paint.setColor(Color.GREEN);
                        if (oneTime && face.getSmilingProbability() > .45) {
                            saveImage(originalCameraImage);
                            smileListener.getSmiledPicture(originalCameraImage);
                            oneTime = false;
                        }
                        paint.setTextSize(60);
                        canvas.drawText("Smile ☺", centerOfCanvas.x, bottom * 1.0f, paint);
                    }else{
                        canvas.drawText("Keep face centered", centerOfCanvas.x, bottom * 1.0f, paint);
                    }
//                    canvas.drawOval(rect, paint);

                }
            });

        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    private void saveImage(Bitmap picture) {
        Log.d(TAG, "Taken picture is here!");


        FileOutputStream out = null;

        try {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), MEDIA_DIRECTORY_NAME);

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return;
                }
            }

            //create a new file, specifying the path, and the filename
            //which we want to save the file as.
            File file = new File(mediaStorageDir, "NF_" + System.currentTimeMillis() + "_PIC.jpg");

            out = new FileOutputStream(file);
            picture.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface ISmileListener {
        public void getSmiledPicture(Bitmap picture);
    }
}