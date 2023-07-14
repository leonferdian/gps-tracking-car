package com.gps.cartracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gps.cartracker.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.gps.cartracker.ui.home.HomeFragment;
import com.gps.cartracker.ui.report.ReportFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView name;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    SharedPreferences sharedpreferences;

    private boolean doubleBackToExitPressedOnce = false;
    private static final int DOUBLE_BACK_EXIT_INTERVAL = 2000; // 2 seconds
    private static final int REQUEST_CODE_PERMISSION = 123;

    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancellationSignal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        NavigationView nav_View = (NavigationView) findViewById(R.id.nav_view);
        View headerView = nav_View.getHeaderView(0);
        name = (TextView) headerView.findViewById(R.id.user_name);

        sharedpreferences = getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedpreferences.getBoolean(LoginActivity.session_status, false);
        boolean BiometricActivated = sharedpreferences.getBoolean(SettingsActivity.biometric_lock, false);
        String get_email = sharedpreferences.getString("name", null);
        String user_id = sharedpreferences.getString("user_id", "");
        name.setText(get_email);

        changeStatusBarColor();
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(MainActivity.this, ListDevicePosition.class);
                intent.putExtra("user_id", user_id);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_report)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (!isLoggedIn) {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(LoginActivity.session_status, false);
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        if (BiometricActivated) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the necessary permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_CODE_PERMISSION);
            } else {
                // Initialize the fingerprint authentication
                initFingerprintAuth();
            }
        }

        //get menu items
        Menu menu = nav_View.getMenu();
        MenuItem logout = menu.findItem(R.id.nav_logout);
        MenuItem home = menu.findItem(R.id.nav_home);
        MenuItem report = menu.findItem(R.id.nav_report);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        binding.appBarMain.bottomNavigation.findViewById(R.id.navigation_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeFragment homeFragment = new HomeFragment();
                openFragment(homeFragment);
                bottomNavigationView.getMenu().findItem(R.id.navigation_home).setChecked(true);
                home.setChecked(true);
                getSupportActionBar().setTitle("Home");
            }
        });

        binding.appBarMain.bottomNavigation.findViewById(R.id.navigation_report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportFragment reportFragment = new ReportFragment();
                openFragment(reportFragment);
                bottomNavigationView.getMenu().findItem(R.id.navigation_report).setChecked(true);
                report.setChecked(true);
                getSupportActionBar().setTitle("Track Report");
            }
        });

        //Logout
        logout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showLogoutDialog();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem action_settings = menu.findItem(R.id.action_settings);
        action_settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // TODO Auto-generated method stub
                // Open setting page
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        });
        return true;
    }

    public void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Call your logout function here
                logout();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(LoginActivity.session_status, false);
        editor.clear();
        editor.apply();
        finish();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        Toast.makeText(MainActivity.this, "Log Out Successfully", Toast.LENGTH_LONG).show();
    }

    private void initFingerprintAuth() {
        fingerprintManager = FingerprintManagerCompat.from(this);

        // Check if the device has fingerprint hardware and if the user has enrolled fingerprints
        if (!fingerprintManager.isHardwareDetected()) {
            // Fingerprint hardware is not available
            Toast.makeText(this, "Fingerprint hardware not detected", Toast.LENGTH_SHORT).show();
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            // User has not enrolled any fingerprints
            Toast.makeText(this, "No fingerprints enrolled", Toast.LENGTH_SHORT).show();
        } else {
            // Start fingerprint authentication
            startFingerprintAuth();
        }
    }

    private void startFingerprintAuth() {
        cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(null, 0, cancellationSignal,
                new FingerprintManagerCompat.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errMsgId, CharSequence errString) {
                        // Authentication error occurred
                        Toast.makeText(MainActivity.this,
                                "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                        // Authentication help message
                        Toast.makeText(MainActivity.this,
                                "Authentication help: " + helpString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                        // Authentication succeeded
                        Toast.makeText(MainActivity.this, "Authentication succeeded",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Authentication failed
                        Toast.makeText(MainActivity.this, "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel fingerprint authentication if it's in progress
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.setStatusBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar));
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            exitApplication();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, DOUBLE_BACK_EXIT_INTERVAL);
    }



    private void exitApplication() {
        // Clear the session or perform any necessary cleanup
        // Clear shared preferences or perform any other logout-related actions

        // Finish all activities and exit the application
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        System.exit(0);
    }
}