package com.gps.cartracker.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.gps.cartracker.DevicePosition;
import com.gps.cartracker.LoginActivity;
import com.gps.cartracker.R;
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
    String ListTrackURL = server.URL2 + "gps/list_device_user";
    private static final String TAG = HomeFragment.class.getSimpleName();
    public static final String device_id = "device_id";
    private FragmentHomeBinding binding;
    private Handler handler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 120 * 1000; // 2 minute in milliseconds
    ConnectivityManager conMgr;

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
        list_car(user_id);
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
        list_car(user_id);
    }

    private void list_car(final String user_id) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ListTrackURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Data Response: " + response.toString());
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String id = jsonObject.getString("id");
                        String name = jsonObject.getString("name");
                        String status = jsonObject.getString("status");
                        String lastupdate = jsonObject.getString("lastupdate");
                        String category = jsonObject.getString("category");
                        String positionid = jsonObject.getString("positionid");

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

                        Date now = new Date();
                        long days = TimeUnit.MILLISECONDS.toDays(now.getTime());
                        long hours = TimeUnit.MILLISECONDS.toHours(now.getTime()) % 24;
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime()) % 60;

                        if (!lastupdate.equals("null")) {
                            try {

                                // Parse the datetime string into a Date object
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date date = format.parse(lastupdate);

                                // Calculate the time difference in milliseconds
                                long diff = now.getTime() - date.getTime();

                                // Add 7 hours in milliseconds to the diff variable
                                // diff -= 7 * 60 * 60 * 1000;

                                // Calculate the days, hours, and minutes
                                days = TimeUnit.MILLISECONDS.toDays(diff);
                                hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                                minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

                                // Now you have the day, hour, and minute values
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
                                // TODO Auto-generated method stub
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

        AppController.getInstance(getContext()).addToRequestQueue(stringRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}