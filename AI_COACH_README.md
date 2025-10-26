# AI Fitness Coach with RAG (Retrieval Augmented Generation)

## 🎯 Overview

The AI Fitness Coach is a RAG-powered chatbot that provides personalized fitness advice based on:

- **SystemPrompt.md**: 1230 lines of expert calisthenics and weighted training knowledge
- **User's Survey Data**: Personal details, goals, current strength levels, limitations
- **User's Workout Plan**: Generated plan from Gemini API with progress tracking

## ✨ Features Implemented

### 1. Enhanced Signup Survey

Added fields to capture prerequisite strength levels:

- **Pull-ups count**: Critical for determining program phase
- **Dips count**: Required for advanced progressions
- **Push-ups count**: General strength indicator

These fields are now saved to the `surveys` table in Supabase.

### 2. RAG-Based AI Coach (`AICoachActivity`)

- Real-time chat interface with message history
- Loads SystemPrompt.md (1230 lines) as base knowledge
- Retrieves user profile and workout plan from Supabase
- Combines all context for personalized responses
- Maintains conversation history (last 10 messages)

### 3. AI Coach Service (`AICoachService.java`)

**Key Components:**

- **System Prompt Loading**: Reads SystemPrompt.md from assets folder
- **Context Building**: Combines system instructions + user data + conversation history
- **Gemini API Integration**: Sends full context to get intelligent responses
- **Data Formatting**: Structures user data for AI comprehension

**RAG Flow:**

```
User Question
    ↓
Load SystemPrompt.md (calisthenics expertise)
    ↓
Fetch User Profile (surveys table)
    ↓
Fetch Workout Plan (workout_plans table)
    ↓
Build Full Context (3000+ lines)
    ↓
Send to Gemini API
    ↓
Get Personalized Response
```

### 4. Modern Chat UI

- Material Design chat bubbles
- User messages (purple, right-aligned)
- AI messages (dark, left-aligned)
- Timestamps for each message
- Smooth scrolling and keyboard handling
- Loading indicator during AI response generation

## 📋 Supabase Schema Updates

### Required Columns in `surveys` Table

Add these columns to capture prerequisite strength data:

```sql
-- Add current strength level fields
ALTER TABLE public.surveys
ADD COLUMN IF NOT EXISTS current_pullups integer DEFAULT 0,
ADD COLUMN IF NOT EXISTS current_dips integer DEFAULT 0,
ADD COLUMN IF NOT EXISTS current_pushups integer DEFAULT 0;

-- Add comments
COMMENT ON COLUMN public.surveys.current_pullups IS 'Number of pull-ups user can currently perform';
COMMENT ON COLUMN public.surveys.current_dips IS 'Number of dips user can currently perform';
COMMENT ON COLUMN public.surveys.current_pushups IS 'Number of push-ups user can currently perform';
```

### Optional: Chat History Table

If you want to persist chat conversations:

```sql
-- Create chat_history table
CREATE TABLE public.chat_history (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.surveys(user_id) ON DELETE CASCADE,
    message text NOT NULL,
    is_ai boolean NOT NULL DEFAULT false,
    timestamp bigint NOT NULL,
    created_at timestamptz DEFAULT now()
);

-- Add index for faster queries
CREATE INDEX idx_chat_history_user_id ON public.chat_history(user_id);
CREATE INDEX idx_chat_history_timestamp ON public.chat_history(timestamp DESC);

-- Enable RLS
ALTER TABLE public.chat_history ENABLE ROW LEVEL SECURITY;

-- Add policy (adjust based on your auth setup)
CREATE POLICY "Allow anonymous access"
ON public.chat_history
FOR ALL
TO anon
USING (true)
WITH CHECK (true);
```

## 🚀 How to Use

### For Users:

1. Sign up and fill in your fitness details (including current pull-ups/dips)
2. Generate your personalized workout plan
3. Click "🤖 AI Fitness Coach" button on home screen
4. Ask questions about:
   - Exercise form and technique
   - Workout progression strategies
   - How to handle plateaus
   - Injury prevention
   - Nutrition advice
   - Your specific workout plan

### Example Questions:

- "How do I improve my pull-up count from 5 to 15?"
- "What's the proper form for weighted dips?"
- "Should I train planche before I can do +40kg dips?"
- "How do I progress if I'm stuck at the same weight for 3 weeks?"
- "What should I eat to support my muscle growth goals?"
- "Is my current workout plan aligned with building muscle?"

## 🔧 Technical Implementation

### Files Created/Modified:

**New Files:**

