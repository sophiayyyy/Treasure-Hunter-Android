package com.example.lt.treasurehunter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton btn_profile_back;
    private ImageButton btn_profile_save;
    private ImageButton btn_profile_edit;

    private EditText edt_profile_username;
    private EditText edt_profile_email;
    private EditText edt_profile_pwd;

    private Button btn_logout;

    private ImageView iv_profile_photo;
    private GoogleSignInClient mGoogleSignInClient;

    //firebase
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    DatabaseReference databaseUser;

    //signInType: 0 refers to general user, 1 refers to google user, 2 refers to ins user
    private int signInType;
    private Boolean isSignIn;
    private String insname;
    private String cur_name;
    private String cur_email;
    private String profileBase64;
    GlobalClass globalClass;

    private Boolean isUpdate = false;

    ProgressDialog progressDialog;
    private CheckEmail emailChecker;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btn_profile_back = (ImageButton)findViewById(R.id.btn_profile_back);
        btn_profile_save = (ImageButton)findViewById(R.id.btn_profile_save);
        btn_profile_edit = (ImageButton)findViewById(R.id.btn_profile_edit);

        edt_profile_username = (EditText)findViewById(R.id.edt_profile_username);
        edt_profile_email = (EditText)findViewById(R.id.edt_profile_email);
        edt_profile_pwd = (EditText)findViewById(R.id.edt_profile_pwd);

        btn_logout = (Button)findViewById(R.id.btn_logout);

        iv_profile_photo = (ImageView)findViewById(R.id.iv_profile_photo);

        emailChecker = new CheckEmail(null);

        btn_profile_back.setOnClickListener(this);
        btn_profile_save.setOnClickListener(this);
        btn_profile_edit.setOnClickListener(this);
        btn_logout.setOnClickListener(this);

        globalClass = (GlobalClass) getApplicationContext();
        cur_email = globalClass.getEmail();
        cur_name = globalClass.getUserName();
        signInType = globalClass.getLogInType();
        isSignIn = globalClass.getLogIn();
        profileBase64 = globalClass.getNewImgPath();
        if(profileBase64.equals("default")){
            profileBase64 = globalClass.getProfileBase64();
        }

        //Init Firebase
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        databaseUser = FirebaseDatabase.getInstance().getReference("User");

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by signInOptions.
        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        //progressing part
        setProgressDialog();

        //load original info
        loadInfo();

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_logout:
                signOut();
                break;
            case R.id.btn_profile_back:
                //clear the change in globalClass
                //do nothing to the firebase
                globalClass.setNewImgPath("default");
                Intent i = new Intent(getApplicationContext(), MeActivity.class );
                startActivity(i);
                break;
            case R.id.btn_profile_save:
                //setProgressDialog();
                checkValidationForUpdate();
                if(isUpdate){
                    updateDatabase();
                }
                break;
            case R.id.btn_profile_edit:
                Intent intent = new Intent(getApplicationContext(), ProfilePhotoActivity.class );
                startActivity(intent);
                break;
        }
    }

    public void setProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processing...");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    private void loadInfo() {
        if (isSignIn == false) { //haven't login, jump to login page
            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(i);
        } else {
            switch (signInType){
                case 0 :
                    edt_profile_username.setText(cur_name);
                    edt_profile_email.setText(cur_email);
                    if(profileBase64.equals("default"))
                        iv_profile_photo.setImageResource(R.drawable.ic_person_black_24dp);
                    else {
                        iv_profile_photo.setImageBitmap(stringToBitMap(profileBase64));
                    }
                    if(!cur_email.isEmpty()){
                        databaseUser.orderByChild("email").equalTo(cur_email)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                                            String key = child.getKey();
                                            Log.d("User key", child.getKey());
                                            edt_profile_pwd.setText(child.child("pwd").getValue(String.class));
                                        }
                                        progressDialog.dismiss();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    }else {
                        Toast.makeText(this, "Empty email error.", Toast.LENGTH_SHORT);
                    }
                    break;
                case 1:
                    edt_profile_username.setText(cur_name);
                    edt_profile_email.setText(cur_email);
                    if(profileBase64.equals("default"))
                        iv_profile_photo.setImageResource(R.drawable.ic_person_black_24dp);
                    else {
                        iv_profile_photo.setImageBitmap(stringToBitMap(profileBase64));
                    }
                    if(!cur_email.isEmpty()){
                        databaseUser.orderByChild("email").equalTo(cur_email)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                                            String key = child.getKey();
                                            Log.d("User key", child.getKey());
                                            edt_profile_pwd.setText(child.child("pwd").getValue(String.class));
                                        }
                                        progressDialog.dismiss();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    }else {
                        Toast.makeText(this, "Empty email error.", Toast.LENGTH_SHORT);
                    }
                    break;
                case 2:
                    insname = globalClass.getInsname();
                    edt_profile_username.setText(cur_name);
                    edt_profile_email.setText(cur_email);
                    if(profileBase64.equals("default"))
                        iv_profile_photo.setImageResource(R.drawable.ic_person_black_24dp);
                    else {
                        iv_profile_photo.setImageBitmap(stringToBitMap(profileBase64));
                    }
                    if(!insname.isEmpty()){
                        databaseUser.orderByChild("insName").equalTo(insname)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                                            String key = child.getKey();
                                            Log.d("User key", child.getKey());
                                            edt_profile_pwd.setText(child.child("pwd").getValue(String.class));
                                        }
                                        progressDialog.dismiss();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }else {
                        Toast.makeText(this, "Empty insname error.", Toast.LENGTH_SHORT);
                    }
                    break;
            }
        }
    }


    private void signOut(){
        switch (signInType){
            case 0:
                updateInfo();
                backToMainActivity();
                break;
            case 1:
                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                updateInfo();
                                backToMainActivity();
                            }
                        });
                break;
            case 2:
                updateInfo();
                backToMainActivity();
                break;
        }
    }

    private void updateInfo(){
        globalClass.setLogIn(false);
        globalClass.setLogInType(-1);
        globalClass.setEmail("");
        globalClass.setInsname("");
        globalClass.setNewImgPath("default");
        globalClass.setUserId("");
        globalClass.setProfileBase64("default");
        globalClass.setUserName("");
    }

    private void backToMainActivity(){
        Intent i = new Intent(getApplicationContext(), MainActivity.class );
        startActivity(i);
    }

    private void checkValidationForUpdate(){
        String new_username = edt_profile_username.getText().toString();
        String new_email = edt_profile_email.getText().toString();
        String new_password = edt_profile_pwd.getText().toString();

        emailChecker.setEmail(new_email);
        //check validation
        if(TextUtils.isEmpty(new_username)) {
            edt_profile_username.setError("Username can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(TextUtils.isEmpty(new_email)) {
            edt_profile_email.setError("Email can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(!emailChecker.checkValid()){
            edt_profile_email.setError("Invaild Email.");
            progressDialog.dismiss();
            return;
        }
        if(TextUtils.isEmpty(new_password)) {
            edt_profile_pwd.setError("Password can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(new_password.length() < 6 || new_password.length() > 11){
            edt_profile_pwd.setError("Length of password should between 6 to 11.");
            progressDialog.dismiss();
            return;
        }
        if(!CheckPasswordVaildation(new_password)){
            edt_profile_pwd.setError("Passwords need to contain uppercase letters, lowercase letters and numbers.");
            progressDialog.dismiss();
            return;
        }

        //Check new email (email should be unique in database)
        if(cur_email.equals(new_email)){
            isUpdate = true;
            progressDialog.dismiss();
            return;
        }else {
            databaseUser.orderByChild("email").equalTo(new_email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                //user already in our database
                                edt_profile_email.setError("Email has been registered.");
                                progressDialog.dismiss();
                            } else {
                                //if email hasn't been used
                                //it can be saved
                                isUpdate = true;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    public Boolean CheckPasswordVaildation(String pwd){
        char ch;
        boolean upperFlag = false;
        boolean lowerFlag = false;
        boolean digitFlag = false;
        for(int i = 0; i < pwd.length(); i++) {
            ch = pwd.charAt(i);
            if( Character.isDigit(ch))
                digitFlag = true;
            else if (Character.isUpperCase(ch))
                upperFlag = true;
            else if (Character.isLowerCase(ch))
                lowerFlag = true;

            if(digitFlag && upperFlag && lowerFlag)
                return true;
        }
        return false;
    }

    private void updateDatabase(){
        final String new_username = edt_profile_username.getText().toString();
        final String new_email = edt_profile_email.getText().toString();
        final String new_password = edt_profile_pwd.getText().toString();
        //final String curUserId = globalClass.getUserId();
        databaseUser.orderByChild("email").equalTo(cur_email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            String key = child.getKey();
                            databaseUser.child(key).child("userName").setValue(new_username);
                            databaseUser.child(key).child("pwd").setValue(new_password);
                            databaseUser.child(key).child("email").setValue(new_email);
                            databaseUser.child(key).child("email_pwd").setValue(new_email + "_" + new_password);
                            databaseUser.child(key).child("profileBase64").setValue(profileBase64);
                            globalClass.setEmail(new_email);
                            globalClass.setUserName(new_username);
                        }
                        progressDialog.dismiss();
                        //back to MeActivity
                        Intent i = new Intent(getApplicationContext(), MeActivity.class );
                        startActivity(i);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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
}
