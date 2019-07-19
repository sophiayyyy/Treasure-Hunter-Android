package com.example.lt.treasurehunter;

import android.app.Application;
import android.graphics.Bitmap;

public class GlobalClass extends Application {

    private Boolean isLogIn;
    private int logInType;
    private String insname;
    private String userName;
    private String email;
    private String profileBase64;
    private String newImgPath;
    private String userId;

    public GlobalClass(){
        this.isLogIn = false;
        this.logInType = -1;
        this.email = "";
        this.insname = "";
        this.profileBase64 = "default";
        this.newImgPath = "default";
        this.userId = "";
        this.userName= "";
    }

    public void setLogIn(Boolean logIn) {
        isLogIn = logIn;
    }

    public void setLogInType(int logInType) {
        this.logInType = logInType;
    }

    public void setInsname(String insname) { this.insname = insname; }

    public void setEmail(String email) { this.email = email; }

    public void setNewImgPath(String imgPath) { this.newImgPath = imgPath; }

    public void setProfileBase64(String profileBase64) { this.profileBase64 = profileBase64; }

    public void setUserId(String userId) { this.userId = userId; }

    public void setUserName(String userName) { this.userName = userName; }

    public Boolean getLogIn() { return isLogIn; }

    public int getLogInType() {
        return logInType;
    }

    public String getInsname() { return insname; }

    public String getEmail() { return email; }

    public String getProfileBase64(){ return profileBase64; }

    public String getNewImgPath() { return newImgPath; }

    public String getUserId() { return userId; }

    public String getUserName(){return userName;}

}
