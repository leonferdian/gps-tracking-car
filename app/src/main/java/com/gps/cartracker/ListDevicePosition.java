package com.gps.cartracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.gps.cartracker.util.AppController;
import com.gps.cartracker.util.server;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListDevicePosition extends AppCompatActivity {

    private GoogleMap mMap;
    private static final String TAG = ListDevicePosition.class.getSimpleName();
    private static final long UPDATE_INTERVAL = 60 * 1000; // 1 minute in milliseconds
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_device_position);

        //menerapkan tool bar sesuai id toolbar | ToolBarAtas adalah variabel buatan sndiri
        Toolbar ToolBarAtasaccount_user = (Toolbar) findViewById(R.id.toolbar_main_ms);
        setSupportActionBar(ToolBarAtasaccount_user);
        // ToolBarAtas.setLogo(R.mipmap.ic_launcher);
        ToolBarAtasaccount_user.setLogoDescription(getResources().getString(R.string.app_name) + " - Device Location");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        changeStatusBarColor();

        SharedPreferences sharedpreferences = getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
        String user_id = sharedpreferences.getString("user_id", "");

        // Initialize the handler and runnable
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMarkerPosition();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        // Retrieve the SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                // Attach a listener to the date_changed event of the timeline control
                mMap = googleMap;
                Intent intent = getIntent();
                String device_id = intent.getStringExtra("device_id");
                LoadMapTrack(user_id);

                // Set listeners for click events.
                if (ActivityCompat.checkSelfPermission(ListDevicePosition.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ListDevicePosition.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
                mMap.setTrafficEnabled(true);
                mMap.animateCamera(CameraUpdateFactory.zoomTo(5));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateMarkerPosition() {
        SharedPreferences sharedpreferences = getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
        String user_id = sharedpreferences.getString("user_id", "");
        LoadMapTrack(user_id);
    }

    private void LoadMapTrack(final String user_id) {
        String ListTrackURL = server.URL2 + "gps/list_device_position";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    double totalLatitude = 0;
                    double totalLongitude = 0;
                    int count = 0;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String status = jsonObject.getString("status");
                        String devicetime = jsonObject.getString("devicetime");
                        String category = jsonObject.getString("category");

                        Geocoder geocoder = new Geocoder(ListDevicePosition.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        assert addresses != null;
                        String address = addresses.get(0).getAddressLine(0);

                        int resourceId;
                        switch (category) {
                            case "car":
                                resourceId = R.mipmap.ic_car_marker_yellow;
                                break;
                            case "motorcycle":
                                resourceId = R.mipmap.motor1;
                                break;
                            case "truck":
                                resourceId = R.mipmap.truck;
                                break;
                            default:
                                // Handle the default case or set a fallback BitmapDescriptor
                                resourceId = R.mipmap.motor2;
                                break;
                        }

                        // Create start marker
                        Marker pointMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(resourceId))
                                .title(name + " (" + status + ")")
                                .snippet("(" + devicetime + ") " + address)
                        );

                        totalLatitude += latitude;
                        totalLongitude += longitude;
                        count++;
                    }

                    if (jsonArray.length() > 0) {
                        double averageLatitude = totalLatitude / count;
                        double averageLongitude = totalLongitude / count;

                        LatLng centerPoint = new LatLng(averageLatitude, averageLongitude);
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(centerPoint);
                        mMap.moveCamera(cameraUpdate);
                    } else {
                        Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                        // Get the user's current location
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (ActivityCompat.checkSelfPermission(ListDevicePosition.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ListDevicePosition.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }

                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        // Check if the location is not null
                        if (location != null) {
                            // Get the latitude and longitude of the location
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();

                            // Create a new LatLng object with the location coordinates
                            LatLng latLng = new LatLng(lat, lng);

                            // Create a new CameraUpdate object and move the camera to the user's location
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
                            mMap.moveCamera(cameraUpdate);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.getMessage() != null) {
                    Log.e(TAG, "Error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Connection error!", Toast.LENGTH_LONG).show();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
//                params.put("_token", csrfToken);
                params.put("userid", user_id);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("X-CSRF-Token", csrfToken);
//                return headers;
//            }
        };

        AppController.getInstance(this).addToRequestQueue(stringRequest);
    }

    public boolean onSupportNavigateUp(){
        onBackPressed();

        return true;
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.setStatusBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar));
        }
    }
}