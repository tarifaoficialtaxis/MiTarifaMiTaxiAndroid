package com.mitarifamitaxi.taximetrousuario.states

import android.net.Uri

data class ProfileState(
    val originalProfilePictureUrl: String = "",
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
    val familyNumber: String? = null,
    val familyNumberIsError: Boolean = false,
    val familyNumberErrorMessage: String = "",
    val supportNumber: String? = null,
    val supportNumberIsError: Boolean = false,
    val supportNumberErrorMessage: String = "",
    val email: String = "",
    val emailIsError: Boolean = false,
    val emailErrorMessage: String = "",
    val showDialogSelectPhoto: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val showPasswordPopUp: Boolean = false,
    val tripsCount: Int = 0,
    val distanceCount: Int = 0
)