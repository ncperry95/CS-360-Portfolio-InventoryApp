package com.snhu.inventoryapp;

import android.Manifest;
import android.os.Bundle;
import android.text.Editable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Settings screen for SMS alerts. Shows permission status, stores the phone
 * number, and requests SEND_SMS. All preference access goes through SmsHelper
 * now; this class no longer knows the preferences file name.
 */
public class SmsSettingsActivity extends AppCompatActivity {

    private TextView textStatus;
    private TextInputEditText editPhoneNumber;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateStatusText();
                Toast.makeText(this,
                        granted ? R.string.toast_sms_enabled : R.string.toast_sms_disabled,
                        granted ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        textStatus = findViewById(R.id.text_status);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        editPhoneNumber.setText(SmsHelper.getSavedPhoneNumber(this));

        updateStatusText();

        MaterialButton buttonEnable = findViewById(R.id.button_enable_sms);
        buttonEnable.setOnClickListener(v -> handleEnableClick());

        MaterialButton buttonSave = findViewById(R.id.button_save);
        buttonSave.setOnClickListener(v -> handleSaveClick());
    }

    private void updateStatusText() {
        if (SmsHelper.isSmsPermissionGranted(this)) {
            textStatus.setText(R.string.status_permission_granted);
        } else if (SmsHelper.hasAskedBefore(this)) {
            textStatus.setText(R.string.status_permission_denied);
        } else {
            textStatus.setText(R.string.status_permission_not_set);
        }
    }

    private void handleEnableClick() {
        if (SmsHelper.isSmsPermissionGranted(this)) {
            Toast.makeText(this, R.string.toast_sms_already_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        SmsHelper.markAskedBefore(this);
        requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
    }

    private void handleSaveClick() {
        Editable text = editPhoneNumber.getText();
        String number = text == null ? "" : text.toString().trim();
        if (number.isEmpty()) {
            editPhoneNumber.setError(getString(R.string.error_phone_required));
            return;
        }
        SmsHelper.savePhoneNumber(this, number);
        Toast.makeText(this, R.string.toast_phone_saved, Toast.LENGTH_SHORT).show();
    }
}
