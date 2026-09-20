package com.snhu.inventoryapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.inventoryapp.auth.AuthResult;
import com.snhu.inventoryapp.auth.SessionManager;
import com.snhu.inventoryapp.data.UserRepository;

/**
 * Screen state for the login screen. The Activity calls {@link #login} or
 * {@link #createAccount}, observes {@link #getAuthResult()}, and renders.
 * On success the session is already populated by the time the event fires.
 */
public class LoginViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<Event<AuthResult>> authResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    public LoginViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public LiveData<Event<AuthResult>> getAuthResult() {
        return authResult;
    }

    /** True while a hash is being computed, so the buttons can be disabled. */
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    public void login(String username, char[] password) {
        busy.setValue(true);
        userRepository.login(username, password, this::deliver);
    }

    public void createAccount(String username, char[] password) {
        busy.setValue(true);
        userRepository.createAccount(username, password, this::deliver);
    }

    /** Runs on the repository thread, hence postValue. */
    private void deliver(AuthResult result) {
        if (result.isSuccess()) {
            sessionManager.login(result.getUserId(), result.getUsername());
        }
        busy.postValue(false);
        authResult.postValue(new Event<>(result));
    }
}
