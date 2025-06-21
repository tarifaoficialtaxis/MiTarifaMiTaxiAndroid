package com.mitarifamitaxi.taximetrousuario.states

data class ForgotPasswordState(
    val email: String = "",
    val emailIsError: Boolean = false,
    val emailErrorMessage: String = ""
)