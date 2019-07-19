package com.example.lt.treasurehunter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PostDisplayActivity extends AppCompatActivity {
    ImageView postImageView;
    ListView bestOptionLV;
    TextView tags;
    AwesomeAdapter bestOptionAdapter;
    DatabaseReference databaseUser;
    DatabaseReference databasePosts;
    HashMap<String, String> bestOption = new HashMap<String, String>();
    String postId;
    String userId;
    String imageBase64;
    Post post;
    GlobalClass globalClass;
    ProgressDialog progressDialog;

    private Boolean isLogIn; //true: already login, false: haven't login
    private BottomNavigationView post_navigation;
    //Identifier for the permission request
    private static final int CAMERA_PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_display);
        //initialization
        postImageView = (ImageView) findViewById(R.id.historyImageView);
        tags = (TextView) findViewById(R.id.historytags);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        userId = intent.getStringExtra("userId");
        //firebase references
        databaseUser = FirebaseDatabase.getInstance().getReference("User");
        databasePosts = databaseUser.child(userId).child("historyPostList");

        post_navigation = (BottomNavigationView)findViewById(R.id.post_navigation);
        globalClass = (GlobalClass) getApplicationContext();
        isLogIn = globalClass.getLogIn();

        // read post from firebase based on the postId
        getPost();

        post_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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
                                Intent intent = new Intent(PostDisplayActivity.this, TakePicActivity.class);
                                startActivity(intent);
                            }
                            else{
                                getPermissionToCamera();
                            }
                        }else {
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class );
                            startActivity(intent);
                        }
                        break;
                    case R.id.nav_profile:
                        if(isLogIn){
                            Intent intent = new Intent(getApplicationContext(), MeActivity.class);
                            startActivity(intent);
                        }else {
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                            startActivity(intent);
                        }
                        break;
                }
                return false;
            }
        });
    }
    // read post from firebase based on the postId
    private void getPost(){
        databasePosts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setProgressDialog();
                post = dataSnapshot.child(postId).getValue(Post.class);
                //read image base64 string and setView
                imageBase64 = post.getPostImageBase64();
                Bitmap bmp = stringToBitMap(imageBase64);
                Bitmap roundImage = roundImageHelper.getRoundedCornerBitmap(bmp,8);
                postImageView.setImageBitmap(roundImage);
                tags.setText(post.getTag());
                bestOption=post.getBestOption();
                bestOption.put("visible", "false");
                final ArrayList<HashMap<String, String>> bestOptionList = new ArrayList<HashMap<String, String>>();
                final String itemLink = bestOption.get("link");
                bestOptionList.clear();
                bestOptionList.add(bestOption);
                bestOptionLV = (ListView) findViewById(R.id.historybestOptionLV);
                bestOptionAdapter = new AwesomeAdapter(getBaseContext(), bestOptionList,1);
                bestOptionLV.setAdapter(bestOptionAdapter);
                progressDialog.dismiss();
                bestOptionLV.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if(position==0){
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(itemLink));
                            startActivity(browserIntent);
                        }
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });
    }

    public void setProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
        progressDialog.show();
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
                        ActivityCompat.requestPermissions(PostDisplayActivity.this,
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
                    Intent intent = new Intent(PostDisplayActivity.this,TakePicActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Read Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}

