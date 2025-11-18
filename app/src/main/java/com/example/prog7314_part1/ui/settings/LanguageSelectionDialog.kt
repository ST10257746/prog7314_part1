package com.example.prog7314_part1.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.prog7314_part1.R
import com.example.prog7314_part1.utils.LocaleHelper

/**
 * Language Selection Dialog
 * Allows users to select their preferred language
 */
class LanguageSelectionDialog : DialogFragment() {
    
    interface LanguageSelectionListener {
        fun onLanguageSelected(language: LocaleHelper.Language)
    }
    
    private var listener: LanguageSelectionListener? = null
    
    fun setLanguageSelectionListener(listener: LanguageSelectionListener) {
        this.listener = listener
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languages = LocaleHelper.Language.values()
        
        // Get language names using the current app locale
        val currentLocale = LocaleHelper.getSavedLanguage(requireContext())
        val localizedContext = LocaleHelper.getLocalizedContext(requireContext(), currentLocale)
        
        val languageNames = languages.map { language ->
            when (language) {
                LocaleHelper.Language.ENGLISH -> localizedContext.getString(R.string.english)
                LocaleHelper.Language.ZULU -> localizedContext.getString(R.string.zulu)
                LocaleHelper.Language.AFRIKAANS -> localizedContext.getString(R.string.afrikaans)
            }
        }.toTypedArray()
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_single_choice,
            languageNames
        )
        
        val currentLanguage = LocaleHelper.getSavedLanguage(requireContext())
        val selectedIndex = languages.indexOf(currentLanguage)
        
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(adapter, selectedIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                LocaleHelper.saveLanguage(requireContext(), selectedLanguage)
                listener?.onLanguageSelected(selectedLanguage)
                dialog.dismiss()
                
                // Restart activity to apply language change across the whole app
                activity?.recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}

