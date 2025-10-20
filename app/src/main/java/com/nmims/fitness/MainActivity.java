package com.nmims.fitness;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * MainActivity - Entry point and splash screen
 * Checks if user has completed profile setup
 * - First time: Navigate to PersonalDetailsActivity
 * - Returning user: Navigate to HomeActivity
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AISpotterPrefs";
    private static final String KEY_HAS_PROFILE = "hasProfile";
    private static final String KEY_USER_ID = "userId";
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show splash screen for 2 seconds, then navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserProfile();
        }, SPLASH_DELAY);
    }

    private void checkUserProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasProfile = prefs.getBoolean(KEY_HAS_PROFILE, false);

        Intent intent;
        if (hasProfile) {
            // User has completed setup, go to home
            intent = new Intent(MainActivity.this, HomeActivity.class);
        } else {
            // First time user, go to profile setup
            intent = new Intent(MainActivity.this, PersonalDetailsActivity.class);
        }

        startActivity(intent);
        finish(); // Don't allow back to splash screen
    }

    /**
     * Helper method to save user profile completion status
     * Call this from PersonalDetailsActivity after successful setup
     */
    public static void markProfileComplete(AppCompatActivity activity, String userId) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_HAS_PROFILE, true)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    /**
     * Helper method to get stored user ID
     */
    public static String getUserId(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID, "user_" + System.currentTimeMillis());
    }

    /**
     * Helper method to clear profile (for testing or logout)
     */
    public static void clearProfile(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}