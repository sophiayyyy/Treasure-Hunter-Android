package com.example.lt.treasurehunter;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.midi.MidiDeviceService;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceIdReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BottomNavigationView home_navigation;
    private ImageButton searchBtn;
    private EditText searchEdt;

    private List<Post> postList = new ArrayList<>();
    private List<Post> postSearchList = new ArrayList<>();
    PostAdapter adapter;

    GlobalClass globalClass;
    private Boolean isLogIn; //true: already login, false: haven't login
    private int signInType;//0: google, 1: treasure hunter, 2:ins, -1: haven't login
    private String userId;
    private boolean flag = true;
    private int curPosition = 0;


    //firebase
    DatabaseReference databaseUser;
    DatabaseReference databaseHome;
    DatabaseReference databaseFavorite;

    //Identifier for the permission request
    private static final int CAMERA_PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseUser = FirebaseDatabase.getInstance().getReference("User");
        databaseHome = FirebaseDatabase.getInstance().getReference("Home");


        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);

        searchBtn = (ImageButton)findViewById(R.id.btn_home_search);
        searchEdt = (EditText) findViewById(R.id.edt_home_search);

        globalClass = (GlobalClass) getApplicationContext();
        isLogIn = globalClass.getLogIn();
        userId = globalClass.getUserId();
        signInType = globalClass.getLogInType();
        if(isLogIn){
            databaseFavorite = databaseUser.child(userId).child("favoritePostList");
        }


        home_navigation = (BottomNavigationView)findViewById(R.id.home_navigation);

        home_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.nav_home:
                        break;
                    case R.id.nav_photo:
                        if(isLogIn){
                            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                Intent intent = new Intent(MainActivity.this, TakePicActivity.class);
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
                        if(isLogIn){
                            Intent i = new Intent(getApplicationContext(), MeActivity.class );
                            startActivity(i);
                        }else {
                            Intent i = new Intent(getApplicationContext(), SignInActivity.class );
                            startActivity(i);
                        }
                        break;
                }
                return false;
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLogIn){
                    postSearchList.clear();
                    searchResult();
                    recyclerView.scrollToPosition(0);
                }else{
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        populateRecyclerView();
    }

    private void populateRecyclerView() {
        databaseHome.orderByChild("postId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot child : dataSnapshot.getChildren()){
                    Post post = child.getValue(Post.class);
                    postList.add(post);
                }
                adapter = new PostAdapter(getApplicationContext(), postList);
                recyclerView.setAdapter(adapter);
                adapter.setOnItemClickListener(new PostAdapter.OnRecyclerViewItemClickListener() {
                    @Override
                    public void onClick(View view, PostAdapter.ViewName viewName, int position) {
                        if (viewName.equals(PostAdapter.ViewName.ITEM)){
                            Toast.makeText(MainActivity.this,"item clicked",Toast.LENGTH_SHORT).show();
                            Post post = postList.get(position);
                            // 2 ways pass data
                            // 1st read post's data from database here, pass the post object and start postDisplay activity
                            Intent intent = new Intent(getApplicationContext(), HomePostDisplayActivity.class);
                            // 2nd pass only postId and read the post's data from database at postDisplayActivity
                            intent.putExtra("postId", post.getPostId());
                            startActivity(intent);
                            finish();


                        }
                    }
                });

                adapter.setOnItemCheckedListener(new PostAdapter.OnRecyclerViewItemCheckedListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, PostAdapter.ViewName viewName, boolean isChecked, int position) {

                        if(isLogIn){
                            curPosition = position;
                            if(viewName.equals(PostAdapter.ViewName.LIKE)){

                                    buttonView.setEnabled(false);
                                    final Post post = postList.get(position);
                                    final String postId = post.getPostId();

                                    DatabaseReference currentRefFavorite = FirebaseDatabase.getInstance()
                                            .getReference().child("User").child(userId).child("favoritePostList");
                                    ValueEventListener eventListenerFavorite = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            boolean added = false;
                                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                String pid = (String) ds.getKey();
                                                if(pid.equals(postId)){

                                                    Toast.makeText(MainActivity.this,"unlike!",Toast.LENGTH_SHORT).show();
                                                    ValueEventListener eventListenerHome = new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                                String pid = (String) ds.getKey();
                                                                if (pid.equals(postId)) {
                                                                    // user click thumb, set the value as like-1
                                                                    int like = ds.child("like").getValue(int.class);
                                                                    like = like - 1;
                                                                    databaseHome.child(postId).child("like").setValue(like);
                                                                    databaseUser.child(userId).child("favoritePostList").child(postId).removeValue();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            throw databaseError.toException();
                                                        }
                                                    };
                                                    databaseHome.addListenerForSingleValueEvent(eventListenerHome);
                                                    added = true;
                                                }
                                            }
                                            if(added == false){
                                                Toast.makeText(MainActivity.this,"like!",Toast.LENGTH_SHORT).show();
                                                DatabaseReference currentRefHome = FirebaseDatabase.getInstance()
                                                        .getReference().child("Home");
                                                ValueEventListener eventListenerHome = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                                            String pid = (String) ds.getKey();
                                                            if (pid.equals(postId)) {
                                                                int like = ds.child("like").getValue(int.class);

                                                                //user click the thumb first time, thus like+1
                                                                like = like + 1;
                                                                databaseHome.child(postId).child("like").setValue(like);
                                                                databaseUser.child(userId).child("favoritePostList").child(postId).setValue(post);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        throw databaseError.toException();
                                                    }
                                                };
                                                currentRefHome.addListenerForSingleValueEvent(eventListenerHome);

                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            throw databaseError.toException();
                                        }
                                    };
                                    currentRefFavorite.addListenerForSingleValueEvent(eventListenerFavorite);
                                    buttonView.setEnabled(true);
                            }
                        }else{
                            Toast.makeText(MainActivity.this,"Please log in first",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                recyclerView.scrollToPosition(curPosition);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void searchResult(){
        //Obtain input keywords
        String postTag = searchEdt.getText().toString();
        //If user doesn't enter any keywords, make toast as a reminder
        if(postTag.equals("")){
            Toast.makeText(this, "Please input keywords.", Toast.LENGTH_SHORT).show();
        } else {
            final ArrayList<String> postTagList = new ArrayList<String>();
            for (String tempPost : postTag.split(" ")) {
                postTagList.add(tempPost);
            }
            final int tagListSize = postTagList.size();

            //count the number of keywords contained by each post
            DatabaseReference currentRefHome = FirebaseDatabase.getInstance()
                    .getReference().child("Home");
            ValueEventListener eventListenerHome = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<Integer> tagNumList = new ArrayList<Integer>();
                    ArrayList<String> searchPostIdList = new ArrayList<String>();

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        String pid = (String) ds.getKey();
                        Post post = ds.getValue(Post.class);
                        String tag = ds.child("tag").getValue(String.class);

                        Integer count = 0;
                        for (int i = 0; i < postTagList.size(); i++) {
                            if (tag.toLowerCase().contains(postTagList.get(i).toLowerCase())) {
                                count += 1;
                            }
                        }
                        tagNumList.add(count);
                        postSearchList.add(post);
                        searchPostIdList.add(pid);

                    }
                    // sort posts by number of tags contained
                    for (int i = 0; i < tagNumList.size() - 1; i++) {
                        for (int j = 0; j < tagNumList.size() - i - 1; j++) {
                            if (tagNumList.get(j) < tagNumList.get(j + 1)) {
                                Integer tempTagNum = tagNumList.get(j);
                                tagNumList.set(j, tagNumList.get(j + 1));
                                tagNumList.set(j + 1, tempTagNum);
                                Post tempPost = postSearchList.get(j);
                                postSearchList.set(j, postSearchList.get(j + 1));
                                postSearchList.set(j + 1, tempPost);
                                String tempPostId = searchPostIdList.get(j);
                                searchPostIdList.set(j, searchPostIdList.get(j + 1));
                                searchPostIdList.set(j + 1, tempPostId);
                            }
                        }
                    }

                    for (int i = 0; i < tagNumList.size() - 1; i++) {
                        Log.d("search", String.valueOf(tagNumList.get(i)));
                        Log.d("search", searchPostIdList.get(i));
                    }

                    Log.d("search main size ",String.valueOf(postSearchList));
                    //update with new order of posts
                    adapter.updateItems(postSearchList);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    throw databaseError.toException();
                }
            };
            currentRefHome.addListenerForSingleValueEvent(eventListenerHome);
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
                        ActivityCompat.requestPermissions(MainActivity.this,
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
                    Intent intent = new Intent(MainActivity.this,TakePicActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Read Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
