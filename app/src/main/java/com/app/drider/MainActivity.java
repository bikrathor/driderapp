package com.app.drider;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.ui.IconGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {
    private UserSessionManager session;
    private String id, email, name, num, role;
    private String dname, dnum;
    private ImageView img, img2;
    private GoogleApiClient mGoogleApiClient;
    private com.google.android.gms.location.places.Place place;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private Location location;
    private String result = null;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker myMarker;
    private String source = "normal";
    private TextView enterLoc;
    private String ServerKey = "AIzaSyCgyRDXUhxRFzaohusDyLY4iyvoxdMVFgI";
    private Button searchBtn;
    private ProgressDialog pDialog;
    private DatabaseReference mFirebaseDatabase;
    private float dis;
    private android.app.AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        session = new UserSessionManager(getApplicationContext());
        HashMap<String, String> user = session.getUserDetails();

        id = user.get(UserSessionManager.TAG_uuid);
        email = user.get(UserSessionManager.TAG_mail);
        name = user.get(UserSessionManager.TAG_fullname);
        num = user.get(UserSessionManager.TAG_number);
        role = user.get(UserSessionManager.TAG_roles);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        img = (ImageView) findViewById(R.id.imageView);
        img2 = (ImageView) findViewById(R.id.imageView2);
        enterLoc = (TextView) findViewById(R.id.input_location);
        searchBtn = (Button) findViewById(R.id.button2);

        if (role.equals("driver"))
            searchBtn.setText("Ride");

        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i("PLace", "Placeeeeeeeeeee: " + place.getName());
                source = "auto";
            }

            @Override
            public void onError(Status status) {
            }
        });

        enterLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                source = "auto";
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(MainActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            }
        });

        buildGoogleApiClient();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (source.equals("manual")) {
                    place = PlaceAutocomplete.getPlace(this, data);

                    final LatLng latLong;
                    latLong = place.getLatLng();
                    Location loc = new Location("Sample");
                    loc.setLatitude(latLong.latitude);
                    loc.setLongitude(latLong.longitude);

                    updateMyLocation(mMap, loc);
                    location = loc;
                } else {
                    place = PlaceAutocomplete.getPlace(this, data);
                    enterLoc.setText("My location");

                    final LatLng latLong;
                    latLong = place.getLatLng();
                    Location loc = new Location("Sample");
                    loc.setLatitude(latLong.latitude);
                    loc.setLongitude(latLong.longitude);

                    //                updateMyLocation(mMap, loc);
                    enterLoc.setText(place.getName());
                    //                location = loc;
                    //                new MainActivity.PostData().execute();

                    final LatLng sourcePosition = new LatLng(location.getLatitude(), location.getLongitude());
                    GoogleDirection.withServerKey(ServerKey)
                            .from(sourcePosition)
                            .to(latLong)
                            .transportMode(TransportMode.DRIVING)
                            .execute(new DirectionCallback() {
                                @Override
                                public void onDirectionSuccess(Direction direction, String rawBody) {
                                    if (direction.isOK()) {
                                        mMap.clear();
                                        Marker marker1 = mMap.addMarker(new MarkerOptions().position(sourcePosition));
                                        Marker marker2 = mMap.addMarker(new MarkerOptions().position(latLong));

                                        ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
                                        mMap.addPolyline(DirectionConverter.createPolyline(MainActivity.this, directionPositionList, 5, getResources().getColor(R.color.colorPrimary)));

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        builder.include(marker1.getPosition());
                                        builder.include(marker2.getPosition());
                                        LatLngBounds bounds = builder.build();

                                        int padding = 100;
                                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                        mMap.animateCamera(cu);

                                        searchBtn.setVisibility(View.VISIBLE);
                                        searchBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                pDialog = new ProgressDialog(MainActivity.this);
                                                pDialog.setMessage("Starting ride..");
                                                pDialog.setIndeterminate(false);
                                                pDialog.setCancelable(true);
                                                pDialog.show();

                                                if (role.equals("driver")) {
                                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + role + "/" + id);
                                                    databaseReference.child("location").setValue(enterLoc.getText().toString());

                                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/target");
                                                    GeoFire geoFire = new GeoFire(ref);
                                                    geoFire.setLocation(id, new GeoLocation(latLong.latitude, latLong.longitude), new GeoFire.CompletionListener() {
                                                        @Override
                                                        public void onComplete(String key, DatabaseError error) {
                                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/current");
                                                            GeoFire geoFire = new GeoFire(ref);
                                                            geoFire.setLocation(id, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                                                @Override
                                                                public void onComplete(String key, DatabaseError error) {
                                                                    pDialog.dismiss();
                                                                    Toast.makeText(MainActivity.this, "Submitted to server", Toast.LENGTH_SHORT).show();
                                                                    searchBtn.setText("Stop ride");
                                                                }
                                                            });
                                                        }
                                                    });

                                                    mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("geofire/interested/" + id);
                                                    GeoFire geoFire2 = new GeoFire(mFirebaseDatabase);
                                                    GeoQuery geoQuery = geoFire2.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 5.0);
                                                    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                                        @Override
                                                        public void onKeyEntered(final String key, final GeoLocation location2) {
                                                            mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("users/rider/" + key);
                                                            mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    if (dataSnapshot.getChildrenCount() > 0) {
                                                                        Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                                                        dname = (String) itemMap.get("name");
                                                                        dnum = (String) itemMap.get("contact");

                                                                        Location targetLocation = new Location("");
                                                                        targetLocation.setLatitude(location2.latitude);
                                                                        targetLocation.setLongitude(location2.longitude);
                                                                        dis = location.distanceTo(targetLocation);
                                                                        dis /= 1000;
                                                                        dis = (float) Math.round(dis * 100f) / 100f;

                                                                        if (dis < 5.0) {
                                                                            IconGenerator bubbleIconFactory = new IconGenerator(getApplicationContext());
                                                                            bubbleIconFactory.setStyle(IconGenerator.STYLE_GREEN);
                                                                            Bitmap bit = bubbleIconFactory.makeIcon(dis + " KM");
                                                                            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bit)).position(new LatLng(location2.latitude, location2.longitude)).title(""));
                                                                            marker.setTag(key);
                                                                        }

                                                                        pDialog.dismiss();

                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {
                                                                    pDialog.dismiss();
                                                                }
                                                            });
                                                            mFirebaseDatabase.keepSynced(true);
                                                        }

                                                        @Override
                                                        public void onKeyExited(String key) {

                                                        }

                                                        @Override
                                                        public void onKeyMoved(String key, GeoLocation location) {

                                                        }

                                                        @Override
                                                        public void onGeoQueryReady() {

                                                        }

                                                        @Override
                                                        public void onGeoQueryError(DatabaseError error) {

                                                        }
                                                    });
                                                } else {
                                                    pDialog.setMessage("Searching rides..");

                                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/users/" + role + "/" + id);
                                                    databaseReference.child("location").setValue(enterLoc.getText().toString());

                                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/");
                                                    GeoFire geoFire2 = new GeoFire(ref);
                                                    geoFire2.setLocation(id, new GeoLocation(latLong.latitude, latLong.longitude));

                                                    mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("geofire/target");
                                                    GeoFire geoFire = new GeoFire(mFirebaseDatabase);
                                                    GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLong.latitude, latLong.longitude), 5.0);
                                                    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                                        @Override
                                                        public void onKeyEntered(final String key, final GeoLocation location2) {
                                                            mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("users/driver/" + key);
                                                            mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    if (dataSnapshot.getChildrenCount() > 0) {
                                                                        Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                                                        dname = (String) itemMap.get("name");
                                                                        dnum = (String) itemMap.get("contact");

                                                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/current");
                                                                        GeoFire geoFire = new GeoFire(ref);
                                                                        geoFire.getLocation(key, new LocationCallback() {
                                                                            @Override
                                                                            public void onLocationResult(String key, GeoLocation location3) {
                                                                                if (location3 != null) {
                                                                                    Location targetLocation = new Location("");
                                                                                    targetLocation.setLatitude(location3.latitude);
                                                                                    targetLocation.setLongitude(location3.longitude);
                                                                                    dis = location.distanceTo(targetLocation);
                                                                                    dis /= 1000;
                                                                                    dis = (float) Math.round(dis * 100f) / 100f;

                                                                                    if (dis < 5.0) {
                                                                                        IconGenerator bubbleIconFactory = new IconGenerator(getApplicationContext());
                                                                                        bubbleIconFactory.setStyle(IconGenerator.STYLE_GREEN);
                                                                                        Bitmap bit = bubbleIconFactory.makeIcon(dis + " KM");
                                                                                        Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bit)).position(new LatLng(location3.latitude, location3.longitude)).title(""));
                                                                                        marker.setTag(key);
                                                                                    }

                                                                                    pDialog.dismiss();
                                                                                } else {
                                                                                    System.out.println(String.format("There is no location for key %s in GeoFire", key));
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onCancelled(DatabaseError databaseError) {
                                                                                System.err.println("There was an error getting the GeoFire location: " + databaseError);
                                                                                pDialog.dismiss();
                                                                            }
                                                                        });
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {
                                                                    pDialog.dismiss();
                                                                }
                                                            });
                                                            mFirebaseDatabase.keepSynced(true);
                                                        }

                                                        @Override
                                                        public void onKeyExited(String key) {

                                                        }

                                                        @Override
                                                        public void onKeyMoved(String key, GeoLocation location) {

                                                        }

                                                        @Override
                                                        public void onGeoQueryReady() {

                                                        }

                                                        @Override
                                                        public void onGeoQueryError(DatabaseError error) {

                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    } else {
                                        // Do something
                                    }
                                }

                                @Override
                                public void onDirectionFailure(Throwable t) {
                                    // Do something
                                }
                            });

                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(final Marker marker) {
                            LayoutInflater inflater = getLayoutInflater();
                            final View dialoglayout = inflater.inflate(R.layout.custom_show_detail, null);
                            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setCancelable(true);
                            builder.setView(dialoglayout);
                            final AlertDialog dialog = builder.create();
                            dialog.show();

                            final TextView rpmm = (TextView) dialoglayout.findViewById(R.id.headerTextName);
                            final TextView throttle = (TextView) dialoglayout.findViewById(R.id.headerTextnum);
                            final TextView temp = (TextView) dialoglayout.findViewById(R.id.headerTextDetail);
                            final TextView temp2 = (TextView) dialoglayout.findViewById(R.id.headerTextrole);
                            final Button btn = (Button) dialoglayout.findViewById(R.id.loginButton2);

                            final ImageView mageView = (ImageView) dialoglayout.findViewById(R.id.crossButton);
                            mageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                }
                            });

                            if (role.equals("rider")) {
                                mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("users/driver/" + marker.getTag());
                                mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.getChildrenCount() > 0) {
                                            Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                            temp.setText((String) itemMap.get("name"));
                                            rpmm.setText("Number: " + (String) itemMap.get("contact"));
                                            SpannableString content = new SpannableString(rpmm.getText().toString());
                                            content.setSpan(new UnderlineSpan(), 8, content.length(), 0);
                                            rpmm.setText(content);
                                            throttle.setText("Heading to: " + (String) itemMap.get("location"));

                                            rpmm.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                                    intent.setData(Uri.parse("tel:" + rpmm.getText().toString().substring(8, rpmm.getText().toString().length())));
                                                    startActivity(intent);
                                                }
                                            });

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/target");
                                            GeoFire geoFire = new GeoFire(ref);
                                            geoFire.getLocation(marker.getTag().toString(), new LocationCallback() {
                                                @Override
                                                public void onLocationResult(String key, GeoLocation location2) {
                                                    Location targetLocation = new Location("");
                                                    targetLocation.setLatitude(latLong.latitude);
                                                    targetLocation.setLongitude(latLong.longitude);

                                                    Location targetLocation2 = new Location("");
                                                    targetLocation2.setLatitude(location2.latitude);
                                                    targetLocation2.setLongitude(location2.longitude);

                                                    dis = targetLocation.distanceTo(targetLocation2);
                                                    dis /= 1000;
                                                    dis = (float) Math.round(dis * 100f) / 100f;

                                                    temp2.setText("(" + dis + "KM from " + enterLoc.getText().toString() + ")");
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });

                                            btn.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    pDialog = new ProgressDialog(MainActivity.this);
                                                    pDialog.setMessage("Submitting..");
                                                    pDialog.setIndeterminate(false);
                                                    pDialog.setCancelable(true);
                                                    pDialog.show();

                                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/interested/" + marker.getTag());
                                                    GeoFire geoFire = new GeoFire(ref);
                                                    geoFire.setLocation(id, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                                        @Override
                                                        public void onComplete(String key, DatabaseError error) {
                                                            pDialog.dismiss();
                                                            dialog.dismiss();
                                                            Toast.makeText(MainActivity.this, "Submitted to server", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            } else {
                                btn.setVisibility(View.GONE);
                                mFirebaseDatabase = FirebaseDatabase.getInstance().getReference().child("users/rider/" + marker.getTag());
                                mFirebaseDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.getChildrenCount() > 0) {
                                            Map<String, Object> itemMap = (HashMap<String, Object>) dataSnapshot.getValue();
                                            temp.setText((String) itemMap.get("name"));
                                            rpmm.setText("Number: " + (String) itemMap.get("contact"));
                                            SpannableString content = new SpannableString(rpmm.getText().toString());
                                            content.setSpan(new UnderlineSpan(), 8, content.length(), 0);
                                            rpmm.setText(content);
                                            throttle.setText("Heading to: " + (String) itemMap.get("location"));

                                            rpmm.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                                    intent.setData(Uri.parse("tel:" + rpmm.getText().toString().substring(8, rpmm.getText().toString().length())));
                                                    startActivity(intent);
                                                }
                                            });

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("geofire/");
                                            GeoFire geoFire = new GeoFire(ref);
                                            geoFire.getLocation(marker.getTag().toString(), new LocationCallback() {
                                                @Override
                                                public void onLocationResult(String key, GeoLocation location2) {
                                                    Location targetLocation = new Location("");
                                                    targetLocation.setLatitude(latLong.latitude);
                                                    targetLocation.setLongitude(latLong.longitude);

                                                    Location targetLocation2 = new Location("");
                                                    targetLocation2.setLatitude(location2.latitude);
                                                    targetLocation2.setLongitude(location2.longitude);

                                                    dis = targetLocation.distanceTo(targetLocation2);
                                                    dis /= 1000;
                                                    dis = (float) Math.round(dis * 100f) / 100f;

                                                    temp2.setText("(" + dis + "KM from " + enterLoc.getText().toString() + ")");
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                            mFirebaseDatabase.keepSynced(true);

                            //                            final Button submit = (Button) dialoglayout.findViewById(R.id.submitButton);
                            //                            submit.setTypeface(FontsManager.getBoldTypeface(ShowCentresNearMe.this));
                            //                            submit.setOnClickListener(new View.OnClickListener() {
                            //                                @Override
                            //                                public void onClick(View view) {
                            //                                    dialog.dismiss();
                            //                                    selectedCenter = (String) marker.getTag();
                            //                                    if (isNetwork()) {
                            //                                        raiseRequest();
                            //                                    }
                            //                                }
                            //                            });

                            return false;
                        }
                    });
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("status", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setMessage("Location not enabled");
            dialog.setPositiveButton("Enable location", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    finish();
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    finish();
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        } else {
            Toast.makeText(MainActivity.this, "Getting location..", Toast.LENGTH_SHORT).show();
        }

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                source = "normal";
                place = null;
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    return;
                }

                location = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                if (location != null) {
                    updateMyLocation(mMap, location);
                } else {
                    builder = new android.app.AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Unable to fetch location");
                    builder.setMessage("Update manually?");
                    builder.setCancelable(false);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            source = "manual";
                            try {
                                Intent intent =
                                        new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                                .build(MainActivity.this);
                                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                            } catch (GooglePlayServicesRepairableException e) {
                                // TODO: Handle the error.
                            } catch (GooglePlayServicesNotAvailableException e) {
                                // TODO: Handle the error.
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    android.app.AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });

        img2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                source = "manual";
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(MainActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            }
        });
    }

    class PostData extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            getCurrentName();
            return null;
        }

        protected void onPostExecute(String file_url) {
            if (result != null)
                enterLoc.setText(result);
        }
    }

    private void getCurrentName() {
        List<Address> addressList = null;
        StringBuilder sb = new StringBuilder();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (null != location) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                if (location != null)
                    addressList = geocoder.getFromLocation(
                            latitude, longitude, 1);
            } catch (IOException e) {
                Log.e("errrrrrrrrr", e.getMessage());
                getName(addressList, geocoder, latitude, longitude);
            }
        }

        if (place != null)
            sb.append(place.getName()).append("\n");

        if (addressList != null && addressList.size() > 0) {
            Address address = addressList.get(0);
            if (source.equals("auto"))
                sb.append(place.getName()).append("\n");
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                sb.append(address.getAddressLine(i)).append("\n");
            }
            sb.append(address.getLocality()).append("");
            result = sb.toString();
        } else
            result = "My location";
    }

    private List<Address> getName(List<Address> addressList, Geocoder geocoder, double latitude, double longitude) {
        try {
            addressList = geocoder.getFromLocation(
                    latitude, longitude, 1);
        } catch (IOException e) {
            Log.e("errrrrrrrrr", e.getMessage());
            getName(addressList, geocoder, latitude, longitude);
        }

        return addressList;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        location = mLastLocation;

        if (mLastLocation != null) {
            updateMyLocation(mMap, mLastLocation);
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void updateMyLocation(final GoogleMap googleMap, Location location) {
        mMap = googleMap;

        LatLng myLoc = null;
        if (location != null) {
            myLoc = new LatLng(location.getLatitude(), location.getLongitude());
        }

        if (myMarker == null)
            try {
                myMarker = mMap.addMarker(new MarkerOptions().position(myLoc).
                        title("My location")
                        .icon(getMarkerIcon("#8b3e58")));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        else {
            try {
                myMarker.setPosition(myLoc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 15));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        updateMyLocation(mMap, location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public boolean isNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void showProf() {
        LayoutInflater inflater = getLayoutInflater();
        final View dialoglayout = inflater.inflate(R.layout.custom_show_detail, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setView(dialoglayout);
        final AlertDialog dialog = builder.create();
        dialog.show();

        final TextView rpmm = (TextView) dialoglayout.findViewById(R.id.headerTextDetail);
        rpmm.setText(email);

        final TextView throttle = (TextView) dialoglayout.findViewById(R.id.headerTextName);
        throttle.setText("Name: " + name);

        final TextView temp = (TextView) dialoglayout.findViewById(R.id.headerTextnum);
        temp.setText("Number: " + num);

        final TextView temp2 = (TextView) dialoglayout.findViewById(R.id.headerTextrole);
        temp2.setText("Role: " + role);

        final Button temp3 = (Button) dialoglayout.findViewById(R.id.loginButton2);
        temp3.setVisibility(View.GONE);

        ImageView mageView = (ImageView) dialoglayout.findViewById(R.id.crossButton);
        mageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_game:
                showProf();
                return true;
            case R.id.help:
                session.logoutUser();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
