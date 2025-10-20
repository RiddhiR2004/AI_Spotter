# AI Spotter - Gemini-Powered Workout Plan Implementation Guide

## 🎯 Overview

AI Spotter generates personalized workout plans using **Google Gemini 2.5 Flash API** based on user health metrics (BMI, goals, equipment availability, etc.). Each workout day is displayed as an interactive to-do list where users can check off completed exercises.

## 🏗️ Architecture

```
User Profile → Gemini API → Structured JSON → Parser → Data Models → UI (To-Do List)
                                                                    ↓
                                                                Supabase
```

### Data Flow

1. **User Input**: Personal details (age, BMI, goals, equipment) collected
2. **Gemini Generation**: AI creates personalized 7-day workout plan in JSON format
3. **Parsing**: JSON response parsed into `WorkoutPlan`, `WorkoutDay`, and `Exercise` objects
4. **Storage**: Plan saved to Supabase for persistence
5. **Display**: Interactive RecyclerView shows exercises as checkable to-do items
6. **Progress Tracking**: Completed exercises saved to Supabase

## 📦 Project Structure

```
app/src/main/java/com/nmims/fitness/
├── models/
│   ├── Exercise.java          # Single exercise with sets, reps, instructions
│   ├── WorkoutDay.java         # Collection of exercises for one day
│   └── WorkoutPlan.java        # Complete 7-day workout plan
├── api/
│   ├── GeminiApiClient.java    # Gemini API integration
│   └── SupabaseClient.java     # Supabase database operations
├── utils/
│   └── WorkoutParser.java      # JSON parsing & data conversion
├── adapters/
│   └── ExerciseAdapter.java    # RecyclerView adapter for to-do list
└── activities/
    ├── WorkoutPlanActivity.java # Main workout display screen
    └── ...
```

## 🚀 Setup Instructions

### 1. Get Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Replace in `GeminiApiClient.java`:
   ```java
   private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";
   ```

### 2. Configure Supabase

1. Go to your Supabase project dashboard
2. Run the SQL queries from `SUPABASE_SCHEMA.sql` in the SQL Editor
3. API keys are already configured in the code (or update if using a different project)

### 3. Build & Run

```bash
./gradlew assembleDebug
```

## 📝 Data Models

### Exercise

```java
{
  "name": "Push-ups",
  "sets": 3,
  "reps": 12,
  "duration": "N/A",           // For timed exercises like plank
  "restPeriod": "60 seconds",
  "instructions": "Keep core tight...",
  "targetMuscles": "Chest, Triceps",
  "completed": false
}
```

### WorkoutDay

```java
{
  "day": "Monday",
  "focus": "Upper Body Strength",
  "notes": "Focus on form",
  "exercises": [Exercise, ...]
}
```

### WorkoutPlan

```java
{
  "userId": "user_123",
  "planName": "Beginner Strength Training",
  "goal": "Build muscle",
  "durationWeeks": 4,
  "overallNotes": "General advice...",
  "workoutDays": [WorkoutDay, ...]
}
```

## 🤖 Gemini Prompt Structure

The prompt is carefully structured to ensure JSON responses:

```
You are a professional fitness trainer. Create a personalized 7-day workout plan for:

USER PROFILE:
- Name: John Doe
- Age: 25 years
- BMI: 22.5 (Normal)
- Goal: Build muscle
- Activity Frequency: 3-4 times/week
- Available Time: 1.5 hours per session
- Equipment: Has access to gym equipment
- Injuries/Limitations: None

IMPORTANT: Respond ONLY with valid JSON in this exact format...
```

### Why This Works

1. **Explicit Format**: Shows exact JSON structure expected
2. **No Markdown**: Instructs to avoid ```json code blocks
3. **Context-Rich**: Provides all user data for personalization
4. **Structured Output**: Gemini excels at following structured formats

## 🔄 JSON Parsing

### Handling Gemini Responses

Sometimes Gemini wraps JSON in markdown:

````
```json
{ "workoutDays": [...] }
````

````

