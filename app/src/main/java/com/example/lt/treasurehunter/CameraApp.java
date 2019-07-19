package com.example.lt.treasurehunter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Camera;
import android.os.storage.StorageManager;
import android.view.Surface;

import com.orhanobut.hawk.Hawk;

import java.lang.reflect.Method;

public class CameraApp extends Application {

    private static CameraApp app = null;
    private int mCameraDirection = 0;

    public static CameraApp getInstance(){
        if (app == null){
            app = new CameraApp();
        }
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Hawk.init(this).build();
    }

    public int getmCameraDirection(){
        return mCameraDirection;
    }
    public void setmCameraDirection(int dir){
        mCameraDirection = dir;
    }

    public void setCameraDisplayOrientation(Activity activity , int cameraId , android.hardware.Camera camera){
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId,info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface
                    .ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;

        }
        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;//compensate the mirror
        }else{
            //back facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
    public static String[] getAllSdPaths(Context context) {
        Method mMethodGetPaths = null;
        String[] paths = null;

        StorageManager mStorageManager = (StorageManager)context
                .getSystemService(context.STORAGE_SERVICE);//storage
        try {
            mMethodGetPaths = mStorageManager.getClass().getMethod("getVolumePaths");
            paths = (String[]) mMethodGetPaths.invoke(mStorageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

}

