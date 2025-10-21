package com.nmims.fitness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.nmims.fitness.api.SupabaseClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * PersonalDetailStep3Activity - Step 3 of 3
 * Collects workout preferences: Equipment, goals, and injuries.
 * Receives all data from Steps 1 and 2.
 * Saves complete profile to Supabase.
 * Navigates to WorkoutPlanActivity to generate plan.
 */
// *** RENAMED CLASS ***
public class PersonalDetailStep3Activity extends AppCompatActivity {

    private static final String TAG = "PersonalDetailStep3";
    private static final String SUPABASE_URL = "https://rvqrakvctwekgewdunhw.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ2cXJha3ZjdHdla2dld2R1bmh3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1OTYwMDEsImV4cCI6MjA3NjE3MjAwMX0.XuaYn1KZNe9vQ8pGT2xxlSKnAbWpKeazUCumfWygg";

    // Data from Steps 1 & 2
    private String name, email, activityFrequency;
    private int age;
    private double height, weight, availableHours;

    // Data from this step
    private boolean hasEquipment;
    private String goal;
    private String injuries;
    private double bmi;
    private String bmiCategory;
    private String userId;

    // UI Elements
    private SwitchMaterial gymEquipmentSwitch;
    private ChipGroup goalsChipGroup;
    private EditText injuriesEditText;
    private Button generateButton;
    private ImageButton backButton;

    // API Clients
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Matches your XML file name
        setContentView(R.layout.activity_personal_detail_step3);

        if (!receiveIntentData()) {
            Toast.makeText(this, "Error: Missing user data. Returning to first step.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, PersonalDetailsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        initViews();
        setupClickListeners();
    }

    private boolean receiveIntentData() {
        Intent intent = getIntent();
        // Step 1 data
        name = intent.getStringExtra("USER_NAME");
        email = intent.getStringExtra("USER_EMAIL");
        age = intent.getIntExtra("USER_AGE", -1);
        height = intent.getDoubleExtra("USER_HEIGHT", -1.0);
        weight = intent.getDoubleExtra("USER_WEIGHT", -1.0);
        // Step 2 data
        activityFrequency = intent.getStringExtra("ACTIVITY_FREQUENCY");
        availableHours = intent.getDoubleExtra("AVAILABLE_HOURS", -1.0);

        return name != null && email != null && activityFrequency != null &&
                age != -1 && height != -1.0 && weight != -1.0 && availableHours != -1.0;
    }

    private void initViews() {
        gymEquipmentSwitch = findViewById(R.id.gymEquipmentSwitch);
        goalsChipGroup = findViewById(R.id.goalsChipGroup);
        injuriesEditText = findViewById(R.id.injuriesEditText);
        generateButton = findViewById(R.id.generateButton);
        backButton = findViewById(R.id.button_back);
    }

    private void setupClickListeners() {
        generateButton.setOnClickListener(v -> {
            collectAndSaveProfile();
        });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void collectAndSaveProfile() {
        // Collect data from this step
        hasEquipment = gymEquipmentSwitch.isChecked();
        injuries = injuriesEditText.getText().toString().trim();

        // Get selected goal from ChipGroup
        int selectedChipId = goalsChipGroup.getCheckedChipId();
        if (selectedChipId == -1) { // -1 is the default for no chip selected
            Toast.makeText(this, "Please select a fitness goal", Toast.LENGTH_SHORT).show();
            return;
        }
        Chip selectedChip = findViewById(selectedChipId);
        goal = selectedChip.getText().toString();

        // Perform final calculations
        bmi = weight / Math.pow(height / 100.0, 2);
        bmiCategory = getBMICategory(bmi);
        userId = "user_" + email.hashCode();

        // Now, save the complete profile to Supabase
        saveToSupabase();
    }

    private void saveToSupabase() {
        generateButton.setEnabled(false);
        generateButton.setText("Saving Profile...");

        new Thread(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                // Step 1 Data
                jsonObject.put("name", name);
                jsonObject.put("email", email);
                jsonObject.put("age", age);
                jsonObject.put("height_cm", height);
                jsonObject.put("weight_kg", weight);
                // Step 2 Data
                jsonObject.put("activity_frequency", activityFrequency);
                jsonObject.put("available_hours", availableHours);
                // Step 3 Data
                jsonObject.put("gym_equipment", hasEquipment);
                jsonObject.put("goals", goal);
                jsonObject.put("injuries", injuries.isEmpty() ? JSONObject.NULL : injuries);
                // Calculated Data
                jsonObject.put("bmi", bmi);
                jsonObject.put("bmi_category", bmiCategory);

                RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/surveys")
                        .post(body)
                        .addHeader("apikey", SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        handleSaveResponse(false, e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        handleSaveResponse(response.isSuccessful(), response.message());
                        response.close();
                    }
                });
            } catch (JSONException e) {
                handleSaveResponse(false, "JSON Error: " + e.getMessage());
            }
        }).start();
    }

    private void handleSaveResponse(boolean success, String message) {
        runOnUiThread(() -> {
            if (success) {
                Log.d(TAG, "Profile saved to Supabase");
                // Mark profile as complete in SharedPreferences
                // *** UPDATED CONTEXT ***
                MainActivity.markProfileComplete(PersonalDetailStep3Activity.this, userId);
            } else {
                Log.e(TAG, "Failed to save profile: " + message);
                Toast.makeText(PersonalDetailStep3Activity.this,
                        "Note: Failed to save profile. Continuing anyway...", Toast.LENGTH_SHORT).show();
                // We still let the user proceed. Mark profile complete locally.
                // *** UPDATED CONTEXT ***
                MainActivity.markProfileComplete(PersonalDetailStep3Activity.this, userId);
            }

            // After saving (or failing to save), proceed to check for a workout plan
            proceedToWorkoutGeneration();
        });
    }

    private void proceedToWorkoutGeneration() {
        // This logic is moved from the original PersonalDetailsActivity
        // It checks if a plan *already* exists before generating a new one
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        if (jsonResponse != null && !jsonResponse.trim().isEmpty() &&
                                !jsonResponse.equals("[]") && !jsonResponse.equals("null")) {
                            Log.d(TAG, "Workout plan already exists, going to HomeActivity");
                            // *** UPDATED CONTEXT ***
                            Intent intent = new Intent(PersonalDetailStep3Activity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // No plan exists, generate one
                            navigateToWorkoutGeneration();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking plan, generating new one", e);
                        navigateToWorkoutGeneration();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error fetching plan, generating new one: " + error);
                    navigateToWorkoutGeneration();
                });
            }
        });
    }

    private void navigateToWorkoutGeneration() {
        // This is the original final destination
        // *** UPDATED CONTEXT ***
        Intent intent = new Intent(PersonalDetailStep3Activity.this, WorkoutPlanActivity.class);

        // Pass all the data WorkoutPlanActivity needs for the Gemini API call
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", name);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_AGE", age);
        intent.putExtra("USER_HEIGHT", height);
        intent.putExtra("USER_WEIGHT", weight);
        intent.putExtra("USER_BMI", bmi);
        intent.putExtra("USER_GOAL", goal);
        intent.putExtra("HAS_EQUIPMENT", hasEquipment);

        // Pass the new fields from Step 2
        intent.putExtra("ACTIVITY_FREQUENCY", activityFrequency);
        intent.putExtra("AVAILABLE_HOURS", availableHours);
        intent.putExtra("INJURIES", injuries);


        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }
}