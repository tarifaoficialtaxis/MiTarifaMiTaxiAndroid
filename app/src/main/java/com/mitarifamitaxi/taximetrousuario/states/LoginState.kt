package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.LocalUser

data class LoginState(
    val userName: String = "",
    val userNameIsError: Boolean = false,
    val userNameErrorMessage: String = "",
    val password: String = "",
    val passwordIsError: Boolean = false,
    val passwordErrorMessage: String = "",
    val rememberMe: Boolean = false,
    val mustCompleteProfile: Boolean = false,
    val tempUserData: LocalUser? = null,
    val showDialogSelectRole: Boolean = false
)