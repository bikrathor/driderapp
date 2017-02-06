package com.app.drider.managers;

import com.google.firebase.iid.FirebaseInstanceIdService;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

public class FirebaseInstanceID extends FirebaseInstanceIdService {

    private static final String TAG = "FirebaseIDDriDerService";
    private DatabaseReference databaseReference;
    private UserSessionManager session;
    private String uid, role;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        session = new UserSessionManager(getApplicationContext());
        HashMap<String, String> user = session.getUserDetails();
        uid = user.get(UserSessionManager.TAG_uuid);
        role = user.get(UserSessionManager.TAG_roles);

        if (uid != null)
            sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + role + "/" + uid);
        databaseReference.child("fcm").setValue(token);
    }
}
