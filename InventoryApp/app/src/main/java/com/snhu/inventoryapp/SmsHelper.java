package com.snhu.inventoryapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Wraps everything the rest of the app needs to know about SMS.
 *
 * Centralizing this here means the inventory screens never have to handle
 * Android permission APIs or SmsManager directly. If permission was denied
 * or no phone number is saved, sendLowStockAlert quietly does nothing so
 * the rest of the app keeps working.
 */
public class SmsHelper {

    // Items at or below this number count as low stock
    public static final int LOW_STOCK_THRESHOLD = 5;

    // SharedPreferences keys for the saved phone number
    private static final String PREFS_NAME = "sms_prefs";
    private static final String KEY_PHONE_NUMBER = "phone_number";

    private static final String TAG = "SmsHelper";

    /**
     * Returns true only when SEND_SMS permission has been granted by the user.
     * The rest of the app uses this as a single yes or no check.
     */
    public static boolean isSmsPermissionGranted(Context context) {
        int status = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
        return status == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Reads the phone number the user saved in SMS Settings.
     * Returns an empty string if nothing has been saved yet.
     */
    public static String getSavedPhoneNumber(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PHONE_NUMBER, "");
    }

    /**
     * Saves the phone number the user entered in SMS Settings so future
     * alerts know where to go.
     */
    public static void savePhoneNumber(Context context, String phoneNumber) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply();
    }

    /**
     * Sends a low stock alert if (and only if) all three of these are true:
     *   1. SEND_SMS permission has been granted
     *   2. A phone number has been saved
     *   3. SmsManager actually accepts the send
     *
     * If any of those fail, the method quietly returns without throwing.
     * The rest of the app keeps working either way, which is what the
     * rubric calls for.
     */
    public static void sendLowStockAlert(Context context, String itemName, int quantity) {
        // Bail out early if the user has not granted permission yet
        if (!isSmsPermissionGranted(context)) {
            return;
        }

        // Bail out if the user never saved a phone number
        String phoneNumber = getSavedPhoneNumber(context);
        if (phoneNumber.isEmpty()) {
            return;
        }

        // Build the alert text and hand it to SmsManager
        String message = "Low stock alert: \"" + itemName
                + "\" is down to " + quantity + ". Time to restock.";
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            // SmsManager can throw if the phone number is malformed or the
            // device cannot send. We swallow it so the inventory screen
            // never crashes because of a bad SMS attempt.
            Log.w(TAG, "Could not send SMS alert", e);
        }
    }
}
