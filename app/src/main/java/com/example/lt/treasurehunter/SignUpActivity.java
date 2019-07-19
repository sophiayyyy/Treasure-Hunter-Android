package com.example.lt.treasurehunter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText edt_create_username;
    private EditText edt_create_email;
    private EditText edt_create_password;
    private EditText edt_create_password_again;
    private TextView sign_up_have_account;
    private Button btn_sign_up;

    private String username;
    private String email;
    private String password;
    private String password2;
    DatabaseReference databaseUser;

    ProgressDialog progressDialog;

    private CheckEmail emailChecker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edt_create_username = (EditText)findViewById(R.id.edt_create_username);
        edt_create_email = (EditText)findViewById(R.id.edt_create_email);
        edt_create_password = (EditText)findViewById(R.id.edt_create_password);
        edt_create_password_again = (EditText)findViewById(R.id.edt_create_password_again);
        sign_up_have_account = (TextView)findViewById(R.id.sign_up_have_account);
        btn_sign_up = (Button)findViewById(R.id.btn_sign_up);

        databaseUser = FirebaseDatabase.getInstance().getReference("User");

        btn_sign_up.setOnClickListener(this);
        sign_up_have_account.setOnClickListener(this);

        emailChecker = new CheckEmail(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_sign_up:
                //progressing part
                setProgressDialog();
                UserSignUp();
                break;
            case R.id.sign_up_have_account:
                Intent i = new Intent(getApplicationContext(), SignInActivity.class );
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
    }

    public void UserSignUp(){
        //get text
        username = edt_create_username.getText().toString();
        email = edt_create_email.getText().toString();
        password = edt_create_password.getText().toString();
        password2 = edt_create_password_again.getText().toString();

        emailChecker.setEmail(email);

        //check: 1.each input cannot be empty
        //       2.the two password must be same
        if(TextUtils.isEmpty(username)) {
            edt_create_username.setError("Username can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(TextUtils.isEmpty(email)) {
            edt_create_email.setError("Email can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(!emailChecker.checkValid()){
            edt_create_email.setError("Invaild Email.");
            progressDialog.dismiss();
            return;
        }
        if(TextUtils.isEmpty(password)) {
            edt_create_password.setError("Password can't be empty.");
            progressDialog.dismiss();
            return;
        }
        if(password.length() < 6 || password.length() > 11){
            edt_create_password.setError("Length of password should between 6 to 11.");
            progressDialog.dismiss();
            return;
        }
        if(!CheckPasswordVaildation(password)){
            edt_create_password.setError("Passwords need to contain uppercase letters, lowercase letters and numbers.");
            progressDialog.dismiss();
            return;
        }
        if(!password.equals(password2)){
            edt_create_password_again.setError("Inconsistent password entered twice.");
            progressDialog.dismiss();
            return;
        }

        //signup part
        //Check email (email should be unique in database)
        databaseUser.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            //user already in our database
                            System.out.println("user already exist");
                        } else {
                            //if email hasn't been used create an account for this new user
                            String userId = databaseUser.push().getKey();
                            User user = new User(userId, username, email, password,email + "_" + password, "", "default",  new ArrayList<Post>(), new ArrayList<Post>());
                            databaseUser.child(userId).setValue(user);
                            System.out.println("new user added");
                            //back to login page
                            Intent i = new Intent(getApplicationContext(), MeActivity.class );
                            startActivity(i);
                        }
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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
}
