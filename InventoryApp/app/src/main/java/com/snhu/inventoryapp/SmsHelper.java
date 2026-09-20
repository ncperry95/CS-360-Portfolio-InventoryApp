package com.snhu.inventoryapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Everything the app needs to know about SMS, plus the one place that owns
 * the SMS preferences file and its keys.
 *
 * The original had SmsSettingsActivity re typing the preferences name and a
 * second key as string literals; those now go through this class. The low
 * stock threshold moved to domain.LowStockRule so the rule is not tied to the
 * delivery mechanism.
 */
public final class SmsHelper {

    private static final String PREFS_NAME = "sms_prefs";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_ASKED_BEFORE = "asked_before";

    private static final String TAG = "SmsHelper";

    private SmsHelper() {
    }

    public static boolean isSmsPermissionGranted(Context context) {
        int status = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
        return status == PackageManager.PERMISSION_GRANTED;
    }

    public static String getSavedPhoneNumber(Context context) {
        return prefs(context).getString(KEY_PHONE_NUMBER, "");
    }

    public static void savePhoneNumber(Context context, String phoneNumber) {
        prefs(context).edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply();
    }

    /** Whether the permission prompt has ever been shown. */
    public static boolean hasAskedBefore(Context context) {
        return prefs(context).getBoolean(KEY_ASKED_BEFORE, false);
    }

    public static void markAskedBefore(Context context) {
        prefs(context).edit().putBoolean(KEY_ASKED_BEFORE, true).apply();
    }

    /**
     * Sends a low stock alert if permission is granted and a number is saved.
     * Returns true when a send was attempted, false when it was skipped or
     * failed, so callers can log or ignore as they see fit.
     */
    public static boolean sendLowStockAlert(Context context, String itemName, int quantity) {
        if (!isSmsPermissionGranted(context)) {
            return false;
        }
        String phoneNumber = getSavedPhoneNumber(context);
        if (phoneNumber.isEmpty()) {
            return false;
        }
        String message = context.getString(R.string.sms_low_stock_message, itemName, quantity);
        try {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            if (smsManager == null) {
                smsManager = SmsManager.getDefault();
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            // Malformed number or permission revoked mid session. Log and
            // keep the inventory screen alive; a failed text is not fatal.
            Log.w(TAG, "Could not send SMS alert", e);
            return false;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
