package com.nmims.fitness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nmims.fitness.api.SupabaseClient;
import com.nmims.fitness.models.WorkoutDay;
import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * HomeActivity - Main dashboard after user setup
 * Shows:
 * - Today's workout summary
 * - Progress overview
 * - Quick navigation to workout plan and progress tracking
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    
    private TextView welcomeTextView;
    private TextView workoutTitleTextView;
    private TextView exerciseCountTextView;
    private TextView progressOverallTextView;
    private TextView progressDetailsTextView;
    private Button startWorkoutButton;
    private Button viewProgressButton;
    private Button viewFullPlanButton;
    private Button aiCoachButton;
    
    private String userId;
    private WorkoutPlan currentWorkoutPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupClickListeners();
        
        // Get user ID
        userId = MainActivity.getUserId(this);
        
        // Load user's workout plan
        loadWorkoutPlan();
    }

    private void initViews() {
        welcomeTextView = findViewById(R.id.textView_welcome);
        workoutTitleTextView = findViewById(R.id.textView_workout_title);
        exerciseCountTextView = findViewById(R.id.textView_exercise_count);
        progressOverallTextView = findViewById(R.id.textView_progress_overall);
        progressDetailsTextView = findViewById(R.id.textView_progress_details);
        startWorkoutButton = findViewById(R.id.button_start_workout);
        viewProgressButton = findViewById(R.id.button_view_progress);
        viewFullPlanButton = findViewById(R.id.button_view_full_plan);
        aiCoachButton = findViewById(R.id.button_ai_coach);
        
        // Profile icon
        findViewById(R.id.icon_profile).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // Set welcome message
        String greeting = getGreeting();
        welcomeTextView.setText(greeting + "!");
    }

    private void setupClickListeners() {
        startWorkoutButton.setOnClickListener(v -> {
            if (currentWorkoutPlan != null) {
                // Navigate to workout mode (exercise-by-exercise view)
                Intent intent = new Intent(HomeActivity.this, WorkoutModeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No workout plan found. Create one first!", 
                             Toast.LENGTH_LONG).show();
            }
        });

        viewProgressButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProgressTrackingActivity.class);
            startActivity(intent);
        });

        viewFullPlanButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WorkoutPlanActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        aiCoachButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AICoachActivity.class);
            startActivity(intent);
        });
    }

    private void loadWorkoutPlan() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        parseAndDisplayWorkoutPlan(jsonResponse);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing workout plan", e);
                        showNoWorkoutMessage();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading workout plan: " + error);
                    showNoWorkoutMessage();
                });
            }
        });
    }

    private void parseAndDisplayWorkoutPlan(String jsonResponse) throws JSONException {
        JSONArray plansArray = new JSONArray(jsonResponse);
        
        if (plansArray.length() > 0) {
            JSONObject planJson = plansArray.getJSONObject(0);
            
            // Extract workout_days JSONB field
            String workoutDaysJson = planJson.getString("workout_days");
            
            // Build complete workout plan JSON
            JSONObject completeJson = new JSONObject();
            completeJson.put("planName", planJson.optString("plan_name", "Your Workout Plan"));
            completeJson.put("goal", planJson.optString("goal", ""));
            completeJson.put("durationWeeks", planJson.optInt("duration_weeks", 4));
            completeJson.put("overallNotes", planJson.optString("overall_notes", ""));
            completeJson.put("workoutDays", new JSONArray(workoutDaysJson));
            
            currentWorkoutPlan = WorkoutParser.parseWorkoutPlan(userId, completeJson.toString());
            
            // Load exercise progress and merge it
            loadAndMergeProgress();
        } else {
            showNoWorkoutMessage();
        }
    }

    private void loadAndMergeProgress() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getExerciseProgress(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String progressJson) {
                runOnUiThread(() -> {
                    WorkoutParser.mergeExerciseProgress(currentWorkoutPlan, progressJson);
                    displayTodayWorkout();
                    displayProgress();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Could not load progress: " + error);
                    // Continue without progress data
                    displayTodayWorkout();
                    displayProgress();
                });
            }
        });
    }

    private void displayTodayWorkout() {
        if (currentWorkoutPlan == null || currentWorkoutPlan.getWorkoutDays().isEmpty()) {
            showNoWorkoutMessage();
            return;
        }

        // Get today's day of week
        String today = new SimpleDateFormat("EEEE", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Find today's workout
        WorkoutDay todayWorkout = null;
        for (WorkoutDay day : currentWorkoutPlan.getWorkoutDays()) {
            if (day.getDay().equalsIgnoreCase(today)) {
                todayWorkout = day;
                break;
            }
        }

        if (todayWorkout != null) {
            // Set workout title
            workoutTitleTextView.setText(todayWorkout.getFocus());
            
            // Set exercise count
            exerciseCountTextView.setText(todayWorkout.getExercises().size() + " exercises");
            
            startWorkoutButton.setEnabled(true);
            startWorkoutButton.setText("Start Today's Workout");
        } else {
            workoutTitleTextView.setText("Rest Day");
            exerciseCountTextView.setText("Your body needs recovery");
            startWorkoutButton.setText("View Full Plan");
            startWorkoutButton.setEnabled(true);
        }

        // Display progress
        displayProgress();
    }

    private void displayProgress() {
        if (currentWorkoutPlan == null) return;

        int totalExercises = 0;
        int completedExercises = 0;

        for (WorkoutDay day : currentWorkoutPlan.getWorkoutDays()) {
            totalExercises += day.getExercises().size();
            for (var exercise : day.getExercises()) {
                if (exercise.isCompleted()) {
                    completedExercises++;
                }
            }
        }

        float progressPercentage = totalExercises > 0 
            ? (float) completedExercises / totalExercises * 100 
            : 0;

        progressOverallTextView.setText(String.format(Locale.getDefault(),
                "Progress %.0f%%", progressPercentage));
        progressDetailsTextView.setText(String.format(Locale.getDefault(),
                "%d / %d exercises done", completedExercises, totalExercises));
    }

    private void showNoWorkoutMessage() {
        workoutTitleTextView.setText("No Workout Plan");
        exerciseCountTextView.setText("Create a personalized plan by completing your profile");
        startWorkoutButton.setText("Create Workout Plan");
        startWorkoutButton.setEnabled(true);
        
        startWorkoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PersonalDetailsActivity.class);
            startActivity(intent);
        });
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload workout plan when returning to home (in case progress changed)
        loadWorkoutPlan();
    }
}