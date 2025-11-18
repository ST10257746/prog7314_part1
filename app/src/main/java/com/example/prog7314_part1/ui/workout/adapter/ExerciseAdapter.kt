package com.example.prog7314_part1.ui.workout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
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
            val context = binding.root.context
            binding.exerciseName.text = exercise.name
            binding.exerciseDetails.text = when {
                exercise.reps != null -> context.getString(R.string.sets_x_reps_format, exercise.sets ?: 1, exercise.reps)
                exercise.durationSeconds != null -> context.getString(R.string.sec_format, exercise.durationSeconds)
                else -> context.getString(R.string.no_details)
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
