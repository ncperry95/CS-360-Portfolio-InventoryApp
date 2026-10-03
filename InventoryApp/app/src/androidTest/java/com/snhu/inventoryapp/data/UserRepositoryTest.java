package com.snhu.inventoryapp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snhu.inventoryapp.auth.AuthResult;
import com.snhu.inventoryapp.security.PasswordHasher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Account creation and login against an in memory Room database, including
 * the one time upgrade of a legacy SHA-256 row.
 */
@RunWith(AndroidJUnit4.class)
public class UserRepositoryTest {

    private AppDatabase database;
    private UserDao users;
    private UserRepository repo;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = AppDatabase.createInMemory(context);
        users = database.userDao();
        repo = new UserRepository(users, new PasswordHasher(), Runnable::run);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void createThenLogin_succeeds() {
        AuthResult created = create("nathan", "correct horse 1");
        assertTrue(created.isSuccess());
        assertTrue(created.getUserId() > 0);

        AuthResult login = login("nathan", "correct horse 1");
        assertTrue(login.isSuccess());
        assertEquals(created.getUserId(), login.getUserId());
        assertEquals("nathan", login.getUsername());
    }

    @Test
    public void login_wrongPassword_fails() {
        create("nathan", "correct horse 1");
        AuthResult login = login("nathan", "correct horse 2");
        assertFalse(login.isSuccess());
        assertEquals(AuthResult.Error.INVALID_CREDENTIALS, login.getError());
    }

    @Test
    public void login_unknownUser_failsWithSameErrorAsWrongPassword() {
        AuthResult login = login("nobody", "correct horse 1");
        assertFalse(login.isSuccess());
        assertEquals(AuthResult.Error.INVALID_CREDENTIALS, login.getError());
    }

    @Test
    public void create_duplicateUsername_isRejected() {
        assertTrue(create("nathan", "correct horse 1").isSuccess());
        AuthResult second = create("nathan", "different pass 2");
        assertFalse(second.isSuccess());
        assertEquals(AuthResult.Error.USERNAME_TAKEN, second.getError());
    }

    @Test
    public void create_weakPassword_isRejectedBeforeTouchingDatabase() {
        AuthResult tooShort = create("nathan", "abc1");
        assertEquals(AuthResult.Error.PASSWORD_TOO_SHORT, tooShort.getError());
        assertFalse("no row should exist after a rejected create", users.userExists("nathan"));

        AuthResult noDigit = create("nathan", "abcdefghij");
        assertEquals(AuthResult.Error.PASSWORD_NO_DIGIT, noDigit.getError());
    }

    @Test
    public void create_emptyFields_isRejected() {
        assertEquals(AuthResult.Error.FIELDS_REQUIRED, create("", "correct horse 1").getError());
        assertEquals(AuthResult.Error.FIELDS_REQUIRED, create("nathan", "").getError());
        assertEquals(AuthResult.Error.FIELDS_REQUIRED, login("   ", "correct horse 1").getError());
    }

    @Test
    public void storedHash_isNotThePasswordAndIsPbkdf2() {
        create("nathan", "correct horse 1");
        UserEntity row = users.findByUsername("nathan");
        assertNotEquals("correct horse 1", row.passwordHash);
        assertTrue(row.passwordHash.startsWith("pbkdf2_sha256$"));
    }

    @Test
    public void legacySha256Row_logsInOnceAndIsUpgraded() {
        // Insert a row exactly as the original app would have: unsalted SHA-256 hex.
        String legacy = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // "password"
        long id = users.insert(new UserEntity("legacyuser", legacy));
        assertTrue(id > 0);
        assertTrue(PasswordHasher.isLegacyHash(users.findByUsername("legacyuser").passwordHash));

        AuthResult login = login("legacyuser", "password");
        assertTrue(login.isSuccess());

        String upgraded = users.findByUsername("legacyuser").passwordHash;
        assertFalse("legacy hash must be replaced after login", PasswordHasher.isLegacyHash(upgraded));
        assertTrue(upgraded.startsWith("pbkdf2_sha256$"));

        // And the upgraded row still works with the same password.
        assertTrue(login("legacyuser", "password").isSuccess());
        assertFalse(login("legacyuser", "Password").isSuccess());
    }

    // -------------------- helpers --------------------

    private AuthResult create(String user, String pass) {
        AtomicReference<AuthResult> out = new AtomicReference<>();
        repo.createAccount(user, pass.toCharArray(), out::set);
        return out.get();
    }

    private AuthResult login(String user, String pass) {
        AtomicReference<AuthResult> out = new AtomicReference<>();
        repo.login(user, pass.toCharArray(), out::set);
        return out.get();
    }
}
