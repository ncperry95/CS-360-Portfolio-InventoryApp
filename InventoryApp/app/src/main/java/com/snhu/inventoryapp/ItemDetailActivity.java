package com.snhu.inventoryapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

/**
 * Detail screen for a single inventory item. Lets the user adjust the
 * quantity up or down, or delete the item entirely. All changes are
 * written to the database right away so the grid shows fresh data
 * when the user returns.
 *
 * After a quantity change, this screen also checks whether the item just
 * crossed below the low stock threshold and, if so, asks SmsHelper to
 * send an alert. SmsHelper handles all the permission and phone number
 * checks, so this screen never has to care whether SMS is set up.
 */
public class ItemDetailActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private long itemId;

    // UI references we update as the quantity changes
    private TextView textName;
    private TextView textQuantity;

    // Local copy of the current item so we can do the math without
    // hitting the database on every button tap, and so we can pass
    // the item name into the SMS message.
    private String currentName;
    private int currentQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Pull the item id from the intent. If something went wrong and we
        // do not have one, just close the screen.
        itemId = getIntent().getLongExtra(InventoryGridActivity.EXTRA_ITEM_ID, -1);
        if (itemId == -1) {
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        // Set up the toolbar so the back arrow finishes this screen and
        // returns to the inventory grid.
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Grab the views we will update
        textName = findViewById(R.id.text_item_name);
        textQuantity = findViewById(R.id.text_quantity);

        // Load the item from the database and fill in the screen
        loadItem();

        // Plus button: bump the quantity up by 1
        MaterialButton buttonIncrease = findViewById(R.id.button_increase);
        buttonIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeQuantity(currentQuantity + 1);
            }
        });

        // Minus button: bring the quantity down by 1, but never below zero
        MaterialButton buttonDecrease = findViewById(R.id.button_decrease);
        buttonDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentQuantity > 0) {
                    changeQuantity(currentQuantity - 1);
                }
            }
        });

        // Delete button: confirm first, then delete and return to the grid
        MaterialButton buttonDelete = findViewById(R.id.button_delete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    /**
     * Loads the item record fresh from the database and fills the screen.
     * If the row has somehow disappeared (deleted elsewhere), we just close
     * the screen instead of crashing.
     */
    private void loadItem() {
        InventoryItem item = databaseHelper.getItem(itemId);
        if (item == null) {
            finish();
            return;
        }
        currentName = item.getName();
        currentQuantity = item.getQuantity();
        textName.setText(currentName);
        textQuantity.setText(String.valueOf(currentQuantity));
    }

    /**
     * Writes the new quantity to the database, updates the on screen text,
     * and fires a low stock SMS alert if we just crossed the threshold.
     *
     * The "just crossed" check looks at the value before and after the
     * change. If the quantity was at or above the threshold before and is
     * below it after, we send one alert. We do not send another until the
     * user brings it back up above the threshold and drops it again, which
     * avoids spamming the user with a text on every minus tap.
     */
    private void changeQuantity(int newQuantity) {
        int previousQuantity = currentQuantity;

        databaseHelper.updateQuantity(itemId, newQuantity);
        currentQuantity = newQuantity;
        textQuantity.setText(String.valueOf(currentQuantity));

        boolean justCrossedThreshold =
                previousQuantity >= SmsHelper.LOW_STOCK_THRESHOLD
                        && newQuantity < SmsHelper.LOW_STOCK_THRESHOLD;

        if (justCrossedThreshold) {
            // SmsHelper handles its own permission and phone number checks,
            // so this screen never has to care whether SMS is configured.
            SmsHelper.sendLowStockAlert(this, currentName, newQuantity);
        }
    }

    /**
     * Shows a confirmation dialog before deleting. On confirm, removes the
     * item from the database and closes this screen so the grid refreshes.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete item")
                .setMessage("Remove this item from inventory?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        databaseHelper.deleteItem(itemId);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}