package com.nmims.fitness;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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

public class MainActivity extends AppCompatActivity {

    // === 🔑 Supabase Configuration ===
    private static final String SUPABASE_URL = "https://rvqrakvctwekgewdunhw.supabase.co";
    // IMPORTANT: It's better practice to store keys in build.gradle or a secrets file, not directly in code.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ2cXJha3ZjdHdla2dld2R1bmh3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1OTYwMDEsImV4cCI6MjA3NjE3MjAwMX0.XuaY9Yn1KZNe9vQ8pGT2xxlSKnAbWpKeazUCumfWygg";
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Run network operations on a background thread
        new Thread(() -> {
            try {
                insertSurveyData("John Doe", "john@example.com", 28, 180, 75);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "❌ Error while preparing or sending data", e);
            }
        }).start();
    }

    // === 📤 Supabase Insert ===
    private void insertSurveyData(String name, String email, int age, double height, double weight) throws IOException, JSONException {
        double bmi = weight / Math.pow(height / 100.0, 2);
        String bmiCategory = getBMICategory(bmi);

        // ✨ Use JSONObject for safer JSON creation
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
        jsonObject.put("injuries", JSONObject.NULL); // Correctly handles JSON null

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/surveys")
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // Using asynchronous call for better practice, though synchronous works in a background thread.
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Supabase request failed.", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) { // try-with-resources to ensure response body is closed
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Supabase insert successful! Code: " + response.code());
                        Log.d(TAG, "Response Body: " + responseBody);
                    } else {
                        Log.e(TAG, "❌ Supabase insert failed! Code: " + response.code());
                        Log.e(TAG, "Response Body: " + responseBody);
                    }
                }
            }
        });
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal"; // Adjusted to standard < 25
        if (bmi < 30) return "Overweight"; // Adjusted to standard < 30
        return "Obese";
    }
}