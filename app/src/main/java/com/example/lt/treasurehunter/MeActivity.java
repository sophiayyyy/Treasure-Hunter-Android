package com.example.lt.treasurehunter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MeActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{

    private Button btnlogout;
    private TextView txt_username, txt_email;
    private ImageView im_profile;
    private ImageButton btn_edit;

    private String cur_name;
    private String cur_email;
    private String isProfilePhotoChange = "0";
    private String profileBase64="";

    private Boolean isLogIn; //true: already login, false: haven't login

    //firebase
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    DatabaseReference databaseUser;
    private GoogleSignInClient mGoogleSignInClient;

    private BottomNavigationView me_navigation;

    //Identifier for the permission request
    private static final int CAMERA_PERMISSIONS_REQUEST = 1;
    private View to_favorites;
    private View to_history;

    GlobalClass globalClass;

    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);

        txt_username = (TextView)findViewById(R.id.txt_username);
        txt_email = (TextView)findViewById(R.id.txt_email);
        im_profile = (ImageView)findViewById(R.id.im_profile);
        me_navigation = (BottomNavigationView)findViewById(R.id.me_navigation);
        btn_edit = (ImageButton)findViewById(R.id.btn_edit);
        to_favorites = (View) findViewById(R.id.to_favorites);
        to_history = (View) findViewById(R.id.to_history);
        btn_edit.setOnClickListener(this);
        //get global variables
        globalClass = (GlobalClass) getApplicationContext();
        isLogIn = globalClass.getLogIn();
        cur_name = globalClass.getUserName();
        cur_email = globalClass.getEmail();
        profileBase64 = globalClass.getNewImgPath();
        if(profileBase64.equals("default")){
            profileBase64 = globalClass.getProfileBase64();
        }
        setProgressDialog();
        checkLogInStatus();
        me_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.nav_home:
                        Intent i = new Intent(getApplicationContext(), MainActivity.class );
                        startActivity(i);
                        break;
                    case R.id.nav_photo:
                        if(isLogIn){
                            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                Intent intent = new Intent(MeActivity.this, TakePicActivity.class);
                                startActivity(intent);
                            }
                            else{
                                getPermissionToCamera();
                            }
                        }else {
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                            startActivity(intent);
                        }
                        break;
                    case R.id.nav_profile:

                        break;
                }
                return false;
            }
        });

        to_favorites.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(getApplicationContext(), FavoritesActivity.class);
                startActivity(i);
            }

        });

        to_history.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(getApplicationContext(), HistoryActivity.class);
                startActivity(i);
            }

        });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_edit:
                if(isLogIn){
                    Intent i = new Intent(getApplicationContext(), ProfileActivity.class );
                    startActivity(i);
                    break;
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void setProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processing...");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
        progressDialog.show();
    }


    public void checkLogInStatus(){
        if (isLogIn == false) { //haven't login, jump to login page
            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(i);
        } else { //load info
            if (profileBase64.equals("default"))
                im_profile.setImageResource(R.drawable.ic_person_black_24dp);
            else {
                im_profile.setImageBitmap(stringToBitMap(profileBase64));
            }
        }
        txt_username.setText(cur_name);
        txt_email.setText(cur_email);
        progressDialog.dismiss();
    }

    // Called when the user is performing an action which requires the app to read the user's camera
    public void getPermissionToCamera() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Display a popup message or equivalent.  Explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Camera is Disabled").setMessage("Treasure Hunter is a camera based search app. To continue, you need to allow Camera access in Settings.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MeActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_PERMISSIONS_REQUEST);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {

                // Fire off an async request to actually get the permission
                // This will show the standard permission request dialog UI
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSIONS_REQUEST);
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
            case CAMERA_PERMISSIONS_REQUEST: {
                if (grantResults.length >0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Read Camera permission granted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MeActivity.this,TakePicActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Read Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
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
