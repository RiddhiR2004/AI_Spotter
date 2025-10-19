package com.nmims.fitness; // Make sure this matches your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PersonalDetailsActivity extends AppCompatActivity {

    // 1. Declare the UI elements
    EditText nameEditText, emailEditText, ageEditText, heightEditText, weightEditText;
    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        // 2. Link the UI elements from XML to Java variables
        nameEditText = findViewById(R.id.editText_name);
        emailEditText = findViewById(R.id.editText_email);
        ageEditText = findViewById(R.id.editText_age);
        heightEditText = findViewById(R.id.editText_height);
        weightEditText = findViewById(R.id.editText_weight);
        nextButton = findViewById(R.id.button_next);

        // 3. Set a click listener on the button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the code that runs when the button is clicked
                collectAndProceed();
            }
        });
    }

    private void collectAndProceed() {
        // 4. Get the text from the EditText fields
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String heightStr = heightEditText.getText().toString().trim();
        String weightStr = weightEditText.getText().toString().trim();

        // 5. Basic Validation: Check if any field is empty
        if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return; // Stop the function if validation fails
        }

        // 6. Navigate to the next activity and pass the data
        // NOTE: You need to create "NextActivity.class" for this to work
        Intent intent = new Intent(PersonalDetailsActivity.this, NextActivity.class);

        // Put the data into the intent to carry it to the next screen
        intent.putExtra("USER_NAME", name);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_AGE", Integer.parseInt(ageStr));
        intent.putExtra("USER_HEIGHT", Double.parseDouble(heightStr));
        intent.putExtra("USER_WEIGHT", Double.parseDouble(weightStr));

        startActivity(intent);
    }
}