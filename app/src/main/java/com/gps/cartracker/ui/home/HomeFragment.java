package com.gps.cartracker.ui.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.gps.cartracker.DevicePosition;
import com.gps.cartracker.LoginActivity;
import com.gps.cartracker.R;
import com.gps.cartracker.SettingsActivity;
import com.gps.cartracker.databinding.FragmentHomeBinding;
import com.gps.cartracker.util.AppController;
import com.gps.cartracker.util.server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {
    LinearLayout linearLayout;
    String url;
    String ListTrackURL = server.URL2 + "gps/list_device_user";
    String ListTrackURL2 = server.URL2 + "gps/list_device";
    String url_version = server.URL2 + "gps/check_version";
    private static final String TAG = HomeFragment.class.getSimpleName();
    private FragmentHomeBinding binding;
    private Handler handler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 120 * 1000; // 2 minute in milliseconds
    int initialTimeoutMs = 10000; // Initial timeout in milliseconds
    int maxNumRetries = 3; // Maximum number of retries
    float backoffMultiplier = 1.5f; // Backoff multiplier for exponential backoff
    RetryPolicy retryPolicy = new DefaultRetryPolicy(initialTimeoutMs, maxNumRetries, backoffMultiplier);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
//        HomeViewModel homeViewModel =
//                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        linearLayout = root.findViewById(R.id.linearLayout);
        // Initialize the handler and runnable
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateData();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
        String user_id = sharedPreferences.getString("user_id", "");
        boolean authority_lock = sharedPreferences.getBoolean(SettingsActivity.authority_lock, false);

        if (authority_lock) {
            url = ListTrackURL2;
        } else {
            url = ListTrackURL;
        }

        list_car(user_id, url);
        check_version();
//        final TextView textView = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateData() {
//        Intent intent = getActivity().getIntent();
//        getActivity().finish();
//        startActivity(intent);
        linearLayout.removeAllViews();
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
        String user_id = sharedPreferences.getString("user_id", "");
        list_car(user_id, url);
    }

    private void list_car(final String user_id, final String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response);
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String id = jsonObject.optString("id", "N/A");
                        String name = jsonObject.optString("name", "Unknown");
                        String status = jsonObject.optString("status", "Unknown");
                        String lastupdate = jsonObject.optString("lastupdate", "null");
                        String category = jsonObject.optString("category", "unknown");

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
                                resourceId = R.mipmap.motor2;
                                break;
                        }

                        Date now = new Date();
                        long days = TimeUnit.MILLISECONDS.toDays(now.getTime());
                        long hours = TimeUnit.MILLISECONDS.toHours(now.getTime()) % 24;
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime()) % 60;

                        if (!lastupdate.equals("null")) {
                            try {
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date date = format.parse(lastupdate);
                                long diff = now.getTime() - date.getTime();
                                days = TimeUnit.MILLISECONDS.toDays(diff);
                                hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                                minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

                                System.out.println("Days: " + days);
                                System.out.println("Hours: " + hours);
                                System.out.println("Minutes: " + minutes);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        Button button = new Button(getContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                        params.width = 800;
                        button.setLayoutParams(params);
                        button.setCompoundDrawablesWithIntrinsicBounds(resourceId, 0, 0, 0);
                        button.setTextSize(10);
                        button.setText(" " + name + " (" + status + ") " + "\r\n" + days + " day " + hours + " hour " + minutes + " minute");
                        button.setTextColor(getResources().getColor(R.color.white));
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(getActivity(), DevicePosition.class);
                                intent.putExtra("device_id", id);
                                startActivity(intent);
                            }
                        });
                        linearLayout.addView(button);
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
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Connection error!", Toast.LENGTH_LONG).show();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userid", user_id);
                return params;
            }
        };
        stringRequest.setRetryPolicy(retryPolicy);
        AppController.getInstance(getContext()).addToRequestQueue(stringRequest);
    }

    private void check_version() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url_version, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response);
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String UpdateVersion = jsonObject.optString("version", "0");
                        String InstalledVersion = getString(R.string.name_version);

                        if (!InstalledVersion.equals(UpdateVersion)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Update Available");
                            builder.setMessage("A new version of the app is available. Tap to update.");

                            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Get the entered name from the EditText
                                    String url = "https://play.google.com/store/apps/details?id=com.gps.cartracker";

                                    // Create an intent to open the URL
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(intent);
                                }
                            });

                            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Handle the cancellation or dismiss the dialog
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();

                            // Get the buttons and apply styles
                            Button updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            Button closeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                            // Apply custom styles
                            updateButton.setTextColor(Color.WHITE);
                            updateButton.setBackgroundResource(R.drawable.blue_sky_button);
                            closeButton.setTextColor(Color.WHITE);
                            closeButton.setBackgroundResource(R.drawable.red_button);
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
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "Connection error!", Toast.LENGTH_LONG).show();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
//                params.put("userid", user_id);
                return params;
            }
        };
        stringRequest.setRetryPolicy(retryPolicy);
        AppController.getInstance(getContext()).addToRequestQueue(stringRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}