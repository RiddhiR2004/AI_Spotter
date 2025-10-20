package com.nmims.fitness.models;

import java.util.ArrayList;
import java.util.List;

public class WorkoutPlan {
    private String userId;
    private String planName;
    private String goal; // e.g., "Build Muscle", "Lose Weight"
    private int durationWeeks;
    private List<WorkoutDay> workoutDays;
    private String overallNotes; // General advice from Gemini

    public WorkoutPlan() {
        this.workoutDays = new ArrayList<>();
    }

    public WorkoutPlan(String userId, String planName, String goal, int durationWeeks, 
                      List<WorkoutDay> workoutDays, String overallNotes) {
        this.userId = userId;
        this.planName = planName;
        this.goal = goal;
        this.durationWeeks = durationWeeks;
        this.workoutDays = workoutDays != null ? workoutDays : new ArrayList<>();
        this.overallNotes = overallNotes;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public List<WorkoutDay> getWorkoutDays() { return workoutDays; }
    public void setWorkoutDays(List<WorkoutDay> workoutDays) { this.workoutDays = workoutDays; }

    public String getOverallNotes() { return overallNotes; }
    public void setOverallNotes(String overallNotes) { this.overallNotes = overallNotes; }

    public void addWorkoutDay(WorkoutDay day) {
        this.workoutDays.add(day);
    }
}

