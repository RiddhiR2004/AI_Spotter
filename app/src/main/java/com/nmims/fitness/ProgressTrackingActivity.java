package com.nmims.fitness;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nmims.fitness.api.SupabaseClient;
import com.nmims.fitness.models.Exercise;
import com.nmims.fitness.models.WorkoutDay;
import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ProgressTrackingActivity - Displays workout completion analytics
 * Shows:
 * - Overall completion percentage
 * - Per-day breakdown
 * - Total exercises completed
 * - Weekly consistency
 */
public class ProgressTrackingActivity extends AppCompatActivity {

    private static final String TAG = "ProgressTrackingActivity";
    
    private TextView overallProgressTextView;
    private TextView weeklyBreakdownTextView;

    
    private String userId;
    private WorkoutPlan currentWorkoutPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_tracking);

        initViews();
        
        userId = MainActivity.getUserId(this);
        loadProgressData();
    }

    private void initViews() {
        overallProgressTextView = findViewById(R.id.textView_overall_progress);
        weeklyBreakdownTextView = findViewById(R.id.textView_weekly_breakdown);

        
        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
    }

    private void loadProgressData() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        parseAndDisplayProgress(jsonResponse);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing workout plan", e);
                        showNoDataMessage();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading progress: " + error);
                    showNoDataMessage();
                });
            }
        });
    }

    private void parseAndDisplayProgress(String jsonResponse) throws JSONException {
        JSONArray plansArray = new JSONArray(jsonResponse);
        
        if (plansArray.length() > 0) {
            JSONObject planJson = plansArray.getJSONObject(0);
            String workoutDaysJson = planJson.getString("workout_days");
            
            JSONObject completeJson = new JSONObject();
            completeJson.put("planName", planJson.optString("plan_name", "Your Workout Plan"));
            completeJson.put("goal", planJson.optString("goal", ""));
            completeJson.put("durationWeeks", planJson.optInt("duration_weeks", 4));
            completeJson.put("overallNotes", planJson.optString("overall_notes", ""));
            completeJson.put("workoutDays", new JSONArray(workoutDaysJson));
            
            currentWorkoutPlan = WorkoutParser.parseWorkoutPlan(userId, completeJson.toString());
            
            // Load and merge exercise progress
            loadAndMergeProgress();
        } else {
            showNoDataMessage();
        }
    }

    private void loadAndMergeProgress() {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getExerciseProgress(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String progressJson) {
                runOnUiThread(() -> {
                    WorkoutParser.mergeExerciseProgress(currentWorkoutPlan, progressJson);
                    displayProgressAnalytics();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Could not load progress: " + error);
                    // Continue without progress data
                    displayProgressAnalytics();
                });
            }
        });
    }

    private void displayProgressAnalytics() {
        if (currentWorkoutPlan == null) {
            showNoDataMessage();
            return;
        }

        // Calculate overall progress
        int totalExercises = 0;
        int completedExercises = 0;
        Map<String, int[]> dayProgress = new HashMap<>(); // day -> [completed, total]

        for (WorkoutDay day : currentWorkoutPlan.getWorkoutDays()) {
            int dayTotal = day.getExercises().size();
            int dayCompleted = 0;
            
            for (Exercise exercise : day.getExercises()) {
                totalExercises++;
                if (exercise.isCompleted()) {
                    completedExercises++;
                    dayCompleted++;
                }
            }
            
            dayProgress.put(day.getDay(), new int[]{dayCompleted, dayTotal});
        }

        // Display overall progress
        float overallPercentage = totalExercises > 0 
            ? (float) completedExercises / totalExercises * 100 
            : 0;

        String overallText = String.format(Locale.getDefault(),
                "🎯 Overall Progress\n\n" +
                "%.0f%% Complete\n\n" +
                "%d / %d exercises completed",
                overallPercentage, completedExercises, totalExercises);
        
        overallProgressTextView.setText(overallText);

        // Display weekly breakdown
        StringBuilder weeklyText = new StringBuilder("\n");
        
        for (WorkoutDay day : currentWorkoutPlan.getWorkoutDays()) {
            int[] progress = dayProgress.get(day.getDay());
            int completed = progress[0];
            int total = progress[1];
            float dayPercentage = total > 0 ? (float) completed / total * 100 : 0;
            
            String emoji = dayPercentage == 100 ? "✅" : 
                          dayPercentage > 0 ? "🔸" : "⬜";
            
            weeklyText.append(String.format(Locale.getDefault(),
                    "%s %s: %d/%d (%.0f%%)\n",
                    emoji, day.getDay(), completed, total, dayPercentage));
        }
        
        weeklyBreakdownTextView.setText(weeklyText.toString());

        // Display additional stats
        int completedDays = 0;
        for (int[] progress : dayProgress.values()) {
            if (progress[0] == progress[1] && progress[1] > 0) {
                completedDays++;
            }
        }

        String statsText = String.format(Locale.getDefault(),
                "📊 Statistics\n\n" +
                "Days Completed: %d / %d\n" +
                "Goal: %s\n" +
                "Plan Duration: %d weeks\n\n" +
                "%s",
                completedDays, currentWorkoutPlan.getWorkoutDays().size(),
                currentWorkoutPlan.getGoal(),
                currentWorkoutPlan.getDurationWeeks(),
                getMotivationalMessage(overallPercentage));
    }

    private String getMotivationalMessage(float progress) {
        if (progress == 100) {
            return "🎉 Amazing! You've completed your plan!";
        } else if (progress >= 75) {
            return "💪 Great work! Keep pushing!";
        } else if (progress >= 50) {
            return "🔥 You're halfway there! Don't give up!";
        } else if (progress >= 25) {
            return "👍 Good start! Stay consistent!";
        } else if (progress > 0) {
            return "🌟 Every journey begins with a single step!";
        } else {
            return "🚀 Ready to start? You got this!";
        }
    }

    private void showNoDataMessage() {
        overallProgressTextView.setText("No workout data found.\n\n" +
                "Complete your profile and start a workout plan to track progress.");
    }
}