package com.nmims.fitness.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.nmims.fitness.models.WorkoutPlan;
import com.nmims.fitness.utils.WorkoutParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AICoachService - RAG-based AI fitness coach
 * Combines SystemPrompt.md + user survey data + workout plan for personalized coaching
 */
public class AICoachService {
    
    private static final String TAG = "AICoachService";
    private static final String GEMINI_API_KEY = "AIzaSyB0kg44gKphUVsTlxrn8X0ev97Y3FJLUHU";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;
    
    private final Context context;
    private final OkHttpClient client;
    private final SupabaseClient supabaseClient;
    private final SharedPreferences chatPrefs;
    
    // Large timeouts for large context processing
    private static final int CONNECT_TIMEOUT = 30; // seconds
    private static final int READ_TIMEOUT = 90; // seconds - Gemini needs time to process large context
    private static final int WRITE_TIMEOUT = 30; // seconds
    
    // SharedPreferences keys
    private static final String PREF_NAME = "ai_coach_prefs";
    private static final String KEY_CHAT_HISTORY = "chat_history";
    private static final String KEY_PENDING_MESSAGES = "pending_messages";
    
    // RAG Context - loaded once
    private String systemPrompt = "";
    private String userSurveyData = "";
    private String userWorkoutPlan = "";
    private List<ChatMessage> conversationHistory;
    private List<String> pendingMessages;
    
    public AICoachService(Context context) {
        this.context = context;
        
        // Configure OkHttp with longer timeouts for large context processing
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        this.supabaseClient = new SupabaseClient();
        this.chatPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.conversationHistory = new ArrayList<>();
        this.pendingMessages = new ArrayList<>();
        
        // Load system prompt from assets
        loadSystemPrompt();
        
        // Load conversation history from cache
        loadConversationHistory();
        
        // Load pending messages
        loadPendingMessages();
    }

    /**
     * Load SystemPrompt.md from assets
     */
    private void loadSystemPrompt() {
        try {
            InputStream is = context.getAssets().open("SystemPrompt.md");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            systemPrompt = sb.toString();
            reader.close();
            Log.d(TAG, "System prompt loaded: " + systemPrompt.length() + " characters");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to load system prompt", e);
            systemPrompt = "You are an expert fitness coach specializing in calisthenics and weighted training.";
        }
    }

