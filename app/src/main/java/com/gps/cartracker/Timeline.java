package com.gps.cartracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Timeline extends AppCompatActivity implements
        GoogleMap.OnPolylineClickListener {

    private static final String TAG = Timeline.class.getSimpleName();
    Button loadMap;
    private ProgressDialog pDialog;
    private GoogleMap mMap;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);
    EditText date;
    TextView device_name;
    DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        //menerapkan tool bar sesuai id toolbar | ToolBarAtas adalah variabel buatan sndiri
        Toolbar ToolBarAtasaccount_user = (Toolbar) findViewById(R.id.toolbar_main_ms);
        setSupportActionBar(ToolBarAtasaccount_user);
        // ToolBarAtas.setLogo(R.mipmap.ic_launcher);
        ToolBarAtasaccount_user.setLogoDescription(getResources().getString(R.string.app_name) + " - Timeline" );

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        changeStatusBarColor();

        device_name = (TextView) findViewById(R.id.device_name);
        Intent intent = getIntent();
        String device_id = intent.getStringExtra("device_id");
        String name_device = intent.getStringExtra("device_name");
        device_name.setText(name_device);
        LinearLayout linearLayout = findViewById(R.id.filter_layout);

        ImageButton ToggFilter = findViewById(R.id.toggle);

        // Set initial visibility
        linearLayout.setVisibility(View.VISIBLE);

        // Set click listener for the button
        ToggFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle between visible and invisible
                if (linearLayout.getVisibility() == View.VISIBLE) {
                    linearLayout.setVisibility(View.GONE);
                    ToggFilter.setBackgroundResource(R.drawable.baseline_arrow_drop_up_grey_24);
                } else {
                    linearLayout.setVisibility(View.VISIBLE);
                    ToggFilter.setBackgroundResource(R.drawable.ic_arrow_drop_grey);
                }
            }
        });

        pDialog = new ProgressDialog(Timeline.this);
        pDialog.setCancelable(false);
        pDialog.setMessage("Load data...");
        loadMap = (Button) findViewById(R.id.loadMap);

        // calender class's instance and get current date , month and year from calender
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR); // current year
        int mMonth = c.get(Calendar.MONTH) + 1; // current month
        int mDay = c.get(Calendar.DAY_OF_MONTH); // current day

        // initiate the date picker and a button
        date = (EditText) findViewById(R.id.date);
        date.setText(mYear + "-" + mMonth + "-" + mDay);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // perform click event on edit text
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                // update login session ke FALSE dan mengosongkan nilai id dan username
                String tgl_absen = "filter1";
                showDateDialog(tgl_absen, date);
            }
        });

        loadMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
                // Do something when the button is clicked
                showDialog();
                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        // Attach a listener to the date_changed event of the timeline control
                        mMap = googleMap;
                        LoadMapTrack(device_id, date.getText().toString());

                        if (ActivityCompat.checkSelfPermission(Timeline.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Timeline.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        // Set listeners for click events.
                        mMap.setOnPolylineClickListener(Timeline.this);

                        hideDialog();
                    }
                });
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

    private void LoadMapTrack(final String device_id, final String tanggal) {
        String ListTrackURL = server.URL2 + "gps/tracking_report";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideDialog();
                Log.e(TAG, "Data Response: " + response.toString());
                PolylineOptions polylineOptions = new PolylineOptions();
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String devicetime = jsonObject.getString("devicetime");
                        String type = jsonObject.getString("type");
                        String category = jsonObject.getString("category");
//                        points.add(new LatLng(latitude, longitude));
//                        if (i == jsonArray.length() - 1 || i == 0) {
                        if (i == jsonArray.length() - 1) {
                            Geocoder geocoder = new Geocoder(Timeline.this, Locale.getDefault());
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
                                    .title(name + " (" + type + ")")
                                    .snippet(" (" + devicetime + ")" + address)
                                    .draggable(true)
                            );
                        }

                        polylineOptions.add(new LatLng(latitude, longitude));
                        polylineOptions.clickable(true);
                        polylineOptions.color(Color.BLUE);
                        polylineOptions.width(12);
                        polylineOptions.jointType(JointType.ROUND);
//                        polyline.setTag(address);
                    }

                    Polyline polyline = mMap.addPolyline(polylineOptions);

                    if (jsonArray.length() > 0) {
                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        for (LatLng latLng : polyline.getPoints()) {
                            boundsBuilder.include(latLng);
                        }

                        LatLngBounds bounds = boundsBuilder.build();
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 12);
                        mMap.animateCamera(cameraUpdate);
                    } else {
//                        Toast.makeText(getApplicationContext(), "No track found", Toast.LENGTH_SHORT).show();
                        // Get the user's current location
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (ActivityCompat.checkSelfPermission(Timeline.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Timeline.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
//                params.put("_token", csrfToken);
                params.put("device_id", device_id);
                params.put("tanggal", tanggal);
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

    private void showDateDialog(final String absen, final EditText tgl_awal) {

        /**
         * Calendar untuk mendapatkan tanggal sekarang
         */
        Calendar newCalendar = Calendar.getInstance();

        /**
         * Initiate DatePicker dialog
         */
        datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                /**
                 * Method ini dipanggil saat kita selesai memilih tanggal di DatePicker
                 */

                /**
                 * Set Calendar untuk menampung tanggal yang dipilih
                 */
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);

                /**
                 * Update TextView dengan tanggal yang kita pilih
                 */
                if (absen.equals("filter1")) {
                    tgl_awal.setText(dateFormatter.format(newDate.getTime()));
                }

            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        /**
         * Tampilkan DatePicker dialog
         */
        datePickerDialog.show();
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

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}