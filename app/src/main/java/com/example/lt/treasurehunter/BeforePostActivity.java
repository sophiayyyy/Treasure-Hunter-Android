package com.example.lt.treasurehunter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.lang.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BeforePostActivity extends AppCompatActivity {
    TextView BStxt,noResultTextView;
    EditText tagInput;
    Button loadmoreBtn,selectBestBtn,postBtn;
    ImageButton editBtn, fetchBtn, shareBtn;
    ProgressBar progressBar;
    ListView itemListView,bestOptionLV;
    ImageView postImageView;
    AwesomeAdapter itemAdapter,bestOptionAdapter;
    HashMap<String, String> bestOption = new HashMap<String, String>();
    String labels;
    Integer responseCode = null;
    String responseMessage = "";
    String imageBase64;
    Bitmap postImage;
    Uri shareImage;
    static String result = null;
    final static String TAG = "SearchTest";
    final static String Apikey="dfff1bc9bdfe6225c5d62f45acf9ffdf774420a1641434b016f989063ee268a5";//Api Key $50/month with 5000 searches
    private ArrayList<HashMap<String, String>>  itemList = new ArrayList<HashMap<String, String>>();
    private ArrayList<HashMap<String, String>>  displayList = new ArrayList<HashMap<String, String>>();
    private int itemCount = 0;
    int imgID;
    int like;
    boolean focusable = false;

    GlobalClass globalClass;

    private int signInType;//0: general, 1: google, 2:ins, -1: haven't login
    //Identifier for the permission request
    private static final int WRITE_STORAGE_PERMISSIONS_REQUEST = 3;

    //firebase Reference
    DatabaseReference databaseUser;
    DatabaseReference databaseHome;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_before_post);


        Intent intent = getIntent();
        final String tags = intent.getStringExtra("labels");
        Log.i("labels",tags);
        //get image from cropImage activity
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(this);
        imageBase64 = shre.getString("recImage", "");
        postImage = stringToBitMap(imageBase64);
        Bitmap roundPostImage = roundImageHelper.getRoundedCornerBitmap(postImage,12);  //pixels number need to associated with the image taken size

        //initialization
        postImageView = (ImageView)findViewById(R.id.historyImageView);
        postImageView.setImageBitmap(roundPostImage);

        tagInput = (EditText)findViewById(R.id.tagInput);
        tagInput.setText(tags);

        BStxt = (TextView)findViewById(R.id.history_BStxt);
        BStxt.setVisibility(View.GONE);

        selectBestBtn = (Button)findViewById(R.id.selectBestBtn);
        selectBestBtn.setVisibility(View.GONE);

        postBtn = (Button)findViewById(R.id.postBtn);
        postBtn.setVisibility(View.GONE);

        shareBtn = (ImageButton)findViewById(R.id.shareBtn);

        bestOptionLV = (ListView) findViewById(R.id.historybestOptionLV);
        bestOptionLV.setVisibility(View.GONE);

        editBtn= (ImageButton)findViewById(R.id.editBtn);
        progressBar = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        loadmoreBtn =(Button)findViewById(R.id.loadmoreBtn);
        loadmoreBtn.setVisibility(View.GONE);

        fetchBtn = (ImageButton)findViewById(R.id.fetchBtn);

        noResultTextView = (TextView)findViewById(R.id.noResultTextView);
        noResultTextView.setVisibility(View.GONE);

        //initialize itemListView
        itemListView =(ListView)findViewById(R.id.itemListView);
        itemAdapter = new AwesomeAdapter(getBaseContext(),displayList,0);
        itemListView.setAdapter(itemAdapter);

        //search shopping list based on labels
        shoppingListSearch();

        globalClass = (GlobalClass) getApplicationContext();
        signInType = globalClass.getLogInType();

        databaseUser = FirebaseDatabase.getInstance().getReference("User");
        databaseHome = FirebaseDatabase.getInstance().getReference("Home");

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                postBtn.setClickable(false);
                postBtn.setFocusable(false);
                postBtn.setText("Posting...");
                setProgressDialog();
                String postId,price,source,title,link,thumbnail;

                price = bestOption.get("price");
                source = bestOption.get("source");
                title = bestOption.get("title");
                link = bestOption.get("link");
                thumbnail = bestOption.get("thumbnail");
                String tag = String.valueOf(tagInput.getText());
                Post post = new Post("",tag, imgID, like,imageBase64,price,source,title,link,thumbnail);
                //search user info
                //add new post to historyPostList
                addPostToHistoryList(post);
                progressDialog.dismiss();
            }
        });
        loadmoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                if(itemCount+3 >= itemList.size()){
                    loadmoreBtn.setVisibility(View.GONE);
                    displayList.clear();
                    displayList.addAll(itemList);
                    itemAdapter.updateAdapter();
                }else {
                    itemCount += 3;
                    Toast.makeText(getApplicationContext(),String.valueOf(itemCount),Toast.LENGTH_SHORT).show();
                    displayList.clear();
                    displayList.addAll(itemList.subList(0, itemCount));
                    itemAdapter.updateAdapter();
                }
            }
        });
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(focusable) {
                    disableEditText(tagInput);
                    focusable =false;
                    fetchBtn.setVisibility(View.VISIBLE);
                }else {
                    enableEditText(tagInput);
                    focusable = true;
                    fetchBtn.setVisibility(View.GONE);
                }
            }
        });
        fetchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchBtn.setEnabled(false);
                noResultTextView.setVisibility(View.GONE);
                shoppingListSearch();
                itemAdapter.updateAdapter();
                fetchBtn.setEnabled(true);
            }
        });

        //INS share
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    shareImage = getImageUri(getApplicationContext(), postImage);
                    Intent intent=new Intent(Intent.ACTION_SEND);
                    // Add the URI to the Intent
                    intent.putExtra(Intent.EXTRA_STREAM, shareImage);

                    // Set the MIME type
                    intent.setType("image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Broadcast the Intent
                    startActivity(Intent.createChooser(intent, getTitle()));
                }
                else{
                    getPermissionToWriteStorage();
                }

            }
        });

    }
    //0: general, 1: google, 2:ins, -1: haven't login
    private void addPostToHistoryList(final Post post){
        switch (signInType){
            case 0:
                final String emailUser = globalClass.getEmail();
                databaseUser.orderByChild("email").equalTo(emailUser)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String userId = "";
                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                    userId = child.getKey();
                                    String postId = databaseUser.child(userId).child("historyPostList").push().getKey();
                                    post.setPostId(postId);
                                    databaseUser.child(userId).child("historyPostList").child(postId).setValue(post);
                                    databaseHome.child(postId).setValue(post);
                                }
                                Intent intent = new Intent(getApplicationContext(), PostDisplayActivity.class);
                                //pass only postId and read the post's data from database at postDisplayActivity
                                intent.putExtra("postId", post.getPostId());
                                intent.putExtra("userId", userId);
                                postBtn.setText("Post");
                                postBtn.setClickable(true);
                                postBtn.setFocusable(true);

                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                break;
            case 1:
                final String emailGoogle = globalClass.getEmail();
                databaseUser.orderByChild("email").equalTo(emailGoogle)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String userId = "";
                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                    userId = child.getKey();
                                    String postId = databaseUser.child(userId).child("historyPostList").push().getKey();
                                    post.setPostId(postId);
                                    databaseUser.child(userId).child("historyPostList").child(postId).setValue(post);
                                    databaseHome.child(postId).setValue(post);
                                }
                                Intent intent = new Intent(getApplicationContext(), PostDisplayActivity.class);
                                //pass only postId and read the post's data from database at postDisplayActivity
                                intent.putExtra("postId", post.getPostId());
                                intent.putExtra("userId", userId);
                                postBtn.setText("Post");
                                postBtn.setClickable(true);
                                postBtn.setFocusable(true);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                break;
            case 2:
                final String insName = globalClass.getInsname();
                databaseUser.orderByChild("insName").equalTo(insName)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String userId = "";
                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                    userId = child.getKey();
                                    String postId = databaseUser.child(userId).child("historyPostList").push().getKey();
                                    post.setPostId(postId);
                                    databaseUser.child(userId).child("historyPostList").child(postId).setValue(post);
                                    databaseHome.child(postId).setValue(post);
                                }
                                Intent intent = new Intent(getApplicationContext(), PostDisplayActivity.class);
                                // pass only postId and read the post's data from database at postDisplayActivity
                                intent.putExtra("postId", post.getPostId());
                                intent.putExtra("userId", userId);
                                postBtn.setClickable(true);
                                postBtn.setFocusable(true);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                break;
        }
    }
    //API reference: https://serpapi.com/
    private void shoppingListSearch(){
        labels = tagInput.getText().toString();
        Log.d(TAG, "Search for: " + labels);
        String searchString = labels.replace(" ", "+");
        Log.d(TAG, "searchString: " + searchString);
        //URL format: https://serpapi.com/search.json?q=Coffee&hl=en&gl=us&tbm=shop&num=10&api_key=dfff1bc9bdfe6225c5d62f45acf9ffdf774420a1641434b016f989063ee268a5
        String urlString = "https://serpapi.com/search.json?q=" + searchString +"&hl=en&gl=us&tbm=shop&num=10" + "&api_key=" + Apikey;
        //Below is test url
        //String urlString = "https://serpapi.com/search.json?q=Coffee&location=Austin%2C+Texas%2C+United+States&hl=en&gl=us";
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(TAG, "ERROR converting String to URL " + e.toString());
        }
        Log.d(TAG, "Url = "+  urlString);
        // start AsyncTask
        SerpSearchAsyncTask searchTask = new SerpSearchAsyncTask();
        searchTask.execute(url);
    }

    private class SerpSearchAsyncTask extends AsyncTask<URL, Integer, String>{
        protected void onPreExecute(){
            Log.d(TAG, "AsyncTask - onPreExecute");
            // show progressbar
            progressBar.setVisibility(View.VISIBLE);
            loadmoreBtn.setVisibility(View.GONE);
            selectBestBtn.setVisibility(View.GONE);
            itemListView.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(URL... urls) {
            URL url = urls[0];
            Log.d(TAG, "AsyncTask - doInBackground, url=" + url);

            // Http connection
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                Log.e(TAG, "Http connection ERROR " + e.toString());
            }
            try {
                responseCode = conn.getResponseCode();
                responseMessage = conn.getResponseMessage();
            } catch (IOException e) {
                Log.e(TAG, "Http getting response code ERROR " + e.toString());
            }

            Log.d(TAG, "Http response code =" + responseCode + " message=" + responseMessage);
            try {

                if(responseCode == 200) {
                    // response OK
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = rd.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    rd.close();
                    conn.disconnect();
                    result = sb.toString();
                    Log.d(TAG, "result=" + result);
                    return result;
                }else{
                    // response problem
                    String errorMsg = "Http ERROR response " + responseMessage + "\n";
                    Log.e(TAG, errorMsg);
                    result = errorMsg;
                    return  result;

                }
            } catch (IOException e) {
                Log.e(TAG, "Http Response ERROR " + e.toString());
            }
            return null;
        }
        protected void onProgressUpdate(Integer... progress) {
            Log.d(TAG, "AsyncTask - onProgressUpdate, progress=" + progress);

        }

        protected void onPostExecute(String result) {
            Log.d(TAG, "AsyncTask - onPostExecute, result=" + result);
            // hide progressbar
            progressBar.setVisibility(View.GONE);

            try {
                JSONObject obj = new JSONObject(result);
                JSONArray m_jArry = obj.getJSONArray("shopping_results");
                HashMap<String, String> item;
                itemList.clear();
                displayList.clear();
                for (int i = 0; i < m_jArry.length(); i++) {
                    JSONObject jo_inside = m_jArry.getJSONObject(i);
                    String title_value = jo_inside.optString("title");
                    String link_value = jo_inside.optString("link");
                    String source_value = jo_inside.optString("source");
                    String price_value = jo_inside.optString("price");
                    String thumbnail_value = jo_inside.optString("thumbnail");

                    //Add elements in each shopping item:
                    item = new HashMap<String, String>();
                    item.put("title", title_value);
                    item.put("link", link_value);
                    item.put("source", source_value);
                    item.put("price", price_value);
                    item.put("thumbnail", thumbnail_value);

                    itemList.add(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // initial set adapter and listView
            if(itemList.size()>=3) {
                Toast.makeText(getApplicationContext(),"itemList size: " + String.valueOf(itemList.size()),Toast.LENGTH_SHORT).show();
                itemCount = 3;
                displayList.addAll(itemList.subList(0, itemCount));
                loadmoreBtn.setVisibility(View.VISIBLE);
                selectBestBtn.setVisibility(View.VISIBLE);
                itemListView.setVisibility(View.VISIBLE);
            }else if(itemList.size()==0) {
                selectBestBtn.setVisibility(View.GONE);
                noResultTextView.setVisibility(View.VISIBLE);
            }
            else{
                displayList = itemList;
                selectBestBtn.setVisibility(View.VISIBLE);
                itemListView.setVisibility(View.VISIBLE);
            }

            selectBestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int SelectedPosition = itemAdapter.getSelectedPosition();
                    Toast.makeText(getApplicationContext(),Integer.toString(SelectedPosition),Toast.LENGTH_SHORT).show();
                    if(SelectedPosition >=0) {
                        bestOption.put("title", displayList.get(SelectedPosition).get("title"));
                        bestOption.put("link", displayList.get(SelectedPosition).get("link"));
                        bestOption.put("source", displayList.get(SelectedPosition).get("source"));
                        bestOption.put("price", displayList.get(SelectedPosition).get("price"));
                        bestOption.put("thumbnail", displayList.get(SelectedPosition).get("thumbnail"));
                        final ArrayList<HashMap<String, String>> bestOptionList = new ArrayList<HashMap<String, String>>();
                        bestOptionList.clear();
                        bestOptionList.add(bestOption);
                        bestOptionAdapter = new AwesomeAdapter(getBaseContext(), bestOptionList,1);
                        bestOptionLV.setAdapter(bestOptionAdapter);

                        postBtn.setVisibility(View.VISIBLE);
                        BStxt.setVisibility(View.VISIBLE);
                        bestOptionLV.setVisibility(View.VISIBLE);
                    }
                }
            });

        }
    }

    public Bitmap stringToBitMap(String encodedString){
        try {
            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void setProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating the post...");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(progressDialog.STYLE_SPINNER);
        progressDialog.show();
    }
    //response animation when button being clicked
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.8F);

    private void disableEditText(EditText editText) {
        editText.setFocusableInTouchMode(false);
        editText.setEnabled(false);
        editText.setCursorVisible(false);
    }
    private void enableEditText(EditText editText) {
        editText.setEnabled(true);
        editText.setFocusableInTouchMode(true);
        editText.setCursorVisible(true);
    }

    public void getPermissionToWriteStorage() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Display a popup message or equivalent.  Explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Storage is Closed").setMessage("Give Treasure Hunter access to your storage in order to share the image on Instagram.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(BeforePostActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_STORAGE_PERMISSIONS_REQUEST);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }else {

                // Fire off an async request to actually get the permission
                // This will show the standard permission request dialog UI
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE_PERMISSIONS_REQUEST);
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
            case WRITE_STORAGE_PERMISSIONS_REQUEST:{
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Write Storage permission granted", Toast.LENGTH_SHORT).show();

                    shareImage = getImageUri(getApplicationContext(), postImage);
                    Intent intent=new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, shareImage);

                    // Set the MIME type
                    intent.setType("image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Broadcast the Intent
                    startActivity(Intent.createChooser(intent, getTitle()));
                } else {
                    Toast.makeText(this, "Write Storage permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }
}
