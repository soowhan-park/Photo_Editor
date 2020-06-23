package com.example.photogallery.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.photogallery.Fragments.BrushOptions;
import com.example.photogallery.Fragments.TextEditor;
import com.example.photogallery.R;
import com.yalantis.ucrop.UCrop;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;


public class MainActivity extends AppCompatActivity implements BrushOptions.Properties, OnPhotoEditorListener {
    private Button addPhoto;

    private Button takePhoto;
    private Button savePhoto;

    //Editing buttons
    private Button btnCrop;
    private Button btnText;
    private Button btnErase;
    private Button btnDraw;
    private Button btnUndo;
    private Button btnRedo;

    private TextView fileLoc;
    private TextView isImage;
    // Set the request codes
    private static final int PICK_REQUEST = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private static final int IMG_GALLERY = 3;

    //variables for camera
    private String currentPhotoPath;
    private String SAMPLE_CROPPED_IMG_NAME = "SampleCrop";
    private OutputStream outputStream;

    //URI for the displayed photo on image view
    private Uri selectedPhoto;

    //Photo editor from the github library
    private PhotoEditorView mPhotoEditorView;
    PhotoEditor mPhotoEditor;

    // Brush tools fragments
    private BrushOptions mBrushOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addPhoto = findViewById(R.id.btnAddButton);
        takePhoto = findViewById(R.id.btnTake);
        savePhoto = findViewById(R.id.btnSave);
        fileLoc = findViewById(R.id.tvLocation);
        isImage = findViewById(R.id.tvImg);
        mPhotoEditorView = findViewById(R.id.photoEditorView);

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
                intent.setAction(Intent.ACTION_GET_CONTENT);
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

       // mPhotoEditorView.getSource().setImageResource(R.drawable.ic_launcher_foreground);

       // Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);

//loading font from assest
        //Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");

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

    private void saveImage() {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + ""
                + System.currentTimeMillis() + ".png");
        try {
            file.createNewFile();

            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build();

            mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    Uri mSaveImageUri = Uri.fromFile(new File(imagePath));
                    mPhotoEditorView.getSource().setImageURI(mSaveImageUri);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO:
                    mPhotoEditor.clearAllViews();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhoto);
                        Glide.with(this).load(bitmap).into(mPhotoEditorView.getSource());
                        isImage.setVisibility(View.GONE);
//                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileLoc.setText(selectedPhoto.toString());
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        selectedPhoto = uri;
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        Glide.with(this).load(bitmap).into(mPhotoEditorView.getSource());
                        isImage.setVisibility(View.GONE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case UCrop.REQUEST_CROP:
                    Uri resultUri = UCrop.getOutput(data);
                    try {
                            Bitmap b_map = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                            mPhotoEditorView.getSource().setImageBitmap(b_map);
                            isImage.setVisibility(View.GONE);
                            selectedPhoto = resultUri;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
            }
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