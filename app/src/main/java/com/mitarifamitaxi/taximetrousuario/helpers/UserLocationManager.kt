package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import com.google.gson.Gson
import androidx.core.content.edit
import com.mitarifamitaxi.taximetrousuario.models.UserLocation

class UserLocationManager(private val context: Context) {

    companion object {
        private const val USER_LOCATION_PREFS = "UserLocationData"
        private const val USER_LOCATION_OBJECT_KEY = "USER_LOCATION_OBJECT"
    }

    fun getUserLocationState(): UserLocation? {
        val sharedPref = context.getSharedPreferences(USER_LOCATION_PREFS, Context.MODE_PRIVATE)
        val userLocationJson = sharedPref.getString(USER_LOCATION_OBJECT_KEY, null)
        return userLocationJson?.let {
            try {
                Gson().fromJson(it, UserLocation::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteUserLocationState() {
        val sharedPref = context.getSharedPreferences(USER_LOCATION_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { remove(USER_LOCATION_OBJECT_KEY) }
    }

    fun saveUserLocationState(location: UserLocation) {
        val sharedPref = context.getSharedPreferences(USER_LOCATION_PREFS, Context.MODE_PRIVATE)
        val userLocationJson = Gson().toJson(location)
        sharedPref.edit { putString(USER_LOCATION_OBJECT_KEY, userLocationJson) }
    }
}