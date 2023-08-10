package com.gps.cartracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    EditText txt_jam_awal,txt_jam_akhir;
    private Polyline polyline;
    // Create a custom RetryPolicy with an extended timeout
    int initialTimeoutMs = 10000; // Initial timeout in milliseconds
    int maxNumRetries = 3; // Maximum number of retries
    float backoffMultiplier = 1.5f; // Backoff multiplier for exponential backoff
    RetryPolicy retryPolicy = new DefaultRetryPolicy(initialTimeoutMs, maxNumRetries, backoffMultiplier);

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

        txt_jam_awal = findViewById(R.id.txt_jam_awal);
        txt_jam_akhir = findViewById(R.id.txt_jam_akhir);

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Calendar cal = Calendar.getInstance();
        //Add or minus day to current date.
        //cal.add(Calendar.DATE, 1);
        //cal.add(Calendar.DATE, -7);
        cal.add(Calendar.HOUR, 1);

        String timeStamp="";
        timeStamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
//        txt_jam_awal.setText(timeStamp);
        txt_jam_awal.setText("00:00");
//        txt_jam_akhir.setText(dateFormat.format(cal.getTime()));
        txt_jam_akhir.setText("23:59");
        txt_jam_awal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(Timeline.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String selectedMinutee,selectedHourr;

                        if(selectedMinute<10){
                            selectedMinutee = "0"+selectedMinute;
                        }
                        else{
                            selectedMinutee = String.valueOf(selectedMinute);
                        }

                        if(selectedHour<10){
                            selectedHourr = "0"+selectedHour;
                        }
                        else{
                            selectedHourr = String.valueOf(selectedHour);
                        }
                        txt_jam_awal.setText(selectedHourr + ":" + selectedMinutee);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Pilih Jam Awal");
                mTimePicker.show();
            }
        });
        txt_jam_akhir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(Timeline.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String selectedMinutee,selectedHourr;

                        if(selectedMinute<10){
                            selectedMinutee = "0"+selectedMinute;
                        }
                        else{
                            selectedMinutee = String.valueOf(selectedMinute);
                        }

                        if(selectedHour<10){
                            selectedHourr = "0"+selectedHour;
                        }
                        else{
                            selectedHourr = String.valueOf(selectedHour);
                        }
                        txt_jam_akhir.setText(selectedHourr + ":" + selectedMinutee);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Pilih Jam Akhir");
                mTimePicker.show();
            }
        });

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
                        LoadMapTrack(device_id, date.getText().toString(), txt_jam_awal.getText().toString(), txt_jam_akhir.getText().toString());

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

        FloatingActionButton fabButton = (FloatingActionButton) Timeline.this.findViewById(R.id.list_route);
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
                ListRoute(device_id, date.getText().toString(), txt_jam_awal.getText().toString(), txt_jam_akhir.getText().toString());
            }
        });

        FloatingActionButton fabButton2 = (FloatingActionButton) Timeline.this.findViewById(R.id.list_route_stop);
        fabButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
                ListRouteStop(device_id, date.getText().toString(), txt_jam_awal.getText().toString(), txt_jam_akhir.getText().toString());
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

    private void LoadMapTrack(final String device_id, final String tanggal, final String jam_awal, final String jam_akhir) {
        String ListTrackURL = server.URL2 + "gps/tracking_report2";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mMap.clear();
                hideDialog();
                Log.e(TAG, "Data Response: " + response.toString());
                PolylineOptions polylineOptions = new PolylineOptions();
                PolylineOptions polylineOptions2 = new PolylineOptions();
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    int count_trip = 0;
                    double startLat = 0;
                    double startLng = 0;
                    FrameLayout frameLayout = findViewById(R.id.mapFrame); // Replace with your actual FrameLayout ID
                    String jenis_kendaraan = "";
                    String nama_kendaraan = "";
                    Marker pointMarker = null;
                    List<LatLng> polylinePoints = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.getString("name");
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String devicetime = jsonObject.getString("devicetime");
                        String type = jsonObject.getString("type");
                        String category = jsonObject.getString("category");
                        int durasi = jsonObject.getInt("durasi");
                        String tag = jsonObject.getString("tag");
//                        points.add(new LatLng(latitude, longitude));
//                        if (i == jsonArray.length() - 1 || i == 0) {

                        jenis_kendaraan = category;
                        nama_kendaraan = name;

                        if (i == 0) {
                            startLat = latitude;
                            startLng = longitude;
                        }

                        polylinePoints.add(new LatLng(latitude, longitude));

                        String start_trip = "00:00";
                        String end_trip = "23:59";
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                        try {
                            Date date = dateFormat.parse(devicetime);

                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);

                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            int minute = calendar.get(Calendar.MINUTE);

                            if (i == 0) {
                                start_trip = hour + ":" + minute;
                            }

                            System.out.println("Hour: " + hour);
                            System.out.println("Minute: " + minute);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }



                        if (i == jsonArray.length() - 1 || durasi > 2) {
                            Geocoder geocoder = new Geocoder(Timeline.this, Locale.getDefault());
                            List<Address> addresses = null;
                            String address = "";

                            if (durasi >= 3 || i == jsonArray.length() - 1) {
                                try {
                                    addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                assert addresses != null;
                                if (!addresses.isEmpty()) {
                                    address = addresses.get(0).getAddressLine(0);
                                }

                            }

                            int resourceId;

                            if (i == jsonArray.length() - 1) {
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
                            } else {
                                if (durasi > 3) {
                                    resourceId = R.mipmap.stop_car;
                                } else {
                                    resourceId = R.mipmap.tfc_light;
                                }
                            }

                            // Create start marker
                            pointMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude, longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(resourceId))
                                    .title(name)
                                    .snippet(" (" + devicetime + ")" + address)
                                    .draggable(true)
                            );
                        }

                        if (!tag.equals("null")) {
                            count_trip++;
                            String trip = "";
                            int color = R.color.grey_40;

                            switch(count_trip) {
                                case 1:
                                    trip = "A";
                                    color = R.color.orange_500;
                                    break;
                                case 2:
                                    trip = "B";
                                    color = R.color.green_color;
                                    break;
                                case 3:
                                    trip = "C";
                                    color = R.color.red_purple;
                                    break;
                                case 4:
                                    trip = "D";
                                    color = R.color.yellow_color;
                                    break;
                            }

                            try {
                                Date date = dateFormat.parse(devicetime);

                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);

                                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                                int minute = calendar.get(Calendar.MINUTE);
                                end_trip = hour + ":" + minute;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            Button btn_trip = new Button(Timeline.this);
                            btn_trip.setText("Trip " + trip); // Set the text for the button
                            btn_trip.setBackgroundColor(getResources().getColor(color));
                            String finalStart_trip = start_trip;
                            String finalEnd_trip = end_trip;
                            btn_trip.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showDialog();
                                    LoadTrip(device_id, tanggal, finalStart_trip, finalEnd_trip);
                                    btn_trip.setBackgroundColor(getResources().getColor(R.color.grey_60));
                                }
                            });

                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.gravity = Gravity.END; // Set the position of the button

                            int mrgn_top = 16;
                            if (count_trip > 1) {
                                mrgn_top = 200;
                            }

                            params.setMargins(16, mrgn_top, 16, 16); // Set margins if needed

                            frameLayout.addView(btn_trip, params);
                        }

                        polylineOptions.add(new LatLng(latitude, longitude));
                        polylineOptions.clickable(true);
                        polylineOptions.color(Color.BLUE);
                        polylineOptions.width(12);
                        polylineOptions.jointType(JointType.ROUND);
