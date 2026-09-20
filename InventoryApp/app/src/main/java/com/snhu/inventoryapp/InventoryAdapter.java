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
 *
 * Still does a full notifyDataSetChanged on setItems. Replacing that with
 * DiffUtil is part of Enhancement Two, where the list gets search and sort.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onItemClick(InventoryItem item);

        void onDeleteClick(InventoryItem item);
    }

    private final List<InventoryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public InventoryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

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
        holder.textName.setText(item.getName());
        holder.textQuantity.setText(String.valueOf(item.getQuantity()));
        holder.card.setOnClickListener(v -> listener.onItemClick(item));
        holder.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
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
