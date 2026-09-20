package com.snhu.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.snhu.inventoryapp.auth.AuthResult;
import com.snhu.inventoryapp.ui.LoginViewModel;

/**
 * First screen. Collects a username and password and hands them to
 * LoginViewModel. Everything about hashing, policy, and the database lives
 * below the ViewModel; this class only reads fields and shows results.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private TextView textError;
    private MaterialButton buttonLogin;
    private MaterialButton buttonCreateAccount;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        textError = findViewById(R.id.text_error);
        buttonLogin = findViewById(R.id.button_login);
        buttonCreateAccount = findViewById(R.id.button_create_account);

        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(InventoryApplication.containerOf(this)))
                .get(LoginViewModel.class);

        buttonLogin.setOnClickListener(v ->
                viewModel.login(readText(editUsername), readPassword()));
        buttonCreateAccount.setOnClickListener(v ->
                viewModel.createAccount(readText(editUsername), readPassword()));

        viewModel.isBusy().observe(this, busy -> {
            boolean enabled = busy == null || !busy;
            buttonLogin.setEnabled(enabled);
            buttonCreateAccount.setEnabled(enabled);
        });

        viewModel.getAuthResult().observe(this, event -> {
            AuthResult result = event.getContentIfNotHandled();
            if (result == null) {
                return;
            }
            if (result.isSuccess()) {
                goToInventory();
            } else {
                showError(errorMessage(result.getError()));
            }
        });
    }

    private void goToInventory() {
        startActivity(new Intent(this, InventoryGridActivity.class));
        finish();
    }

    /** Maps an auth error code to the string shown under the password field. */
    @StringRes
    private static int errorMessage(AuthResult.Error error) {
        switch (error) {
            case FIELDS_REQUIRED:
                return R.string.error_fields_required;
            case INVALID_CREDENTIALS:
                return R.string.error_invalid_login;
            case USERNAME_TAKEN:
                return R.string.error_username_taken;
            case USERNAME_TOO_LONG:
                return R.string.error_username_too_long;
            case PASSWORD_TOO_SHORT:
                return R.string.error_password_too_short;
            case PASSWORD_TOO_LONG:
                return R.string.error_password_too_long;
            case PASSWORD_NO_LETTER:
                return R.string.error_password_no_letter;
            case PASSWORD_NO_DIGIT:
                return R.string.error_password_no_digit;
            case STORAGE_FAILED:
            default:
                return R.string.error_create_failed;
        }
    }

    private static String readText(TextInputEditText field) {
        Editable text = field.getText();
        return text == null ? "" : text.toString().trim();
    }

    /**
     * Passwords travel as char[] so the repository can zero them after use.
     * Not trimmed: a leading or trailing space is part of the password.
     */
    private char[] readPassword() {
        Editable text = editPassword.getText();
        if (text == null) {
            return new char[0];
        }
        char[] out = new char[text.length()];
        text.getChars(0, text.length(), out, 0);
        return out;
    }

    private void showError(@StringRes int message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }
}
