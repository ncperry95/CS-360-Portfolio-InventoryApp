package com.snhu.inventoryapp.domain;

/**
 * The one business rule in the app: when does an item count as low stock, and
 * when does a quantity change deserve an alert.
 *
 * In the original code the threshold constant lived in SmsHelper with a
 * comment that said "at or below" while the comparison in ItemDetailActivity
 * used strictly below. This class is the single source of truth for both, and
 * the SMS settings screen text ("drops below 5") matches it.
 */
public final class LowStockRule {

    /** Quantities strictly below this number are low stock. */
    public static final int THRESHOLD = 5;

    private LowStockRule() {
    }

    /** True when the given quantity is below the threshold. */
    public static boolean isLowStock(int quantity) {
        return quantity < THRESHOLD;
    }

    /**
     * True only when a change moves an item from not low to low. A second
     * decrement while already low does not fire again, so the user gets one
     * text per dip instead of one per tap.
     */
    public static boolean crossedBelowThreshold(int previousQuantity, int newQuantity) {
        return !isLowStock(previousQuantity) && isLowStock(newQuantity);
    }
}
