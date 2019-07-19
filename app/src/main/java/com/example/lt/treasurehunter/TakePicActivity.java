package com.example.lt.treasurehunter;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.orhanobut.hawk.Hawk;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TakePicActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    String imgTakenPath;
    private final String SAMPLE_CROPPED_IMG_NAME = "SampleCropImg";

    private String TAG = "MyCamera";
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private ImageButton mTakePhoto = null;
    private ImageButton mReadImage = null;
    private ToggleButton switchCameraBtn = null;
    private ToggleButton flashBtn = null;
    private boolean openFlashLight = false;
    private final int CODE_IMG_GALLERY = 1;
    private final int REQUEST_GET_SINGLE_FILE = 999;

    //Identifier for the permission request
    private static final int READ_STORAGE_PERMISSIONS_REQUEST = 2;

    private BottomNavigationView camera_navigation;

    GlobalClass globalClass;
    private Boolean isLogIn; //true: already login, false: haven't login
    private int signInType;//0: google, 1: treasure hunter, 2:ins, -1: haven't login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_pic);
        setViews();
        initData();

        globalClass = (GlobalClass) getApplicationContext();
        isLogIn = globalClass.getLogIn();
        signInType = globalClass.getLogInType();
        camera_navigation = (BottomNavigationView)findViewById(R.id.camera_navigation);

        camera_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.nav_home:
                        Intent i = new Intent(getApplicationContext(), MainActivity.class );
                        startActivity(i);
                        break;
                    case R.id.nav_photo:
                        if(isLogIn){
                        }else {
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                            startActivity(intent);
                        }
                        break;
                    case R.id.nav_profile:
                        if(isLogIn){
                            Intent intent = new Intent(getApplicationContext(), MeActivity.class );
                            startActivity(intent);
                        }else {
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class );
                            startActivity(intent);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void initData(){
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);
    }

    private void setViews(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        flashBtn = (ToggleButton) findViewById(R.id.flash_light);
        flashBtn.setOnCheckedChangeListener(flashLightListner);

        mReadImage = (ImageButton) findViewById(R.id.round_img);
        mReadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),"read image",Toast.LENGTH_SHORT).show();
                    readImage();
                }
                else{
                    getPermissionToReadStorage();
                }
            }
        });


        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCamera!=null){
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success){
                                Log.e(TAG,"surfaceView CLICK auto focus success");
                            }
                        }
                    });
                }
            }
        });
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.equals(MotionEvent.ACTION_DOWN)){
                    if(mCamera!=null){
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (success){
                                    Log.e(TAG,"surfaceView TOUCH auto focus success");
                                }
                            }
                        });
                    }
                }
                return false;
            }
        });

        mTakePhoto = (ImageButton) findViewById(R.id.start_photo);
        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private ToggleButton.OnCheckedChangeListener mtoggle = new ToggleButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.e(TAG,"is Checked" + isChecked);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

            if(isChecked){
                CameraApp.getInstance().setmCameraDirection(0);
                Log.e(TAG,"front camera is opened");
            }else{
                CameraApp.getInstance().setmCameraDirection(1);
                Log.e(TAG,"back camera is opened");
            }
            initCamera();
        }
    };

    private ToggleButton.OnCheckedChangeListener flashLightListner = new ToggleButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.e(TAG,"isChecked"+isChecked);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

            if(isChecked){
                openFlashLight = false;
            }else{
                openFlashLight = true;
            }
            initCamera();
        }
    };

    private void initCamera(){
        try{
            mCamera = Camera.open(CameraApp.getInstance().getmCameraDirection());
        }catch (RuntimeException e){
            if ("Fail to connect to camera service".equals(e.getMessage())) {
                //check permission
            } else if ("Camera initialization failed".equals(e.getMessage())) {
                //can not initialize
            } else {
                //unknown error
            }
            e.printStackTrace();
            return;
        }

        CameraApp.getInstance().setCameraDisplayOrientation(TakePicActivity.this,0,mCamera);
        if (mCamera != null){
            try{
                Camera.Parameters parameters = mCamera.getParameters();
                int picWidth = parameters.getPictureSize().width;
                int picHeight = parameters.getPictureSize().height;
                Log.e("camera",String.valueOf(picWidth));
                Log.e("camera",String.valueOf(picHeight));
                List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
                int supportLength = supportedPictureSizes.size();
                int middleLength = (int)supportLength/2;
                parameters.setPictureSize(supportedPictureSizes.get(middleLength).width,supportedPictureSizes.get(middleLength).height);//1280 720; 1920 1440
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();


                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                if (openFlashLight) {
                    parameters.setFlashMode(parameters.FLASH_MODE_ON);
                } else
                {
                    parameters.setFlashMode(parameters.FLASH_MODE_OFF);
                }
                mCamera.setParameters(parameters);
                //start preview
                mCamera.setPreviewDisplay(mHolder);

                mCamera.startPreview();


            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    //solve deformation
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void takePhoto() {
        if (mCamera == null ) {
            return;
        }
        Log.d(TAG,"takePhoto ");
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                Intent i = new Intent(TakePicActivity.this,CropImageActivity.class);
                SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit=shre.edit();
                String encodedImage = Base64.encodeToString(data, Base64.DEFAULT);
                edit.putString("imgTaken",encodedImage);
                edit.commit();
                //i.putExtra("imgTaken",data);
                int direction = CameraApp.getInstance().getmCameraDirection();
                i.putExtra("direction",direction);
                startActivity(i);

            }
        });
    }

    private void readImage(){
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        startActivityForResult(new Intent()
                .setAction(Intent.ACTION_GET_CONTENT)
                .setType("image/*"),REQUEST_GET_SINGLE_FILE);
        initCamera();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try{
            if(resultCode == RESULT_OK){
                if (requestCode == REQUEST_GET_SINGLE_FILE){
                    Toast.makeText(getApplicationContext(),"in request code",Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getApplicationContext(),CropImageActivity.class);
                    InputStream iStream =   getContentResolver().openInputStream(data.getData());
                    byte[] inputData = getBytes(iStream);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(inputData, 0, inputData.length);
                    bitmap = getResizedBitmap(bitmap,1024);
                    SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor edit=shre.edit();
                    String encodedImage = BitMapToString(bitmap);
                    edit.putString("imgTaken",encodedImage);
                    edit.commit();
                    i.putExtra("choosePhoto",3);
                    startActivity(i);
                }
            }
        }catch (Exception e){
            Log.e("FileSelectorActivity", "File select error", e);
        }
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {

        byte[] bytesResult = null;
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        try {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        } finally {
            // close the stream
            try{ byteBuffer.close(); } catch (IOException ignored){ /* do nothing */ }
        }
        return bytesResult;
    }



    // save pic
    class SavePictureTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... params) {
            final String fname = DateFormat.format("yyyyMMddhhmmss", new Date()).toString()+".jpg";

            Log.e(TAG, "fname="+fname+";dir="+ CameraApp.getInstance().getAllSdPaths(TakePicActivity.this)[0]);
            //picture = new File(Environment.getExternalStorageDirectory(),fname);// create file
            File folder = new File(CameraApp.getInstance().getAllSdPaths(TakePicActivity.this)[0]+"/MyCamera/Photo");
            if(!folder.exists())
            {
                folder.mkdirs();
            }
            File picture = new File(CameraApp.getInstance().getAllSdPaths(TakePicActivity.this)[0]+"/MyCamera/Photo/"+fname);

            Log.e(TAG,"path "+picture.getPath() +"getAbsolutePath "+picture.getAbsolutePath());
            try {
                FileOutputStream fos = new FileOutputStream(picture.getPath()); // Get file output stream
                fos.write(params[0]); // Written to the file
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //solve 90
            Bitmap bitmap = BitmapFactory.decodeFile(picture.getPath());
            Matrix matrix = new Matrix();
            if(CameraApp.getInstance().getmCameraDirection() == 1)
            {
                matrix.postRotate(270);
            }
            else
            {
                matrix.postRotate(90);
            }

            Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);

            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(Environment.getExternalStorageDirectory()+"/MyCamera/Photo/"+"rotate"+fname);
                dstbmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (Exception e) {

            }
            picture.delete();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCamera.startPreview();
                    File f = new File(Environment.getExternalStorageDirectory() + "/MyCamera/photo/" + "rotate" + fname);
                    Toast.makeText(TakePicActivity.this, "store img path：" + Environment.getExternalStorageDirectory() + "/MyCamera/photo/" + "rotate" + fname, Toast.LENGTH_SHORT).show();
                    Log.e(TAG,"path "+"store img path：" + Environment.getExternalStorageDirectory() + "/MyCamera/photo/" + "rotate" + fname);
                    imgTakenPath = Environment.getExternalStorageDirectory() + "/MyCamera/photo/" + "rotate" + fname;
                }
            });

            return null;
        }
    }

    private UCrop.Options getCropOptions(){
        UCrop.Options options = new UCrop.Options();

        options.setCompressionQuality(100);

        // CompressType
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);

        // UI
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        // Colors
        options.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        options.setToolbarColor(getResources().getColor(R.color.colorPrimary));

//        options.setToolbarTitle("CROP");

        return options;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void getPermissionToReadStorage() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Display a popup message or equivalent.  Explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Camera Roll is Closed").setMessage("Search with saved images on your device. Give Treasure Hunter access to your Camera Roll in order to access your photo library.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(TakePicActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_STORAGE_PERMISSIONS_REQUEST);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {

                // Fire off an async request to actually get the permission
                // This will show the standard permission request dialog UI
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_STORAGE_PERMISSIONS_REQUEST);
            }
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            // Make sure it's our original request
            case READ_STORAGE_PERMISSIONS_REQUEST:{
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Read Storage permission granted", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(),"read image",Toast.LENGTH_SHORT).show();
                    readImage();

                } else {
                    Toast.makeText(this, "Read Storage permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

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
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }
}

