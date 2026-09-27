package com.snhu.inventoryapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.auth.SessionManager;
import com.snhu.inventoryapp.data.InventoryRepository;
import com.snhu.inventoryapp.domain.InventoryQuery;
import com.snhu.inventoryapp.domain.ItemSortOrder;
import com.snhu.inventoryapp.domain.LowStockQueue;
import com.snhu.inventoryapp.search.NameTrie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen state for the inventory grid.
 *
 * Holds the item list across rotation, validates the add item form, and
 * turns repository results into one shot messages. The Activity has no
 * business logic left.
 *
 * Search, sort and low stock live here too. The full list from the database
 * is kept once; on top of it sit a prefix index (search), a min heap on
 * quantity (low stock), the current search text, the chosen sort order and
 * the low stock filter. Any change to those recomputes the visible list
 * through InventoryQuery and publishes it. The database is only hit when
 * the data itself changes, never when the user types or picks a sort.
 */
public class InventoryViewModel extends ViewModel {

    /** How many names the low stock banner lists before saying "and N more". */
    public static final int BANNER_ITEMS = 3;

    /** Validation outcome for the add item dialog. The Activity maps to strings. */
    public enum FormError {
        NONE,
        NAME_REQUIRED,
        QUANTITY_REQUIRED,
        QUANTITY_NOT_A_NUMBER,
        QUANTITY_NEGATIVE
    }

    /** One shot messages the grid screen can show. */
    public enum Message {
        ADD_FAILED,
        DELETE_FAILED,
        SESSION_EXPIRED
    }

    /** What the low stock banner shows: the most urgent few and the total count. */
    public static final class LowStockSummary {
        public final List<InventoryItem> lowest;
        public final int total;

        LowStockSummary(List<InventoryItem> lowest, int total) {
            this.lowest = lowest;
            this.total = total;
        }

        public boolean isEmpty() {
            return total == 0;
        }
    }

    private final InventoryRepository repository;
    private final SessionManager sessionManager;

    /** Guards everything below it. Loads arrive on the database thread. */
    private final Object lock = new Object();
    private final List<InventoryItem> allItems = new ArrayList<>();
    private final NameTrie index = new NameTrie();
    private final LowStockQueue lowStock = new LowStockQueue();
    private String query = "";
    private ItemSortOrder sortOrder = ItemSortOrder.NAME_ASC;
    private boolean lowStockOnly = false;

    private final MutableLiveData<List<InventoryItem>> items = new MutableLiveData<>();
    private final MutableLiveData<LowStockSummary> lowStockSummary = new MutableLiveData<>();
    private final MutableLiveData<Event<Message>> message = new MutableLiveData<>();

    public InventoryViewModel(InventoryRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public LiveData<List<InventoryItem>> getItems() {
        return items;
    }

    public LiveData<LowStockSummary> getLowStockSummary() {
        return lowStockSummary;
    }

    public LiveData<Event<Message>> getMessage() {
        return message;
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void logout() {
        sessionManager.logout();
    }

    // -------------------- Search, sort, low stock --------------------

    public String getQuery() {
        synchronized (lock) {
            return query;
        }
    }

    /** Called on every keystroke. Filters in memory; no database work. */
    public void setQuery(String text) {
        String next = text == null ? "" : text;
        synchronized (lock) {
            if (next.equals(query)) {
                return;
            }
            query = next;
            publishLocked();
        }
    }

    public ItemSortOrder getSortOrder() {
        synchronized (lock) {
            return sortOrder;
        }
    }

    public void setSortOrder(ItemSortOrder order) {
        synchronized (lock) {
            if (order == sortOrder) {
                return;
            }
            sortOrder = order;
            publishLocked();
        }
    }

    public boolean isLowStockOnly() {
        synchronized (lock) {
            return lowStockOnly;
        }
    }

    public void setLowStockOnly(boolean enabled) {
        synchronized (lock) {
            if (enabled == lowStockOnly) {
                return;
            }
            lowStockOnly = enabled;
            publishLocked();
        }
    }

    public void toggleLowStockOnly() {
        synchronized (lock) {
            lowStockOnly = !lowStockOnly;
            publishLocked();
        }
    }

    /** True when the visible list is narrowed by search or the low stock filter. */
    public boolean isFiltered() {
        synchronized (lock) {
            return lowStockOnly || !InventoryQuery.isBlank(query);
        }
    }

    // -------------------- Data --------------------

    public void refresh() {
        if (!requireSession()) {
            return;
        }
        repository.loadInventory(sessionManager.getUserId(), this::replaceAll);
    }

    /** Runs on the database thread. Rebuilds the index and the heap, then publishes. */
    private void replaceAll(List<InventoryItem> loaded) {
        synchronized (lock) {
            allItems.clear();
            allItems.addAll(loaded);
            index.clear();
            for (InventoryItem item : loaded) {
                index.insert(item.getName(), item.getId());
            }
            lowStock.rebuild(loaded);
            publishLocked();
        }
    }

    /** Must hold {@link #lock}. Uses postValue so it is safe from any thread. */
    private void publishLocked() {
        List<InventoryItem> base = lowStockOnly ? lowStock.lowStockItems() : allItems;
        items.postValue(InventoryQuery.apply(base, index, query, sortOrder));

        List<InventoryItem> lowest = lowStock.hasLowStock()
                ? lowStock.lowestLowStock(BANNER_ITEMS)
                : Collections.<InventoryItem>emptyList();
        lowStockSummary.postValue(new LowStockSummary(lowest, lowStock.lowStockCount()));
    }

    /**
     * Checks the form without touching the database so the dialog can show
     * inline errors synchronously. Returns NONE when the input is usable.
     */
    public static FormError validateNewItem(String name, String quantityText) {
        if (name == null || name.trim().isEmpty()) {
            return FormError.NAME_REQUIRED;
        }
        if (quantityText == null || quantityText.trim().isEmpty()) {
            return FormError.QUANTITY_REQUIRED;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText.trim());
        } catch (NumberFormatException e) {
            return FormError.QUANTITY_NOT_A_NUMBER;
        }
        if (quantity < 0) {
            return FormError.QUANTITY_NEGATIVE;
        }
        return FormError.NONE;
    }

    /** Call only after {@link #validateNewItem} returned NONE. */
    public void addItem(String name, String quantityText) {
        if (!requireSession()) {
            return;
        }
        int quantity = Integer.parseInt(quantityText.trim());
        repository.addItem(sessionManager.getUserId(), name.trim(), quantity, newId -> {
            if (newId == -1) {
                message.postValue(new Event<>(Message.ADD_FAILED));
            }
            refresh();
        });
    }

    public void deleteItem(InventoryItem item) {
        if (!requireSession()) {
            return;
        }
        repository.deleteItem(item.getId(), sessionManager.getUserId(), deleted -> {
            if (!deleted) {
                message.postValue(new Event<>(Message.DELETE_FAILED));
            }
            refresh();
        });
    }

    private boolean requireSession() {
        if (sessionManager.isLoggedIn()) {
            return true;
        }
        message.postValue(new Event<>(Message.SESSION_EXPIRED));
        return false;
    }
}
