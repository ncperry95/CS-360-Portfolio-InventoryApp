package com.snhu.inventoryapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.snhu.inventoryapp.ui.DeleteConfirmationDialog;
import com.snhu.inventoryapp.ui.ItemDetailViewModel;

/**
 * Detail screen for one item. Renders ItemDetailViewModel and, when it says a
 * low stock line was crossed, asks SmsHelper to send the text. The threshold
 * math and the ownership check are no longer in here.
 */
public class ItemDetailActivity extends AppCompatActivity {

    private ItemDetailViewModel viewModel;
    private TextView textName;
    private TextView textQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        long itemId = getIntent().getLongExtra(InventoryGridActivity.EXTRA_ITEM_ID, -1);
        if (itemId == -1) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(InventoryApplication.containerOf(this)))
                .get(ItemDetailViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        textName = findViewById(R.id.text_item_name);
        textQuantity = findViewById(R.id.text_quantity);

        MaterialButton buttonIncrease = findViewById(R.id.button_increase);
        MaterialButton buttonDecrease = findViewById(R.id.button_decrease);
        MaterialButton buttonDelete = findViewById(R.id.button_delete);

        buttonIncrease.setOnClickListener(v -> viewModel.increase());
        buttonDecrease.setOnClickListener(v -> viewModel.decrease());
        buttonDelete.setOnClickListener(v -> {
            String name = textName.getText() == null ? "" : textName.getText().toString();
            DeleteConfirmationDialog.show(this, name, viewModel::delete);
        });

        viewModel.getItem().observe(this, item -> {
            textName.setText(item.getName());
            textQuantity.setText(String.valueOf(item.getQuantity()));
        });

        viewModel.getLowStockAlert().observe(this, event -> {
            ItemDetailViewModel.LowStockAlert alert = event.getContentIfNotHandled();
            if (alert != null) {
                SmsHelper.sendLowStockAlert(this, alert.itemName, alert.quantity);
            }
        });

        viewModel.getMessage().observe(this, event -> {
            ItemDetailViewModel.Message msg = event.getContentIfNotHandled();
            if (msg == null) {
                return;
            }
            switch (msg) {
                case ITEM_NOT_FOUND:
                    Toast.makeText(this, R.string.error_item_not_found, Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_FAILED:
                    Toast.makeText(this, R.string.error_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case SESSION_EXPIRED:
                    Toast.makeText(this, R.string.error_session_expired, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getCloseScreen().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                finish();
            }
        });

        viewModel.load(itemId);
    }
}
