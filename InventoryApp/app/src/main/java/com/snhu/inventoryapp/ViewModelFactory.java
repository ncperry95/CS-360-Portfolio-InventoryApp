package com.snhu.inventoryapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.inventoryapp.ui.InventoryViewModel;
import com.snhu.inventoryapp.ui.ItemDetailViewModel;
import com.snhu.inventoryapp.ui.LoginViewModel;

/**
 * Builds ViewModels with their repositories injected from the AppContainer.
 * ViewModels take constructor arguments, so the default provider cannot
 * create them; this factory is the one place that knows how.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final AppContainer container;

    public ViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(
                    container.getUserRepository(), container.getSessionManager());
        }
        if (modelClass.isAssignableFrom(InventoryViewModel.class)) {
            return (T) new InventoryViewModel(
                    container.getInventoryRepository(), container.getSessionManager());
        }
        if (modelClass.isAssignableFrom(ItemDetailViewModel.class)) {
            return (T) new ItemDetailViewModel(
                    container.getInventoryRepository(), container.getSessionManager());
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
