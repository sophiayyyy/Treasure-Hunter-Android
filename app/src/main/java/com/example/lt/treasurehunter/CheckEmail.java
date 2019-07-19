package com.example.lt.treasurehunter;

import java.util.regex.Pattern;

public class CheckEmail {
    //Use regular expressions to detect whether mailbox formats is right
    private String email;
    Pattern emailer = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    public CheckEmail(String newEmail){
        setEmail(newEmail);
    }
    public void setEmail(String newEmail){
        email = newEmail;
    }
    public boolean checkValid(){

        email = email.toLowerCase();
        if(email.endsWith(".con")) return false;
        if(email.endsWith(".cm")) return false;
        return emailer.matcher(email).matches();

    }
}
