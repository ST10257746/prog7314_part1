package com.example.prog7314_part1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.repository.UserRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private lateinit var userRepository: UserRepository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        userRepository = UserRepository(requireContext())
        
        loadUserData(view)
        
        return view
    }

    private fun loadUserData(view: View) {
        lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Update greeting with user's name
                    view.findViewById<TextView>(R.id.userGreeting)?.text = user.displayName
                    
                    // For now, keep the hardcoded activity data (steps, calories, etc.)
                    // This would normally come from DailyActivity database
                    // But we can update the goal displays if needed
                    
                    // You could add more real-time data here as you implement
                    // the activity tracking features
                }
            }
        }
    }
}


