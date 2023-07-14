package com.gps.cartracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity
        implements Preference.OnPreferenceChangeListener {
    SharedPreferences sharedpreferences;
    public static final String biometric_lock = "biometric_lock";
    private static final int REQUEST_CODE_PERMISSION = 123;

    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancellationSignal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        sharedpreferences = getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("attachment")) {
            boolean attachmentEnabled = (boolean) newValue;
            // Perform the action based on the attachmentEnabled value
            if (attachmentEnabled) {
                // Attachment is enabled
                initFingerprintAuth();
            } else {
                // Attachment is disabled
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(biometric_lock, false);
            }
            return true; // Return true to update the preference value
        }
        return false;
    }

    private void AddBiometric() {
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

        if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
            // Biometric authentication is supported on this device
            // Proceed with the authentication setup
            // Create a BiometricPrompt instance
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(SettingsActivity.this)
                        .setTitle("Biometric Authentication")
                        .setSubtitle("Use your biometric credentials to login")
                        .setDescription("Place your finger on the fingerprint sensor")
                        .setNegativeButton("Cancel", SettingsActivity.this.getMainExecutor(), (dialog, which) -> {
                            // Biometric authentication was canceled by the user
                            // Handle the cancellation
                            Toast.makeText(SettingsActivity.this, "Biometric authentication was canceled by the user", Toast.LENGTH_LONG).show();
                        })
                        .build();

                BiometricPrompt.AuthenticationCallback authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        // Authentication error occurred
                        // Handle the error, such as showing an error message
                        Toast.makeText(SettingsActivity.this, "Authentication error occurred", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        // Non-fatal error occurred during authentication
                        // Handle the help message, such as showing a hint or guidance
                        Toast.makeText(SettingsActivity.this, "Non-fatal error occurred during authentication", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        // Authentication succeeded
                        // Proceed with the authenticated logic, such as logging in the user
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putBoolean(biometric_lock, true);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Authentication failed
                        // Handle the failed authentication, such as showing an error message
                        Toast.makeText(SettingsActivity.this, "Authentication Failed", Toast.LENGTH_LONG).show();
                    }
                };
            }
        } else {
            // Biometric authentication is not supported on this device
            // Handle the scenario accordingly
            Toast.makeText(SettingsActivity.this, "Biometric authentication is supported on this device", Toast.LENGTH_LONG).show();
        }

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
                        Toast.makeText(SettingsActivity.this,
                                "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                        // Authentication help message
                        Toast.makeText(SettingsActivity.this,
                                "Authentication help: " + helpString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                        // Authentication succeeded
                        Toast.makeText(SettingsActivity.this, "Authentication succeeded",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Authentication failed
                        Toast.makeText(SettingsActivity.this, "Authentication failed",
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
}