//                        polyline.setTag(address);
                    }

                    Polyline polyline = mMap.addPolyline(polylineOptions);

                    // Create a circular play button
                    ImageButton playButton = new ImageButton(Timeline.this);
                    playButton.setImageResource(R.drawable.baseline_play_circle_48_violet); // Set your play button icon here
                    playButton.setBackgroundResource(android.R.color.transparent); // Remove background

                    // Set button size
                    int buttonSize = getResources().getDimensionPixelSize(R.dimen.button_bottom_height); // Define in dimensions resources
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);

                    // Set button position to center bottom
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
                    layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin); // Define in dimensions resources

                    // Apply layout parameters to the button
                    playButton.setLayoutParams(layoutParams);

                    int icon_marker;

                    switch (jenis_kendaraan) {
                        case "car":
                            icon_marker = R.mipmap.ic_car_yellow_front;
                            break;
                        case "motorcycle":
                            icon_marker = R.mipmap.motor1;
                            break;
                        case "truck":
                            icon_marker = R.mipmap.truck;
                            break;
                        default:
                            // Handle the default case or set a fallback BitmapDescriptor
                            icon_marker = R.mipmap.motor2;
                            break;
                    }

                    final Handler handler = new Handler();
                    final long startTime = SystemClock.uptimeMillis();
                    final int duration = 100000; // Total duration of animation in milliseconds

                    double finalStartLat = startLat;
                    double finalStartLng = startLng;
                    String finalNama_kendaraan = nama_kendaraan;
                    Marker finalPointMarker = pointMarker;
                    playButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            finalPointMarker.remove();

                            // Assuming you have a GoogleMap instance named "googleMap"
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(new LatLng(finalStartLat, finalStartLng))
                                    .icon(BitmapDescriptorFactory.fromResource(icon_marker))
                                    .title(finalNama_kendaraan);

                            final Marker movingMarker = mMap.addMarker(markerOptions);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    long elapsedTime = SystemClock.uptimeMillis() - startTime;
                                    float fraction = elapsedTime / (float) duration; // Fraction of elapsed time

                                    LatLng newPosition = interpolate(fraction, polylinePoints);
                                    movingMarker.setPosition(newPosition);

                                    if (fraction < 1.0f) {
                                        float rotationAngle = calculateBearing(newPosition, interpolate(fraction + 0.01f, polylinePoints)); // Adjust fraction to get next point
                                        movingMarker.setRotation(rotationAngle);

                                        handler.postDelayed(this, 16); // Update marker position and rotation every 16ms
                                    }
                                }
                            });
                        }
                    });

                    // Add the button to the FrameLayout
                    frameLayout.addView(playButton);

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
                params.put("jam_awal", jam_awal);
                params.put("jam_akhir", jam_akhir);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("X-CSRF-Token", csrfToken);
