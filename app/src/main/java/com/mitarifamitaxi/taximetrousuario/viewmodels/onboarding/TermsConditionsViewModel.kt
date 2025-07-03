package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TermsConditionsViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    fun saveAcceptedTerms() {
        val sharedPref = appContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("accepted_terms", true)
            apply()
        }
    }
}

class TermsConditionsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TermsConditionsViewModel(context) as T
    }
}
