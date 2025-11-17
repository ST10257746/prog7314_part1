// UserSession.kt
package com.example.prog7314_part1.utils

import com.google.firebase.auth.FirebaseAuth

object UserSession {
    val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
}
