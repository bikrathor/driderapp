package com.app.drider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class SignUp extends AppCompatActivity {
    Button signup;
    TextView signin;
    UserSessionManager session;
    EditText LoginEmail, LoginPassword, confirmPass;
    String Login_Email, Login_Password, confirm_Pass;
    private ProgressDialog pDialog;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        setupUI(findViewById(R.id.parent));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        mAuth = FirebaseAuth.getInstance();

        session = new UserSessionManager(getApplicationContext());

        LoginEmail = (EditText) findViewById(R.id.input_email);
        LoginPassword = (EditText) findViewById(R.id.input_password);
        confirmPass = (EditText) findViewById(R.id.input_cpass);
        signup = (Button) findViewById(R.id.signUpButton);

        LoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirmPass.setTransformationMethod(PasswordTransformationMethod.getInstance());

        signin = (TextView) findViewById(R.id.signInTxt);
        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(SignUp.this, Login.class);
                startActivity(in);
                SignUp.this.finish();
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Login_Email = LoginEmail.getText().toString();
                Login_Password = LoginPassword.getText().toString();
                confirm_Pass = confirmPass.getText().toString();

                if (validate() && isNetwork()) {
                    processSignUp();
                } else if (!isNetwork()) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setupUI(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(SignUp.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        try {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(getApplicationContext(), Login.class);
        startActivity(in);
        SignUp.this.finish();
    }

    public boolean isNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private boolean validate() {
        boolean valid = true;

        if (Login_Email.isEmpty() && (!android.util.Patterns.EMAIL_ADDRESS.matcher(Login_Email).matches())) {
            LoginEmail.setError("Enter a valid email");
            valid = false;
        } else {
            LoginEmail.setError(null);
        }

        if (Login_Password.isEmpty() || Login_Password.length() < 6 || Login_Password.length() > 12) {
            LoginPassword.setError("Between 6 and 12 alphanumeric characters");
            valid = false;
        } else {
            LoginPassword.setError(null);
        }

        if (confirm_Pass.isEmpty() || confirm_Pass.length() < 6 || confirm_Pass.length() > 12) {
            confirmPass.setError("Between 6 and 12 alphanumeric characters");
            valid = false;
        } else {
            confirmPass.setError(null);
        }

        if (!Login_Password.equals(confirm_Pass)) {
            confirmPass.setError("Password do not match");
            LoginPassword.setError("Password do not match");
            valid = false;
        }

        return valid;
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((AppCompatRadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_pirates:
                if (checked)
                    // Pirates are the best
                    break;
            case R.id.radio_driver:
                if (checked)
                    // Ninjas rule
                    break;
        }
    }

    public void processSignUp() {
        pDialog = new ProgressDialog(SignUp.this);
        pDialog.setMessage("Signing up...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);

        pDialog.show();

        mAuth.createUserWithEmailAndPassword(Login_Email, Login_Password)
                .addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pDialog.dismiss();

                        if (!task.isSuccessful()) {
                            Toast.makeText(SignUp.this, "Authentication failed." + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

//                            session.createUserLoginSession(user.getUid(), Login_Email, Login_Password);

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/Customer/" + user.getUid());

                            databaseReference.child("name").setValue("na");
                            databaseReference.child("location").setValue("na");
                            databaseReference.child("contact").setValue("na");
                            databaseReference.child("fcm").setValue(FirebaseInstanceId.getInstance().getToken());

//                            session.createUserLoginSession("na", "", "na", "na");

                            Toast.makeText(getApplicationContext(), "Sign up successful",
                                    Toast.LENGTH_LONG).show();

                            Intent in = new Intent(SignUp.this, MainActivity.class);
                            startActivity(in);
                            SignUp.this.finish();
                        }
                    }
                });
    }
}