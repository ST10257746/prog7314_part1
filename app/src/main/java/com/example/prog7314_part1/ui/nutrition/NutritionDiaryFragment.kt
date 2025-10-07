package com.example.prog7314_part1.ui.nutrition

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.NutritionEntry
import com.example.prog7314_part1.data.repository.ApiUserRepository
import com.example.prog7314_part1.data.repository.DailyActivityRepository
import com.example.prog7314_part1.data.repository.NutritionRepository
import com.example.prog7314_part1.utils.showToast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NutritionDiaryFragment : Fragment() {

    private lateinit var viewModel: NutritionViewModel
    private lateinit var nutritionAdapter: NutritionEntryAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private var selectedDate = Date()

    // UI Elements
    private lateinit var dateButton: Button
    private lateinit var backButton: Button
    private lateinit var calendarView: CalendarView
    private lateinit var totalCaloriesText: TextView
    private lateinit var totalProteinText: TextView
    private lateinit var totalCarbsText: TextView
    private lateinit var totalFatsText: TextView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var noEntriesText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nutrition_diary, container, false)

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

        // Load initial data
        loadDataForDate()

        // Observe ViewModel state
        observeViewModel()

        return view
    }

    private fun initializeViews(view: View) {
        dateButton = view.findViewById(R.id.dateButton)
        backButton = view.findViewById(R.id.backButton)
        calendarView = view.findViewById(R.id.calendarView)
        totalCaloriesText = view.findViewById(R.id.totalCaloriesText)
        totalProteinText = view.findViewById(R.id.totalProteinText)
        totalCarbsText = view.findViewById(R.id.totalCarbsText)
        totalFatsText = view.findViewById(R.id.totalFatsText)
        entriesRecyclerView = view.findViewById(R.id.entriesRecyclerView)
        noEntriesText = view.findViewById(R.id.noEntriesText)

        // Set initial date
        dateButton.text = displayDateFormat.format(selectedDate)
    }

    private fun setupRecyclerView() {
        nutritionAdapter = NutritionEntryAdapter(
            onEntryClick = { entry ->
                requireContext().showToast("${entry.foodName} - ${entry.calories} kcal")
            },
            onDeleteClick = { entry ->
                showDeleteConfirmation(entry)
            }
        )
        entriesRecyclerView.apply {
            adapter = nutritionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            // Use Navigation Component to go back
            try {
                findNavController().navigateUp()
            } catch (e: Exception) {
                // Fallback to fragment manager if navigation fails
                parentFragmentManager.popBackStack()
            }
        }

        dateButton.setOnClickListener {
            showDatePicker()
        }

        // Calendar date selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            dateButton.text = displayDateFormat.format(selectedDate)
            loadDataForDate()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                dateButton.text = displayDateFormat.format(selectedDate)
                loadDataForDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadDataForDate() {
        val dateString = dateFormat.format(selectedDate)
        viewModel.loadDataForDate(dateString)
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
        val entries = state.selectedDateEntries

        // Update summary
        totalCaloriesText.text = "${summary.totalCalories} kcal"
        totalProteinText.text = "${summary.totalProtein.toInt()}g"
        totalCarbsText.text = "${summary.totalCarbs.toInt()}g"
        totalFatsText.text = "${summary.totalFats.toInt()}g"

        // Update entries list
        updateEntriesList(entries)
    }

    private fun updateEntriesList(entries: List<NutritionEntry>) {
        if (entries.isEmpty()) {
            noEntriesText.visibility = View.VISIBLE
            entriesRecyclerView.visibility = View.GONE
        } else {
            noEntriesText.visibility = View.GONE
            entriesRecyclerView.visibility = View.VISIBLE
            nutritionAdapter.submitList(entries)
        }
    }

    private fun showDeleteConfirmation(entry: NutritionEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Meal Entry")
            .setMessage("Are you sure you want to delete \"${entry.foodName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNutritionEntry(entry)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
