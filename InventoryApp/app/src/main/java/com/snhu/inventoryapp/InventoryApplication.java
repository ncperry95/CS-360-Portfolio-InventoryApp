package com.snhu.inventoryapp;

import android.app.Application;
import android.content.Context;

/**
 * Application subclass whose only job is to own the {@link AppContainer}.
 * Registered in the manifest with android:name=".InventoryApplication".
 */
public class InventoryApplication extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }

    /** Convenience for Activities: any Context reaches the container. */
    public static AppContainer containerOf(Context context) {
        return ((InventoryApplication) context.getApplicationContext()).getContainer();
    }
}
