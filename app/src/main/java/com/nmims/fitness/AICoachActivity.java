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
    private ImageButton clearHistoryButton;
    private ProgressBar loadingProgressBar;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private AICoachService aiCoachService;
    private String userId;
    private Markwon markwon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_coach);

        userId = MainActivity.getUserId(this);
        aiCoachService = new AICoachService(this);

        // Initialize Markwon for markdown rendering
        markwon = Markwon.create(this);

        initViews();
        setupRecyclerView();
        
        // Load conversation history from cache
        loadConversationHistory();
        
        // If no history, add welcome message
        if (messages.isEmpty()) {
            addWelcomeMessage();
        }
        
        // Load user context in background
        loadUserContext();
        
        // Check for pending messages
        checkPendingMessages();
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.editText_message);
        sendButton = findViewById(R.id.button_send);
        clearHistoryButton = findViewById(R.id.button_clear_history);
        loadingProgressBar = findViewById(R.id.progress_loading);

        findViewById(R.id.imageView_back).setOnClickListener(v -> finish());
        sendButton.setOnClickListener(v -> sendMessage());
        clearHistoryButton.setOnClickListener(v -> clearChatHistory());
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
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
                runOnUiThread(() -> {
                    Toast.makeText(AICoachActivity.this, 
                        "Warning: Could not load full context", Toast.LENGTH_SHORT).show();
                });
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

    /**
     * Load conversation history from AICoachService cache
     */
    private void loadConversationHistory() {
        List<com.nmims.fitness.api.AICoachService.ChatMessage> history = 
            aiCoachService.getConversationHistory();
        
        for (com.nmims.fitness.api.AICoachService.ChatMessage msg : history) {
            boolean isAI = msg.role.equals("assistant");
            ChatMessage chatMessage = new ChatMessage(msg.content, isAI, System.currentTimeMillis());
            messages.add(chatMessage);
        }
        
        if (!messages.isEmpty()) {
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            Log.d(TAG, "Loaded " + messages.size() + " messages from history");
        }
    }

    /**
     * Clear chat history
     */
    private void clearChatHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Chat History")
            .setMessage("Are you sure you want to clear all chat history? This cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                // Clear from service
                aiCoachService.clearConversationHistory();
                
                // Clear UI
                messages.clear();
                chatAdapter.notifyDataSetChanged();
                
                // Add welcome message back
                addWelcomeMessage();
                
                Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Check for pending messages that are still being processed
     */
    private void checkPendingMessages() {
        if (aiCoachService.hasPendingMessages()) {
            int pendingCount = aiCoachService.getPendingMessagesCount();
            Toast.makeText(this, 
                pendingCount + " message(s) still being processed in background", 
                Toast.LENGTH_LONG).show();
            showLoading(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload conversation history when returning to activity
        // This handles cases where responses completed in background
        refreshConversationHistory();
    }

    /**
     * Refresh conversation history from service
     */
    private void refreshConversationHistory() {
        List<com.nmims.fitness.api.AICoachService.ChatMessage> history = 
            aiCoachService.getConversationHistory();
        
        // Only update if there are new messages
        if (history.size() > messages.size()) {
            // Clear and reload all messages
            messages.clear();
            for (com.nmims.fitness.api.AICoachService.ChatMessage msg : history) {
                boolean isAI = msg.role.equals("assistant");
                ChatMessage chatMessage = new ChatMessage(msg.content, isAI, System.currentTimeMillis());
                messages.add(chatMessage);
            }
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            Log.d(TAG, "Refreshed conversation history with " + messages.size() + " messages");
        }
        
        // Hide loading if no more pending messages
        if (!aiCoachService.hasPendingMessages()) {
            showLoading(false);
        }
    }
}

