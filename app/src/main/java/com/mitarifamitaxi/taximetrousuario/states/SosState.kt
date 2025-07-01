package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.ItemImageButton

data class SosState(
    val itemSelected: ItemImageButton? = null,
    val showContactDialog: Boolean = false,
    val contact: Contact = Contact()
)