//                return headers;
//            }
        };

        stringRequest.setRetryPolicy(retryPolicy);
        AppController.getInstance(this).addToRequestQueue(stringRequest);
    }

    private float calculateBearing(LatLng startLatLng, LatLng endLatLng) {
        double startLat = Math.toRadians(startLatLng.latitude);
        double startLng = Math.toRadians(startLatLng.longitude);
        double endLat = Math.toRadians(endLatLng.latitude);
        double endLng = Math.toRadians(endLatLng.longitude);

        double deltaLng = endLng - startLng;

        double y = Math.sin(deltaLng) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(deltaLng);

        double bearing = Math.atan2(y, x);
        return (float) Math.toDegrees(bearing);
    }

    private LatLng interpolate(float fraction, List<LatLng> polylinePoints) {
        int numberOfPoints = polylinePoints.size() - 1;
        int index = (int) (fraction * numberOfPoints);

        // Ensure the index is within bounds
        if (index >= numberOfPoints) {
            return polylinePoints.get(numberOfPoints);
        }

        float remainder = fraction * numberOfPoints - index;

        LatLng start = polylinePoints.get(index);
        LatLng end = polylinePoints.get(index + 1);

        double lat = start.latitude + remainder * (end.latitude - start.latitude);
        double lng = start.longitude + remainder * (end.longitude - start.longitude);

        return new LatLng(lat, lng);
    }


    private void LoadTrip(final String device_id, final String tanggal, final String jam_awal, final String jam_akhir) {
        String ListTrackURL = server.URL2 + "gps/tracking_report2";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mMap.clear();
                hideDialog();
                Log.e(TAG, "Data Response: " + response.toString());
                PolylineOptions polylineOptions = new PolylineOptions();
                PolylineOptions polylineOptions2 = new PolylineOptions();
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
                        int durasi = jsonObject.getInt("durasi");
                        String tag = jsonObject.getString("tag");
//                        points.add(new LatLng(latitude, longitude));
//                        if (i == jsonArray.length() - 1 || i == 0) {
                        if (i == jsonArray.length() - 1 || durasi > 2) {
                            Geocoder geocoder = new Geocoder(Timeline.this, Locale.getDefault());
                            List<Address> addresses = null;
                            String address = "";
                            if (durasi >= 3 || i == jsonArray.length() - 1) {
                                try {
                                    addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                assert addresses != null;
                                if (!addresses.isEmpty()) {
                                    address = addresses.get(0).getAddressLine(0);
                                }
                            }

                            int resourceId;

                            if (i == jsonArray.length() - 1) {
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
                            } else {
                                if (durasi > 3) {
                                    resourceId = R.mipmap.stop_car;
                                } else {
                                    resourceId = R.mipmap.tfc_light;
                                }
                            }

                            // Create start marker
                            Marker pointMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude, longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(resourceId))
                                    .title(name)
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
                params.put("jam_awal", jam_awal);
                params.put("jam_akhir", jam_akhir);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("X-CSRF-Token", csrfToken);
//                return headers;
//            }
        };

        stringRequest.setRetryPolicy(retryPolicy);
        AppController.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void ListRoute(final String device_id, final String tanggal, final String jam_awal, final String jam_akhir) {
        String ListTrackURL = server.URL2 + "gps/list_route";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideDialog();
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    // Create a LinearLayout as the container for TextViews
                    LinearLayout linearLayout = new LinearLayout(Timeline.this);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.white));
                    int leftPadding = 16;
                    int topPadding = 8;
                    int rightPadding = 16;
                    int bottomPadding = 8;
                    linearLayout.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

                    ScrollView scrollView = new ScrollView(Timeline.this);
                    scrollView.addView(linearLayout);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String deviceid = jsonObject.getString("deviceid");
                        String name = jsonObject.getString("name");
                        int durasi = jsonObject.getInt("durasi");
                        String devicetime = jsonObject.getString("devicetime");

                        TextView textView = new TextView(Timeline.this);
                        textView.setTextColor(getResources().getColor(R.color.action_bar));

                        int RouteIcon;
                        String address = "";
                        if (durasi > 1 || i == jsonArray.length() - 1 || i == 0) {
                            switch (durasi) {
                                case 2:
                                    RouteIcon = R.drawable.baseline_near_me_24_green;
                                    break;
                                case 3:
                                    RouteIcon = R.drawable.baseline_traffic_24_red;
                                    break;
                                default:
                                    RouteIcon = R.drawable.baseline_do_not_disturb_on_total_silence_24_red;
                                    break;
                            }

                            Drawable drawable = getResources().getDrawable(RouteIcon);
                            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

                            Geocoder geocoder = new Geocoder(Timeline.this, Locale.getDefault());
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            assert addresses != null;
                            if (!addresses.isEmpty()) {
                                address = addresses.get(0).getAddressLine(0);
                            }

                            if (name != "null") {
                                textView.setText(" (" + devicetime + ") \r\n " + address + " \r\n Tag: " + name);
                            } else {
                                textView.setText(" (" + devicetime + ") \r\n " + address);
                            }
                            linearLayout.addView(textView);

                            View lineView = new View(Timeline.this);
                            int lineHeight = 2; // Set the desired height of the line in pixels
                            int lineColor = Color.BLACK; // Set the desired color of the line
                            // Set the width and height of the line
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    lineHeight
                            );
                            lineView.setLayoutParams(layoutParams);
                            lineView.setBackgroundColor(lineColor);
                            linearLayout.addView(lineView);
                        }
                    }

                    // Create an AlertDialog with the LinearLayout as the custom view
                    AlertDialog.Builder builder = new AlertDialog.Builder(Timeline.this);
                    builder.setTitle("Route Device");
                    builder.setView(scrollView);
                    builder.setPositiveButton("Close", null);
                    // Add any desired buttons or listeners
                    // Show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
                params.put("tanggal", tanggal);
                params.put("jam_awal", jam_awal);
                params.put("jam_akhir", jam_akhir);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("X-CSRF-Token", csrfToken);
