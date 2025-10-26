package com.nmims.fitness;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.util.Locale;

/**
 * PersonalDetailStep2Activity - Step 2 of 3
 * Collects fitness habits: Activity frequency and session time.
 * Receives data from Step 1.
 * Passes all data to PersonalDetailStep3Activity.
 */
// *** RENAMED CLASS ***
public class PersonalDetailStep2Activity extends AppCompatActivity {

    // Data from Step 1
    private String name, email;
    private int age;
    private double height, weight;
    private int pullUps, dips, pushUps;

    // UI Elements
    private AutoCompleteTextView activeDropdown;
    private Slider timeSlider;
    private TextView timeValueTextView;
    private Button nextButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Matches your XML file name
        setContentView(R.layout.activity_personal_detail_step2);

        // Get data from Step 1
        if (!receiveIntentData()) {
            // If data is missing, go back to Step 1
            Toast.makeText(this, "Error: Missing user data. Returning to first step.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, PersonalDetailsActivity.class));
            finish();
            return;
        }

        initViews();
        setupDropdown();
        setupSlider();
        setupClickListeners();
    }

    private boolean receiveIntentData() {
        Intent intent = getIntent();
        name = intent.getStringExtra("USER_NAME");
        email = intent.getStringExtra("USER_EMAIL");
        age = intent.getIntExtra("USER_AGE", -1);
        height = intent.getDoubleExtra("USER_HEIGHT", -1.0);
        weight = intent.getDoubleExtra("USER_WEIGHT", -1.0);
        pullUps = intent.getIntExtra("CURRENT_PULLUPS", 0);
        dips = intent.getIntExtra("CURRENT_DIPS", 0);
        pushUps = intent.getIntExtra("CURRENT_PUSHUPS", 0);

        // Check if any data is missing
        return name != null && email != null && age != -1 && height != -1.0 && weight != -1.0;
    }

    private void initViews() {
        activeDropdown = findViewById(R.id.autoComplete_active);
        timeSlider = findViewById(R.id.slider_time);
        timeValueTextView = findViewById(R.id.textView_timeValue);
        nextButton = findViewById(R.id.button_next);
        backButton = findViewById(R.id.button_back);
    }

    private void setupDropdown() {
        String[] activityLevels = {"1-2 times/week", "3-4 times/week", "5+ times/week", "Sedentary"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, activityLevels);
        activeDropdown.setAdapter(adapter);

        // Set default value
        activeDropdown.setText("3-4 times/week", false);
    }

    private void setupSlider() {
        timeSlider.addOnChangeListener((slider, value, fromUser) -> {
            // Update the TextView as the slider moves
            timeValueTextView.setText(String.format(Locale.getDefault(), "%.0f mins", value));
        });

        // Set initial value
        timeValueTextView.setText(String.format(Locale.getDefault(), "%.0f mins", timeSlider.getValue()));
    }

    private void setupClickListeners() {
        nextButton.setOnClickListener(v -> {
            String activityFrequency = activeDropdown.getText().toString();
            if (activityFrequency.isEmpty()) {
                Toast.makeText(this, "Please select your activity frequency", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get slider value (in minutes) and convert to hours for the API
            float timeInMinutes = timeSlider.getValue();
            double availableHours = timeInMinutes / 60.0;

            // Create intent for the next step
            // *** UPDATED: Navigates to PersonalDetailStep3Activity ***
            Intent intent = new Intent(this, PersonalDetailStep3Activity.class);

            // Pass Step 1 data
            intent.putExtra("USER_NAME", name);
            intent.putExtra("USER_EMAIL", email);
            intent.putExtra("USER_AGE", age);
            intent.putExtra("USER_HEIGHT", height);
            intent.putExtra("USER_WEIGHT", weight);
            intent.putExtra("CURRENT_PULLUPS", pullUps);
            intent.putExtra("CURRENT_DIPS", dips);
            intent.putExtra("CURRENT_PUSHUPS", pushUps);

            // Pass Step 2 data
            intent.putExtra("ACTIVITY_FREQUENCY", activityFrequency);
            intent.putExtra("AVAILABLE_HOURS", availableHours);

            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            // Go back to the previous activity
            finish();
        });
    }
}