package com.example.lt.treasurehunter;

import java.util.ArrayList;
import java.util.List;

public class User {
    String userId;
    String userName;
    String email;
    String insName;
    String profileBase64;
    String pwd;
    String email_pwd;

    List<Post> favoritePostList;
    List<Post> historyPostList;

    public User() {

    }

    public User(String userId, String userName, String email, String pwd, String email_pwd, String insName, String profileBase64,List<Post> favoritePostList, List<Post> historyPostList){
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.pwd = pwd;
        this.email_pwd = email_pwd;
        this.insName = insName;
        this.profileBase64 = profileBase64;
        this.favoritePostList = favoritePostList;
        this.historyPostList = historyPostList;
    }

    public String getEmail_pwd(){
        return email_pwd;
    }

    public String getPwd(){
        return pwd;
    }

    public String getProfileBase64() {
        return profileBase64;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getInsName() {
        return insName;
    }

    public void setUserId(String userId) { this.userId = userId; }

    public void setUserName(String userName) { this.userName = userName; }

    public void setEmail(String email) { this.email = email; }

    public void setInsName(String insName) { this.insName = insName; }

    public void setProfileBase64(String profileBase64) { this.profileBase64 = profileBase64; }

    public void setPwd(String pwd) { this.pwd = pwd; }

    public void setEmail_pwd(String email_pwd) { this.email_pwd = email_pwd; }
}
