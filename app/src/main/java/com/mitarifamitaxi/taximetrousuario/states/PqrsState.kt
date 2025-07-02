package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.EmailTemplate

data class PqrsState(
    val contact: Contact = Contact(),
    val emailTemplate: EmailTemplate = EmailTemplate(),
    val plate: String = "",
    val plateIsError: Boolean = false,
    val plateErrorMessage: String = "",
    val isHighFare: Boolean = false,
    val isUserMistreated: Boolean = false,
    val isServiceAbandonment: Boolean = false,
    val isUnauthorizedCharges: Boolean = false,
    val isNoFareNotice: Boolean = false,
    val isDangerousDriving: Boolean = false,
    val isOther: Boolean = false,
    val otherValue: String = "",
    val isOtherValueError: Boolean = false,
    val otherValueErrorMessage: String = ""
)