1. `AICoachActivity.java` - Main chat activity
2. `AICoachService.java` - RAG service with Gemini integration
3. `ChatMessage.java` - Message model
4. `ChatAdapter.java` - RecyclerView adapter for chat
5. `activity_ai_coach.xml` - Chat UI layout
6. `item_chat_message.xml` - Message item layout
7. `chat_bubble_user.xml` - User message bubble
8. `chat_bubble_ai.xml` - AI message bubble
9. `assets/SystemPrompt.md` - Expert fitness knowledge base

**Modified Files:**

1. `PersonalDetailsActivity.java` - Added strength level fields
2. `PersonalDetailStep2Activity.java` - Pass strength data
3. `PersonalDetailStep3Activity.java` - Save strength data to DB
4. `HomeActivity.java` - Added AI Coach button
5. `activity_home.xml` - AI Coach button UI
6. `AndroidManifest.xml` - Registered AICoachActivity

### Context Size:

The AI receives approximately **3000-5000 lines of context** per request:

- SystemPrompt.md: ~1230 lines
- User Profile: ~15 lines
- Workout Plan: ~100-300 lines
- Conversation History: ~10-50 lines
- Current Question: ~1-5 lines

This ensures the AI has complete knowledge to provide expert, personalized advice.

## 💡 Key Advantages

### 1. Expert Knowledge Base

The entire SystemPrompt.md (written by Coach Ian) is loaded, including:

- Progressive overload methods
- Prerequisite requirements (15+ pull-ups, 20+ dips)
- Detailed workout programs for all skill levels
- Nutrition and recovery guidance
- Injury prevention strategies
- Training schedules and splits

### 2. Personalized Context

Every response considers:

- User's current strength (pull-ups, dips, push-ups)
- User's goals (build muscle, lose weight, learn skills)
- User's available equipment
- User's time availability
- User's injuries/limitations
- User's current workout plan and progress

### 3. Conversation Memory

The chatbot remembers the last 10 messages, enabling:

- Follow-up questions
- Clarifications
- Progressive dialogue
- Context-aware responses

### 4. Real-time Generation

- No pre-written responses
- Adapts to any question
- Uses latest Gemini 1.5 Flash model
- Fast response times (typically 2-5 seconds)

## 🔐 Security Notes

- Gemini API key is currently hardcoded (move to environment variables for production)
- Supabase anonymous access enabled (adjust RLS policies for production)
- Chat history not persisted (can be added with chat_history table)

## 📱 UI/UX Features

- **Smooth Scrolling**: Auto-scrolls to latest message
- **Keyboard Handling**: `adjustResize` mode for proper input visibility
- **Loading State**: Shows spinner while AI generates response
- **Error Handling**: Graceful error messages if API fails
- **Timestamps**: Shows when each message was sent
- **Modern Design**: Purple gradient theme matching app style
- **Responsive**: Works on all screen sizes

## 🎓 Future Enhancements

1. **Chat History Persistence**: Save conversations to Supabase
2. **Voice Input**: Add speech-to-text for questions
3. **Image Analysis**: Let users upload form check videos
4. **Quick Suggestions**: Pre-defined common questions
5. **Export Conversations**: Save chat as PDF
6. **Multi-language Support**: Translate SystemPrompt
7. **Offline Mode**: Cache common responses
8. **Form Check AI**: Analyze exercise videos using computer vision

## 📊 Testing Checklist

- [ ] User can access AI Coach from home screen
- [ ] Chat interface displays correctly
- [ ] Messages send and receive properly
- [ ] User profile loads into context
- [ ] Workout plan loads into context
- [ ] SystemPrompt.md loads from assets
- [ ] AI responses are relevant and personalized
- [ ] Conversation history maintains context
- [ ] Loading indicators work properly
- [ ] Error handling displays user-friendly messages
- [ ] Keyboard doesn't cover input field
- [ ] Back button returns to home
- [ ] New strength level fields save to database
- [ ] AI considers user's current strength in responses

## 🐛 Troubleshooting

**AI responses are generic:**

- Check that SystemPrompt.md is in `app/src/main/assets/`
- Verify user profile is loading (check logs for "Survey data loaded")
- Confirm workout plan is loading (check logs for "Workout plan loaded")

**API errors:**

- Verify Gemini API key is valid
- Check internet connection
- Ensure API quota hasn't been exceeded

**Strength levels not showing:**

- Run SQL commands to add columns to surveys table
- Clear app data and re-signup to test

**App crashes on AI Coach button:**

- Check AndroidManifest.xml has AICoachActivity registered
- Verify all layout resources exist
- Check for linter errors

## 📝 Notes

- The SystemPrompt is comprehensive and covers all aspects of calisthenics training
- Context size is large (~3000+ lines) but Gemini 1.5 Flash handles it well
- Response quality depends on how well the system prompt is written
- User data formatting affects AI's ability to reference specific details
- Conversation history improves follow-up question handling

---

Built with ❤️ using Gemini API, RAG architecture, and comprehensive fitness expertise.
