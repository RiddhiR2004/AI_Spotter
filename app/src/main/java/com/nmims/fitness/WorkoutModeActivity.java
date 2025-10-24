package com.nmims.fitness;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nmims.fitness.api.SupabaseClient;
import com.nmims.fitness.models.Exercise;
import com.nmims.fitness.models.WorkoutDay;
import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * WorkoutModeActivity - Exercise-by-exercise workout mode with timer
 * Shows one exercise at a time with navigation and working stopwatch
 */
public class WorkoutModeActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutModeActivity";

    // Views
    private TextView exerciseNameTextView;
    private TextView exerciseDetailsTextView;
    private TextView targetMusclesTextView;
    private TextView instructionsTextView;
    private TextView progressTextView;
    private TextView timerHoursTextView;
    private TextView timerMinutesTextView;
    private TextView timerSecondsTextView;
    private Button previousButton;
    private Button nextButton;
    private Button markCompleteButton;
    private Button startPauseButton;
    private ProgressBar loadingProgressBar;

    // Data
    private String userId;
    private WorkoutDay todayWorkout;
    private List<Exercise> exercises;
    private int currentExerciseIndex = 0;

    // Stopwatch
    private Handler timerHandler;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updateTime = 0L;
    private boolean isTimerRunning = false;

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime;
            updateTime = timeSwapBuff + timeInMilliseconds;

            int seconds = (int) (updateTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;

            timerSecondsTextView.setText(String.format(Locale.getDefault(), "%02d", seconds));
            timerMinutesTextView.setText(String.format(Locale.getDefault(), "%02d", minutes));
            timerHoursTextView.setText(String.format(Locale.getDefault(), "%02d", hours));

            timerHandler.postDelayed(this, 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_mode);

        initViews();
        setupClickListeners();

        userId = MainActivity.getUserId(this);
        timerHandler = new Handler(Looper.getMainLooper());

        loadTodayWorkout();
    }

    private void initViews() {
        exerciseNameTextView = findViewById(R.id.textView_exercise_name);
        exerciseDetailsTextView = findViewById(R.id.textView_exercise_details);
        targetMusclesTextView = findViewById(R.id.textView_target_muscles);
        instructionsTextView = findViewById(R.id.textView_instructions);
        progressTextView = findViewById(R.id.textView_progress);
        timerHoursTextView = findViewById(R.id.timer_hours);
        timerMinutesTextView = findViewById(R.id.timer_minutes);
        timerSecondsTextView = findViewById(R.id.timer_seconds);
        previousButton = findViewById(R.id.button_previous);
        nextButton = findViewById(R.id.button_next);
        markCompleteButton = findViewById(R.id.button_mark_complete);
        startPauseButton = findViewById(R.id.button_start_pause);
        loadingProgressBar = findViewById(R.id.progress_bar);
        
        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        previousButton.setOnClickListener(v -> navigateToPrevious());
        nextButton.setOnClickListener(v -> navigateToNext());
        markCompleteButton.setOnClickListener(v -> markCurrentExerciseComplete());
        startPauseButton.setOnClickListener(v -> toggleTimer());
    }

    private void loadTodayWorkout() {
        showLoading(true);
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                runOnUiThread(() -> {
                    try {
                        parseAndDisplayWorkout(jsonResponse);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing workout plan", e);
                        showError("Failed to load workout plan");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading workout plan: " + error);
                    showError("Failed to load workout. Please try again.");
                    finish();
                });
            }
        });
    }

    private void parseAndDisplayWorkout(String jsonResponse) throws Exception {
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

            WorkoutPlan workoutPlan = WorkoutParser.parseWorkoutPlan(userId, completeJson.toString());

            // Load progress and merge
            loadAndMergeProgress(workoutPlan);
        } else {
            showError("No workout plan found");
            finish();
        }
    }

    private void loadAndMergeProgress(WorkoutPlan workoutPlan) {
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.getExerciseProgress(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String progressJson) {
                runOnUiThread(() -> {
                    WorkoutParser.mergeExerciseProgress(workoutPlan, progressJson);
                    findAndDisplayTodayWorkout(workoutPlan);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.d(TAG, "No existing progress: " + error);
                    findAndDisplayTodayWorkout(workoutPlan);
                });
            }
        });
    }

    private void findAndDisplayTodayWorkout(WorkoutPlan workoutPlan) {
        String today = new SimpleDateFormat("EEEE", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        todayWorkout = null;
        for (WorkoutDay day : workoutPlan.getWorkoutDays()) {
            if (day.getDay().equalsIgnoreCase(today)) {
                todayWorkout = day;
                break;
            }
        }

        if (todayWorkout == null || todayWorkout.getExercises().isEmpty()) {
            showError("No workout scheduled for today!");
            finish();
            return;
        }

        exercises = todayWorkout.getExercises();
        showLoading(false);
        displayCurrentExercise();
    }

    private void displayCurrentExercise() {
        if (exercises == null || exercises.isEmpty()) {
            return;
        }

        Exercise exercise = exercises.get(currentExerciseIndex);

        // Update exercise info
        exerciseNameTextView.setText(exercise.getName());

        // Build details
        StringBuilder details = new StringBuilder();
        if (exercise.getSets() > 0) {
            details.append("Sets: ").append(exercise.getSets());
        }
        if (exercise.getReps() > 0) {
            if (details.length() > 0) details.append("  •  ");
            details.append("Reps: ").append(exercise.getReps());
        }
        if (exercise.getDuration() != null && !exercise.getDuration().equals("N/A")) {
            if (details.length() > 0) details.append("  •  ");
            details.append("Duration: ").append(exercise.getDuration());
        }
        if (exercise.getRestPeriod() != null && !exercise.getRestPeriod().isEmpty()) {
            if (details.length() > 0) details.append("\n");
            details.append("Rest: ").append(exercise.getRestPeriod());
        }
        exerciseDetailsTextView.setText(details.toString());

        targetMusclesTextView.setText("Target: " + exercise.getTargetMuscles());
        instructionsTextView.setText(exercise.getInstructions());

        // Update progress indicator
        progressTextView.setText(String.format(Locale.getDefault(), 
                "Exercise %d of %d", currentExerciseIndex + 1, exercises.size()));

        // Update button states
        previousButton.setEnabled(currentExerciseIndex > 0);
        nextButton.setEnabled(currentExerciseIndex < exercises.size() - 1);

        // Update complete button
        if (exercise.isCompleted()) {
            markCompleteButton.setText("✓ Completed");
            markCompleteButton.setEnabled(false);
        } else {
            markCompleteButton.setText("Mark Complete");
            markCompleteButton.setEnabled(true);
        }

        // If this is the last exercise and it's completed, show finish option
        if (currentExerciseIndex == exercises.size() - 1 && exercise.isCompleted()) {
            nextButton.setText("Finish Workout");
            nextButton.setEnabled(true);
        } else {
            nextButton.setText("Next Exercise");
        }
    }

    private void navigateToPrevious() {
        if (currentExerciseIndex > 0) {
            currentExerciseIndex--;
            displayCurrentExercise();
        }
    }

    private void navigateToNext() {
        if (currentExerciseIndex < exercises.size() - 1) {
            currentExerciseIndex++;
            displayCurrentExercise();
        } else {
            // Last exercise - finish workout
            finishWorkout();
        }
    }

    private void markCurrentExerciseComplete() {
        if (exercises == null || currentExerciseIndex >= exercises.size()) {
            return;
        }

        Exercise exercise = exercises.get(currentExerciseIndex);
        exercise.setCompleted(true);

        // Save to database
        SupabaseClient supabaseClient = new SupabaseClient();
        supabaseClient.updateExerciseProgress(userId, todayWorkout.getDay(), 
                exercise.getName(), true, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(WorkoutModeActivity.this, 
                            "Exercise completed! 🎉", Toast.LENGTH_SHORT).show();
                    displayCurrentExercise();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to save progress: " + error);
                runOnUiThread(() -> displayCurrentExercise());
            }
        });
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            // Pause timer
            timeSwapBuff += timeInMilliseconds;
            timerHandler.removeCallbacks(updateTimerThread);
            startPauseButton.setText("Resume Timer");
            isTimerRunning = false;
        } else {
            // Start/Resume timer
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(updateTimerThread, 0);
            startPauseButton.setText("Pause Timer");
            isTimerRunning = true;
        }
    }

    private void finishWorkout() {
        new AlertDialog.Builder(this)
                .setTitle("Workout Complete! 🎉")
                .setMessage("Great job! You've completed today's workout.\n\n" +
                        "Total time: " + getFormattedTime())
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private String getFormattedTime() {
        int seconds = (int) (updateTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_content).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause timer when app goes to background
        if (isTimerRunning) {
            timeSwapBuff += timeInMilliseconds;
            timerHandler.removeCallbacks(updateTimerThread);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume timer if it was running
        if (isTimerRunning) {
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(updateTimerThread, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up timer
        if (timerHandler != null) {
            timerHandler.removeCallbacks(updateTimerThread);
        }
    }
}

