package com.nmims.fitness.models;

public class ChatMessage {
    private String message;
    private boolean isAI;
    private long timestamp;

    public ChatMessage(String message, boolean isAI, long timestamp) {
        this.message = message;
        this.isAI = isAI;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean AI) {
        isAI = AI;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

