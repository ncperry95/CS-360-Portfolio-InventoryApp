package com.snhu.inventoryapp.data;

/**
 * Receives the result of a repository call. Invoked on the repository's
 * background thread, never on the main thread, so callers (the ViewModels)
 * hand the value to LiveData with postValue rather than touching views.
 */
public interface RepositoryCallback<T> {
    void onResult(T result);
}
