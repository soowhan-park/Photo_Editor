package com.example.photogallery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
Photo Gallery App (진행사항)
- 갤러리 또는 사진 캡쳐로 이미지 불러오고 저장하기
- UCrop 오픈 라이브러리를 사용하여 간단한 포토 에디터 적용 (https://github.com/Yalantis/uCrop)

To Do List
- Intent를 사용하여 폴더 익스플로러 실행 후 저장하고픈 폴더 선택 (Intent -> Save file)
- SQLite 안에 파일 정보 저장
- 기타 자잘한 버그들 수정 (SaveInstanceState, Null(Empty Photo)
 */
public class MainActivity extends AppCompatActivity {
    private ImageButton addPhoto;
    private ImageView displayPhoto;

    private Button editPhoto;
    private Button takePhoto;
    private Button savePhoto;

    private TextView fileLoc;

    // Set the request codes
    private static final int PICK_REQUEST = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private static final int IMG_GALLERY = 3;

    //name of the file that is saved by the camera
    private String currentPhotoPath;
    private String SAMPLE_CROPPED_IMG_NAME = "SampleCrop";
    private OutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addPhoto = findViewById(R.id.imgBtnAddButton);
        displayPhoto = findViewById(R.id.imgPhoto);
        editPhoto = findViewById(R.id.btnEdit);
        takePhoto = findViewById(R.id.btnTake);
        savePhoto = findViewById(R.id.btnSave);
        fileLoc = findViewById(R.id.tvLocation);

        displayPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent().setAction(Intent.ACTION_GET_CONTENT).setType("image/*"), IMG_GALLERY);
            }
        });

        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);
            }
        });

        //Link the photo on imageview to bitmap and save the image file using the path.
        savePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable drawable = (BitmapDrawable) displayPhoto.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                String path = currentPhotoPath;
                File file = new File(path);
                PhotoData photoData = new PhotoData();
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setType("image/*");
//                startActivityForResult(intent, READ_REQUEST_CODE);
                try{
                    photoData = new PhotoData(-1, file.getName(), path);
                    Toast.makeText(MainActivity.this, photoData.toString(),Toast.LENGTH_SHORT);
                    outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                    Toast.makeText(getApplicationContext(), "Image Saved to internal",Toast.LENGTH_SHORT).show();
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Failed to save", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                PhotoDBHelper photoDBHelper = new PhotoDBHelper(MainActivity.this);
                boolean success = photoDBHelper.addOne(photoData);
                Toast.makeText(MainActivity.this, "Success" + success, Toast.LENGTH_SHORT).show();

            }
        });

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }



    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Failed to create the file",Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                         "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Glide.with(this).load(currentPhotoPath).into(displayPhoto);
            fileLoc.setText(currentPhotoPath);
        }
        else if (requestCode == PICK_REQUEST && resultCode == RESULT_OK) {
            Uri selectedPhoto = data.getData();
            Glide.with(this).load(selectedPhoto).into(displayPhoto);
            fileLoc.setText(selectedPhoto.toString());
        }
        else if (requestCode == IMG_GALLERY && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            if (imageUri != null){
                startCrop(imageUri);
            }
        }
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK){
            Uri imageUriResultCrop = UCrop.getOutput(data);
            if (imageUriResultCrop != null){
                displayPhoto.setImageURI(imageUriResultCrop);
            }

        }
    }

    private void startCrop(@NonNull Uri uri){
        String destFileName = SAMPLE_CROPPED_IMG_NAME;
        destFileName += ".jpg";
        UCrop uCrop = UCrop.of(uri,Uri.fromFile(new File(getCacheDir(),destFileName)));
        uCrop.withAspectRatio(1,1);
//        uCrop.withAspectRatio(3,4);
//        uCrop.useSourceImageAspectRatio();
//        uCrop.withAspectRatio(2,3);
//        uCrop.withAspectRatio(16,9);
        uCrop.withAspectRatio(450,450);
        uCrop.withOptions(getCropOptions());
        uCrop.start(MainActivity.this);

    }
    private UCrop.Options getCropOptions(){
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(70);

        //CompressType
//        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);

        //UI
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        //Colors
        options.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        options.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));

        options.setToolbarTitle("TESTING EDITOR");
        return options;
    }
//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        outState.putAll();
//        super.onSaveInstanceState(outState);
//    }



}
