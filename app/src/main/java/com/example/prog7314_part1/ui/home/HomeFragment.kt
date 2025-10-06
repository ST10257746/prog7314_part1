package com.example.prog7314_part1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.prog7314_part1.R
import com.example.prog7314_part1.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
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
        
        setupProfileNavigation(view)
        loadUserData(view)
        
        return view
    }

    private fun setupProfileNavigation(view: View) {
        view.findViewById<View>(R.id.profileIconContainer)?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun loadUserData(view: View) {
        lifecycleScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    // Update greeting with user's name
                    view.findViewById<TextView>(R.id.userGreeting)?.text = user.displayName
                    
                    // Load profile image
                    loadProfileImage(view, user.profileImageUrl)
                    
                    // Show badge if profile is incomplete
                    updateProfileBadge(view, user)
                    
                    // For now, keep the hardcoded activity data (steps, calories, etc.)
                    // This would normally come from DailyActivity database
                    // But we can update the goal displays if needed
                    
                    // You could add more real-time data here as you implement
                    // the activity tracking features
                }
            }
        }
    }
    
    private fun loadProfileImage(view: View, photoUrl: String?) {
        val profileIcon = view.findViewById<ImageView>(R.id.profileIcon) ?: return
        
        // Try to load from photoUrl first (Google Sign-In profile picture)
        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(photoUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.icon_default_profile)
                .error(R.drawable.icon_default_profile)
                .into(profileIcon)
        } else {
            // Try to load from Firebase Auth (Google profile picture)
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val googlePhotoUrl = firebaseUser?.photoUrl
            
            if (googlePhotoUrl != null) {
                Glide.with(this)
                    .load(googlePhotoUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.icon_default_profile)
                    .error(R.drawable.icon_default_profile)
                    .into(profileIcon)
            } else {
                // Use default profile icon
                Glide.with(this)
                    .load(R.drawable.icon_default_profile)
                    .transform(CircleCrop())
                    .into(profileIcon)
            }
        }
    }
    
    private fun updateProfileBadge(view: View, user: com.example.prog7314_part1.data.local.entity.User) {
        val badge = view.findViewById<View>(R.id.profileBadge) ?: return
        
        // Show badge if profile is incomplete (age or weight not set)
        // Or if there are other notifications (you can add more conditions)
        val isProfileIncomplete = user.age == 0 || user.weightKg == 0.0
        
        badge.visibility = if (isProfileIncomplete) View.VISIBLE else View.GONE
    }
}


