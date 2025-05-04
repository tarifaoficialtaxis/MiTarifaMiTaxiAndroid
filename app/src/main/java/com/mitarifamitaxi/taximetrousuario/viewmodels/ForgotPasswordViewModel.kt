package com.mitarifamitaxi.taximetrousuario.viewmodels

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.Constants
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var email by mutableStateOf("")
    var emailIsValid by mutableStateOf(true)
    var emailErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoBack : NavigationEvent()
    }

    init {
        if (Constants.IS_DEV) {
            email = "mateotest1@yopmail.com"
        }
    }

    fun validateEmail() {
        emailIsValid = email.isNotEmpty() && email.isValidEmail()
        if (!emailIsValid) {
            emailErrorMessage = appContext.getString(R.string.invalid_email)
        }

        if (emailIsValid) {
            sendPasswordReset()
        }
    }

    fun sendPasswordReset() {
        appViewModel.isLoading = true
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                appViewModel.isLoading = false
                if (task.isSuccessful) {
                    appViewModel.showMessage(
                        type = DialogType.SUCCESS,
                        title = appContext.getString(R.string.recoveryEmailSent),
                        message = appContext.getString(R.string.weHaveSentRecoveryEmailPassword),
                        buttonText = appContext.getString(R.string.accept)
                    )

                    appViewModel.dialogOnPrimaryActionClicked = {
                        goBack()
                    }
                    appViewModel.dialogOnDismiss = {
                        goBack()
                    }

                } else {
                    val exception = task.exception

                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.somethingWentWrong),
                        message = exception?.localizedMessage
                            ?: appContext.getString(R.string.generalError)
                    )

                    appViewModel.dialogOnPrimaryActionClicked = null
                    appViewModel.dialogOnDismiss = null
                }
            }
    }

    fun goBack() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.GoBack)
        }
    }
}

class ForgotPasswordViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            return ForgotPasswordViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
