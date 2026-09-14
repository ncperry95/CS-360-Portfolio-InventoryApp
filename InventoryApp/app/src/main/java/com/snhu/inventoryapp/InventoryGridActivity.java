package com.snhu.inventoryapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Main screen after login. Shows the user's inventory as a grid of cards,
 * lets the user add new items through a dialog, delete items, and open
 * the detail screen for any item. The toolbar menu offers SMS Settings
 * and Log Out.
 */
public class InventoryGridActivity extends AppCompatActivity {

    // Key used to pass an item id to the detail screen
    public static final String EXTRA_ITEM_ID = "com.snhu.inventoryapp.ITEM_ID";

    private DatabaseHelper databaseHelper;
    private long currentUserId;

    private InventoryAdapter adapter;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_grid);

        // Get the user id passed from the login screen. If we don't have one,
        // something went wrong, so bounce back to the login screen.
        currentUserId = getIntent().getLongExtra(LoginActivity.EXTRA_USER_ID, -1);
        if (currentUserId == -1) {
            returnToLogin();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        // Wire up the toolbar so it shows the three-dot menu
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_inventory);
        }

        // Set up the empty-state text and the grid itself
        textEmpty = findViewById(R.id.text_empty);
        RecyclerView recycler = findViewById(R.id.recycler_inventory);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new InventoryAdapter(actionListener);
        recycler.setAdapter(adapter);

        // The floating action button opens the add-item dialog
        FloatingActionButton fab = findViewById(R.id.fab_add_item);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the grid every time the screen comes back into view, so any
        // quantity change made on the detail screen shows up here.
        refreshItems();
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
            returnToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -------------------- Grid loading --------------------

    private void refreshItems() {
        List<InventoryItem> items = databaseHelper.getInventoryForUser(currentUserId);
        adapter.setItems(items);
        // Show the "no items yet" message only when the list is empty
        textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // -------------------- Adapter callbacks --------------------

    private final InventoryAdapter.OnItemActionListener actionListener =
            new InventoryAdapter.OnItemActionListener() {
                @Override
                public void onItemClick(InventoryItem item) {
                    // Open the detail screen for the tapped item
                    Intent intent = new Intent(
                            InventoryGridActivity.this, ItemDetailActivity.class);
                    intent.putExtra(EXTRA_ITEM_ID, item.getId());
                    startActivity(intent);
                }

                @Override
                public void onDeleteClick(InventoryItem item) {
                    confirmDelete(item);
                }
            };

    // -------------------- Add item dialog --------------------

    private void showAddItemDialog() {
        // Inflate the custom dialog layout so we can use the buttons we designed
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_item, null);

        final TextInputEditText editName = dialogView.findViewById(R.id.edit_item_name);
        final TextInputEditText editQuantity = dialogView.findViewById(R.id.edit_starting_quantity);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.button_cancel);
        MaterialButton buttonSave = dialogView.findViewById(R.id.button_save);

        // Wrap the custom view in an AlertDialog so it pops up modally
        final AlertDialog dialog = new AlertDialog.Builder(
                this, R.style.Theme_InventoryApp_Dialog)
                .setView(dialogView)
                .create();

        // Cancel just closes the dialog
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Save validates the input, writes to the database, and refreshes the grid
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText() == null
                        ? ""
                        : editName.getText().toString().trim();
                String quantityText = editQuantity.getText() == null
                        ? ""
                        : editQuantity.getText().toString().trim();

                // Both fields are required
                if (name.isEmpty()) {
                    editName.setError("Required");
                    return;
                }
                if (quantityText.isEmpty()) {
                    editQuantity.setError("Required");
                    return;
                }

                // Make sure quantity is actually a whole number
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityText);
                } catch (NumberFormatException e) {
                    editQuantity.setError("Must be a number");
                    return;
                }

                databaseHelper.addItem(currentUserId, name, quantity);
                dialog.dismiss();
                refreshItems();
            }
        });

        dialog.show();
    }

    // -------------------- Delete confirmation --------------------

    private void confirmDelete(final InventoryItem item) {
        new AlertDialog.Builder(this, R.style.Theme_InventoryApp_Dialog)
                .setTitle("Delete item")
                .setMessage("Remove \"" + item.getName() + "\" from inventory?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        databaseHelper.deleteItem(item.getId());
                        refreshItems();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // -------------------- Logout helper --------------------

    /**
     * Sends the user back to the login screen and clears the back stack so
     * pressing Back from the login screen does not return here.
     */
    private void returnToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}