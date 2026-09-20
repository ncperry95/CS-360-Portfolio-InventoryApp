package com.snhu.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.snhu.inventoryapp.ui.DeleteConfirmationDialog;
import com.snhu.inventoryapp.ui.InventoryViewModel;

/**
 * Main screen after login. Renders whatever InventoryViewModel publishes and
 * forwards taps to it. The user id is no longer passed in through the Intent;
 * the ViewModel reads it from the session.
 */
public class InventoryGridActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "com.snhu.inventoryapp.ITEM_ID";

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_grid);

        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(InventoryApplication.containerOf(this)))
                .get(InventoryViewModel.class);

        // No session means this screen was reached without logging in.
        if (!viewModel.isLoggedIn()) {
            returnToLogin();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_inventory);
        }

        textEmpty = findViewById(R.id.text_empty);
        RecyclerView recycler = findViewById(R.id.recycler_inventory);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new InventoryAdapter(new InventoryAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(InventoryItem item) {
                Intent intent = new Intent(InventoryGridActivity.this, ItemDetailActivity.class);
                intent.putExtra(EXTRA_ITEM_ID, item.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(InventoryItem item) {
                DeleteConfirmationDialog.show(InventoryGridActivity.this, item.getName(),
                        () -> viewModel.deleteItem(item));
            }
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_item);
        fab.setOnClickListener(v -> showAddItemDialog());

        viewModel.getItems().observe(this, items -> {
            adapter.setItems(items);
            textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(this, event -> {
            InventoryViewModel.Message msg = event.getContentIfNotHandled();
            if (msg == null) {
                return;
            }
            switch (msg) {
                case ADD_FAILED:
                    Toast.makeText(this, R.string.error_add_failed, Toast.LENGTH_SHORT).show();
                    break;
                case DELETE_FAILED:
                    Toast.makeText(this, R.string.error_item_not_found, Toast.LENGTH_SHORT).show();
                    break;
                case SESSION_EXPIRED:
                    returnToLogin();
                    break;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The detail screen may have changed a quantity or deleted the item.
        viewModel.refresh();
    }

    // -------------------- Toolbar menu --------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sms_settings) {
            startActivity(new Intent(this, SmsSettingsActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            viewModel.logout();
            returnToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -------------------- Add item dialog --------------------

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null);

        TextInputEditText editName = dialogView.findViewById(R.id.edit_item_name);
        TextInputEditText editQuantity = dialogView.findViewById(R.id.edit_starting_quantity);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.button_cancel);
        MaterialButton buttonSave = dialogView.findViewById(R.id.button_save);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_InventoryApp_Dialog)
                .setView(dialogView)
                .create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonSave.setOnClickListener(v -> {
            String name = readText(editName);
            String quantityText = readText(editQuantity);

            InventoryViewModel.FormError error =
                    InventoryViewModel.validateNewItem(name, quantityText);
            switch (error) {
                case NAME_REQUIRED:
                    editName.setError(getString(R.string.error_required));
                    return;
                case QUANTITY_REQUIRED:
                    editQuantity.setError(getString(R.string.error_required));
                    return;
                case QUANTITY_NOT_A_NUMBER:
                    editQuantity.setError(getString(R.string.error_must_be_number));
                    return;
                case QUANTITY_NEGATIVE:
                    editQuantity.setError(getString(R.string.error_quantity_negative));
                    return;
                case NONE:
                    break;
            }

            viewModel.addItem(name, quantityText);
            dialog.dismiss();
        });

        dialog.show();
    }

    private static String readText(TextInputEditText field) {
        Editable text = field.getText();
        return text == null ? "" : text.toString().trim();
    }

    // -------------------- Logout helper --------------------

    private void returnToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
