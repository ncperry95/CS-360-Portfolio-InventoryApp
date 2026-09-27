package com.snhu.inventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.snhu.inventoryapp.domain.LowStockRule;

/**
 * Adapter that fills the inventory grid with one card per item.
 *
 * Extends ListAdapter so every new list goes through DiffUtil on a
 * background thread. The original called notifyDataSetChanged on every
 * change, which throws away every bound view and rebinds all of them even
 * when one quantity moved by one. DiffUtil compares the old and new lists
 * (O(n) in practice, with a worst case of O(n + d^2) where d is the edit
 * distance) and dispatches only the inserts, removes and changes that
 * actually happened. That is what makes search feel instant and restores
 * the RecyclerView item animations for free.
 */
public class InventoryAdapter extends ListAdapter<InventoryItem, InventoryAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onItemClick(InventoryItem item);

        void onDeleteClick(InventoryItem item);
    }

    /**
     * Two entries are the same item when their row ids match. Their
     * contents are the same when name and quantity match, which is exactly
     * InventoryItem.equals once the ids are known to be equal.
     */
    static final DiffUtil.ItemCallback<InventoryItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<InventoryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull InventoryItem oldItem,
                                               @NonNull InventoryItem newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull InventoryItem oldItem,
                                                  @NonNull InventoryItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnItemActionListener listener;

    public InventoryAdapter(OnItemActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_cell, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final InventoryItem item = getItem(position);
        holder.textName.setText(item.getName());
        holder.textQuantity.setText(String.valueOf(item.getQuantity()));

        int color = LowStockRule.isLowStock(item.getQuantity()) ? R.color.error : R.color.primary;
        holder.textQuantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));

        holder.card.setOnClickListener(v -> listener.onItemClick(item));
        holder.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textName;
        final TextView textQuantity;
        final ImageButton buttonDelete;

        ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            textName = itemView.findViewById(R.id.text_item_name);
            textQuantity = itemView.findViewById(R.id.text_quantity);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}
