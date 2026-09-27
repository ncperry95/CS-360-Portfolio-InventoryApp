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
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.snhu.inventoryapp.domain.ItemSortOrder;
import com.snhu.inventoryapp.ui.DeleteConfirmationDialog;
import com.snhu.inventoryapp.ui.InventoryViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen after login. Renders whatever InventoryViewModel publishes and
 * forwards taps to it. The user id is no longer passed in through the Intent;
 * the ViewModel reads it from the session.
 *
 * Search text, sort choice and the low stock filter are forwarded to the
 * ViewModel and nothing is filtered or sorted here. The grid gets its list
 * through ListAdapter.submitList so DiffUtil works out what changed.
 */
public class InventoryGridActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "com.snhu.inventoryapp.ITEM_ID";

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private TextView textEmpty;
    private TextView textLowStock;

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
        textLowStock = findViewById(R.id.text_low_stock);
        textLowStock.setOnClickListener(v -> viewModel.toggleLowStockOnly());

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
            adapter.submitList(items);
            if (items.isEmpty()) {
                textEmpty.setText(viewModel.isFiltered()
                        ? R.string.empty_filtered
                        : R.string.empty_inventory);
                textEmpty.setVisibility(View.VISIBLE);
            } else {
                textEmpty.setVisibility(View.GONE);
            }
        });

        viewModel.getLowStockSummary().observe(this, this::renderLowStockBanner);

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

    // -------------------- Low stock banner --------------------

    private void renderLowStockBanner(InventoryViewModel.LowStockSummary summary) {
        if (summary == null || summary.isEmpty()) {
            textLowStock.setVisibility(View.GONE);
            if (viewModel.isLowStockOnly()) {
                // Nothing is low any more; drop the filter so the grid is not stuck empty.
                viewModel.setLowStockOnly(false);
            }
            return;
        }
        if (viewModel.isLowStockOnly()) {
            textLowStock.setText(R.string.low_stock_showing_only);
        } else {
            List<String> entries = new ArrayList<>(summary.lowest.size());
            for (InventoryItem item : summary.lowest) {
                entries.add(getString(R.string.low_stock_entry, item.getName(), item.getQuantity()));
            }
            String joined = String.join(", ", entries);
            int more = summary.total - summary.lowest.size();
            textLowStock.setText(more > 0
                    ? getString(R.string.low_stock_banner_more, joined, more)
                    : getString(R.string.low_stock_banner, joined));
        }
        textLowStock.setVisibility(View.VISIBLE);
    }

    // -------------------- Toolbar menu --------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inventory, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String text) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    viewModel.setQuery(text);
                    return true;
                }
            });

            // Rotation keeps the ViewModel but rebuilds the menu, so put the
            // search text back where the user left it.
            String current = viewModel.getQuery();
            if (!current.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(current, false);
                searchView.clearFocus();
            }
        }

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                viewModel.setQuery("");
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int checkedId;
        switch (viewModel.getSortOrder()) {
            case QUANTITY_ASC:
                checkedId = R.id.sort_quantity;
                break;
            case NEWEST_FIRST:
                checkedId = R.id.sort_newest;
                break;
            case NAME_ASC:
            default:
                checkedId = R.id.sort_name;
                break;
        }
        MenuItem checked = menu.findItem(checkedId);
        if (checked != null) {
            checked.setChecked(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_name) {
            item.setChecked(true);
            viewModel.setSortOrder(ItemSortOrder.NAME_ASC);
            return true;
        }
        if (id == R.id.sort_quantity) {
            item.setChecked(true);
            viewModel.setSortOrder(ItemSortOrder.QUANTITY_ASC);
            return true;
        }
        if (id == R.id.sort_newest) {
            item.setChecked(true);
            viewModel.setSortOrder(ItemSortOrder.NEWEST_FIRST);
            return true;
        }
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
