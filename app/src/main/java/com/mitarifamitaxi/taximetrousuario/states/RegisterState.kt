package com.mitarifamitaxi.taximetrousuario.states

import android.net.Uri

data class RegisterState(
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
    val emailIsError: Boolean = false,
    val emailErrorMessage: String = "",
    val password: String = "",
    val passwordIsError: Boolean = false,
    val passwordErrorMessage: String = "",
    val confirmPassword: String = "",
    val confirmPasswordIsError: Boolean = false,
    val confirmPasswordErrorMessage: String = "",
    val showDialogSelectPhoto: Boolean = false,
    val hasCameraPermission: Boolean = false
)