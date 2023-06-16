package com.gps.cartracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.gps.cartracker.util.AppController;
import com.gps.cartracker.util.server;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    ProgressDialog pDialog;
    ConnectivityManager conMgr;
    private final String TAG = LoginActivity.class.getSimpleName();
    private final String TAG_MESSAGE = "message";
    private final String TAG_SUCCESS = "success";
    private final String TAG_EMAIL = "email";
    private String HttpURL   = server.URL3;
    Button btn_login;
    EditText txt_server, txt_username, txt_password;
    Boolean CheckEditText ;
    String ServerHolder, PasswordHolder, EmailHolder;
    SharedPreferences sharedpreferences;
    public static final String session_status = "session_status";
    public static final String my_shared_preferences = "gpstrack_shared_preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        changeStatusBarColor();

        txt_server = findViewById(R.id.txt_server);
        txt_username = findViewById(R.id.txt_username);
        txt_password = findViewById(R.id.txt_password);
        btn_login = findViewById(R.id.btn_login);

        txt_server.setHintTextColor(getResources().getColor(R.color.JotFormDark));

        sharedpreferences = getSharedPreferences(my_shared_preferences, Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedpreferences.getBoolean(session_status, false);

        if (isLoggedIn) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        {
            if (conMgr.getActiveNetworkInfo() != null
                    && conMgr.getActiveNetworkInfo().isAvailable()
                    && conMgr.getActiveNetworkInfo().isConnected()) {
            } else {
                Toast.makeText(getApplicationContext(), "No Internet Connection",
                        Toast.LENGTH_LONG).show();
            }
        }

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String server = txt_server.getText().toString();
                String username = txt_username.getText().toString();
                String password = txt_password.getText().toString();

                CheckEditTextIsEmptyOrNot();

                if(CheckEditText) {
                    if (conMgr.getActiveNetworkInfo() != null
                            && conMgr.getActiveNetworkInfo().isAvailable()
                            && conMgr.getActiveNetworkInfo().isConnected()) {
                        login(ServerHolder, EmailHolder, PasswordHolder);
                    } else {
                        Toast.makeText(getApplicationContext() ,"No Internet Connection", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(LoginActivity.this, "all fields cannot be empty!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void CheckEditTextIsEmptyOrNot(){
        ServerHolder = txt_server.getText().toString();
        EmailHolder = txt_username.getText().toString();
        PasswordHolder = txt_password.getText().toString();

        if(TextUtils.isEmpty(EmailHolder) || TextUtils.isEmpty(PasswordHolder) || TextUtils.isEmpty(ServerHolder))
        {
            CheckEditText = false;
        }
        else {

            CheckEditText = true ;
        }
    }

    private void login(final String server, final String email, final String password) {

        String URL_server = "https://" + server + "/api/session";
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.setMessage("Checking...");
        showDialog();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_server, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Login Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
//                    int success = jObj.getInt(TAG_SUCCESS);
                    // Check for error node in json
//                    if (success == 1) {
                    if (jObj.length() > 0) {
                        String user_id = jObj.getString("id");
                        String user_email = jObj.getString(TAG_EMAIL);
                        String name = jObj.getString("name");
                        finish();

                        // menyimpan login ke session
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putBoolean(session_status, true);
                        editor.putString("user_id", user_id);
                        editor.putString("email", user_email);
                        editor.putString("name", name);
                        editor.putString("server", server);
                        editor.putString("password", password);
                        editor.commit();

                        // Memanggil main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("user_id", user_id);
                        intent.putExtra("email", user_email);
                        intent.putExtra("name", name);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), jObj.getString(TAG_MESSAGE), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.getMessage() != null) {
                    Log.e(TAG, "Login Error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(),error.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),"Connection Error try again", Toast.LENGTH_LONG).show();
                }
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
//                params.put("_token", csrfToken);
//                params.put("server", server);
                params.put("email", email);
                params.put("password", password);
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

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.setStatusBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(getResources().getColor(R.color.cyan_100));
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Exit Application").setMessage("Are You Sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        Toast.makeText(LoginActivity.this, "See you next time",Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("No", null).show();
    }
}