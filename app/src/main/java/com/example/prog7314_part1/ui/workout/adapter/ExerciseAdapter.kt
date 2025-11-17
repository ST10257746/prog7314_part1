package com.example.prog7314_part1.ui.workout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.databinding.ItemExerciseBinding

class ExerciseAdapter : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    private var exercises: List<Exercise> = emptyList()
    private val checkedItems = mutableSetOf<String>()

    fun submitList(list: List<Exercise>) {
        exercises = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount(): Int = exercises.size

    inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.exerciseName.text = exercise.name
            binding.exerciseDetails.text = when {
                exercise.reps != null -> "${exercise.sets ?: 1} sets × ${exercise.reps} reps"
                exercise.durationSeconds != null -> "${exercise.durationSeconds} sec"
                else -> "No details"
            }

            binding.exerciseCheckbox.setOnCheckedChangeListener(null)
            binding.exerciseCheckbox.isChecked = checkedItems.contains(exercise.exerciseId)

            binding.exerciseCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checkedItems.add(exercise.exerciseId)
                else checkedItems.remove(exercise.exerciseId)
            }
        }
    }
}
