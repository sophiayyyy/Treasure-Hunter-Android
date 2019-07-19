package com.example.lt.treasurehunter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ProfilePhotoActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_choose;
    private Button btn_submit;
    private ImageView im_profile_photo;
    private Uri filePath;

    private final int PICK_IMAGE_REQUEST = 71;

    //firebase
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    DatabaseReference databaseUser;


    private Bitmap bitmap;
    private String profileBase64;
    GlobalClass globalClass;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activtiy_profile_photo);

        //Init view
        btn_choose = (Button)findViewById(R.id.btn_choose);
        btn_submit = (Button)findViewById(R.id.btn_submit);
        im_profile_photo = (ImageView)findViewById(R.id.im_profile_photo);

        btn_submit.setOnClickListener(this);
        btn_choose.setOnClickListener(this);

        //Init Firebase
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        databaseUser = FirebaseDatabase.getInstance().getReference("User");

        globalClass = (GlobalClass) getApplicationContext();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_choose:
                chooseImage();
                break;
            case R.id.btn_submit:
                uploadImage();
                break;
        }
    }

    //require minsdk>21
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void chooseImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            filePath = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                Bitmap newBitmap =getResizedBitmap(bitmap,500);
                im_profile_photo.setImageBitmap(newBitmap);
                profileBase64 = BitMapToString(newBitmap);
            }catch (IOException e){
                e.printStackTrace();
            }
        }else{
            im_profile_photo.setImageResource(R.drawable.ic_person_black_24dp);
            profileBase64 = "default";
        }
    }

    private void uploadImage(){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        progressDialog.show();

        //globalClass has the new imagebase64 but database has the oldimagebase64
        globalClass.setNewImgPath(profileBase64);

        progressDialog.dismiss();
        Toast.makeText(ProfilePhotoActivity.this, "Uploaded", Toast.LENGTH_LONG);
        //back to profile activity
        Intent i = new Intent(getApplicationContext(), ProfileActivity.class );
        startActivity(i);
    }
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
    //From https://stackoverflow.com/questions/15759195/reduce-size-of-bitmap-to-some-specified-pixel-in-android
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
