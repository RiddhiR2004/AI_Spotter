# 🎯 Three Key Activities Explained

## Quick Summary

| Activity                     | Role           | When Used                   | Key Features                                    |
| ---------------------------- | -------------- | --------------------------- | ----------------------------------------------- |
| **MainActivity**             | Entry & Router | App launch                  | Splash screen, route to profile or home         |
| **HomeActivity**             | Dashboard      | After setup                 | Today's workout, progress, navigation hub       |
| **ProgressTrackingActivity** | Analytics      | User clicks "View Progress" | Completion stats, per-day breakdown, motivation |

---

## 1️⃣ MainActivity - The Entry Point

### Purpose

Splash screen and smart routing based on user state.

### What It Does

```
App Opens
    ↓
Show splash (2 seconds)
    ↓
Check: Has user completed profile?
    ↓
NO  → PersonalDetailsActivity (setup)
YES → HomeActivity (dashboard)
```

### Key Logic

```java
// In onCreate()
new Handler().postDelayed(() -> {
    if (hasProfile) {
        // Go to dashboard
        startActivity(new Intent(this, HomeActivity.class));
    } else {
        // Go to setup
        startActivity(new Intent(this, PersonalDetailsActivity.class));
    }
}, 2000);
```

### Data Storage

Uses **SharedPreferences** to remember:

