package com.example.prog7314_part1.ui.nutrition

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.entity.NutritionEntry

/**
 * Adapter for displaying nutrition entries
 */
class NutritionEntryAdapter(
    private val onEntryClick: (NutritionEntry) -> Unit = {},
    private val onDeleteClick: (NutritionEntry) -> Unit = {}
) : ListAdapter<NutritionEntry, NutritionEntryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nutrition_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mealTypeText: TextView = itemView.findViewById(R.id.mealTypeText)
        private val foodNameText: TextView = itemView.findViewById(R.id.foodNameText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val caloriesText: TextView = itemView.findViewById(R.id.caloriesText)
        private val proteinText: TextView = itemView.findViewById(R.id.proteinText)
        private val carbsText: TextView = itemView.findViewById(R.id.carbsText)
        private val fatsText: TextView = itemView.findViewById(R.id.fatsText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(entry: NutritionEntry) {
            mealTypeText.text = entry.mealType.name
            foodNameText.text = entry.foodName
            descriptionText.text = entry.description ?: entry.servingSize
            caloriesText.text = "${entry.calories} kcal"
            proteinText.text = "P: ${entry.proteinG.toInt()}g"
            carbsText.text = "C: ${entry.carbsG.toInt()}g"
            fatsText.text = "F: ${entry.fatsG.toInt()}g"
            timeText.text = entry.time

            itemView.setOnClickListener { onEntryClick(entry) }
            deleteButton.setOnClickListener { onDeleteClick(entry) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<NutritionEntry>() {
        override fun areItemsTheSame(oldItem: NutritionEntry, newItem: NutritionEntry): Boolean {
            return oldItem.entryId == newItem.entryId
        }

        override fun areContentsTheSame(oldItem: NutritionEntry, newItem: NutritionEntry): Boolean {
            return oldItem == newItem
        }
    }
}
