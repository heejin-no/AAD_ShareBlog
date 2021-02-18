package com.example.shareblog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;


public class SetupActivity extends AppCompatActivity {
    private CircleImageView setupImage;
    private Uri mainImageURI = null;

//    private String user_id = "";
    private String user_id;

    private boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private Bitmap compressedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setting");

        firebaseAuth = FirebaseAuth.getInstance();
        user_id = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        setupImage = findViewById(R.id.setup_image);
        setupName = findViewById(R.id.setup_name);
        setupBtn = findViewById(R.id.setup_btn);
        setupProgress = findViewById(R.id.setup_progress);

        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);

        isChanged = false; //아직없음

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    //Chequeo antes si el path existe
                    if (task.getResult().exists()) {

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        setupName.setText(name);
                        mainImageURI = Uri.parse(image);

                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.default_image);

                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);

//                        if (mainImageURI.toString().isEmpty())
//                            setupImage.setImageResource(R.drawable.default_image);
//                        else
//                            Glide.with(SetupActivity.this).load(mainImageURI).into(setupImage);

                    }
                }else{

                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"FIRESTORE Retrieve Error: "+error,Toast.LENGTH_LONG).show();

                }
                setupProgress.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });


        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String user_name = setupName.getText().toString();

                if (!TextUtils.isEmpty(user_name) && mainImageURI != null) {

                    setupProgress.setVisibility(View.VISIBLE);

                    if (isChanged){

                        user_id = firebaseAuth.getCurrentUser().getUid();

                        File newImageFile = new File(mainImageURI.getPath());
                        try {

                            compressedImageFile = new Compressor(SetupActivity.this)
                                    .setMaxHeight(125)
                                    .setMaxWidth(125)
                                    .setQuality(50)
                                    .compressToBitmap(newImageFile);

                        } catch (IOException e){
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbData = baos.toByteArray();

                        UploadTask image_path = storageReference.child("profile_images").child(user_id + ".jpg").putBytes(thumbData);

                        image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()  {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                if (task.isSuccessful()){

                                    storeFirestore(task.toString(), user_name);

                                } else {

                                    String error = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "(Image Error) : " + error, Toast.LENGTH_LONG).show();

                                    setupProgress.setVisibility(View.INVISIBLE);


                                }
                            }
                        });

                    }else {

                        storeFirestore(null, user_name);

                    }

                } else {
                    storeFirestore(mainImageURI.toString(), user_name);
                }
            }

        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){ //Si la version es Android 6 Marshmallow o superior entonces...
                    if (ContextCompat.checkSelfPermission(SetupActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        //No tengo el permiso. Se lo pido al usuario
                        Toast.makeText(SetupActivity.this,"Permission Denied!",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }else{
                        BringImagePicker();
                    }
                }else{
                    //En versiones anteriores, o se aceptan todos los permisos o no podes usar la app.
                    //Si se esta usando la app, es que se aceptaron todos los permisos entonces.
                    BringImagePicker();
                }
            }
        });
    }

    private void storeFirestore(String download_url, String user_name) {
        //Uri download_uri = task.getResult().getUploadSessionUri();

        Map<String,String> userMap = new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",download_url);

        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(SetupActivity.this,"The user settings were updated.",Toast.LENGTH_LONG).show();
                    Intent mainIntent = new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }else{
                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"FIRESTORE Error: "+error,Toast.LENGTH_LONG).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void BringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .setMinCropResultSize(512,512)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                isChanged = true;
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(SetupActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}