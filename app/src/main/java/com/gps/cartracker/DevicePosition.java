package com.gps.cartracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gps.cartracker.util.AppController;
import com.gps.cartracker.util.server;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DevicePosition extends AppCompatActivity implements
        GoogleMap.OnPolylineClickListener {
    private ProgressDialog pDialog;
    private GoogleMap mMap;
    private static final String TAG = DevicePosition.class.getSimpleName();
    private static final long UPDATE_INTERVAL = 3 * 1000; // 1 second in milliseconds
    private Handler handler;
    private Runnable updateRunnable;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_position);

        //Menerapkan tool bar sesuai id toolbar | ToolBarAtas adalah variabel buatan sndiri
        Toolbar ToolBarAtasaccount_user = (Toolbar) findViewById(R.id.toolbar_main_ms);
        setSupportActionBar(ToolBarAtasaccount_user);
        // ToolBarAtas.setLogo(R.mipmap.ic_launcher);
        ToolBarAtasaccount_user.setLogoDescription(getResources().getString(R.string.app_name) + " - Timeline");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        changeStatusBarColor();

        // Initialize the handler and runnable
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMarkerPosition();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        pDialog = new ProgressDialog(DevicePosition.this);
        pDialog.setCancelable(false);
        pDialog.setMessage("Please wait...");

        // Retrieve the SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                // Attach a listener to the date_changed event of the timeline control
                mMap = googleMap;
                Intent intent = getIntent();
                String device_id = intent.getStringExtra("device_id");
                LoadMapTrack(device_id);

                // Get the user's current location
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
//                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
                    mMap.animateCamera(cameraUpdate);
                }

                mMap.setMyLocationEnabled(true);
                mMap.setTrafficEnabled(true);
                mMap.animateCamera(CameraUpdateFactory.zoomTo(5));
                // Set listeners for click events.
                mMap.setOnPolylineClickListener(DevicePosition.this);
            }
        });
    }

    /**
     * Listens for clicks on a polyline.
     * @param polyline The polyline object that the user has clicked.
     */
    @Override
    public void onPolylineClick(Polyline polyline) {
        // Flip from solid stroke to dotted stroke pattern.
        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
        } else {
            // The default pattern is a solid stroke.
            polyline.setPattern(null);
        }
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
        Intent intent = getIntent();
        String device_id = intent.getStringExtra("device_id");
        LoadMapTrack2(device_id);
    }

    private void LoadMapTrack(final String device_id) {
        String ListTrackURL = server.URL2 + "gps/device_location";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    PolylineOptions polylineOptions = new PolylineOptions();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String id = jsonObject.getString("id");
                        String name = jsonObject.getString("name");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String status = jsonObject.getString("status");
                        String devicetime = jsonObject.getString("devicetime");
                        String category = jsonObject.getString("category");
                        String positionid = jsonObject.getString("positionid");

                        Geocoder geocoder = new Geocoder(DevicePosition.this, Locale.getDefault());
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
                                .title(name)
                                .snippet("(" + status + ") " + devicetime)
                        );

                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        boundsBuilder.include(new LatLng(latitude, longitude));
                        LatLngBounds bounds = boundsBuilder.build();

                        // Set the ideal zoom level based on the map bounds
                        int padding = 500; // Padding around the bounds (in pixels)
                        int idealZoom = calculateIdealZoomLevel(bounds, padding);

                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, idealZoom);
                        mMap.animateCamera(cameraUpdate);

                        // Set a click listener on the marker
                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                Toast.makeText(getApplicationContext(), address, Toast.LENGTH_LONG).show();
                                // Return 'false' to allow the default behavior of displaying an info window (if applicable)
                                // Return 'true' if you have consumed the click event and don't want to show an info window
                                return false;
                            }
                        });

                        FloatingActionButton fabButton = (FloatingActionButton) DevicePosition.this.findViewById(R.id.track_device);
                        fabButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle FAB click
                                Intent intent = new Intent(DevicePosition.this, Timeline.class);
                                intent.putExtra("device_id", id);
                                intent.putExtra("device_name", name);
                                startActivity(intent);
                            }
                        });

                        FloatingActionButton fabButton2 = (FloatingActionButton) DevicePosition.this.findViewById(R.id.save_address);
                        fabButton2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle FAB click
                                // Create an AlertDialog Builder
                                AlertDialog.Builder builder = new AlertDialog.Builder(DevicePosition.this);
                                builder.setTitle("Location Name"); // Set the dialog title

                                final EditText nameEditText = new EditText(DevicePosition.this);
                                builder.setView(nameEditText); // Set the custom view as the dialog content

                                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Get the entered name from the EditText
                                        String name = nameEditText.getText().toString();
                                        SaveAddress(positionid, address, device_id, name, latitude, longitude);
                                        // Perform any further actions with the entered name
                                        // For example, you can save it to a variable or pass it to another function
                                    }
                                });

                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Handle the cancellation or dismiss the dialog
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });

                        polylineOptions.add(new LatLng(latitude, longitude));
                        polylineOptions.clickable(true);
                        polylineOptions.color(Color.BLUE);
                        polylineOptions.width(12);
                        polylineOptions.jointType(JointType.ROUND);
                    }

                    if (jsonArray.length() == 0) {
                        Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                        // Get the user's current location
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
//                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
                            mMap.animateCamera(cameraUpdate);
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
                params.put("device_id", device_id);
//                params.put("date", tanggal);
//                params.put("id_company", id_company);
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

    private void LoadMapTrack2(final String device_id) {
        String ListTrackURL = server.URL2 + "gps/device_location";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mMap.clear();
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    PolylineOptions polylineOptions = new PolylineOptions();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String id = jsonObject.getString("id");
                        String name = jsonObject.getString("name");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String status = jsonObject.getString("status");
                        String devicetime = jsonObject.getString("devicetime");
                        String category = jsonObject.getString("category");
                        String positionid = jsonObject.getString("positionid");

                        Geocoder geocoder = new Geocoder(DevicePosition.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        assert addresses != null;
                        String address = "";
                        if (!addresses.isEmpty()) {
                            address = addresses.get(0).getAddressLine(0);
                        }

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
                                .title(name)
                                .snippet("(" + status + ") " + devicetime)
                        );

                        String finalAddress = address;
                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                Toast.makeText(getApplicationContext(), finalAddress, Toast.LENGTH_LONG).show();
                                // Return 'false' to allow the default behavior of displaying an info window (if applicable)
                                // Return 'true' if you have consumed the click event and don't want to show an info window
                                return false;
                            }
                        });

                        FloatingActionButton fabButton = (FloatingActionButton) DevicePosition.this.findViewById(R.id.track_device);
                        fabButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle FAB click
                                Intent intent = new Intent(DevicePosition.this, Timeline.class);
                                intent.putExtra("device_id", id);
                                intent.putExtra("device_name", name);
                                startActivity(intent);
                            }
                        });

                        FloatingActionButton fabButton2 = (FloatingActionButton) DevicePosition.this.findViewById(R.id.save_address);
                        fabButton2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle FAB click
                                // Create an AlertDialog Builder
                                AlertDialog.Builder builder = new AlertDialog.Builder(DevicePosition.this);
                                builder.setTitle("Location Name"); // Set the dialog title

                                final EditText nameEditText = new EditText(DevicePosition.this);
                                builder.setView(nameEditText); // Set the custom view as the dialog content

                                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Get the entered name from the EditText
                                        String name = nameEditText.getText().toString();
                                        SaveAddress(positionid, finalAddress, device_id, name, latitude, longitude);
                                        // Perform any further actions with the entered name
                                        // For example, you can save it to a variable or pass it to another function
                                    }
                                });

                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Handle the cancellation or dismiss the dialog
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });

                        polylineOptions.add(new LatLng(latitude, longitude));
                        polylineOptions.clickable(true);
                        polylineOptions.color(Color.BLUE);
                        polylineOptions.width(12);
                        polylineOptions.jointType(JointType.ROUND);
                    }

                    if (jsonArray.length() == 0) {
                        Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                        // Get the user's current location
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DevicePosition.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
//                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
                            mMap.animateCamera(cameraUpdate);
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
                params.put("device_id", device_id);
//                params.put("date", tanggal);
//                params.put("id_company", id_company);
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

    private void SaveAddress(final String positionid, final String address, final String device_id, final String name, final double latitude, final double longitude) {
        String ListTrackURL = server.URL2 + "gps/save_address";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONObject jsonObject = new JSONObject(response);
//                    JSONArray jsonArray = new JSONArray(response);
//                    for (int i = 0; i < jsonArray.length(); i++) {
//                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String success = jsonObject.getString("success");
                        String status = jsonObject.getString("status");
                        String message = jsonObject.getString("message");
//                    }
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
                params.put("deviceid", device_id);
                params.put("positionid", positionid);
                params.put("address", address);
                params.put("name", name);
                params.put("latitude", String.valueOf(latitude));
                params.put("longitude", String.valueOf(longitude));
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

    private int calculateIdealZoomLevel(LatLngBounds bounds, int padding) {
        // Calculate the screen width and height
        int screenWidth = getResources().getDisplayMetrics().widthPixels - padding;
        int screenHeight = getResources().getDisplayMetrics().heightPixels - padding;

        // Get the zoom level based on the screen width and height
        int zoomWidth = (int) (Math.log(screenWidth / (bounds.northeast.longitude - bounds.southwest.longitude)) / Math.log(2));
        int zoomHeight = (int) (Math.log(screenHeight / (bounds.northeast.latitude - bounds.southwest.latitude)) / Math.log(2));
        int zoomLevel = Math.min(zoomWidth, zoomHeight);

        return zoomLevel;
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