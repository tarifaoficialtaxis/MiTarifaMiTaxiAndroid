package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import androidx.core.content.edit

class LocalUserManager(private val context: Context) {

    companion object {
        private const val USER_PREFS = "UserData"
        private const val USER_OBJECT_KEY = "USER_OBJECT"
    }

    fun getUserState(): LocalUser? {
        val sharedPref = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        val userJson = sharedPref.getString(USER_OBJECT_KEY, null)
        return userJson?.let {
            try {
                Gson().fromJson(it, LocalUser::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteUserState() {
        val sharedPref = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { remove(USER_OBJECT_KEY) }
    }

    fun saveUserState(user: LocalUser) {
        val sharedPref = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        val userJson = Gson().toJson(user)
        sharedPref.edit { putString(USER_OBJECT_KEY, userJson) }
    }
}