    /**
     * Load user context (survey data + workout plan)
     */
    public void loadUserContext(String userId, ContextLoadCallback callback) {
        // Load survey data
        supabaseClient.getUserProfile(userId, new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(String surveyJson) {
                userSurveyData = surveyJson;
                Log.d(TAG, "Survey data loaded");
                
                // Load workout plan
                supabaseClient.getWorkoutPlan(userId, new SupabaseClient.WorkoutPlanCallback() {
                    @Override
                    public void onSuccess(String planJson) {
                        userWorkoutPlan = planJson;
                        Log.d(TAG, "Workout plan loaded");
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "No workout plan found: " + error);
                        callback.onSuccess(); // Still success, just no plan yet
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load survey data: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Get AI coach response with full RAG context (async, continues in background)
     */
    public void getCoachResponse(String userMessage, CoachResponseCallback callback) {
        // Add user message to history
        conversationHistory.add(new ChatMessage("user", userMessage));
        saveConversationHistory();
        
        // Add to pending messages for background processing
        pendingMessages.add(userMessage);
        savePendingMessages();
        
        // Build the full context
        String fullContext = buildFullContext(userMessage);
        
        // Call Gemini API (async)
        callGeminiAPI(fullContext, new CoachResponseCallback() {
            @Override
            public void onSuccess(String response) {
                // Remove from pending when complete
                pendingMessages.remove(userMessage);
                savePendingMessages();
                
                // Call original callback
                callback.onSuccess(response);
            }

            @Override
            public void onError(String error) {
                // Keep in pending on error so user can retry
                callback.onError(error);
            }
        });
    }

    /**
     * Build full RAG context
     */
    private String buildFullContext(String userMessage) {
        StringBuilder context = new StringBuilder();
        
        // Add system prompt
        context.append("SYSTEM INSTRUCTIONS:\n");
        context.append(systemPrompt);
        context.append("\n\n");
        
        // Add user's survey data
        if (!userSurveyData.isEmpty()) {
            context.append("USER PROFILE:\n");
            context.append(formatSurveyData(userSurveyData));
            context.append("\n\n");
        }
        
        // Add user's workout plan
        if (!userWorkoutPlan.isEmpty()) {
            context.append("USER'S CURRENT WORKOUT PLAN:\n");
            context.append(formatWorkoutPlan(userWorkoutPlan));
            context.append("\n\n");
        }
        
        // Add conversation history (last 10 messages)
        if (conversationHistory.size() > 1) {
            context.append("CONVERSATION HISTORY:\n");
            int start = Math.max(0, conversationHistory.size() - 10);
            for (int i = start; i < conversationHistory.size() - 1; i++) {
                ChatMessage msg = conversationHistory.get(i);
                context.append(msg.role).append(": ").append(msg.content).append("\n");
            }
            context.append("\n");
        }
        
        // Add current user question
        context.append("USER QUESTION:\n");
        context.append(userMessage);
        context.append("\n\n");
        context.append("Please provide a detailed, personalized response based on the system instructions, ");
        context.append("the user's profile, and their current workout plan. Be specific and reference their data when relevant.");
        
        return context.toString();
    }

    /**
     * Format survey data for context
     */
    private String formatSurveyData(String jsonData) {
        try {
            JSONArray array = new JSONArray(jsonData);
            if (array.length() > 0) {
                JSONObject survey = array.getJSONObject(0);
                
                StringBuilder formatted = new StringBuilder();
                formatted.append("Name: ").append(survey.optString("name", "N/A")).append("\n");
                formatted.append("Age: ").append(survey.optInt("age", 0)).append(" years\n");
                formatted.append("Height: ").append(survey.optDouble("height_cm", 0)).append(" cm\n");
                formatted.append("Weight: ").append(survey.optDouble("weight_kg", 0)).append(" kg\n");
                formatted.append("BMI: ").append(survey.optDouble("bmi", 0)).append(" (").append(survey.optString("bmi_category", "N/A")).append(")\n");
                formatted.append("Activity Frequency: ").append(survey.optString("activity_frequency", "N/A")).append("\n");
                formatted.append("Available Hours: ").append(survey.optDouble("available_hours", 0)).append(" hours\n");
                formatted.append("Has Gym Equipment: ").append(survey.optBoolean("gym_equipment", false) ? "Yes" : "No").append("\n");
                formatted.append("Goals: ").append(survey.optString("goals", "N/A")).append("\n");
                formatted.append("Injuries/Limitations: ").append(survey.optString("injuries", "None")).append("\n");
                formatted.append("Pushups: ").append(survey.optInt("current_pushups", 0)).append("\n");
                formatted.append("Dips: ").append(survey.optInt("current_dips", 0)).append("\n");
                formatted.append("Pullups: ").append(survey.optInt("current_pullups", 0)).append("\n");
                return formatted.toString();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error formatting survey data", e);
        }
        
        return "No profile data available";
    }

    /**
     * Format workout plan for context
     */
    private String formatWorkoutPlan(String jsonData) {
        try {
            JSONArray array = new JSONArray(jsonData);
            if (array.length() > 0) {
                JSONObject planObj = array.getJSONObject(0);
                String workoutDaysJson = planObj.getString("workout_days");
                
                JSONObject completeJson = new JSONObject();
                completeJson.put("planName", planObj.optString("plan_name", "Workout Plan"));
                completeJson.put("goal", planObj.optString("goal", ""));
                completeJson.put("durationWeeks", planObj.optInt("duration_weeks", 4));
                completeJson.put("overallNotes", planObj.optString("overall_notes", ""));
                completeJson.put("workoutDays", new JSONArray(workoutDaysJson));
                
                // Parse and format
                WorkoutPlan plan = WorkoutParser.parseWorkoutPlan("", completeJson.toString());
                
                StringBuilder formatted = new StringBuilder();
                formatted.append("Plan: ").append(plan.getPlanName()).append("\n");
                formatted.append("Goal: ").append(plan.getGoal()).append("\n");
                formatted.append("Duration: ").append(plan.getDurationWeeks()).append(" weeks\n\n");
                
                formatted.append("Workout Days:\n");
                plan.getWorkoutDays().forEach(day -> {
                    formatted.append("- ").append(day.getDay()).append(": ").append(day.getFocus()).append("\n");
                    formatted.append("  Exercises: ");
                    day.getExercises().forEach(ex -> {
                        formatted.append(ex.getName()).append(", ");
                    });
                    formatted.append("\n");
                });
                
                return formatted.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting workout plan", e);
        }
        
        return "No workout plan generated yet";
    }

    /**
     * Call Gemini API
     */
    private void callGeminiAPI(String prompt, CoachResponseCallback callback) {
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

            RequestBody body = RequestBody.create(
                    requestJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String errorMsg;
                    if (e instanceof java.net.SocketTimeoutException) {
                        Log.e(TAG, "Gemini API timeout - response took too long", e);
                        errorMsg = "The AI is processing a lot of information and took too long. Please try asking a shorter question or try again.";
                    } else {
                        Log.e(TAG, "Gemini API call failed", e);
                        errorMsg = "Network error: " + e.getMessage();
                    }
                    callback.onError(errorMsg);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        String responseBody = response.body().string();
                        
                        if (response.isSuccessful()) {
                            String aiResponse = parseGeminiResponse(responseBody);
                            
                            // Add AI response to history
                            conversationHistory.add(new ChatMessage("assistant", aiResponse));
                            saveConversationHistory();
                            
                            callback.onSuccess(aiResponse);
                        } else {
                            Log.e(TAG, "Gemini API error: " + response.code() + " - " + responseBody);
                            callback.onError("API error: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        callback.onError("Processing error: " + e.getMessage());
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "Error building request", e);
            callback.onError("Request building error: " + e.getMessage());
        }
    }

    /**
     * Parse Gemini API response
     */
    private String parseGeminiResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONArray candidates = json.getJSONArray("candidates");
        
        if (candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            
            if (parts.length() > 0) {
                JSONObject part = parts.getJSONObject(0);
                return part.getString("text");
            }
        }
        
        return "Sorry, I couldn't generate a response. Please try again.";
    }

    // Helper class for conversation history
    public static class ChatMessage {
        public String role;
        public String content;
        
        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * Save conversation history to SharedPreferences
     */
    private void saveConversationHistory() {
        try {
            JSONArray historyArray = new JSONArray();
            for (ChatMessage msg : conversationHistory) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.role);
                msgObj.put("content", msg.content);
                historyArray.put(msgObj);
            }
            chatPrefs.edit().putString(KEY_CHAT_HISTORY, historyArray.toString()).apply();
            Log.d(TAG, "Conversation history saved: " + conversationHistory.size() + " messages");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving conversation history", e);
        }
    }

    /**
     * Load conversation history from SharedPreferences
     */
    private void loadConversationHistory() {
        String historyJson = chatPrefs.getString(KEY_CHAT_HISTORY, "[]");
        try {
            JSONArray historyArray = new JSONArray(historyJson);
            conversationHistory.clear();
            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject msgObj = historyArray.getJSONObject(i);
                conversationHistory.add(new ChatMessage(
                    msgObj.getString("role"),
                    msgObj.getString("content")
                ));
            }
            Log.d(TAG, "Conversation history loaded: " + conversationHistory.size() + " messages");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading conversation history", e);
            conversationHistory.clear();
        }
    }

    /**
     * Clear conversation history
     */
    public void clearConversationHistory() {
        conversationHistory.clear();
        pendingMessages.clear();
        chatPrefs.edit()
            .remove(KEY_CHAT_HISTORY)
            .remove(KEY_PENDING_MESSAGES)
            .apply();
        Log.d(TAG, "Conversation history cleared");
    }

    /**
     * Get conversation history
     */
    public List<ChatMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * Save pending messages
     */
    private void savePendingMessages() {
        try {
            JSONArray pendingArray = new JSONArray();
            for (String msg : pendingMessages) {
                pendingArray.put(msg);
            }
            chatPrefs.edit().putString(KEY_PENDING_MESSAGES, pendingArray.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving pending messages", e);
        }
    }

    /**
     * Load pending messages
     */
    private void loadPendingMessages() {
        String pendingJson = chatPrefs.getString(KEY_PENDING_MESSAGES, "[]");
        try {
            JSONArray pendingArray = new JSONArray(pendingJson);
            pendingMessages.clear();
            for (int i = 0; i < pendingArray.length(); i++) {
                pendingMessages.add(pendingArray.getString(i));
            }
            Log.d(TAG, "Pending messages loaded: " + pendingMessages.size());
        } catch (JSONException e) {
            Log.e(TAG, "Error loading pending messages", e);
            pendingMessages.clear();
        }
    }

    /**
     * Check if there are pending messages being processed
     */
    public boolean hasPendingMessages() {
        return !pendingMessages.isEmpty();
    }

    /**
     * Get pending messages count
     */
    public int getPendingMessagesCount() {
        return pendingMessages.size();
    }

    // Callbacks
    public interface ContextLoadCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface CoachResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}