- `hasProfile` - boolean (completed setup?)
- `userId` - String (user's unique ID)

### Helper Methods (Used by Other Activities)

```java
MainActivity.markProfileComplete(activity, userId)  // After profile saved
MainActivity.getUserId(activity)                    // Get current user ID
MainActivity.clearProfile(activity)                 // Clear for testing
```

---

## 2️⃣ HomeActivity - The Dashboard

### Purpose

Main hub showing today's workout and quick navigation.

### What It Displays

#### Welcome Section

```
Good Morning!  (or Afternoon/Evening based on time)
```

#### Today's Workout

```
🎯 Upper Body Strength

📋 6 exercises

• Push-ups
• Bench Press
• Shoulder Press
• and 3 more...

[Start Today's Workout]
```

#### Progress Overview

```
📊 Overall Progress: 45%
15 / 33 exercises completed
```

#### Navigation Buttons

```
[View Full Plan]  [View Progress]
```

### Smart Features

**Day Detection:**

```java
// Automatically finds today's workout
String today = new SimpleDateFormat("EEEE").format(new Date());
// Returns: "Monday", "Tuesday", etc.
```

**Rest Day Handling:**

```
🎉 Rest day! Your body needs recovery.
[View Full Plan]
```

**Auto-Refresh:**

```java
@Override
protected void onResume() {
    // Reload workout plan when user returns
    loadWorkoutPlan();
}
```

### Data Flow

```
1. Load workout plan from Supabase
2. Parse JSON → WorkoutPlan object
3. Find today's day (Monday, Tuesday, etc.)
4. Display today's exercises
5. Calculate overall progress
6. Navigate to workout or progress screen
```

---

## 3️⃣ ProgressTrackingActivity - The Analytics

### Purpose

Show detailed workout completion analytics with motivation.

### What It Displays

#### Overall Progress

```
🎯 Overall Progress

75% Complete

25 / 33 exercises completed
```

#### Weekly Breakdown

```
📅 Weekly Breakdown

✅ Monday: 6/6 (100%)
✅ Tuesday: 5/5 (100%)
🔸 Wednesday: 4/6 (67%)
⬜ Thursday: 0/5 (0%)
⬜ Friday: 0/6 (0%)
⬜ Saturday: 0/4 (0%)
✅ Sunday: 1/1 (100%)
```

**Legend:**

- ✅ = 100% complete
- 🔸 = Partially complete (1-99%)
- ⬜ = Not started (0%)

#### Statistics & Motivation

```
📊 Statistics

Days Completed: 3 / 7
Goal: Build Muscle
Plan Duration: 4 weeks

💪 Great work! Keep pushing!
```

### Motivational Messages

Progress-based encouragement:

```java
100%:   🎉 Amazing! You've completed your plan!
75-99%: 💪 Great work! Keep pushing!
50-74%: 🔥 You're halfway there! Don't give up!
25-49%: 👍 Good start! Stay consistent!
1-24%:  🌟 Every journey begins with a single step!
0%:     🚀 Ready to start? You got this!
```

### Data Flow

```
1. Load workout plan from Supabase
2. Parse JSON → WorkoutPlan object
3. Loop through all days and exercises
4. Count completed vs total per day
5. Calculate overall percentage
6. Display with visual indicators
7. Show motivational message
```

### Calculations

```java
// Per-day progress
for each day:
    dayCompleted = count exercises where isCompleted == true
    dayTotal = total exercises
    dayPercentage = (dayCompleted / dayTotal) * 100

// Overall progress
totalCompleted = sum of all completed exercises
totalExercises = sum of all exercises
overallPercentage = (totalCompleted / totalExercises) * 100
```

---

## 🔄 How They Work Together

### Scenario 1: First-Time User

```
1. MainActivity
   ↓ (No profile found)
2. PersonalDetailsActivity
   ↓ (Profile saved)
3. WorkoutPlanActivity
   ↓ (Workout generated)
4. HomeActivity (on back button)
   ↓ (Shows today's workout)
5. ProgressTrackingActivity (click "View Progress")
   ↓ (Shows 0% progress)
Back to HomeActivity
```

### Scenario 2: Returning User

```
1. MainActivity
   ↓ (Profile exists)
2. HomeActivity (directly!)
   ↓ (Shows today's workout + current progress)
3a. Click "Start Workout" → WorkoutPlanActivity
3b. Click "View Progress" → ProgressTrackingActivity
```

### Scenario 3: Completing Workout

```
1. HomeActivity
   ↓ (Click "Start Today's Workout")
2. WorkoutPlanActivity
   ↓ (Check off exercises)
   Each check saves to Supabase
   ↓ (Press back)
3. HomeActivity
   ↓ (onResume() refreshes data)
   Progress updated automatically!
   ↓ (Click "View Progress")
4. ProgressTrackingActivity
   Shows updated completion percentages
```

---

## 📊 Data Sharing Between Activities

All activities use the **same user ID**:

```java
String userId = MainActivity.getUserId(this);
```

All activities load from **same Supabase source**:

```java
SupabaseClient client = new SupabaseClient();
client.getWorkoutPlan(userId, callback);
```

All activities use **same parser**:

```java
WorkoutPlan plan = WorkoutParser.parseWorkoutPlan(userId, jsonResponse);
```

---

## 🎯 Design Patterns Used

### MainActivity - Router Pattern

```
Single entry point → Route based on state
```

### HomeActivity - Dashboard Pattern

```
Aggregates data from multiple sources
Provides quick navigation to features
```

### ProgressTrackingActivity - Observer Pattern

```
Observes workout completion data
Displays analytics and insights
```

---

## 🚀 Key Features Summary

### MainActivity

✅ **Splash screen** for branding  
✅ **Smart routing** (profile check)  
✅ **SharedPreferences** for persistence  
✅ **Helper methods** for other activities

### HomeActivity

✅ **Today's workout** auto-detection  
✅ **Progress overview** with percentages  
✅ **Time-based greetings** (Morning/Afternoon/Evening)  
✅ **Auto-refresh** on return (onResume)  
✅ **Quick navigation** to all features  
✅ **Rest day** detection

### ProgressTrackingActivity

✅ **Visual indicators** (✅, 🔸, ⬜)  
✅ **Per-day breakdown** with percentages  
✅ **Overall statistics** (days completed, goal)  
✅ **Motivational messages** based on progress  
✅ **Days completed** counter

---

## 💡 Pro Tips

### Testing Flow

```java
// Clear profile to test first-time user flow
MainActivity.clearProfile(this);
// Restart app - should go to PersonalDetailsActivity
```

### Debugging

```java
// Check what activity should show
SharedPreferences prefs = getSharedPreferences("AISpotterPrefs", MODE_PRIVATE);
boolean hasProfile = prefs.getBoolean("hasProfile", false);
Log.d("DEBUG", "Has profile: " + hasProfile);
```

### Navigation

- MainActivity → Sets up routing, then **finish()**
- PersonalDetailsActivity → After save, **finish()** (can't go back)
- HomeActivity → Main hub (back button exits app)
- WorkoutPlanActivity → Back returns to HomeActivity
- ProgressTrackingActivity → Back returns to HomeActivity

---

## 🎓 What Makes This Architecture Good?

### 1. Single Responsibility

Each activity has ONE clear purpose:

- MainActivity: Route users
- HomeActivity: Show dashboard
- ProgressTrackingActivity: Show analytics

### 2. Data Consistency

All activities:

- Use same user ID
- Load from same Supabase source
- Parse with same parser
- Use same data models

### 3. User Experience

- Smooth navigation flow
- No confusing back button behavior
- Data refreshes automatically
- Progress always accurate

### 4. Maintainability

- Easy to understand
- Clear data flow
- Consistent patterns
- Well-documented

---

**Now you have a complete, production-ready app with proper navigation, data flow, and user experience! 🎉**
