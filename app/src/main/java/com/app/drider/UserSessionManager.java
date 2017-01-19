package com.app.drider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.firebase.geofire.GeoFire;
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

    public void createUserLoginSession(String uEmail, String uPwd, String num, String uuid, String mail) {
        editor.putBoolean(IS_USER_LOGIN, true);

        editor.putString(TAG_fullname, uEmail);
        editor.putString(TAG_roles, uPwd);
        editor.putString(TAG_number, num);
        editor.putString(TAG_uuid, uuid);
        editor.putString(TAG_mail, mail);

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
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + pref.getString(TAG_roles, null) + "/" + pref.getString(TAG_uuid, null));
        databaseReference.removeValue();

        if (pref.getString(TAG_roles, null).equals("driver")) {
            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/current/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();

            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/target/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();

            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/interested/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();
        } else {
            databaseReference = FirebaseDatabase.getInstance().getReference("geofire/" + pref.getString(TAG_uuid, null));
            databaseReference.removeValue();
        }

        editor.clear();
        editor.commit();

        Intent i = new Intent(_context, Login.class);

        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Staring Login Activity
        _context.startActivity(i);
    }

    public void logoutUser2() {
        editor.clear();
        editor.commit();
    }

    public boolean isUserLoggedIn() {
        return pref.getBoolean(IS_USER_LOGIN, false);
    }

    public boolean isOpened() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isClosed() {
        // TODO Auto-generated method stub
        return false;
    }
}
