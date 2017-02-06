package com.app.drider.auth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.drider.MainActivity;
import com.app.drider.R;
import com.app.drider.managers.UserSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pkmmte.view.CircularImageView;

import java.io.ByteArrayOutputStream;

public class SignUp extends AppCompatActivity {
    Button signup;
    TextView signin;
    EditText LoginEmail, LoginPassword, confirmPass;
    String Login_Email, Login_Password, confirm_Pass;
    private String role = "rider";
    private ProgressDialog pDialog;
    private FirebaseAuth mAuth;

    private CircularImageView circularImageView;
    private String picturePath;
    private static int REQUEST_CAMERA = 0;
    private static int SELECT_FILE = 1;
    private static int RESULT_LOAD_IMAGE = 1;
    UserSessionManager session;
    private String encodedImage, source = "";
    private Bitmap thumbnail;
    private TextView headerText;
    private AlertDialog dialog;
    private byte[] imageData;
    private LinearLayout linearLayout;

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
                    if (role.equals("driver"))
                        showDialog();
                    else
                        processSignUp();
                } else if (!isNetwork()) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View dialoglayout = inflater.inflate(R.layout.custom_dialog_pic, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
        builder.setCancelable(true);
        builder.setView(dialoglayout);
        dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();

        circularImageView = (CircularImageView) dialoglayout.findViewById(R.id.circularImage);
        circularImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        headerText = (TextView) dialoglayout.findViewById(R.id.headerText3);

        linearLayout = (LinearLayout) dialoglayout.findViewById(R.id.lin);

        final Button ok = (Button) dialoglayout.findViewById(R.id.yes);
        final Button cancel = (Button) dialoglayout.findViewById(R.id.no);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                headerText.setText("Updating..");

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://drider-14e5b.appspot.com");
                StorageReference imageRef = storageRef.child("ids/" + Login_Email + ".jpg");
                UploadTask uploadTask = imageRef.putBytes(imageData);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getApplicationContext(), "Image updated", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                        processSignUp();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                    }
                });

                ok.setEnabled(false);
                cancel.setEnabled(false);

                ok.setAlpha((float) 0.7);
                cancel.setAlpha((float) 0.7);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(SignUp.this);
        builder.setTitle("Update Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Choose from Library")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");

                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"),
                            SELECT_FILE);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                if (requestCode == RESULT_LOAD_IMAGE && resultCode == -1 && null != data) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);
                    cursor.close();

                    try {
                        Bitmap bm = BitmapFactory.decodeFile(picturePath);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        imageData = baos.toByteArray();

                        source = "gallery";
                        linearLayout.setVisibility(View.VISIBLE);
                        circularImageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                        headerText.setText("Confirm Upload");
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(getApplicationContext(), "Out Of Memory", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Some Error", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    private void onCaptureImageResult(Intent data) {
        thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        try {
            imageData = bytes.toByteArray();

            source = "camera";
            linearLayout.setVisibility(View.VISIBLE);
            circularImageView.setImageBitmap(thumbnail);
            headerText.setText("Confirm Upload");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        boolean checked = ((AppCompatRadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.radio_pirates:
                if (checked)
                    role = "rider";
                break;
            case R.id.radio_driver:
                if (checked)
                    role = "driver";
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

                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + role + "/" + user.getUid());

                            databaseReference.child("name").setValue("na");
                            databaseReference.child("location").setValue("na");
                            databaseReference.child("contact").setValue("na");
                            databaseReference.child("mail").setValue(Login_Email);
                            databaseReference.child("fcm").setValue(FirebaseInstanceId.getInstance().getToken());

                            if (role.equals("driver")) {
                                databaseReference = FirebaseDatabase.getInstance().getReference("validate/");
                                databaseReference.child(user.getUid()).setValue("no");
                            }

                            session.createUserLoginSession("na", role, "na", user.getUid(), Login_Email, "na");

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