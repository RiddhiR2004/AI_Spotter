package com.nmims.fitness;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * PersonalDetailsActivity - Step 1 of 3
 * Collects basic user info: Name, Email, Age, Height, Weight.
 * Passes data to PersonalDetailStep2Activity.
 */
public class PersonalDetailsActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, ageEditText, heightEditText, weightEditText;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        initViews();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.editText_name);
        emailEditText = findViewById(R.id.editText_email);
        ageEditText = findViewById(R.id.editText_age);
        heightEditText = findViewById(R.id.editText_height);
        weightEditText = findViewById(R.id.editText_weight);
        nextButton = findViewById(R.id.button_next);

        nextButton.setOnClickListener(v -> collectAndProceed());
    }

    private void collectAndProceed() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String heightStr = heightEditText.getText().toString().trim();
        String weightStr = weightEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty() ||
                heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse values to validate
            int age = Integer.parseInt(ageStr);
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);

            // Create intent for the next step
            // *** UPDATED: Navigates to PersonalDetailStep2Activity ***
            Intent intent = new Intent(PersonalDetailsActivity.this, PersonalDetailStep2Activity.class);

            // Pass all data to the next activity
            intent.putExtra("USER_NAME", name);
            intent.putExtra("USER_EMAIL", email);
            intent.putExtra("USER_AGE", age);
            intent.putExtra("USER_HEIGHT", height);
            intent.putExtra("USER_WEIGHT", weight);

            startActivity(intent);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for age, height, and weight", Toast.LENGTH_SHORT).show();
        }
    }
}