package com.nmims.fitness.utils;

import android.util.Log;

import com.nmims.fitness.models.Exercise;
import com.nmims.fitness.models.WorkoutDay;
import com.nmims.fitness.models.WorkoutPlan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorkoutParser {
    private static final String TAG = "WorkoutParser";

    /**
     * Parses Gemini's JSON response into a WorkoutPlan object
     * Handles markdown code blocks if Gemini wraps JSON in ```json...```
     */
    public static WorkoutPlan parseWorkoutPlan(String userId, String jsonResponse) throws JSONException {
        // Clean the response in case Gemini wraps it in markdown code blocks
        String cleanJson = cleanJsonResponse(jsonResponse);
        
        Log.d(TAG, "Parsing JSON response of length: " + cleanJson.length());
        
        // Validate JSON is complete
        if (!cleanJson.trim().endsWith("}") && !cleanJson.trim().endsWith("]")) {
            Log.e(TAG, "JSON appears truncated - last 100 chars: " + 
                  cleanJson.substring(Math.max(0, cleanJson.length() - 100)));
            throw new JSONException("Response appears truncated. Please try again.");
        }
        
        JSONObject json = new JSONObject(cleanJson);
        
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setPlanName(json.optString("planName", "Personalized Workout Plan"));
        plan.setGoal(json.optString("goal", ""));
        plan.setDurationWeeks(json.optInt("durationWeeks", 4));
        plan.setOverallNotes(json.optString("overallNotes", ""));

        // Parse workout days
        if (!json.has("workoutDays")) {
            throw new JSONException("Missing 'workoutDays' field in response");
        }
        
        JSONArray workoutDaysArray = json.getJSONArray("workoutDays");
        for (int i = 0; i < workoutDaysArray.length(); i++) {
            JSONObject dayJson = workoutDaysArray.getJSONObject(i);
            WorkoutDay workoutDay = parseWorkoutDay(dayJson);
            plan.addWorkoutDay(workoutDay);
        }

        Log.d(TAG, "Successfully parsed workout plan with " + plan.getWorkoutDays().size() + " days");
        return plan;
    }

    private static WorkoutDay parseWorkoutDay(JSONObject json) throws JSONException {
        WorkoutDay day = new WorkoutDay();
        day.setDay(json.getString("day"));
        day.setFocus(json.optString("focus", ""));
        day.setNotes(json.optString("notes", ""));

        // Parse exercises
        JSONArray exercisesArray = json.getJSONArray("exercises");
        for (int i = 0; i < exercisesArray.length(); i++) {
            JSONObject exerciseJson = exercisesArray.getJSONObject(i);
            Exercise exercise = parseExercise(exerciseJson);
            day.addExercise(exercise);
        }

        return day;
    }

    private static Exercise parseExercise(JSONObject json) throws JSONException {
        Exercise exercise = new Exercise();
        exercise.setName(json.getString("name"));
        exercise.setSets(json.optInt("sets", 0));
        exercise.setReps(json.optInt("reps", 0));
        exercise.setDuration(json.optString("duration", "N/A"));
        exercise.setRestPeriod(json.optString("restPeriod", "60 seconds"));
        exercise.setInstructions(json.optString("instructions", ""));
        exercise.setTargetMuscles(json.optString("targetMuscles", ""));
        exercise.setCompleted(false);

        return exercise;
    }

    /**
     * Cleans JSON response by removing markdown code blocks
     * Gemini sometimes returns: ```json\n{...}\n```
     */
    private static String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    /**
     * Converts WorkoutPlan to JSON for Supabase storage
     */
    public static JSONObject workoutPlanToJson(WorkoutPlan plan) throws JSONException {
        JSONObject json = new JSONObject();
        // Convert user_id string to long for bigint field in database
        try {
            json.put("user_id", Long.parseLong(plan.getUserId()));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid user_id format: " + plan.getUserId(), e);
            throw new JSONException("Invalid user_id format");
        }
        json.put("plan_name", plan.getPlanName());
        json.put("goal", plan.getGoal());
        json.put("duration_weeks", plan.getDurationWeeks());
        json.put("overall_notes", plan.getOverallNotes());

        JSONArray daysArray = new JSONArray();
        for (WorkoutDay day : plan.getWorkoutDays()) {
            daysArray.put(workoutDayToJson(day));
        }
        json.put("workout_days", daysArray);

        return json;
    }

    private static JSONObject workoutDayToJson(WorkoutDay day) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("day", day.getDay());
        json.put("focus", day.getFocus());
        json.put("notes", day.getNotes());

        JSONArray exercisesArray = new JSONArray();
        for (Exercise exercise : day.getExercises()) {
            exercisesArray.put(exerciseToJson(exercise));
        }
        json.put("exercises", exercisesArray);

        return json;
    }

    private static JSONObject exerciseToJson(Exercise exercise) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", exercise.getName());
        json.put("sets", exercise.getSets());
        json.put("reps", exercise.getReps());
        json.put("duration", exercise.getDuration());
        json.put("rest_period", exercise.getRestPeriod());
        json.put("instructions", exercise.getInstructions());
        json.put("target_muscles", exercise.getTargetMuscles());
        json.put("completed", exercise.isCompleted());

        return json;
    }

    /**
     * Merge exercise progress from Supabase into WorkoutPlan
     * @param workoutPlan The workout plan to update
     * @param progressJson JSON array from exercise_progress table
     */
    public static void mergeExerciseProgress(WorkoutPlan workoutPlan, String progressJson) {
        try {
            JSONArray progressArray = new JSONArray(progressJson);
            
            // Create a map of exercise completions: "day|exerciseName" -> completed
            java.util.Map<String, Boolean> progressMap = new java.util.HashMap<>();
            
            for (int i = 0; i < progressArray.length(); i++) {
                JSONObject progress = progressArray.getJSONObject(i);
                String day = progress.getString("day");
                String exerciseName = progress.getString("exercise_name");
                boolean completed = progress.getBoolean("completed");
                
                String key = day + "|" + exerciseName;
                progressMap.put(key, completed);
            }
            
            // Update workout plan with progress
            for (WorkoutDay day : workoutPlan.getWorkoutDays()) {
                for (Exercise exercise : day.getExercises()) {
                    String key = day.getDay() + "|" + exercise.getName();
                    if (progressMap.containsKey(key)) {
                        exercise.setCompleted(progressMap.get(key));
                    }
                }
            }
            
            Log.d(TAG, "Successfully merged " + progressMap.size() + " exercise progress entries");
        } catch (JSONException e) {
            Log.e(TAG, "Error merging exercise progress", e);
        }
    }
}

