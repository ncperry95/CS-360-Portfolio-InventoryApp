package com.snhu.inventoryapp.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.snhu.inventoryapp.R;

/**
 * The one delete confirmation dialog. The original had two copies with
 * slightly different wording and one of them ignored the app's dialog theme.
 */
public final class DeleteConfirmationDialog {

    private DeleteConfirmationDialog() {
    }

    public static void show(Context context, String itemName, Runnable onConfirm) {
        new AlertDialog.Builder(context, R.style.Theme_InventoryApp_Dialog)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(context.getString(R.string.dialog_delete_message, itemName))
                .setPositiveButton(R.string.button_delete_confirm, (d, which) -> onConfirm.run())
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }
}
