package com.example.photogallery.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.photogallery.Database.PhotoDBHelper;
import com.example.photogallery.Database.PhotoData;
import com.example.photogallery.Fragments.BrushOptions;
import com.example.photogallery.Fragments.TextEditor;
import com.example.photogallery.GoogleVisionUtils.LabelDetectionTask;
import com.example.photogallery.GoogleVisionUtils.PackageManagerUtils;
import com.example.photogallery.R;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;



public class MainActivity extends AppCompatActivity implements BrushOptions.Properties, OnPhotoEditorListener {
    private ImageView addPhoto;

    private ImageView takePhoto;
    private ImageView savePhoto;

    //Editing buttons
    private ImageView btnCrop;
    private ImageView btnText;
    private ImageView btnErase;
    private ImageView btnDraw;
    private ImageView btnUndo;
    private ImageView btnRedo;

    private LinearLayout linearLayout;

    private TextView isImage;
    private TextView mImageDetails;

    private SpinKitView loadView;

    // Set the request codes
    private static final int PICK_REQUEST = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;

    //URI for the displayed photo on image view
    private Uri selectedPhoto;

    //Photo editor from the github library
    private PhotoEditorView mPhotoEditorView;
    PhotoEditor mPhotoEditor;

    // Brush tools fragments
    private BrushOptions mBrushOptions;

    //API Key, Google Vision Setups
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCcu8nNolVAROzgcUn0zOquyrgnwAUM1Jk";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final int MAX_LABEL_RESULTS = 10;

    private static String[] tags;
    private static int counter;
    private static int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        addPhoto = findViewById(R.id.btnAddButton);
        takePhoto = findViewById(R.id.btnTake);
        savePhoto = findViewById(R.id.btnSave);
        isImage = findViewById(R.id.tvImg);
        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mImageDetails = findViewById(R.id.image_details);
        linearLayout = findViewById(R.id.linearlayout);

        loadView = findViewById(R.id.spin_kit2);
        loadView.setVisibility(View.INVISIBLE);

        //Edit buttons
        btnCrop = findViewById(R.id.btnCrop);
        btnErase = findViewById(R.id.btnErase);
        btnText = findViewById(R.id.btnText);
        btnDraw = findViewById(R.id.btnDraw);
        btnRedo = findViewById(R.id.btnRedo);
        btnUndo = findViewById(R.id.btnUndo);


        //Button onClickListeners
        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoEditor.setBrushDrawingMode(true);
                mBrushOptions.show(getSupportFragmentManager(), mBrushOptions.getTag());
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoEditor.undo();
            }
        });


        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPhoto != null)
                    startCrop(selectedPhoto);
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoEditor.redo();
            }
        });

        btnErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhotoEditor.brushEraser();
            }
        });

        btnText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextEditor textEditor = TextEditor.show((AppCompatActivity)v.getContext());
                textEditor.setOnTextEditorListener(new TextEditor.AddText() {
                    @Override
                    public void onDone(String inputText, int colorCode) {
                        final TextStyleBuilder textStyleBuilder = new TextStyleBuilder();
                        textStyleBuilder.withTextColor(colorCode);
                        mPhotoEditor.addText(inputText, textStyleBuilder);
                    }
                });
            }
        });

        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);

            }
        });

        isImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);
            }
        });

        savePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPhoto == null)
                    Toast.makeText(MainActivity.this, "이미지를 추가하세요", Toast.LENGTH_SHORT).show();
                else {
                    saveImage();

                }
            }
        });

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

