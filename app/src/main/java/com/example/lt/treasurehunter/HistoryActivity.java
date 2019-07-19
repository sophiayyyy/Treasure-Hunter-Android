package com.example.lt.treasurehunter;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView recyclerView;
    private BottomNavigationView history_navigation;

    private List<Post> historyList;
    HistoryAdapter adapter;

    private ImageButton btn_history_back;

    GlobalClass globalClass;
    private Boolean isLogIn; //true: already login, false: haven't login
    private int signInType;//0: google, 1: treasure hunter, 2:ins, -1: haven't login
    private String userId;

    //firebase
    DatabaseReference databaseUser;
    DatabaseReference databaseHome;
    DatabaseReference databaseHistory;

    //Identifier for the permission request
    private static final int CAMERA_PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        btn_history_back = (ImageButton)findViewById(R.id.btn_history_back);
        btn_history_back.setOnClickListener(this);

        globalClass = (GlobalClass) getApplicationContext();
        isLogIn = globalClass.getLogIn();
        signInType = globalClass.getLogInType();
        userId = globalClass.getUserId();

        //Init Firebase
        databaseUser = FirebaseDatabase.getInstance().getReference("User");
        databaseHome = FirebaseDatabase.getInstance().getReference("Home");
        databaseHistory = databaseUser.child(userId).child("historyPostList");

        recyclerView = findViewById(R.id.history_recyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(HistoryActivity.this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        historyList = new ArrayList<>();

        history_navigation = (BottomNavigationView)findViewById(R.id.history_navigation);

        history_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.nav_home:
                        Intent i = new Intent(getApplicationContext(), MainActivity.class );
                        startActivity(i);
                        break;
                    case R.id.nav_photo:
                        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            Intent intent = new Intent(HistoryActivity.this, TakePicActivity.class);
                            startActivity(intent);
                        }
                        else{
                            getPermissionToCamera();
                        }
                        break;
                    case R.id.nav_profile:
                        Intent intent = new Intent(getApplicationContext(), MeActivity.class );
                        startActivity(intent);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        populateRecyclerView();
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
                        ActivityCompat.requestPermissions(HistoryActivity.this,
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

    private void populateRecyclerView() {
        //search user info
        databaseHistory.orderByChild("postId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                historyList.clear();
                for(DataSnapshot child : dataSnapshot.getChildren()){
                    Post post = child.getValue(Post.class);
                    historyList.add(post);
                }
                adapter = new HistoryAdapter(getApplicationContext(), historyList);
                recyclerView.setAdapter(adapter);

                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                        Post post = historyList.get(viewHolder.getAdapterPosition());
                        historyList.remove(viewHolder.getAdapterPosition());
                        adapter.notifyDataSetChanged();
                        deletePost(post.getPostId());
                    }
                }).attachToRecyclerView(recyclerView);

                adapter.setOnItemClickListener(new HistoryAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        Post post = historyList.get(position);
                        // 2 ways pass data
                        // 1st read post's data from database here, pass the post object and start postDisplay activity
                        Intent intent = new Intent(getApplicationContext(), HistoryPostDisplayActivity.class);
                        // 2nd pass only postId and read the post's data from database at postDisplayActivity
                        intent.putExtra("postId", post.getPostId());
                        intent.putExtra("userId", userId);

                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deletePost(String postId){
        databaseHistory.child(postId).removeValue();
        databaseHome.child(postId).removeValue();
        Toast.makeText(this, "Post has been removed.", Toast.LENGTH_LONG);
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
                    Intent intent = new Intent(HistoryActivity.this,TakePicActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Read Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_history_back:
                //jump to meactivity
                Intent i = new Intent(getApplicationContext(), MeActivity.class);
                startActivity(i);
                break;
        }
    }
}
