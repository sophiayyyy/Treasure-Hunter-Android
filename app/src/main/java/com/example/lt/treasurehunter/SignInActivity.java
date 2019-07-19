package com.example.lt.treasurehunter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lt.treasurehunter.CustomerViews.AuthenticationDialog;
import com.example.lt.treasurehunter.interfaces.AuthenticationListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, AuthenticationListener {

    private SignInButton login_google_login;
    private Button btn_instagram_login;
    private TextView login_no_account;
    private Button btn_login;

    private EditText edt_login_email;
    private EditText edt_login_pwd;

    private static final int REQ_CODE = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 2;
    private String cur_name;
    private String cur_email;
    private String profileBase64 = "default";//default means no profile photo upload
    Bitmap bmp;
    private boolean isProgressDialog = false;

    //firebase
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    DatabaseReference databaseUser;

    //Instagram login part
    private AuthenticationDialog auth_dialog;
    String token = null;

    //signInType: 0 refers to general user, 1 refers to google user, 2 refers to ins user
    private int signInType;
    GlobalClass globalClass;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        login_google_login = (SignInButton)findViewById(R.id.login_google_login);
        btn_instagram_login = (Button) findViewById(R.id.btn_instagram_login);
        login_no_account = (TextView)findViewById(R.id.login_no_account);
        btn_login = (Button)findViewById(R.id.btn_login);

        edt_login_email = (EditText)findViewById(R.id.edt_login_email);
        edt_login_pwd = (EditText)findViewById(R.id.edt_login_pwd);

        btn_login.setOnClickListener(this);//user
        login_google_login.setOnClickListener(this);//google user
        btn_instagram_login.setOnClickListener(this);//ins user

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by signInOptions.
        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        //Init Firebase
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        databaseUser = FirebaseDatabase.getInstance().getReference("User");
        globalClass = (GlobalClass) getApplicationContext();
    }

    @Override
    public void onBackPressed(){
        Intent i = new Intent(this, MainActivity.class);
        if(isProgressDialog) {
            progressDialog.dismiss();
            isProgressDialog =false;
        }
        startActivity(i);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_login:
                signInType = 0;
                setProgressDialog();
                UserSignIn();
                break;
            case R.id.login_google_login:
                signInType = 1;
                setProgressDialog();
                GoogleSignIn();
                break;
            case R.id.btn_instagram_login:
                signInType = 2;
                //setProgressDialog();
                InsSignIn();//after_click_login();
                break;
            case R.id.login_no_account:
                Intent i = new Intent(getApplicationContext(), SignUpActivity.class );
                startActivity(i);
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
        isProgressDialog = true;
    }

    private void UserSignIn(){
        //get user input of email and pwd
        final String useremail = edt_login_email.getText().toString();
        final String pwd = edt_login_pwd.getText().toString();
        //if it's empty make a toast
        if(useremail.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "User doesn't exist or wrong password.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
        else {
            //search in the database
            String search = useremail + "_" + pwd;
            //search by email_pwd attributes
            databaseUser.orderByChild("email_pwd").equalTo(search)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            //if this user exist
                            if(dataSnapshot.exists()){
                                cur_email = useremail;
                                //save global variable
                                //jump to MeActivity
                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                    globalClass.setUserId(child.child("userId").getValue(String.class));
                                    globalClass.setUserName(child.child("userName").getValue(String.class));
                                    globalClass.setProfileBase64(child.child("profileBase64").getValue(String.class));
                                }
                                globalClass.setLogInType(0);
                                globalClass.setLogIn(true);
                                globalClass.setEmail(cur_email);
                                Intent i = new Intent(getApplicationContext(), MeActivity.class );
                                startActivity(i);
                            }else {
                                Toast.makeText(getApplicationContext(), "User doesn't exist or wrong password.", Toast.LENGTH_SHORT).show();
                            }
                            if(isProgressDialog) {
                                progressDialog.dismiss();
                                isProgressDialog =false;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }
    }

    private void GoogleSignIn(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if(requestCode == REQ_CODE){
            System.out.println("requestCode == REQ_CODE");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleResult(task);
        }
    }

    private void handleResult(Task<GoogleSignInAccount> completedTask){
        try {
            //get basic info from google account(email, username, profile photo)
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            final String name = account.getDisplayName();
            Log.d("signin",  name);
            final String email = account.getEmail();
            Log.d("signin",  email);
            cur_name = name;
            cur_email = email;
            if(account.getPhotoUrl() == null) {
                profileBase64 = "default";
                Log.d("signin", "default");
                globalClass.setProfileBase64(profileBase64);

                databaseUser.orderByChild("email").equalTo(cur_email)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    //user already in our database
                                    System.out.println("user already exist");
                                    //save global variable
                                    //jump to MeActivity
                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        globalClass.setUserId(child.child("userId").getValue(String.class));
                                        globalClass.setUserName(child.child("userName").getValue(String.class));
                                        globalClass.setProfileBase64(child.child("profileBase64").getValue(String.class));
                                    }
                                    globalClass.setLogInType(1);
                                    globalClass.setLogIn(true);
                                    globalClass.setEmail(cur_email);
                                    Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                    startActivity(i);
                                } else {
                                    //if no create user account for this google user
                                    String userId = databaseUser.push().getKey();

                                    User user = new User(userId, cur_name, cur_email, "", cur_email + "_", "", profileBase64, new ArrayList<Post>(), new ArrayList<Post>());
                                    databaseUser.child(userId).setValue(user);
                                    System.out.println("new user added");
                                    //jump to MeActivity
                                    globalClass.setUserId(userId);
                                    globalClass.setLogInType(1);
                                    globalClass.setLogIn(true);
                                    globalClass.setEmail(cur_email);
                                    globalClass.setUserName(cur_name);
                                    globalClass.setProfileBase64(profileBase64);
                                    Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                    startActivity(i);
                                }
                                if(isProgressDialog) {
                                    progressDialog.dismiss();
                                    isProgressDialog =false;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
            }
            else {

                String url  = account.getPhotoUrl().toString();
                Log.d("signin",  url);
                new GoogleDownloadImageTask(profileBase64).execute(url);
            }

            System.out.println("handleResult finished");
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
            if(isProgressDialog) {
                progressDialog.dismiss();
                isProgressDialog =false;
            }
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(isProgressDialog) {
            progressDialog.dismiss();
            isProgressDialog =false;
        }
    }

    //for the ins token
    @Override
    public void onCodeReceived(String auth_token) {
        if(auth_token == null){
            Log.e("onCodeReceive","token null");
            if(isProgressDialog) {
                progressDialog.dismiss();
                isProgressDialog =false;
            }
            return;
        }
        //use the token for further
        //save the token in sharedPreference
        token = auth_token;
        setProgressDialog();
        getUserInfoByAccessToken(token);

    }

    private void getUserInfoByAccessToken(String token) {
        new RequestInstagramAPI().execute();
    }
    private void InsSignIn(){
        auth_dialog = new AuthenticationDialog(this, this);
        auth_dialog.setCancelable(true);
        auth_dialog.show();
    }
    private class RequestInstagramAPI extends AsyncTask<Void,String,String> {

        @Override
        protected String doInBackground(Void... voids) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(Constants.GET_USER_INFO_URL+token);
            token = null;
            try{
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity httpEntity = response.getEntity();
                String json = EntityUtils.toString(httpEntity);

                return json;

            }catch(ClientProtocolException e){
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if(response!=null){
                try{
                    JSONObject json = new JSONObject(response);
                    Log.e("response",json.toString());
                    //we need the user id
                    JSONObject jsonData = json.getJSONObject("data");
                    if(jsonData.has("id")){
                        String id = jsonData.getString("id");

                        //we can use the other data, profile pic
                        final String user_name = jsonData.getString("username");
                        cur_name = user_name;
                        final String profile_picUrl = jsonData.getString("profile_picture");
                        if (profile_picUrl!=null) {
                            new InsDownloadImageTask(profileBase64).execute(profile_picUrl);
                        } else{
                            profileBase64 = "default";
                        }
                    }
                }catch(JSONException e){
                    e.printStackTrace();

                }
            }
        }
    }


    private class InsDownloadImageTask extends AsyncTask<String, Void, String> {
        String profilebase64;

        public InsDownloadImageTask(String profilebase64) {
            this.profilebase64 = profilebase64;
        }

        protected String doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            String ImageBase64 = BitMapToString(mIcon11);
            return ImageBase64;
        }

        protected void onPostExecute(String result) {
            final String InsProfileBase64 = result;
            globalClass.setProfileBase64(InsProfileBase64);

            databaseUser.orderByChild("insName").equalTo(cur_name).
                    addListenerForSingleValueEvent(new ValueEventListener(){
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                //user already in our database
                                System.out.println("user already exist");
                                //save global variable
                                //jump to MeActivity
                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                    globalClass.setUserId(child.child("userId").getValue(String.class));
                                    globalClass.setUserName(child.child("userName").getValue(String.class));
                                    globalClass.setEmail(child.child("email").getValue(String.class));
                                    globalClass.setProfileBase64(child.child("profileBase64").getValue(String.class));
                                }
                                globalClass.setLogInType(2);
                                globalClass.setLogIn(true);
                                globalClass.setInsname(cur_name);
                                Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                startActivity(i);
                            } else {
                                //if no create user account for this user
                                String userId = databaseUser.push().getKey();
                                User user = new User(userId, cur_name, "", "",""+"_", cur_name, InsProfileBase64,new ArrayList<Post>(), new ArrayList<Post>());
                                databaseUser.child(userId).setValue(user);
                                System.out.println("new user added");
                                //jump to MeActivity
                                globalClass.setUserId(userId);
                                globalClass.setLogInType(2);
                                globalClass.setLogIn(true);
                                globalClass.setInsname(cur_name);
                                globalClass.setUserName(cur_name);
                                globalClass.setProfileBase64(InsProfileBase64);
                                Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                startActivity(i);
                            }
                            if(isProgressDialog) {
                                progressDialog.dismiss();
                                isProgressDialog =false;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }
    }

    private class GoogleDownloadImageTask extends AsyncTask<String, Void, String> {
        String profilebase64;

        public GoogleDownloadImageTask(String profilebase64) {
            this.profilebase64 = profilebase64;
        }

        protected String doInBackground(String... urls) {
            String urldisplay = urls[0];
            Log.d("signin",  urldisplay);
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            String ImageBase64 = BitMapToString(mIcon11);
            return ImageBase64;
        }

        protected void onPostExecute(String result) {
            final String InsProfileBase64 = result;
            globalClass.setProfileBase64(InsProfileBase64);

            databaseUser.orderByChild("email").equalTo(cur_email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                //user already in our database
                                System.out.println("user already exist");
                                //save global variable
                                //jump to MeActivity
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    globalClass.setUserId(child.child("userId").getValue(String.class));
                                    globalClass.setUserName(child.child("userName").getValue(String.class));
                                    globalClass.setProfileBase64(child.child("profileBase64").getValue(String.class));
                                }
                                globalClass.setLogInType(1);
                                globalClass.setLogIn(true);
                                globalClass.setEmail(cur_email);
                                Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                startActivity(i);
                            } else {
                                //if no create user account for this google user
                                String userId = databaseUser.push().getKey();

                                User user = new User(userId, cur_name, cur_email, "", cur_email + "_", "", InsProfileBase64, new ArrayList<Post>(), new ArrayList<Post>());
                                databaseUser.child(userId).setValue(user);
                                System.out.println("new user added");
                                //jump to MeActivity
                                globalClass.setUserId(userId);
                                globalClass.setLogInType(1);
                                globalClass.setLogIn(true);
                                globalClass.setEmail(cur_email);
                                globalClass.setUserName(cur_name);
                                globalClass.setProfileBase64(InsProfileBase64);
                                Intent i = new Intent(getApplicationContext(), MeActivity.class);
                                startActivity(i);
                            }
                            if(isProgressDialog) {
                                progressDialog.dismiss();
                                isProgressDialog =false;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    //from https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp= Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
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
