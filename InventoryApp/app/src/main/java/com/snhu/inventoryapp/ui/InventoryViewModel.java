package com.snhu.inventoryapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.auth.SessionManager;
import com.snhu.inventoryapp.data.InventoryRepository;

import java.util.List;

/**
 * Screen state for the inventory grid.
 *
 * Holds the item list across rotation (the original re queried the whole
 * table in onResume), validates the add item form, and turns repository
 * results into one shot messages. The Activity has no business logic left.
 */
public class InventoryViewModel extends ViewModel {

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

    private final InventoryRepository repository;
    private final SessionManager sessionManager;

    private final MutableLiveData<List<InventoryItem>> items = new MutableLiveData<>();
    private final MutableLiveData<Event<Message>> message = new MutableLiveData<>();

    public InventoryViewModel(InventoryRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public LiveData<List<InventoryItem>> getItems() {
        return items;
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

    public void refresh() {
        if (!requireSession()) {
            return;
        }
        repository.loadInventory(sessionManager.getUserId(), items::postValue);
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
