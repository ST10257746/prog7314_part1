package com.example.prog7314_part1.ui.workout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.entity.Workout
import com.example.prog7314_part1.data.local.entity.WorkoutCategory
import com.example.prog7314_part1.data.local.entity.WorkoutDifficulty
import com.example.prog7314_part1.databinding.ItemWorkoutBinding
import com.example.prog7314_part1.utils.WorkoutLocalizationHelper

class WorkoutAdapter(
    private val onWorkoutClick: (Workout) -> Unit,
    private val onStartClick: (Workout) -> Unit = onWorkoutClick
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    private var selectedCategory: WorkoutCategory? = null

    /** Optionally highlight a category */
    fun setSelectedCategory(category: WorkoutCategory?) {
        selectedCategory = category
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding, onWorkoutClick, onStartClick)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position), selectedCategory)
    }

    class WorkoutViewHolder(
        private val binding: ItemWorkoutBinding,
        private val onWorkoutClick: (Workout) -> Unit,
        private val onStartClick: (Workout) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout, selectedCategory: WorkoutCategory?) {
            val context = binding.root.context
            binding.apply {
                // Use localized workout name, fallback to original if custom workout
                tvWorkoutName.text = if (workout.isCustom == true) {
                    workout.name // Custom workouts keep their original name
                } else {
                    WorkoutLocalizationHelper.getLocalizedWorkoutName(context, workout)
                }
                tvWorkoutMeta.text = buildMetaString(context, workout, selectedCategory)
                tvRating.text = context.getString(R.string.rating_format, workout.rating)
                tvCalories.text = context.getString(R.string.calories_format, workout.estimatedCalories)
                tvExerciseCount.text = "${getCategoryEmoji(workout.category)} ${workout.exerciseCount} ${context.getString(R.string.exercises)}"

                // Show CUSTOM badge if user-created
                if (workout.isCustom == true) {
                    tvCustomBadge.visibility = TextView.VISIBLE
                } else {
                    tvCustomBadge.visibility = TextView.GONE
                }

                root.setOnClickListener { onWorkoutClick(workout) }
                btnStart.setOnClickListener { onStartClick(workout) }
            }
        }

        private fun buildMetaString(context: android.content.Context, workout: Workout, selectedCategory: WorkoutCategory?): String {
            val duration = context.getString(R.string.min_format, workout.durationMinutes)
            val difficulty = getLocalizedDifficulty(context, workout.difficulty)
            val categoryDisplay = if (workout.category == selectedCategory) "★ ${formatCategoryName(context, workout.category)}"
            else formatCategoryName(context, workout.category)
            return "$duration • $difficulty • $categoryDisplay"
        }
        
        private fun getLocalizedDifficulty(context: android.content.Context, difficulty: WorkoutDifficulty): String {
            return when (difficulty) {
                WorkoutDifficulty.BEGINNER -> context.getString(R.string.difficulty_beginner)
                WorkoutDifficulty.INTERMEDIATE -> context.getString(R.string.difficulty_intermediate)
                WorkoutDifficulty.ADVANCED -> context.getString(R.string.difficulty_advanced)
            }
        }

        private fun formatCategoryName(context: android.content.Context, category: WorkoutCategory) = when (category) {
            WorkoutCategory.HIIT -> context.getString(R.string.hiit)
            WorkoutCategory.FULL_BODY -> context.getString(R.string.full_body)
            WorkoutCategory.UPPER_BODY -> context.getString(R.string.upper_body)
            WorkoutCategory.LOWER_BODY -> context.getString(R.string.lower_body)
            WorkoutCategory.CARDIO -> context.getString(R.string.cardio)
            WorkoutCategory.STRENGTH -> context.getString(R.string.strength)
            WorkoutCategory.YOGA -> context.getString(R.string.yoga)
            WorkoutCategory.FLEXIBILITY -> context.getString(R.string.flexibility)
            WorkoutCategory.CORE -> context.getString(R.string.core)
        }

        private fun getCategoryEmoji(category: WorkoutCategory) = when (category) {
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

    private class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
        override fun areItemsTheSame(oldItem: Workout, newItem: Workout) = oldItem.workoutId == newItem.workoutId
        override fun areContentsTheSame(oldItem: Workout, newItem: Workout) = oldItem == newItem
    }
}
