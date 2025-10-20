package com.nmims.fitness.models;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDay {
    private String day; // e.g., "Monday", "Day 1"
    private String focus; // e.g., "Upper Body Strength"
    private List<Exercise> exercises;
    private String notes; // Additional notes from Gemini

    public WorkoutDay() {
        this.exercises = new ArrayList<>();
    }

    public WorkoutDay(String day, String focus, List<Exercise> exercises, String notes) {
        this.day = day;
        this.focus = focus;
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        this.notes = notes;
    }

    // Getters and Setters
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getFocus() { return focus; }
    public void setFocus(String focus) { this.focus = focus; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void addExercise(Exercise exercise) {
        this.exercises.add(exercise);
    }
}

