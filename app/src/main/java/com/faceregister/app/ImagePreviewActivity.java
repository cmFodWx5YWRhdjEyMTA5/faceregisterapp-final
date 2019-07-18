package com.faceregister.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImagePreviewActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private int IS_FROM_SMILE_SCAN = 0;
    private int IS_FROM_DOC_SCAN = 1;
    private int isFrom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        ImageView imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        Button takeAnother = findViewById(R.id.takeOther);
        Button upload = findViewById(R.id.upload);
        Bitmap bitmap = null;
        progressBar.setVisibility(View.GONE);
        Intent intent = getIntent();
        if (intent.getExtras().get("MOVED_FROM").equals("SmileScan")) {
            isFrom = IS_FROM_SMILE_SCAN;
        } else {
            isFrom = IS_FROM_DOC_SCAN;
        }
        try {
            bitmap = BitmapFactory.decodeStream(this.openFileInput("myImage"));
            imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        takeAnother.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ImagePreviewActivity.this, (isFrom == IS_FROM_SMILE_SCAN) ? MainActivity.class : DocScanActivity.class);
                startActivity(intent);
            }
        });
        final Bitmap finalBitmap = bitmap;
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                uploadImage(finalBitmap);
                saveToLocal(finalBitmap);
            }
        });
    }

    private void uploadImage(Bitmap finalBitmap) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference storageRef = storage.getReference();

        long currentMilis = System.currentTimeMillis();
        StorageReference mountainsRef = storageRef.child(currentMilis + ".jpg");
        StorageReference mountainImagesRef = storageRef.child("images/" + currentMilis + ".jpg");

        // While the file names are the same, the references point to different files
        mountainsRef.getName().equals(mountainImagesRef.getName());    // true
        mountainsRef.getPath().equals(mountainImagesRef.getPath());    // false
        Bitmap bitmap = finalBitmap;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mountainsRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ImagePreviewActivity.this, "Error occurred", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ImagePreviewActivity.this, "Photo uploaded successfully", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ImagePreviewActivity.this, DocScanActivity.class);
                startActivity(intent);
            }
        });

    }

    private void saveToLocal(Bitmap picture) {
        FileOutputStream out = null;

        try {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), "FACEREGISTERAPP");

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
}
