package com.snhu.inventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that fills the inventory grid with one card per item.
 * The activity supplies a listener so taps on the card and on the
 * trash icon can be handled separately.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    /**
     * The activity implements this to react to taps on cards and trash icons.
     */
    public interface OnItemActionListener {
        void onItemClick(InventoryItem item);
        void onDeleteClick(InventoryItem item);
    }

    private final List<InventoryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public InventoryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current list and refreshes the grid.
     */
    public void setItems(List<InventoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
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
        final InventoryItem item = items.get(position);

        // Fill in the text views for this card
        holder.textName.setText(item.getName());
        holder.textQuantity.setText(String.valueOf(item.getQuantity()));

        // Tapping anywhere on the card (other than the trash icon) opens the detail screen
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(item);
            }
        });

        // The small trash icon removes the item
        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Holds the views for a single inventory cell so we don't have to call
     * findViewById every time the cell scrolls into view.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView textName;
        final TextView textQuantity;
        final ImageButton buttonDelete;

        ViewHolder(View itemView) {
            super(itemView);
            // The cell layout's root view is the MaterialCardView itself
            card = (MaterialCardView) itemView;
            textName = itemView.findViewById(R.id.text_item_name);
            textQuantity = itemView.findViewById(R.id.text_quantity);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}