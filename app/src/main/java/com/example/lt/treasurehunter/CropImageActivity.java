package com.example.lt.treasurehunter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;

import java.io.ByteArrayOutputStream;

public class CropImageActivity extends AppCompatActivity implements View.OnClickListener{
    private com.example.lt.treasurehunter.CropImageView mCropImageView;
    private Bitmap dstbmp;
    private Bitmap bmSrc;
    private String imageBase64;
    private int cropWidth = 300;
    private int cropHeight = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        mCropImageView = (com.example.lt.treasurehunter.CropImageView) findViewById(R.id.cropimage);
        Button mBtnImg1 = (Button) findViewById(R.id.btn_img1);
        Button mBtnCut = (Button) findViewById(R.id.btn_cut);
        mBtnImg1.setOnClickListener(this);
        mBtnCut.setOnClickListener(this);

        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(this);
        imageBase64 = shre.getString("imgTaken", "");
        Bundle bundle = getIntent().getExtras();
        int choosePhoto = bundle.getInt("choosePhoto",0);
        Matrix matrix = new Matrix();
        if(CameraApp.getInstance().getmCameraDirection() == 1)
        {
            matrix.postRotate(270);
        }
        else
        {
            matrix.postRotate(90);
        }

        if(choosePhoto!=0){
            matrix.postRotate(270);
        }
        bmSrc = stringToBitMap(imageBase64);
        dstbmp = Bitmap.createBitmap(bmSrc, 0, 0, bmSrc.getWidth(), bmSrc.getHeight(),
                matrix, true);

        mCropImageView.setDrawable(dstbmp, cropWidth, cropHeight);
        mCropImageView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        Bitmap bitmap;
        // Set visibility to GONE so that onMeasure in CropImageView will be triggered to resize the control
        mCropImageView.setVisibility(View.GONE);
        switch (v.getId()){
            case R.id.btn_img1:
                v.startAnimation(buttonClick);
                bitmap = dstbmp;
                mCropImageView.setDrawable(bitmap, cropWidth, cropHeight);
                mCropImageView.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_cut:
                v.startAnimation(buttonClick);
                Bitmap croppedImage = mCropImageView.getCropImage();
                Bitmap recImage = mCropImageView.getRecImage();

                Intent i = new Intent(getApplicationContext(),PicProcessActivity.class);
                SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit=shre.edit();
                String croppedImageString = BitMapToString(croppedImage);
                String recImageString = BitMapToString(recImage);
                edit.putString("croppedImage",croppedImageString);
                edit.putString("recImage",recImageString);
                edit.commit();
                startActivity(i);
                finish();
                break;
            default:
                break;
        }
    }
    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.8F);

}
