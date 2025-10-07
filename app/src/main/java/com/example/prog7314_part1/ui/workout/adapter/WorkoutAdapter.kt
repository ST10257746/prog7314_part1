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
import com.example.prog7314_part1.databinding.ItemWorkoutBinding

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
            binding.apply {
                tvWorkoutName.text = workout.name
                tvWorkoutMeta.text = buildMetaString(workout, selectedCategory)
                tvRating.text = "⭐ ${String.format("%.1f", workout.rating)}"
                tvCalories.text = "🔥 ${workout.estimatedCalories} kcal"
                tvExerciseCount.text = "${getCategoryEmoji(workout.category)} ${workout.exerciseCount} exercises"

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

        private fun buildMetaString(workout: Workout, selectedCategory: WorkoutCategory?): String {
            val duration = "${workout.durationMinutes} min"
            val difficulty = workout.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
            val categoryDisplay = if (workout.category == selectedCategory) "★ ${formatCategoryName(workout.category)}"
            else formatCategoryName(workout.category)
            return "$duration • $difficulty • $categoryDisplay"
        }

        private fun formatCategoryName(category: WorkoutCategory) = when (category) {
            WorkoutCategory.HIIT -> "HIIT"
            WorkoutCategory.FULL_BODY -> "Full Body"
            WorkoutCategory.UPPER_BODY -> "Upper Body"
            WorkoutCategory.LOWER_BODY -> "Lower Body"
            WorkoutCategory.CARDIO -> "Cardio"
            WorkoutCategory.STRENGTH -> "Strength"
            WorkoutCategory.YOGA -> "Yoga"
            WorkoutCategory.FLEXIBILITY -> "Flexibility"
            WorkoutCategory.CORE -> "Core"
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