//loading font from assest

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
//                .setDefaultTextTypeface(mTextRobotoTf)
//                .setDefaultEmojiTypeface(mEmojiTypeFace)
                .build();
        //Fragments
        mBrushOptions = new BrushOptions();
        mBrushOptions.setPropertiesChangeListener(this);
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
                selectedPhoto = FileProvider.getUriForFile(this,
                         "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedPhoto);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
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
        return image;
}

    private void saveImage() {
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + ""
                + System.currentTimeMillis() + ".png");
        Log.d("fileLoc",getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());
        try {
            file.createNewFile();
//            SaveSettings saveSettings = new SaveSettings.Builder()
//                    .setClearViewsEnabled(true)
//                    .setTransparencyEnabled(true)
//                    .build();
//
//            mPhotoEditor.saveAsFile(file.getAbsolutePa th(), saveSettings, new PhotoEditor.OnSaveListener() {
//                @Override
//                public void onSuccess(@NonNull String imagePath) {
//                    Uri mSaveImageUri = Uri.fromFile(new File(imagePath));
//                    mPhotoEditorView.getSource().setImageURI(mSaveImageUri);
//                    Toast.makeText(MainActivity.this, "저장 성공", Toast.LENGTH_SHORT).show();
//                }
            mPhotoEditor.saveAsFile(file.getAbsolutePath(), new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    //Save path and image file name to database
                    PhotoData photoData = new PhotoData(-1, selectedPhoto.toString(), file.getName());
                    PhotoDBHelper photoDBHelper = new PhotoDBHelper(MainActivity.this);
                    boolean success = photoDBHelper.addOne(photoData);
                    Toast.makeText(MainActivity.this, photoData.toString(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Success=" + success, Toast.LENGTH_SHORT).show();
                    Uri mSaveImageUri = Uri.fromFile(new File(imagePath));
                    mPhotoEditorView.getSource().setImageURI(mSaveImageUri);
                    mPhotoEditorView.getSource().setImageBitmap(null);
                    isImage.setVisibility(View.VISIBLE);
                    btnCrop.setVisibility(View.INVISIBLE);
                    btnErase.setVisibility(View.INVISIBLE);
                    btnText.setVisibility(View.INVISIBLE);
                    btnDraw.setVisibility(View.INVISIBLE);
                    btnRedo.setVisibility(View.INVISIBLE);
                    btnUndo.setVisibility(View.INVISIBLE);
                    linearLayout.removeAllViews();

                    Log.d("aftersave",mSaveImageUri.toString());
                    Toast.makeText(MainActivity.this, "저장 성공", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("PhotoEditor","Failed to save Image");
                    Toast.makeText(MainActivity.this, "저장 실패", Toast.LENGTH_SHORT).show();
                    exception.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO:
                    mPhotoEditor.clearAllViews();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhoto);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                        callCloudVision(bitmap);
                        try (InputStream inputStream = getContentResolver().openInputStream(selectedPhoto)) {
                            assert inputStream != null;
                            ExifInterface exif = new ExifInterface(inputStream);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            mPhotoEditorView.getSource().setRotation(rotationInDegrees);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        isImage.setVisibility(View.GONE);
                        btnCrop.setVisibility(View.VISIBLE);
                        btnErase.setVisibility(View.VISIBLE);
                        btnText.setVisibility(View.VISIBLE);
                        btnDraw.setVisibility(View.VISIBLE);
                        btnRedo.setVisibility(View.VISIBLE);
                        btnUndo.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        selectedPhoto = uri;
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        callCloudVision(bitmap);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                        isImage.setVisibility(View.GONE);
                        btnCrop.setVisibility(View.VISIBLE);
                        btnErase.setVisibility(View.VISIBLE);
                        btnText.setVisibility(View.VISIBLE);
                        btnDraw.setVisibility(View.VISIBLE);
                        btnRedo.setVisibility(View.VISIBLE);
                        btnUndo.setVisibility(View.VISIBLE);
                        //Fixed issue when the image is printed in landscape mode
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            assert inputStream != null;
                            ExifInterface exif = new ExifInterface(inputStream);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            mPhotoEditorView.getSource().setRotation(rotationInDegrees);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case UCrop.REQUEST_CROP:
                    Uri resultUri = UCrop.getOutput(data);
                    try {
                            Bitmap b_map = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                            mPhotoEditorView.getSource().setImageBitmap(b_map);
                            callCloudVision(b_map);
                        try (InputStream inputStream = getContentResolver().openInputStream(resultUri)) {
                            assert inputStream != null;
                            ExifInterface exif = new ExifInterface(inputStream);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            mPhotoEditorView.getSource().setRotation(rotationInDegrees);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                            isImage.setVisibility(View.GONE);
                            selectedPhoto = resultUri;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
            }
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(final Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("LABEL_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    public static String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message.append(String.format(Locale.US, "%s", label.getDescription()));
                message.append(",");
                //message.append(String.format(Locale.US, "%.3f: %s", labels.get(i).getScore(), labels.get(i).getDescription()));
                //message.append("\n");
            }
        } else {
            message.append("nothing");
        }
        return message.toString();
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        mImageDetails.setText(R.string.loading_message);
        loadView.setVisibility(View.VISIBLE);
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LabelDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
        }
    }

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setBrushColor(colorCode);
    }

    @Override
    public void onBrushSizeChanged(int brushSize) {
        mPhotoEditor.setBrushSize(brushSize);
    }

    private void startCrop(@NonNull Uri uri){
        UCrop uCrop = UCrop.of(uri,Uri.fromFile(new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + ""
                + System.currentTimeMillis() + ".png")));
        uCrop.withAspectRatio(1,1);
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

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditor textEditor =
                TextEditor.show(this, text, colorCode);
        textEditor.setOnTextEditorListener(new TextEditor.AddText() {
            @Override
            public void onDone(String inputText, int colorCode) {
                final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                styleBuilder.withTextColor(colorCode);
                mPhotoEditor.editText(rootView, inputText, styleBuilder);
            }
        });
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
    }

    //Save and restore the image after the cycle is killed
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(selectedPhoto != null)
            outState.putString("selectedPhoto",selectedPhoto.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedPhoto = Uri.parse(savedInstanceState.getString("selectedPhoto"));
    }
}