package com.mitarifamitaxi.taximetrousuario.models.uistate

import com.mitarifamitaxi.taximetrousuario.models.LocalUser

data class LoginUiState(
    val userName: String = "",
    val userNameIsError: Boolean = false,
    val userNameErrorMessage: String = "",
    val password: String = "",
    val passwordIsError: Boolean = false,
    val passwordErrorMessage: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val mustCompleteProfile: Boolean = false,
    val tempUserData: LocalUser? = null,
    val showDialogSelectRole: Boolean = false
)