package com.mitarifamitaxi.taximetrousuario.states

import android.net.Uri

data class CompleteProfileState(
    val userId: String = "",
    val imageUri: Uri? = null,
    val tempImageUri: Uri? = null,
    val firstName: String = "",
    val firstNameIsError: Boolean = false,
    val firstNameErrorMessage: String = "",
    val lastName: String = "",
    val lastNameIsError: Boolean = false,
    val lastNameErrorMessage: String = "",
    val mobilePhone: String = "",
    val mobilePhoneIsError: Boolean = false,
    val mobilePhoneErrorMessage: String = "",
    val email: String = "",
    val showDialogSelectPhoto: Boolean = false,
    val hasCameraPermission: Boolean = false
)