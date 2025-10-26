package com.nmims.fitness.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nmims.fitness.R;
import com.nmims.fitness.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;
    private Markwon markwon;

    public ChatAdapter(List<ChatMessage> messages, Markwon markwon) {
        this.messages = messages;
        this.markwon = markwon;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout messageContainer;
        private TextView messageTextView;
        private TextView timeTextView;
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.message_container);
            messageTextView = itemView.findViewById(R.id.textView_message);
            timeTextView = itemView.findViewById(R.id.textView_time);
        }

        public void bind(ChatMessage message) {
            // Render markdown for AI messages, plain text for user messages
            if (message.isAI()) {
                markwon.setMarkdown(messageTextView, message.getMessage());
            } else {
                messageTextView.setText(message.getMessage());
            }
            
            timeTextView.setText(timeFormat.format(new Date(message.getTimestamp())));

            // Style based on message type
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            
            if (message.isAI()) {
                // AI message - left aligned
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.chat_bubble_ai);
            } else {
                // User message - right aligned
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.chat_bubble_user);
            }
            
            messageContainer.setLayoutParams(params);
        }
    }
}

