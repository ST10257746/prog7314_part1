package com.example.prog7314_part1.ui.nutrition

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.entity.MealType
import com.example.prog7314_part1.databinding.DialogAddNutritionEntryBinding
import com.example.prog7314_part1.utils.hideKeyboard
import com.example.prog7314_part1.utils.showToast

/**
 * Dialog for adding new nutrition entries
 */
class AddNutritionEntryDialog : DialogFragment() {
    
    private var _binding: DialogAddNutritionEntryBinding? = null
    private val binding get() = _binding!!
    
    private var onAddEntry: ((
        mealType: MealType,
        foodName: String,
        description: String?,
        servingSize: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double
    ) -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set dialog width to be more spacious
        dialog.setOnShowListener {
            val window = dialog.window
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(), // 90% of screen width
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddNutritionEntryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMealTypeSpinner()
        setupClickListeners()
    }
    
    private fun setupMealTypeSpinner() {
        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mealTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mealTypeSpinner.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.addButton.setOnClickListener {
            handleAddEntry()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun handleAddEntry() {
        requireContext().hideKeyboard(binding.root)
        
        // Get values
        val mealTypeIndex = binding.mealTypeSpinner.selectedItemPosition
        val mealType = when (mealTypeIndex) {
            0 -> MealType.BREAKFAST
            1 -> MealType.LUNCH
            2 -> MealType.DINNER
            3 -> MealType.SNACK
            else -> MealType.BREAKFAST
        }
        
        val foodName = binding.foodNameInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim().takeIf { it.isNotEmpty() }
        val servingSize = binding.servingSizeInput.text.toString().trim()
        val caloriesText = binding.caloriesInput.text.toString().trim()
        val proteinText = binding.proteinInput.text.toString().trim()
        val carbsText = binding.carbsInput.text.toString().trim()
        val fatsText = binding.fatsInput.text.toString().trim()
        
        // Validation
        if (foodName.isEmpty()) {
            requireContext().showToast("Please enter food name")
            return
        }
        
        if (servingSize.isEmpty()) {
            requireContext().showToast("Please enter serving size")
            return
        }
        
        val calories = caloriesText.toIntOrNull()
        if (calories == null || calories <= 0) {
            requireContext().showToast("Please enter valid calories")
            return
        }
        
        val protein = proteinText.toDoubleOrNull()
        if (protein == null || protein < 0) {
            requireContext().showToast("Please enter valid protein amount")
            return
        }
        
        val carbs = carbsText.toDoubleOrNull()
        if (carbs == null || carbs < 0) {
            requireContext().showToast("Please enter valid carbs amount")
            return
        }
        
        val fats = fatsText.toDoubleOrNull()
        if (fats == null || fats < 0) {
            requireContext().showToast("Please enter valid fats amount")
            return
        }
        
        // Add entry
        onAddEntry?.invoke(mealType, foodName, description, servingSize, calories, protein, carbs, fats)
        dismiss()
    }
    
    /**
     * Set callback for when entry is added
     */
    fun setOnAddEntryListener(listener: (
        mealType: MealType,
        foodName: String,
        description: String?,
        servingSize: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatsG: Double
    ) -> Unit) {
        onAddEntry = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): AddNutritionEntryDialog {
            return AddNutritionEntryDialog()
        }
    }
}
