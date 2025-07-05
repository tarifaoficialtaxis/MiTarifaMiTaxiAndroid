package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.ContactCatalog

data class SosState(
    val contactCatalogSelected: ContactCatalog? = null,
    val showContactDialog: Boolean = false,
    val contact: Contact = Contact()
)
