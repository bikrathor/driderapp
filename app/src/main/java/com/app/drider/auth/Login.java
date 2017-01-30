package com.app.drider.auth;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.drider.MainActivity;
import com.app.drider.R;
import com.app.drider.managers.UserSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {
    private Button loginBtn;
    private UserSessionManager session;
    private EditText LoginEmail, LoginPassword;
    private String Login_Email, Login_Password;
    private ProgressDialog pDialog;
    private FirebaseAuth mAuth;
    private TextView forgot, signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        session = new UserSessionManager(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        LoginEmail = (EditText) findViewById(R.id.input_email_login);
        LoginPassword = (EditText) findViewById(R.id.input_password_login);
        loginBtn = (Button) findViewById(R.id.loginButton);
        forgot = (TextView) findViewById(R.id.forgotPassTxt);
        signUp = (TextView) findViewById(R.id.signUpTxt);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login_Email = LoginEmail.getText().toString();
                Login_Password = LoginPassword.getText().toString();

                if (isNetwork() && validate()) {
                    processSignIn();
                } else if (!isNetwork()) {
                    Toast.makeText(Login.this, "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(Login.this, ForgotPass.class);
                startActivity(in);
                Login.this.finish();
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(Login.this, SignUp.class);
                startActivity(in);
                Login.this.finish();
            }
        });
    }

    private boolean validate() {
        boolean valid = true;

        if (Login_Email.isEmpty() || (!android.util.Patterns.EMAIL_ADDRESS.matcher(Login_Email).matches())) {
            LoginEmail.setError("enter a valid email");
            valid = false;
        } else {
            LoginEmail.setError(null);
        }

        if (Login_Password.isEmpty() || Login_Password.length() < 6) {
            LoginPassword.setError("password should be greater than 6 alphanumeric characters");
            valid = false;
        } else {
            LoginPassword.setError(null);
        }

        return valid;
    }

    public boolean isNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public void processSignIn() {
        pDialog = new ProgressDialog(Login.this);
        pDialog.setMessage("Signing in...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();

        mAuth.signInWithEmailAndPassword(Login_Email, Login_Password)
                .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            if (task.getException().toString().contains("FirebaseAuthInvalidCredentialsException")) {
                                pDialog.dismiss();
                                Toast.makeText(Login.this, "Invalid password", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Login.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                pDialog.dismiss();
                            }
                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            final String uid = user.getUid();

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/driver/" + uid);
                            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getChildrenCount() > 0) {
                                        Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                        session.createUserLoginSession((String) itemMap.get("name"), "driver",
                                                (String) itemMap.get("contact"), uid, Login_Email,
                                                (String) itemMap.get("location"));

                                        Toast.makeText(getApplicationContext(), "Welcome",
                                                Toast.LENGTH_LONG).show();

                                        Intent in = new Intent(Login.this, MainActivity.class);
                                        startActivity(in);

                                        pDialog.dismiss();
                                        Login.this.finish();

                                        String token = FirebaseInstanceId.getInstance().getToken();
                                        DatabaseReference databaseReference2 = FirebaseDatabase.getInstance().getReference("/users/driver/" + uid);
                                        databaseReference2.child("fcm").setValue(token);
                                    } else {
                                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/rider/" + uid);
                                        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.getChildrenCount() > 0) {
                                                    Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                                    session.createUserLoginSession((String) itemMap.get("name"), "rider",
                                                            (String) itemMap.get("contact"), uid, Login_Email,
                                                            (String) itemMap.get("location"));

                                                    Toast.makeText(getApplicationContext(), "Welcome",
                                                            Toast.LENGTH_LONG).show();

                                                    Intent in = new Intent(Login.this, MainActivity.class);
                                                    startActivity(in);

                                                    pDialog.dismiss();
                                                    Login.this.finish();

                                                    String token = FirebaseInstanceId.getInstance().getToken();
                                                    DatabaseReference databaseReference2 = FirebaseDatabase.getInstance().getReference("/users/rider/" + uid);
                                                    databaseReference2.child("fcm").setValue(token);
                                                } else {
                                                    pDialog.dismiss();
                                                    Toast.makeText(Login.this, "User doesn't exist", Toast.LENGTH_SHORT).show();
                                                    session.clearData();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                                                pDialog.dismiss();
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                                    pDialog.dismiss();
                                }
                            });
                        }
                    }
                });
    }
}
