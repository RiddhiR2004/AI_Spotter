package com.nmims.fitness;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.nmims.fitness.api.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "AISpotterPrefs";
    private static final String PREFS_SETTINGS = "AISpotterSettings";
    
    // UI Elements
    private ImageView iconBack;
    private TextView textUserName, textUserEmail;
    private Button buttonEditProfile, buttonLogout;
    
    // Account rows
    private View rowAccountDetails, rowConnectedAccounts;
    
    // Notification switches
    private SwitchCompat switchWorkoutReminders, switchProgressUpdates, switchPromotionalMessages;
    
    // App settings rows
    private View rowUnits, rowLanguage;
    private TextView textUnitsValue, textLanguageValue;
    private SwitchCompat switchDarkMode;
    
    // Support rows
    private View rowHelpCenter, rowSendFeedback, rowPrivacyPolicy, rowTermsOfService;
    
    private String userId;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
 
        userId = MainActivity.getUserId(this);
        settings = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        
        initViews();
        loadUserProfile();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        // Navigation
        iconBack = findViewById(R.id.icon_back);
        
        // Profile section
        textUserName = findViewById(R.id.text_user_name);
        textUserEmail = findViewById(R.id.text_user_email);
        buttonEditProfile = findViewById(R.id.button_edit_profile);
        
        // Account section - the include itself is the clickable CardView root
        rowAccountDetails = findViewById(R.id.settings_account_details);
        rowConnectedAccounts = findViewById(R.id.settings_connected_accounts);
        ((TextView) rowAccountDetails.findViewById(R.id.title)).setText("Account Details");
        ((TextView) rowConnectedAccounts.findViewById(R.id.title)).setText("Connected Accounts");
        
        // Notification switches - access through include IDs
        View workoutRemindersInclude = findViewById(R.id.settings_workout_reminders);
        View progressUpdatesInclude = findViewById(R.id.settings_progress_updates);
        View promotionalInclude = findViewById(R.id.settings_promotional);
        
        ((TextView) workoutRemindersInclude.findViewById(R.id.title)).setText("Workout Reminders");
        ((TextView) progressUpdatesInclude.findViewById(R.id.title)).setText("Progress Updates");
        ((TextView) promotionalInclude.findViewById(R.id.title)).setText("Promotional Messages");
        
        switchWorkoutReminders = workoutRemindersInclude.findViewById(R.id.switch_compat);
        switchProgressUpdates = progressUpdatesInclude.findViewById(R.id.switch_compat);
        switchPromotionalMessages = promotionalInclude.findViewById(R.id.switch_compat);
        
        // App settings - access through include IDs
        View unitsInclude = findViewById(R.id.settings_units);
        View languageInclude = findViewById(R.id.settings_language);
        View darkModeInclude = findViewById(R.id.settings_dark_mode);
        
        ((TextView) unitsInclude.findViewById(R.id.title)).setText("Units");
        ((TextView) languageInclude.findViewById(R.id.title)).setText("Language");
        ((TextView) darkModeInclude.findViewById(R.id.title)).setText("Dark Mode");
        
        rowUnits = unitsInclude;
        rowLanguage = languageInclude;
        
        textUnitsValue = unitsInclude.findViewById(R.id.value);
        textLanguageValue = languageInclude.findViewById(R.id.value);
        switchDarkMode = darkModeInclude.findViewById(R.id.switch_compat);
        
        // Support section - access through include IDs
        rowHelpCenter = findViewById(R.id.settings_help);
        rowSendFeedback = findViewById(R.id.settings_feedback);
        rowPrivacyPolicy = findViewById(R.id.settings_privacy);
        rowTermsOfService = findViewById(R.id.settings_terms);
        
        ((TextView) rowHelpCenter.findViewById(R.id.title)).setText("Help Center");
        ((TextView) rowSendFeedback.findViewById(R.id.title)).setText("Send Feedback");
        ((TextView) rowPrivacyPolicy.findViewById(R.id.title)).setText("Privacy Policy");
        ((TextView) rowTermsOfService.findViewById(R.id.title)).setText("Terms of Service");
        
        // Logout
        buttonLogout = findViewById(R.id.button_logout);
    }

    private void loadUserProfile() {
        // Try to load from Supabase surveys table
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getUserProfile(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONArray surveys = new JSONArray(response);
                        if (surveys.length() > 0) {
                            JSONObject profile = surveys.getJSONObject(0);
                            String name = profile.optString("name", "User");
                            String email = profile.optString("email", "user@email.com");
                            
                            textUserName.setText(name);
                            textUserEmail.setText(email);
                        } else {
                            setDefaultProfile();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing profile", e);
                        setDefaultProfile();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading profile: " + error);
                    setDefaultProfile();
                });
            }
        });
    }

    private void setDefaultProfile() {
        textUserName.setText("Fitness User");
        textUserEmail.setText("user@fitness.app");
    }

    private void loadSettings() {
        // Load notification preferences
        switchWorkoutReminders.setChecked(settings.getBoolean("workout_reminders", true));
        switchProgressUpdates.setChecked(settings.getBoolean("progress_updates", true));
        switchPromotionalMessages.setChecked(settings.getBoolean("promotional_messages", false));
        
        // Load app preferences
        String units = settings.getString("units", "kg");
        String language = settings.getString("language", "English");
        boolean darkMode = settings.getBoolean("dark_mode", false);
        
        textUnitsValue.setText(units);
        textLanguageValue.setText(language);
        switchDarkMode.setChecked(darkMode);
    }

    private void setupClickListeners() {
        // Back button
        iconBack.setOnClickListener(v -> finish());
        
        // Edit Profile
        buttonEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit Profile coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to edit profile screen
        });
        
        // Account section
        rowAccountDetails.setOnClickListener(v -> showAccountDetails());
        rowConnectedAccounts.setOnClickListener(v -> {
            Toast.makeText(this, "Connected Accounts coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        // Notification switches
        switchWorkoutReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean("workout_reminders", isChecked).apply();
            Toast.makeText(this, "Workout reminders " + (isChecked ? "enabled" : "disabled"), 
                          Toast.LENGTH_SHORT).show();
        });
        
        switchProgressUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean("progress_updates", isChecked).apply();
            Toast.makeText(this, "Progress updates " + (isChecked ? "enabled" : "disabled"), 
                          Toast.LENGTH_SHORT).show();
        });
        
        switchPromotionalMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean("promotional_messages", isChecked).apply();
            Toast.makeText(this, "Promotional messages " + (isChecked ? "enabled" : "disabled"), 
                          Toast.LENGTH_SHORT).show();
        });
        
        // App settings
        rowUnits.setOnClickListener(v -> showUnitsDialog());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean("dark_mode", isChecked).apply();
            applyDarkMode(isChecked);
        });
        
        // Support section
        rowHelpCenter.setOnClickListener(v -> openHelpCenter());
        rowSendFeedback.setOnClickListener(v -> sendFeedback());
        rowPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
        rowTermsOfService.setOnClickListener(v -> openTermsOfService());
        
        // Logout
        buttonLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showAccountDetails() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getUserProfile(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONArray surveys = new JSONArray(response);
                        if (surveys.length() > 0) {
                            JSONObject profile = surveys.getJSONObject(0);
                            
                            String name = profile.optString("name", "N/A");
                            String email = profile.optString("email", "N/A");
                            int age = profile.optInt("age", 0);
                            double height = profile.optDouble("height_cm", 0);
                            double weight = profile.optDouble("weight_kg", 0);
                            double bmi = profile.optDouble("bmi", 0);
                            String bmiCategory = profile.optString("bmi_category", "N/A");
                            
                            String details = String.format(
                                "Name: %s\n" +
                                "Email: %s\n" +
                                "Age: %d years\n" +
                                "Height: %.1f cm\n" +
                                "Weight: %.1f kg\n" +
                                "BMI: %.1f (%s)",
                                name, email, age, height, weight, bmi, bmiCategory
                            );
                            
                            new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("Account Details")
                                .setMessage(details)
                                .setPositiveButton("OK", null)
                                .show();
                        } else {
                            Toast.makeText(SettingsActivity.this, 
                                         "No account details found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing profile", e);
                        Toast.makeText(SettingsActivity.this, 
                                     "Error loading details", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, 
                                 "Error loading account details", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showUnitsDialog() {
        String[] units = {"kg", "lbs"};
        String currentUnit = settings.getString("units", "kg");
        int checkedItem = currentUnit.equals("kg") ? 0 : 1;
        
        new AlertDialog.Builder(this)
            .setTitle("Select Units")
            .setSingleChoiceItems(units, checkedItem, (dialog, which) -> {
                String selectedUnit = units[which];
                settings.edit().putString("units", selectedUnit).apply();
                textUnitsValue.setText(selectedUnit);
                Toast.makeText(this, "Units set to " + selectedUnit, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Hindi", "Chinese"};
        String currentLanguage = settings.getString("language", "English");
        int checkedItem = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLanguage)) {
                checkedItem = i;
                break;
            }
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                String selectedLanguage = languages[which];
                settings.edit().putString("language", selectedLanguage).apply();
                textLanguageValue.setText(selectedLanguage);
                Toast.makeText(this, "Language set to " + selectedLanguage, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        Toast.makeText(this, "Dark mode " + (enabled ? "enabled" : "disabled"), 
                      Toast.LENGTH_SHORT).show();
    }

    private void openHelpCenter() {
        String url = "https://fitness.app/help";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Help Center: Visit " + url, Toast.LENGTH_LONG).show();
        }
    }

    private void sendFeedback() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@fitness.app"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "AI Spotter Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, "I would like to share feedback about...");
        
        try {
            startActivity(Intent.createChooser(intent, "Send Feedback"));
        } catch (Exception e) {
            Toast.makeText(this, "Email: support@fitness.app", Toast.LENGTH_LONG).show();
        }
    }

    private void openPrivacyPolicy() {
        String url = "https://fitness.app/privacy";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Privacy Policy: Visit " + url, Toast.LENGTH_LONG).show();
        }
    }

    private void openTermsOfService() {
        String url = "https://fitness.app/terms";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Terms of Service: Visit " + url, Toast.LENGTH_LONG).show();
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?\n\nYou'll need to set up your profile again.")
            .setPositiveButton("Logout", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performLogout() {
        // Clear all user data
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        settings.edit().clear().apply();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate back to MainActivity (which will redirect to PersonalDetailsActivity)
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
