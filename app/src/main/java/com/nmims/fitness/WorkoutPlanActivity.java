package com.nmims.fitness;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.nmims.fitness.adapters.ExerciseAdapter;
import com.nmims.fitness.api.GeminiApiClient;
import com.nmims.fitness.api.SupabaseClient;
import com.nmims.fitness.models.Exercise;
import com.nmims.fitness.models.WorkoutDay;
import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

import org.json.JSONException;

public class WorkoutPlanActivity extends AppCompatActivity implements ExerciseAdapter.OnExerciseClickListener {

    private static final String TAG = "WorkoutPlanActivity";

    private TabLayout tabDays;
    private RecyclerView recyclerExercises;
    private TextView titleTextView;
    private ProgressBar progressBar;

    private WorkoutPlan workoutPlan;
    private ExerciseAdapter exerciseAdapter;
    private int currentDayIndex = 0;
    private String userId;
    private boolean isNewPlan = false;

    // --- START OF FIX 1: Add variables to hold all user data ---
    private String name, email, goal, activityFrequency, injuries, bmiCategory;
    private int age;
    private double height, weight, bmi, availableHours;
    private boolean hasEquipment;
    // --- END OF FIX 1 ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_plan);

        initViews();
        setupRecyclerView();

        // Get user data from intent
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            userId = MainActivity.getUserId(this);
        }

        // --- START OF FIX 2: Get ALL data from the Intent ---
        name = getIntent().getStringExtra("USER_NAME");
        email = getIntent().getStringExtra("USER_EMAIL");
        age = getIntent().getIntExtra("USER_AGE", 25);
        height = getIntent().getDoubleExtra("USER_HEIGHT", 170.0);
        weight = getIntent().getDoubleExtra("USER_WEIGHT", 70.0);
        bmi = getIntent().getDoubleExtra("USER_BMI", 22.5);
        goal = getIntent().getStringExtra("USER_GOAL"); // This will have all goals
        hasEquipment = getIntent().getBooleanExtra("HAS_EQUIPMENT", true);

        // Get the NEW fields from Step 2 and 3
        activityFrequency = getIntent().getStringExtra("ACTIVITY_FREQUENCY");
        availableHours = getIntent().getDoubleExtra("AVAILABLE_HOURS", 1.0); // Default to 1 hour
        injuries = getIntent().getStringExtra("INJURIES");
        // --- END OF FIX 2 ---

        // Calculate BMI category
        bmiCategory = getBMICategory(bmi);

        // --- START OF FIX 3: Call the refactored method (no parameters) ---
        // This method will now use the class variables we just set
        loadExistingPlanOrGenerate();
        // --- END OF FIX 3 ---
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    private void initViews() {
        tabDays = findViewById(R.id.tab_days);
        recyclerExercises = findViewById(R.id.recycler_exercises);
        titleTextView = findViewById(R.id.textView_workoutTitle);
        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());

        tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentDayIndex = tab.getPosition();
                displayWorkoutDay(currentDayIndex);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        recyclerExercises.setLayoutManager(new LinearLayoutManager(this));
    }

    // --- START OF FIX 4: Refactor method to use class variables ---
    // (Removed all parameters from the method signature)
    private void loadExistingPlanOrGenerate() {
        showLoading(true);
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONArray plansArray = new org.json.JSONArray(jsonResponse);
                        if (plansArray.length() > 0) {
                            // ... (parsing logic is fine)
                            org.json.JSONObject planJson = plansArray.getJSONObject(0);
                            String workoutDaysJson = planJson.getString("workout_days");

                            org.json.JSONObject completeJson = new org.json.JSONObject();
                            completeJson.put("planName", planJson.optString("plan_name", "Your Workout Plan"));
                            completeJson.put("goal", planJson.optString("goal", ""));
                            completeJson.put("durationWeeks", planJson.optInt("duration_weeks", 4));
                            completeJson.put("overallNotes", planJson.optString("overall_notes", ""));
                            completeJson.put("workoutDays", new org.json.JSONArray(workoutDaysJson));

                            workoutPlan = WorkoutParser.parseWorkoutPlan(userId, completeJson.toString());
                            isNewPlan = false;
                            displayWorkoutPlan();
                            loadAndMergeProgress();
                            showLoading(false);
                        } else {
                            // No plan found; generate a new one
                            // This call now uses the class variables
                            generateWorkoutPlan();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing existing plan; generating new one", e);
                        generateWorkoutPlan(); // Use class variables
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.d(TAG, "No existing plan or error ('" + error + "'), generating new plan");
                    generateWorkoutPlan(); // Use class variables
                });
            }
        });
    }

    // --- START OF FIX 5: Refactor method to use class variables ---
    // (Removed all parameters from the method signature)
    private void generateWorkoutPlan() {
        showLoading(true);
        Toast.makeText(this, "Generating your personalized workout plan...\nThis may take 20-30 seconds",
                Toast.LENGTH_LONG).show();

        GeminiApiClient geminiClient = new GeminiApiClient();

        // This call now passes all the *correct* data from the class variables
        geminiClient.generateWorkoutPlan(name, age, bmi, bmiCategory, goal, activityFrequency,
                availableHours, hasEquipment, injuries, new GeminiApiClient.GeminiCallback() {

                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            try {
                                workoutPlan = WorkoutParser.parseWorkoutPlan(userId, response);
                                isNewPlan = true;
                                displayWorkoutPlan();
                                showLoading(false);
                                Toast.makeText(WorkoutPlanActivity.this,
                                        "Workout plan generated successfully!", Toast.LENGTH_SHORT).show();
                                savePlanIfNew();
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing workout plan", e);
                                showError("Failed to parse workout plan: " + e.getMessage());
                                showLoading(false);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Gemini API error: " + error);
                            showError("Failed to generate workout plan. Please check:\n" +
                                    "1. Internet connection\n" +
                                    "2. Gemini API key is valid\n" +
                                    "Error: " + error);
                            showLoading(false);
                        });
                    }
                });
    }
    // --- END OF FIX 5 ---


    // --- NO CHANGES NEEDED BELOW THIS LINE ---

    private void displayWorkoutPlan() {
        if (workoutPlan == null || workoutPlan.getWorkoutDays().isEmpty()) {
            showError("No workout plan available");
            return;
        }

        // Setup tabs for days
        tabDays.removeAllTabs();
        for (WorkoutDay day : workoutPlan.getWorkoutDays()) {
            tabDays.addTab(tabDays.newTab().setText(day.getDay()));
        }

        // Display first day
        displayWorkoutDay(0);

        // If plan was loaded from DB, don't re-save; just ensure progress is merged
        if (!isNewPlan) {
            loadAndMergeProgress();
        }
    }

    private void savePlanIfNew() {
        if (!isNewPlan || workoutPlan == null) return;
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.saveWorkoutPlan(workoutPlan, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Workout plan saved to Supabase");
                runOnUiThread(() -> {
                    Toast.makeText(WorkoutPlanActivity.this, "Plan saved!", Toast.LENGTH_SHORT).show();
                    loadAndMergeProgress();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to save to Supabase: " + error);
            }
        });
    }

    private void loadAndMergeProgress() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getExerciseProgress(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String progressJson) {
                runOnUiThread(() -> {
                    WorkoutParser.mergeExerciseProgress(workoutPlan, progressJson);
                    // Refresh display to show updated completion status
                    displayWorkoutDay(currentDayIndex);
                    calculateAndDisplayProgress();
                });
            }

           // Opening in ...
            @Override
            public void onError(String error) {
                Log.d(TAG, "No existing progress found or error loading: " + error);
                // This is fine for new users
            }
        });
    }

    private void displayWorkoutDay(int dayIndex) {
        if (workoutPlan == null || dayIndex >= workoutPlan.getWorkoutDays().size()) {
            return;
        }

        WorkoutDay workoutDay = workoutPlan.getWorkoutDays().get(dayIndex);
        titleTextView.setText(workoutDay.getFocus());

        // Setup adapter with exercises
        if (exerciseAdapter == null) {
            exerciseAdapter = new ExerciseAdapter(workoutDay.getExercises(), this);
            recyclerExercises.setAdapter(exerciseAdapter);
        } else {
            exerciseAdapter.updateExercises(workoutDay.getExercises());
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerExercises.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    @Override
    public void onExerciseClick(Exercise exercise, int position) {
        // Create a beautiful dialog showing full exercise details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Build detailed message
        StringBuilder details = new StringBuilder();
        details.append("💪 Exercise: ").append(exercise.getName()).append("\n\n");

        if (exercise.getSets() > 0) {
            details.append("📊 Sets: ").append(exercise.getSets()).append("\n");
        }
        if (exercise.getReps() > 0) {
            details.append("🔢 Reps: ").append(exercise.getReps()).append("\n");
        }
        if (exercise.getDuration() != null && !exercise.getDuration().equals("N/A")) {
            details.append("⏱️ Duration: ").append(exercise.getDuration()).append("\n");
        }
        if (exercise.getRestPeriod() != null) {
            details.append("⏸️ Rest: ").append(exercise.getRestPeriod()).append("\n");
        }

        details.append("\n🎯 Target Muscles:\n").append(exercise.getTargetMuscles()).append("\n");
        details.append("\n📝 Instructions:\n").append(exercise.getInstructions());

        builder.setTitle("Exercise Details")
                .setMessage(details.toString())
                .setPositiveButton("Got it!", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Mark Complete", (dialog, which) -> {
                    exercise.setCompleted(true);
                    exerciseAdapter.notifyItemChanged(position);
                    onExerciseChecked(exercise, position, true);
                })
                .show();
    }

    private void calculateAndDisplayProgress() {
        if (workoutPlan == null) return;

        int totalExercises = 0;
        int completedExercises = 0;

        for (WorkoutDay day : workoutPlan.getWorkoutDays()) {
            for (Exercise exercise : day.getExercises()) {
                totalExercises++;
                if (exercise.isCompleted()) {
                    completedExercises++;
                }
            }
        }

        float progressPercentage = (totalExercises > 0) ? (float) completedExercises / totalExercises * 100 : 0;
        Log.d(TAG, String.format("Overall progress: %.1f%% (%d/%d)",
                progressPercentage, completedExercises, totalExercises));
    }

    @Override
    public void onExerciseChecked(Exercise exercise, int position, boolean isChecked) {
        Log.d(TAG, "Exercise " + exercise.getName() + " marked as " +
                (isChecked ? "completed" : "incomplete"));

        // Get current day
        String currentDay = workoutPlan.getWorkoutDays().get(currentDayIndex).getDay();
        String userId = workoutPlan.getUserId();

        // Save progress to Supabase
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.updateExerciseProgress(userId, currentDay, exercise.getName(),
                isChecked, new SupabaseClient.SupabaseCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Progress saved to Supabase");

                        runOnUiThread(() -> {
                            calculateAndDisplayProgress();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to save progress: " + error);
                    }
                });
    }
}