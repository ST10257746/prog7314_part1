package com.example.prog7314_part1.ui.workout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.data.local.entity.Exercise
import com.example.prog7314_part1.databinding.ItemExerciseCreateBinding
import com.example.prog7314_part1.ui.workout.ExerciseCreateItem
import java.util.UUID

/**
 * Adapter for exercise creation in custom workout form
 */
class ExerciseCreateAdapter(
    private val exercises: MutableList<ExerciseCreateItem>,
    private val onRemoveExercise: (Int) -> Unit
) : RecyclerView.Adapter<ExerciseCreateAdapter.ExerciseCreateViewHolder>() {

    private val muscleGroups = listOf(
        "Upper Body", "Lower Body", "Core", "Full Body", 
        "Chest", "Back", "Shoulders", "Arms", "Legs", "Glutes", "Cardio"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseCreateViewHolder {
        val binding = ItemExerciseCreateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseCreateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseCreateViewHolder, position: Int) {
        viewHolders[position] = holder
        holder.bind(exercises[position], position)
    }

    override fun getItemCount(): Int = exercises.size

    private val viewHolders = mutableMapOf<Int, ExerciseCreateViewHolder>()

    /**
     * Get exercise data from a specific position for validation and saving
     */
    fun getExerciseData(position: Int): Exercise {
        val holder = viewHolders[position]
        return holder?.getExerciseData() ?: createDefaultExercise(position)
    }

    private fun createDefaultExercise(position: Int): Exercise {
        val item = exercises[position]
        return Exercise(
            exerciseId = item.id,
            workoutId = "", // Will be set when saving
            orderIndex = position,
            name = item.name,
            description = item.description,
            muscleGroup = item.muscleGroup,
            sets = item.sets,
            reps = item.reps,
            durationSeconds = item.durationSeconds,
            restSeconds = item.restSeconds
        )
    }

    inner class ExerciseCreateViewHolder(
        private val binding: ItemExerciseCreateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: ExerciseCreateItem, position: Int) {
            // Set up muscle group spinner
            val adapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_spinner_item,
                muscleGroups
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerMuscleGroup.adapter = adapter

            // Set current values
            binding.etExerciseName.setText(exercise.name)
            binding.etExerciseDescription.setText(exercise.description)
            binding.etSets.setText(exercise.sets?.toString() ?: "")
            binding.etReps.setText(exercise.reps?.toString() ?: "")
            binding.etDuration.setText(exercise.durationSeconds?.toString() ?: "")
            binding.etRest.setText(exercise.restSeconds.toString())

            // Set muscle group selection
            val muscleGroupIndex = muscleGroups.indexOf(exercise.muscleGroup)
            if (muscleGroupIndex >= 0) {
                binding.spinnerMuscleGroup.setSelection(muscleGroupIndex)
            }

            // Remove button click listener
            binding.btnRemoveExercise.setOnClickListener {
                onRemoveExercise(adapterPosition)
            }
        }

        fun getExerciseData(): Exercise {
            val name = binding.etExerciseName.text.toString().trim()
            val description = binding.etExerciseDescription.text.toString().trim()
            val muscleGroup = binding.spinnerMuscleGroup.selectedItem.toString()
            val sets = binding.etSets.text.toString().toIntOrNull()
            val reps = binding.etReps.text.toString().toIntOrNull()
            val duration = binding.etDuration.text.toString().toIntOrNull()
            val rest = binding.etRest.text.toString().toIntOrNull() ?: 60

            return Exercise(
                exerciseId = exercises[adapterPosition].id,
                workoutId = "", // Will be set when saving
                orderIndex = adapterPosition,
                name = name,
                description = description,
                muscleGroup = muscleGroup,
                sets = sets,
                reps = reps,
                durationSeconds = duration,
                restSeconds = rest
            )
        }
    }
}
