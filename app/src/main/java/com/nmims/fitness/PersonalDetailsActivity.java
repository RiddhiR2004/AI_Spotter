package com.nmims.fitness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
 * PersonalDetailsActivity - Collects user profile data
 * - Saves to Supabase surveys table
 * - Marks profile as complete in SharedPreferences
 * - Navigates to WorkoutPlanActivity to generate AI workout
 */
public class PersonalDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PersonalDetailsActivity";
    private static final String SUPABASE_URL = "https://rvqrakvctwekgewdunhw.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ2cXJha3ZjdHdla2dld2R1bmh3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1OTYwMDEsImV4cCI6MjA3NjE3MjAwMX0.XuaY9Yn1KZNe9vQ8pGT2xxlSKnAbWpKeazUCumfWygg";

    private EditText nameEditText, emailEditText, ageEditText, heightEditText, weightEditText;
    private Button nextButton;
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        initViews();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.editText_name);
        emailEditText = findViewById(R.id.editText_email);
        ageEditText = findViewById(R.id.editText_age);
        heightEditText = findViewById(R.id.editText_height);
        weightEditText = findViewById(R.id.editText_weight);
        nextButton = findViewById(R.id.button_next);

        nextButton.setOnClickListener(v -> collectAndProceed());
    }

    private void collectAndProceed() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String heightStr = heightEditText.getText().toString().trim();
        String weightStr = weightEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty() ||
            heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse values
            int age = Integer.parseInt(ageStr);
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);

            // Calculate BMI
            double bmi = weight / Math.pow(height / 100.0, 2);

            // Generate user ID
            String userId = "user_" + email.hashCode();

            // Save to Supabase first
            saveToSupabase(name, email, age, height, weight, bmi, userId);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToSupabase(String name, String email, int age, double height,
                                double weight, double bmi, String userId) {

        nextButton.setEnabled(false);
        nextButton.setText("Saving...");

        new Thread(() -> {
            try {
                String bmiCategory = getBMICategory(bmi);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", name);
                jsonObject.put("email", email);
                jsonObject.put("age", age);
                jsonObject.put("height_cm", height);
                jsonObject.put("weight_kg", weight);
                jsonObject.put("bmi", bmi);
                jsonObject.put("bmi_category", bmiCategory);
                jsonObject.put("activity_frequency", "3-4 times/week");
                jsonObject.put("available_hours", 1.5);
                jsonObject.put("gym_equipment", true);
                jsonObject.put("goals", "Build muscle");
                jsonObject.put("injuries", JSONObject.NULL);

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
                        runOnUiThread(() -> {
                            Log.e(TAG, "Failed to save profile", e);
                            Toast.makeText(PersonalDetailsActivity.this,
                                    "Failed to save. Continuing anyway...", Toast.LENGTH_SHORT).show();
                            proceedToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            try (response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "Profile saved to Supabase");
                                    // Mark profile as complete
                                    MainActivity.markProfileComplete(PersonalDetailsActivity.this, userId);
                                    proceedToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                                } else {
                                    Log.e(TAG, "Supabase error: " + response.code());
                                    Toast.makeText(PersonalDetailsActivity.this,
                                            "Save failed. Continuing anyway...", Toast.LENGTH_SHORT).show();
                                    proceedToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                                }
                            }
                        });
                    }
                });
            } catch (JSONException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "JSON error", e);
                    Toast.makeText(PersonalDetailsActivity.this,
                            "Error saving data", Toast.LENGTH_SHORT).show();
                    nextButton.setEnabled(true);
                    nextButton.setText("Next");
                });
            }
        }).start();
    }

    private void proceedToWorkoutGeneration(String name, String email, int age,
                                           double height, double weight, double bmi, String userId) {
        // Check if workout plan already exists
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        // Check if response contains any workout plans
                        if (jsonResponse != null && !jsonResponse.trim().isEmpty() 
                                && !jsonResponse.equals("[]") && !jsonResponse.equals("null")) {
                            Log.d(TAG, "Workout plan already exists, going to HomeActivity");
                            Intent intent = new Intent(PersonalDetailsActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // No plan exists, generate one
                            navigateToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking plan existence", e);
                        // On error, try to generate
                        navigateToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error fetching plan: " + error);
                    // On error, proceed to generation
                    navigateToWorkoutGeneration(name, email, age, height, weight, bmi, userId);
                });
            }
        });
    }

    private void navigateToWorkoutGeneration(String name, String email, int age,
                                            double height, double weight, double bmi, String userId) {
        // Navigate to WorkoutPlanActivity
        Intent intent = new Intent(PersonalDetailsActivity.this, WorkoutPlanActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", name);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_AGE", age);
        intent.putExtra("USER_HEIGHT", height);
        intent.putExtra("USER_WEIGHT", weight);
        intent.putExtra("USER_BMI", bmi);
        intent.putExtra("USER_GOAL", "Build muscle");
        intent.putExtra("HAS_EQUIPMENT", true);

        startActivity(intent);
        finish(); // Don't allow back to profile entry
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }
}