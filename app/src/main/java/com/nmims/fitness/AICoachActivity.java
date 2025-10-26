package com.nmims.fitness;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nmims.fitness.adapters.ChatAdapter;
import com.nmims.fitness.api.AICoachService;
import com.nmims.fitness.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

/**
 * AICoachActivity - AI-powered fitness coach with RAG
 * Uses SystemPrompt + user's workout plan + survey data for personalized coaching
 */
public class AICoachActivity extends AppCompatActivity {

    private static final String TAG = "AICoachActivity";

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ProgressBar loadingProgressBar;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private AICoachService aiCoachService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_coach);

        userId = MainActivity.getUserId(this);
        aiCoachService = new AICoachService(this);

        initViews();
        setupRecyclerView();
        
        // Add welcome message
        addWelcomeMessage();
        
        // Load user context in background
        loadUserContext();
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.editText_message);
        sendButton = findViewById(R.id.button_send);
        loadingProgressBar = findViewById(R.id.progress_loading);

        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        
        // Initialize Markwon for markdown rendering
        Markwon markwon = Markwon.create(this);
        
        chatAdapter = new ChatAdapter(messages, markwon);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
                "Hello! I'm your AI fitness coach. I have access to your workout plan and profile. " +
                "Ask me anything about:\n\n" +
                "• Exercise form and technique\n" +
                "• Workout progression\n" +
                "• Training schedules\n" +
                "• Nutrition advice\n" +
                "• Injury prevention\n" +
                "• Your specific workout plan\n\n" +
                "What can I help you with?",
                true,
                System.currentTimeMillis()
        );
        messages.add(welcomeMessage);
        chatAdapter.notifyItemInserted(messages.size() - 1);
    }

    private void loadUserContext() {
        aiCoachService.loadUserContext(userId, new AICoachService.ContextLoadCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "User context loaded successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load user context: " + error);
                Toast.makeText(AICoachActivity.this, 
                    "Warning: Could not load full context", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            return;
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(messageText, false, System.currentTimeMillis());
        messages.add(userMessage);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);

        // Clear input
        messageInput.setText("");
        
        // Show loading
        showLoading(true);

        // Get AI response
        aiCoachService.getCoachResponse(messageText, new AICoachService.CoachResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // Add AI response
                    ChatMessage aiMessage = new ChatMessage(response, true, System.currentTimeMillis());
                    messages.add(aiMessage);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(AICoachActivity.this, 
                        "Error: " + error, Toast.LENGTH_SHORT).show();
                    
                    // Add error message
                    ChatMessage errorMessage = new ChatMessage(
                        "Sorry, I encountered an error. Please try again.",
                        true,
                        System.currentTimeMillis()
                    );
                    messages.add(errorMessage);
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                });
            }
        });
    }

    private void showLoading(boolean show) {
        loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!show);
    }
}

