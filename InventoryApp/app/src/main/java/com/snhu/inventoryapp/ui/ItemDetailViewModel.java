package com.snhu.inventoryapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.inventoryapp.InventoryItem;
import com.snhu.inventoryapp.auth.SessionManager;
import com.snhu.inventoryapp.data.InventoryRepository;
import com.snhu.inventoryapp.domain.LowStockRule;

/**
 * Screen state for one item. Owns the current quantity, the low stock
 * crossing check, and the delete, so the Activity only renders and forwards
 * the SMS request (which needs a Context) to SmsHelper.
 *
 * Every write goes through the owner scoped repository call. If the row is
 * missing or belongs to someone else the write reports false and the screen
 * closes with a message instead of silently pretending it worked.
 */
public class ItemDetailViewModel extends ViewModel {

    public enum Message {
        ITEM_NOT_FOUND,
        UPDATE_FAILED,
        SESSION_EXPIRED
    }

    /** Payload for a low stock alert: what to text about. */
    public static final class LowStockAlert {
        public final String itemName;
        public final int quantity;

        LowStockAlert(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    private final InventoryRepository repository;
    private final SessionManager sessionManager;

    private final MutableLiveData<InventoryItem> item = new MutableLiveData<>();
    private final MutableLiveData<Event<Message>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<LowStockAlert>> lowStockAlert = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeScreen = new MutableLiveData<>();

    private long itemId = -1;

    public ItemDetailViewModel(InventoryRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public LiveData<InventoryItem> getItem() {
        return item;
    }

    public LiveData<Event<Message>> getMessage() {
        return message;
    }

    public LiveData<Event<LowStockAlert>> getLowStockAlert() {
        return lowStockAlert;
    }

    public LiveData<Event<Boolean>> getCloseScreen() {
        return closeScreen;
    }

    /** Loads the item once; a rotation reuses the ViewModel and skips this. */
    public void load(long itemId) {
        if (this.itemId == itemId && item.getValue() != null) {
            return;
        }
        this.itemId = itemId;
        if (!requireSession()) {
            return;
        }
        repository.getItem(itemId, sessionManager.getUserId(), found -> {
            if (found == null) {
                message.postValue(new Event<>(Message.ITEM_NOT_FOUND));
                closeScreen.postValue(new Event<>(true));
            } else {
                item.postValue(found);
            }
        });
    }

    public void increase() {
        InventoryItem current = item.getValue();
        if (current != null) {
            changeQuantity(current, current.getQuantity() + 1);
        }
    }

    public void decrease() {
        InventoryItem current = item.getValue();
        if (current != null && current.getQuantity() > 0) {
            changeQuantity(current, current.getQuantity() - 1);
        }
    }

    public void delete() {
        if (!requireSession()) {
            return;
        }
        repository.deleteItem(itemId, sessionManager.getUserId(), deleted -> {
            if (!deleted) {
                message.postValue(new Event<>(Message.ITEM_NOT_FOUND));
            }
            closeScreen.postValue(new Event<>(true));
        });
    }

    /**
     * Applies the new quantity optimistically on the main thread so rapid taps
     * each see the latest value, then writes it. If the write reports no row
     * changed, the optimistic value is rolled back and the screen closes.
     */
    private void changeQuantity(InventoryItem current, int newQuantity) {
        if (!requireSession()) {
            return;
        }
        int previous = current.getQuantity();
        item.setValue(new InventoryItem(current.getId(), current.getName(), newQuantity));

        repository.updateQuantity(itemId, sessionManager.getUserId(), newQuantity, updated -> {
            if (!updated) {
                item.postValue(current);
                message.postValue(new Event<>(Message.UPDATE_FAILED));
                closeScreen.postValue(new Event<>(true));
                return;
            }
            if (LowStockRule.crossedBelowThreshold(previous, newQuantity)) {
                lowStockAlert.postValue(new Event<>(
                        new LowStockAlert(current.getName(), newQuantity)));
            }
        });
    }

    private boolean requireSession() {
        if (sessionManager.isLoggedIn()) {
            return true;
        }
        message.postValue(new Event<>(Message.SESSION_EXPIRED));
        closeScreen.postValue(new Event<>(true));
        return false;
    }
}
