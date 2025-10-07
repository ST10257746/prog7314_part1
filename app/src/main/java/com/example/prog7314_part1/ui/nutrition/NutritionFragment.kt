package com.example.prog7314_part1.ui.nutrition

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.MealType
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.data.repository.DailyActivityRepository
import com.example.prog7314_part1.data.repository.NutritionRepository
import com.example.prog7314_part1.utils.showToast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

class NutritionFragment : Fragment() {

    private lateinit var viewModel: NutritionViewModel
    private lateinit var pieChart: PieChart
    private lateinit var nutritionAdapter: NutritionEntryAdapter

    // UI Elements
    private lateinit var caloriesText: TextView
    private lateinit var caloriesProgress: ProgressBar
    private lateinit var proteinText: TextView
    private lateinit var proteinProgress: ProgressBar
    private lateinit var carbsText: TextView
    private lateinit var carbsProgress: ProgressBar
    private lateinit var fatsText: TextView
    private lateinit var fatsProgress: ProgressBar
    private lateinit var waterText: TextView
    private lateinit var waterProgress: ProgressBar
    private lateinit var addWaterButton: Button
    private lateinit var removeWaterButton: Button
    private lateinit var logMealButton: Button
    private lateinit var viewDiaryButton: Button
    private lateinit var mealsRecyclerView: RecyclerView
    private lateinit var noMealsText: TextView

    // Center chart text
    private lateinit var totalCaloriesText: TextView
    
