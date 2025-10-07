package com.example.prog7314_part1.ui.workout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import com.example.prog7314_part1.databinding.ItemWorkoutBinding

/**
 * Adapter for displaying workouts in RecyclerView
 */
class WorkoutAdapter(
    private val onWorkoutClick: (Workout) -> Unit,
    private val onStartClick: (Workout) -> Unit = onWorkoutClick
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkoutViewHolder(binding, onWorkoutClick, onStartClick)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WorkoutViewHolder(
        private val binding: ItemWorkoutBinding,
        private val onWorkoutClick: (Workout) -> Unit,
        private val onStartClick: (Workout) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            binding.apply {
                tvWorkoutName.text = workout.name
                tvWorkoutMeta.text = buildMetaString(workout)
                tvRating.text = "⭐ ${String.format("%.1f", workout.rating)}"
                tvCalories.text = "🔥 ${workout.estimatedCalories} kcal"
                tvExerciseCount.text = "${getCategoryEmoji(workout.category)} ${workout.exerciseCount} exercises"
                
                root.setOnClickListener {
                    onWorkoutClick(workout)
                }
                
                btnStart.setOnClickListener {
                    onStartClick(workout)
                }
            }
        }

        private fun buildMetaString(workout: Workout): String {
            val duration = "${workout.durationMinutes} min"
            val difficulty = workout.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val category = formatCategoryName(workout.category)
            return "$duration • $difficulty • $category"
        }

        private fun formatCategoryName(category: WorkoutCategory): String {
            return when (category) {
                WorkoutCategory.HIIT -> "HIIT"
                WorkoutCategory.FULL_BODY -> "Full Body"
                WorkoutCategory.UPPER_BODY -> "Upper Body"
                WorkoutCategory.LOWER_BODY -> "Lower Body"
                else -> category.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        private fun getCategoryEmoji(category: WorkoutCategory): String {
            return when (category) {
                WorkoutCategory.CARDIO -> "🏃‍♀️"
                WorkoutCategory.STRENGTH -> "💪"
                WorkoutCategory.YOGA -> "🧘‍♀️"
                WorkoutCategory.HIIT -> "🔥"
                WorkoutCategory.FLEXIBILITY -> "🤸‍♀️"
                WorkoutCategory.FULL_BODY -> "💪"
                WorkoutCategory.UPPER_BODY -> "💪"
                WorkoutCategory.LOWER_BODY -> "🦵"
                WorkoutCategory.CORE -> "🎯"
            }
        }
    }

    private class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            return oldItem.workoutId == newItem.workoutId
        }

        override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
            return oldItem == newItem
        }
    }
}
