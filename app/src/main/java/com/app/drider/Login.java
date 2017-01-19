package com.app.drider;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class Login extends AppCompatActivity {
    private Button loginBtn, loginBtn2;
    private UserSessionManager session;
    private EditText LoginEmail, LoginPassword, LoginName;
    private String Login_Email, Login_Password, Login_Name;
    private String currentRole = "";
    private ProgressDialog pDialog;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        session = new UserSessionManager(getApplicationContext());

        LoginName = (EditText) findViewById(R.id.input_name_login);
        LoginEmail = (EditText) findViewById(R.id.input_email_login);
        LoginPassword = (EditText) findViewById(R.id.input_password_login);
        loginBtn = (Button) findViewById(R.id.loginButton);
        loginBtn2 = (Button) findViewById(R.id.loginButton2);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRole = "driver";
                Login_Email = LoginEmail.getText().toString();
                Login_Password = LoginPassword.getText().toString();
                Login_Name = LoginName.getText().toString();

                if (isNetwork() && validate()) {
                    processSignIn();
                } else if (!isNetwork()) {
                    Toast.makeText(Login.this, "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loginBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRole = "rider";
                Login_Email = LoginEmail.getText().toString();
                Login_Password = LoginPassword.getText().toString();
                Login_Name = LoginName.getText().toString();

                if (isNetwork() && validate()) {
                    processSignIn();
                } else if (!isNetwork()) {
                    Toast.makeText(Login.this, "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
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

        if (Login_Name.isEmpty()) {
            LoginName.setError("enter a valid name");
            valid = false;
        } else {
            LoginName.setError(null);
        }

        if (Login_Password.isEmpty() || Login_Password.length() < 6) {
            LoginPassword.setError("enter a valid number");
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
                                pDialog.setMessage("Signing up...");
                                mAuth.createUserWithEmailAndPassword(Login_Email, Login_Password)
                                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (!task.isSuccessful()) {
                                                    Toast.makeText(Login.this, "Authentication failed." + task.getException(),
                                                            Toast.LENGTH_SHORT).show();
                                                    pDialog.dismiss();
                                                } else {
                                                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                                    session.createUserLoginSession(Login_Name, currentRole, Login_Password, user.getUid(), Login_Email);
                                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + currentRole + "/" + user.getUid());
                                                    databaseReference.child("name").setValue(Login_Name);
                                                    databaseReference.child("location").setValue("na");
                                                    databaseReference.child("contact").setValue(Login_Password);
                                                    databaseReference.child("role").setValue(currentRole);

                                                    Toast.makeText(getApplicationContext(), "Sign up successful",
                                                            Toast.LENGTH_LONG).show();

                                                    Intent in = new Intent(Login.this, MainActivity.class);
                                                    startActivity(in);
                                                    pDialog.dismiss();
                                                    Login.this.finish();
                                                }
                                            }
                                        });
                            }
                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            String uid = user.getUid();

                            session.createUserLoginSession(Login_Name, currentRole, Login_Password, uid, Login_Email);

                            Intent in = new Intent(Login.this, MainActivity.class);
                            startActivity(in);

                            pDialog.dismiss();
                            Login.this.finish();

                            session.createUserLoginSession(Login_Name, currentRole, Login_Password, user.getUid(), Login_Email);
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + currentRole + "/" + user.getUid());
                            databaseReference.child("name").setValue(Login_Name);
                            databaseReference.child("location").setValue("na");
                            databaseReference.child("contact").setValue(Login_Password);
                            databaseReference.child("role").setValue(currentRole);
                        }
                    }
                });
    }
}
