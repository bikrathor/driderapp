package com.app.drider;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.app.drider.auth.Login;
import com.app.drider.managers.UserSessionManager;

public class Splash extends AppCompatActivity {
    public Handler handler = new Handler();
    private static final int TIME = 1 * 1000;
    private UserSessionManager session;
    private String firstTime = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        session = new UserSessionManager(getApplicationContext());

        if (session.isUserLoggedIn()) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    Splash.this.finish();
                }
            }, TIME);
        } else {
            if (firstTime == null) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        //to intro
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        startActivity(intent);
                        Splash.this.finish();
                    }
                }, TIME);
            }
        }
    }
}
