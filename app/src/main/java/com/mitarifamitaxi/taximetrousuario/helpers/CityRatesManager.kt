package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import com.google.gson.Gson
import androidx.core.content.edit
import com.mitarifamitaxi.taximetrousuario.models.Rates

class CityRatesManager(private val context: Context) {

    companion object {
        private const val RATES_PREFS = "RatesData"
        private const val RATES_OBJECT_KEY = "RATES_OBJECT"
    }

    fun getRatesState(): Rates? {
        val sharedPref = context.getSharedPreferences(RATES_PREFS, Context.MODE_PRIVATE)
        val ratesJson = sharedPref.getString(RATES_OBJECT_KEY, null)
        return ratesJson?.let {
            try {
                Gson().fromJson(it, Rates::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteRatesState() {
        val sharedPref = context.getSharedPreferences(RATES_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { remove(RATES_OBJECT_KEY) }
    }

    fun saveRatesState(rates: Rates) {
        val sharedPref = context.getSharedPreferences(RATES_PREFS, Context.MODE_PRIVATE)
        val ratesJson = Gson().toJson(rates)
        sharedPref.edit { putString(RATES_OBJECT_KEY, ratesJson) }
    }
}