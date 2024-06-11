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
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {
    public static final String biometric_lock = "biometric_lock";
    public static final String authority_lock = "authority_lock";
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
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences sharedpreferences;
        private SharedPreferences.Editor editor;
        private FingerprintManagerCompat fingerprintManager;
        private CancellationSignal cancellationSignal;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            sharedpreferences = getActivity().getSharedPreferences(LoginActivity.my_shared_preferences, Context.MODE_PRIVATE);

            String user_id = sharedpreferences.getString("user_id", "");
            PreferenceCategory advanceCategory = findPreference("advance");
            if (!"8".equals(user_id)) {
                if (advanceCategory != null) {
                    advanceCategory.setVisible(false);
                }
            }

            Context context = getContext();
            if (context != null) {
                editor = sharedpreferences.edit();
            }

            SwitchPreferenceCompat authorityModePreference = findPreference("authority_mode");
            if (authorityModePreference != null) {
                authorityModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isOn = (Boolean) newValue;
                        if (isOn) {
                            // Action when authority_mode is turned on
                            performActionOn();
                        } else {
                            // Action when authority_mode is turned off
                            performActionOff();
                        }
                        return true; // Save the preference change
                    }
                });
            }

            SwitchPreferenceCompat fingerLockModePreference = findPreference("finger_lock");
            if (fingerLockModePreference != null) {
                fingerLockModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isOn = (Boolean) newValue;
                        if (isOn) {
                            initFingerprintAuth();
                        } else {
                            editor.putBoolean(biometric_lock, false);
                            editor.apply();
                        }
                        return true; // Save the preference change
                    }
                });
            }
        }

        private void performActionOn() {
            // Add your action when authority_mode is turned on
            // For example, show a toast
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "Authority mode is ON", Toast.LENGTH_SHORT).show();
            }

            if (editor != null) {
                editor.putBoolean(authority_lock, true);
                editor.apply();
            }
        }

        private void performActionOff() {
            // Add your action when authority_mode is turned off
            // For example, show a toast
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "Authority mode is OFF", Toast.LENGTH_SHORT).show();
            }

            if (editor != null) {
                editor.putBoolean(authority_lock, false);
                editor.apply();
            }
        }

        private void initFingerprintAuth() {
            fingerprintManager = FingerprintManagerCompat.from(requireContext());

            // Check if the device has fingerprint hardware and if the user has enrolled fingerprints
            if (!fingerprintManager.isHardwareDetected()) {
                // Fingerprint hardware is not available
                Toast.makeText(getContext(), "Fingerprint hardware not detected", Toast.LENGTH_SHORT).show();
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                // User has not enrolled any fingerprints
                Toast.makeText(getContext(), "No fingerprints enrolled", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getContext(),
                                    "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            // Authentication help message
                            Toast.makeText(getContext(),
                                    "Authentication help: " + helpString, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                            // Authentication succeeded
                            Toast.makeText(getContext(), "Authentication succeeded",
                                    Toast.LENGTH_SHORT).show();
                            editor.putBoolean("biometric_lock", true);
                            editor.apply();
                            SwitchPreferenceCompat fingerLockModePreference = findPreference("finger_lock");
                            if (fingerLockModePreference != null) {
                                fingerLockModePreference.setChecked(true);
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            // Authentication failed
                            Toast.makeText(getContext(), "Authentication failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        }
    }
}