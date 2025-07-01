package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import com.google.gson.Gson
import androidx.core.content.edit
import com.mitarifamitaxi.taximetrousuario.models.Contact

class ContactsCatalogManager(private val context: Context) {

    companion object {
        private const val CONTACTS_PREFS = "ContactsData"
        private const val CONTACTS_OBJECT_KEY = "CONTACTS_OBJECT"
    }

    fun getContactsState(): Contact? {
        val sharedPref = context.getSharedPreferences(CONTACTS_PREFS, Context.MODE_PRIVATE)
        val contactJson = sharedPref.getString(CONTACTS_OBJECT_KEY, null)
        return contactJson?.let {
            try {
                Gson().fromJson(it, Contact::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteContactsState() {
        val sharedPref = context.getSharedPreferences(CONTACTS_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit { remove(CONTACTS_OBJECT_KEY) }
    }

    fun saveContactsState(contact: Contact) {
        val sharedPref = context.getSharedPreferences(CONTACTS_PREFS, Context.MODE_PRIVATE)
        val contactJson = Gson().toJson(contact)
        sharedPref.edit { putString(CONTACTS_OBJECT_KEY, contactJson) }
    }

}