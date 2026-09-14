package com.snhu.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * First screen the user sees. Lets an existing user log in or a new user
 * create an account. Credentials are checked against the SQLite database
 * through DatabaseHelper.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private TextView textError;

    // Database helper used for both login and account creation
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Grab the input fields and the error label so we can read and update them
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        textError = findViewById(R.id.text_error);

        // Open (or create) the database
        databaseHelper = new DatabaseHelper(this);

        // Hook up the two buttons to their handlers
        MaterialButton buttonLogin = findViewById(R.id.button_login);
        MaterialButton buttonCreateAccount = findViewById(R.id.button_create_account);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        buttonCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCreateAccount();
            }
        });
    }

    /**
     * Tries to log in with whatever is in the two fields.
     * On success, opens the inventory screen. On failure, shows an error.
     */
    private void handleLogin() {
        String username = getText(editUsername);
        String password = getText(editPassword);

        // Both fields are required
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError("Please enter a username and password.");
            return;
        }

        // Ask the database if the credentials match
        long userId = databaseHelper.checkLogin(username, password);
        if (userId == -1) {
            showError("Invalid username or password.");
            return;
        }

        // Good login. Open the inventory grid for this user.
        goToInventory(userId);
    }

    /**
     * Creates a new account with whatever is in the two fields, then opens
     * the inventory screen for the new user.
     */
    private void handleCreateAccount() {
        String username = getText(editUsername);
        String password = getText(editPassword);

        // Both fields are required
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError("Please enter a username and password.");
            return;
        }

        // Reject a username that is already in use
        if (databaseHelper.userExists(username)) {
            showError("That username is already taken.");
            return;
        }

        // Create the user. createUser returns the new row id, or -1 on failure.
        long newUserId = databaseHelper.createUser(username, password);
        if (newUserId == -1) {
            showError("Could not create account. Please try again.");
            return;
        }

        // Account created. Drop them into their empty inventory.
        goToInventory(newUserId);
    }

    /**
     * Starts the inventory grid for the given user and finishes this screen
     * so the back button does not bring the user back to the login form.
     */
    private void goToInventory(long userId) {
        Intent intent = new Intent(this, InventoryGridActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        startActivity(intent);
        finish();
    }

    /**
     * Helper to pull a trimmed string out of a TextInputEditText.
     */
    private String getText(TextInputEditText field) {
        if (field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }

    /**
     * Shows an error message in the red text view below the password field.
     */
    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    // Key used to pass the logged in user's id to other activities
    public static final String EXTRA_USER_ID = "com.snhu.inventoryapp.USER_ID";
}