`WorkoutParser.cleanJsonResponse()` handles this:
```java
private static String cleanJsonResponse(String response) {
    String cleaned = response.trim();
    if (cleaned.startsWith("```json")) {
        cleaned = cleaned.substring(7);
    }
    if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    return cleaned.trim();
}
````

## 🎨 UI Components

### RecyclerView Adapter

`ExerciseAdapter` displays exercises as checkable cards:

```java
public interface OnExerciseClickListener {
    void onExerciseClick(Exercise exercise, int position);  // View details
    void onExerciseChecked(Exercise exercise, int position, boolean isChecked);  // Mark complete
}
```

### Layout Structure

- **TabLayout**: Switch between days (Mon-Sun)
- **RecyclerView**: Scrollable exercise list
- **MaterialCardView**: Each exercise as a card with checkbox
- **ProgressBar**: Loading indicator during generation

## 💾 Supabase Integration

### Tables

1. **surveys**: User profile data
2. **workout_plans**: Generated plans (JSONB for flexibility)
3. **exercise_progress**: Track completion per exercise

### Operations

```java
// Save workout plan
supabaseClient.saveWorkoutPlan(workoutPlan, callback);

// Update progress
supabaseClient.updateExerciseProgress(userId, day, exerciseName, completed, callback);

// Fetch plan
supabaseClient.getWorkoutPlan(userId, callback);
```

## 🔧 Customization

### Change Prompt Strategy

Edit `GeminiApiClient.buildWorkoutPrompt()` to:

- Add more user parameters (fitness level, preferences)
- Change workout duration (7-day → 4-week plan)
- Include specific exercise types (yoga, HIIT, etc.)

### Modify UI

- `item_exercise.xml`: Exercise card design
- `activity_workout_plan.xml`: Main layout
- `ExerciseAdapter`: Card behavior

### Add Features

- **Timer**: Integrate with workout timer
- **Video Tutorials**: Link exercises to YouTube
- **Progress Charts**: Visualize completion rates
- **Social Sharing**: Share achievements

## 🐛 Troubleshooting

### Gemini Returns Invalid JSON

1. Check API key is valid
2. Increase `maxOutputTokens` in generation config
3. Add more explicit instructions to prompt
4. Log raw response: `Log.d(TAG, "Raw: " + response)`

### RecyclerView Not Showing

1. Verify `LinearLayoutManager` is set
2. Check if data list is empty
3. Ensure adapter is attached: `recyclerView.setAdapter(adapter)`

### Supabase Errors

1. Check RLS policies allow anonymous access
2. Verify table schema matches JSON structure
3. Test queries in Supabase SQL Editor

## 📊 Example Gemini Response

```json
{
  "planName": "Beginner Full Body Strength",
  "durationWeeks": 4,
  "overallNotes": "Focus on progressive overload and proper form",
  "workoutDays": [
    {
      "day": "Monday",
      "focus": "Upper Body",
      "notes": "Start with compound movements",
      "exercises": [
        {
          "name": "Push-ups",
          "sets": 3,
          "reps": 10,
          "duration": "N/A",
          "restPeriod": "60 seconds",
          "instructions": "Keep body straight, lower chest to ground",
          "targetMuscles": "Chest, Triceps, Shoulders"
        }
      ]
    }
  ]
}
```

## 🎯 Next Steps

1. **Add Authentication**: Implement user login/signup
2. **Local Caching**: Store plans offline with Room DB
3. **Progress Analytics**: Charts showing weekly progress
4. **Exercise Details Screen**: Full instructions with images
5. **Notifications**: Daily workout reminders
6. **AI Form Checking**: Use Gemini Vision for exercise form feedback

## 📚 Resources

- [Gemini API Docs](https://ai.google.dev/docs)
- [Supabase Android Guide](https://supabase.com/docs/guides/getting-started/quickstarts/android)
- [Material Design 3](https://m3.material.io/)

## 🤝 Contributing

When adding features:

1. Keep code simple (avoid over-engineering)
2. Update this guide
3. Add inline comments for complex logic
4. Test with various user profiles

---

**Built with ❤️ for NMIMS Fitness Enthusiasts**
