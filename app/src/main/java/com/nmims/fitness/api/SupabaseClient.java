package com.nmims.fitness.api;

import android.util.Log;

import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

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

public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static final String SUPABASE_URL = "https://rvqrakvctwekgewdunhw.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ2cXJha3ZjdHdla2dld2R1bmh3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1OTYwMDEsImV4cCI6MjA3NjE3MjAwMX0.XuaY9Yn1KZNe9vQ8pGT2xxlSKnAbWpKeazUCumfWygg";
    
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public SupabaseClient() {
        this.client = new OkHttpClient();
    }

    public interface SupabaseCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Save workout plan to Supabase
     */
    public void saveWorkoutPlan(WorkoutPlan workoutPlan, SupabaseCallback callback) {
        try {
            JSONObject json = WorkoutParser.workoutPlanToJson(workoutPlan);
            
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/workout_plans")
                    .post(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to save workout plan", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        String responseBody = response.body().string();
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Workout plan saved successfully");
                            callback.onSuccess("Workout plan saved!");
                        } else {
                            Log.e(TAG, "Failed to save workout plan: " + response.code() + " - " + responseBody);
                            callback.onError("Failed to save: " + response.code());
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error converting workout plan to JSON", e);
            callback.onError("Failed to convert data: " + e.getMessage());
        }
    }

    /**
     * Update exercise completion status
     */
    public void updateExerciseProgress(String userId, String day, String exerciseName, 
                                      boolean completed, SupabaseCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("day", day);
            json.put("exercise_name", exerciseName);
            json.put("completed", completed);
            json.put("completed_at", completed ? System.currentTimeMillis() : JSONObject.NULL);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/exercise_progress")
                    .post(body)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to update exercise progress", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Exercise progress updated");
                            callback.onSuccess("Progress saved!");
                        } else {
                            Log.e(TAG, "Failed to update progress: " + response.code());
                            callback.onError("Failed to save progress");
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating progress JSON", e);
            callback.onError("Failed to create data: " + e.getMessage());
        }
    }

    /**
     * Get workout plan for user
     */
    public void getWorkoutPlan(String userId, WorkoutPlanCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/workout_plans?user_id=eq." + userId + "&order=created_at.desc&limit=1")
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch workout plan", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        Log.e(TAG, "Failed to fetch workout plan: " + response.code());
                        callback.onError("Failed to fetch plan: " + response.code());
                    }
                }
            }
        });
    }

    public interface WorkoutPlanCallback {
        void onSuccess(String jsonResponse);
        void onError(String error);
    }

    /**
     * Get exercise progress for user
     */
    public void getExerciseProgress(String userId, SupabaseCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/exercise_progress?user_id=eq." + userId)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch exercise progress", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = response.body().string();
                    
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        Log.e(TAG, "Failed to fetch exercise progress: " + response.code());
                        callback.onError("Failed to fetch progress: " + response.code());
                    }
                }
            }
        });
    }
}