    // Pie chart legend
    private lateinit var proteinLegendLabel: TextView
    private lateinit var proteinLegendValue: TextView
    private lateinit var carbsLegendLabel: TextView
    private lateinit var carbsLegendValue: TextView
    private lateinit var fatsLegendLabel: TextView
    private lateinit var fatsLegendValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nutrition, container, false)

        // Initialize repositories and ViewModel
        val nutritionRepository = NutritionRepository(requireContext())
        val userRepository = ApiUserRepository(requireContext())
        val dailyActivityRepository = DailyActivityRepository(requireContext())
        val factory = NutritionViewModelFactory(nutritionRepository, userRepository, dailyActivityRepository)
        viewModel = ViewModelProvider(this, factory)[NutritionViewModel::class.java]

        // Initialize UI elements
        initializeViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup listeners
        setupClickListeners()

        // Setup pie chart
        setupPieChart()

        // Observe ViewModel state
        observeViewModel()

        return view
    }

    private fun initializeViews(view: View) {
        pieChart = view.findViewById(R.id.macroPieChart)

        // UI elements with IDs
        totalCaloriesText = view.findViewById(R.id.totalCaloriesText)
        caloriesText = view.findViewById(R.id.caloriesGoalText)
        caloriesProgress = view.findViewById(R.id.caloriesProgressBar)
        proteinText = view.findViewById(R.id.proteinCurrentText)
        proteinProgress = view.findViewById(R.id.proteinProgressBar)
        carbsText = view.findViewById(R.id.carbsCurrentText)
        carbsProgress = view.findViewById(R.id.carbsProgressBar)
        fatsText = view.findViewById(R.id.fatsCurrentText)
        fatsProgress = view.findViewById(R.id.fatsProgressBar)
        waterText = view.findViewById(R.id.waterGoalText)
        waterProgress = view.findViewById(R.id.waterProgressBar)

        // Buttons
        addWaterButton = view.findViewById(R.id.addWaterButton)
        removeWaterButton = view.findViewById(R.id.removeWaterButton)
        logMealButton = view.findViewById(R.id.logMealButton)
        viewDiaryButton = view.findViewById(R.id.viewDiaryButton)

        // RecyclerView and no meals text
        mealsRecyclerView = view.findViewById(R.id.mealsRecyclerView)
        noMealsText = view.findViewById(R.id.noMealsText)
        
        // Pie chart legend
        proteinLegendLabel = view.findViewById(R.id.proteinLegendLabel)
        proteinLegendValue = view.findViewById(R.id.proteinLegendValue)
        carbsLegendLabel = view.findViewById(R.id.carbsLegendLabel)
        carbsLegendValue = view.findViewById(R.id.carbsLegendValue)
        fatsLegendLabel = view.findViewById(R.id.fatsLegendLabel)
        fatsLegendValue = view.findViewById(R.id.fatsLegendValue)
    }

    private fun setupRecyclerView() {
        nutritionAdapter = NutritionEntryAdapter(
            onEntryClick = { entry ->
                // Handle single tap (could show details)
                requireContext().showToast("${entry.foodName} - ${entry.calories} kcal")
            },
            onDeleteClick = { entry ->
                // Handle delete button click
                showDeleteConfirmation(entry)
            }
        )
        mealsRecyclerView.apply {
            adapter = nutritionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }
    }



    private fun setupClickListeners() {
        addWaterButton.setOnClickListener {
            viewModel.addWaterGlass()
        }
        
        removeWaterButton.setOnClickListener {
            viewModel.removeWaterGlass()
        }

        logMealButton.setOnClickListener {
            showAddNutritionDialog()
        }

        viewDiaryButton.setOnClickListener {
            showNutritionDiary()
        }

        // Add long click listener to add sample data
        logMealButton.setOnLongClickListener {
            requireContext().showToast("Adding sample meals...")
            viewModel.addSampleEntries()
            true
        }
    }

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setDrawCenterText(false)
        pieChart.setDrawEntryLabels(false)
        pieChart.setHoleRadius(60f)
        pieChart.setTransparentCircleRadius(0f)
        pieChart.animateY(1000)

        // Set initial empty data
        updatePieChart(0.0, 0.0, 0.0)
    }

    private fun updatePieChart(protein: Double, carbs: Double, fats: Double) {
        val entries = ArrayList<PieEntry>()

        // Calculate calories from macros (protein: 4 kcal/g, carbs: 4 kcal/g, fats: 9 kcal/g)
        val proteinCals = (protein * 4).toFloat()
        val carbsCals = (carbs * 4).toFloat()
        val fatsCals = (fats * 9).toFloat()

        if (proteinCals > 0 || carbsCals > 0 || fatsCals > 0) {
            if (proteinCals > 0) entries.add(PieEntry(proteinCals, "Protein"))
            if (carbsCals > 0) entries.add(PieEntry(carbsCals, "Carbs"))
            if (fatsCals > 0) entries.add(PieEntry(fatsCals, "Fats"))
        } else {
            // Show placeholder data when no nutrition data
            entries.add(PieEntry(33f, "Protein"))
            entries.add(PieEntry(34f, "Carbs"))
            entries.add(PieEntry(33f, "Fats"))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#70E000"), // Secondary - Protein
            Color.parseColor("#3A86FF"), // Primary - Carbs
            Color.parseColor("#FF5C5C")  // Accent - Fats
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                updateUI(state)

                // Handle messages
                state.errorMessage?.let { message ->
                    requireContext().showToast(message)
                    viewModel.clearErrorMessage()
                }

                state.successMessage?.let { message ->
                    requireContext().showToast(message)
                    viewModel.clearSuccessMessage()
                }
            }
        }
    }

    private fun updateUI(state: NutritionState) {
        val summary = state.nutritionSummary
        val user = state.user

        // Calculate macro calories (protein×4, carbs×4, fats×9)
        val proteinKcal = (summary.totalProtein * 4).toInt()
        val carbsKcal = (summary.totalCarbs * 4).toInt()
        val fatsKcal = (summary.totalFats * 9).toInt()
        val totalMacroKcal = proteinKcal + carbsKcal + fatsKcal
        
        // Update center text of pie chart
        totalCaloriesText.text = summary.totalCalories.toString()

        // Update pie chart with actual data
        updatePieChart(summary.totalProtein, summary.totalCarbs, summary.totalFats)
        
        // Update pie chart legend with calculated values
        if (totalMacroKcal > 0) {
            val proteinPercent = ((proteinKcal.toFloat() / totalMacroKcal) * 100).toInt()
            val carbsPercent = ((carbsKcal.toFloat() / totalMacroKcal) * 100).toInt()
            val fatsPercent = ((fatsKcal.toFloat() / totalMacroKcal) * 100).toInt()
            
            proteinLegendLabel.text = "Protein ($proteinPercent%)"
            proteinLegendValue.text = "$proteinKcal kcal"
            
            carbsLegendLabel.text = "Carbohydrates ($carbsPercent%)"
            carbsLegendValue.text = "$carbsKcal kcal"
            
            fatsLegendLabel.text = "Fats ($fatsPercent%)"
            fatsLegendValue.text = "$fatsKcal kcal"
        } else {
            // Default values when no data
            proteinLegendLabel.text = "Protein (0%)"
            proteinLegendValue.text = "0 kcal"
            
            carbsLegendLabel.text = "Carbohydrates (0%)"
            carbsLegendValue.text = "0 kcal"
            
            fatsLegendLabel.text = "Fats (0%)"
            fatsLegendValue.text = "0 kcal"
        }

        // Update macro displays
        proteinText.text = "${summary.totalProtein.toInt()}g"
        carbsText.text = "${summary.totalCarbs.toInt()}g"
        fatsText.text = "${summary.totalFats.toInt()}g"
        
        // Update macro progress bars
        user?.let {
            // Protein progress
            val proteinGoal = it.proteinGoalG ?: 150
            val proteinPercentage = if (proteinGoal > 0) {
                ((summary.totalProtein / proteinGoal) * 100).toInt().coerceAtMost(100)
            } else 0
            proteinProgress.progress = proteinPercentage
            
            // Carbs progress
            val carbsGoal = it.carbsGoalG ?: 250
            val carbsPercentage = if (carbsGoal > 0) {
                ((summary.totalCarbs / carbsGoal) * 100).toInt().coerceAtMost(100)
            } else 0
            carbsProgress.progress = carbsPercentage
            
            // Fats progress
            val fatsGoal = it.fatsGoalG ?: 70
            val fatsPercentage = if (fatsGoal > 0) {
                ((summary.totalFats / fatsGoal) * 100).toInt().coerceAtMost(100)
            } else 0
            fatsProgress.progress = fatsPercentage
        }

        // Update calorie display
        user?.dailyCalorieGoal?.let { goal ->
            caloriesText.text = "${summary.totalCalories} of $goal kcal"
            val percentage = if (goal > 0) (summary.totalCalories * 100 / goal).coerceAtMost(100) else 0
            caloriesProgress.progress = percentage

            // Update percentage text
            view?.findViewById<TextView>(R.id.caloriesPercentageText)?.text = "$percentage%"
        }

        // Update water display
        user?.dailyWaterGoal?.let { goal ->
            waterText.text = "${state.waterGlasses} of $goal glasses"
            val percentage = if (goal > 0) (state.waterGlasses * 100 / goal).coerceAtMost(100) else 0
            waterProgress.progress = percentage

            // Update water percentage text
            view?.findViewById<TextView>(R.id.waterPercentageText)?.text = "$percentage%"
        }

        // Update meals list
        updateMealsList(state.todayEntries)
    }

    private fun updateMealsList(entries: List<com.example.prog7314_part1.data.local.entity.NutritionEntry>) {
        if (entries.isEmpty()) {
            noMealsText.visibility = View.VISIBLE
            mealsRecyclerView.visibility = View.GONE
        } else {
            noMealsText.visibility = View.GONE
            mealsRecyclerView.visibility = View.VISIBLE
            nutritionAdapter.submitList(entries)
        }
    }



    private fun showAddNutritionDialog() {
        val dialog = AddNutritionEntryDialog.newInstance()
        dialog.setOnAddEntryListener { mealType, foodName, description, servingSize, calories, protein, carbs, fats ->
            viewModel.addNutritionEntry(mealType, foodName, description, servingSize, calories, protein, carbs, fats)
        }
        dialog.show(parentFragmentManager, "AddNutritionEntry")
    }

    private fun showDeleteConfirmation(entry: com.example.prog7314_part1.data.local.entity.NutritionEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Meal Entry")
            .setMessage("Are you sure you want to delete \"${entry.foodName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNutritionEntry(entry)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNutritionDiary() {
        try {
            // Use proper navigation to avoid crashes
            findNavController().navigate(R.id.action_nutrition_to_diary)
        } catch (e: Exception) {
            requireContext().showToast("Unable to open nutrition diary. Please try again.")
        }
    }
}

