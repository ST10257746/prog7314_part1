package com.example.prog7314_part1.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for common operations
 */

// ========== Context Extensions ==========

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.hideKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

// ========== View Extensions ==========

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.showSnackbarWithAction(
    message: String,
    actionText: String,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(this, message, duration)
        .setAction(actionText) { action() }
        .show()
}

// ========== Date Extensions ==========

fun Long.toDateString(pattern: String = "yyyy-MM-dd"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toTimeString(pattern: String = "HH:mm"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.toTimestamp(pattern: String = "yyyy-MM-dd"): Long {
    return try {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.parse(this)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun getCurrentDate(): String {
    return System.currentTimeMillis().toDateString()
}

fun getCurrentTime(): String {
    return System.currentTimeMillis().toTimeString()
}

// ========== String Extensions ==========

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

// ========== Number Extensions ==========

fun Int.toCalorieString(): String = "$this kcal"

fun Double.toDistanceString(): String = String.format("%.2f km", this)

fun Double.toWeightString(): String = String.format("%.1f kg", this)

fun Int.toStepsString(): String = "%,d steps".format(this)
