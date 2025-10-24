package com.nmims.fitness.api;

import android.util.Log;

import org.json.JSONArray;
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

public class GeminiApiClient {
    private static final String TAG = "GeminiApiClient";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    // TODO: Replace with your actual Gemini API key
    // Get it from: https://makersuite.google.com/app/apikey
    private static final String GEMINI_API_KEY = "AIzaSyB0kg44gKphUVsTlxrn8X0ev97Y3FJLUHU";
    
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public GeminiApiClient() {
        // Increase timeout for Gemini API (can take 15-30 seconds to generate workout plans)
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void generateWorkoutPlan(String name, int age, double bmi, String bmiCategory, 
                                   String goal, String activityFrequency, double availableHours,
                                   boolean hasGymEquipment, String injuries, 
                                   GeminiCallback callback) {
        
        String prompt = buildWorkoutPrompt(name, age, bmi, bmiCategory, goal, 
                                          activityFrequency, availableHours, 
                                          hasGymEquipment, injuries);

        try {
            JSONObject requestJson = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestJson.put("contents", contents);

            // Add generation config for better JSON output
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 16384);
            requestJson.put("generationConfig", generationConfig);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Gemini API request failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        String responseBody = response.body().string();
                        
                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                String generatedText = extractTextFromResponse(jsonResponse);
                                Log.d(TAG, "Gemini response length: " + generatedText.length() + " characters");
                                
                                // Check if response seems truncated
                                if (!generatedText.trim().endsWith("}") && !generatedText.trim().endsWith("```")) {
                                    Log.w(TAG, "Warning: Response may be truncated");
                                }
                                
                                callback.onSuccess(generatedText);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing Gemini response", e);
                                Log.e(TAG, "Response body: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                                callback.onError("Failed to parse response: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, "Gemini API error: " + response.code() + " - " + responseBody);
                            callback.onError("API error: " + response.code());
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error building request", e);
            callback.onError("Failed to build request: " + e.getMessage());
        }
    }

    private String extractTextFromResponse(JSONObject response) throws JSONException {
        JSONArray candidates = response.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text");
            }
        }
        throw new JSONException("No text found in response");
    }

    private String buildWorkoutPrompt(String name, int age, double bmi, String bmiCategory,
                                     String goal, String activityFrequency, double availableHours,
                                     boolean hasGymEquipment, String injuries) {
        
        String injuryInfo = (injuries == null || injuries.isEmpty()) ? "None" : injuries;
        String equipment = hasGymEquipment ? "Has access to gym equipment" : "No gym equipment (bodyweight only)";

        return String.format(
            "You are a professional fitness trainer. Create a personalized 7-day workout plan for:\n\n" +
            "USER PROFILE:\n" +
            "- Name: %s\n" +
            "- Age: %d years\n" +
            "- BMI: %.2f (%s)\n" +
            "- Goal: %s\n" +
            "- Activity Frequency: %s\n" +
            "- Available Time: %.1f hours per session\n" +
            "- Equipment: %s\n" +
            "- Injuries/Limitations: %s\n\n" +
            
            "IMPORTANT: Respond ONLY with valid JSON in this exact format (no markdown, no code blocks, just raw JSON):\n\n" +
            "{\n" +
            "  \"planName\": \"Brief plan name\",\n" +
            "  \"durationWeeks\": 4,\n" +
            "  \"overallNotes\": \"General advice and tips\",\n" +
            "  \"workoutDays\": [\n" +
            "    {\n" +
            "      \"day\": \"Monday\",\n" +
            "      \"focus\": \"Upper Body Strength\",\n" +
            "      \"notes\": \"Tips for today\",\n" +
            "      \"exercises\": [\n" +
            "        {\n" +
            "          \"name\": \"Push-ups\",\n" +
            "          \"sets\": 3,\n" +
            "          \"reps\": 12,\n" +
            "          \"duration\": \"N/A\",\n" +
            "          \"restPeriod\": \"60 seconds\",\n" +
            "          \"instructions\": \"Keep core tight, lower chest to ground\",\n" +
            "          \"targetMuscles\": \"Chest, Triceps, Shoulders\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            
            "Create a complete 7-day plan (Monday-Sunday) with 4-6 exercises per day. " +
            "Consider rest days if needed. For cardio/endurance exercises, use 'duration' instead of 'reps'. " +
            "Make it progressive and suitable for the user's BMI category and goals.",
            
            name, age, bmi, bmiCategory, goal, activityFrequency, availableHours, equipment, injuryInfo
        );
    }
}

