package com.example.prog7314_part1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Set up circular progress rings with sample progress values
        setupCircularProgress(view)

        return view
    }

    private fun setupCircularProgress(view: View) {
        // Steps progress (86% of 10,000 steps = 8,600 steps)
        val stepsProgress = view.findViewById<View>(R.id.stepsProgressCircle)
        stepsProgress.background.level = 8600  // 86% progress

        // Calories progress (73% of 2,200 kcal = 1,606 kcal)
        val caloriesProgress = view.findViewById<View>(R.id.caloriesProgressCircle)
        caloriesProgress.background.level = 2450  // 73% progress

        // Heart points progress (90% of target)
        val heartProgress = view.findViewById<View>(R.id.heartProgressCircle)
        heartProgress.background.level = 9000  // 90% progress
    }
}



