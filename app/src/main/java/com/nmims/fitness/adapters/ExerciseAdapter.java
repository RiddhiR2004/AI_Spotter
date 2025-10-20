package com.nmims.fitness.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nmims.fitness.R;
import com.nmims.fitness.models.Exercise;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    
    private List<Exercise> exercises;
    private OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise, int position);
        void onExerciseChecked(Exercise exercise, int position, boolean isChecked);
    }

    public ExerciseAdapter(List<Exercise> exercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.bind(exercise, position);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public void updateExercises(List<Exercise> newExercises) {
        this.exercises = newExercises;
        notifyDataSetChanged();
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView nameTextView;
        TextView detailsTextView;
        TextView musclesTextView;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.exercise_checkbox);
            nameTextView = itemView.findViewById(R.id.exercise_name);
            detailsTextView = itemView.findViewById(R.id.exercise_details);
            musclesTextView = itemView.findViewById(R.id.exercise_muscles);

            // Click on entire item to see details
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExerciseClick(exercises.get(position), position);
                }
            });

            // Checkbox for marking complete
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Exercise exercise = exercises.get(position);
                    exercise.setCompleted(isChecked);
                    listener.onExerciseChecked(exercise, position, isChecked);
                }
            });
        }

        public void bind(Exercise exercise, int position) {
            nameTextView.setText(exercise.getName());
            
            // Build details string
            StringBuilder details = new StringBuilder();
            if (exercise.getSets() > 0) {
                details.append(exercise.getSets()).append(" sets");
            }
            if (exercise.getReps() > 0) {
                if (details.length() > 0) details.append(" × ");
                details.append(exercise.getReps()).append(" reps");
            }
            if (exercise.getDuration() != null && !exercise.getDuration().equals("N/A")) {
                if (details.length() > 0) details.append(" • ");
                details.append(exercise.getDuration());
            }
            if (exercise.getRestPeriod() != null && !exercise.getRestPeriod().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append("Rest: ").append(exercise.getRestPeriod());
            }
            
            detailsTextView.setText(details.toString());
            musclesTextView.setText(exercise.getTargetMuscles());
            
            // Set checkbox state without triggering listener
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(exercise.isCompleted());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    exercise.setCompleted(isChecked);
                    listener.onExerciseChecked(exercise, pos, isChecked);
                }
            });
        }
    }
}

