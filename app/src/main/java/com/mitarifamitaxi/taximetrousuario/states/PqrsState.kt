package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.EmailTemplate
import com.mitarifamitaxi.taximetrousuario.models.PqrsData
import com.mitarifamitaxi.taximetrousuario.models.PqrsReasons

data class PqrsState(
    val pqrsData: PqrsData = PqrsData(),
    val emailTemplate: EmailTemplate = EmailTemplate(),
    val plate: String = "",
    val plateIsError: Boolean = false,
    val plateErrorMessage: String = "",
    val reasonsSelected: List<PqrsReasons> = emptyList(),
    val otherValue: String = "",
    val isOtherValueError: Boolean = false,
    val otherValueErrorMessage: String = ""
)