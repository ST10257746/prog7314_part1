package com.example.prog7314_part1

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class NutritionFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nutrition, container, false)

        // Setup pie chart
        setupPieChart(view)

        return view
    }

    private fun setupPieChart(view: View) {
        val pieChart = view.findViewById<PieChart>(R.id.macroPieChart) ?: return

        // Sample data - Protein: 30%, Carbs: 45%, Fats: 25%
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(30f, "Protein"))
        entries.add(PieEntry(45f, "Carbs"))
        entries.add(PieEntry(25f, "Fats"))

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

        // Customize chart appearance
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setDrawCenterText(false)
        pieChart.setDrawEntryLabels(false)
        pieChart.setHoleRadius(60f)
        pieChart.setTransparentCircleRadius(0f)
        pieChart.animateY(1000)

        pieChart.invalidate()
    }
}


