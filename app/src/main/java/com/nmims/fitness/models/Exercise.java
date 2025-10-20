package com.nmims.fitness.models;

public class Exercise {
    private String name;
    private int sets;
    private int reps;
    private String duration; // e.g., "30 seconds" for planks
    private String restPeriod; // e.g., "60 seconds"
    private String instructions;
    private String targetMuscles;
    private boolean completed;

    public Exercise() {
        this.completed = false;
    }

    public Exercise(String name, int sets, int reps, String duration, String restPeriod, 
                   String instructions, String targetMuscles) {
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.duration = duration;
        this.restPeriod = restPeriod;
        this.instructions = instructions;
        this.targetMuscles = targetMuscles;
        this.completed = false;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getRestPeriod() { return restPeriod; }
    public void setRestPeriod(String restPeriod) { this.restPeriod = restPeriod; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getTargetMuscles() { return targetMuscles; }
    public void setTargetMuscles(String targetMuscles) { this.targetMuscles = targetMuscles; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}

