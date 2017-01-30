package com.app.drider.managers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.app.drider.auth.Login;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class UserSessionManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    int PRIVATE_MODE = 0;

    private static final String PREFER_NAME = "MyPref2";
    // All Shared Preferences Keys
    private static final String IS_USER_LOGIN = "IsUserLoggedIn";
    public static final String TAG_fullname = "fullname";
    public static final String TAG_number = "number";
    public static final String TAG_mail = "mail";
    public static final String TAG_roles = "roles";
    public static final String TAG_uuid = "uuid";
    public static final String TAG_location = "location";

    public UserSessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREFER_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void createUserLoginSession(String uName, String uRole, String num, String uuid, String mail, String loc) {
        editor.putBoolean(IS_USER_LOGIN, true);

        editor.putString(TAG_fullname, uName);
        editor.putString(TAG_roles, uRole);
        editor.putString(TAG_number, num);
        editor.putString(TAG_uuid, uuid);
        editor.putString(TAG_mail, mail);
        editor.putString(TAG_location, loc);

        editor.commit();
    }

    public void clearData() {
        editor.clear();
        editor.commit();
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();

        user.put(TAG_fullname, pref.getString(TAG_fullname, null));
        user.put(TAG_roles, pref.getString(TAG_roles, null));
        user.put(TAG_number, pref.getString(TAG_number, null));
        user.put(TAG_uuid, pref.getString(TAG_uuid, null));
        user.put(TAG_mail, pref.getString(TAG_mail, null));
        user.put(TAG_location, pref.getString(TAG_location, null));

        return user;
    }

    public void logoutUser() {
        DatabaseReference databaseReference;
        databaseReference = FirebaseDatabase.getInstance().getReference("users/" + pref.getString(TAG_roles, null) + "/" + pref.getString(TAG_uuid, null));
        databaseReference.child("fcm").setValue("out");

        if (pref.getString(TAG_roles, null).equals("driver")) {
            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/target/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();

            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/interested/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();
        }

        editor.clear();
        editor.commit();

        Intent i = new Intent(_context, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(i);
    }

    public boolean isUserLoggedIn() {
        return pref.getBoolean(IS_USER_LOGIN, false);
    }
}
