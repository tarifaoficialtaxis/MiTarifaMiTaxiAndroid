package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.states.ForgotPasswordState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ForgotPasswordState())
    val uiState: StateFlow<ForgotPasswordState> = _uiState

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoBack : NavigationEvent()
    }

    init {
        /*if (Constants.IS_DEV) {
            email = "mateotest1@yopmail.com"
        }*/
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value)
    }

    fun validateEmail() {
        _uiState.update { state ->
            state.copy(
                emailIsError = state.email.isEmpty(),
                emailErrorMessage = if (state.email.isBlank()) appContext.getString(R.string.required_field) else "",
            )
        }

        if (_uiState.value.email.isNotEmpty()) {
            if (!_uiState.value.email.isValidEmail()) {
                _uiState.update { state ->
                    state.copy(
                        emailIsError = true,
                        emailErrorMessage = appContext.getString(R.string.invalid_email)
                    )
                }
            }

        }

        if (!_uiState.value.emailIsError) {
            sendPasswordReset()
        }
    }

    fun sendPasswordReset() {
        appViewModel.setLoading(true)
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(_uiState.value.email.trim())
            .addOnCompleteListener { task ->
                appViewModel.setLoading(false)
                if (task.isSuccessful) {
                    appViewModel.showMessage(
                        type = DialogType.WARNING,
                        title = appContext.getString(R.string.recoveryEmailSent),
                        message = appContext.getString(R.string.weHaveSentRecoveryEmailPassword),
                        buttonText = appContext.getString(R.string.accept),
                        onDismiss = {
                            goBack()
                        },
                        onButtonClicked = {
                            goBack()
                        }
                    )

                } else {
                    val exception = task.exception

                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.somethingWentWrong),
                        message = exception?.localizedMessage
                            ?: appContext.getString(R.string.generalError)
                    )

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
        return ForgotPasswordViewModel(context, appViewModel) as T
    }
}