//                return headers;
//            }
        };
        stringRequest.setRetryPolicy(retryPolicy);
        AppController.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void ListRouteStop(final String device_id, final String tanggal, final String jam_awal, final String jam_akhir) {
        String ListTrackURL = server.URL2 + "gps/list_route";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideDialog();
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    // Create a LinearLayout as the container for TextViews
                    LinearLayout linearLayout = new LinearLayout(Timeline.this);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.white));

                    ScrollView scrollView = new ScrollView(Timeline.this);
                    scrollView.addView(linearLayout);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        double latitude = jsonObject.getDouble("latitude");
                        double longitude = jsonObject.getDouble("longitude");
                        String deviceid = jsonObject.getString("deviceid");
                        String name = jsonObject.getString("name");
                        int durasi = jsonObject.getInt("durasi");
                        String devicetime = jsonObject.getString("devicetime");

                        TextView textView = new TextView(Timeline.this);
                        textView.setTextColor(getResources().getColor(R.color.action_bar));

                        int RouteIcon;
                        String address = "";
                        if (durasi > 3 || i == jsonArray.length() - 1 || i == 0) {
                            switch (durasi) {
                                case 3:
                                    RouteIcon = R.drawable.baseline_near_me_24_green;
                                    break;
                                default:
                                    RouteIcon = R.drawable.baseline_do_not_disturb_on_total_silence_24_red;
                                    break;
                            }

                            Drawable drawable = getResources().getDrawable(RouteIcon);
                            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

                            Geocoder geocoder = new Geocoder(Timeline.this, Locale.getDefault());
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            assert addresses != null;
                            if (!addresses.isEmpty()) {
                                address = addresses.get(0).getAddressLine(0);
                            }

                            if (name != "null") {
                                textView.setText(" (" + devicetime + ") \r\n " + address + " \r\n Tag: " + name);
                            } else {
                                textView.setText(" (" + devicetime + ") \r\n " + address);
                            }
                            linearLayout.addView(textView);

                            View lineView = new View(Timeline.this);
                            int lineHeight = 2; // Set the desired height of the line in pixels
                            int lineColor = Color.BLACK; // Set the desired color of the line
                            // Set the width and height of the line
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    lineHeight
                            );
                            lineView.setLayoutParams(layoutParams);
                            lineView.setBackgroundColor(lineColor);
                            linearLayout.addView(lineView);
                        }
                    }

                    // Create an AlertDialog with the LinearLayout as the custom view
                    AlertDialog.Builder builder = new AlertDialog.Builder(Timeline.this);
                    builder.setTitle("Route Stop");
                    builder.setView(scrollView);
                    builder.setPositiveButton("Close", null);
                    // Add any desired buttons or listeners
                    // Show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
                params.put("tanggal", tanggal);
                params.put("jam_awal", jam_awal);
                params.put("jam_akhir", jam_akhir);
                return params;
            }

//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("X-CSRF-Token", csrfToken);
//                return headers;
//            }
        };
        stringRequest.setRetryPolicy(retryPolicy);
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