package com.snhu.inventoryapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Settings screen for SMS alerts.
 *
 * Shows the current permission status, lets the user enter a phone number
 * for alerts, and asks Android for SEND_SMS permission when the user taps
 * Enable. If permission is denied, the screen says so and the rest of the
 * app keeps working without SMS.
 */
public class SmsSettingsActivity extends AppCompatActivity {

    private TextView textStatus;
    private TextInputEditText editPhoneNumber;

    /**
     * Modern way to request a runtime permission. The lambda runs after
     * the system dialog closes, with the user's choice.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    new androidx.activity.result.ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean granted) {
                            // Permission state may have changed, so refresh the status text
                            updateStatusText();
                            // Give the user a quick confirmation either way
                            if (granted) {
                                Toast.makeText(SmsSettingsActivity.this,
                                        "SMS alerts enabled.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SmsSettingsActivity.this,
                                        "SMS alerts disabled. The app will still work without them.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_settings);

        // Toolbar back arrow returns to the inventory grid
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Grab the views we will read and update
        textStatus = findViewById(R.id.text_status);
        editPhoneNumber = findViewById(R.id.edit_phone_number);

        // Pre fill the phone number field with whatever was saved before
        editPhoneNumber.setText(SmsHelper.getSavedPhoneNumber(this));

        // Show the current permission state
        updateStatusText();

        // Enable button asks Android for SEND_SMS permission
        MaterialButton buttonEnable = findViewById(R.id.button_enable_sms);
        buttonEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleEnableClick();
            }
        });

        // Save button stores whatever phone number is in the field
        MaterialButton buttonSave = findViewById(R.id.button_save);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSaveClick();
            }
        });
    }

    /**
     * Reads the current permission state and updates the status label.
     * Called on screen load and after every permission request.
     */
    private void updateStatusText() {
        if (SmsHelper.isSmsPermissionGranted(this)) {
            textStatus.setText(R.string.status_permission_granted);
        } else if (hasAskedBefore()) {
            textStatus.setText(R.string.status_permission_denied);
        } else {
            textStatus.setText(R.string.status_permission_not_set);
        }
    }

    /**
     * Tracks whether we have ever asked for permission, so the status text
     * can say "denied" instead of "not yet requested" after a denial.
     */
    private boolean hasAskedBefore() {
        return getSharedPreferences("sms_prefs", MODE_PRIVATE)
                .getBoolean("asked_before", false);
    }

    private void markAskedBefore() {
        getSharedPreferences("sms_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("asked_before", true)
                .apply();
    }

    /**
     * Handles the Enable SMS Alerts button.
     * If permission is already granted, just confirms it. Otherwise, kicks
     * off the system permission dialog through the launcher above.
     */
    private void handleEnableClick() {
        if (SmsHelper.isSmsPermissionGranted(this)) {
            Toast.makeText(this, "SMS alerts are already enabled.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        markAskedBefore();
        requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
    }

    /**
     * Handles the Save button. Stores the phone number for future alerts.
     * The phone number is required, but permission is not, so the user
     * can save a number even before granting permission.
     */
    private void handleSaveClick() {
        String number = editPhoneNumber.getText() == null
                ? ""
                : editPhoneNumber.getText().toString().trim();

        if (TextUtils.isEmpty(number)) {
            editPhoneNumber.setError("Please enter a phone number.");
            return;
        }

        SmsHelper.savePhoneNumber(this, number);
        Toast.makeText(this, "Phone number saved.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Older permission callback. Not used by the launcher above (the lambda
     * handles results), but Android Studio will sometimes generate code
     * that calls this. Left as a no op for safety.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}