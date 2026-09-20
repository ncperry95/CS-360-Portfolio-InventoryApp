package com.snhu.inventoryapp.ui;

import androidx.annotation.Nullable;

/**
 * Wrapper for a LiveData value that should be acted on once.
 *
 * LiveData re delivers its last value to every new observer, which is what
 * you want for "the current list of items" and exactly wrong for "navigate to
 * the inventory screen" or "show this toast": a rotation would replay it.
 * Observers call {@link #getContentIfNotHandled()} and get null the second
 * time.
 */
public final class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
